package com.campus.runner.controller;

import com.campus.runner.common.*;
import com.campus.runner.entity.User;
import com.campus.runner.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenManager tokenStore;

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody UserRegisterDTO dto) {
        // 用户名查重
        long usernameCount = userService.lambdaQuery()
                .eq(User::getUsername, dto.getUsername())
                .count();
        if (usernameCount > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户名已存在，请换一个");
        }

        // 手机号查重
        long phoneCount = userService.lambdaQuery()
                .eq(User::getPhone, dto.getPhone())
                .count();
        if (phoneCount > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该手机号已被注册");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPhone(dto.getPhone());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setRole(dto.getRole() != null ? dto.getRole() : 0);

        userService.save(user);
        return Result.ok();
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody UserLoginDTO dto) {
        // 支持用户名或手机号登录
        User user = userService.lambdaQuery()
                .and(wrapper -> wrapper
                        .eq(User::getUsername, dto.getAccount())
                        .or()
                        .eq(User::getPhone, dto.getAccount())
                )
                .one();
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名/手机号或密码错误");
        }

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名/手机号或密码错误");
        }

        // 生成 UUID Token
        String token = UUID.randomUUID().toString().replace("-", "");
        tokenStore.put(token, user.getId());

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRole(user.getRole());

        return Result.ok(vo);
    }

    /**
     * 通过手机号找回密码（无需登录）
     * 校验手机号存在 → 直接重置为新密码
     */
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody UserResetPasswordDTO dto) {
        User user = userService.lambdaQuery()
                .eq(User::getPhone, dto.getPhone())
                .one();
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "该手机号未注册");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userService.updateById(user);
        return Result.ok();
    }
}