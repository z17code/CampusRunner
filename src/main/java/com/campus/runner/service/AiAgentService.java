package com.campus.runner.service;

import com.campus.runner.common.AiParseResult;

/**
 * AI 智能发单 Agent —— Service 接口。
 *
 * 职责：接收用户大白话，调用大模型进行结构化实体提取，
 *       返回包含 title / description / errandFee / goodsPrice /
 *       pickupAddress / shippingAddress 的结构化结果。
 */
public interface AiAgentService {

    /**
     * 解析用户口语文本，提取跑腿订单结构化字段。
     *
     * @param userText 用户输入的日常大白话（如："帮我赶快去西区食堂二楼买一份15块钱的螺蛳粉，送货到东区11号楼302，给5块钱小费"）
     * @return 结构化解析结果
     * @throws BusinessException 当大模型调用超时或返回异常时抛出
     */
    AiParseResult parseOrderText(String userText);
}
