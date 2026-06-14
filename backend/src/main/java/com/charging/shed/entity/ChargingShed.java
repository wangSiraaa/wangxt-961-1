package com.charging.shed.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "charging_shed")
public class ChargingShed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shed_name", nullable = false, length = 100)
    private String shedName;

    @Column(nullable = false)
    private String location;

    @Column(name = "total_ports")
    private Integer totalPorts = 0;

    @Column(name = "available_ports")
    private Integer availablePorts = 0;

    @Column(name = "max_power_limit", precision = 10, scale = 2)
    private BigDecimal maxPowerLimit = new BigDecimal("100.00");

    @Column(name = "current_total_power", precision = 10, scale = 2)
    private BigDecimal currentTotalPower = BigDecimal.ZERO;

    @Column(length = 20)
    private String status = "OPEN";

    @Column(name = "manager_id")
    private Long managerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", insertable = false, updatable = false)
    private User manager;

    @OneToMany(mappedBy = "shed", fetch = FetchType.LAZY)
    private List<ChargingPort> ports;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public interface Status {
        String OPEN = "OPEN";
        String CLOSED = "CLOSED";
        String MAINTENANCE = "MAINTENANCE";
    }
}
