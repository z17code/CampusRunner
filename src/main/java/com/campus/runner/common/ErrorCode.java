package com.campus.runner.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ========== 通用参数类 ==========
    PARAM_ERROR(40001, "参数错误"),

    // ========== 认证授权类 ==========
    UNAUTHORIZED(40101, "未登录或登录已过期，请重新登录"),
    FORBIDDEN(40301, "权限不足，无法操作他人订单"),

    // ========== 业务资源类 ==========
    ORDER_NOT_FOUND(40401, "订单不存在"),
    USER_NOT_FOUND(40402, "用户不存在"),

    // ========== 系统异常类 ==========
    SYSTEM_ERROR(50000, "系统内部错误");

    private final int code;
    private final String message;
}