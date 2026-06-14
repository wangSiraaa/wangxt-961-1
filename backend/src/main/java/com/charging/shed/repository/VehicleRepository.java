package com.charging.shed.repository;

import com.charging.shed.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByUserId(Long userId);

    Optional<Vehicle> findByPlateNumber(String plateNumber);

    List<Vehicle> findByUserIdAndVerified(Long userId, Boolean verified);

    boolean existsByPlateNumber(String plateNumber);

    List<Vehicle> findByStatus(String status);
}
