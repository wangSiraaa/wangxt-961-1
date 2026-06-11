package com.charging.shed.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChargingShedDTO {
    @NotBlank(message = "车棚名称不能为空")
    private String name;

    @NotBlank(message = "位置不能为空")
    private String location;

    @NotNull(message = "总充电位数不能为空")
    private Integer totalPorts;

    private String status;

    private Long managerId;
}
