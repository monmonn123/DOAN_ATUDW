package com.edumoet.controller.manager;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.edumoet.entity.Report;
import com.edumoet.service.common.ActivityLogService;
import com.edumoet.service.common.ReportService;

@Controller("managerReportController")
@RequestMapping("/manager/reports")
public class ReportController {

    private final ReportService reportService;
    private final ActivityLogService logService;

    public ReportController(ReportService reportService, ActivityLogService logService) {
        this.reportService = reportService;
        this.logService = logService;
    }

    @GetMapping
    public String listReports(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size,
                              Model model) {
        Page<Report> reports = reportService.listOpenReports(page, size);
        model.addAttribute("reports", reports.getContent());
        model.addAttribute("page", page);
        model.addAttribute("totalPages", reports.getTotalPages());
        return "manager/report/list";
    }

    @GetMapping("/{id}")
    public String viewReport(@PathVariable Long id, Model model) {
        Report report = reportService.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found"));
        
        model.addAttribute("report", report);
        model.addAttribute("pageTitle", "Chi Tiết Báo Cáo #" + id);
        
        return "manager/report/view";
    }

    @PostMapping("/{id}/resolve")
    public String resolveReport(@PathVariable Long id,
                                @RequestParam String status,
                                @RequestParam String note) {
        reportService.resolveReport(id, status, note, 1L);
        logService.logAction(1L, "RESOLVE_REPORT", "REPORT", id, "Resolved report: " + note);
        return "redirect:/manager/reports";
    }
}
