package com.charging.shed.service;

import com.charging.shed.entity.Vehicle;
import com.charging.shed.entity.User;
import com.charging.shed.exception.BusinessException;
import com.charging.shed.repository.VehicleRepository;
import com.charging.shed.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    public VehicleService(VehicleRepository vehicleRepository, UserRepository userRepository) {
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
    }

    public List<Vehicle> getByUserId(Long userId) {
        return vehicleRepository.findByUserId(userId);
    }

    public List<Vehicle> getVerifiedVehicles(Long userId) {
        return vehicleRepository.findByUserIdAndVerified(userId, true);
    }

    public Vehicle getById(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("车辆不存在"));
    }

    @Transactional
    public Vehicle bindVehicle(Long userId, String plateNumber, String brand, String model,
                               java.math.BigDecimal batteryCapacity, String vin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (!user.getVerified()) {
            throw new BusinessException("请先完成实名认证后再绑定车辆");
        }

        if (vehicleRepository.existsByPlateNumber(plateNumber)) {
            throw new BusinessException("该车牌号已被绑定");
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setUserId(userId);
        vehicle.setPlateNumber(plateNumber);
        vehicle.setVehicleType(brand + " " + model);
        vehicle.setBatteryCapacity(batteryCapacity);
        vehicle.setVerified(true);
        vehicle.setStatus(Vehicle.Status.NORMAL);

        return vehicleRepository.save(vehicle);
    }

    @Transactional
    public Vehicle verifyVehicle(Long userId, Long vehicleId) {
        Vehicle vehicle = getById(vehicleId);

        if (!vehicle.getUserId().equals(userId)) {
            throw new BusinessException("无权认证他人车辆");
        }

        if (vehicle.getVerified()) {
            throw new BusinessException("车辆已完成实名认证");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (!user.getVerified()) {
            throw new BusinessException("用户未完成实名认证，请先完成实名认证");
        }

        vehicle.setVerified(true);
        return vehicleRepository.save(vehicle);
    }

    public List<Vehicle> getVehicles(Long userId) {
        return getByUserId(userId);
    }

    public Vehicle getVehicleById(Long userId, Long vehicleId) {
        Vehicle vehicle = getById(vehicleId);
        if (!vehicle.getUserId().equals(userId)) {
            throw new BusinessException("无权查看他人车辆");
        }
        return vehicle;
    }

    @Transactional
    public void unbindVehicle(Long userId, Long vehicleId) {
        Vehicle vehicle = getById(vehicleId);

        if (!vehicle.getUserId().equals(userId)) {
            throw new BusinessException("无权解绑他人车辆");
        }

        vehicleRepository.delete(vehicle);
    }

    public boolean canVehicleCharge(Long vehicleId) {
        Vehicle vehicle = getById(vehicleId);
        return vehicle.getVerified() && Vehicle.Status.NORMAL.equals(vehicle.getStatus());
    }
}
