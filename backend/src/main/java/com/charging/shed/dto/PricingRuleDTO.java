package com.charging.shed.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
public class PricingRuleDTO {
    @NotNull(message = "车棚ID不能为空")
    private Long shedId;

    @NotNull(message = "电价不能为空")
    private BigDecimal pricePerKwh;

    @NotNull(message = "服务费不能为空")
    private BigDecimal serviceFee;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime peakStartTime;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime peakEndTime;

    private BigDecimal peakPriceMultiplier;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime valleyStartTime;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime valleyEndTime;

    private BigDecimal valleyPriceMultiplier;

    @NotNull(message = "是否启用不能为空")
    private Boolean isActive;
}
