package com.charging.shed.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class VehicleBindDTO {
    @NotBlank(message = "车牌号不能为空")
    private String plateNumber;

    @NotBlank(message = "车辆品牌不能为空")
    private String brand;

    @NotBlank(message = "车辆型号不能为空")
    private String model;

    @NotNull(message = "电池容量不能为空")
    private BigDecimal batteryCapacity;

    @NotBlank(message = "车架号不能为空")
    private String vin;
}
