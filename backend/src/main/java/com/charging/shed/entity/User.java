package com.charging.shed.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sys_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(name = "real_name", length = 50)
    private String realName;

    @Column(unique = true, length = 20)
    private String phone;

    @Column(name = "id_card", length = 18)
    private String idCard;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(precision = 10, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(length = 20)
    private String status = "ACTIVE";

    @Column(name = "is_verified")
    private Boolean verified = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public interface Role {
        String RESIDENT = "RESIDENT";
        String PROPERTY = "PROPERTY";
        String SAFETY_OFFICER = "SAFETY_OFFICER";
    }

    public interface Status {
        String ACTIVE = "ACTIVE";
        String FROZEN = "FROZEN";
    }
}
