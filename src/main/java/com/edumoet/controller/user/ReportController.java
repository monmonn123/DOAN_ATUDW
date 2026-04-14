package com.edumoet.controller.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.edumoet.entity.Answer;
import com.edumoet.entity.User;
import com.edumoet.service.common.AnswerService;
import com.edumoet.service.common.ReportService;

/**
 * Report Controller - User báo cáo vi phạm
 */
@Controller
@RequestMapping("/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private com.edumoet.service.common.UserService userService;

    @Autowired
    private AnswerService answerService;

    /**
     * Form báo cáo
     */
    @GetMapping("/create")
    public String showReportForm(
            @RequestParam String type,
            @RequestParam Long id,
            Model model) {
        
        model.addAttribute("entityType", type);
        model.addAttribute("entityId", id);
        model.addAttribute("pageTitle", "Report Content");
        
        return "reports/create";
    }

    /**
     * Submit báo cáo
     */
    @PostMapping("/create")
    public String submitReport(
            @RequestParam String entityType,
            @RequestParam Long entityId,
            @RequestParam String reason,
            @RequestParam(required = false, defaultValue = "") String description,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        
        try {
            User user = userService.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            reportService.createReport(user, entityType, entityId, reason, description);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "✅ Báo cáo đã được gửi! Đội ngũ kiểm duyệt sẽ xem xét trong 24-48 giờ.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "❌ Lỗi: " + e.getMessage());
        }
        
        // Redirect back to the entity page
        if ("QUESTION".equals(entityType)) {
            return "redirect:/questions/" + entityId;
        } else if ("ANSWER".equals(entityType)) {
            // Get question ID from answer
            try {
                Answer answer = answerService.findById(entityId)
                        .orElseThrow(() -> new RuntimeException("Answer not found"));
                return "redirect:/questions/" + answer.getQuestion().getId();
            } catch (Exception e) {
                return "redirect:/";
            }
        }
        
        return "redirect:/";
    }

    /**
     * Xem báo cáo của mình
     */
    @GetMapping("/my-reports")
    public String myReports(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        
        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // TODO: Add pagination
        model.addAttribute("pageTitle", "My Reports");
        
        return "reports/my-reports";
    }
}

