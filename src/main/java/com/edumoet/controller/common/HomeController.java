package com.edumoet.controller.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.edumoet.entity.Question;
import com.edumoet.service.common.QuestionService;

@Controller
public class HomeController {

    @Autowired
    private QuestionService questionService;

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

    @GetMapping({"/", "/home"})
    public String home(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "newest") String sort,
            Authentication authentication,
            Model model) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Question> questions;
        
        // Check if user is Admin or Manager
        boolean isAdminOrManager = false;
        if (authentication != null) {
            isAdminOrManager = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || 
                                   a.getAuthority().equals("ROLE_MANAGER"));
        }
        
        if ("votes".equals(sort)) {
            // Admin/Manager see all, users see only unlocked
            questions = isAdminOrManager ? 
                questionService.getQuestionsByVotes(pageable) : 
                questionService.getPublicQuestionsByVotes(pageable);
        } else {
            // Admin/Manager see all, users see only unlocked
            questions = isAdminOrManager ? 
                questionService.getAllQuestions(pageable) : 
                questionService.getPublicQuestions(pageable);
        }
        
        // Debug: Log avatar URLs for first few questions
        questions.getContent().stream().limit(3).forEach(q -> {
            if (q.getAuthor() != null) {
                String avatarUrl = resolveAvatarUrl(q.getAuthor().getProfileImage(), q.getAuthor().getUsername(), 24);
                System.out.println("🔍 [HOME] Question: " + q.getTitle() 
                    + " | Author: " + q.getAuthor().getUsername()
                    + " | profileImage: " + q.getAuthor().getProfileImage() 
                    + " | resolved URL: " + avatarUrl);
            }
        });
        
        model.addAttribute("questions", questions);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", questions.getTotalPages());
        model.addAttribute("sort", sort);
        model.addAttribute("pageTitle", "Top Questions - EDUMOET");
        
        return "home";
    }

    @GetMapping("/search")
    public String search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            Model model) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Question> questions = questionService.searchQuestions(q, pageable);
        
        model.addAttribute("questions", questions);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", questions.getTotalPages());
        model.addAttribute("searchQuery", q);
        model.addAttribute("pageTitle", "Search Results - EDUMOET");
        
        return "search";
    }
}

