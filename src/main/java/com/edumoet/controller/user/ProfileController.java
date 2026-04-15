package com.edumoet.controller.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.edumoet.entity.Question;
import com.edumoet.entity.User;
import com.edumoet.service.common.AnswerService;
import com.edumoet.service.common.QuestionService;
import com.edumoet.service.common.UserService;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Profile Controller - Quản lý hồ sơ cá nhân (dùng AWS S3 cho avatar)
 */
@Controller
@RequestMapping("/profile")
public class ProfileController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private AnswerService answerService;

    // 🟢 AWS S3 dependencies
    @Autowired
    private S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${cloud.aws.s3.base-folder:uploads}")
    private String baseFolder;

    /**
     * Helper: Resolve avatar URL from S3 or fallback to default
     */
    private String resolveAvatarUrl(String profileImage, String username, int size) {
        if (profileImage != null && !profileImage.isBlank() && !profileImage.isEmpty()) {
            // If already full URL, return as is
            if (profileImage.startsWith("http://") || profileImage.startsWith("https://")) {
                return profileImage;
            }
            // Construct S3 URL - use consistent path: ltWeb/avatars/
            // This matches the upload path used in ProfileController and MessageController
            return "https://tungbacket.s3.ap-southeast-1.amazonaws.com/ltWeb/avatars/" + profileImage;
        }
        // Fallback to UI Avatars
        String safeUsername = (username != null && !username.trim().isEmpty()) 
            ? username.trim().replaceAll("\\s+", "+") 
            : "User";
        return "https://ui-avatars.com/api/?name=" + safeUsername + "&size=" + size + "&background=0D6EFD&color=fff&bold=true";
    }

    /**
     * Xem profile của mình
     */
    @GetMapping
    public String myProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Add resolved avatar URL to model
        model.addAttribute("avatarUrl", resolveAvatarUrl(user.getProfileImage(), user.getUsername(), 128));
        // Add base folder for fallback in template
        model.addAttribute("s3BaseFolder", baseFolder);

        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Page<Question> questions = questionService.getQuestionsByAuthor(user, pageable);
        Page<com.edumoet.entity.Answer> answers = answerService.getAnswersByAuthor(user, pageable);

        Long totalQuestions = questionService.countByAuthor(user);
        Long totalAnswers = answerService.countByAuthor(user);

        model.addAttribute("user", user);
        model.addAttribute("currentUser", user);
        model.addAttribute("questions", questions);
        model.addAttribute("answers", answers);
        model.addAttribute("totalQuestions", totalQuestions);
        model.addAttribute("totalAnswers", totalAnswers);
        model.addAttribute("totalComments", 0L);
        model.addAttribute("activities", List.of());
        model.addAttribute("badges", List.of());
        model.addAttribute("pageTitle", "My Profile");

        return "profile/view";
    }

    /**
     * Form chỉnh sửa profile
     */
    @GetMapping("/edit")
    public String editProfileForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", user);
        // Add resolved avatar URL to model
        model.addAttribute("avatarUrl", resolveAvatarUrl(user.getProfileImage(), user.getUsername(), 150));
        // Add base folder for fallback in template
        model.addAttribute("s3BaseFolder", baseFolder);
        model.addAttribute("pageTitle", "Edit Profile");
        return "profile/edit";
    }

    /**
     * Cập nhật profile (Upload avatar lên S3)
     */
    @PostMapping("/update")
    public String updateProfile(
            @RequestParam String email,
            @RequestParam(required = false) String bio,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String website,
            @RequestParam(required = false) String githubUrl,
            @RequestParam(required = false) String linkedinUrl,
            @RequestParam(required = false) MultipartFile profilePicture,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            User user = userService.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            user.setEmail(email);
            user.setBio(bio);
            user.setLocation(location);
            user.setWebsite(website);
            user.setGithubUrl(githubUrl);
            user.setLinkedinUrl(linkedinUrl);

            // ================== Upload Avatar lên S3 ==================
            System.out.println("📤 [PROFILE UPDATE] Checking for avatar upload...");
            System.out.println("   profilePicture: " + (profilePicture != null ? "NOT NULL" : "NULL"));
            if (profilePicture != null) {
                System.out.println("   profilePicture.isEmpty(): " + profilePicture.isEmpty());
                System.out.println("   profilePicture.getSize(): " + profilePicture.getSize());
                System.out.println("   profilePicture.getOriginalFilename(): " + profilePicture.getOriginalFilename());
                System.out.println("   profilePicture.getContentType(): " + profilePicture.getContentType());
            }
            
            if (profilePicture != null && !profilePicture.isEmpty()) {
                System.out.println("✅ [AVATAR UPLOAD] Starting upload process...");
                
                String contentType = profilePicture.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    System.out.println("❌ [AVATAR UPLOAD] Invalid content type: " + contentType);
                    throw new IllegalArgumentException("File must be an image");
                }
                if (profilePicture.getSize() > 5 * 1024 * 1024) {
                    System.out.println("❌ [AVATAR UPLOAD] File too large: " + profilePicture.getSize());
                    throw new IllegalArgumentException("File size must be less than 5MB");
                }

                // Tạo tên file duy nhất
                String originalFilename = profilePicture.getOriginalFilename();
                String extension = (originalFilename != null && originalFilename.contains("."))
                        ? originalFilename.substring(originalFilename.lastIndexOf("."))
                        : ".jpg";

                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String uuid = UUID.randomUUID().toString().substring(0, 8);
                String filename = String.format("user%d_%s_%s%s", user.getId(), timestamp, uuid, extension);
                // Use consistent path: ltWeb/avatars/ to match all other controllers
                String key = "ltWeb/avatars/" + filename;

                System.out.println("📝 [AVATAR UPLOAD] File details:");
                System.out.println("   Original filename: " + originalFilename);
                System.out.println("   Generated filename: " + filename);
                System.out.println("   S3 key: " + key);
                System.out.println("   Content type: " + contentType);
                System.out.println("   File size: " + profilePicture.getSize() + " bytes");

                // Xoá ảnh cũ trên S3 nếu có
                if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
                    try {
                        String oldKey = "ltWeb/avatars/" + user.getProfileImage();
                        System.out.println("🗑️ [AVATAR UPLOAD] Deleting old avatar from S3: " + oldKey);
                        s3Client.deleteObject(DeleteObjectRequest.builder()
                                .bucket(bucketName)
                                .key(oldKey)
                                .build());
                        System.out.println("✅ [AVATAR UPLOAD] Deleted old avatar from S3");
                    } catch (Exception e) {
                        System.out.println("⚠️ [AVATAR UPLOAD] Could not delete old avatar: " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                // Upload ảnh mới
                try {
                    System.out.println("📤 [AVATAR UPLOAD] Uploading to S3...");
                    System.out.println("   Bucket: " + bucketName);
                    System.out.println("   Key: " + key);
                    
                    byte[] fileBytes = profilePicture.getBytes();
                    System.out.println("   File bytes length: " + fileBytes.length);
                    
                    s3Client.putObject(
                            PutObjectRequest.builder()
                                    .bucket(bucketName)
                                    .key(key)
                                    .contentType(contentType)
                                    .build(),
                            RequestBody.fromBytes(fileBytes)
                    );
                    
                    System.out.println("✅ [AVATAR UPLOAD] Successfully uploaded to S3!");
                    user.setProfileImage(filename);
                    System.out.println("💾 [AVATAR UPLOAD] Updated user profileImage to: " + filename);
                } catch (Exception e) {
                    System.err.println("❌ [AVATAR UPLOAD] Error uploading to S3: " + e.getMessage());
                    e.printStackTrace();
                    throw new RuntimeException("Failed to upload avatar to S3: " + e.getMessage(), e);
                }
            } else {
                System.out.println("ℹ️ [PROFILE UPDATE] No avatar file to upload");
            }

            userService.updateUser(user);
            redirectAttributes.addFlashAttribute("successMessage", "✅ Profile updated successfully!");
            return "redirect:/profile";

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
            return "redirect:/profile/edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Error: " + e.getMessage());
            return "redirect:/profile/edit";
        }
    }

    /**
     * Xóa ảnh đại diện (trên S3)
     */
    @PostMapping("/remove-avatar")
    public String removeAvatar(
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            User user = userService.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
                // Use consistent path: ltWeb/avatars/ to match all other controllers
                String key = "ltWeb/avatars/" + user.getProfileImage();

                try {
                    s3Client.deleteObject(DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .build());
                    System.out.println("🗑️ Deleted avatar from S3: " + key);
                } catch (Exception e) {
                    System.out.println("⚠️ Failed to delete avatar: " + e.getMessage());
                }

                user.setProfileImage(null);
                userService.updateUser(user);
                redirectAttributes.addFlashAttribute("successMessage",
                        "✅ Profile picture removed successfully!");
            }

            return "redirect:/profile/edit";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "❌ Error: " + e.getMessage());
            return "redirect:/profile/edit";
        }
    }

    /**
     * Form đổi mật khẩu
     */
    @GetMapping("/change-password")
    public String changePasswordForm(Model model) {
        model.addAttribute("pageTitle", "Change Password");
        return "profile/change-password";
    }

    /**
     * Đổi mật khẩu
     */
    @PostMapping("/change-password")
    public String changePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            User user = userService.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                redirectAttributes.addFlashAttribute("errorMessage", "❌ Current password is incorrect!");
                return "redirect:/profile/change-password";
            }

            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("errorMessage", "❌ New passwords do not match!");
                return "redirect:/profile/change-password";
            }

            user.setPassword(passwordEncoder.encode(newPassword));
            userService.updateUser(user);
            redirectAttributes.addFlashAttribute("successMessage", "✅ Password changed successfully!");
            return "redirect:/profile";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Error: " + e.getMessage());
            return "redirect:/profile/change-password";
        }
    }

    /**
     * Xem profile người khác
     */
    @GetMapping("/{username}")
    public String viewUserProfile(@PathVariable String username, Model model) {
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        model.addAttribute("user", user);
        // Add resolved avatar URL to model
        model.addAttribute("avatarUrl", resolveAvatarUrl(user.getProfileImage(), user.getUsername(), 128));
        // Add base folder for fallback in template
        model.addAttribute("s3BaseFolder", baseFolder);
        model.addAttribute("pageTitle", username + "'s Profile");
        return "profile/public-view";
    }

    /**
     * Danh sách câu hỏi của tôi
     */
    @GetMapping("/my-questions")
    public String myQuestions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "all") String filter,
            Principal principal,
            Model model) {

        if (principal == null) {
            return "redirect:/login";
        }

        User currentUser = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Question> questions;

        switch (filter) {
            case "pending":
                questions = questionService.getPendingQuestionsByAuthor(currentUser, pageable);
                break;
            case "approved":
                questions = questionService.getApprovedQuestionsByAuthor(currentUser, pageable);
                break;
            default:
                questions = questionService.getQuestionsByAuthor(currentUser, pageable);
                break;
        }

        model.addAttribute("questions", questions);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", questions.getTotalPages());
        model.addAttribute("filter", filter);
        model.addAttribute("pageTitle", "My Questions - EDUMOET");

        return "profile/my-questions";
    }
}
