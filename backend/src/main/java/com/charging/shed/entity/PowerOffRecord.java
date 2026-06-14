package com.charging.shed.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "power_off_record")
public class PowerOffRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "port_id", nullable = false)
    private Long portId;

    @Column(name = "reservation_id")
    private Long reservationId;

    @Column(name = "vehicle_id")
    private Long vehicleId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "alert_id")
    private Long alertId;

    @Column(name = "power_off_type", nullable = false, length = 30)
    private String powerOffType;

    @Column(nullable = false, length = 255)
    private String reason;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "power_off_time", nullable = false)
    private LocalDateTime powerOffTime;

    @Column(name = "power_on_time")
    private LocalDateTime powerOnTime;

    @Column(length = 20)
    private String status = Status.POWER_OFF;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "port_id", insertable = false, updatable = false)
    private ChargingPort port;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", insertable = false, updatable = false)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", insertable = false, updatable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id", insertable = false, updatable = false)
    private SafetyAlert alert;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id", insertable = false, updatable = false)
    private User operator;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public interface PowerOffType {
        String AUTO = "AUTO";
        String MANUAL = "MANUAL";
        String EMERGENCY = "EMERGENCY";
    }

    public interface Status {
        String POWER_OFF = "POWER_OFF";
        String POWER_ON = "POWER_ON";
    }
}
