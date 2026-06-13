package com.charging.shed.service;

import com.charging.shed.entity.*;
import com.charging.shed.exception.BusinessException;
import com.charging.shed.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TemperatureMonitorService {

    @Value("${charging.temperature.threshold:55.0}")
    private BigDecimal temperatureThreshold;

    @Value("${charging.temperature.auto-power-off:true}")
    private boolean autoPowerOffEnabled;

    private final ChargingPortRepository chargingPortRepository;
    private final TemperatureAlertRepository temperatureAlertRepository;
    private final TemperatureHistoryRepository temperatureHistoryRepository;
    private final ChargingRecordRepository chargingRecordRepository;
    private final ReservationRepository reservationRepository;

    public TemperatureMonitorService(ChargingPortRepository chargingPortRepository,
                                     TemperatureAlertRepository temperatureAlertRepository,
                                     TemperatureHistoryRepository temperatureHistoryRepository,
                                     ChargingRecordRepository chargingRecordRepository,
                                     ReservationRepository reservationRepository) {
        this.chargingPortRepository = chargingPortRepository;
        this.temperatureAlertRepository = temperatureAlertRepository;
        this.temperatureHistoryRepository = temperatureHistoryRepository;
        this.chargingRecordRepository = chargingRecordRepository;
        this.reservationRepository = reservationRepository;
    }

    public List<TemperatureAlert> getPendingAlerts() {
        return temperatureAlertRepository.findByStatus(TemperatureAlert.Status.PENDING);
    }

    public List<TemperatureAlert> getAlertsByPort(Long portId) {
        return temperatureAlertRepository.findByPortId(portId);
    }

    public List<TemperatureAlert> getAlertsByHandler(Long handlerId) {
        return temperatureAlertRepository.findByHandledBy(handlerId);
    }

    @Transactional
    public TemperatureAlert reportTemperature(Long portId, BigDecimal temperature, Long reservationId) {
        ChargingPort port = chargingPortRepository.findById(portId)
                .orElseThrow(() -> new BusinessException("充电位不存在"));

        TemperatureHistory history = new TemperatureHistory();
        history.setPortId(portId);
        history.setReservationId(reservationId != null ? reservationId : port.getCurrentReservationId());
        history.setTemperature(temperature);
        temperatureHistoryRepository.save(history);

        port.setCurrentTemperature(temperature);
        chargingPortRepository.save(port);

        if (temperature.compareTo(temperatureThreshold) >= 0 && port.getPowerOn()) {
            return createAlert(port, temperature);
        }

        return null;
    }

    @Transactional
    public TemperatureAlert createAlert(ChargingPort port, BigDecimal temperature) {
        String alertLevel = temperature.compareTo(temperatureThreshold.add(new BigDecimal("5"))) >= 0
                ? TemperatureAlert.AlertLevel.DANGER
                : TemperatureAlert.AlertLevel.WARNING;

        TemperatureAlert alert = new TemperatureAlert();
        alert.setPortId(port.getId());
        alert.setReservationId(port.getCurrentReservationId());

        if (port.getCurrentReservationId() != null) {
            Reservation reservation = reservationRepository.findById(port.getCurrentReservationId()).orElse(null);
            if (reservation != null) {
                alert.setVehicleId(reservation.getVehicleId());
            }
        }

        alert.setTemperature(temperature);
        alert.setThreshold(temperatureThreshold);
        alert.setAlertLevel(alertLevel);
        alert.setStatus(TemperatureAlert.Status.PENDING);

        if (autoPowerOffEnabled && TemperatureAlert.AlertLevel.DANGER.equals(alertLevel)) {
            executeAutoPowerOff(port, alert);
        }

        return temperatureAlertRepository.save(alert);
    }

    @Transactional
    public void executeAutoPowerOff(ChargingPort port, TemperatureAlert alert) {
        port.setPowerOn(false);
        chargingPortRepository.save(port);
        alert.setAutoPowerOff(true);

        if (port.getCurrentReservationId() != null) {
            chargingRecordRepository.findByReservationIdAndStatus(
                    port.getCurrentReservationId(), ChargingRecord.Status.CHARGING
            ).ifPresent(record -> {
                record.setStatus(ChargingRecord.Status.EMERGENCY_STOP);
                record.setEndTime(LocalDateTime.now());
                record.setStopReason("电池温度过高，自动断电保护");
                chargingRecordRepository.save(record);
            });
        }
    }

    @Transactional
    public TemperatureAlert handleAlert(Long alertId, Long handlerId, String handleResult, String remark) {
        TemperatureAlert alert = temperatureAlertRepository.findById(alertId)
                .orElseThrow(() -> new BusinessException("告警不存在"));

        if (!TemperatureAlert.Status.PENDING.equals(alert.getStatus())) {
            throw new BusinessException("该告警已处理或处理中");
        }

        alert.setStatus(TemperatureAlert.Status.PROCESSING);
        alert.setHandledBy(handlerId);
        alert.setHandleResult(handleResult);
        alert.setRemark(remark);

        return temperatureAlertRepository.save(alert);
    }

    @Transactional
    public TemperatureAlert resolveAlert(Long alertId, Long handlerId, String remark) {
        TemperatureAlert alert = temperatureAlertRepository.findById(alertId)
                .orElseThrow(() -> new BusinessException("告警不存在"));

        if (!TemperatureAlert.Status.PROCESSING.equals(alert.getStatus())) {
            throw new BusinessException("该告警不在处理中，无法完成");
        }

        alert.setStatus(TemperatureAlert.Status.RESOLVED);
        alert.setHandledBy(handlerId);
        alert.setHandledAt(LocalDateTime.now());
        if (remark != null && !remark.isEmpty()) {
            alert.setRemark(remark);
        }

        return temperatureAlertRepository.save(alert);
    }

    @Transactional
    public void manualPowerOff(Long portId, Long officerId) {
        ChargingPort port = chargingPortRepository.findById(portId)
                .orElseThrow(() -> new BusinessException("充电位不存在"));

        port.setPowerOn(false);
        chargingPortRepository.save(port);

        if (port.getCurrentReservationId() != null) {
            chargingRecordRepository.findByReservationIdAndStatus(
                    port.getCurrentReservationId(), ChargingRecord.Status.CHARGING
            ).ifPresent(record -> {
                record.setStatus(ChargingRecord.Status.INTERRUPTED);
                record.setEndTime(LocalDateTime.now());
                record.setStopReason("安全员手动断电");
                chargingRecordRepository.save(record);
            });
        }
    }

    @Transactional
    public void manualPowerOn(Long portId, Long officerId) {
        ChargingPort port = chargingPortRepository.findById(portId)
                .orElseThrow(() -> new BusinessException("充电位不存在"));

        if (port.getCurrentTemperature().compareTo(temperatureThreshold) >= 0) {
            throw new BusinessException("当前温度过高，无法通电，请先降温");
        }

        port.setPowerOn(true);
        chargingPortRepository.save(port);
    }

    public List<TemperatureAlert> getAlerts(String status, String level) {
        if (status != null && level != null) {
            return temperatureAlertRepository.findByStatusAndAlertLevel(status, level);
        } else if (status != null) {
            return temperatureAlertRepository.findByStatus(status);
        } else if (level != null) {
            return temperatureAlertRepository.findByAlertLevel(level);
        } else {
            return temperatureAlertRepository.findAllByOrderByCreatedAtDesc();
        }
    }

    public TemperatureAlert getAlertById(Long alertId) {
        return temperatureAlertRepository.findById(alertId)
                .orElseThrow(() -> new BusinessException("告警不存在"));
    }

    public List<TemperatureAlert> getHighTemperatureAlerts() {
        return temperatureAlertRepository.findByStatusAndAlertLevel(
                TemperatureAlert.Status.PENDING,
                TemperatureAlert.AlertLevel.DANGER
        );
    }

    @Scheduled(fixedRate = 30000)
    @Transactional
    public void monitorHighTemperaturePorts() {
        List<ChargingPort> highTempPorts = chargingPortRepository.findPortsWithHighTemperature(temperatureThreshold);

        for (ChargingPort port : highTempPorts) {
            boolean hasPendingAlert = temperatureAlertRepository.findByPortId(port.getId())
                    .stream()
                    .anyMatch(alert -> TemperatureAlert.Status.PENDING.equals(alert.getStatus()));

            if (!hasPendingAlert) {
                createAlert(port, port.getCurrentTemperature());
            }
        }
    }

    public List<TemperatureHistory> getTemperatureHistory(Long portId, LocalDateTime startTime) {
        return temperatureHistoryRepository.findByPortIdAndTimeRange(portId, startTime);
    }

    public List<TemperatureHistory> getTemperatureHistoryByReservation(Long reservationId) {
        return temperatureHistoryRepository.findByReservationIdOrdered(reservationId);
    }
}
