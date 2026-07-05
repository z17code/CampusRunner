package com.campus.runner.service.impl;

import com.campus.runner.common.AiParseResult;
import com.campus.runner.common.BusinessException;
import com.campus.runner.common.ErrorCode;
import com.campus.runner.service.AiAgentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * AI 智能发单 Agent —— 基于商汤 sensenova 开放平台的大模型服务实现。
 *
 * 技术栈：Spring RestTemplate + OpenAI 兼容协议（/v1/chat/completions）。
 */
@Slf4j
@Service
public class AiAgentServiceImpl implements AiAgentService {

    // ==================== 配置注入（来源 application-dev.yml 或环境变量） ====================
    @Value("${hp.ai.agent.key:}")
    private String apiKey;

    @Value("${hp.ai.agent.base-url:}")
    private String baseUrl;

    @Value("${hp.ai.agent.model:deepseek-v4-flash}")
    private String model;

    // ==================== 依赖 ====================
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== 常量 ====================
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    /**
     * 系统提示词：极其严厉的结构化实体提取指令。
     * 要求大模型严格按照 JSON Schema 返回，不允许任何多余字符。
     */
    private static final String SYSTEM_PROMPT = """
            你是一个校园跑腿代购系统的数据结构化解析专家。你的唯一任务是从用户输入的日常白话口语中，精准提取并分析出跑腿订单所需的关键结构化字段。

            ⚠️ 铁律要求（违反任意一条将被视为严重错误）：
            1. 你必须严格返回 JSON 格式，不要包含任何 Markdown 标记（如 ```json、```），不要包含任何解释性文字、注释或多余换行符。
            2. JSON 中所有字段名必须与以下 Schema 完全一致（区分大小写）。
            3. 所有金额字段必须是数字类型（整数或浮点数），不要用字符串包裹。
            4. 未提及的字段必须使用规定的默认值。

            输出 JSON Schema（请严格遵循）：
            {
              "title": "生成一个10字以内的简短任务标题，概括用户的核心诉求",
              "description": "提取出的具体跑腿诉求、代购细节、快递信息、商品规格口味等全部细节",
              "errandFee": 提取出来的小费或跑腿费（数字，未提及则默认为 5.00）,
              "goodsPrice": 提取出来的垫付商品代购金额（数字，未提及则默认为 0.00）,
              "pickupAddress": "提取出的明确取件地/出发地/代购食堂窗口，若未提及则返回空字符串",
              "shippingAddress": "提取出的明确收件地/送达目的地/宿舍楼号，若未提及则返回空字符串"
            }

            解析示例参考：
            输入："帮我赶快去西区食堂二楼买一份15块钱的螺蛳粉，送货到东区11号楼302，给5块钱小费"
            输出：{"title":"代购西区螺蛳粉","description":"西区食堂二楼购买螺蛳粉一份，送货到东区11号楼302室","errandFee":5.00,"goodsPrice":15.00,"pickupAddress":"西区食堂二楼","shippingAddress":"东区11号楼302"}

            现在请解析用户的输入，只返回 JSON，不要输出任何其他内容：
            """;

    @Override
    public AiParseResult parseOrderText(String userText) {
        log.info("AI Agent 开始解析用户文本：{}", userText);

        // 1. 构造 OpenAI 兼容请求体
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        // messages 数组：system 提示词 + user 输入
        Map<String, Object> systemMessage = Map.of(
                "role", "system",
                "content", SYSTEM_PROMPT
        );
        Map<String, Object> userMessage = Map.of(
                "role", "user",
                "content", userText
        );

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(systemMessage, userMessage),
                "temperature", 0.1,
                "max_tokens", 512
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // 2. 发送 POST 请求
        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(
                    baseUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );
        } catch (ResourceAccessException e) {
            log.error("AI Agent 请求超时或网络异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 服务响应超时，请稍后重试");
        } catch (Exception e) {
            log.error("AI Agent 请求失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 解析服务暂时不可用，请稍后重试");
        }

        // 3. 解析大模型返回的 JSON
        String body = response.getBody();
        if (body == null || body.isBlank()) {
            log.error("AI Agent 返回空响应");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 返回数据异常，请重试");
        }

        try {
            // 从 OpenAI 兼容响应中提取 content 字段
            Map<?, ?> responseMap = objectMapper.readValue(body, Map.class);
            List<?> choices = (List<?>) responseMap.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 返回结果为空，请重试");
            }

            Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
            Map<?, ?> message = (Map<?, ?>) firstChoice.get("message");
            String content = (String) message.get("content");

            if (content == null || content.isBlank()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 未返回有效内容，请重试");
            }

            // 清洗可能存在的 Markdown 代码块标记（兜底防御）
            content = content.trim();
            if (content.startsWith("```")) {
                int firstNewline = content.indexOf('\n');
                if (firstNewline > -1) {
                    content = content.substring(firstNewline + 1);
                }
                if (content.endsWith("```")) {
                    content = content.substring(0, content.length() - 3);
                }
                content = content.trim();
            }

            // 4. 反序列化为 AiParseResult
            AiParseResult result = objectMapper.readValue(content, AiParseResult.class);

            // 5. 强制补齐默认值（防御性编程，防止大模型未按规范返回）
            if (result.getTitle() == null) result.setTitle("待发布跑腿任务");
            if (result.getDescription() == null) result.setDescription("");
            if (result.getErrandFee() == null) result.setErrandFee(new BigDecimal("5.00"));
            if (result.getGoodsPrice() == null) result.setGoodsPrice(BigDecimal.ZERO);
            if (result.getPickupAddress() == null) result.setPickupAddress("");
            if (result.getShippingAddress() == null) result.setShippingAddress("");

            log.info("AI Agent 解析完成：{}", result);
            return result;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI Agent JSON 解析失败，原始响应：{}", body, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 解析结果格式异常，请重试");
        }
    }
}
