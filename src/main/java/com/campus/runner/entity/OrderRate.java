package com.campus.runner.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("order_rate")
public class OrderRate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long clientId;

    private Long runnerId;

    private Integer score;

    private String content;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}