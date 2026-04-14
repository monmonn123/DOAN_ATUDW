package com.edumoet.service.common;

import com.edumoet.config.FileStorageConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Local File Storage Service - Lưu trữ file trên ổ đĩa cục bộ thay vì AWS S3
 * Tương ứng với: F:\anh\doan\atudw\{users,questions,answers}
 */
@Service
public class LocalFileStorageService {

    @Autowired
    private FileStorageConfig fileStorageConfig;

    /**
     * Upload file avatar user
     * 
     * @param file file cần upload
     * @param userId ID của user
     * @return tên file sau khi lưu (ví dụ: user_123_uuid.jpg)
     */
    public String uploadUserFile(MultipartFile file, Long userId) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File không được để trống");
        }

        String filename = generateFilename("user_" + userId, file.getOriginalFilename());
        Path directory = Paths.get(fileStorageConfig.getUsersDir());
        
        // Tạo thư mục nếu chưa tồn tại
        Files.createDirectories(directory);
        
        Path targetPath = directory.resolve(filename);
        Files.write(targetPath, file.getBytes());
        
        return filename;
    }

    /**
     * Upload file ảnh câu hỏi
     */
    public String uploadQuestionFile(MultipartFile file, Long questionId) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File không được để trống");
        }

        String filename = generateFilename("question_" + questionId, file.getOriginalFilename());
        Path directory = Paths.get(fileStorageConfig.getQuestionsDir());
        
        Files.createDirectories(directory);
        
        Path targetPath = directory.resolve(filename);
        Files.write(targetPath, file.getBytes());
        
        return filename;
    }

    /**
     * Upload file ảnh câu trả lời
     */
    public String uploadAnswerFile(MultipartFile file, Long answerId) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File không được để trống");
        }

        String filename = generateFilename("answer_" + answerId, file.getOriginalFilename());
        Path directory = Paths.get(fileStorageConfig.getAnswersDir());
        
        Files.createDirectories(directory);
        
        Path targetPath = directory.resolve(filename);
        Files.write(targetPath, file.getBytes());
        
        return filename;
    }

    /**
     * Xóa file
     */
    public void deleteFile(String filename, String type) throws IOException {
        Path directory = switch (type) {
            case "user" -> Paths.get(fileStorageConfig.getUsersDir());
            case "question" -> Paths.get(fileStorageConfig.getQuestionsDir());
            case "answer" -> Paths.get(fileStorageConfig.getAnswersDir());
            default -> throw new IllegalArgumentException("Loại file không hợp lệ: " + type);
        };
        
        Path filePath = directory.resolve(filename);
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }
    }

    /**
     * Kiểm tra file có tồn tại
     */
    public boolean fileExists(String filename, String type) {
        Path directory = switch (type) {
            case "user" -> Paths.get(fileStorageConfig.getUsersDir());
            case "question" -> Paths.get(fileStorageConfig.getQuestionsDir());
            case "answer" -> Paths.get(fileStorageConfig.getAnswersDir());
            default -> throw new IllegalArgumentException("Loại file không hợp lệ: " + type);
        };
        
        return Files.exists(directory.resolve(filename));
    }

    /**
     * Tạo tên file duy nhất
     */
    private String generateFilename(String prefix, String originalFilename) {
        String extension = ".jpg";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return prefix + "_" + uuid + extension;
    }
}
