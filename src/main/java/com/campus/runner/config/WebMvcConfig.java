package com.campus.runner.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 综合配置类。
 *
 * 这里同时承担两件事：
 *  1. 静态资源映射 —— 把上传到磁盘的跑腿小票/凭证图片暴露成可访问的 URL；
 *  2. 全局 CORS 跨域配置 —— 解决「前端(localhost:3000)调用后端(localhost:8080)」时
 *     浏览器抛出的跨域拦截问题，这是实训九【步骤 7】的核心打分点。
 *
 * 为什么需要 CORS？
 *  浏览器有「同源策略」：前端页面跑在 http://localhost:3000，而后端接口在
 *  http://localhost:8080，端口不同属于「跨源」。前端用 axios 发请求时，浏览器会
 *  先看后端有没有在响应头里声明「允许这个来源访问」，没有就直接拦截并报 CORS 错误。
 *  我们在这里统一配置，就等于在后端挂了一块「允许校园跑腿前端访问」的牌子。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 上传文件存储根目录，由配置注入（campus.upload.path）。
     * 不再硬编码 C:/uploads/，由各环境配置文件提供与平台一致的路径。
     */
    @Value("${campus.upload.path}")
    private String uploadPath;

    /**
     * 静态资源映射：将虚拟路径 /uploads/** 映射到实际磁盘上传目录。
     * 路径来自配置，开发环境指向 C:/uploads，生产环境指向 /app/campus_runner/uploads。
     * 「file:」前缀 + 末尾斜杠是 Spring 对文件系统资源定位的标准要求。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + ensureTrailingSlash(uploadPath));
    }

    /**
     * 保证路径以斜杠结尾，兼容 Windows（/）与 Linux（/）分隔符，
     * 避免 Spring 资源定位时把末级目录当成文件而抛 FileNotFoundException。
     */
    private String ensureTrailingSlash(String path) {
        return path.endsWith("/") || path.endsWith("\\") ? path : path + "/";
    }

    /**
     * 全局 CORS 跨域配置（实训九【步骤 7】）。
     *
     * 各参数含义（白话文）：
     *  - addMapping("/**")           ：对所有后端接口都生效，不论是 /user/login 还是 /errand/order/page；
     *  - allowedOriginPatterns(...)  ：允许哪些前端地址来访问。用 allowedOriginPatterns 而不是
     *                                  allowedOrigins，是因为下面开启了 allowCredentials(true)，
     *                                  Spring 要求此时不能用 "*" 通配，必须用 patterns 形式；
     *                                  这里列出了开发期常见的几个前端端口（Vite 默认 5173/3000、
     *                                  Vue CLI 默认 8080/8081），兼顾实训与本地联调；
     *  - allowedMethods(...)         ：允许的 HTTP 动词。涵盖了增(POST)删(DELETE)改(PUT)查(GET)
     *                                  以及 OPTIONS（浏览器跨域预检请求会用）；
     *  - allowedHeaders("*")         ：允许前端携带任意请求头，尤其要放行 Authorization（登录后
     *                                  用来携带 Token 的头），否则带 Token 的请求会被 CORS 拦截；
     *  - exposedHeaders(...)         ：允许前端 JS 能读到的响应头，这里把 Token、分页总数等
     *                                  自定义头暴露出去，方便前端读取；
     *  - allowCredentials(true)      ：允许携带 Cookie / 凭证。校园跑腿用 Token 鉴权，
     *                                  虽然主要靠 Authorization 头，但开启凭证是跨域「带身份」请求的标准做法；
     *  - maxAge(3600)                ：预检请求(OPTIONS)的缓存时间(秒)。1 小时内同一类请求
     *                                  浏览器不再重复发预检，减少一次往返，提升联调体验。
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 允许的前端来源：放行所有源。
                // 生产环境前端通过云服务器公网 IP 访问，IP 不固定且端口可能变化，
                // 因此这里用通配模式 "*" 兼容任意公网 IP / 端口 / 协议。
                // 注意：因为开启了 allowCredentials(true)，Spring Boot 3.x 规范下
                // 不能直接用 allowedOrigins("*")，必须用 allowedOriginPatterns("*")，
                // 否则会抛 Invalid CORS request 被浏览器拦截。
                .allowedOriginPatterns("*")
                // 允许的 HTTP 方法：增删改查 + 跨域预检 OPTIONS
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                // 允许所有请求头（必须放行 Authorization，登录后携带 Token 全靠它）
                .allowedHeaders("*")
                // 暴露给前端 JS 可读的响应头
                .exposedHeaders("Authorization", "Content-Disposition", "X-Total-Count")
                // 允许携带凭证（Cookie 等）
                .allowCredentials(true)
                // 预检请求缓存 1 小时，减少浏览器重复探测
                .maxAge(3600);
    }
}
