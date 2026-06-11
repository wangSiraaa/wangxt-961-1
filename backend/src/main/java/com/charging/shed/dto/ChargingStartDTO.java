package com.charging.shed.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ChargingStartDTO {
    @NotNull(message = "预约ID不能为空")
    private Long reservationId;

    @NotNull(message = "起始SOC不能为空")
    private BigDecimal startSoc;
}
