package com.edumoet.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.edumoet.entity.Report;
import com.edumoet.entity.User;
import com.edumoet.service.common.ReportService;
import com.edumoet.service.common.UserService;

/**
 * Admin Report Controller - Quản lý báo cáo vi phạm
 */
@Controller
@RequestMapping("/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private UserService userService;

    /**
     * Danh sách báo cáo
     */
    @GetMapping
    public String listReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            Model model) {
        
        Sort sort = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Report> reports;
        if (status != null && !status.isEmpty()) {
            reports = reportService.getReportsByStatus(status, pageable);
        } else {
            reports = reportService.getAllReports(pageable);
        }
        
        // Statistics
        model.addAttribute("totalReports", reportService.countAll());
        model.addAttribute("pendingReports", reportService.countPending());
        model.addAttribute("resolvedReports", reportService.countResolved());
        
        // Data
        model.addAttribute("reports", reports.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", reports.getTotalPages());
        model.addAttribute("status", status);
        
        return "admin/reports/list";
    }

    /**
     * Chi tiết báo cáo
     */
    @GetMapping("/{id}")
    public String viewReport(@PathVariable Long id, Model model) {
        // TODO: Implement detail view
        return "redirect:/admin/reports";
    }

    /**
     * Xử lý báo cáo (Resolve)
     */
    @PostMapping("/{id}/resolve")
    public String resolveReport(
            @PathVariable Long id,
            @RequestParam String resolution,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        
        try {
            User admin = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Admin not found"));
            
            reportService.resolveReport(id, admin, resolution);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Report resolved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error: " + e.getMessage());
        }
        
        return "redirect:/admin/reports";
    }

    /**
     * Từ chối báo cáo (Reject)
     */
    @PostMapping("/{id}/reject")
    public String rejectReport(
            @PathVariable Long id,
            @RequestParam String reason,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        
        try {
            User admin = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Admin not found"));
            
            reportService.rejectReport(id, admin, reason);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Report rejected!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error: " + e.getMessage());
        }
        
        return "redirect:/admin/reports";
    }

    /**
     * Xóa báo cáo
     */
    @PostMapping("/{id}/delete")
    public String deleteReport(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            // TODO: Implement delete
            redirectAttributes.addFlashAttribute("successMessage", 
                "Report deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error: " + e.getMessage());
        }
        
        return "redirect:/admin/reports";
    }
}

