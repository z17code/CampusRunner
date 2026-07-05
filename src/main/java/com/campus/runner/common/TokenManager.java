package com.campus.runner.common;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简易内存 Token 管理器（单例 Bean）。
 *
 * 把原来散落在 UserController 里的 tokenStore 提取为全局组件，
 * 这样订单控制器等其它接口也能通过 Token 解析出「当前登录用户」，
 * 从而做越权校验。生产环境应替换为 Redis 实现。
 */
@Component
public class TokenManager {

    /** token -> userId */
    private final Map<String, Long> tokenStore = new ConcurrentHashMap<>();

    /** 登录成功时记录 token 与用户 ID 的映射 */
    public void put(String token, Long userId) {
        if (token != null && userId != null) {
            tokenStore.put(token, userId);
        }
    }

    /** 根据 token 取用户 ID，token 无效或未登录返回 null */
    public Long getUserId(String token) {
        if (token == null) {
            return null;
        }
        return tokenStore.get(token);
    }

    /** 退出登录时移除 token（预留） */
    public void remove(String token) {
        if (token != null) {
            tokenStore.remove(token);
        }
    }
}
