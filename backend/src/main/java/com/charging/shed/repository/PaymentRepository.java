package com.charging.shed.repository;

import com.charging.shed.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByUserId(Long userId);

    List<Payment> findByBillingId(Long billingId);

    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);
}
