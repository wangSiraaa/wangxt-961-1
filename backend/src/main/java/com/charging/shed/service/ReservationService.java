package com.charging.shed.service;

import com.charging.shed.entity.*;
import com.charging.shed.exception.BusinessException;
import com.charging.shed.repository.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ChargingPortRepository chargingPortRepository;
    private final VehicleService vehicleService;
    private final UserService userService;
    private final ChargingShedRepository chargingShedRepository;
    private final BillingRepository billingRepository;
    private final BatteryBlacklistRepository batteryBlacklistRepository;
    private final PropertyService propertyService;

    public ReservationService(ReservationRepository reservationRepository,
                              ChargingPortRepository chargingPortRepository,
                              VehicleService vehicleService,
                              UserService userService,
                              ChargingShedRepository chargingShedRepository,
                              BillingRepository billingRepository,
                              BatteryBlacklistRepository batteryBlacklistRepository,
                              PropertyService propertyService) {
        this.reservationRepository = reservationRepository;
        this.chargingPortRepository = chargingPortRepository;
        this.vehicleService = vehicleService;
        this.userService = userService;
        this.chargingShedRepository = chargingShedRepository;
        this.billingRepository = billingRepository;
        this.batteryBlacklistRepository = batteryBlacklistRepository;
        this.propertyService = propertyService;
    }

    public List<Reservation> getByUserId(Long userId) {
        return reservationRepository.findByUserId(userId);
    }

    public List<Reservation> getReservations(Long userId) {
        return reservationRepository.findByUserId(userId);
    }

    public List<Reservation> getByUserIdAndStatus(Long userId, String status) {
        return reservationRepository.findByUserIdAndStatus(userId, status);
    }

    public Reservation getById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("预约不存在"));
    }

    public Reservation getReservationById(Long userId, Long reservationId) {
        Reservation reservation = getById(reservationId);
        if (!reservation.getUserId().equals(userId)) {
            throw new BusinessException("无权查看他人预约");
        }
        return reservation;
    }

    public List<Long> getAvailablePorts(Long shedId, LocalDateTime startTime, LocalDateTime endTime) {
        List<ChargingPort> allPorts = chargingPortRepository.findByShedIdAndStatus(shedId, ChargingPort.Status.AVAILABLE);

        List<String> conflictStatuses = Arrays.asList(
                Reservation.Status.PENDING,
                Reservation.Status.CONFIRMED,
                Reservation.Status.IN_PROGRESS
        );

        List<Reservation> conflicts = reservationRepository.findConflictingReservationsByShed(
                shedId, startTime, endTime, conflictStatuses);

        java.util.Set<Long> occupiedPortIds = new java.util.HashSet<>();
        for (Reservation r : conflicts) {
            occupiedPortIds.add(r.getPortId());
        }

        return allPorts.stream()
                .map(ChargingPort::getId)
                .filter(id -> !occupiedPortIds.contains(id))
                .toList();
    }

    @Transactional
    public Reservation createReservation(Long userId, Long vehicleId, Long portId,
                                         LocalDateTime startTime, LocalDateTime endTime) {
        User user = userService.getById(userId);

        if (!user.isVerified()) {
            throw new BusinessException("请先完成实名认证");
        }

        Vehicle vehicle = vehicleService.getById(vehicleId);
        if (!vehicle.getUserId().equals(userId)) {
            throw new BusinessException("无权绑定他人车辆");
        }

        if (batteryBlacklistRepository.existsByBrandNameAndStatus(vehicle.getBatteryBrand(), "ACTIVE")) {
            throw new BusinessException("该电池品牌已被列入黑名单，无法预约充电");
        }

        List<String> activeStatuses = Arrays.asList(
                Reservation.Status.PENDING,
                Reservation.Status.CONFIRMED,
                Reservation.Status.QUEUED,
                Reservation.Status.IN_PROGRESS
        );
        List<Reservation> existingReservations = reservationRepository.findByVehicleIdAndStatuses(vehicleId, activeStatuses);
        if (!existingReservations.isEmpty()) {
            throw new BusinessException("该车辆已有进行中或排队中的预约");
        }

        if (billingRepository.hasUnpaidBills(userId) || user.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("您有未结清的账单，请先缴费后再预约");
        }

        ChargingPort port = chargingPortRepository.findByIdWithLock(portId)
                .orElseThrow(() -> new BusinessException("充电位不存在"));

        ChargingShed shed = chargingShedRepository.findById(port.getShedId())
                .orElseThrow(() -> new BusinessException("车棚不存在"));

        PricingRule rule = propertyService.getApplicableRule(shed.getId());
        if (rule != null) {
            BigDecimal hours = BigDecimal.valueOf(Duration.between(startTime, endTime).toMinutes())
                    .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
            BigDecimal electricityCost = port.getPowerRating().multiply(hours).multiply(rule.getPricePerKwh()).multiply(new BigDecimal("1.5"));
            BigDecimal serviceCost = port.getPowerRating().multiply(hours).multiply(rule.getServiceFee());
            BigDecimal estimate = electricityCost.add(serviceCost);
            if (user.getBalance().compareTo(estimate.multiply(new BigDecimal("0.5"))) < 0) {
                throw new BusinessException("账户余额不足，请先充值");
            }
        }

        if (shed.getCurrentTotalPower().add(port.getPowerRating()).compareTo(shed.getMaxPowerLimit()) > 0) {
            return createQueuedReservation(userId, vehicleId, port.getShedId(), startTime, endTime);
        }

        if (!ChargingPort.Status.AVAILABLE.equals(port.getStatus())) {
            return createQueuedReservation(userId, vehicleId, port.getShedId(), startTime, endTime);
        }

        if (!ChargingShed.Status.OPEN.equals(shed.getStatus())) {
            throw new BusinessException("车棚当前未营业");
        }

        List<String> conflictStatuses = Arrays.asList(
                Reservation.Status.PENDING,
                Reservation.Status.CONFIRMED,
                Reservation.Status.IN_PROGRESS
        );
        List<Reservation> conflicts = reservationRepository.findConflictingReservations(
                portId, startTime, endTime, conflictStatuses);

        if (!conflicts.isEmpty()) {
            return createQueuedReservation(userId, vehicleId, port.getShedId(), startTime, endTime);
        }

        Reservation reservation = new Reservation();
        reservation.setUserId(userId);
        reservation.setVehicleId(vehicleId);
        reservation.setPortId(portId);
        reservation.setShedId(port.getShedId());
        reservation.setReserveStartTime(startTime);
        reservation.setReserveEndTime(endTime);
        reservation.setStatus(Reservation.Status.CONFIRMED);

        reservation = reservationRepository.save(reservation);

        port.setStatus(ChargingPort.Status.OCCUPIED);
        port.setCurrentReservationId(reservation.getId());
        chargingPortRepository.save(port);

        shed.setAvailablePorts(shed.getAvailablePorts() - 1);
        shed.setCurrentTotalPower(shed.getCurrentTotalPower().add(port.getPowerRating()));
        chargingShedRepository.save(shed);

        return reservation;
    }

    private Reservation createQueuedReservation(Long userId, Long vehicleId, Long shedId,
                                                 LocalDateTime startTime, LocalDateTime endTime) {
        Integer maxQueue = reservationRepository.findMaxQueuePosition(shedId);
        int queuePosition = (maxQueue != null ? maxQueue : 0) + 1;

        LocalDateTime scheduledStart = calculateScheduledStartTime(shedId);

        Reservation reservation = new Reservation();
        reservation.setUserId(userId);
        reservation.setVehicleId(vehicleId);
        reservation.setPortId(0L);
        reservation.setShedId(shedId);
        reservation.setReserveStartTime(startTime);
        reservation.setReserveEndTime(endTime);
        reservation.setStatus(Reservation.Status.QUEUED);
        reservation.setQueuePosition(queuePosition);
        reservation.setScheduledStartTime(scheduledStart);

        return reservationRepository.save(reservation);
    }

    private LocalDateTime calculateScheduledStartTime(Long shedId) {
        List<Reservation> inProgress = reservationRepository.findByShedIdAndStatus(shedId, Reservation.Status.IN_PROGRESS);
        if (inProgress.isEmpty()) {
            return LocalDateTime.now();
        }

        LocalDateTime earliestEnd = null;
        for (Reservation r : inProgress) {
            LocalDateTime end = r.getReserveEndTime();
            if (earliestEnd == null || end.isBefore(earliestEnd)) {
                earliestEnd = end;
            }
        }
        return earliestEnd != null ? earliestEnd : LocalDateTime.now();
    }

    @Transactional
    public void processQueue(Long shedId, Long freedPortId) {
        List<Reservation> queued = reservationRepository.findQueuedByShedId(shedId);
        if (queued.isEmpty()) {
            return;
        }

        Reservation next = queued.get(0);

        ChargingPort port = chargingPortRepository.findById(freedPortId).orElseThrow();
        if (!ChargingPort.Status.AVAILABLE.equals(port.getStatus())) {
            return;
        }

        ChargingShed shed = chargingShedRepository.findById(shedId).orElseThrow();

        next.setStatus(Reservation.Status.CONFIRMED);
        next.setPortId(freedPortId);
        next.setQueuePosition(null);
        next.setScheduledStartTime(null);
        reservationRepository.save(next);

        port.setStatus(ChargingPort.Status.OCCUPIED);
        port.setCurrentReservationId(next.getId());
        chargingPortRepository.save(port);

        shed.setAvailablePorts(shed.getAvailablePorts() - 1);
        shed.setCurrentTotalPower(shed.getCurrentTotalPower().add(port.getPowerRating()));
        chargingShedRepository.save(shed);

        reorderQueue(shedId);
    }

    private void reorderQueue(Long shedId) {
        List<Reservation> queued = reservationRepository.findQueuedByShedId(shedId);
        int position = 1;
        for (Reservation r : queued) {
            r.setQueuePosition(position++);
            reservationRepository.save(r);
        }
    }

    public Reservation cancelReservation(Long userId, Long reservationId) {
        return cancelReservation(userId, reservationId, "用户主动取消");
    }

    @Transactional
    public Reservation cancelReservation(Long userId, Long reservationId, String reason) {
        Reservation reservation = getById(reservationId);

        if (!reservation.getUserId().equals(userId)) {
            throw new BusinessException("无权取消他人预约");
        }

        if (!Arrays.asList(Reservation.Status.PENDING, Reservation.Status.CONFIRMED, Reservation.Status.QUEUED)
                .contains(reservation.getStatus())) {
            throw new BusinessException("该预约状态无法取消");
        }

        boolean wasQueued = Reservation.Status.QUEUED.equals(reservation.getStatus());

        reservation.setStatus(Reservation.Status.CANCELLED);
        reservation.setCancelReason(reason);
        reservation = reservationRepository.save(reservation);

        if (wasQueued) {
            reorderQueue(reservation.getShedId());
        } else {
            if (reservation.getPortId() != null && reservation.getPortId() > 0) {
                ChargingPort port = chargingPortRepository.findById(reservation.getPortId()).orElseThrow();
                port.setStatus(ChargingPort.Status.AVAILABLE);
                port.setCurrentReservationId(null);
                chargingPortRepository.save(port);

                ChargingShed shed = chargingShedRepository.findById(reservation.getShedId()).orElseThrow();
                shed.setAvailablePorts(shed.getAvailablePorts() + 1);
                shed.setCurrentTotalPower(shed.getCurrentTotalPower().subtract(port.getPowerRating()));
                chargingShedRepository.save(shed);

                processQueue(reservation.getShedId(), port.getId());
            }
        }

        return reservation;
    }

    @Transactional
    public Reservation startCharging(Long reservationId) {
        Reservation reservation = getById(reservationId);

        if (!Reservation.Status.CONFIRMED.equals(reservation.getStatus())) {
            throw new BusinessException("预约未确认，无法开始充电");
        }

        reservation.setActualStartTime(LocalDateTime.now());
        reservation.setStatus(Reservation.Status.IN_PROGRESS);
        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation completeCharging(Long reservationId) {
        Reservation reservation = getById(reservationId);

        if (!Reservation.Status.IN_PROGRESS.equals(reservation.getStatus())) {
            throw new BusinessException("充电未进行中");
        }

        reservation.setActualEndTime(LocalDateTime.now());
        reservation.setStatus(Reservation.Status.COMPLETED);
        reservation = reservationRepository.save(reservation);

        ChargingPort port = chargingPortRepository.findById(reservation.getPortId()).orElseThrow();
        Long shedId = reservation.getShedId();
        port.setStatus(ChargingPort.Status.AVAILABLE);
        port.setCurrentReservationId(null);
        chargingPortRepository.save(port);

        ChargingShed shed = chargingShedRepository.findById(shedId).orElseThrow();
        shed.setAvailablePorts(shed.getAvailablePorts() + 1);
        shed.setCurrentTotalPower(shed.getCurrentTotalPower().subtract(port.getPowerRating()));
        chargingShedRepository.save(shed);

        processQueue(shedId, port.getId());

        return reservation;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expireReservations() {
        List<Reservation> expired = reservationRepository.findExpiredReservations(
                Reservation.Status.CONFIRMED, LocalDateTime.now());

        for (Reservation reservation : expired) {
            reservation.setStatus(Reservation.Status.EXPIRED);
            reservationRepository.save(reservation);

            ChargingPort port = chargingPortRepository.findById(reservation.getPortId()).orElse(null);
            if (port != null) {
                Long shedId = reservation.getShedId();
                port.setStatus(ChargingPort.Status.AVAILABLE);
                port.setCurrentReservationId(null);
                chargingPortRepository.save(port);

                ChargingShed shed = chargingShedRepository.findById(shedId).orElse(null);
                if (shed != null) {
                    shed.setAvailablePorts(shed.getAvailablePorts() + 1);
                    shed.setCurrentTotalPower(shed.getCurrentTotalPower().subtract(port.getPowerRating()));
                    chargingShedRepository.save(shed);

                    processQueue(shedId, port.getId());
                }
            }
        }
    }

    public List<Reservation> getQueuedReservations(Long shedId) {
        return reservationRepository.findQueuedByShedId(shedId);
    }
}
