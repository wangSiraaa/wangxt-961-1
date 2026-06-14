package com.charging.shed.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "smoke_detector")
public class SmokeDetector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "port_id", nullable = false)
    private Long portId;

    @Column(name = "device_code", nullable = false, unique = true, length = 50)
    private String deviceCode;

    @Column(length = 100)
    private String location;

    @Column(length = 20)
    private String status = Status.NORMAL;

    @Column(name = "smoke_level", precision = 5, scale = 2)
    private BigDecimal smokeLevel = BigDecimal.ZERO;

    @Column(name = "last_check_time")
    private LocalDateTime lastCheckTime;

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
        String NORMAL = "NORMAL";
        String FAULT = "FAULT";
        String ALARM = "ALARM";
    }
}
