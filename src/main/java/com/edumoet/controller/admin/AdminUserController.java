package com.edumoet.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.edumoet.entity.User;
import com.edumoet.service.common.AdminService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin User Controller - Quản lý người dùng
 */
@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    @Autowired
    private AdminService adminService;

    /**
     * Helper: tạo URL avatar hợp lệ (S3 hoặc fallback)
     */
    private String resolveAvatarUrl(String profileImage, String username, int size) {
        // Always ensure we return a valid URL, never null or empty
        if (profileImage != null && !profileImage.trim().isEmpty()) {
            String trimmed = profileImage.trim();
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                return trimmed;
            }
            // Use consistent path: ltWeb/avatars/ to match all other controllers
            return "https://tungbacket.s3.ap-southeast-1.amazonaws.com/ltWeb/avatars/" + trimmed;
        }
        // Fallback to UI Avatars service with username
        String safeUsername = (username != null && !username.trim().isEmpty()) 
            ? username.trim().replaceAll("\\s+", "+") 
            : "User";
        return "https://ui-avatars.com/api/?name=" +
                safeUsername +
                "&size=" + size +
                "&background=0D6EFD&color=fff";
    }

    /**
     * Danh sách người dùng
     */
    @GetMapping
    public String listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            Model model) {
        
        Sort sort = sortDir.equalsIgnoreCase("asc") ? 
                    Sort.by(sortBy).ascending() : 
                    Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<User> users;
        
        // Apply filters
        if (search != null && !search.trim().isEmpty()) {
            users = adminService.searchUsers(search.trim(), pageable);
            model.addAttribute("search", search);
        } else if (role != null && !role.isEmpty()) {
            users = adminService.getUsersByRole(role, pageable);
            model.addAttribute("role", role);
        } else if ("active".equals(status)) {
            users = adminService.getActiveUsers(pageable);
            model.addAttribute("status", status);
        } else if ("banned".equals(status)) {
            users = adminService.getBannedUsers(pageable);
            model.addAttribute("status", status);
        } else {
            users = adminService.getAllUsers(pageable);
        }
        
        // Statistics
        model.addAttribute("totalUsers", adminService.getAllUsers().size());
        model.addAttribute("activeUsers", adminService.countActiveUsers());
        model.addAttribute("bannedUsers", adminService.countBannedUsers());
        model.addAttribute("newUsersToday", 0); // TODO: implement if needed
        
        // Create user list with resolved avatar URLs
        List<Map<String, Object>> usersWithAvatars = users.getContent().stream().map(user -> {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("username", user.getUsername());
            userMap.put("email", user.getEmail());
            userMap.put("role", user.getRole());
            userMap.put("isBanned", user.getIsBanned());
            userMap.put("createdAt", user.getCreatedAt());
            userMap.put("avatarUrl", resolveAvatarUrl(user.getProfileImage(), user.getUsername(), 32));
            String avatarUrl = resolveAvatarUrl(user.getProfileImage(), user.getUsername(), 32);
            userMap.put("avatarUrl", avatarUrl);
            System.out.println("🔍 [ADMIN USERS] User: " + user.getUsername() 
                + " | profileImage: " + user.getProfileImage() 
                + " | resolved URL: " + avatarUrl);
            return userMap;
        }).toList();
        
        // Create a new Page with the transformed content
        Page<Map<String, Object>> usersPage = new org.springframework.data.domain.PageImpl<>(
            usersWithAvatars,
            users.getPageable(),
            users.getTotalElements()
        );
        
        // Pagination and sorting
        model.addAttribute("users", usersPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", users.getTotalPages());
        model.addAttribute("totalItems", users.getTotalElements());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");
        model.addAttribute("pageTitle", "Quản Lý Người Dùng - Quản Trị");
        
        return "admin/users/list";
    }

    /**
     * Xem chi tiết người dùng
     */
    @GetMapping("/{id}")
    public String viewUser(@PathVariable Long id, Model model) {
        User user = adminService.getUserById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        
        // Get user statistics
        long questionCount = adminService.countUserQuestions(id);
        long answerCount = adminService.countUserAnswers(id);
        
        // Get recent activity (questions and answers)
        Pageable pageable = PageRequest.of(0, 5, Sort.by("createdAt").descending());
        
        model.addAttribute("user", user);
        model.addAttribute("questionCount", questionCount);
        model.addAttribute("answerCount", answerCount);
        model.addAttribute("pageTitle", "Chi Tiết Người Dùng - " + user.getUsername());
        
        return "admin/users/view";
    }

    /**
     * Form chỉnh sửa người dùng
     */
    @GetMapping("/{id}/edit")
    public String editUserForm(@PathVariable Long id, Model model) {
        User user = adminService.getUserById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        
        model.addAttribute("user", user);
        model.addAttribute("pageTitle", "Chỉnh Sửa Người Dùng - " + user.getUsername());
        
        return "admin/users/edit";
    }

    /**
     * Xử lý cập nhật thông tin người dùng
     */
    @PostMapping("/{id}/edit")
    public String editUser(
            @PathVariable Long id,
            @RequestParam String email,
            @RequestParam(required = false) String bio,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String website,
            @RequestParam(required = false) String githubUrl,
            @RequestParam(required = false) String linkedinUrl,
            @RequestParam(required = false) String about,
            @RequestParam(required = false) String croppedImageData,
            RedirectAttributes redirectAttributes) {
        
        try {
            User user = adminService.getUserById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
            
            // Update basic fields
            user.setEmail(email);
            user.setBio(bio);
            user.setLocation(location);
            user.setWebsite(website);
            user.setGithubUrl(githubUrl);
            user.setLinkedinUrl(linkedinUrl);
            user.setAbout(about);
            
            // Handle avatar upload (cropped image)
            System.out.println("📤 [ADMIN EDIT] Checking for avatar upload...");
            System.out.println("   croppedImageData: " + (croppedImageData != null ? "NOT NULL (length: " + croppedImageData.length() + ")" : "NULL"));
            
            if (croppedImageData != null && !croppedImageData.trim().isEmpty()) {
                try {
                    System.out.println("✅ [ADMIN EDIT] Avatar data found, updating...");
                    adminService.updateUserAvatar(id, croppedImageData);
                    System.out.println("✅ [ADMIN EDIT] Avatar updated successfully!");
                } catch (Exception e) {
                    System.err.println("❌ [ADMIN EDIT] Error uploading avatar: " + e.getMessage());
                    e.printStackTrace();
                    redirectAttributes.addFlashAttribute("errorMessage", 
                        "Lỗi khi upload avatar: " + e.getMessage());
                    return "redirect:/admin/users/" + id + "/edit";
                }
            } else {
                System.out.println("ℹ️ [ADMIN EDIT] No avatar data to upload");
            }
            
            adminService.updateUser(user);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "✅ Cập nhật thông tin người dùng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "❌ Lỗi: " + e.getMessage());
        }
        
        return "redirect:/admin/users/" + id;
    }
    
    /**
     * Xóa avatar người dùng
     */
    @GetMapping("/{id}/remove-picture")
    public String removeUserPicture(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminService.removeUserAvatar(id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "✅ Đã xóa avatar người dùng!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "❌ Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/users/" + id + "/edit";
    }

    /**
     * Khóa tài khoản tạm thời
     */
    @PostMapping("/{id}/ban")
    public String banUser(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "Violated community guidelines") String reason,
            @RequestParam(required = false) Integer days,
            RedirectAttributes redirectAttributes) {
        
        try {
            // Default reason if empty
            String banReason = (reason == null || reason.trim().isEmpty()) 
                    ? "Violated community guidelines" 
                    : reason;
            
            if (days != null && days > 0) {
                LocalDateTime bannedUntil = LocalDateTime.now().plusDays(days);
                adminService.banUser(id, banReason, bannedUntil);
                redirectAttributes.addFlashAttribute("successMessage", 
                    "Đã khóa tài khoản trong " + days + " ngày!");
            } else {
                adminService.banUserPermanently(id, banReason);
                redirectAttributes.addFlashAttribute("successMessage", 
                    "Đã khóa tài khoản vĩnh viễn!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Lỗi: " + e.getMessage());
        }
        
        return "redirect:/admin/users/" + id;
    }

    /**
     * Mở khóa tài khoản
     */
    @PostMapping("/{id}/unban")
    public String unbanUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminService.unbanUser(id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Đã mở khóa tài khoản thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Lỗi: " + e.getMessage());
        }
        
        return "redirect:/admin/users/" + id;
    }

    /**
     * Vô hiệu hóa tài khoản
     */
    @PostMapping("/{id}/deactivate")
    public String deactivateUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminService.deactivateUser(id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Đã vô hiệu hóa tài khoản!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Lỗi: " + e.getMessage());
        }
        
        return "redirect:/admin/users/" + id;
    }

    /**
     * Kích hoạt tài khoản
     */
    @PostMapping("/{id}/activate")
    public String activateUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminService.activateUser(id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Đã kích hoạt tài khoản!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Lỗi: " + e.getMessage());
        }
        
        return "redirect:/admin/users/" + id;
    }

    /**
     * Đổi mật khẩu người dùng
     */
    @PostMapping("/{id}/reset-password")
    public String resetPassword(
            @PathVariable Long id,
            @RequestParam String newPassword,
            RedirectAttributes redirectAttributes) {
        
        System.out.println("🔐 AdminUserController.resetPassword endpoint called");
        System.out.println("   Path ID: " + id);
        System.out.println("   Password received: " + (newPassword != null && !newPassword.isEmpty() ? "Yes" : "No"));
        
        try {
            adminService.resetUserPassword(id, newPassword);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Đặt lại mật khẩu thành công!");
            System.out.println("   ✅ Redirect with success message");
        } catch (Exception e) {
            System.err.println("   ❌ Error: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Lỗi: " + e.getMessage());
        }
        
        return "redirect:/admin/users/" + id;
    }

    /**
     * Thay đổi vai trò người dùng
     */
    @PostMapping("/{id}/change-role")
    public String changeRole(
            @PathVariable Long id,
            @RequestParam String role,
            RedirectAttributes redirectAttributes) {
        
        System.out.println("👤 AdminUserController.changeRole endpoint called");
        System.out.println("   Path ID: " + id);
        System.out.println("   Role parameter: " + role);
        
        try {
            adminService.changeUserRole(id, role);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Đã thay đổi vai trò thành " + role + " thành công!");
            System.out.println("   ✅ Redirect with success message");
        } catch (Exception e) {
            System.err.println("   ❌ Error: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Lỗi: " + e.getMessage());
        }
        
        return "redirect:/admin/users/" + id;
    }

    /**
     * Xóa người dùng
     */
    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminService.deleteUser(id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Đã xóa người dùng!");
            return "redirect:/admin/users";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Lỗi: " + e.getMessage());
            return "redirect:/admin/users/" + id;
        }
    }
}
