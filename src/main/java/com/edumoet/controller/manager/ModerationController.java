package com.edumoet.controller.manager;

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
 * Manager Moderation Controller - Kiểm duyệt nội dung (Manager role)
 */
@Controller("managerModerationController")
@RequestMapping("/manager/moderation")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class ModerationController {

    @Autowired
    private ModerationService moderationService;

    @Autowired
    private QuestionService questionService;

    /**
     * Trang kiểm duyệt chính
     */
    @GetMapping
    public String moderationDashboard(Model model) {
        model.addAttribute("pageTitle", "Content Moderation - Manager");
        
        // Statistics
        model.addAttribute("pendingQuestions", moderationService.countPendingQuestions());
        model.addAttribute("approvedQuestions", moderationService.countApprovedQuestions());
        model.addAttribute("pendingComments", 0); // Removed Comment entity
        model.addAttribute("approvedComments", 0); // Removed Comment entity
        
        return "manager/moderation/dashboard";
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
        model.addAttribute("pageTitle", "Pending Questions - Manager");
        
        return "manager/moderation/pending-questions";
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
                "✅ Question approved!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "❌ Error: " + e.getMessage());
        }
        
        return "redirect:/manager/moderation/questions/pending";
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
                "✅ Question rejected!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "❌ Error: " + e.getMessage());
        }
        
        return "redirect:/manager/moderation/questions/pending";
    }

    /**
     * Khóa bài viết
     */
    @PostMapping("/questions/{id}/lock")
    public String lockQuestion(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            questionService.lockQuestion(id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "✅ Question locked successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "❌ Error: " + e.getMessage());
        }
        return "redirect:/manager/questions/" + id;
    }

    /**
     * Mở khóa bài viết
     */
    @PostMapping("/questions/{id}/unlock")
    public String unlockQuestion(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            questionService.unlockQuestion(id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "✅ Question unlocked successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "❌ Error: " + e.getMessage());
        }
        return "redirect:/manager/questions/" + id;
    }

    /**
     * Ghim bài viết
     */
    @PostMapping("/questions/{id}/pin")
    public String pinQuestion(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            questionService.pinQuestion(id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "✅ Question pinned successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "❌ Error: " + e.getMessage());
        }
        return "redirect:/manager/questions/" + id;
    }

    /**
     * Bỏ ghim bài viết
     */
    @PostMapping("/questions/{id}/unpin")
    public String unpinQuestion(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            questionService.unpinQuestion(id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "✅ Question unpinned successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "❌ Error: " + e.getMessage());
        }
        return "redirect:/manager/questions/" + id;
    }

    /**
     * Xóa bài viết
     */
    @PostMapping("/questions/{id}/delete")
    public String deleteQuestion(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            questionService.deleteQuestion(id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "✅ Question deleted successfully!");
            return "redirect:/manager/questions";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "❌ Error: " + e.getMessage());
            return "redirect:/manager/questions/" + id;
        }
    }
}

