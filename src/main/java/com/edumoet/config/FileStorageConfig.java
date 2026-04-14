package com.edumoet.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình đường dẫn lưu trữ file ảnh
 * Tương ứng với: file.upload.base-path trong application.properties
 */
@Configuration
@ConfigurationProperties(prefix = "file.upload")
public class FileStorageConfig {
    
    private String basePath = "F:\\anh\\doan\\atudw";
    
    public String getBasePath() {
        return basePath;
    }
    
    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }
    
    /**
     * Thư mục lưu avatar user
     */
    public String getUsersDir() {
        return basePath + "\\users";
    }
    
    /**
     * Thư mục lưu ảnh câu hỏi
     */
    public String getQuestionsDir() {
        return basePath + "\\questions";
    }
    
    /**
     * Thư mục lưu ảnh câu trả lời
     */
    public String getAnswersDir() {
        return basePath + "\\answers";
    }
}
