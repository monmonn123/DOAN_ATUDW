package com.edumoet.controller.manager;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.edumoet.entity.Notification;
import com.edumoet.entity.User;
import com.edumoet.service.common.ActivityLogService;
import com.edumoet.service.common.NotificationService;
import com.edumoet.service.common.UserService;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/manager/notifications")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class NotificationController {

    private final NotificationService notiService;
    private final ActivityLogService logService;
    private final UserService userService;

    public NotificationController(NotificationService notiService, 
                                   ActivityLogService logService,
                                   UserService userService) {
        this.notiService = notiService;
        this.logService = logService;
        this.userService = userService;
    }

    // 🔹 Danh sách thông báo
    @GetMapping
    public String listNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        
        Sort sort = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Notification> notifications = notiService.getAllNotifications(pageable);
        
        model.addAttribute("notifications", notifications);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", notifications.getTotalPages());
        model.addAttribute("totalItems", notifications.getTotalElements());
        model.addAttribute("pageTitle", "Quản lý thông báo");
        
        return "manager/notification/list";
    }

    // 🔹 Form gửi thông báo
    @GetMapping("/send")
    public String showSendForm(Principal principal, Model model) {
        // Get all users for dropdown
        List<User> users = userService.getAllUsers(PageRequest.of(0, 100)).getContent();
        model.addAttribute("users", users);
        model.addAttribute("pageTitle", "Gửi thông báo");
        return "manager/notification/send-form";
    }

    // 🔹 Gửi thông báo
    @PostMapping("/send")
    public String sendNotification(@RequestParam String message,
                                   @RequestParam(required = false) String link,
                                   @RequestParam(defaultValue = "SYSTEM") String type,
                                   @RequestParam(defaultValue = "all") String target,
                                   Principal principal,
                                   RedirectAttributes redirectAttributes) {
        try {
            // Get sender from logged-in user
            User sender = userService.findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            Long senderId = sender.getId();
            
            // Check if target is a role
            if (target.startsWith("role:")) {
                String role = target.substring(5); // Remove "role:" prefix
                int count = notiService.notifyByRole(role, type, message, link, senderId);
                logService.logAction(senderId, "SEND_ROLE_NOTIFICATION", "NOTIFICATION", null,
                        "Manager sent notification to role " + role);
                redirectAttributes.addFlashAttribute("successMessage", 
                    "✅ Đã gửi thông báo đến " + count + " người dùng có role " + role + "!");
            } else if ("all".equalsIgnoreCase(target)) {
                notiService.notifyAllUsers(type, message, link, senderId);
                logService.logAction(senderId, "SEND_GLOBAL_NOTIFICATION", "NOTIFICATION", null,
                        "Manager sent global notification");
                redirectAttributes.addFlashAttribute("successMessage", 
                    "✅ Đã gửi thông báo đến tất cả người dùng!");
            } else {
                try {
                    Long userId = Long.parseLong(target);
                    notiService.notifyUser(userId, type, message, link, senderId);
                    logService.logAction(senderId, "SEND_USER_NOTIFICATION", "NOTIFICATION", userId,
                            "Sent notification to user " + userId);
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
        return "redirect:/manager/notifications/send";
    }
    
    // 🔹 Xóa 1 notification (không được xóa notification của Admin)
    @PostMapping("/{id}/delete")
    public String deleteNotification(@PathVariable Long id,
                                      Principal principal,
                                      RedirectAttributes redirectAttributes) {
        try {
            Notification notification = notiService.findById(id);
            
            // Check if sender is Admin - Manager CANNOT delete Admin's notifications
            if (notification.getSender() != null && 
                notification.getSender().getRole() != null &&
                notification.getSender().getRole().equalsIgnoreCase("ADMIN")) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "❌ Bạn không thể xóa thông báo của Admin!");
                return "redirect:/manager/notifications";
            }
            
            notiService.deleteNotification(id);
            logService.logAction(1L, "DELETE_NOTIFICATION", "NOTIFICATION", id, 
                "Deleted notification " + id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "✅ Đã xóa thông báo!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "❌ Lỗi: " + e.getMessage());
        }
        return "redirect:/manager/notifications";
    }
    
    // 🔹 Xóa hàng loạt notifications (không xóa notification của Admin)
    @PostMapping("/delete-batch")
    public String deleteBatchNotifications(@RequestParam("notificationIds") List<Long> notificationIds,
                                            Principal principal,
                                            RedirectAttributes redirectAttributes) {
        try {
            int deletedCount = 0;
            int skippedCount = 0;
            
            for (Long id : notificationIds) {
                try {
                    Notification notification = notiService.findById(id);
                    
                    // Skip if sender is Admin
                    if (notification.getSender() != null && 
                        notification.getSender().getRole() != null &&
                        notification.getSender().getRole().equalsIgnoreCase("ADMIN")) {
                        skippedCount++;
                        continue;
                    }
                    
                    notiService.deleteNotification(id);
                    deletedCount++;
                } catch (Exception e) {
                    skippedCount++;
                }
            }
            
            logService.logAction(1L, "DELETE_BATCH_NOTIFICATIONS", "NOTIFICATION", null, 
                "Deleted " + deletedCount + " notifications");
            
            if (deletedCount > 0) {
                redirectAttributes.addFlashAttribute("successMessage", 
                    "✅ Đã xóa " + deletedCount + " thông báo!" + 
                    (skippedCount > 0 ? " (Bỏ qua " + skippedCount + " thông báo của Admin)" : ""));
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "❌ Không thể xóa thông báo nào (tất cả đều của Admin)");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "❌ Lỗi: " + e.getMessage());
        }
        return "redirect:/manager/notifications";
    }
}
