package com.charging.shed.repository;

import com.charging.shed.entity.SafetyAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SafetyAlertRepository extends JpaRepository<SafetyAlert, Long> {

    List<SafetyAlert> findByStatus(String status);

    List<SafetyAlert> findByAlertType(String alertType);

    List<SafetyAlert> findByStatusAndAlertType(String status, String alertType);

    List<SafetyAlert> findByPortId(Long portId);

    List<SafetyAlert> findByVehicleId(Long vehicleId);

    @Query("SELECT sa FROM SafetyAlert sa WHERE sa.status = :status ORDER BY sa.createdAt DESC")
    List<SafetyAlert> findByStatusOrderByCreatedAtDesc(@Param("status") String status);

    List<SafetyAlert> findAllByOrderByCreatedAtDesc();

    List<SafetyAlert> findByStatusAndAlertLevel(String status, String alertLevel);
}
