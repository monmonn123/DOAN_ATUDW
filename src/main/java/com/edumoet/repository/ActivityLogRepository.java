package com.edumoet.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.edumoet.entity.ActivityLog;
import com.edumoet.entity.User;

import java.time.LocalDateTime;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    
    Page<ActivityLog> findByUser(User user, Pageable pageable);
    
    Page<ActivityLog> findByAction(String action, Pageable pageable);
    
    Page<ActivityLog> findByDetailsContaining(String keyword, Pageable pageable);
    
    Page<ActivityLog> findByEntityType(String entityType, Pageable pageable);
    
    Page<ActivityLog> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    void deleteByCreatedAtBefore(LocalDateTime date);
    
    long countByAction(String action);
    
    long countByCreatedAtAfter(LocalDateTime date);
    
    // For user deletion
    void deleteByUser(User user);
    
    // Advanced search with multiple filters
    @Query("SELECT a FROM ActivityLog a WHERE " +
           "(:search IS NULL OR :search = '' OR CAST(a.details AS string) LIKE CONCAT('%', :search, '%')) AND " +
           "(:action IS NULL OR :action = '' OR a.action = :action) AND " +
           "(:entityType IS NULL OR :entityType = '' OR a.entityType = :entityType) AND " +
           "(:userId IS NULL OR a.user.id = :userId) AND " +
           "(:startDate IS NULL OR a.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR a.createdAt <= :endDate)")
    Page<ActivityLog> searchWithFilters(
            @Param("search") String search,
            @Param("action") String action,
            @Param("entityType") String entityType,
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
}
