package com.charging.shed.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "charging_port", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"shed_id", "port_code"})
})
public class ChargingPort {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shed_id", nullable = false)
    private Long shedId;

    @Column(name = "port_code", nullable = false, length = 50)
    private String portCode;

    @Column(name = "port_type", length = 20)
    private String portType = "NORMAL";

    @Column(name = "power_rating", precision = 5, scale = 2)
    private BigDecimal powerRating = new BigDecimal("7.0");

    @Column(length = 20)
    private String status = "AVAILABLE";

    @Column(name = "current_temperature", precision = 5, scale = 2)
    private BigDecimal currentTemperature = new BigDecimal("25.0");

    @Column(name = "is_power_on")
    private Boolean powerOn = true;

    @Column(name = "current_reservation_id")
    private Long currentReservationId;

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
        String AVAILABLE = "AVAILABLE";
        String OCCUPIED = "OCCUPIED";
        String MAINTENANCE = "MAINTENANCE";
        String FAULT = "FAULT";
    }

    public interface Type {
        String NORMAL = "NORMAL";
        String FAST = "FAST";
    }
}
