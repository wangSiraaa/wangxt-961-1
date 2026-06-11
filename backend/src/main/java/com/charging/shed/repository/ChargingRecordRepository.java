package com.charging.shed.repository;

import com.charging.shed.entity.ChargingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChargingRecordRepository extends JpaRepository<ChargingRecord, Long> {

    List<ChargingRecord> findByUserId(Long userId);

    List<ChargingRecord> findByReservationId(Long reservationId);

    List<ChargingRecord> findByPortId(Long portId);

    List<ChargingRecord> findByUserIdAndStatus(Long userId, String status);

    Optional<ChargingRecord> findByReservationIdAndStatus(Long reservationId, String status);
}
