package com.campus.runner.controller;

import com.campus.runner.common.ErrorCode;
import com.campus.runner.common.Result;
import com.campus.runner.common.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/upload")
public class UploadController {

    // 与 application.yml 中 spring.servlet.multipart.max-file-size 保持一致
    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024L; // 2MB

    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "webp");

    /**
     * 文件上传存储根目录，由配置注入（campus.upload.path）。
     * 不再硬编码 Windows 路径，由 application-{profile}.yml 提供与平台一致的绝对路径：
     *   - dev  → C:/uploads
     *   - prod → /app/campus_runner/uploads
     */
    @Value("${campus.upload.path}")
    private String uploadDir;

    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        // 空文件校验
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上传文件不能为空");
        }

        // 大小校验
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "图片大小不能超过 2MB");
        }

        // 扩展名校验
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件格式不支持");
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅支持 jpg/jpeg/png/webp 格式的图片");
        }

        try {
            // 日期目录 + UUID 重命名
            String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String newFileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
            // 直接使用配置的路径，通过 Paths.get 跨平台拼接，绝不手工拼 Windows 分隔符
            Path targetDir = Paths.get(uploadDir, dateDir);
            Path targetPath = targetDir.resolve(newFileName);

            // 创建目录并写入
            Files.createDirectories(targetDir);
            file.transferTo(targetPath.toFile());

            // 返回可访问的 URL 路径（dateDir 已是 yyyy/MM/dd 格式，固定用 / 作为 URL 分隔符）
            String url = "/uploads/" + dateDir + "/" + newFileName;
            log.info("图片上传成功，存储至 {}，URL：{}", targetPath, url);
            return Result.ok(url);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
        }
    }
}