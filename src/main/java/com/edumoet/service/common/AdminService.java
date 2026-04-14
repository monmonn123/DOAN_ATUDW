package com.edumoet.service.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.edumoet.entity.Question;
import com.edumoet.entity.User;
import com.edumoet.repository.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Admin Service - Quản lý người dùng
 */
@Service
@Transactional
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private QuestionRepository questionRepository;
    
    @Autowired
    private AnswerRepository answerRepository;
    
    @Autowired
    private ImageService imageService;

    // ================== QUẢN LÝ NGƯỜI DÙNG ==================

    /**
     * Lấy tất cả người dùng (có phân trang)
     */
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    /**
     * Lấy tất cả người dùng (không phân trang)
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Tìm người dùng theo ID
     */
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Tìm người dùng theo username
     */
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Tìm kiếm người dùng theo từ khóa (username hoặc email)
     */
    public Page<User> searchUsers(String keyword, Pageable pageable) {
        return userRepository.findByUsernameContainingOrEmailContaining(keyword, keyword, pageable);
    }

    /**
     * Lấy người dùng theo vai trò
     */
    public List<User> getUsersByRole(String role) {
        return userRepository.findByRole(role);
    }
    
    /**
     * Lấy người dùng theo vai trò (có phân trang)
     */
    public Page<User> getUsersByRole(String role, Pageable pageable) {
        return userRepository.findByRole(role, pageable);
    }
    
    /**
     * Lấy người dùng đang hoạt động (có phân trang)
     */
    public Page<User> getActiveUsers(Pageable pageable) {
        return userRepository.findByIsBanned(false, pageable);
    }
    
    /**
     * Lấy người dùng bị khóa (có phân trang)
     */
    public Page<User> getBannedUsers(Pageable pageable) {
        return userRepository.findByIsBanned(true, pageable);
    }

    /**
     * Khóa tài khoản người dùng
     */
    public void banUser(Long userId, String reason, LocalDateTime bannedUntil) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setIsBanned(true);
        user.setBanReason(reason);
        user.setBannedUntil(bannedUntil);
        
        userRepository.save(user);
    }

    /**
     * Khóa tài khoản vĩnh viễn
     */
    public void banUserPermanently(Long userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setIsBanned(true);
        user.setBanReason(reason);
        user.setBannedUntil(null); // null = permanent ban
        
        userRepository.save(user);
    }

    /**
     * Mở khóa tài khoản
     */
    public void unbanUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setIsBanned(false);
        user.setBanReason(null);
        user.setBannedUntil(null);
        
        userRepository.save(user);
    }

    /**
     * Vô hiệu hóa tài khoản (không cho đăng nhập nhưng không xóa dữ liệu)
     */
    public void deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setIsActive(false);
        userRepository.save(user);
    }

    /**
     * Kích hoạt lại tài khoản
     */
    public void activateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setIsActive(true);
        userRepository.save(user);
    }

    /**
     * Đổi mật khẩu cho người dùng (Admin reset password)
     */
    public void resetUserPassword(Long userId, String newPassword) {
        System.out.println("🔐 AdminService.resetUserPassword called");
        System.out.println("   User ID: " + userId);
        System.out.println("   New Password Length: " + (newPassword != null ? newPassword.length() : 0));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        System.out.println("   User found: " + user.getUsername());
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        System.out.println("   ✅ Password updated successfully");
    }

    /**
     * Thay đổi vai trò của người dùng
     */
    public void changeUserRole(Long userId, String newRole) {
        System.out.println("👤 AdminService.changeUserRole called");
        System.out.println("   User ID: " + userId);
        System.out.println("   New Role: " + newRole);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        System.out.println("   User found: " + user.getUsername());
        System.out.println("   Old Role: " + user.getRole());
        user.setRole(newRole);
        userRepository.save(user);
        System.out.println("   ✅ Role changed successfully");
    }

    /**
     * Xóa người dùng (cẩn thận!)
     * Xóa tất cả dữ liệu liên quan trước
     */
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Delete related entities first to avoid FK constraint violations
        // Note: This will delete ALL user data permanently!
        
        // 1. Delete notifications
        notificationRepository.deleteByUser(user);
        
        // 2. Delete activity logs
        activityLogRepository.deleteByUser(user);
        
        // 3. Delete messages (sent and received)
        messageRepository.deleteBySender(user);
        messageRepository.deleteByReceiver(user);
        
        // 4. Delete reports
        reportRepository.deleteByReporter(user);
        
        // 5. Delete questions manually (to properly handle question_tags FK constraint)
        // Query all questions by this user and delete them one by one
        List<Question> userQuestions = questionRepository.findByAuthor(user);
        System.out.println("Deleting " + userQuestions.size() + " questions for user " + user.getUsername());
        
        for (Question question : userQuestions) {
            try {
                questionService.deleteQuestion(question.getId());
                System.out.println("Deleted question ID: " + question.getId());
            } catch (Exception e) {
                System.err.println("Error deleting question " + question.getId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // 6. Answers and Comments will be cascade deleted via JPA @OneToMany mappings
        
        // Finally, delete the user
        System.out.println("Deleting user ID: " + userId);
        userRepository.deleteById(userId);
        System.out.println("Successfully deleted user: " + user.getUsername());
    }

    /**
     * Lấy số lượng người dùng theo trạng thái
     */
    public long countActiveUsers() {
        return userRepository.countByIsActive(true);
    }

    public long countBannedUsers() {
        return userRepository.countByIsBanned(true);
    }

    public long countUsersByRole(String role) {
        return userRepository.countByRole(role);
    }

    /**
     * Xác minh email cho người dùng
     */
    public void verifyUserEmail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    /**
     * Cập nhật thông tin người dùng
     */
    public User updateUser(User user) {
        return userRepository.save(user);
    }
    
    /**
     * Đếm số câu hỏi của user
     */
    public long countUserQuestions(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return questionRepository.countByAuthor(user);
    }
    
    /**
     * Đếm số câu trả lời của user
     */
    public long countUserAnswers(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return answerRepository.countByAuthor(user);
    }
    
    /**
     * Cập nhật avatar người dùng
     */
    @Transactional
    public void updateUserAvatar(Long userId, String base64ImageData) {
        System.out.println("📤 [ADMIN UPDATE AVATAR] Starting update for user ID: " + userId);
        System.out.println("   Base64 data length: " + (base64ImageData != null ? base64ImageData.length() : 0));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        System.out.println("   User: " + user.getUsername());
        System.out.println("   Current profileImage: " + user.getProfileImage());
        
        // Delete old avatar if exists
        if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
            System.out.println("🗑️ [ADMIN UPDATE AVATAR] Deleting old avatar: " + user.getProfileImage());
            imageService.deleteAvatar(user.getProfileImage());
        }
        
        // Save new avatar
        System.out.println("📤 [ADMIN UPDATE AVATAR] Saving new avatar...");
        String filename = imageService.saveAvatarFromBase64(base64ImageData, "user" + userId);
        System.out.println("✅ [ADMIN UPDATE AVATAR] New filename: " + filename);
        
        user.setProfileImage(filename);
        userRepository.save(user);
        
        System.out.println("✅ [ADMIN UPDATE AVATAR] Avatar updated successfully!");
    }
    
    /**
     * Xóa avatar người dùng
     */
    @Transactional
    public void removeUserAvatar(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
            imageService.deleteAvatar(user.getProfileImage());
            user.setProfileImage(null);
            userRepository.save(user);
        }
    }
    
    /**
     * Count users created within date range
     */
    public long countUsersByDateRange(LocalDateTime start, LocalDateTime end) {
        return userRepository.findAll().stream()
                .filter(u -> u.getCreatedAt() != null)
                .filter(u -> !u.getCreatedAt().isBefore(start) && !u.getCreatedAt().isAfter(end))
                .count();
    }
}

