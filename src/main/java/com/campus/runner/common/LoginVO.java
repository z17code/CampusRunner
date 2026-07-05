package com.campus.runner.common;

import lombok.Data;

/**
 * 登录成功返回给前端的对象。
 *
 * 旧版登录接口只返回 Token 字符串，前端拿不到角色信息，无法做按钮级权限控制。
 * 改为返回 LoginVO：既含 Token（后续请求携带），又含 userId / role / nickname，
 * 前端登录后缓存这些字段，用于「管理员才显示删除按钮」等交互判断。
 */
@Data
public class LoginVO {

    /** 登录凭证，前端存 localStorage，请求头 Authorization 携带 */
    private String token;

    /** 用户 ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 角色：0-发单同学 1-跑腿骑手 2-管理员 */
    private Integer role;
}
