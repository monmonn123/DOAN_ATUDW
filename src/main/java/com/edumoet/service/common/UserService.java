package com.edumoet.service.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.edumoet.entity.User;
import com.edumoet.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User registerUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already exists!");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists!");
        }
        
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("USER");
        user.setReputation(1);
        user.setViews(0);
        user.setPoints(0);
        user.setLevel(1);
        user.setIsActive(true);
        user.setIsBanned(false);
        user.setEmailVerified(false);
        user.setTwoFactorEnabled(false);
        
        return userRepository.save(user);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public Page<User> getUsersByReputation(Pageable pageable) {
        return userRepository.findAllByOrderByReputationDesc(pageable);
    }

    public Page<User> searchUsers(String search, Pageable pageable) {
        return userRepository.searchUsers(search, pageable);
    }

    public User updateUser(User user) {
        return userRepository.save(user);
    }

    public void incrementReputation(User user, int points) {
        user.setReputation(user.getReputation() + points);
        userRepository.save(user);
    }

    public void incrementViews(User user) {
        user.setViews(user.getViews() + 1);
        userRepository.save(user);
    }
    
    /**
     * Search users by username or email (for messaging)
     */
    public List<User> searchByUsernameOrEmail(String query) {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = userRepository.findByUsernameContainingOrEmailContaining(query, query, pageable);
        return page.getContent();
    }
    
    // ================== MANAGER FEATURES ==================
    
    /**
     * Count active members
     */
    public long countActiveMembers() {
        return userRepository.countByIsActiveTrue();
    }
}

