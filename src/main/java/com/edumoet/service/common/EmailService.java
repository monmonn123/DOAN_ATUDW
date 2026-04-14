package com.edumoet.service.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@edumoet.com}")
    private String fromEmail;

    /**
     * Gửi OTP reset password qua email
     */
    public void sendOtp(String toEmail, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("[EDUMOET] Mã OTP Đặt Lại Mật Khẩu");
            message.setText(buildOtpEmailBody(otp));
            
            mailSender.send(message);
            System.out.println("Email sent successfully to: " + toEmail);
        } catch (Exception e) {
            System.err.println("Failed to send email to: " + toEmail);
            e.printStackTrace();
            throw new RuntimeException("Không thể gửi email. Vui lòng thử lại sau.", e);
        }
    }

    /**
     * Xây dựng nội dung email OTP
     */
    private String buildOtpEmailBody(String otp) {
        return String.format("""
            Xin chào,
            
            Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản EDUMOET của mình.
            
            Mã OTP của bạn là: %s
            
            Mã này có hiệu lực trong vòng 5 phút.
            
            Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.
            
            Trân trọng,
            Đội ngũ EDUMOET
            """, otp);
    }

    /**
     * Gửi email xác nhận đặt lại mật khẩu thành công
     */
    public void sendPasswordResetConfirmation(String toEmail) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("[EDUMOET] Mật Khẩu Đã Được Đặt Lại Thành Công");
            message.setText("""
                Xin chào,
                
                Mật khẩu của bạn đã được đặt lại thành công.
                
                Nếu bạn không thực hiện hành động này, vui lòng liên hệ với chúng tôi ngay lập tức.
                
                Trân trọng,
                Đội ngũ EDUMOET
                """);
            
            mailSender.send(message);
            System.out.println("Password reset confirmation email sent to: " + toEmail);
        } catch (Exception e) {
            System.err.println("Failed to send confirmation email to: " + toEmail);
            e.printStackTrace();
        }
    }
}

