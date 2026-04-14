package com.edumoet.service.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.edumoet.entity.PasswordResetToken;
import com.edumoet.entity.User;
import com.edumoet.repository.PasswordResetTokenRepository;
import com.edumoet.repository.UserRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int OTP_EXPIRY_MINUTES = 5;

    @Autowired
    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                EmailService emailService,
                                PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Gửi OTP reset password qua email
     */
    @Transactional
    public void sendPasswordResetOTP(String email) {
        // 1. Kiểm tra email tồn tại
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Email không tồn tại trong hệ thống");
        }

        // 2. Tạo OTP 6 chữ số ngẫu nhiên
        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        LocalDateTime now = LocalDateTime.now();

        // 3. Lưu OTP vào database
        PasswordResetToken token = new PasswordResetToken();
        token.setEmail(email);
        token.setOtpCode(otp);
        token.setCreatedAt(now);
        token.setExpiresAt(now.plusMinutes(OTP_EXPIRY_MINUTES));
        token.setUsed(false);
        tokenRepository.save(token);

        // 4. Gửi email chứa OTP
        emailService.sendOtp(email, otp);
        
        System.out.println("DEBUG: OTP sent to " + email + ": " + otp); // For testing
    }

    /**
     * Verify OTP có hợp lệ không
     */
    public boolean verifyOTP(String email, String otp) {
        return tokenRepository.findValid(email, otp, LocalDateTime.now()).isPresent();
    }

    /**
     * Reset password với OTP
     */
    @Transactional
    public void resetPassword(String email, String otp, String newPassword) {
        // 1. Verify OTP
        if (!verifyOTP(email, otp)) {
            throw new IllegalArgumentException("OTP không hợp lệ hoặc đã hết hạn");
        }

        // 2. Tìm user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email không tồn tại trong hệ thống"));

        // 3. Hash mật khẩu mới và lưu
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // 4. Đánh dấu OTP đã sử dụng
        tokenRepository.markUsed(email, otp);
        
        System.out.println("DEBUG: Password reset successfully for " + email);
    }

    /**
     * Cleanup các OTP đã hết hạn hoặc đã dùng
     */
    @Transactional
    public int cleanupExpiredTokens() {
        return tokenRepository.deleteExpiredOrUsed(LocalDateTime.now());
    }
}

