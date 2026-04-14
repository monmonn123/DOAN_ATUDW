package com.edumoet.controller.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.edumoet.service.common.PasswordResetService;

@Controller
@RequestMapping("/password")
public class PasswordResetController {

    @Autowired
    private PasswordResetService passwordResetService;

    /**
     * Hiển thị form "Quên mật khẩu" - nhập email
     */
    @GetMapping("/forgot")
    public String showForgotPasswordForm(Model model) {
        model.addAttribute("pageTitle", "Forgot Password - EDUMOET");
        return "auth/forgot-password";
    }

    /**
     * Xử lý gửi OTP qua email
     */
    @PostMapping("/forgot")
    public String sendOTP(@RequestParam String email, 
                         Model model,
                         RedirectAttributes redirectAttributes) {
        try {
            passwordResetService.sendPasswordResetOTP(email);
            model.addAttribute("email", email);
            model.addAttribute("successMessage", "Mã OTP đã được gửi đến email của bạn!");
            model.addAttribute("pageTitle", "Verify OTP - EDUMOET");
            return "auth/verify-otp";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("pageTitle", "Forgot Password - EDUMOET");
            return "auth/forgot-password";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Đã xảy ra lỗi khi gửi OTP. Vui lòng thử lại sau.");
            model.addAttribute("pageTitle", "Forgot Password - EDUMOET");
            return "auth/forgot-password";
        }
    }

    /**
     * Verify OTP (Bước 1: Chỉ xác nhận OTP)
     */
    @PostMapping("/verify-otp")
    public String verifyOTPOnly(@RequestParam String email,
                                @RequestParam String otp,
                                Model model) {
        try {
            // Verify OTP
            if (!passwordResetService.verifyOTP(email, otp)) {
                model.addAttribute("errorMessage", "OTP không hợp lệ hoặc đã hết hạn!");
                model.addAttribute("email", email);
                model.addAttribute("pageTitle", "Verify OTP - EDUMOET");
                return "auth/verify-otp";
            }
            
            // OTP hợp lệ → chuyển sang trang nhập mật khẩu mới
            model.addAttribute("email", email);
            model.addAttribute("otp", otp);
            model.addAttribute("pageTitle", "Reset Password - EDUMOET");
            return "auth/reset-password";
            
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Đã xảy ra lỗi. Vui lòng thử lại sau.");
            model.addAttribute("email", email);
            model.addAttribute("pageTitle", "Verify OTP - EDUMOET");
            return "auth/verify-otp";
        }
    }

    /**
     * Reset Password (Bước 2: Đặt lại mật khẩu sau khi verify OTP)
     */
    @PostMapping("/reset")
    public String resetPassword(@RequestParam String email,
                               @RequestParam String otp,
                               @RequestParam String newPassword,
                               @RequestParam String confirmPassword,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        try {
            // Validate password match
            if (!newPassword.equals(confirmPassword)) {
                model.addAttribute("errorMessage", "Mật khẩu xác nhận không khớp!");
                model.addAttribute("email", email);
                model.addAttribute("otp", otp);
                model.addAttribute("pageTitle", "Reset Password - EDUMOET");
                return "auth/reset-password";
            }

            // Validate password length
            if (newPassword.length() < 6) {
                model.addAttribute("errorMessage", "Mật khẩu phải có ít nhất 6 ký tự!");
                model.addAttribute("email", email);
                model.addAttribute("otp", otp);
                model.addAttribute("pageTitle", "Reset Password - EDUMOET");
                return "auth/reset-password";
            }

            // Reset password
            passwordResetService.resetPassword(email, otp, newPassword);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Mật khẩu đã được đặt lại thành công! Vui lòng đăng nhập.");
            return "redirect:/login?reset_success";
            
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("email", email);
            model.addAttribute("otp", otp);
            model.addAttribute("pageTitle", "Reset Password - EDUMOET");
            return "auth/reset-password";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Đã xảy ra lỗi. Vui lòng thử lại sau.");
            model.addAttribute("email", email);
            model.addAttribute("otp", otp);
            model.addAttribute("pageTitle", "Reset Password - EDUMOET");
            return "auth/reset-password";
        }
    }

    /**
     * Gửi lại OTP (resend)
     */
    @PostMapping("/resend")
    public String resendOTP(@RequestParam String email,
                           Model model) {
        try {
            passwordResetService.sendPasswordResetOTP(email);
            model.addAttribute("email", email);
            model.addAttribute("successMessage", "Mã OTP mới đã được gửi lại!");
            model.addAttribute("pageTitle", "Verify OTP - EDUMOET");
            return "auth/verify-otp";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("email", email);
            model.addAttribute("pageTitle", "Verify OTP - EDUMOET");
            return "auth/verify-otp";
        }
    }
}

