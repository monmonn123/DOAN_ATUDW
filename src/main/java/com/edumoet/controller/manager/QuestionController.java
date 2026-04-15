package com.edumoet.controller.manager;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.edumoet.entity.ImageAttachment;
import com.edumoet.entity.Question;
import com.edumoet.entity.User;
import com.edumoet.service.common.ActivityLogService;
import com.edumoet.service.common.ImageService;
import com.edumoet.service.common.QuestionService;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller("managerQuestionController")
@RequestMapping("/manager/questions")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class QuestionController {

    private final QuestionService questionService;
    private final ImageService imageService;
    private final ActivityLogService logService;

    public QuestionController(QuestionService questionService,
                              ImageService imageService,
                              ActivityLogService logService) {
        this.questionService = questionService;
        this.imageService = imageService;
        this.logService = logService;
    }

    // 🔹 Danh sách câu hỏi với phân trang, tìm kiếm, filter
    @GetMapping
    @Transactional(readOnly = true)
    public String listQuestions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            Model model) {
        
        Sort sort = sortDir.equalsIgnoreCase("asc") ? 
                    Sort.by(sortBy).ascending() : 
                    Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Question> questions;
        
        // Apply filters
        if (search != null && !search.trim().isEmpty()) {
            questions = questionService.searchQuestions(search, pageable);
            model.addAttribute("search", search);
        } else if ("pending".equals(status)) {
            questions = questionService.getAllPendingQuestions(pageable);
        } else if ("approved".equals(status)) {
            questions = questionService.getAllApprovedQuestions(pageable);
        } else {
            questions = questionService.getAllQuestions(pageable);
        }
        
        // Statistics for dashboard cards
        long totalQuestions = questionService.countPending() + questionService.findAllApproved().size();
        long pendingCount = questionService.countPending();
        long approvedCount = questionService.findAllApproved().size();
        
        model.addAttribute("questions", questions);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", questions.getTotalPages());
        model.addAttribute("totalItems", questions.getTotalElements());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");
        model.addAttribute("status", status);
        model.addAttribute("pageTitle", "Quản lý câu hỏi");
        
        // Add statistics
        model.addAttribute("totalQuestions", totalQuestions);
        model.addAttribute("pendingQuestions", pendingCount);
        model.addAttribute("approvedQuestions", approvedCount);
        
        return "manager/question/list";
    }

    // 🔹 Danh sách câu hỏi chưa duyệt
    @GetMapping("/pending")
    @Transactional(readOnly = true)
    public String listPendingQuestions(Model model) {
        List<Question> pending = questionService.findAllPending();
        model.addAttribute("title", "Câu hỏi chờ duyệt");
        model.addAttribute("isPending", true);
        model.addAttribute("questions", pending);
        return "manager/question/list";
    }

    // 🔹 Xem chi tiết 1 câu hỏi
    @GetMapping("/{id}")
    public String viewQuestion(@PathVariable Long id, Model model) {
        Question q = questionService.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found"));
        List<ImageAttachment> images = imageService.getImagesByQuestion(id);

        model.addAttribute("question", q);
        model.addAttribute("images", images);
        model.addAttribute("isApproved", q.getIsApproved());
        return "manager/question/detail";
    }

    // 🔹 Duyệt bài viết
    @PostMapping("/{id}/approve")
    public String approveQuestion(@PathVariable Long id, 
                                   Principal principal,
                                   RedirectAttributes redirectAttributes) {
        try {
        questionService.approve(id);
        logService.logAction(1L, "APPROVE_QUESTION", "QUESTION", id, "Approved question " + id);
            redirectAttributes.addFlashAttribute("successMessage", "✅ Đã duyệt câu hỏi!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Lỗi: " + e.getMessage());
        }
        return "redirect:/manager/questions";
    }

    // 🔹 Từ chối bài viết
    @PostMapping("/{id}/reject")
    public String rejectQuestion(@PathVariable Long id,
                                  Principal principal,
                                  RedirectAttributes redirectAttributes) {
        try {
        questionService.reject(id);
        logService.logAction(1L, "REJECT_QUESTION", "QUESTION", id, "Rejected question " + id);
            redirectAttributes.addFlashAttribute("successMessage", "✅ Đã từ chối câu hỏi!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Lỗi: " + e.getMessage());
        }
        return "redirect:/manager/questions";
    }
    
    // 🔹 Lock câu hỏi
    @PostMapping("/{id}/lock")
    public String lockQuestion(@PathVariable Long id,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {
        try {
            Question question = questionService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Question not found"));
            question.setIsLocked(!question.getIsLocked());
            questionService.save(question);
            logService.logAction(1L, "LOCK_QUESTION", "QUESTION", id, "Locked/Unlocked question " + id);
            redirectAttributes.addFlashAttribute("successMessage", 
                question.getIsLocked() ? "✅ Đã khóa câu hỏi!" : "✅ Đã mở khóa câu hỏi!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Lỗi: " + e.getMessage());
        }
        return "redirect:/manager/questions";
    }
    
    // 🔹 Pin câu hỏi
    @PostMapping("/{id}/pin")
    public String pinQuestion(@PathVariable Long id,
                              Principal principal,
                              RedirectAttributes redirectAttributes) {
        try {
            Question question = questionService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Question not found"));
            question.setIsPinned(!question.getIsPinned());
            questionService.save(question);
            logService.logAction(1L, "PIN_QUESTION", "QUESTION", id, "Pinned/Unpinned question " + id);
            redirectAttributes.addFlashAttribute("successMessage", 
                question.getIsPinned() ? "✅ Đã ghim câu hỏi!" : "✅ Đã bỏ ghim câu hỏi!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Lỗi: " + e.getMessage());
        }
        return "redirect:/manager/questions";
    }
    
    // 🔹 Delete câu hỏi (không được xóa câu hỏi của Admin)
    @PostMapping("/{id}/delete")
    public String deleteQuestion(@PathVariable Long id,
                                  Principal principal,
                                  RedirectAttributes redirectAttributes) {
        try {
            Question question = questionService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Question not found"));
            
            // Check if author is Admin - Manager CANNOT delete Admin's questions
            if (question.getAuthor() != null && hasRole(question.getAuthor(), "ADMIN")) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "❌ Bạn không thể xóa câu hỏi của Admin!");
                return "redirect:/manager/questions";
            }
            
            questionService.deleteQuestion(id);
            logService.logAction(1L, "DELETE_QUESTION", "QUESTION", id, "Deleted question " + id);
            redirectAttributes.addFlashAttribute("successMessage", "✅ Đã xóa câu hỏi!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Lỗi: " + e.getMessage());
        }
        return "redirect:/manager/questions";
    }
    
    // 🔹 Edit question page
    @GetMapping("/{id}/edit")
    public String editQuestionPage(@PathVariable Long id,
                                   Principal principal,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        try {
            Question question = questionService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Question not found"));
            
            // Check if author is Admin - Manager CANNOT edit Admin's questions
            if (question.getAuthor() != null && hasRole(question.getAuthor(), "ADMIN")) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "❌ Bạn không thể chỉnh sửa câu hỏi của Admin!");
                return "redirect:/manager/questions";
            }
            
            model.addAttribute("question", question);
            model.addAttribute("pageTitle", "Chỉnh sửa câu hỏi");
            return "manager/question/edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Lỗi: " + e.getMessage());
            return "redirect:/manager/questions";
        }
    }
    
    // 🔹 Update question
    @PostMapping("/{id}/update")
    public String updateQuestion(@PathVariable Long id,
                                  @RequestParam String title,
                                  @RequestParam String body,
                                  @RequestParam(required = false) String tags,
                                  @RequestParam(required = false) List<MultipartFile> images,
                                  Principal principal,
                                  RedirectAttributes redirectAttributes) {
        try {
            Question question = questionService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Question not found"));
            
            // Check if author is Admin - Manager CANNOT edit Admin's questions
            if (question.getAuthor() != null && hasRole(question.getAuthor(), "ADMIN")) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "❌ Bạn không thể chỉnh sửa câu hỏi của Admin!");
                return "redirect:/manager/questions";
            }
            
            question.setTitle(title);
            question.setBody(body);
            
            // Update tags if provided
            if (tags != null && !tags.trim().isEmpty()) {
                Set<String> tagNames = Arrays.stream(tags.split(","))
                        .map(String::trim)
                        .filter(t -> !t.isEmpty())
                        .collect(Collectors.toSet());
                questionService.updateQuestion(question, tagNames);
            } else {
                questionService.save(question);
            }
            
            // Handle image uploads
            if (images != null && !images.isEmpty()) {
                for (MultipartFile file : images) {
                    if (!file.isEmpty()) {
                        imageService.uploadForQuestion(file, id, question.getAuthor().getId());
                    }
                }
            }
            
            logService.logAction(1L, "UPDATE_QUESTION", "QUESTION", id, "Updated question " + id);
            redirectAttributes.addFlashAttribute("successMessage", "✅ Đã cập nhật câu hỏi!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Lỗi: " + e.getMessage());
        }
        return "redirect:/manager/questions";
    }
    
    /**
     * Helper method to check if user has a specific role
     */
    private boolean hasRole(User user, String roleName) {
        return user.getRole() != null && user.getRole().equalsIgnoreCase(roleName);
    }

    // 🔹 Upload ảnh cho câu hỏi
    @PostMapping("/{id}/upload")
    public String uploadQuestionImage(@PathVariable Long id,
                                      @RequestParam("file") MultipartFile file,
                                      @RequestParam(name = "uploaderId", required = false) Long uploaderId) {
        if (uploaderId == null) uploaderId = 1L;
        imageService.uploadForQuestion(file, id, uploaderId);
        logService.logAction(uploaderId, "UPLOAD_QUESTION_IMAGE", "IMAGE", id, "Uploaded image for question " + id);
        return "redirect:/manager/questions/" + id + "?uploaded";
    }
}
