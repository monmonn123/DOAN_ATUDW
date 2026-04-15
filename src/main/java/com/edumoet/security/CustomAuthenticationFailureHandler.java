package com.edumoet.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        String errorMessage = "Đăng nhập thất bại!";

        if (exception instanceof LockedException) {
            errorMessage = "Tài khoản của bạn đã bị khóa. Vui lòng thử lại sau hoặc liên hệ quản trị viên.";
            setDefaultFailureUrl("/login?locked");

        } else if (exception instanceof DisabledException) {
            errorMessage = "Tài khoản của bạn đã bị vô hiệu hóa. Vui lòng liên hệ quản trị viên.";
            setDefaultFailureUrl("/login?disabled");

        } else if (exception instanceof BadCredentialsException) {
            String username = request.getParameter("username");
            boolean lockedNow = false;

            if (username != null && !username.trim().isEmpty()) {
                lockedNow = loginAttemptService.processFailedLogin(username.trim());
            }

            if (lockedNow) {
                errorMessage = "Tài khoản tạm thời bị khóa do nhập sai quá nhiều lần. Vui lòng thử lại sau 15 phút.";
                setDefaultFailureUrl("/login?locked");
            } else {
                errorMessage = "Tên đăng nhập hoặc mật khẩu không đúng!";
                setDefaultFailureUrl("/login?invalid");
            }

        } else {
            errorMessage = "Đăng nhập thất bại: " + exception.getMessage();
            setDefaultFailureUrl("/login?error");
        }

        request.getSession().setAttribute("SPRING_SECURITY_LAST_EXCEPTION", exception);
        request.getSession().setAttribute("LOGIN_ERROR_MESSAGE", errorMessage);

        super.onAuthenticationFailure(request, response, exception);
    }
}