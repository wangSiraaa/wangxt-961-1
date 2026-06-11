package com.charging.shed.repository;

import com.charging.shed.entity.TemperatureAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TemperatureAlertRepository extends JpaRepository<TemperatureAlert, Long> {

    List<TemperatureAlert> findByStatus(String status);

    List<TemperatureAlert> findByPortId(Long portId);

    List<TemperatureAlert> findByReservationId(Long reservationId);

    List<TemperatureAlert> findByHandledBy(Long handledBy);

    List<TemperatureAlert> findByStatusAndAlertLevel(String status, String alertLevel);

    List<TemperatureAlert> findByAlertLevel(String alertLevel);

    List<TemperatureAlert> findAllByOrderByCreatedAtDesc();
}
