package com.edumoet.service.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.edumoet.entity.Answer;
import com.edumoet.entity.ImageAttachment;
import com.edumoet.entity.Question;
import com.edumoet.entity.User;
import com.edumoet.repository.AnswerRepository;
import com.edumoet.repository.ImageAttachmentRepository;
import com.edumoet.repository.QuestionRepository;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ImageService {

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${cloud.aws.s3.base-folder:uploads}")
    private String baseFolder;

    @Value("${cloud.aws.region:ap-southeast-1}")
    private String region;

    @Autowired
    private ImageAttachmentRepository imageAttachmentRepository;
    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private AnswerRepository answerRepository;
    @Autowired
    private S3Client s3Client;

    // ================== SAVE IMAGE ==================
    public ImageAttachment saveImage(MultipartFile file, Long questionId, Long answerId, User uploadedBy) throws IOException {
        validateImage(file);

        String originalFilename = file.getOriginalFilename();
        String extension = (originalFilename != null && originalFilename.contains(".")) ?
                originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String typePrefix = questionId != null ? "q" + questionId :
                answerId != null ? "a" + answerId : "img";
        String newFilename = String.format("%s_%s_%s%s", typePrefix, timestamp, uuid, extension);
        String key = baseFolder + "/" + newFilename;

        System.out.println("📤 [SAVE IMAGE] Upload to S3 | bucket=" + bucketName + " | key=" + key);

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromBytes(file.getBytes())
            );
        } catch (S3Exception e) {
            throw new IOException("Failed to upload image to S3: " + e.awsErrorDetails().errorMessage(), e);
        }

        ImageAttachment attachment = new ImageAttachment();
        attachment.setFileName(originalFilename);
        attachment.setPath(newFilename);
        attachment.setContentType(file.getContentType());
        attachment.setUploadedBy(uploadedBy);
        attachment.setCreatedAt(LocalDateTime.now());

        if (questionId != null) {
            Question question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new RuntimeException("Question not found"));
            attachment.setQuestion(question);
        }

        if (answerId != null) {
            Answer answer = answerRepository.findById(answerId)
                    .orElseThrow(() -> new RuntimeException("Answer not found"));
            attachment.setAnswer(answer);
        }

        return imageAttachmentRepository.save(attachment);
    }

    // ================== QUESTION IMAGE ==================
    public ImageAttachment saveQuestionImage(MultipartFile file, Question question) throws IOException {
        validateImage(file);
        String originalFilename = file.getOriginalFilename();
        String extension = (originalFilename != null && originalFilename.contains(".")) ?
                originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String filename = String.format("q%d_%s_%s%s", question.getId(), timestamp, uuid, extension);
        String key = baseFolder + "/" + filename;

        System.out.println("📤 [SAVE QUESTION IMAGE] key=" + key);

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType(file.getContentType())
                        .build(),
                RequestBody.fromBytes(file.getBytes())
        );

        ImageAttachment image = new ImageAttachment();
        image.setFileName(originalFilename);
        image.setPath(filename);
        image.setContentType(file.getContentType());
        image.setQuestion(question);
        image.setUploadedBy(question.getAuthor());
        image.setCreatedAt(LocalDateTime.now());
        return imageAttachmentRepository.save(image);
    }

    // ================== ANSWER IMAGE ==================
    public ImageAttachment saveAnswerImage(MultipartFile file, Answer answer) throws IOException {
        validateImage(file);
        String originalFilename = file.getOriginalFilename();
        String extension = (originalFilename != null && originalFilename.contains(".")) ?
                originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String filename = String.format("a%d_%s_%s%s", answer.getId(), timestamp, uuid, extension);
        String key = baseFolder + "/" + filename;

        System.out.println("📤 [SAVE ANSWER IMAGE] key=" + key);

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType(file.getContentType())
                        .build(),
                RequestBody.fromBytes(file.getBytes())
        );

        ImageAttachment image = new ImageAttachment();
        image.setFileName(originalFilename);
        image.setPath(filename);
        image.setContentType(file.getContentType());
        image.setAnswer(answer);
        image.setUploadedBy(answer.getAuthor());
        image.setCreatedAt(LocalDateTime.now());
        return imageAttachmentRepository.save(image);
    }

    // ================== DELETE IMAGE ==================
    public void deleteImage(ImageAttachment attachment) throws IOException {
        String key = baseFolder + "/" + attachment.getPath();
        System.out.println("🗑️ [DELETE IMAGE] key=" + key);
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());
        } catch (S3Exception e) {
            System.out.println("⚠️ Failed to delete image from S3: " + e.awsErrorDetails().errorMessage());
        }
        imageAttachmentRepository.delete(attachment);
        System.out.println("✅ Image record deleted from DB and S3");
    }

    // ================== GET IMAGE DATA (via public S3 URL) ==================
    public byte[] getImageData(ImageAttachment attachment) throws IOException {
        // 🔹 Đọc ảnh qua link công khai trên S3 thay vì SDK
        String fileUrl = String.format(
                "https://%s.s3.%s.amazonaws.com/%s/%s",
                bucketName, region, baseFolder, attachment.getPath());

        System.out.println("🌐 [READ IMAGE FROM URL] " + fileUrl);

        URL url = new URL(fileUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(10000);

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("Failed to fetch image from S3 (HTTP " + responseCode + ")");
        }

        try (InputStream in = connection.getInputStream()) {
            return in.readAllBytes();
        } finally {
            connection.disconnect();
        }
    }

    // ================== OTHER METHODS ==================
    private void validateImage(MultipartFile file) {
        if (file.isEmpty()) throw new IllegalArgumentException("File is empty");
        if (file.getContentType() == null || !file.getContentType().startsWith("image/"))
            throw new IllegalArgumentException("File must be an image");
        if (file.getSize() > 5 * 1024 * 1024)
            throw new IllegalArgumentException("File size must be less than 5MB");
    }

    public Optional<ImageAttachment> findById(Long id) {
        return imageAttachmentRepository.findById(id);
    }

    public List<ImageAttachment> getImagesForQuestion(Question question) {
        return imageAttachmentRepository.findByQuestion(question);
    }

    public List<ImageAttachment> getImagesForAnswer(Answer answer) {
        return imageAttachmentRepository.findByAnswer(answer);
    }

    public List<ImageAttachment> getImagesByQuestion(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));
        return imageAttachmentRepository.findByQuestion(question);
    }

    public ImageAttachment uploadForQuestion(MultipartFile file, Long questionId, Long uploaderId) {
        try {
            Question question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new RuntimeException("Question not found"));
            return saveQuestionImage(file, question);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload image: " + e.getMessage(), e);
        }
    }

    public ImageAttachment uploadForAnswer(MultipartFile file, Long answerId, Long uploaderId) {
        try {
            Answer answer = answerRepository.findById(answerId)
                    .orElseThrow(() -> new RuntimeException("Answer not found"));
            return saveAnswerImage(file, answer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload image: " + e.getMessage(), e);
        }
    }

    // ================== AVATAR FEATURES ==================
    public String saveAvatarFromBase64(String base64Data, String prefix) {
        try {
            String imageData = base64Data.contains(",") ? base64Data.split(",")[1] : base64Data;
            byte[] imageBytes = Base64.getDecoder().decode(imageData);

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String uuid = UUID.randomUUID().toString().substring(0, 8);
            String filename = String.format("%s_%s_%s.jpg", prefix, timestamp, uuid);

            // Use consistent path: ltWeb/avatars/ to match all other controllers
            String key = "ltWeb/avatars/" + filename;

            System.out.println("📤 [SAVE AVATAR] key=" + key);
            System.out.println("   Bucket: " + bucketName);
            System.out.println("   Filename: " + filename);

            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .contentType("image/jpeg")
                            .build(),
                    RequestBody.fromBytes(imageBytes)
            );

            System.out.println("✅ [SAVE AVATAR] Successfully uploaded to S3!");
            return filename;
        } catch (Exception e) {
            System.err.println("❌ [SAVE AVATAR] Error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to save avatar to S3: " + e.getMessage(), e);
        }
    }

    public void deleteAvatar(String filename) {
        // Use consistent path: ltWeb/avatars/ to match all other controllers
        String key = "ltWeb/avatars/" + filename;

        System.out.println("🗑️ [DELETE AVATAR] key=" + key);
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());
            System.out.println("✅ [DELETE AVATAR] Successfully deleted from S3");
        } catch (S3Exception e) {
            System.out.println("⚠️ [DELETE AVATAR] Failed to delete avatar from S3: " + e.awsErrorDetails().errorMessage());
        }
    }
}
