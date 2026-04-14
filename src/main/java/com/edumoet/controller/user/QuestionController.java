package com.edumoet.controller.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.edumoet.entity.*;
import com.edumoet.service.common.*;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.security.core.Authentication;
import jakarta.validation.Valid;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/questions")
public class QuestionController {

    @Autowired
    private QuestionService questionService;

    @Autowired
    private AnswerService answerService;

    @Autowired
    private UserService userService;

    @Autowired
    private WebSocketService webSocketService;
    
    @Autowired
    private ImageService imageService;

    @Autowired
    private TagService tagService;

    // ============ SPECIFIC ROUTES (Must be BEFORE generic /{id} route) ============
    
    @GetMapping("/ask")
    public String askQuestionForm(Model model) {
        model.addAttribute("question", new Question());
        model.addAttribute("pageTitle", "Ask a Question - EDUMOET");
        return "question/ask";
    }

    @PostMapping("/ask")
    public String askQuestion(
            @Valid @ModelAttribute("question") Question question,
            BindingResult result,
            @RequestParam(required = false) MultipartFile[] files,
            Authentication authentication,
            Model model) {
        
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Ask a Question - EDUMOET");
            return "question/ask";
        }
        
        try {
            User author = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            question.setAuthor(author);
            
            // Process tags from the tag string
            if (question.getTagString() != null && !question.getTagString().isEmpty()) {
                Set<String> tagNames = Arrays.stream(question.getTagString().split("\\s*,\\s*"))
                    .filter(tag -> !tag.isEmpty())
                    .collect(Collectors.toSet());
                Set<Tag> tags = tagService.getOrCreateTags(tagNames);
                question.setTags(tags);
            }
            
            // Save question first to get ID
            Question savedQuestion = questionService.save(question);
            
            // Handle image uploads if any
            if (files != null && files.length > 0) {
                for (MultipartFile file : files) {
                    if (!file.isEmpty()) {
                        ImageAttachment image = imageService.saveQuestionImage(file, savedQuestion);
                        savedQuestion.getImages().add(image);
                    }
                }
                questionService.save(savedQuestion); // Save again with images
            }
            
            return "redirect:/questions/" + savedQuestion.getId();
        } catch (Exception e) {
            model.addAttribute("error", "Failed to create question: " + e.getMessage());
            model.addAttribute("pageTitle", "Ask a Question - EDUMOET");
            return "question/ask";
        }
    }

    // ============ GENERIC ROUTES (with path variables) ============

    @GetMapping("/{id}")
    public String viewQuestion(@PathVariable Long id, Model model, Authentication authentication) {
        Question question = questionService.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found"));
        
        // Check if question is approved or user has permission to view
        if (!question.getIsApproved()) {
            // Only author, admin, or manager can view unapproved questions
            if (authentication == null) {
                model.addAttribute("errorTitle", "Câu Hỏi Đang Chờ Kiểm Duyệt");
                model.addAttribute("errorMessage", "Câu hỏi này đang chờ được kiểm duyệt bởi quản trị viên.");
                model.addAttribute("errorIcon", "bi-hourglass-split");
                return "error/custom";
            }
            
            String username = authentication.getName();
            boolean isAuthor = question.getAuthor().getUsername().equals(username);
            boolean isAdminOrManager = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER"));
            
            if (!isAuthor && !isAdminOrManager) {
                model.addAttribute("errorTitle", "Câu Hỏi Đang Chờ Kiểm Duyệt");
                model.addAttribute("errorMessage", "Câu hỏi này đang chờ được kiểm duyệt bởi quản trị viên.");
                model.addAttribute("errorIcon", "bi-hourglass-split");
                return "error/custom";
            }
        }
        
        // Check if question is locked - only Admin and Manager can view
        if (question.getIsLocked()) {
            if (authentication == null) {
                model.addAttribute("errorTitle", "Câu Hỏi Đã Bị Khóa");
                model.addAttribute("errorMessage", "Câu hỏi này đã bị khóa bởi quản trị viên. Vui lòng đăng nhập để xem thêm thông tin.");
                model.addAttribute("errorIcon", "bi-lock-fill");
                return "error/custom";
            }
            
            boolean isAdminOrManager = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER"));
            
            if (!isAdminOrManager) {
                model.addAttribute("errorTitle", "Câu Hỏi Đã Bị Khóa");
                model.addAttribute("errorMessage", "Câu hỏi này đã bị khóa bởi quản trị viên do vi phạm quy định cộng đồng. Chỉ quản trị viên mới có thể xem nội dung này.");
                model.addAttribute("errorIcon", "bi-lock-fill");
                model.addAttribute("errorColor", "danger");
                return "error/custom";
            }
        }
        
        // Increment views
        questionService.incrementViews(question);
        
        List<Answer> answers = answerService.getAnswersByQuestion(question);
        
        // Debug: Log images for each answer
        System.out.println("=== Question ID: " + id + " ===");
        System.out.println("Total Answers: " + answers.size());
        for (Answer answer : answers) {
            System.out.println("Answer ID: " + answer.getId() + " - Images count: " + 
                (answer.getImages() != null ? answer.getImages().size() : "NULL"));
            if (answer.getImages() != null && !answer.getImages().isEmpty()) {
                answer.getImages().forEach(img -> 
                    System.out.println("  - Image ID: " + img.getId() + ", Path: " + img.getPath())
                );
            }
        }
        
        // Check if current user has upvoted question/answers
        boolean hasUpvotedQuestion = false;
        Set<Long> upvotedAnswerIds = new HashSet<>();
        
        if (authentication != null) {
            String username = authentication.getName();
            User currentUser = userService.findByUsername(username).orElse(null);
            
            if (currentUser != null) {
                // Check if user upvoted the question
                hasUpvotedQuestion = currentUser.getVotedQuestions() != null && 
                                     currentUser.getVotedQuestions().contains(question);
                
                // Check which answers user upvoted
                if (currentUser.getVotedAnswers() != null) {
                    upvotedAnswerIds = currentUser.getVotedAnswers().stream()
                        .map(Answer::getId)
                        .collect(Collectors.toSet());
                }
            }
        }
        
        model.addAttribute("question", question);
        model.addAttribute("answers", answers);
        model.addAttribute("hasUpvotedQuestion", hasUpvotedQuestion);
        model.addAttribute("upvotedAnswerIds", upvotedAnswerIds);
        model.addAttribute("pageTitle", question.getTitle() + " - EDUMOET");
        
        return "question/view";
    }

    @GetMapping("/{id}/edit")
    public String editQuestionForm(@PathVariable Long id, Model model, Authentication authentication) {
        Question question = questionService.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found"));
        
        User currentUser = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!question.getAuthor().getId().equals(currentUser.getId())) {
            return "redirect:/questions/" + id;
        }
        
        model.addAttribute("question", question);
        model.addAttribute("pageTitle", "Edit Question - EDUMOET");
        return "question/edit";
    }

    @PostMapping("/{id}/edit")
    public String editQuestion(
            @PathVariable Long id,
            @Valid @ModelAttribute Question updatedQuestion,
            @RequestParam(required = false) String tagString,
            @RequestParam(required = false) MultipartFile[] files,
            BindingResult result,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        try {
            if (result.hasErrors()) {
                model.addAttribute("pageTitle", "Edit Question - EDUMOET");
                return "question/edit";
            }
            
            Question question = questionService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Question not found"));
            
            User currentUser = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (!question.getAuthor().getId().equals(currentUser.getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "❌ You don't have permission to edit this question");
                return "redirect:/questions/" + id;
            }
            
            // Update basic fields
            question.setTitle(updatedQuestion.getTitle());
            question.setBody(updatedQuestion.getBody());
            
            // Update tags
            if (tagString != null && !tagString.isEmpty()) {
                Set<String> tagNames = Arrays.stream(tagString.split("\\s*,\\s*"))
                    .filter(tag -> !tag.isEmpty())
                    .collect(Collectors.toSet());
                Set<Tag> tags = tagService.getOrCreateTags(tagNames);
                question.setTags(tags);
            }
            
            // Save question first
            Question savedQuestion = questionService.updateQuestion(question, null);
            
            // Handle new image uploads if any
            if (files != null && files.length > 0) {
                for (MultipartFile file : files) {
                    if (!file.isEmpty()) {
                        ImageAttachment image = imageService.saveQuestionImage(file, savedQuestion);
                        savedQuestion.getImages().add(image);
                    }
                }
                questionService.save(savedQuestion); // Save again with new images
            }
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "✅ Question updated successfully!");
            
            return "redirect:/questions/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "❌ Error updating question: " + e.getMessage());
            return "redirect:/questions/" + id + "/edit";
        }
    }
    
    /**
     * Delete image from question
     */
    @PostMapping("/{questionId}/images/{imageId}/delete")
    @ResponseBody
    public ResponseEntity<?> deleteQuestionImage(
            @PathVariable Long questionId,
            @PathVariable Long imageId,
            Authentication authentication) {
        
        System.out.println("\n=== DELETE IMAGE REQUEST ===");
        System.out.println("Question ID: " + questionId);
        System.out.println("Image ID: " + imageId);
        System.out.println("Authentication: " + (authentication != null ? authentication.getName() : "NULL"));
        
        try {
            if (authentication == null) {
                System.out.println("❌ No authentication!");
                return ResponseEntity.status(401).body("Not authenticated");
            }
            
            Question question = questionService.findById(questionId)
                    .orElseThrow(() -> new RuntimeException("Question not found"));
            System.out.println("✅ Question found: " + question.getTitle());
            System.out.println("   Total images: " + question.getImages().size());
            
            User currentUser = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            System.out.println("✅ Current user: " + currentUser.getUsername());
            System.out.println("   Question author: " + question.getAuthor().getUsername());
            
            if (!question.getAuthor().getId().equals(currentUser.getId())) {
                System.out.println("❌ Not authorized! Current user ID: " + currentUser.getId() + ", Author ID: " + question.getAuthor().getId());
                return ResponseEntity.status(403).body("Not authorized");
            }
            
            // Find the image
            ImageAttachment image = imageService.findById(imageId)
                    .orElseThrow(() -> new RuntimeException("Image not found"));
            System.out.println("✅ Image found in DB:");
            System.out.println("   - ID: " + image.getId());
            System.out.println("   - Path: " + image.getPath());
            System.out.println("   - Question ID: " + (image.getQuestion() != null ? image.getQuestion().getId() : "NULL"));
            
            // Verify image belongs to this question
            if (image.getQuestion() == null || !image.getQuestion().getId().equals(questionId)) {
                System.out.println("❌ Image does not belong to this question!");
                return ResponseEntity.status(400).body("Image does not belong to this question");
            }
            
            // Method 1: Remove from collection first (orphanRemoval will delete from DB)
            boolean removed = question.getImages().removeIf(img -> img.getId().equals(imageId));
            System.out.println("Removed from collection: " + removed);
            
            if (removed) {
                // Save question (orphanRemoval = true will delete the ImageAttachment from DB)
                questionService.save(question);
                System.out.println("✅ Question saved, orphanRemoval will delete image from DB");
                
                // Now delete the physical file
                try {
                    java.nio.file.Path uploadDir = java.nio.file.Paths.get("uploads/images");
                    java.nio.file.Path filePath = uploadDir.resolve(image.getPath());
                    System.out.println("Deleting physical file: " + filePath.toAbsolutePath());
                    
                    boolean fileDeleted = java.nio.file.Files.deleteIfExists(filePath);
                    System.out.println("Physical file deleted: " + fileDeleted);
                } catch (Exception e) {
                    System.out.println("⚠️ Could not delete physical file: " + e.getMessage());
                    // Don't fail the request - DB record is already deleted
                }
                
                System.out.println("✅ Image deletion completed successfully\n");
                return ResponseEntity.ok().body("Image deleted successfully");
            } else {
                System.out.println("❌ Failed to remove image from collection");
                return ResponseEntity.status(500).body("Failed to remove image");
            }
            
        } catch (Exception e) {
            System.out.println("❌ Exception: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteQuestion(@PathVariable Long id, Authentication authentication) {
        Question question = questionService.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found"));
        
        User currentUser = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!question.getAuthor().getId().equals(currentUser.getId())) {
            return "redirect:/questions/" + id;
        }
        
        questionService.deleteQuestion(id);
        return "redirect:/";
    }

    /**
     * Unified vote endpoint for form submission
     */
    @PostMapping("/{id}/vote")
    public String voteQuestion(
            @PathVariable Long id, 
            @RequestParam String voteType,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        
        try {
            Question question = questionService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Question not found"));
            
            User user = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            if ("UP".equals(voteType)) {
                questionService.upvoteQuestion(question, user);
            } else if ("DOWN".equals(voteType)) {
                questionService.downvoteQuestion(question, user);
            }
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to vote: " + e.getMessage());
        }
        
        return "redirect:/questions/" + id;
    }

    @PostMapping("/{id}/upvote")
    @ResponseBody
    public String upvoteQuestion(@PathVariable Long id, Authentication authentication) {
        Question question = questionService.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found"));
        
        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        questionService.upvoteQuestion(question, user);
        return String.valueOf(question.getVotes());
    }

    @PostMapping("/{id}/downvote")
    @ResponseBody
    public String downvoteQuestion(@PathVariable Long id, Authentication authentication) {
        Question question = questionService.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        questionService.downvoteQuestion(question, user);
        return String.valueOf(question.getVotes());
    }

}