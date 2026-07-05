package com.campus.runner.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "跑腿订单分页查询参数")
public class ErrandOrderPageQuery {

    @Schema(description = "页码（从 1 开始）")
    private int pageNum = 1;

    @Schema(description = "每页条数")
    private int pageSize = 10;

    @Schema(description = "跑腿费下限")
    private BigDecimal errandFeeMin;

    @Schema(description = "跑腿费上限")
    private BigDecimal errandFeeMax;

    @Schema(description = "订单标题关键词")
    private String title;

    @Schema(description = "取件地关键词")
    private String pickupAddress;

    @Schema(description = "送达地关键词")
    private String shippingAddress;

    @Schema(description = "订单状态（0-待接单 1-配送中 2-已送达 3-已完成 4-已取消）")
    private Integer status;
}