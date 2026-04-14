package com.edumoet.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.edumoet.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    Boolean existsByUsername(String username);
    
    Boolean existsByEmail(String email);
    
    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<User> searchUsers(@Param("search") String search, Pageable pageable);
    
    @Query("SELECT u FROM User u ORDER BY u.reputation DESC")
    Page<User> findAllByOrderByReputationDesc(Pageable pageable);
    
    @Query("SELECT u FROM User u ORDER BY u.createdAt DESC")
    Page<User> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    // Admin features
    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<User> findByUsernameContainingOrEmailContaining(@Param("keyword") String username, @Param("keyword") String email, Pageable pageable);
    
    List<User> findByRole(String role);
    
    Page<User> findByRole(String role, Pageable pageable);
    
    Page<User> findByIsBanned(Boolean isBanned, Pageable pageable);
    
    Page<User> findByIsActive(Boolean isActive, Pageable pageable);
    
    long countByIsActive(Boolean isActive);
    
    long countByIsActiveTrue();
    
    long countByIsBanned(Boolean isBanned);
    
    long countByRole(String role);
    
    // For statistics - fetch users by reputation (no join fetch to avoid DISTINCT issue)
    @Query("SELECT u FROM User u ORDER BY u.reputation DESC")
    List<User> findTopUsersByReputation(Pageable pageable);
}

