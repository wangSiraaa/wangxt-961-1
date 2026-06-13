package com.charging.shed.controller;

import com.charging.shed.dto.ApiResponse;
import com.charging.shed.dto.ChargingStartDTO;
import com.charging.shed.dto.ChargingStopDTO;
import com.charging.shed.dto.PaymentDTO;
import com.charging.shed.entity.Billing;
import com.charging.shed.entity.ChargingRecord;
import com.charging.shed.entity.Payment;
import com.charging.shed.service.ChargingService;
import com.charging.shed.repository.PaymentRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/charging")
@CrossOrigin(origins = "*")
public class ChargingController {

    private final ChargingService chargingService;
    private final PaymentRepository paymentRepository;

    public ChargingController(ChargingService chargingService, PaymentRepository paymentRepository) {
        this.chargingService = chargingService;
        this.paymentRepository = paymentRepository;
    }

    @PostMapping("/start")
    public ApiResponse<ChargingRecord> startCharging(@RequestAttribute("userId") Long userId,
                                                     @Valid @RequestBody ChargingStartDTO dto) {
        ChargingRecord record = chargingService.startCharging(dto.getReservationId(), dto.getStartSoc());
        return ApiResponse.success(record);
    }

    @PostMapping("/stop")
    public ApiResponse<ChargingRecord> stopCharging(@RequestAttribute("userId") Long userId,
                                                    @Valid @RequestBody ChargingStopDTO dto) {
        String stopReason = dto.getStopReason() != null ? dto.getStopReason() : "用户手动停止";
        ChargingRecord record = chargingService.stopCharging(dto.getRecordId(), dto.getEndSoc(), stopReason);
        return ApiResponse.success(record);
    }

    @GetMapping("/current")
    public ApiResponse<ChargingRecord> getCurrentCharging(@RequestAttribute("userId") Long userId) {
        ChargingRecord record = chargingService.getCurrentCharging(userId);
        return ApiResponse.success(record);
    }

    @GetMapping("/records")
    public ApiResponse<List<ChargingRecord>> getChargingRecords(@RequestAttribute("userId") Long userId) {
        List<ChargingRecord> records = chargingService.getByUserId(userId);
        return ApiResponse.success(records);
    }

    @GetMapping("/record/{recordId}")
    public ApiResponse<ChargingRecord> getChargingRecord(@RequestAttribute("userId") Long userId,
                                                         @PathVariable Long recordId) {
        ChargingRecord record = chargingService.getById(recordId);
        return ApiResponse.success(record);
    }

    @GetMapping("/bills")
    public ApiResponse<List<Billing>> getBills(@RequestAttribute("userId") Long userId) {
        List<Billing> bills = chargingService.getUserBills(userId);
        return ApiResponse.success(bills);
    }

    @GetMapping("/bills/unpaid")
    public ApiResponse<List<Billing>> getUnpaidBills(@RequestAttribute("userId") Long userId) {
        List<Billing> bills = chargingService.getUserUnpaidBills(userId);
        return ApiResponse.success(bills);
    }

    @PostMapping("/pay")
    public ApiResponse<Billing> payBill(@RequestAttribute("userId") Long userId,
                                        @Valid @RequestBody PaymentDTO dto) {
        Billing billing = chargingService.payBill(userId, dto.getBillingId(),
                dto.getPaymentMethod(), dto.getAmount());
        return ApiResponse.success(billing);
    }

    @GetMapping("/payment/{billingId}")
    public ApiResponse<Payment> getPaymentByBillingId(@RequestAttribute("userId") Long userId,
                                                      @PathVariable Long billingId) {
        List<Payment> payments = paymentRepository.findByBillingId(billingId);
        if (payments.isEmpty()) {
            return ApiResponse.success(null);
        }
        Payment payment = payments.get(0);
        if (!payment.getUserId().equals(userId)) {
            return ApiResponse.success(null);
        }
        return ApiResponse.success(payment);
    }
}
