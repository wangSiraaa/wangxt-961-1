package com.charging.shed.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SmokeReportDTO {

    @NotNull(message = "充电位ID不能为空")
    private Long portId;

    @NotNull(message = "烟雾浓度不能为空")
    private BigDecimal smokeLevel;

    private Long reservationId;
}
