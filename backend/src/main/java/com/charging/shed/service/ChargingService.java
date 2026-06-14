package com.charging.shed.service;

import com.charging.shed.entity.*;
import com.charging.shed.exception.BusinessException;
import com.charging.shed.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
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

        long minutes = Duration.between(record.getStartTime(), record.getEndTime()).toMinutes();
        BigDecimal hours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);

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
                    .divide(BigDecimal.valueOf(tempHistory.size()), 2, RoundingMode.HALF_UP);
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

        BillingDetail detail = calculateTimeSegmentBilling(record, rule);

        int freeMin = rule.getFreeMinutes() != null ? rule.getFreeMinutes() : 0;
        long totalMinutes = Duration.between(record.getStartTime(), record.getEndTime()).toMinutes();
        if (totalMinutes <= 0) {
            totalMinutes = 1;
        }

        BigDecimal freeRatio = BigDecimal.ZERO;
        if (freeMin > 0) {
            freeRatio = BigDecimal.valueOf(Math.min(freeMin, totalMinutes))
                    .divide(BigDecimal.valueOf(totalMinutes), 4, RoundingMode.HALF_UP);
        }

        BigDecimal energyAfterFree = record.getEnergyConsumed()
                .multiply(BigDecimal.ONE.subtract(freeRatio))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal energyAmountAfterFree = detail.totalAmount
                .multiply(BigDecimal.ONE.subtract(freeRatio))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal serviceFee = rule.getServiceFee().multiply(energyAfterFree)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalAmount = energyAmountAfterFree.add(serviceFee);

        Billing billing = new Billing();
        billing.setUserId(record.getUserId());
        billing.setReservationId(record.getReservationId());
        billing.setChargingRecordId(record.getId());
        billing.setBillType(Billing.BillType.CHARGING);
        billing.setAmount(totalAmount);
        billing.setEnergyConsumed(record.getEnergyConsumed());
        billing.setPricePerKwh(rule.getPricePerKwh());
        billing.setServiceFee(serviceFee);
        billing.setPeakEnergy(detail.peakEnergy);
        billing.setValleyEnergy(detail.valleyEnergy);
        billing.setFlatEnergy(detail.flatEnergy);
        billing.setPeakAmount(detail.peakAmount);
        billing.setValleyAmount(detail.valleyAmount);
        billing.setFlatAmount(detail.flatAmount);
        billing.setFreeMinutes(freeMin);
        billing.setStatus(Billing.Status.UNPAID);
        billing.setDueDate(LocalDateTime.now().plusDays(7).toLocalDate());

        billing = billingRepository.save(billing);

        User user = userRepository.findById(record.getUserId()).orElseThrow();
        user.setBalance(user.getBalance().subtract(totalAmount));
        userRepository.save(user);

        return billing;
    }

    public BillingDetail calculateTimeSegmentBilling(ChargingRecord record, PricingRule rule) {
        LocalDateTime start = record.getStartTime();
        LocalDateTime end = record.getEndTime() != null ? record.getEndTime() : LocalDateTime.now();

        long totalMinutes = Duration.between(start, end).toMinutes();
        if (totalMinutes <= 0) {
            totalMinutes = 1;
        }

        long peakMinutes = 0;
        long valleyMinutes = 0;
        long flatMinutes = 0;

        for (long i = 0; i < totalMinutes; i++) {
            LocalDateTime currentMinute = start.plusMinutes(i);
            LocalTime timeOfDay = currentMinute.toLocalTime();

            if (isInTimeRange(timeOfDay, rule.getPeakStartTime(), rule.getPeakEndTime())) {
                peakMinutes++;
            } else if (isInTimeRange(timeOfDay, rule.getValleyStartTime(), rule.getValleyEndTime())) {
                valleyMinutes++;
            } else {
                flatMinutes++;
            }
        }

        BigDecimal totalEnergy = record.getEnergyConsumed();
        BigDecimal energyPerMinute = totalEnergy.divide(BigDecimal.valueOf(totalMinutes), 6, RoundingMode.HALF_UP);

        BigDecimal peakEnergy = energyPerMinute.multiply(BigDecimal.valueOf(peakMinutes))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal valleyEnergy = energyPerMinute.multiply(BigDecimal.valueOf(valleyMinutes))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal flatEnergy = energyPerMinute.multiply(BigDecimal.valueOf(flatMinutes))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal basePrice = rule.getPricePerKwh();
        BigDecimal peakAmount = peakEnergy.multiply(basePrice)
                .multiply(rule.getPeakPriceMultiplier())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal valleyAmount = valleyEnergy.multiply(basePrice)
                .multiply(rule.getValleyPriceMultiplier())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal flatAmount = flatEnergy.multiply(basePrice)
                .multiply(rule.getFlatPriceMultiplier())
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalAmount = peakAmount.add(valleyAmount).add(flatAmount);

        return new BillingDetail(peakEnergy, valleyEnergy, flatEnergy,
                peakAmount, valleyAmount, flatAmount, totalAmount);
    }

    private boolean isInTimeRange(LocalTime time, LocalTime rangeStart, LocalTime rangeEnd) {
        if (rangeStart == null || rangeEnd == null) {
            return false;
        }
        if (rangeStart.isBefore(rangeEnd)) {
            return !time.isBefore(rangeStart) && time.isBefore(rangeEnd);
        } else {
            return !time.isBefore(rangeStart) || time.isBefore(rangeEnd);
        }
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

    public static class BillingDetail {
        private final BigDecimal peakEnergy;
        private final BigDecimal valleyEnergy;
        private final BigDecimal flatEnergy;
        private final BigDecimal peakAmount;
        private final BigDecimal valleyAmount;
        private final BigDecimal flatAmount;
        private final BigDecimal totalAmount;

        public BillingDetail(BigDecimal peakEnergy, BigDecimal valleyEnergy, BigDecimal flatEnergy,
                             BigDecimal peakAmount, BigDecimal valleyAmount, BigDecimal flatAmount,
                             BigDecimal totalAmount) {
            this.peakEnergy = peakEnergy;
            this.valleyEnergy = valleyEnergy;
            this.flatEnergy = flatEnergy;
            this.peakAmount = peakAmount;
            this.valleyAmount = valleyAmount;
            this.flatAmount = flatAmount;
            this.totalAmount = totalAmount;
        }

        public BigDecimal getPeakEnergy() { return peakEnergy; }
        public BigDecimal getValleyEnergy() { return valleyEnergy; }
        public BigDecimal getFlatEnergy() { return flatEnergy; }
        public BigDecimal getPeakAmount() { return peakAmount; }
        public BigDecimal getValleyAmount() { return valleyAmount; }
        public BigDecimal getFlatAmount() { return flatAmount; }
        public BigDecimal getTotalAmount() { return totalAmount; }
    }
}
