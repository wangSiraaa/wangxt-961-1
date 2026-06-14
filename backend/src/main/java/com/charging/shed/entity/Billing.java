package com.charging.shed.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "billing")
public class Billing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "reservation_id")
    private Long reservationId;

    @Column(name = "charging_record_id")
    private Long chargingRecordId;

    @Column(name = "bill_type", length = 20)
    private String billType = "CHARGING";

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "energy_consumed", precision = 8, scale = 2)
    private BigDecimal energyConsumed;

    @Column(name = "price_per_kwh", precision = 8, scale = 4)
    private BigDecimal pricePerKwh;

    @Column(name = "service_fee", precision = 10, scale = 2)
    private BigDecimal serviceFee = BigDecimal.ZERO;

    @Column(name = "peak_energy", precision = 8, scale = 2)
    private BigDecimal peakEnergy = BigDecimal.ZERO;

    @Column(name = "valley_energy", precision = 8, scale = 2)
    private BigDecimal valleyEnergy = BigDecimal.ZERO;

    @Column(name = "flat_energy", precision = 8, scale = 2)
    private BigDecimal flatEnergy = BigDecimal.ZERO;

    @Column(name = "peak_amount", precision = 10, scale = 2)
    private BigDecimal peakAmount = BigDecimal.ZERO;

    @Column(name = "valley_amount", precision = 10, scale = 2)
    private BigDecimal valleyAmount = BigDecimal.ZERO;

    @Column(name = "flat_amount", precision = 10, scale = 2)
    private BigDecimal flatAmount = BigDecimal.ZERO;

    @Column(name = "free_minutes")
    private Integer freeMinutes = 0;

    @Column(length = 20)
    private String status = "UNPAID";

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", insertable = false, updatable = false)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "charging_record_id", insertable = false, updatable = false)
    private ChargingRecord chargingRecord;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public interface BillType {
        String CHARGING = "CHARGING";
        String SERVICE = "SERVICE";
        String OTHER = "OTHER";
    }

    public interface Status {
        String UNPAID = "UNPAID";
        String PAID = "PAID";
        String OVERDUE = "OVERDUE";
        String CANCELLED = "CANCELLED";
    }
}
