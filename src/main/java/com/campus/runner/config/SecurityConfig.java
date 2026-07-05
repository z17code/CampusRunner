package com.campus.runner.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * AI Agent 使用的 RestTemplate（用于调用商汤 sensenova 大模型 API）。
     * Spring 轻量级 HTTP 客户端，适合少量外部 HTTP 调用场景。
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}