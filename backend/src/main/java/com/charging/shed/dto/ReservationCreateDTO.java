package com.charging.shed.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReservationCreateDTO {
    @NotNull(message = "车辆ID不能为空")
    private Long vehicleId;

    @NotNull(message = "充电位ID不能为空")
    private Long portId;

    @NotNull(message = "预约开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reserveStartTime;

    @NotNull(message = "预约结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reserveEndTime;
}
