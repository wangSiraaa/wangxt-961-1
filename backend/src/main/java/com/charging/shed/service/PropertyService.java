package com.charging.shed.service;

import com.charging.shed.entity.*;
import com.charging.shed.exception.BusinessException;
import com.charging.shed.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@Service
public class PropertyService {

    private final ChargingShedRepository chargingShedRepository;
    private final ChargingPortRepository chargingPortRepository;
    private final PricingRuleRepository pricingRuleRepository;
    private final BillingRepository billingRepository;
    private final UserRepository userRepository;
    private final BatteryBlacklistRepository batteryBlacklistRepository;

    public PropertyService(ChargingShedRepository chargingShedRepository,
                           ChargingPortRepository chargingPortRepository,
                           PricingRuleRepository pricingRuleRepository,
                           BillingRepository billingRepository,
                           UserRepository userRepository,
                           BatteryBlacklistRepository batteryBlacklistRepository) {
        this.chargingShedRepository = chargingShedRepository;
        this.chargingPortRepository = chargingPortRepository;
        this.pricingRuleRepository = pricingRuleRepository;
        this.billingRepository = billingRepository;
        this.userRepository = userRepository;
        this.batteryBlacklistRepository = batteryBlacklistRepository;
    }

    public List<ChargingShed> getAllSheds() {
        return chargingShedRepository.findAll();
    }

    public List<ChargingShed> getOpenSheds() {
        return chargingShedRepository.findByStatus(ChargingShed.Status.OPEN);
    }

    public ChargingShed getShedById(Long id) {
        return chargingShedRepository.findById(id)
                .orElseThrow(() -> new BusinessException("车棚不存在"));
    }

    @Transactional
    public ChargingShed createShed(String shedName, String location, Integer totalPorts, Long managerId) {
        ChargingShed shed = new ChargingShed();
        shed.setShedName(shedName);
        shed.setLocation(location);
        shed.setTotalPorts(totalPorts);
        shed.setAvailablePorts(totalPorts);
        shed.setStatus(ChargingShed.Status.OPEN);
        shed.setManagerId(managerId);
        return chargingShedRepository.save(shed);
    }

    @Transactional
    public ChargingShed updateShedStatus(Long shedId, String status) {
        ChargingShed shed = getShedById(shedId);
        shed.setStatus(status);
        return chargingShedRepository.save(shed);
    }

    @Transactional
    public ChargingShed updateShed(Long shedId, String shedName, String location, Integer totalPorts, String status) {
        ChargingShed shed = getShedById(shedId);
        if (shedName != null) {
            shed.setShedName(shedName);
        }
        if (location != null) {
            shed.setLocation(location);
        }
        if (totalPorts != null) {
            int currentPortCount = chargingPortRepository.findByShedId(shedId).size();
            if (totalPorts < currentPortCount) {
                throw new BusinessException("总充电位数量不能小于现有充电位数量: " + currentPortCount);
            }
            shed.setTotalPorts(totalPorts);
        }
        if (status != null) {
            shed.setStatus(status);
        }
        return chargingShedRepository.save(shed);
    }

    @Transactional
    public void deleteShed(Long shedId) {
        ChargingShed shed = getShedById(shedId);
        List<ChargingPort> ports = chargingPortRepository.findByShedId(shedId);
        if (!ports.isEmpty()) {
            throw new BusinessException("该车棚下还有充电位，请先删除充电位");
        }
        chargingShedRepository.delete(shed);
    }

    public List<ChargingPort> getPortsByShed(Long shedId) {
        return chargingPortRepository.findByShedId(shedId);
    }

    public List<ChargingPort> getPortsByShedId(Long shedId) {
        return chargingPortRepository.findByShedId(shedId);
    }

    public List<ChargingPort> getAllPorts() {
        return chargingPortRepository.findAll();
    }

    public List<ChargingPort> getAvailablePorts(Long shedId) {
        return chargingPortRepository.findByShedIdAndStatus(shedId, ChargingPort.Status.AVAILABLE);
    }

    public ChargingPort getPortById(Long id) {
        return chargingPortRepository.findById(id)
                .orElseThrow(() -> new BusinessException("充电位不存在"));
    }

    @Transactional
    public ChargingPort createPort(Long shedId, String portCode, String portType, BigDecimal powerRating) {
        ChargingShed shed = getShedById(shedId);

        if (chargingPortRepository.findByShedIdAndPortCode(shedId, portCode).isPresent()) {
            throw new BusinessException("该充电位编号已存在");
        }

        ChargingPort port = new ChargingPort();
        port.setShedId(shedId);
        port.setPortCode(portCode);
        port.setPortType(portType);
        port.setPowerRating(powerRating);
        port.setStatus(ChargingPort.Status.AVAILABLE);
        port.setPowerOn(true);

        port = chargingPortRepository.save(port);

        shed.setTotalPorts(shed.getTotalPorts() + 1);
        shed.setAvailablePorts(shed.getAvailablePorts() + 1);
        chargingShedRepository.save(shed);

        return port;
    }

    @Transactional
    public ChargingPort updatePortStatus(Long portId, String status) {
        ChargingPort port = getPortById(portId);

        String oldStatus = port.getStatus();
        port.setStatus(status);
        port = chargingPortRepository.save(port);

        if (!oldStatus.equals(status)) {
            ChargingShed shed = getShedById(port.getShedId());
            int availableChange = 0;
            if (ChargingPort.Status.AVAILABLE.equals(oldStatus)) {
                availableChange--;
            }
            if (ChargingPort.Status.AVAILABLE.equals(status)) {
                availableChange++;
            }
            if (availableChange != 0) {
                shed.setAvailablePorts(shed.getAvailablePorts() + availableChange);
                chargingShedRepository.save(shed);
            }
        }

        return port;
    }

    @Transactional
    public ChargingPort updatePort(Long portId, Long shedId, String portCode, String portType,
                                   BigDecimal powerRating, String status) {
        ChargingPort port = getPortById(portId);
        
        if (shedId != null && !shedId.equals(port.getShedId())) {
            throw new BusinessException("充电位所属车棚不能修改");
        }
        if (portCode != null) {
            if (chargingPortRepository.findByShedIdAndPortCode(port.getShedId(), portCode)
                    .filter(p -> !p.getId().equals(portId)).isPresent()) {
                throw new BusinessException("该充电位编号已存在");
            }
            port.setPortCode(portCode);
        }
        if (portType != null) {
            port.setPortType(portType);
        }
        if (powerRating != null) {
            port.setPowerRating(powerRating);
        }
        if (status != null) {
            String oldStatus = port.getStatus();
            port.setStatus(status);
            if (!oldStatus.equals(status)) {
                ChargingShed shed = getShedById(port.getShedId());
                int availableChange = 0;
                if (ChargingPort.Status.AVAILABLE.equals(oldStatus)) {
                    availableChange--;
                }
                if (ChargingPort.Status.AVAILABLE.equals(status)) {
                    availableChange++;
                }
                if (availableChange != 0) {
                    shed.setAvailablePorts(shed.getAvailablePorts() + availableChange);
                    chargingShedRepository.save(shed);
                }
            }
        }

        return chargingPortRepository.save(port);
    }

    @Transactional
    public void deletePort(Long portId) {
        ChargingPort port = getPortById(portId);
        
        if (!ChargingPort.Status.AVAILABLE.equals(port.getStatus())) {
            throw new BusinessException("只有可用状态的充电位才能删除");
        }
        
        ChargingShed shed = getShedById(port.getShedId());
        chargingPortRepository.delete(port);
        
        shed.setTotalPorts(shed.getTotalPorts() - 1);
        shed.setAvailablePorts(shed.getAvailablePorts() - 1);
        chargingShedRepository.save(shed);
    }

    public List<PricingRule> getAllPricingRules() {
        return pricingRuleRepository.findAll();
    }

    public List<PricingRule> getActivePricingRules() {
        return pricingRuleRepository.findByStatus("ACTIVE");
    }

    public PricingRule getPricingRuleById(Long id) {
        return pricingRuleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("计费规则不存在"));
    }

    @Transactional
    public PricingRule createPricingRule(Long shedId, BigDecimal pricePerKwh,
                                         BigDecimal serviceFee, LocalTime peakStartTime, LocalTime peakEndTime,
                                         BigDecimal peakMultiplier, LocalTime valleyStartTime, LocalTime valleyEndTime,
                                         BigDecimal valleyMultiplier, Boolean isDefault,
                                         String communityId, Integer freeMinutes, BigDecimal flatPriceMultiplier) {
        PricingRule rule = new PricingRule();
        rule.setRuleName("计费规则-" + System.currentTimeMillis());
        rule.setShedId(shedId);
        rule.setPricePerKwh(pricePerKwh);
        rule.setServiceFee(serviceFee);
        rule.setPeakStartTime(peakStartTime);
        rule.setPeakEndTime(peakEndTime);
        rule.setPeakPriceMultiplier(peakMultiplier);
        rule.setValleyStartTime(valleyStartTime);
        rule.setValleyEndTime(valleyEndTime);
        rule.setValleyPriceMultiplier(valleyMultiplier);
        rule.setDefaultRule(isDefault != null ? isDefault : false);
        rule.setStatus("ACTIVE");
        rule.setCommunityId(communityId);
        rule.setFreeMinutes(freeMinutes);
        rule.setFlatPriceMultiplier(flatPriceMultiplier);

        if (rule.getDefaultRule()) {
            pricingRuleRepository.findDefaultRule().ifPresent(defaultRule -> {
                defaultRule.setDefaultRule(false);
                pricingRuleRepository.save(defaultRule);
            });
        }

        return pricingRuleRepository.save(rule);
    }

    @Transactional
    public PricingRule updatePricingRule(Long ruleId, Long shedId, BigDecimal pricePerKwh,
                                         BigDecimal serviceFee, LocalTime peakStartTime, LocalTime peakEndTime,
                                         BigDecimal peakMultiplier, LocalTime valleyStartTime, LocalTime valleyEndTime,
                                         BigDecimal valleyMultiplier, Boolean isDefault,
                                         String communityId, Integer freeMinutes, BigDecimal flatPriceMultiplier) {
        PricingRule rule = getPricingRuleById(ruleId);

        rule.setShedId(shedId);
        rule.setPricePerKwh(pricePerKwh);
        rule.setServiceFee(serviceFee);
        rule.setPeakStartTime(peakStartTime);
        rule.setPeakEndTime(peakEndTime);
        rule.setPeakPriceMultiplier(peakMultiplier);
        rule.setValleyStartTime(valleyStartTime);
        rule.setValleyEndTime(valleyEndTime);
        rule.setValleyPriceMultiplier(valleyMultiplier);
        rule.setCommunityId(communityId);
        rule.setFreeMinutes(freeMinutes);
        rule.setFlatPriceMultiplier(flatPriceMultiplier);

        if (isDefault != null && isDefault && !rule.getDefaultRule()) {
            rule.setDefaultRule(true);
            pricingRuleRepository.findDefaultRule().ifPresent(defaultRule -> {
                if (!defaultRule.getId().equals(ruleId)) {
                    defaultRule.setDefaultRule(false);
                    pricingRuleRepository.save(defaultRule);
                }
            });
        }

        return pricingRuleRepository.save(rule);
    }

    @Transactional
    public void deletePricingRule(Long ruleId) {
        PricingRule rule = getPricingRuleById(ruleId);
        if (rule.getDefaultRule()) {
            throw new BusinessException("默认计费规则不能删除");
        }
        pricingRuleRepository.delete(rule);
    }

    @Transactional
    public PricingRule updatePricingRuleStatus(Long ruleId, String status) {
        PricingRule rule = getPricingRuleById(ruleId);
        rule.setStatus(status);
        return pricingRuleRepository.save(rule);
    }

    public List<PricingRule> getPricingRulesByShedId(Long shedId) {
        return pricingRuleRepository.findByShedId(shedId);
    }

    public List<Billing> getUnpaidBills() {
        return billingRepository.findByStatus(Billing.Status.UNPAID);
    }

    public List<Billing> getBillsByUser(Long userId) {
        return billingRepository.findByUserId(userId);
    }

    public List<Billing> getAllBills(String status) {
        if (status != null && !status.isEmpty()) {
            return switch (status.toUpperCase()) {
                case "UNPAID" -> billingRepository.findByStatus(Billing.Status.UNPAID);
                case "PAID" -> billingRepository.findByStatus(Billing.Status.PAID);
                case "CANCELLED" -> billingRepository.findByStatus(Billing.Status.CANCELLED);
                default -> billingRepository.findAll();
            };
        }
        return billingRepository.findAll();
    }

    @Transactional
    public Billing markBillAsPaid(Long billId, String paymentMethod) {
        Billing bill = billingRepository.findById(billId)
                .orElseThrow(() -> new BusinessException("账单不存在"));

        if (Billing.Status.PAID.equals(bill.getStatus())) {
            throw new BusinessException("账单已支付");
        }

        bill.setStatus(Billing.Status.PAID);
        bill.setPaidAt(java.time.LocalDateTime.now());
        bill.setPaymentMethod(paymentMethod);
        bill = billingRepository.save(bill);

        User user = userRepository.findById(bill.getUserId()).orElseThrow();
        user.setBalance(user.getBalance().add(bill.getAmount()));
        userRepository.save(user);

        return bill;
    }

    public PricingRule getApplicableRule(Long shedId) {
        List<PricingRule> rules = pricingRuleRepository.findApplicableRules(shedId);
        return rules.isEmpty() ? pricingRuleRepository.findDefaultRule().orElse(null) : rules.get(0);
    }

    public PricingRule getApplicableRuleByCommunity(Long shedId, String communityId) {
        List<PricingRule> rules = pricingRuleRepository.findApplicableRulesByCommunity(shedId, communityId);
        return rules.isEmpty() ? pricingRuleRepository.findDefaultRule().orElse(null) : rules.get(0);
    }

    @Transactional
    public ChargingShed updateShedPower(Long shedId, BigDecimal currentTotalPower) {
        ChargingShed shed = getShedById(shedId);
        shed.setCurrentTotalPower(currentTotalPower);
        return chargingShedRepository.save(shed);
    }

    public List<BatteryBlacklist> getAllBlacklistBrands() {
        return batteryBlacklistRepository.findByStatus("ACTIVE");
    }

    @Transactional
    public BatteryBlacklist addBlacklistBrand(String brandName, String banReason, Long bannedBy) {
        if (batteryBlacklistRepository.existsByBrandNameAndStatus(brandName, "ACTIVE")) {
            throw new BusinessException("该品牌已在黑名单中");
        }
        BatteryBlacklist entry = new BatteryBlacklist();
        entry.setBrandName(brandName);
        entry.setBanReason(banReason);
        entry.setBannedBy(bannedBy);
        entry.setStatus("ACTIVE");
        return batteryBlacklistRepository.save(entry);
    }

    @Transactional
    public void removeBlacklistBrand(Long id) {
        BatteryBlacklist entry = batteryBlacklistRepository.findById(id)
                .orElseThrow(() -> new BusinessException("黑名单记录不存在"));
        entry.setStatus("INACTIVE");
        batteryBlacklistRepository.save(entry);
    }

    public boolean isBrandBlacklisted(String brandName) {
        return batteryBlacklistRepository.existsByBrandNameAndStatus(brandName, "ACTIVE");
    }
}
