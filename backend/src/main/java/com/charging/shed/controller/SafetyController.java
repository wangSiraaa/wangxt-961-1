package com.charging.shed.controller;

import com.charging.shed.dto.AlertHandleDTO;
import com.charging.shed.dto.ApiResponse;
import com.charging.shed.dto.TemperatureReportDTO;
import com.charging.shed.entity.TemperatureAlert;
import com.charging.shed.entity.User;
import com.charging.shed.exception.BusinessException;
import com.charging.shed.service.TemperatureMonitorService;
import com.charging.shed.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/safety")
@CrossOrigin(origins = "*")
public class SafetyController {

    private final TemperatureMonitorService temperatureMonitorService;
    private final SecurityUtil securityUtil;

    public SafetyController(TemperatureMonitorService temperatureMonitorService, SecurityUtil securityUtil) {
        this.temperatureMonitorService = temperatureMonitorService;
        this.securityUtil = securityUtil;
    }

    private void checkSafetyRole() {
        String role = securityUtil.getCurrentRole();
        if (!User.Role.SAFETY_OFFICER.equals(role) && !User.Role.PROPERTY.equals(role)) {
            throw new BusinessException("只有安全员或物业人员可以执行此操作");
        }
    }

    @PostMapping("/temperature/report")
    public ApiResponse<TemperatureAlert> reportTemperature(@Valid @RequestBody TemperatureReportDTO dto) {
        TemperatureAlert alert = temperatureMonitorService.reportTemperature(
                dto.getPortId(), dto.getTemperature(), dto.getReservationId()
        );
        return ApiResponse.success(alert);
    }

    @PostMapping("/alert/handle/{alertId}")
    public ApiResponse<TemperatureAlert> handleAlert(@PathVariable Long alertId,
                                                     @Valid @RequestBody AlertHandleDTO dto) {
        checkSafetyRole();
        Long officerId = securityUtil.getCurrentUserId();
        TemperatureAlert alert = temperatureMonitorService.handleAlert(
                alertId, officerId, dto.getHandleResult(), dto.getRemark()
        );
        return ApiResponse.success(alert);
    }

    @GetMapping("/alert/list")
    public ApiResponse<List<TemperatureAlert>> getAlertList(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String level) {
        checkSafetyRole();
        List<TemperatureAlert> alerts = temperatureMonitorService.getAlerts(status, level);
        return ApiResponse.success(alerts);
    }

    @GetMapping("/alert/{alertId}")
    public ApiResponse<TemperatureAlert> getAlert(@PathVariable Long alertId) {
        checkSafetyRole();
        TemperatureAlert alert = temperatureMonitorService.getAlertById(alertId);
        return ApiResponse.success(alert);
    }

    @PostMapping("/power/off/{portId}")
    public ApiResponse<Void> powerOff(@PathVariable Long portId) {
        checkSafetyRole();
        Long officerId = securityUtil.getCurrentUserId();
        temperatureMonitorService.manualPowerOff(portId, officerId);
        return ApiResponse.success(null);
    }

    @PostMapping("/power/on/{portId}")
    public ApiResponse<Void> powerOn(@PathVariable Long portId) {
        checkSafetyRole();
        Long officerId = securityUtil.getCurrentUserId();
        temperatureMonitorService.manualPowerOn(portId, officerId);
        return ApiResponse.success(null);
    }

    @GetMapping("/high-temperature")
    public ApiResponse<List<TemperatureAlert>> getHighTemperatureAlerts() {
        checkSafetyRole();
        List<TemperatureAlert> alerts = temperatureMonitorService.getHighTemperatureAlerts();
        return ApiResponse.success(alerts);
    }
}
