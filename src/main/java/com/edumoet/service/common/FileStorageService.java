package com.edumoet.service.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.edumoet.entity.Attachment;
import com.edumoet.repository.AttachmentRepository;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class FileStorageService {

    private final AttachmentRepository attachmentRepository;
    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${cloud.aws.s3.base-folder:ltWeb}")
    private String baseFolder;

    // ⚙️ Constructor giữ nguyên
    public FileStorageService(
            @Value("${file.upload-dir:uploads}") String uploadDir,
            AttachmentRepository attachmentRepository,
            S3Client s3Client) {
        this.attachmentRepository = attachmentRepository;
        this.s3Client = s3Client;
    }

    // 📤 Upload file lên S3 — giữ nguyên tên hàm, giá trị trả về
    public Attachment store(MultipartFile file) throws IOException {
        String original = StringUtils.cleanPath(file.getOriginalFilename());
        String ext = "";
        int idx = original.lastIndexOf('.');
        if (idx > -1) ext = original.substring(idx);

        // Tạo tên file duy nhất
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String stored = String.format("att_%s_%s%s", timestamp, uuid, ext);
        String key = baseFolder + "/" + stored;

        try {
            // Upload lên S3
            s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build(),
                RequestBody.fromBytes(file.getBytes())
            );

            // Lưu metadata vào DB
            Attachment a = new Attachment();
            a.setFilename(stored);
            a.setOriginalName(original);
            a.setContentType(file.getContentType());
            a.setSize(file.getSize());
            a.setUploadedAt(LocalDateTime.now());
            attachmentRepository.save(a);
            return a;

        } catch (S3Exception e) {
            throw new IOException("Failed to upload file to S3: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    // 📥 Tải file từ S3 — giữ nguyên kiểu trả về Resource
    public Resource loadAsResource(String filename) throws MalformedURLException {
        String key = baseFolder + "/" + filename;

        try {
            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(
                GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build()
            );

            byte[] data = objectBytes.asByteArray();
            return new InputStreamResource(new ByteArrayInputStream(data));

        } catch (S3Exception e) {
            throw new MalformedURLException("File not found on S3: " + filename);
        }
    }

    // ⚙️ Giữ nguyên phương thức (không dùng local nữa nhưng vẫn return null để tương thích)
    public Path getStorageLocation() {
        return null;
    }
}
