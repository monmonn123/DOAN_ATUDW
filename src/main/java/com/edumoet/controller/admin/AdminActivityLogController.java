package com.edumoet.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.edumoet.entity.ActivityLog;
import com.edumoet.service.common.ActivityLogService;
import com.edumoet.service.common.AdminService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Admin Activity Log Controller - Theo dõi hoạt động
 */
@Controller
@RequestMapping("/admin/activity-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AdminActivityLogController {

    @Autowired
    private ActivityLogService activityLogService;

    @Autowired
    private AdminService adminService;

    /**
     * Danh sách activity logs với bộ lọc nâng cao
     */
    @GetMapping
    public String listActivityLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {
        
        Sort sort = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // Convert LocalDate to LocalDateTime
        LocalDateTime startDateTime = startDate != null ? LocalDateTime.of(startDate, LocalTime.MIN) : null;
        LocalDateTime endDateTime = endDate != null ? LocalDateTime.of(endDate, LocalTime.MAX) : null;
        
        // Use advanced search if any filter is applied
        var logs = activityLogService.searchWithFilters(
                search, action, entityType, userId, startDateTime, endDateTime, pageable
        );
        
        model.addAttribute("logs", logs);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", logs.getTotalPages());
        model.addAttribute("totalItems", logs.getTotalElements());
        
        // Keep filter values for the form
        model.addAttribute("search", search);
        model.addAttribute("action", action);
        model.addAttribute("entityType", entityType);
        model.addAttribute("userId", userId);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        
        // Statistics
        model.addAttribute("totalLogs", activityLogService.countAll());
        model.addAttribute("logsToday", activityLogService.countToday());
        model.addAttribute("loginCount", activityLogService.countByAction("LOGIN"));
        model.addAttribute("questionCount", activityLogService.countByAction("CREATE"));
        
        // Get all users for filter dropdown (Admin and Manager only)
        model.addAttribute("allUsers", adminService.getAllUsers().stream()
                .filter(u -> "ADMIN".equals(u.getRole()) || "MANAGER".equals(u.getRole()))
                .toList());
        
        model.addAttribute("pageTitle", "Activity Logs - Admin");
        
        return "admin/activity-logs/list";
    }

    /**
     * Xóa log cũ
     */
    @PostMapping("/clean-old-logs")
    public String cleanOldLogs(
            @RequestParam(defaultValue = "90") int daysToKeep,
            RedirectAttributes redirectAttributes) {
        
        try {
            long deletedCount = activityLogService.cleanOldLogs(daysToKeep);
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Đã xóa " + deletedCount + " log cũ hơn " + daysToKeep + " ngày!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "Lỗi khi xóa log cũ: " + e.getMessage());
        }
        
        return "redirect:/admin/activity-logs";
    }

    /**
     * Export logs to CSV
     */
    @GetMapping("/export")
    public ResponseEntity<String> exportLogs(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Sort sort = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(0, 10000, sort); // Limit to 10000 records
        
        LocalDateTime startDateTime = startDate != null ? LocalDateTime.of(startDate, LocalTime.MIN) : null;
        LocalDateTime endDateTime = endDate != null ? LocalDateTime.of(endDate, LocalTime.MAX) : null;
        
        var logs = activityLogService.searchWithFilters(
                search, action, entityType, userId, startDateTime, endDateTime, pageable
        );
        
        List<ActivityLog> logList = logs.getContent();
        String csv = activityLogService.exportToCSV(logList);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", "activity-logs-" + LocalDate.now() + ".csv");
        headers.add("Content-Type", "text/csv; charset=UTF-8");
        
        return new ResponseEntity<>(csv, headers, HttpStatus.OK);
    }

    /**
     * Get log detail (AJAX)
     */
    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<ActivityLog> getLogDetail(@PathVariable Long id) {
        ActivityLog log = activityLogService.getAllLogs(PageRequest.of(0, 1))
                .getContent().stream()
                .filter(l -> l.getId().equals(id))
                .findFirst()
                .orElse(null);
        
        if (log == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(log);
    }
}

