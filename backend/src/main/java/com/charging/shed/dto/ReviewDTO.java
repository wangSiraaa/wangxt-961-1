package com.charging.shed.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewDTO {

    @NotNull(message = "车辆ID不能为空")
    private Long vehicleId;

    private Long powerOffId;

    @NotNull(message = "审核结果不能为空")
    private String reviewResult;

    private String remark;
}
