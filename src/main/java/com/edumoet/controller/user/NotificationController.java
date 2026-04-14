package com.edumoet.controller.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.edumoet.entity.Notification;
import com.edumoet.entity.User;
import com.edumoet.service.common.NotificationService;
import com.edumoet.service.common.UserService;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User Notification Controller - Xem và quản lý thông báo của user
 */
@Controller("userNotificationController")
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    /**
     * Danh sách thông báo của user hiện tại
     */
    @GetMapping
    public String listNotifications(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        User currentUser = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Notification> allNotifications = notificationService.getNotificationsByUser(currentUser);
        List<Notification> unreadNotifications = notificationService.getUnreadNotifications(currentUser);

        model.addAttribute("notifications", allNotifications);
        model.addAttribute("unreadCount", unreadNotifications.size());
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("pageTitle", "Notifications");

        return "notifications/list";
    }

    /**
     * API: Lấy số lượng thông báo chưa đọc (cho navbar badge)
     */
    @GetMapping("/api/unread-count")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUnreadCount(Principal principal) {
        if (principal == null) {
            return ResponseEntity.ok(Map.of("count", 0));
        }

        User currentUser = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Notification> unread = notificationService.getUnreadNotifications(currentUser);
        
        Map<String, Object> response = new HashMap<>();
        response.put("count", unread.size());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Đánh dấu 1 thông báo đã đọc
     */
    @PostMapping("/{id}/mark-read")
    @ResponseBody
    public ResponseEntity<Map<String, String>> markAsRead(
            @PathVariable Long id,
            Principal principal) {
        
        try {
            if (principal == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            notificationService.markAsRead(id);
            
            return ResponseEntity.ok(Map.of("status", "success", "message", "Marked as read"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Đánh dấu tất cả thông báo đã đọc
     */
    @PostMapping("/mark-all-read")
    public String markAllAsRead(
            Principal principal,
            RedirectAttributes redirectAttributes) {
        
        try {
            if (principal == null) {
                return "redirect:/login";
            }

            User currentUser = userService.findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            notificationService.markAllAsRead(currentUser);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "✅ All notifications marked as read!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "❌ Error: " + e.getMessage());
        }

        return "redirect:/notifications";
    }

    /**
     * Xóa 1 thông báo
     */
    @PostMapping("/{id}/delete")
    public String deleteNotification(
            @PathVariable Long id,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        
        try {
            if (principal == null) {
                return "redirect:/login";
            }

            notificationService.deleteNotification(id);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "✅ Notification deleted!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "❌ Error: " + e.getMessage());
        }

        return "redirect:/notifications";
    }

    /**
     * API: Lấy danh sách thông báo (cho dropdown hoặc AJAX)
     */
    @GetMapping("/api/recent")
    @ResponseBody
    public ResponseEntity<List<Notification>> getRecentNotifications(
            @RequestParam(defaultValue = "10") int limit,
            Principal principal) {
        
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        User currentUser = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Notification> notifications = notificationService.getNotificationsByUser(currentUser);
        
        // Limit results
        if (notifications.size() > limit) {
            notifications = notifications.subList(0, limit);
        }

        return ResponseEntity.ok(notifications);
    }
}

