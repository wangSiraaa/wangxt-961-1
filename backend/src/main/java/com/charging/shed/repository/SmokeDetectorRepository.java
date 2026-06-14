package com.charging.shed.repository;

import com.charging.shed.entity.SmokeDetector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SmokeDetectorRepository extends JpaRepository<SmokeDetector, Long> {

    List<SmokeDetector> findByPortId(Long portId);

    List<SmokeDetector> findByStatus(String status);

    SmokeDetector findByDeviceCode(String deviceCode);
}
