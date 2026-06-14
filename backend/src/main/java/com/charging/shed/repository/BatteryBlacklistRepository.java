package com.charging.shed.repository;

import com.charging.shed.entity.BatteryBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BatteryBlacklistRepository extends JpaRepository<BatteryBlacklist, Long> {

    List<BatteryBlacklist> findByStatus(String status);

    Optional<BatteryBlacklist> findByBrandName(String brandName);

    boolean existsByBrandNameAndStatus(String brandName, String status);
}
