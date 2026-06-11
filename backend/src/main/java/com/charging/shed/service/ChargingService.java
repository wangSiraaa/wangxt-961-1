package com.charging.shed.service;

import com.charging.shed.entity.*;
import com.charging.shed.exception.BusinessException;
import com.charging.shed.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class ChargingService {

    private final ChargingRecordRepository chargingRecordRepository;
    private final ReservationService reservationService;
    private final PropertyService propertyService;
    private final BillingRepository billingRepository;
    private final UserRepository userRepository;
    private final TemperatureHistoryRepository temperatureHistoryRepository;
    private final PaymentRepository paymentRepository;

    public ChargingService(ChargingRecordRepository chargingRecordRepository,
                           ReservationService reservationService,
                           PropertyService propertyService,
                           BillingRepository billingRepository,
                           UserRepository userRepository,
                           TemperatureHistoryRepository temperatureHistoryRepository,
                           PaymentRepository paymentRepository) {
        this.chargingRecordRepository = chargingRecordRepository;
        this.reservationService = reservationService;
        this.propertyService = propertyService;
        this.billingRepository = billingRepository;
        this.userRepository = userRepository;
        this.temperatureHistoryRepository = temperatureHistoryRepository;
        this.paymentRepository = paymentRepository;
    }

    public List<ChargingRecord> getByUserId(Long userId) {
        return chargingRecordRepository.findByUserId(userId);
    }

    public ChargingRecord getById(Long id) {
        return chargingRecordRepository.findById(id)
                .orElseThrow(() -> new BusinessException("充电记录不存在"));
    }

    public ChargingRecord getCurrentCharging(Long userId) {
        List<ChargingRecord> records = chargingRecordRepository.findByUserIdAndStatus(
                userId, ChargingRecord.Status.CHARGING);
        return records.isEmpty() ? null : records.get(0);
    }

    @Transactional
    public ChargingRecord startCharging(Long reservationId, BigDecimal startSoc) {
        Reservation reservation = reservationService.startCharging(reservationId);

        ChargingRecord record = new ChargingRecord();
        record.setReservationId(reservationId);
        record.setPortId(reservation.getPortId());
        record.setUserId(reservation.getUserId());
        record.setVehicleId(reservation.getVehicleId());
        record.setStartTime(LocalDateTime.now());
        record.setStartSoc(startSoc);
        record.setStatus(ChargingRecord.Status.CHARGING);

        return chargingRecordRepository.save(record);
    }

    @Transactional
    public ChargingRecord stopCharging(Long recordId, BigDecimal endSoc, String stopReason) {
        ChargingRecord record = getById(recordId);

        if (!ChargingRecord.Status.CHARGING.equals(record.getStatus())) {
            throw new BusinessException("充电未进行中");
        }

        record.setEndTime(LocalDateTime.now());
        record.setEndSoc(endSoc);
        record.setStatus(ChargingRecord.Status.COMPLETED);
        record.setStopReason(stopReason);

        long minutes = java.time.Duration.between(record.getStartTime(), record.getEndTime()).toMinutes();
        BigDecimal hours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 4, java.math.RoundingMode.HALF_UP);

        ChargingPort port = propertyService.getPortById(record.getPortId());
        BigDecimal energyConsumed = port.getPowerRating().multiply(hours);
        record.setEnergyConsumed(energyConsumed);

        List<TemperatureHistory> tempHistory = temperatureHistoryRepository
                .findByReservationIdOrdered(record.getReservationId());
        if (!tempHistory.isEmpty()) {
            BigDecimal maxTemp = tempHistory.stream()
                    .map(TemperatureHistory::getTemperature)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            BigDecimal avgTemp = tempHistory.stream()
                    .map(TemperatureHistory::getTemperature)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(tempHistory.size()), 2, java.math.RoundingMode.HALF_UP);
            record.setMaxTemperature(maxTemp);
            record.setAvgTemperature(avgTemp);
        }

        record = chargingRecordRepository.save(record);

        generateBilling(record);

        reservationService.completeCharging(record.getReservationId());

        return record;
    }

    @Transactional
    public Billing generateBilling(ChargingRecord record) {
        PricingRule rule = propertyService.getApplicableRule(
                propertyService.getPortById(record.getPortId()).getShedId());

        BigDecimal totalPrice = calculateChargingPrice(record, rule);

        BigDecimal serviceFee = rule.getServiceFee().multiply(record.getEnergyConsumed());
        BigDecimal totalAmount = totalPrice.add(serviceFee);

        Billing billing = new Billing();
        billing.setUserId(record.getUserId());
        billing.setReservationId(record.getReservationId());
        billing.setChargingRecordId(record.getId());
        billing.setBillType(Billing.BillType.CHARGING);
        billing.setAmount(totalAmount);
        billing.setEnergyConsumed(record.getEnergyConsumed());
        billing.setPricePerKwh(rule.getPricePerKwh());
        billing.setServiceFee(serviceFee);
        billing.setStatus(Billing.Status.UNPAID);
        billing.setDueDate(LocalDateTime.now().plusDays(7).toLocalDate());

        billing = billingRepository.save(billing);

        User user = userRepository.findById(record.getUserId()).orElseThrow();
        user.setBalance(user.getBalance().subtract(totalAmount));
        userRepository.save(user);

        return billing;
    }

    public BigDecimal calculateChargingPrice(ChargingRecord record, PricingRule rule) {
        BigDecimal energy = record.getEnergyConsumed();
        LocalTime startTime = record.getStartTime().toLocalTime();
        LocalTime endTime = record.getEndTime() != null
                ? record.getEndTime().toLocalTime()
                : LocalTime.now();

        BigDecimal multiplier = BigDecimal.ONE;

        if (rule.getPeakStartTime() != null && rule.getPeakEndTime() != null) {
            if (!startTime.isBefore(rule.getPeakStartTime()) && !endTime.isAfter(rule.getPeakEndTime())) {
                multiplier = rule.getPeakPriceMultiplier();
            }
        }

        if (rule.getValleyStartTime() != null && rule.getValleyEndTime() != null) {
            if (!startTime.isBefore(rule.getValleyStartTime()) && !endTime.isAfter(rule.getValleyEndTime())) {
                multiplier = rule.getValleyPriceMultiplier();
            }
        }

        return rule.getPricePerKwh().multiply(energy).multiply(multiplier);
    }

    @Transactional
    public Billing payBill(Long userId, Long billingId, String paymentMethod, BigDecimal amount) {
        Billing billing = billingRepository.findById(billingId)
                .orElseThrow(() -> new BusinessException("账单不存在"));

        if (!billing.getUserId().equals(userId)) {
            throw new BusinessException("无权支付他人账单");
        }

        if (Billing.Status.PAID.equals(billing.getStatus())) {
            throw new BusinessException("账单已支付");
        }

        if (amount.compareTo(billing.getAmount()) < 0) {
            throw new BusinessException("支付金额不足");
        }

        billing.setStatus(Billing.Status.PAID);
        billing.setPaidAt(LocalDateTime.now());
        billing.setPaymentMethod(paymentMethod);
        billing = billingRepository.save(billing);

        User user = userRepository.findById(userId).orElseThrow();
        user.setBalance(user.getBalance().add(amount));
        userRepository.save(user);

        Payment payment = new Payment();
        payment.setUserId(userId);
        payment.setBillingId(billingId);
        payment.setAmount(amount);
        payment.setPaymentMethod(paymentMethod);
        payment.setTransactionId("TXN" + System.currentTimeMillis());
        payment.setStatus("SUCCESS");
        paymentRepository.save(payment);

        return billing;
    }

    public List<Billing> getUserBills(Long userId) {
        return billingRepository.findByUserId(userId);
    }

    public List<Billing> getUserUnpaidBills(Long userId) {
        return billingRepository.findByUserIdAndStatus(userId, Billing.Status.UNPAID);
    }
}
