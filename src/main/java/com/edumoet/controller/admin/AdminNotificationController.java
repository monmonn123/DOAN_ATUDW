package com.edumoet.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.edumoet.service.common.NotificationService;

/**
 * Admin Notification Controller - Gửi thông báo hệ thống
 */
@Controller
@RequestMapping("/admin/notifications")
@PreAuthorize("hasRole('ADMIN')")
public class AdminNotificationController {

    @Autowired
    private NotificationService notificationService;

    /**
     * Trang gửi thông báo
     */
    @GetMapping("/send")
    public String showSendPage(Model model) {
        model.addAttribute("pageTitle", "Send System Notification - Admin");
        
        // Thống kê
        model.addAttribute("totalNotifications", notificationService.countAll());
        model.addAttribute("unreadNotifications", notificationService.countUnread());
        
        return "admin/notifications/send";
    }

    /**
     * Gửi thông báo (unified endpoint)
     */
    @PostMapping("/send")
    public String sendNotification(
            @RequestParam String message,
            @RequestParam(required = false) String link,
            @RequestParam(defaultValue = "SYSTEM") String type,
            @RequestParam(defaultValue = "all") String target,
            @RequestParam(required = false) Long senderId, // Admin ID from security context
            RedirectAttributes redirectAttributes) {
        
        try {
            // Use a fixed admin sender ID = 1 (or get from security context)
            Long adminSenderId = (senderId != null) ? senderId : 1L;
            
            // Check if target is a role
            if (target.startsWith("role:")) {
                String role = target.substring(5); // Remove "role:" prefix
                int count = notificationService.broadcastToRole(role, message, type);
                redirectAttributes.addFlashAttribute("successMessage", 
                    "✅ Đã gửi thông báo đến " + count + " người dùng có role " + role + "!");
            } else if ("all".equalsIgnoreCase(target)) {
                int count = notificationService.broadcastToAll(message, type);
                redirectAttributes.addFlashAttribute("successMessage", 
                    "✅ Đã gửi thông báo đến tất cả người dùng (" + count + " người)!");
            } else {
                // Send to specific user by ID
                try {
                    Long userId = Long.parseLong(target);
                    notificationService.notifyUser(userId, type, message, link, adminSenderId);
                    redirectAttributes.addFlashAttribute("successMessage", 
                        "✅ Đã gửi thông báo đến user ID " + userId + "!");
                } catch (NumberFormatException e) {
                    redirectAttributes.addFlashAttribute("errorMessage", 
                        "❌ User ID không hợp lệ!");
                }
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "❌ Lỗi: " + e.getMessage());
        }
        
        return "redirect:/admin/notifications/send";
    }

    /**
     * Danh sách thông báo
     */
    @GetMapping
    public String listNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        
        org.springframework.data.domain.Sort sort = org.springframework.data.domain.Sort.by("createdAt").descending();
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, sort);
        org.springframework.data.domain.Page<com.edumoet.entity.Notification> notifications = notificationService.getAllNotifications(pageable);
        
        model.addAttribute("notifications", notifications);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", notifications.getTotalPages());
        model.addAttribute("totalItems", notifications.getTotalElements());
        model.addAttribute("pageTitle", "Quản lý thông báo - Admin");
        
        return "admin/notifications/list";
    }
    
    /**
     * Xóa 1 notification (Admin có quyền xóa tất cả)
     */
    @PostMapping("/{id}/delete")
    public String deleteNotification(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            notificationService.deleteNotification(id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "✅ Đã xóa thông báo!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "❌ Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/notifications";
    }
    
    /**
     * Xóa hàng loạt notifications (Admin có quyền xóa tất cả)
     */
    @PostMapping("/delete-batch")
    public String deleteBatchNotifications(
            @RequestParam("notificationIds") java.util.List<Long> notificationIds,
            RedirectAttributes redirectAttributes) {
        try {
            int deletedCount = 0;
            
            for (Long id : notificationIds) {
                try {
                    notificationService.deleteNotification(id);
                    deletedCount++;
                } catch (Exception e) {
                    // Skip if error
                }
            }
            
            if (deletedCount > 0) {
                redirectAttributes.addFlashAttribute("successMessage", 
                    "✅ Đã xóa " + deletedCount + " thông báo!");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "❌ Không thể xóa thông báo nào!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "❌ Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/notifications";
    }
}

