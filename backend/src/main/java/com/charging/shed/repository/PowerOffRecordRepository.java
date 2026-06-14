package com.charging.shed.repository;

import com.charging.shed.entity.PowerOffRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PowerOffRecordRepository extends JpaRepository<PowerOffRecord, Long> {

    List<PowerOffRecord> findByPortId(Long portId);

    List<PowerOffRecord> findByVehicleId(Long vehicleId);

    List<PowerOffRecord> findByStatus(String status);

    List<PowerOffRecord> findByAlertId(Long alertId);
}
