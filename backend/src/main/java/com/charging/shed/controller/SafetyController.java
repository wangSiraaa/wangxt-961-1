package com.charging.shed.controller;

import com.charging.shed.dto.*;
import com.charging.shed.entity.PowerOffRecord;
import com.charging.shed.entity.ReviewRecord;
import com.charging.shed.entity.SafetyAlert;
import com.charging.shed.entity.User;
import com.charging.shed.entity.Vehicle;
import com.charging.shed.exception.BusinessException;
import com.charging.shed.repository.VehicleRepository;
import com.charging.shed.service.SafetyService;
import com.charging.shed.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/safety")
@CrossOrigin(origins = "*")
public class SafetyController {

    private final SafetyService safetyService;
    private final VehicleRepository vehicleRepository;
    private final SecurityUtil securityUtil;

    public SafetyController(SafetyService safetyService,
                            VehicleRepository vehicleRepository,
                            SecurityUtil securityUtil) {
        this.safetyService = safetyService;
        this.vehicleRepository = vehicleRepository;
        this.securityUtil = securityUtil;
    }

    private void checkSafetyRole() {
        String role = securityUtil.getCurrentRole();
        if (!User.Role.SAFETY_OFFICER.equals(role) && !User.Role.PROPERTY.equals(role)) {
            throw new BusinessException("只有安全员或物业人员可以执行此操作");
        }
    }

    @PostMapping("/temperature/report")
    public ApiResponse<SafetyAlert> reportTemperature(@Valid @RequestBody TemperatureReportDTO dto) {
        SafetyAlert alert = safetyService.reportTemperatureAlert(
                dto.getPortId(), dto.getTemperature(), dto.getReservationId()
        );
        return ApiResponse.success(alert);
    }

    @PostMapping("/smoke/report")
    public ApiResponse<SafetyAlert> reportSmoke(@Valid @RequestBody SmokeReportDTO dto) {
        SafetyAlert alert = safetyService.reportSmokeAlert(
                dto.getPortId(), dto.getSmokeLevel(), dto.getReservationId()
        );
        return ApiResponse.success(alert);
    }

    @PostMapping("/alert/handle/{alertId}")
    public ApiResponse<SafetyAlert> handleAlert(@PathVariable Long alertId,
                                                @Valid @RequestBody AlertHandleDTO dto) {
        checkSafetyRole();
        Long officerId = securityUtil.getCurrentUserId();
        SafetyAlert alert = safetyService.handleAlert(
                alertId, officerId, dto.getHandleResult(), dto.getRemark()
        );
        return ApiResponse.success(alert);
    }

    @PostMapping("/alert/resolve/{alertId}")
    public ApiResponse<SafetyAlert> resolveAlert(@PathVariable Long alertId,
                                                 @RequestBody AlertResolveDTO dto) {
        checkSafetyRole();
        Long officerId = securityUtil.getCurrentUserId();
        SafetyAlert alert = safetyService.resolveAlert(
                alertId, officerId, dto != null ? dto.getRemark() : null
        );
        return ApiResponse.success(alert);
    }

    @GetMapping("/alert/list")
    public ApiResponse<List<SafetyAlert>> getAlertList(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String alertType) {
        checkSafetyRole();
        List<SafetyAlert> alerts = safetyService.getAlerts(status, alertType);
        return ApiResponse.success(alerts);
    }

    @GetMapping("/alert/{alertId}")
    public ApiResponse<SafetyAlert> getAlert(@PathVariable Long alertId) {
        checkSafetyRole();
        SafetyAlert alert = safetyService.getAlertById(alertId);
        return ApiResponse.success(alert);
    }

    @PostMapping("/power/off/{portId}")
    public ApiResponse<Void> powerOff(@PathVariable Long portId) {
        checkSafetyRole();
        Long officerId = securityUtil.getCurrentUserId();
        safetyService.manualPowerOff(portId, officerId, null);
        return ApiResponse.success(null);
    }

    @PostMapping("/power/on/{portId}")
    public ApiResponse<Void> powerOn(@PathVariable Long portId) {
        checkSafetyRole();
        Long officerId = securityUtil.getCurrentUserId();
        safetyService.manualPowerOn(portId, officerId);
        return ApiResponse.success(null);
    }

    @GetMapping("/power-off-records/{portId}")
    public ApiResponse<List<PowerOffRecord>> getPowerOffRecords(@PathVariable Long portId) {
        checkSafetyRole();
        List<PowerOffRecord> records = safetyService.getPowerOffRecords(portId);
        return ApiResponse.success(records);
    }

    @PostMapping("/review/unfreeze")
    public ApiResponse<ReviewRecord> reviewUnfreeze(@Valid @RequestBody ReviewDTO dto) {
        checkSafetyRole();
        Long reviewerId = securityUtil.getCurrentUserId();
        ReviewRecord record = safetyService.reviewVehicleUnfreeze(
                dto.getVehicleId(), reviewerId, dto.getReviewResult(), dto.getRemark()
        );
        return ApiResponse.success(record);
    }

    @PostMapping("/review/resume")
    public ApiResponse<ReviewRecord> reviewChargeResume(@Valid @RequestBody ReviewDTO dto) {
        checkSafetyRole();
        Long reviewerId = securityUtil.getCurrentUserId();
        ReviewRecord record = safetyService.reviewChargeResume(
                dto.getVehicleId(), dto.getPowerOffId(), reviewerId, dto.getReviewResult(), dto.getRemark()
        );
        return ApiResponse.success(record);
    }

    @GetMapping("/review-records/{vehicleId}")
    public ApiResponse<List<ReviewRecord>> getReviewRecords(@PathVariable Long vehicleId) {
        checkSafetyRole();
        List<ReviewRecord> records = safetyService.getReviewRecords(vehicleId);
        return ApiResponse.success(records);
    }

    @GetMapping("/frozen-vehicles")
    public ApiResponse<List<Vehicle>> getFrozenVehicles() {
        checkSafetyRole();
        List<Vehicle> vehicles = vehicleRepository.findByStatus(Vehicle.Status.FROZEN);
        return ApiResponse.success(vehicles);
    }
}
