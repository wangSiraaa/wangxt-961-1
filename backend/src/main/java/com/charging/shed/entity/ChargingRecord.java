package com.charging.shed.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "charging_record")
public class ChargingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    @Column(name = "port_id", nullable = false)
    private Long portId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "start_soc", precision = 5, scale = 2)
    private BigDecimal startSoc;

    @Column(name = "end_soc", precision = 5, scale = 2)
    private BigDecimal endSoc;

    @Column(name = "energy_consumed", precision = 8, scale = 2)
    private BigDecimal energyConsumed = BigDecimal.ZERO;

    @Column(name = "max_temperature", precision = 5, scale = 2)
    private BigDecimal maxTemperature;

    @Column(name = "avg_temperature", precision = 5, scale = 2)
    private BigDecimal avgTemperature;

    @Column(length = 20)
    private String status = "CHARGING";

    @Column(name = "stop_reason", length = 255)
    private String stopReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", insertable = false, updatable = false)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "port_id", insertable = false, updatable = false)
    private ChargingPort port;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public interface Status {
        String CHARGING = "CHARGING";
        String COMPLETED = "COMPLETED";
        String INTERRUPTED = "INTERRUPTED";
        String EMERGENCY_STOP = "EMERGENCY_STOP";
    }
}
