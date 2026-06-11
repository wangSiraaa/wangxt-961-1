package com.charging.shed.repository;

import com.charging.shed.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByUserId(Long userId);

    List<Reservation> findByUserIdAndStatus(Long userId, String status);

    List<Reservation> findByPortId(Long portId);

    List<Reservation> findByPortIdAndStatus(Long portId, String status);

    @Query("SELECT r FROM Reservation r WHERE r.portId = :portId AND r.status IN :statuses " +
           "AND ((r.reserveStartTime <= :endTime AND r.reserveEndTime >= :startTime))")
    List<Reservation> findConflictingReservations(
            @Param("portId") Long portId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("statuses") List<String> statuses
    );

    @Query("SELECT r FROM Reservation r WHERE r.shedId = :shedId AND r.status IN :statuses " +
           "AND ((r.reserveStartTime <= :endTime AND r.reserveEndTime >= :startTime))")
    List<Reservation> findConflictingReservationsByShed(
            @Param("shedId") Long shedId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("statuses") List<String> statuses
    );

    @Query("SELECT r FROM Reservation r WHERE r.status = :status AND r.reserveEndTime < :now")
    List<Reservation> findExpiredReservations(@Param("status") String status, @Param("now") LocalDateTime now);

    List<Reservation> findByStatus(String status);
}
