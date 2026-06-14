package com.charging.shed.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Entity
@Table(name = "pricing_rule")
public class PricingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    @Column(name = "shed_id")
    private Long shedId;

    @Column(name = "community_id", length = 50)
    private String communityId;

    @Column(name = "price_per_kwh", nullable = false, precision = 8, scale = 4)
    private BigDecimal pricePerKwh;

    @Column(name = "service_fee", precision = 8, scale = 4)
    private BigDecimal serviceFee = BigDecimal.ZERO;

    @Column(name = "free_minutes")
    private Integer freeMinutes = 0;

    @Column(name = "peak_start_time")
    private LocalTime peakStartTime;

    @Column(name = "peak_end_time")
    private LocalTime peakEndTime;

    @Column(name = "peak_price_multiplier", precision = 4, scale = 2)
    private BigDecimal peakPriceMultiplier = new BigDecimal("1.5");

    @Column(name = "valley_start_time")
    private LocalTime valleyStartTime;

    @Column(name = "valley_end_time")
    private LocalTime valleyEndTime;

    @Column(name = "valley_price_multiplier", precision = 4, scale = 2)
    private BigDecimal valleyPriceMultiplier = new BigDecimal("0.5");

    @Column(name = "flat_price_multiplier", precision = 4, scale = 2)
    private BigDecimal flatPriceMultiplier = new BigDecimal("1.0");

    @Column(name = "is_default")
    private Boolean defaultRule = false;

    @Column(length = 20)
    private String status = "ACTIVE";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shed_id", insertable = false, updatable = false)
    private ChargingShed shed;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
