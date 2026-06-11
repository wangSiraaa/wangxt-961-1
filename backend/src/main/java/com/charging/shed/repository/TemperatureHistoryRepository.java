package com.charging.shed.repository;

import com.charging.shed.entity.TemperatureHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TemperatureHistoryRepository extends JpaRepository<TemperatureHistory, Long> {

    List<TemperatureHistory> findByPortIdOrderByRecordedAtDesc(Long portId);

    List<TemperatureHistory> findByReservationIdOrderByRecordedAtDesc(Long reservationId);

    @Query("SELECT t FROM TemperatureHistory t WHERE t.portId = :portId AND t.recordedAt >= :startTime ORDER BY t.recordedAt")
    List<TemperatureHistory> findByPortIdAndTimeRange(@Param("portId") Long portId, @Param("startTime") LocalDateTime startTime);

    @Query("SELECT t FROM TemperatureHistory t WHERE t.reservationId = :reservationId ORDER BY t.recordedAt")
    List<TemperatureHistory> findByReservationIdOrdered(Long reservationId);
}
