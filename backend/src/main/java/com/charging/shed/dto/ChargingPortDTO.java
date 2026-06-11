package com.charging.shed.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ChargingPortDTO {
    @NotNull(message = "车棚ID不能为空")
    private Long shedId;

    @NotBlank(message = "端口编号不能为空")
    private String portNumber;

    @NotBlank(message = "端口类型不能为空")
    private String portType;

    @NotNull(message = "额定功率不能为空")
    private BigDecimal powerRating;

    private String status;
}
