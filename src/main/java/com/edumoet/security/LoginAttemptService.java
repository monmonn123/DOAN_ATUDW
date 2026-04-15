package com.edumoet.security;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.edumoet.entity.User;
import com.edumoet.repository.UserRepository;

@Service
@Transactional
public class LoginAttemptService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 1;

    @Autowired
    private UserRepository userRepository;

    public boolean processFailedLogin(String username) {
        return userRepository.findByUsername(username)
            .map(user -> handleFailedLogin(user))
            .orElse(false);
    }

    private boolean handleFailedLogin(User user) {
        LocalDateTime now = LocalDateTime.now();

        // Nếu đang bị khóa thì giữ nguyên trạng thái khóa
        if (user.getAccountLockedUntil() != null && now.isBefore(user.getAccountLockedUntil())) {
            return true;
        }

        int attempts = user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts();
        attempts++;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setAccountLockedUntil(now.plusMinutes(LOCK_DURATION_MINUTES));
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
            return true;
        }

        userRepository.save(user);
        return false;
    }

    public void loginSucceeded(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setFailedLoginAttempts(0);
            user.setAccountLockedUntil(null);
            userRepository.save(user);
        });
    }
}