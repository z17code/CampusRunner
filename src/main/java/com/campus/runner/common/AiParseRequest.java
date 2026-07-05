package com.campus.runner.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI 智能解析请求 DTO —— 仅携带用户大白话文本。
 */
@Data
public class AiParseRequest {

    @NotBlank(message = "请输入需要解析的内容")
    @Size(max = 1000, message = "输入内容不能超过 1000 个字符")
    @Schema(description = "用户大白话输入", example = "帮我取个中通快递，在菜鸟驿站，送到3栋402")
    private String rawText;
}
