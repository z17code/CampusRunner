package com.campus.runner.common;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserLoginDTO {

    @NotBlank(message = "请输入用户名或手机号")
    private String account;

    @NotBlank(message = "密码不能为空")
    private String password;
}