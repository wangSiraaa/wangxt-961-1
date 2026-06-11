package com.charging.shed.service;

import com.charging.shed.entity.*;
import com.charging.shed.exception.BusinessException;
import com.charging.shed.repository.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    public ReservationService(ReservationRepository reservationRepository,
                              ChargingPortRepository chargingPortRepository,
                              VehicleService vehicleService,
                              UserService userService,
                              ChargingShedRepository chargingShedRepository,
                              BillingRepository billingRepository) {
        this.reservationRepository = reservationRepository;
        this.chargingPortRepository = chargingPortRepository;
        this.vehicleService = vehicleService;
        this.userService = userService;
        this.chargingShedRepository = chargingShedRepository;
        this.billingRepository = billingRepository;
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

    public Reservation cancelReservation(Long userId, Long reservationId) {
        return cancelReservation(userId, reservationId, "用户主动取消");
    }

    @Transactional
    public Reservation createReservation(Long userId, Long vehicleId, Long portId,
                                         LocalDateTime startTime, LocalDateTime endTime) {
        User user = userService.getById(userId);

        if (billingRepository.hasUnpaidBills(userId) || user.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("您有未结清的账单，请先缴费后再预约");
        }

        Vehicle vehicle = vehicleService.getById(vehicleId);
        if (!vehicle.getUserId().equals(userId)) {
            throw new BusinessException("无权绑定他人车辆");
        }

        if (!vehicleService.canVehicleCharge(vehicleId)) {
            throw new BusinessException("车辆未完成实名认证或状态异常，无法预约充电");
        }

        ChargingPort port = chargingPortRepository.findByIdWithLock(portId)
                .orElseThrow(() -> new BusinessException("充电位不存在"));

        if (!ChargingPort.Status.AVAILABLE.equals(port.getStatus())) {
            throw new BusinessException("充电位当前不可用");
        }

        ChargingShed shed = chargingShedRepository.findById(port.getShedId())
                .orElseThrow(() -> new BusinessException("车棚不存在"));

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
            throw new BusinessException("该时段充电位已被预约，请选择其他时段");
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
        chargingShedRepository.save(shed);

        return reservation;
    }

    @Transactional
    public Reservation cancelReservation(Long userId, Long reservationId, String reason) {
        Reservation reservation = getById(reservationId);

        if (!reservation.getUserId().equals(userId)) {
            throw new BusinessException("无权取消他人预约");
        }

        if (!Arrays.asList(Reservation.Status.PENDING, Reservation.Status.CONFIRMED)
                .contains(reservation.getStatus())) {
            throw new BusinessException("该预约状态无法取消");
        }

        reservation.setStatus(Reservation.Status.CANCELLED);
        reservation.setCancelReason(reason);
        reservation = reservationRepository.save(reservation);

        ChargingPort port = chargingPortRepository.findById(reservation.getPortId()).orElseThrow();
        port.setStatus(ChargingPort.Status.AVAILABLE);
        port.setCurrentReservationId(null);
        chargingPortRepository.save(port);

        ChargingShed shed = chargingShedRepository.findById(reservation.getShedId()).orElseThrow();
        shed.setAvailablePorts(shed.getAvailablePorts() + 1);
        chargingShedRepository.save(shed);

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
        port.setStatus(ChargingPort.Status.AVAILABLE);
        port.setCurrentReservationId(null);
        chargingPortRepository.save(port);

        ChargingShed shed = chargingShedRepository.findById(reservation.getShedId()).orElseThrow();
        shed.setAvailablePorts(shed.getAvailablePorts() + 1);
        chargingShedRepository.save(shed);

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
                port.setStatus(ChargingPort.Status.AVAILABLE);
                port.setCurrentReservationId(null);
                chargingPortRepository.save(port);

                ChargingShed shed = chargingShedRepository.findById(reservation.getShedId()).orElse(null);
                if (shed != null) {
                    shed.setAvailablePorts(shed.getAvailablePorts() + 1);
                    chargingShedRepository.save(shed);
                }
            }
        }
    }
}
