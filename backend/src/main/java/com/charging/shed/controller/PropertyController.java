package com.charging.shed.controller;

import com.charging.shed.dto.ApiResponse;
import com.charging.shed.dto.ChargingPortDTO;
import com.charging.shed.dto.ChargingShedDTO;
import com.charging.shed.dto.PricingRuleDTO;
import com.charging.shed.entity.*;
import com.charging.shed.exception.BusinessException;
import com.charging.shed.service.PropertyService;
import com.charging.shed.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/property")
@CrossOrigin(origins = "*")
public class PropertyController {

    private final PropertyService propertyService;
    private final SecurityUtil securityUtil;

    public PropertyController(PropertyService propertyService, SecurityUtil securityUtil) {
        this.propertyService = propertyService;
        this.securityUtil = securityUtil;
    }

    private void checkPropertyRole() {
        String role = securityUtil.getCurrentRole();
        if (!User.Role.PROPERTY.equals(role)) {
            throw new BusinessException("只有物业人员可以执行此操作");
        }
    }

    @PostMapping("/shed")
    public ApiResponse<ChargingShed> createShed(@Valid @RequestBody ChargingShedDTO dto) {
        checkPropertyRole();
        ChargingShed shed = propertyService.createShed(
                dto.getName(), dto.getLocation(), dto.getTotalPorts(),
                dto.getManagerId() != null ? dto.getManagerId() : securityUtil.getCurrentUserId()
        );
        return ApiResponse.success(shed);
    }

    @PutMapping("/shed/{id}")
    public ApiResponse<ChargingShed> updateShed(@PathVariable Long id, @Valid @RequestBody ChargingShedDTO dto) {
        checkPropertyRole();
        ChargingShed shed = propertyService.updateShed(
                id, dto.getName(), dto.getLocation(), dto.getTotalPorts(), dto.getStatus()
        );
        return ApiResponse.success(shed);
    }

    @DeleteMapping("/shed/{id}")
    public ApiResponse<Void> deleteShed(@PathVariable Long id) {
        checkPropertyRole();
        propertyService.deleteShed(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/shed/list")
    public ApiResponse<List<ChargingShed>> getShedList() {
        List<ChargingShed> sheds = propertyService.getAllSheds();
        return ApiResponse.success(sheds);
    }

    @PostMapping("/port")
    public ApiResponse<ChargingPort> createPort(@Valid @RequestBody ChargingPortDTO dto) {
        checkPropertyRole();
        ChargingPort port = propertyService.createPort(
                dto.getShedId(), dto.getPortNumber(), dto.getPortType(),
                dto.getPowerRating()
        );
        return ApiResponse.success(port);
    }

    @PutMapping("/port/{id}")
    public ApiResponse<ChargingPort> updatePort(@PathVariable Long id, @Valid @RequestBody ChargingPortDTO dto) {
        checkPropertyRole();
        ChargingPort port = propertyService.updatePort(
                id, dto.getShedId(), dto.getPortNumber(), dto.getPortType(),
                dto.getPowerRating(), dto.getStatus()
        );
        return ApiResponse.success(port);
    }

    @DeleteMapping("/port/{id}")
    public ApiResponse<Void> deletePort(@PathVariable Long id) {
        checkPropertyRole();
        propertyService.deletePort(id);
        return ApiResponse.success(null);
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

    @PostMapping("/pricing")
    public ApiResponse<PricingRule> createPricingRule(@Valid @RequestBody PricingRuleDTO dto) {
        checkPropertyRole();
        PricingRule rule = propertyService.createPricingRule(
                dto.getShedId(), dto.getPricePerKwh(), dto.getServiceFee(),
                dto.getPeakStartTime(), dto.getPeakEndTime(), dto.getPeakPriceMultiplier(),
                dto.getValleyStartTime(), dto.getValleyEndTime(), dto.getValleyPriceMultiplier(),
                dto.getIsActive(), dto.getCommunityId(), dto.getFreeMinutes(), dto.getFlatPriceMultiplier()
        );
        return ApiResponse.success(rule);
    }

    @PutMapping("/pricing/{id}")
    public ApiResponse<PricingRule> updatePricingRule(@PathVariable Long id, @Valid @RequestBody PricingRuleDTO dto) {
        checkPropertyRole();
        PricingRule rule = propertyService.updatePricingRule(
                id, dto.getShedId(), dto.getPricePerKwh(), dto.getServiceFee(),
                dto.getPeakStartTime(), dto.getPeakEndTime(), dto.getPeakPriceMultiplier(),
                dto.getValleyStartTime(), dto.getValleyEndTime(), dto.getValleyPriceMultiplier(),
                dto.getIsActive(), dto.getCommunityId(), dto.getFreeMinutes(), dto.getFlatPriceMultiplier()
        );
        return ApiResponse.success(rule);
    }

    @DeleteMapping("/pricing/{id}")
    public ApiResponse<Void> deletePricingRule(@PathVariable Long id) {
        checkPropertyRole();
        propertyService.deletePricingRule(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/pricing/list")
    public ApiResponse<List<PricingRule>> getPricingRuleList(@RequestParam(required = false) Long shedId) {
        List<PricingRule> rules;
        if (shedId != null) {
            rules = propertyService.getPricingRulesByShedId(shedId);
        } else {
            rules = propertyService.getAllPricingRules();
        }
        return ApiResponse.success(rules);
    }

    @GetMapping("/billing/list")
    public ApiResponse<List<Billing>> getAllBills(@RequestParam(required = false) String status) {
        checkPropertyRole();
        List<Billing> bills = propertyService.getAllBills(status);
        return ApiResponse.success(bills);
    }

    @GetMapping("/blacklist/list")
    public ApiResponse<List<BatteryBlacklist>> getBlacklist() {
        checkPropertyRole();
        return ApiResponse.success(propertyService.getAllBlacklistBrands());
    }

    @PostMapping("/blacklist/add")
    public ApiResponse<BatteryBlacklist> addBlacklistBrand(@RequestBody java.util.Map<String, String> body) {
        checkPropertyRole();
        Long operatorId = securityUtil.getCurrentUserId();
        BatteryBlacklist entry = propertyService.addBlacklistBrand(
                body.get("brandName"), body.get("banReason"), operatorId);
        return ApiResponse.success(entry);
    }

    @DeleteMapping("/blacklist/{id}")
    public ApiResponse<Void> removeBlacklistBrand(@PathVariable Long id) {
        checkPropertyRole();
        propertyService.removeBlacklistBrand(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/shed/realtime/{shedId}")
    public ApiResponse<java.util.Map<String, Object>> getShedRealtimeStatus(@PathVariable Long shedId) {
        ChargingShed shed = propertyService.getShedById(shedId);
        List<ChargingPort> ports = propertyService.getPortsByShedId(shedId);

        java.util.Map<String, Object> status = new java.util.HashMap<>();
        status.put("shed", shed);
        status.put("ports", ports);
        status.put("availablePorts", ports.stream()
                .filter(p -> ChargingPort.Status.AVAILABLE.equals(p.getStatus())).count());
        status.put("occupiedPorts", ports.stream()
                .filter(p -> ChargingPort.Status.OCCUPIED.equals(p.getStatus())).count());
        status.put("faultPorts", ports.stream()
                .filter(p -> ChargingPort.Status.FAULT.equals(p.getStatus())).count());
        status.put("highTempPorts", ports.stream()
                .filter(p -> p.getCurrentTemperature() != null && p.getCurrentTemperature()
                        .compareTo(new java.math.BigDecimal("55")) >= 0).count());
        status.put("currentPower", shed.getCurrentTotalPower());
        status.put("maxPower", shed.getMaxPowerLimit());
        status.put("powerUsage", shed.getMaxPowerLimit().compareTo(java.math.BigDecimal.ZERO) > 0
                ? shed.getCurrentTotalPower().divide(shed.getMaxPowerLimit(), 4, java.math.RoundingMode.HALF_UP)
                        .multiply(new java.math.BigDecimal("100"))
                : java.math.BigDecimal.ZERO);

        return ApiResponse.success(status);
    }
}
