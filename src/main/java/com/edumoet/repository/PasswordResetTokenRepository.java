package com.edumoet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.edumoet.entity.PasswordResetToken;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Tìm OTP hợp lệ (chưa dùng, chưa hết hạn)
     */
    @Query("""
        SELECT t FROM PasswordResetToken t
        WHERE t.email = :email 
          AND t.otpCode = :otp
          AND t.used = false 
          AND t.expiresAt >= :now
        ORDER BY t.createdAt DESC
    """)
    Optional<PasswordResetToken> findValid(@Param("email") String email, 
                                           @Param("otp") String otp, 
                                           @Param("now") LocalDateTime now);

    /**
     * Đánh dấu OTP đã sử dụng
     */
    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.used = true WHERE t.email = :email AND t.otpCode = :otp")
    int markUsed(@Param("email") String email, @Param("otp") String otp);

    /**
     * Xóa các OTP cũ đã hết hạn (cleanup)
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :now OR t.used = true")
    int deleteExpiredOrUsed(@Param("now") LocalDateTime now);
}

