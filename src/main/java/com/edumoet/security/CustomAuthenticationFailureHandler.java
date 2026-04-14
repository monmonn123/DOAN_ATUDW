package com.edumoet.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, 
                                       HttpServletResponse response,
                                       AuthenticationException exception) throws IOException, ServletException {
        
        String errorMessage = "Đăng nhập thất bại!";
        
        if (exception instanceof LockedException) {
            // Tài khoản bị khóa
            errorMessage = "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên để biết thêm chi tiết.";
            setDefaultFailureUrl("/login?error=locked");
        } else if (exception instanceof DisabledException) {
            // Tài khoản bị vô hiệu hóa
            errorMessage = "Tài khoản của bạn đã bị vô hiệu hóa. Vui lòng liên hệ quản trị viên.";
            setDefaultFailureUrl("/login?error=disabled");
        } else if (exception instanceof BadCredentialsException) {
            // Sai tên đăng nhập hoặc mật khẩu
            errorMessage = "Tên đăng nhập hoặc mật khẩu không đúng!";
            setDefaultFailureUrl("/login?error=invalid");
        } else {
            // Lỗi khác
            errorMessage = "Đăng nhập thất bại: " + exception.getMessage();
            setDefaultFailureUrl("/login?error=true");
        }
        
        // Encode message for URL
        String encodedMessage = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
        
        // Add error message to session
        request.getSession().setAttribute("SPRING_SECURITY_LAST_EXCEPTION", exception);
        request.getSession().setAttribute("LOGIN_ERROR_MESSAGE", errorMessage);
        
        super.onAuthenticationFailure(request, response, exception);
    }
}

