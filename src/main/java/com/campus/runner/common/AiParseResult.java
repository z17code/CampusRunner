package com.campus.runner.common;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 大模型 AI 结构化解析结果 —— 对应前端表单字段。
 *
 * 后端 {@link com.campus.runner.service.AiAgentService} 调用大模型后，
 * 将返回的 JSON 反序列化为该对象，再由 Controller 直接返回给前端。
 */
@Data
public class AiParseResult {

    /** 生成的任务标题（10 字以内） */
    private String title;

    /** 提取出的具体跑腿诉求/代购细节/快递信息 */
    private String description;

    /** 跑腿小费（未提及默认 5.00） */
    private BigDecimal errandFee;

    /** 垫付商品代购金额（未提及默认 0.00） */
    private BigDecimal goodsPrice;

    /** 取件地/出发地（未提及为空字符串） */
    private String pickupAddress;

    /** 收件地/送达目的地（未提及为空字符串） */
    private String shippingAddress;
}
