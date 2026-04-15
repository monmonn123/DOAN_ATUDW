package com.edumoet.controller.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.edumoet.entity.Question;
import com.edumoet.entity.User;
import com.edumoet.service.common.AnswerService;
import com.edumoet.service.common.QuestionService;
import com.edumoet.service.common.UserService;

@Controller
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private AnswerService answerService;

    @GetMapping
    public String listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "36") int size,
            @RequestParam(defaultValue = "reputation") String sort,
            @RequestParam(required = false) String search,
            Model model) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<User> users;
        
        if (search != null && !search.isEmpty()) {
            users = userService.searchUsers(search, pageable);
            model.addAttribute("searchQuery", search);
        } else if ("reputation".equals(sort)) {
            users = userService.getUsersByReputation(pageable);
        } else {
            users = userService.getAllUsers(pageable);
        }
        
        model.addAttribute("users", users);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", users.getTotalPages());
        model.addAttribute("sort", sort);
        model.addAttribute("pageTitle", "Users - EDUMOET");
        
        return "users/list";
    }

    @GetMapping("/{id}")
    public String viewUserProfile(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "questions") String tab,
            Authentication authentication,
            Model model) {
        
        User user = userService.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Increment profile views
        userService.incrementViews(user);
        
        Pageable pageable = PageRequest.of(page, size);
        
        if ("answers".equals(tab)) {
            model.addAttribute("answers", answerService.getAnswersByAuthor(user, pageable));
        } else {
            Page<Question> questions = questionService.getQuestionsByAuthor(user, pageable);
            
            // Filter out pending questions if viewer is not the author, admin, or manager
            boolean isOwner = authentication != null && 
                            authentication.getName().equals(user.getUsername());
            boolean isAdminOrManager = authentication != null && 
                            authentication.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || 
                                             a.getAuthority().equals("ROLE_MANAGER"));
            
            if (!isOwner && !isAdminOrManager) {
                // Filter to only show approved questions
                questions = questionService.getApprovedQuestionsByAuthor(user, pageable);
            }
            
            model.addAttribute("questions", questions);
        }
        
        Long questionCount = questionService.countByAuthor(user);
        Long answerCount = answerService.countByAuthor(user);
        
        model.addAttribute("user", user);
        model.addAttribute("questionCount", questionCount);
        model.addAttribute("answerCount", answerCount);
        model.addAttribute("currentTab", tab);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageTitle", user.getUsername() + " - User Profile");
        
        return "users/profile";
    }

    /**
     * Chỉ ADMIN mới có quyền edit tài khoản người dùng tại /users/{id}/edit
     */
    @GetMapping("/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String editProfileForm(@PathVariable Long id, Model model) {
        User user = userService.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        model.addAttribute("user", user);
        model.addAttribute("pageTitle", "Edit User - Admin");
        return "users/edit";
    }

    /**
     * Chỉ ADMIN mới có quyền update tài khoản người dùng
     */
    @PostMapping("/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String editProfile(
            @PathVariable Long id,
            @ModelAttribute User updatedUser,
            RedirectAttributes redirectAttributes) {
        
        try {
            User user = userService.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            user.setAbout(updatedUser.getAbout());
            user.setLocation(updatedUser.getLocation());
            user.setWebsite(updatedUser.getWebsite());
            userService.updateUser(user);
            
            redirectAttributes.addFlashAttribute("successMessage", "User updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }
        
        return "redirect:/users/" + id;
    }

    /**
     * Chỉ ADMIN mới có quyền xóa tài khoản người dùng
     */
    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Không cho phép xóa chính mình
            // userService.deleteUser(id);
            
            redirectAttributes.addFlashAttribute("successMessage", "User deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }
        
        return "redirect:/users";
    }
}

