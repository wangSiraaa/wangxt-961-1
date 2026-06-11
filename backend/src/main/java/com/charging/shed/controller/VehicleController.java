package com.charging.shed.controller;

import com.charging.shed.dto.ApiResponse;
import com.charging.shed.dto.VehicleBindDTO;
import com.charging.shed.entity.Vehicle;
import com.charging.shed.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicle")
@CrossOrigin(origins = "*")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PostMapping("/bind")
    public ApiResponse<Vehicle> bindVehicle(@RequestAttribute("userId") Long userId,
                                            @Valid @RequestBody VehicleBindDTO vehicleBindDTO) {
        Vehicle vehicle = vehicleService.bindVehicle(userId,
                vehicleBindDTO.getPlateNumber(),
                vehicleBindDTO.getBrand(),
                vehicleBindDTO.getModel(),
                vehicleBindDTO.getBatteryCapacity(),
                vehicleBindDTO.getVin());
        return ApiResponse.success(vehicle);
    }

    @PostMapping("/verify/{vehicleId}")
    public ApiResponse<Vehicle> verifyVehicle(@RequestAttribute("userId") Long userId,
                                              @PathVariable Long vehicleId) {
        Vehicle vehicle = vehicleService.verifyVehicle(userId, vehicleId);
        return ApiResponse.success(vehicle);
    }

    @DeleteMapping("/unbind/{vehicleId}")
    public ApiResponse<Void> unbindVehicle(@RequestAttribute("userId") Long userId,
                                           @PathVariable Long vehicleId) {
        vehicleService.unbindVehicle(userId, vehicleId);
        return ApiResponse.success(null);
    }

    @GetMapping("/list")
    public ApiResponse<List<Vehicle>> getVehicles(@RequestAttribute("userId") Long userId) {
        List<Vehicle> vehicles = vehicleService.getVehicles(userId);
        return ApiResponse.success(vehicles);
    }

    @GetMapping("/{vehicleId}")
    public ApiResponse<Vehicle> getVehicleById(@RequestAttribute("userId") Long userId,
                                               @PathVariable Long vehicleId) {
        Vehicle vehicle = vehicleService.getVehicleById(userId, vehicleId);
        return ApiResponse.success(vehicle);
    }
}
