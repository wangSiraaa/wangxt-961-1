package com.charging.shed.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "safety_alert")
public class SafetyAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "port_id", nullable = false)
    private Long portId;

    @Column(name = "reservation_id")
    private Long reservationId;

    @Column(name = "vehicle_id")
    private Long vehicleId;

    @Column(name = "alert_type", nullable = false, length = 30)
    private String alertType;

    @Column(name = "alert_level", length = 20)
    private String alertLevel = AlertLevel.WARNING;

    @Column(name = "alert_value", precision = 10, scale = 2)
    private BigDecimal alertValue;

    @Column(precision = 10, scale = 2)
    private BigDecimal threshold;

    @Column(length = 255)
    private String description;

    @Column(length = 20)
    private String status = Status.PENDING;

    @Column(name = "handled_by")
    private Long handledBy;

    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    @Column(name = "handle_result", length = 255)
    private String handleResult;

    @Column(name = "auto_power_off")
    private Boolean autoPowerOff = false;

    @Column(length = 500)
    private String remark;

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
    @JoinColumn(name = "handled_by", insertable = false, updatable = false)
    private User handler;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public interface AlertType {
        String TEMPERATURE = "TEMPERATURE";
        String SMOKE = "SMOKE";
        String OCCUPANCY = "OCCUPANCY";
        String OVERPOWER = "OVERPOWER";
    }

    public interface AlertLevel {
        String WARNING = "WARNING";
        String DANGER = "DANGER";
    }

    public interface Status {
        String PENDING = "PENDING";
        String PROCESSING = "PROCESSING";
        String RESOLVED = "RESOLVED";
    }
}
