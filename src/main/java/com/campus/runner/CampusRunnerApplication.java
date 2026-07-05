package com.campus.runner;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @MapperScan 显式扫描 mapper 包：
 * 本项目用的是 mybatis-plus-spring-boot3-starter 3.5.7，但 Spring Boot 是 4.1.0，
 * 两者版本组合下 @Mapper 注解的自动扫描可能不生效，导致 Mapper bean 未注册。
 * 用 @MapperScan 显式指定扫描路径是最稳妥的写法，不依赖 starter 的自动扫描。
 */
@SpringBootApplication
@MapperScan("com.campus.runner.mapper")
public class CampusRunnerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusRunnerApplication.class, args);
    }
}