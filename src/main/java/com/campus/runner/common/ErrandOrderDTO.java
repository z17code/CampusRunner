package com.campus.runner.common;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ErrandOrderDTO {

    @NotBlank(message = "订单标题不能为空")
    private String title;

    private String description;

    @NotNull(message = "跑腿费不能为空")
    private BigDecimal errandFee;

    private BigDecimal goodsPrice;

    private String pickupAddress;

    @NotBlank(message = "送达目的地不能为空")
    private String shippingAddress;
}