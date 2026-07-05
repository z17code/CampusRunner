package com.campus.runner.controller;

import com.campus.runner.common.AiParseResult;
import com.campus.runner.common.AiParseRequest;
import com.campus.runner.common.BusinessException;
import com.campus.runner.common.Result;
import com.campus.runner.service.AiAgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * AI 智能发单 Agent 控制器。
 *
 * 对外暴露：
 *   POST /errand/order/ai-parse —— 接收大白话文本，返回结构化订单字段
 */
@Tag(name = "AI 智能发单", description = "AI 解析大白话，自动提取跑腿订单结构化字段")
@RestController
@RequestMapping("/errand/order")
public class AiAgentController {

    @Autowired
    private AiAgentService aiAgentService;

    /**
     * AI 智能解析 —— 接收用户输入的大白话，提取结构化跑腿订单字段。
     *
     * @param req 请求体：{@code rawText: 用户大白话}
     * @return {@link AiParseResult} 结构化订单数据
     */
    @Operation(summary = "AI 智能解析大白话填单", description = """
            接收用户输入的日常白话口语，AI 自动提取结构化跑腿订单字段：
            任务标题、详细描述、跑腿小费、垫付费用、取件地址、送达目的地。
            前端拿到结果后可直接回填表单，实现"一键免打字发单"。
            """)
    @ApiResponse(responseCode = "200", description = "解析成功",
            content = @Content(schema = @Schema(implementation = AiParseResult.class)))
    @PostMapping("/ai-parse")
    public Result<AiParseResult> aiParse(@Valid @RequestBody AiParseRequest req) {
        AiParseResult result = aiAgentService.parseOrderText(req.getRawText());
        return Result.ok(result);
    }
}
