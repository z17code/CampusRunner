package com.campus.runner.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("errand_order")
public class ErrandOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long clientId;

    private Long runnerId;

    private String title;

    private String description;

    private BigDecimal errandFee;

    private BigDecimal goodsPrice;

    private String pickupAddress;

    private String shippingAddress;

    /**
     * 订单状态：0-待接单(WAITING)，1-跑腿配送中(RUNNING)，
     * 2-已送达(DELIVERED)，3-已完成(COMPLETED)，4-已取消(CANCELLED)
     */
    private Integer status;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}