package com.edumoet.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.edumoet.entity.User;
import com.edumoet.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        // Nếu khóa brute force đã hết hạn thì mở khóa lại
        if (user.getAccountLockedUntil() != null && LocalDateTime.now().isAfter(user.getAccountLockedUntil())) {
            user.setAccountLockedUntil(null);
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
        }

        boolean isAccountLocked = false;

        // Khóa do admin ban
        if (user.getIsBanned() != null && user.getIsBanned()) {
            if (user.getBannedUntil() != null) {
                isAccountLocked = LocalDateTime.now().isBefore(user.getBannedUntil());
            } else {
                isAccountLocked = true; // khóa vĩnh viễn
            }
        }

        // Khóa tạm do brute force
        if (user.getAccountLockedUntil() != null && LocalDateTime.now().isBefore(user.getAccountLockedUntil())) {
            isAccountLocked = true;
        }

        // Tài khoản bị disable
        boolean isDisabled = (user.getIsActive() != null && !user.getIsActive());

        List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole())
        );

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(isAccountLocked)
                .credentialsExpired(false)
                .disabled(isDisabled)
                .build();
    }

    @Transactional
    public UserDetails loadUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));

        // Nếu khóa brute force đã hết hạn thì mở khóa lại
        if (user.getAccountLockedUntil() != null && LocalDateTime.now().isAfter(user.getAccountLockedUntil())) {
            user.setAccountLockedUntil(null);
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
        }

        boolean isAccountLocked = false;

        // Khóa do admin ban
        if (user.getIsBanned() != null && user.getIsBanned()) {
            if (user.getBannedUntil() != null) {
                isAccountLocked = LocalDateTime.now().isBefore(user.getBannedUntil());
            } else {
                isAccountLocked = true; // khóa vĩnh viễn
            }
        }

        // Khóa tạm do brute force
        if (user.getAccountLockedUntil() != null && LocalDateTime.now().isBefore(user.getAccountLockedUntil())) {
            isAccountLocked = true;
        }

        // Tài khoản bị disable
        boolean isDisabled = (user.getIsActive() != null && !user.getIsActive());

        List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole())
        );

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(isAccountLocked)
                .credentialsExpired(false)
                .disabled(isDisabled)
                .build();
    }
}