package com.charging.shed.service;

import com.charging.shed.entity.*;
import com.charging.shed.exception.BusinessException;
import com.charging.shed.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SafetyService {

    @Value("${charging.temperature.threshold:55.0}")
    private BigDecimal temperatureThreshold;

    @Value("${charging.smoke.threshold:30.0}")
    private BigDecimal smokeThreshold;

    @Value("${charging.occupancy.max-minutes:480}")
    private int maxOccupancyMinutes;

    private final SafetyAlertRepository safetyAlertRepository;
    private final PowerOffRecordRepository powerOffRecordRepository;
    private final ReviewRecordRepository reviewRecordRepository;
    private final SmokeDetectorRepository smokeDetectorRepository;
    private final ChargingPortRepository chargingPortRepository;
    private final ChargingRecordRepository chargingRecordRepository;
    private final ReservationRepository reservationRepository;
    private final VehicleRepository vehicleRepository;
    private final TemperatureAlertRepository temperatureAlertRepository;
    private final TemperatureHistoryRepository temperatureHistoryRepository;

    public SafetyService(SafetyAlertRepository safetyAlertRepository,
                         PowerOffRecordRepository powerOffRecordRepository,
                         ReviewRecordRepository reviewRecordRepository,
                         SmokeDetectorRepository smokeDetectorRepository,
                         ChargingPortRepository chargingPortRepository,
                         ChargingRecordRepository chargingRecordRepository,
                         ReservationRepository reservationRepository,
                         VehicleRepository vehicleRepository,
                         TemperatureAlertRepository temperatureAlertRepository,
                         TemperatureHistoryRepository temperatureHistoryRepository) {
        this.safetyAlertRepository = safetyAlertRepository;
        this.powerOffRecordRepository = powerOffRecordRepository;
        this.reviewRecordRepository = reviewRecordRepository;
        this.smokeDetectorRepository = smokeDetectorRepository;
        this.chargingPortRepository = chargingPortRepository;
        this.chargingRecordRepository = chargingRecordRepository;
        this.reservationRepository = reservationRepository;
        this.vehicleRepository = vehicleRepository;
        this.temperatureAlertRepository = temperatureAlertRepository;
        this.temperatureHistoryRepository = temperatureHistoryRepository;
    }

    public List<SafetyAlert> getAlerts(String status, String alertType) {
        if (status != null && alertType != null) {
            return safetyAlertRepository.findByStatusAndAlertType(status, alertType);
        } else if (status != null) {
            return safetyAlertRepository.findByStatus(status);
        } else if (alertType != null) {
            return safetyAlertRepository.findByAlertType(alertType);
        } else {
            return safetyAlertRepository.findAllByOrderByCreatedAtDesc();
        }
    }

    public SafetyAlert getAlertById(Long id) {
        return safetyAlertRepository.findById(id)
                .orElseThrow(() -> new BusinessException("告警不存在"));
    }

    public List<SafetyAlert> getPendingAlerts() {
        return safetyAlertRepository.findByStatusOrderByCreatedAtDesc(SafetyAlert.Status.PENDING);
    }

    public List<SafetyAlert> getAlertsByPort(Long portId) {
        return safetyAlertRepository.findByPortId(portId);
    }

    public List<SafetyAlert> getAlertsByVehicle(Long vehicleId) {
        return safetyAlertRepository.findByVehicleId(vehicleId);
    }

    @Transactional
    public SafetyAlert handleAlert(Long alertId, Long handlerId, String handleResult, String remark) {
        SafetyAlert alert = safetyAlertRepository.findById(alertId)
                .orElseThrow(() -> new BusinessException("告警不存在"));

        if (!SafetyAlert.Status.PENDING.equals(alert.getStatus())) {
            throw new BusinessException("该告警已处理或处理中");
        }

        alert.setStatus(SafetyAlert.Status.PROCESSING);
        alert.setHandledBy(handlerId);
        alert.setHandleResult(handleResult);
        alert.setRemark(remark);

        return safetyAlertRepository.save(alert);
    }

    @Transactional
    public SafetyAlert resolveAlert(Long alertId, Long handlerId, String remark) {
        SafetyAlert alert = safetyAlertRepository.findById(alertId)
                .orElseThrow(() -> new BusinessException("告警不存在"));

        if (!SafetyAlert.Status.PROCESSING.equals(alert.getStatus())) {
            throw new BusinessException("该告警不在处理中，无法完成");
        }

        alert.setStatus(SafetyAlert.Status.RESOLVED);
        alert.setHandledBy(handlerId);
        alert.setHandledAt(LocalDateTime.now());
        if (remark != null && !remark.isEmpty()) {
            alert.setRemark(remark);
        }

        return safetyAlertRepository.save(alert);
    }

    @Transactional
    public SafetyAlert reportSmokeAlert(Long portId, BigDecimal smokeLevel, Long reservationId) {
        ChargingPort port = chargingPortRepository.findById(portId)
                .orElseThrow(() -> new BusinessException("充电位不存在"));

        String alertLevel = smokeLevel.compareTo(smokeThreshold) >= 0
                ? SafetyAlert.AlertLevel.DANGER
                : SafetyAlert.AlertLevel.WARNING;

        SafetyAlert alert = new SafetyAlert();
        alert.setPortId(portId);
        alert.setReservationId(reservationId != null ? reservationId : port.getCurrentReservationId());
        alert.setAlertType(SafetyAlert.AlertType.SMOKE);
        alert.setAlertLevel(alertLevel);
        alert.setAlertValue(smokeLevel);
        alert.setThreshold(smokeThreshold);
        alert.setDescription("烟雾浓度告警，当前值: " + smokeLevel + "，阈值: " + smokeThreshold);
        alert.setStatus(SafetyAlert.Status.PENDING);

        if (port.getCurrentReservationId() != null) {
            Reservation reservation = reservationRepository.findById(port.getCurrentReservationId()).orElse(null);
            if (reservation != null) {
                alert.setVehicleId(reservation.getVehicleId());
            }
        }

        List<SmokeDetector> detectors = smokeDetectorRepository.findByPortId(portId);
        if (!detectors.isEmpty()) {
            SmokeDetector detector = detectors.get(0);
            detector.setSmokeLevel(smokeLevel);
            detector.setStatus(SmokeDetector.Status.ALARM);
            smokeDetectorRepository.save(detector);
        }

        alert = safetyAlertRepository.save(alert);

        if (SafetyAlert.AlertLevel.DANGER.equals(alertLevel)) {
            executeAutoPowerOff(port, alert);
            alert = safetyAlertRepository.save(alert);
        }

        return alert;
    }

    @Transactional
    public SafetyAlert reportOccupancyAlert(Long portId, Long reservationId, long occupiedMinutes) {
        if (occupiedMinutes <= maxOccupancyMinutes) {
            throw new BusinessException("占用时长未超过阈值，无需告警");
        }

        ChargingPort port = chargingPortRepository.findById(portId)
                .orElseThrow(() -> new BusinessException("充电位不存在"));

        SafetyAlert alert = new SafetyAlert();
        alert.setPortId(portId);
        alert.setReservationId(reservationId != null ? reservationId : port.getCurrentReservationId());
        alert.setAlertType(SafetyAlert.AlertType.OCCUPANCY);
        alert.setAlertLevel(SafetyAlert.AlertLevel.WARNING);
        alert.setAlertValue(BigDecimal.valueOf(occupiedMinutes));
        alert.setThreshold(BigDecimal.valueOf(maxOccupancyMinutes));
        alert.setDescription("充电位占用超时，已占用: " + occupiedMinutes + "分钟，最大允许: " + maxOccupancyMinutes + "分钟");
        alert.setStatus(SafetyAlert.Status.PENDING);

        if (port.getCurrentReservationId() != null) {
            Reservation reservation = reservationRepository.findById(port.getCurrentReservationId()).orElse(null);
            if (reservation != null) {
                alert.setVehicleId(reservation.getVehicleId());
            }
        }

        return safetyAlertRepository.save(alert);
    }

    @Transactional
    public SafetyAlert reportTemperatureAlert(Long portId, BigDecimal temperature, Long reservationId) {
        ChargingPort port = chargingPortRepository.findById(portId)
                .orElseThrow(() -> new BusinessException("充电位不存在"));

        TemperatureHistory history = new TemperatureHistory();
        history.setPortId(portId);
        history.setReservationId(reservationId != null ? reservationId : port.getCurrentReservationId());
        history.setTemperature(temperature);
        temperatureHistoryRepository.save(history);

        port.setCurrentTemperature(temperature);
        chargingPortRepository.save(port);

        String alertLevel = temperature.compareTo(temperatureThreshold.add(new BigDecimal("5"))) >= 0
                ? SafetyAlert.AlertLevel.DANGER
                : SafetyAlert.AlertLevel.WARNING;

        SafetyAlert alert = new SafetyAlert();
        alert.setPortId(portId);
        alert.setReservationId(reservationId != null ? reservationId : port.getCurrentReservationId());
        alert.setAlertType(SafetyAlert.AlertType.TEMPERATURE);
        alert.setAlertLevel(alertLevel);
        alert.setAlertValue(temperature);
        alert.setThreshold(temperatureThreshold);
        alert.setDescription("温度告警，当前温度: " + temperature + "°C，阈值: " + temperatureThreshold + "°C");
        alert.setStatus(SafetyAlert.Status.PENDING);

        if (port.getCurrentReservationId() != null) {
            Reservation reservation = reservationRepository.findById(port.getCurrentReservationId()).orElse(null);
            if (reservation != null) {
                alert.setVehicleId(reservation.getVehicleId());
            }
        }

        alert = safetyAlertRepository.save(alert);

        if (SafetyAlert.AlertLevel.DANGER.equals(alertLevel)) {
            executeAutoPowerOff(port, alert);
            if (alert.getVehicleId() != null) {
                freezeVehicle(alert.getVehicleId(), "温度达到危险级别，自动冻结车辆");
            }
            alert = safetyAlertRepository.save(alert);
        }

        return alert;
    }

    @Transactional
    public void executeAutoPowerOff(ChargingPort port, SafetyAlert alert) {
        port.setPowerOn(false);
        chargingPortRepository.save(port);

        if (port.getCurrentReservationId() != null) {
            chargingRecordRepository.findByReservationIdAndStatus(
                    port.getCurrentReservationId(), ChargingRecord.Status.CHARGING
            ).ifPresent(record -> {
                record.setStatus(ChargingRecord.Status.EMERGENCY_STOP);
                record.setEndTime(LocalDateTime.now());
                record.setStopReason("安全告警自动断电");
                chargingRecordRepository.save(record);
            });
        }

        alert.setAutoPowerOff(true);

        PowerOffRecord powerOffRecord = new PowerOffRecord();
        powerOffRecord.setPortId(port.getId());
        powerOffRecord.setReservationId(port.getCurrentReservationId());
        powerOffRecord.setAlertId(alert.getId());
        powerOffRecord.setPowerOffType(PowerOffRecord.PowerOffType.AUTO);
        powerOffRecord.setReason(alert.getDescription());
        powerOffRecord.setPowerOffTime(LocalDateTime.now());
        powerOffRecord.setStatus(PowerOffRecord.Status.POWER_OFF);

        if (alert.getVehicleId() != null) {
            powerOffRecord.setVehicleId(alert.getVehicleId());
        }
        if (port.getCurrentReservationId() != null) {
            powerOffRecord.setReservationId(port.getCurrentReservationId());
            chargingRecordRepository.findByReservationIdAndStatus(
                    port.getCurrentReservationId(), ChargingRecord.Status.EMERGENCY_STOP
            ).ifPresent(record -> powerOffRecord.setUserId(record.getUserId()));
        }

        powerOffRecordRepository.save(powerOffRecord);
    }

    @Transactional
    public void manualPowerOff(Long portId, Long operatorId, String reason) {
        ChargingPort port = chargingPortRepository.findById(portId)
                .orElseThrow(() -> new BusinessException("充电位不存在"));

        if (!port.getPowerOn()) {
            throw new BusinessException("该充电位已处于断电状态");
        }

        port.setPowerOn(false);
        chargingPortRepository.save(port);

        if (port.getCurrentReservationId() != null) {
            chargingRecordRepository.findByReservationIdAndStatus(
                    port.getCurrentReservationId(), ChargingRecord.Status.CHARGING
            ).ifPresent(record -> {
                record.setStatus(ChargingRecord.Status.INTERRUPTED);
                record.setEndTime(LocalDateTime.now());
                record.setStopReason(reason != null ? reason : "手动断电");
                chargingRecordRepository.save(record);
            });
        }

        PowerOffRecord powerOffRecord = new PowerOffRecord();
        powerOffRecord.setPortId(portId);
        powerOffRecord.setOperatorId(operatorId);
        powerOffRecord.setPowerOffType(PowerOffRecord.PowerOffType.MANUAL);
        powerOffRecord.setReason(reason != null ? reason : "手动断电");
        powerOffRecord.setPowerOffTime(LocalDateTime.now());
        powerOffRecord.setStatus(PowerOffRecord.Status.POWER_OFF);

        if (port.getCurrentReservationId() != null) {
            powerOffRecord.setReservationId(port.getCurrentReservationId());
            chargingRecordRepository.findByReservationIdAndStatus(
                    port.getCurrentReservationId(), ChargingRecord.Status.INTERRUPTED
            ).ifPresent(record -> {
                powerOffRecord.setVehicleId(record.getVehicleId());
                powerOffRecord.setUserId(record.getUserId());
            });
        }

        powerOffRecordRepository.save(powerOffRecord);
    }

    @Transactional
    public void manualPowerOn(Long portId, Long operatorId) {
        ChargingPort port = chargingPortRepository.findById(portId)
                .orElseThrow(() -> new BusinessException("充电位不存在"));

        if (port.getPowerOn()) {
            throw new BusinessException("该充电位已处于通电状态");
        }

        if (port.getCurrentTemperature().compareTo(temperatureThreshold) >= 0) {
            throw new BusinessException("当前温度过高，无法通电，请先降温");
        }

        port.setPowerOn(true);
        chargingPortRepository.save(port);

        List<PowerOffRecord> records = powerOffRecordRepository.findByPortId(portId);
        records.stream()
                .filter(r -> PowerOffRecord.Status.POWER_OFF.equals(r.getStatus()))
                .forEach(r -> {
                    r.setStatus(PowerOffRecord.Status.POWER_ON);
                    r.setPowerOnTime(LocalDateTime.now());
                    powerOffRecordRepository.save(r);
                });
    }

    @Transactional
    public void freezeVehicle(Long vehicleId, String reason) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new BusinessException("车辆不存在"));

        if (Vehicle.Status.FROZEN.equals(vehicle.getStatus())) {
            return;
        }

        vehicle.setStatus(Vehicle.Status.FROZEN);
        vehicle.setFrozenReason(reason);
        vehicle.setFrozenAt(LocalDateTime.now());
        vehicleRepository.save(vehicle);
    }

    @Transactional
    public ReviewRecord reviewVehicleUnfreeze(Long vehicleId, Long reviewerId, String reviewResult, String remark) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new BusinessException("车辆不存在"));

        if (!Vehicle.Status.FROZEN.equals(vehicle.getStatus())) {
            throw new BusinessException("该车辆未被冻结，无需解冻审核");
        }

        ReviewRecord record = new ReviewRecord();
        record.setVehicleId(vehicleId);
        record.setUserId(vehicle.getUserId());
        record.setReviewType(ReviewRecord.ReviewType.VEHICLE_UNFREEZE);
        record.setReviewResult(reviewResult);
        record.setReviewerId(reviewerId);
        record.setReviewRemark(remark);
        record.setReviewedAt(LocalDateTime.now());

        reviewRecordRepository.save(record);

        if (ReviewRecord.ReviewResult.APPROVED.equals(reviewResult)) {
            vehicle.setStatus(Vehicle.Status.NORMAL);
            vehicle.setFrozenReason(null);
            vehicle.setFrozenAt(null);
            vehicleRepository.save(vehicle);
        }

        return record;
    }

    @Transactional
    public ReviewRecord reviewChargeResume(Long vehicleId, Long powerOffId, Long reviewerId, String reviewResult, String remark) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new BusinessException("车辆不存在"));

        PowerOffRecord powerOffRecord = powerOffRecordRepository.findById(powerOffId)
                .orElseThrow(() -> new BusinessException("断电记录不存在"));

        ReviewRecord record = new ReviewRecord();
        record.setVehicleId(vehicleId);
        record.setUserId(vehicle.getUserId());
        record.setPowerOffId(powerOffId);
        record.setReviewType(ReviewRecord.ReviewType.CHARGE_RESUME);
        record.setReviewResult(reviewResult);
        record.setReviewerId(reviewerId);
        record.setReviewRemark(remark);
        record.setReviewedAt(LocalDateTime.now());

        reviewRecordRepository.save(record);

        if (ReviewRecord.ReviewResult.APPROVED.equals(reviewResult)) {
            ChargingPort port = chargingPortRepository.findById(powerOffRecord.getPortId())
                    .orElse(null);
            if (port != null) {
                port.setPowerOn(true);
                chargingPortRepository.save(port);

                powerOffRecord.setStatus(PowerOffRecord.Status.POWER_ON);
                powerOffRecord.setPowerOnTime(LocalDateTime.now());
                powerOffRecordRepository.save(powerOffRecord);
            }
        }

        return record;
    }

    public List<PowerOffRecord> getPowerOffRecords(Long portId) {
        return powerOffRecordRepository.findByPortId(portId);
    }

    public List<ReviewRecord> getReviewRecords(Long vehicleId) {
        return reviewRecordRepository.findByVehicleId(vehicleId);
    }

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void getScheduledOccupancyCheck() {
        List<Reservation> activeReservations = reservationRepository.findByStatus(Reservation.Status.IN_PROGRESS);

        for (Reservation reservation : activeReservations) {
            if (reservation.getActualStartTime() == null) {
                continue;
            }

            long occupiedMinutes = Duration.between(reservation.getActualStartTime(), LocalDateTime.now()).toMinutes();

            if (occupiedMinutes > maxOccupancyMinutes) {
                boolean hasExistingAlert = safetyAlertRepository.findByPortId(reservation.getPortId())
                        .stream()
                        .anyMatch(a -> SafetyAlert.AlertType.OCCUPANCY.equals(a.getAlertType())
                                && SafetyAlert.Status.PENDING.equals(a.getStatus())
                                && reservation.getId().equals(a.getReservationId()));

                if (!hasExistingAlert) {
                    reportOccupancyAlert(reservation.getPortId(), reservation.getId(), occupiedMinutes);
                }
            }
        }
    }
}
