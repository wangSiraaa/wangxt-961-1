package com.charging.shed.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ChargingStopDTO {
    @NotNull(message = "充电记录ID不能为空")
    private Long recordId;

    @NotNull(message = "结束SOC不能为空")
    private BigDecimal endSoc;

    private String stopReason;
}
