package com.campus.runner.common;

import com.campus.runner.entity.User;
import com.campus.runner.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 当前登录用户解析工具。
 *
 * 从请求头 Authorization（形如 "Bearer <token>"）中提取 Token，
 * 经 {@link TokenManager} 反查用户 ID，再加载完整 User 实体。
 * 各控制器在做越权校验时统一调用，避免每个接口手写一遍解析逻辑。
 */
@Component
public class SecurityUtil {

    /** 角色常量：与 user.role 字段对齐（0-发单同学 1-跑腿骑手 2-管理员） */
    public static final int ROLE_CLIENT = 0;
    public static final int ROLE_RUNNER = 1;
    public static final int ROLE_ADMIN = 2;

    @Autowired
    private TokenManager tokenManager;

    @Autowired
    private UserService userService;

    /**
     * 解析当前登录用户。
     *
     * @param authHeader 请求头 Authorization 的值
     * @return 登录用户实体；未登录 / token 失效返回 null
     */
    public User getCurrentUser(String authHeader) {
        if (!StringUtils.hasText(authHeader)) {
            return null;
        }
        // 兼容 "Bearer xxx" 与裸 token 两种写法
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7).trim() : authHeader.trim();
        Long userId = tokenManager.getUserId(token);
        if (userId == null) {
            return null;
        }
        return userService.getById(userId);
    }

    /** 是否管理员 */
    public static boolean isAdmin(User user) {
        return user != null && user.getRole() != null && user.getRole() == ROLE_ADMIN;
    }
}
