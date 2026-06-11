package com.charging.shed.controller;

import com.charging.shed.dto.ApiResponse;
import com.charging.shed.entity.ChargingPort;
import com.charging.shed.entity.ChargingShed;
import com.charging.shed.entity.PricingRule;
import com.charging.shed.service.PropertyService;
import com.charging.shed.service.ReservationService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
@CrossOrigin(origins = "*")
public class PublicController {

    private final PropertyService propertyService;
    private final ReservationService reservationService;

    public PublicController(PropertyService propertyService, ReservationService reservationService) {
        this.propertyService = propertyService;
        this.reservationService = reservationService;
    }

    @GetMapping("/shed/list")
    public ApiResponse<List<ChargingShed>> getShedList() {
        List<ChargingShed> sheds = propertyService.getAllSheds();
        return ApiResponse.success(sheds);
    }

    @GetMapping("/shed/{id}")
    public ApiResponse<ChargingShed> getShed(@PathVariable Long id) {
        ChargingShed shed = propertyService.getShedById(id);
        return ApiResponse.success(shed);
    }

    @GetMapping("/port/list")
    public ApiResponse<List<ChargingPort>> getPortList(@RequestParam(required = false) Long shedId) {
        List<ChargingPort> ports;
        if (shedId != null) {
            ports = propertyService.getPortsByShedId(shedId);
        } else {
            ports = propertyService.getAllPorts();
        }
        return ApiResponse.success(ports);
    }

    @GetMapping("/port/{id}")
    public ApiResponse<ChargingPort> getPort(@PathVariable Long id) {
        ChargingPort port = propertyService.getPortById(id);
        return ApiResponse.success(port);
    }

    @GetMapping("/pricing/{shedId}")
    public ApiResponse<PricingRule> getPricingRule(@PathVariable Long shedId) {
        PricingRule rule = propertyService.getApplicableRule(shedId);
        return ApiResponse.success(rule);
    }

    @GetMapping("/shed/status")
    public ApiResponse<Map<String, Object>> getShedStatus(@RequestParam(required = false) Long shedId) {
        Map<String, Object> status = new HashMap<>();
        
        List<ChargingShed> sheds = shedId != null 
            ? List.of(propertyService.getShedById(shedId))
            : propertyService.getAllSheds();
        
        int totalPorts = 0;
        int availablePorts = 0;
        int chargingPorts = 0;
        int maintenancePorts = 0;
        
        for (ChargingShed shed : sheds) {
            List<ChargingPort> ports = propertyService.getPortsByShedId(shed.getId());
            totalPorts += ports.size();
            for (ChargingPort port : ports) {
                switch (port.getStatus()) {
                    case ChargingPort.Status.AVAILABLE: availablePorts++; break;
                    case ChargingPort.Status.OCCUPIED: chargingPorts++; break;
                    case ChargingPort.Status.MAINTENANCE: maintenancePorts++; break;
                }
            }
        }
        
        status.put("sheds", sheds);
        status.put("totalPorts", totalPorts);
        status.put("availablePorts", availablePorts);
        status.put("chargingPorts", chargingPorts);
        status.put("maintenancePorts", maintenancePorts);
        
        return ApiResponse.success(status);
    }

    @GetMapping("/port/available")
    public ApiResponse<List<Long>> getAvailablePorts(
            @RequestParam("shedId") Long shedId,
            @RequestParam("startTime") LocalDateTime startTime,
            @RequestParam("endTime") LocalDateTime endTime) {
        List<Long> portIds = reservationService.getAvailablePorts(shedId, startTime, endTime);
        return ApiResponse.success(portIds);
    }

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "charging-shed-backend");
        return ApiResponse.success(health);
    }
}
