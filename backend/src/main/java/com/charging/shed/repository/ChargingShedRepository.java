package com.charging.shed.repository;

import com.charging.shed.entity.ChargingShed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChargingShedRepository extends JpaRepository<ChargingShed, Long> {

    List<ChargingShed> findByStatus(String status);

    List<ChargingShed> findByManagerId(Long managerId);
}
