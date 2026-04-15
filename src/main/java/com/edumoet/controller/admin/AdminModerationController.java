package com.edumoet.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.edumoet.service.common.ModerationService;
import com.edumoet.service.common.QuestionService;

/**
 * Admin Moderation Controller - Kiểm duyệt nội dung
 */
@Controller
@RequestMapping("/admin/moderation")
@PreAuthorize("hasRole('ADMIN')")
public class AdminModerationController {

    @Autowired
    private ModerationService moderationService;

    @Autowired
    private QuestionService questionService;

    /**
     * Trang kiểm duyệt chính
     */
    @GetMapping
    public String moderationDashboard(Model model) {
        model.addAttribute("pageTitle", "Content Moderation - Admin");
        
        // Statistics
        model.addAttribute("pendingQuestions", moderationService.countPendingQuestions());
        model.addAttribute("approvedQuestions", moderationService.countApprovedQuestions());
        model.addAttribute("pendingComments", 0); // Removed Comment entity
        model.addAttribute("totalReports", 0); // TODO: implement if needed
        
        // Recent pending items (for quick action)
        Pageable recentPageable = PageRequest.of(0, 5, Sort.by("createdAt").descending());
        model.addAttribute("pendingQuestionsList", moderationService.getPendingQuestions(recentPageable).getContent());
        model.addAttribute("pendingCommentsList", new java.util.ArrayList<>()); // Empty list
        
        return "admin/moderation/dashboard";
    }

    /**
     * Danh sách bài viết chờ duyệt
     */
    @GetMapping("/questions/pending")
    public String pendingQuestions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        var questions = moderationService.getPendingQuestions(pageable);
        
        model.addAttribute("questions", questions);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", questions.getTotalPages());
        model.addAttribute("pageTitle", "Pending Questions - Admin");
        
        return "admin/moderation/pending-questions";
    }

    /**
     * Duyệt bài viết
     */
    @PostMapping("/questions/{id}/approve")
    public String approveQuestion(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        
        try {
            questionService.approveQuestion(id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Question approved!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error: " + e.getMessage());
        }
        
        return "redirect:/admin/moderation/questions/pending";
    }

    /**
     * Từ chối bài viết
     */
    @PostMapping("/questions/{id}/reject")
    public String rejectQuestion(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        
        try {
            questionService.rejectQuestion(id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Question rejected!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error: " + e.getMessage());
        }
        
        return "redirect:/admin/moderation/questions/pending";
    }
}

