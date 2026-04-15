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

import com.edumoet.entity.Answer;
import com.edumoet.service.common.AnswerService;

/**
 * Admin Answer Controller - Quản lý câu trả lời
 */
@Controller
@RequestMapping("/admin/answers")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnswerController {

    @Autowired
    private AnswerService answerService;

    /**
     * Danh sách câu trả lời
     */
    @GetMapping
    public String listAnswers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search,
            Model model) {
        
        Sort sort = sortDir.equalsIgnoreCase("asc") ? 
                    Sort.by(sortBy).ascending() : 
                    Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Answer> answers;
        
        if (search != null && !search.isEmpty()) {
            answers = answerService.searchAnswers(search, pageable);
            model.addAttribute("search", search);
        } else {
            answers = answerService.getAllAnswers(pageable);
        }
        
        // Statistics
        model.addAttribute("totalAnswers", answerService.countAll());
        
        model.addAttribute("answers", answers);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", answers.getTotalPages());
        model.addAttribute("totalItems", answers.getTotalElements());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");
        model.addAttribute("pageTitle", "Answer Management - Admin");
        
        return "admin/answers/list";
    }

    /**
     * Xóa câu trả lời
     */
    @PostMapping("/{id}/delete")
    public String deleteAnswer(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        
        try {
            answerService.deleteAnswer(id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "✅ Đã xóa câu trả lời!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "❌ Lỗi: " + e.getMessage());
        }
        
        return "redirect:/admin/answers";
    }
}

