package com.charging.shed.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AlertHandleDTO {
    @NotBlank(message = "处理结果不能为空")
    private String handleResult;

    private String remark;
}
