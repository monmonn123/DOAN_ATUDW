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

        // Check if user is banned
        boolean isAccountLocked = false;
        if (user.getIsBanned() != null && user.getIsBanned()) {
            // Check if temporary ban has expired
            if (user.getBannedUntil() != null) {
                isAccountLocked = LocalDateTime.now().isBefore(user.getBannedUntil());
            } else {
                // Permanent ban
                isAccountLocked = true;
            }
        }

        // Check if account is disabled
        boolean isDisabled = (user.getIsActive() != null && !user.getIsActive());

        List<GrantedAuthority> authorities = Collections.singletonList(
            new SimpleGrantedAuthority("ROLE_" + user.getRole())
        );

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(isAccountLocked) // TRUE if banned
                .credentialsExpired(false)
                .disabled(isDisabled) // TRUE if not active
                .build();
    }

    @Transactional
    public UserDetails loadUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));

        // Check if user is banned
        boolean isAccountLocked = false;
        if (user.getIsBanned() != null && user.getIsBanned()) {
            // Check if temporary ban has expired
            if (user.getBannedUntil() != null) {
                isAccountLocked = LocalDateTime.now().isBefore(user.getBannedUntil());
            } else {
                // Permanent ban
                isAccountLocked = true;
            }
        }

        // Check if account is disabled
        boolean isDisabled = (user.getIsActive() != null && !user.getIsActive());

        List<GrantedAuthority> authorities = Collections.singletonList(
            new SimpleGrantedAuthority("ROLE_" + user.getRole())
        );

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(isAccountLocked) // TRUE if banned
                .credentialsExpired(false)
                .disabled(isDisabled) // TRUE if not active
                .build();
    }
}

