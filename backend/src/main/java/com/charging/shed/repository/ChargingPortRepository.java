package com.charging.shed.repository;

import com.charging.shed.entity.ChargingPort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChargingPortRepository extends JpaRepository<ChargingPort, Long> {

    List<ChargingPort> findByShedId(Long shedId);

    List<ChargingPort> findByShedIdAndStatus(Long shedId, String status);

    Optional<ChargingPort> findByShedIdAndPortCode(Long shedId, String portCode);

    List<ChargingPort> findByStatus(String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ChargingPort p WHERE p.id = :id")
    Optional<ChargingPort> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT p FROM ChargingPort p WHERE p.currentTemperature > :threshold AND p.powerOn = true")
    List<ChargingPort> findPortsWithHighTemperature(@Param("threshold") java.math.BigDecimal threshold);
}
