package com.charging.shed.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "vehicle")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "plate_number", unique = true, length = 20)
    private String plateNumber;

    @Column(name = "vehicle_type", length = 50)
    private String vehicleType;

    @Column(name = "battery_brand", length = 50)
    private String batteryBrand;

    @Column(name = "battery_capacity", precision = 5, scale = 2)
    private BigDecimal batteryCapacity;

    @Column(name = "is_verified")
    private Boolean verified = false;

    @Column(length = 20)
    private String status = "NORMAL";

    @Column(name = "frozen_reason", length = 255)
    private String frozenReason;

    @Column(name = "frozen_at")
    private LocalDateTime frozenAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public interface Status {
        String NORMAL = "NORMAL";
        String ABNORMAL = "ABNORMAL";
        String FROZEN = "FROZEN";
    }
}
