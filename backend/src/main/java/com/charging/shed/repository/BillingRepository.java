package com.charging.shed.repository;

import com.charging.shed.entity.Billing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface BillingRepository extends JpaRepository<Billing, Long> {

    List<Billing> findByUserId(Long userId);

    List<Billing> findByUserIdAndStatus(Long userId, String status);

    List<Billing> findByStatus(String status);

    List<Billing> findByReservationId(Long reservationId);

    @Query("SELECT COALESCE(SUM(b.amount), 0) FROM Billing b WHERE b.userId = :userId AND b.status = 'UNPAID'")
    BigDecimal getUnpaidAmount(@Param("userId") Long userId);

    @Query("SELECT COUNT(b) > 0 FROM Billing b WHERE b.userId = :userId AND b.status = 'UNPAID'")
    boolean hasUnpaidBills(@Param("userId") Long userId);
}
