package com.charging.shed.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "reservation")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @Column(name = "port_id", nullable = false)
    private Long portId;

    @Column(name = "shed_id", nullable = false)
    private Long shedId;

    @Column(name = "reserve_start_time", nullable = false)
    private LocalDateTime reserveStartTime;

    @Column(name = "reserve_end_time", nullable = false)
    private LocalDateTime reserveEndTime;

    @Column(name = "actual_start_time")
    private LocalDateTime actualStartTime;

    @Column(name = "actual_end_time")
    private LocalDateTime actualEndTime;

    @Column(length = 20)
    private String status = "PENDING";

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", insertable = false, updatable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "port_id", insertable = false, updatable = false)
    private ChargingPort port;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shed_id", insertable = false, updatable = false)
    private ChargingShed shed;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public interface Status {
        String PENDING = "PENDING";
        String CONFIRMED = "CONFIRMED";
        String IN_PROGRESS = "IN_PROGRESS";
        String COMPLETED = "COMPLETED";
        String CANCELLED = "CANCELLED";
        String EXPIRED = "EXPIRED";
    }
}
