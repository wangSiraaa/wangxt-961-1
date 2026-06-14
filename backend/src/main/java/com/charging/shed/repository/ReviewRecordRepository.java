package com.charging.shed.repository;

import com.charging.shed.entity.ReviewRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRecordRepository extends JpaRepository<ReviewRecord, Long> {

    List<ReviewRecord> findByVehicleId(Long vehicleId);

    List<ReviewRecord> findByReviewResult(String reviewResult);

    List<ReviewRecord> findByReviewerId(Long reviewerId);
}
