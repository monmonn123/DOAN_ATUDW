package com.edumoet.service.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.edumoet.entity.ActivityLog;
import com.edumoet.entity.User;
import com.edumoet.repository.ActivityLogRepository;

import java.time.LocalDateTime;

/**
 * Activity Log Service - Theo dõi hoạt động người dùng
 */
@Service
@Transactional
public class ActivityLogService {

    @Autowired
    private ActivityLogRepository activityLogRepository;

    /**
     * Ghi log hoạt động
     */
    public void log(User user, String action, String entityType, Long entityId, String details) {
        log(user, action, entityType, entityId, details, "0.0.0.0");
    }

    /**
     * Ghi log hoạt động với IP address
     */
    public void log(User user, String action, String entityType, Long entityId, String details, String ipAddress) {
        ActivityLog log = new ActivityLog();
        log.setUser(user);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDetails(details);
        log.setIpAddress(ipAddress);
        log.setCreatedAt(LocalDateTime.now());
        
        activityLogRepository.save(log);
    }

    /**
     * Log đơn giản
     */
    public void logAction(User user, String action, String details) {
        log(user, action, null, null, details);
    }

    /**
     * Log với đầy đủ thông tin (cho AOP Aspect)
     */
    public void logAction(User user, String action, String entityType, Long entityId, String details) {
        log(user, action, entityType, entityId, details);
    }

    /**
     * Log với đầy đủ thông tin và IP address (cho AOP Aspect)
     */
    public void logAction(User user, String action, String entityType, Long entityId, String details, String ipAddress) {
        log(user, action, entityType, entityId, details, ipAddress);
    }

    /**
     * Lấy tất cả log
     */
    public Page<ActivityLog> getAllLogs(Pageable pageable) {
        return activityLogRepository.findAll(pageable);
    }

    /**
     * Lấy log của 1 user
     */
    public Page<ActivityLog> getLogsByUser(User user, Pageable pageable) {
        return activityLogRepository.findByUser(user, pageable);
    }

    /**
     * Lấy log theo action
     */
    public Page<ActivityLog> getLogsByAction(String action, Pageable pageable) {
        return activityLogRepository.findByAction(action, pageable);
    }

    /**
     * Tìm kiếm log
     */
    public Page<ActivityLog> searchLogs(String keyword, Pageable pageable) {
        return activityLogRepository.findByDetailsContaining(keyword, pageable);
    }

    /**
     * Tìm kiếm với nhiều bộ lọc
     */
    public Page<ActivityLog> searchWithFilters(
            String search,
            String action,
            String entityType,
            Long userId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {
        return activityLogRepository.searchWithFilters(
                search, action, entityType, userId, startDate, endDate, pageable
        );
    }

    /**
     * Xóa log cũ (sau 90 ngày)
     */
    public void cleanOldLogs() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(90);
        activityLogRepository.deleteByCreatedAtBefore(cutoffDate);
    }

    /**
     * Xóa log cũ (tùy chỉnh số ngày)
     */
    public long cleanOldLogs(int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        java.util.List<ActivityLog> oldLogs = activityLogRepository.findAll().stream()
                .filter(log -> log.getCreatedAt().isBefore(cutoffDate))
                .collect(java.util.stream.Collectors.toList());
        long count = oldLogs.size();
        activityLogRepository.deleteAll(oldLogs);
        return count;
    }

    /**
     * Export logs to CSV
     */
    public String exportToCSV(java.util.List<ActivityLog> logs) {
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Thời Gian,Người Dùng,Vai Trò,Hành Động,Loại,Chi Tiết,IP\n");
        
        for (ActivityLog log : logs) {
            csv.append(log.getId()).append(",");
            csv.append(log.getCreatedAt()).append(",");
            csv.append(log.getUser() != null ? log.getUser().getUsername() : "Hệ thống").append(",");
            csv.append(log.getUser() != null ? log.getUser().getRole() : "N/A").append(",");
            csv.append(log.getAction()).append(",");
            csv.append(log.getEntityType() != null ? log.getEntityType() : "-").append(",");
            csv.append("\"").append(log.getDetails() != null ? log.getDetails().replace("\"", "\"\"") : "-").append("\",");
            csv.append(log.getIpAddress() != null ? log.getIpAddress() : "N/A").append("\n");
        }
        
        return csv.toString();
    }

    /**
     * Thống kê
     */
    public long countAll() {
        return activityLogRepository.count();
    }

    public long countByAction(String action) {
        return activityLogRepository.countByAction(action);
    }

    public long countToday() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        return activityLogRepository.countByCreatedAtAfter(startOfDay);
    }
    
    // ================== MANAGER FEATURES ==================
    
    @Autowired
    private com.edumoet.repository.UserRepository userRepository;
    
    /**
     * Log action với userId - manager version
     */
    public void logAction(Long userId, String action, String entityType, Long entityId, String details) {
        User user = userRepository.findById(userId)
                .orElse(null); // Allow null user for system actions
        log(user, action, entityType, entityId, details);
    }
    
    /**
     * Find recent activity logs
     */
    public java.util.List<ActivityLog> findRecent() {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 20, 
            org.springframework.data.domain.Sort.by("createdAt").descending());
        return activityLogRepository.findAll(pageable).getContent();
    }
}

