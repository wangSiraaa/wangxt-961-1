package com.charging.shed.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentDTO {
    @NotNull(message = "账单ID不能为空")
    private Long billingId;

    @NotBlank(message = "支付方式不能为空")
    private String paymentMethod;

    @NotNull(message = "支付金额不能为空")
    private BigDecimal amount;
}
