package com.edumoet.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import com.edumoet.entity.User;
import com.edumoet.service.common.ActivityLogService;
import com.edumoet.service.common.UserService;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

/**
 * Activity Logging Aspect - Tự động ghi lại tất cả hoạt động của Admin và Manager
 */
@Aspect
@Component
public class ActivityLoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(ActivityLoggingAspect.class);

    @Autowired
    private ActivityLogService activityLogService;

    @Autowired
    private UserService userService;

    @Autowired(required = false)
    private HttpServletRequest request;

    /**
     * Log tất cả các method trong AdminController
     */
    @AfterReturning(
        pointcut = "execution(* com.edumoet.controller.admin..*(..))",
        returning = "result"
    )
    public void logAdminActivity(JoinPoint joinPoint, Object result) {
        try {
            logActivity(joinPoint, "ADMIN");
        } catch (Exception e) {
            logger.error("Error logging admin activity", e);
        }
    }

    /**
     * Log tất cả các method trong ManagerController
     */
    @AfterReturning(
        pointcut = "execution(* com.edumoet.controller.manager..*(..))",
        returning = "result"
    )
    public void logManagerActivity(JoinPoint joinPoint, Object result) {
        try {
            logActivity(joinPoint, "MANAGER");
        } catch (Exception e) {
            logger.error("Error logging manager activity", e);
        }
    }

    /**
     * Ghi log hoạt động
     */
    private void logActivity(JoinPoint joinPoint, String role) {
        // Get current user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return;
        }

        String username = auth.getName();
        Optional<User> userOpt = userService.findByUsername(username);
        if (!userOpt.isPresent()) {
            return;
        }

        User user = userOpt.get();
        
        // Get method info
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        
        // Skip certain methods
        if (shouldSkipLogging(methodName, className)) {
            return;
        }

        // Determine action
        String action = determineAction(methodName, className);
        
        // Get entity type and details
        String entityType = determineEntityType(className);
        String details = buildDetails(joinPoint, methodName, className);
        
        // Get IP address
        String ipAddress = "0.0.0.0";
        if (request != null) {
            ipAddress = getClientIP(request);
        }

        // Log the activity
        logger.info("📝 Logging {} activity: {} - {} - {}", role, username, action, details);
        
        try {
            activityLogService.logAction(
                user,
                action,
                entityType,
                extractEntityId(joinPoint),
                details,
                ipAddress
            );
        } catch (Exception e) {
            logger.error("Failed to save activity log", e);
        }
    }

    /**
     * Skip logging for certain methods
     */
    private boolean shouldSkipLogging(String methodName, String className) {
        // Skip GET methods that just display pages
        String[] skipMethods = {
            "listUsers", "listQuestions", "listNotifications", "listActivityLogs",
            "showSendPage", "settings", "dashboard", "moderationDashboard",
            "viewQuestion", "viewUser", "viewTag", "statisticsDashboard"
        };
        
        for (String skip : skipMethods) {
            if (methodName.equals(skip) || methodName.startsWith("show") || methodName.startsWith("view")) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Determine action name from method
     */
    private String determineAction(String methodName, String className) {
        // Create/Add
        if (methodName.startsWith("create") || methodName.startsWith("add")) {
            return "CREATE_" + extractEntity(methodName, className);
        }
        
        // Update/Edit
        if (methodName.startsWith("update") || methodName.startsWith("edit")) {
            return "UPDATE_" + extractEntity(methodName, className);
        }
        
        // Delete/Remove
        if (methodName.startsWith("delete") || methodName.startsWith("remove")) {
            return "DELETE_" + extractEntity(methodName, className);
        }
        
        // Approve
        if (methodName.startsWith("approve")) {
            return "APPROVE_" + extractEntity(methodName, className);
        }
        
        // Reject
        if (methodName.startsWith("reject")) {
            return "REJECT_" + extractEntity(methodName, className);
        }
        
        // Ban/Unban
        if (methodName.equals("banUser")) {
            return "BAN_USER";
        }
        if (methodName.equals("unbanUser")) {
            return "UNBAN_USER";
        }
        
        // Lock/Unlock
        if (methodName.contains("lock") && methodName.contains("Question")) {
            return methodName.contains("unlock") ? "UNLOCK_QUESTION" : "LOCK_QUESTION";
        }
        
        // Pin/Unpin
        if (methodName.contains("pin") && methodName.contains("Question")) {
            return methodName.contains("unpin") ? "UNPIN_QUESTION" : "PIN_QUESTION";
        }
        
        // Send notification
        if (methodName.contains("sendNotification") || methodName.contains("broadcast")) {
            return "SEND_NOTIFICATION";
        }
        
        // Upload
        if (methodName.startsWith("upload")) {
            return "UPLOAD_" + extractEntity(methodName, className);
        }
        
        // Default
        return methodName.toUpperCase();
    }

    /**
     * Extract entity name from method or class
     */
    private String extractEntity(String methodName, String className) {
        // From method name
        if (methodName.contains("Question")) return "QUESTION";
        if (methodName.contains("Answer")) return "ANSWER";
        if (methodName.contains("User")) return "USER";
        if (methodName.contains("Tag")) return "TAG";
        if (methodName.contains("Comment")) return "COMMENT";
        if (methodName.contains("Notification")) return "NOTIFICATION";
        
        // From class name
        if (className.contains("Question")) return "QUESTION";
        if (className.contains("Answer")) return "ANSWER";
        if (className.contains("User")) return "USER";
        if (className.contains("Tag")) return "TAG";
        if (className.contains("Comment")) return "COMMENT";
        if (className.contains("Notification")) return "NOTIFICATION";
        
        return "UNKNOWN";
    }

    /**
     * Determine entity type from class name
     */
    private String determineEntityType(String className) {
        if (className.contains("Question")) return "QUESTION";
        if (className.contains("Answer")) return "ANSWER";
        if (className.contains("User")) return "USER";
        if (className.contains("Tag")) return "TAG";
        if (className.contains("Comment")) return "COMMENT";
        if (className.contains("Notification")) return "NOTIFICATION";
        if (className.contains("Moderation")) return "MODERATION";
        if (className.contains("Statistics")) return "STATISTICS";
        if (className.contains("Dashboard")) return "DASHBOARD";
        if (className.contains("ActivityLog")) return "ACTIVITY_LOG";
        if (className.contains("Report")) return "REPORT";
        return "SYSTEM";
    }

    /**
     * Extract entity ID from method parameters
     */
    private Long extractEntityId(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        
        // Look for @PathVariable Long id
        for (Object arg : args) {
            if (arg instanceof Long) {
                return (Long) arg;
            }
        }
        
        return null;
    }

    /**
     * Build details string
     */
    private String buildDetails(JoinPoint joinPoint, String methodName, String className) {
        StringBuilder details = new StringBuilder();
        details.append(role(className)).append(" thực hiện: ");
        details.append(translateAction(methodName));
        
        // Add parameters info
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            for (Object arg : args) {
                if (arg instanceof String && ((String) arg).length() < 100) {
                    details.append(" | Tham số: ").append(arg);
                    break;
                } else if (arg instanceof Long) {
                    details.append(" | ID: ").append(arg);
                    break;
                }
            }
        }
        
        return details.toString();
    }

    /**
     * Get role from class name
     */
    private String role(String className) {
        if (className.contains("Admin")) return "Admin";
        if (className.contains("Manager")) return "Manager";
        return "User";
    }

    /**
     * Translate action to Vietnamese
     */
    private String translateAction(String methodName) {
        if (methodName.contains("create") || methodName.contains("add")) return "Tạo mới";
        if (methodName.contains("update") || methodName.contains("edit")) return "Cập nhật";
        if (methodName.contains("delete") || methodName.contains("remove")) return "Xóa";
        if (methodName.contains("approve")) return "Duyệt";
        if (methodName.contains("reject")) return "Từ chối";
        if (methodName.contains("ban")) return "Khóa tài khoản";
        if (methodName.contains("unban")) return "Mở khóa tài khoản";
        if (methodName.contains("lock")) return "Khóa";
        if (methodName.contains("unlock")) return "Mở khóa";
        if (methodName.contains("pin")) return "Ghim";
        if (methodName.contains("unpin")) return "Bỏ ghim";
        if (methodName.contains("send") || methodName.contains("broadcast")) return "Gửi thông báo";
        if (methodName.contains("upload")) return "Tải lên";
        return methodName;
    }

    /**
     * Get client IP address
     */
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}

