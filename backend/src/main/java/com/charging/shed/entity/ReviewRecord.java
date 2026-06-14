package com.charging.shed.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "review_record")
public class ReviewRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "alert_id")
    private Long alertId;

    @Column(name = "power_off_id")
    private Long powerOffId;

    @Column(name = "review_type", nullable = false, length = 30)
    private String reviewType;

    @Column(name = "review_result", nullable = false, length = 20)
    private String reviewResult;

    @Column(name = "reviewer_id", nullable = false)
    private Long reviewerId;

    @Column(name = "review_remark", length = 500)
    private String reviewRemark;

    @Column(name = "reviewed_at", nullable = false)
    private LocalDateTime reviewedAt;

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
    @JoinColumn(name = "power_off_id", insertable = false, updatable = false)
    private PowerOffRecord powerOff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", insertable = false, updatable = false)
    private User reviewer;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public interface ReviewType {
        String VEHICLE_UNFREEZE = "VEHICLE_UNFREEZE";
        String CHARGE_RESUME = "CHARGE_RESUME";
    }

    public interface ReviewResult {
        String APPROVED = "APPROVED";
        String REJECTED = "REJECTED";
    }
}
