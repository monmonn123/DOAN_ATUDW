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

import com.edumoet.entity.Tag;
import com.edumoet.service.common.TagService;

/**
 * Admin Tag Controller - Quản lý thẻ
 */
@Controller
@RequestMapping("/admin/tags")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTagController {

    @Autowired
    private TagService tagService;

    /**
     * Danh sách thẻ
     */
    @GetMapping
    public String listTags(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "questionCount") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search,
            Model model) {
        
        Sort sort = sortDir.equalsIgnoreCase("asc") ? 
                    Sort.by(sortBy).ascending() : 
                    Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Tag> tags;
        
        if (search != null && !search.isEmpty()) {
            tags = tagService.searchTags(search, pageable);
            model.addAttribute("search", search);
        } else {
            tags = tagService.getAllTags(pageable);
        }
        
        // Statistics
        long totalTags = tagService.getAllTagsNoPaging().size();
        long tagsWithQuestions = tags.stream().filter(t -> t.getQuestionCount() > 0).count();
        long unusedTags = tags.stream().filter(t -> t.getQuestionCount() == 0).count();
        
        model.addAttribute("tags", tags);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", tags.getTotalPages());
        model.addAttribute("totalItems", tags.getTotalElements());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");
        model.addAttribute("pageTitle", "Quản Lý Thẻ - Quản Trị");
        
        // Statistics for dashboard
        model.addAttribute("totalTags", totalTags);
        model.addAttribute("tagsWithQuestions", tagsWithQuestions);
        model.addAttribute("unusedTags", unusedTags);
        
        return "admin/tags/list";
    }

    /**
     * Xem chi tiết thẻ
     */
    @GetMapping("/{id}")
    public String viewTag(@PathVariable Long id, Model model) {
        Tag tag = tagService.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thẻ"));
        
        model.addAttribute("tag", tag);
        model.addAttribute("pageTitle", "Chi Tiết Thẻ - " + tag.getName());
        
        return "admin/tags/view";
    }

    /**
     * Form chỉnh sửa thẻ
     */
    @GetMapping("/{id}/edit")
    public String editTagForm(@PathVariable Long id, Model model) {
        Tag tag = tagService.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thẻ"));
        
        model.addAttribute("tag", tag);
        model.addAttribute("pageTitle", "Chỉnh Sửa Thẻ - " + tag.getName());
        
        return "admin/tags/edit";
    }

    /**
     * Xử lý cập nhật thẻ
     */
    @PostMapping("/{id}/edit")
    public String editTag(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            RedirectAttributes redirectAttributes) {
        
        try {
            Tag tag = tagService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thẻ"));
            
            tag.setName(name);
            tag.setDescription(description);
            tagService.save(tag);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Cập nhật thẻ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Lỗi: " + e.getMessage());
        }
        
        return "redirect:/admin/tags/" + id;
    }

    /**
     * Xóa thẻ
     */
    @PostMapping("/{id}/delete")
    public String deleteTag(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            tagService.deleteTag(id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Đã xóa thẻ!");
            return "redirect:/admin/tags";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Lỗi: " + e.getMessage());
            return "redirect:/admin/tags/" + id;
        }
    }

    /**
     * Merge 2 tags (gộp tag nguồn vào tag đích)
     */
    @PostMapping("/merge")
    public String mergeTags(
            @RequestParam Long sourceTagId,
            @RequestParam Long targetTagId,
            RedirectAttributes redirectAttributes) {
        
        try {
            tagService.mergeTags(sourceTagId, targetTagId);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Gộp thẻ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Lỗi: " + e.getMessage());
        }
        
        return "redirect:/admin/tags";
    }
    
    /**
     * Tạo tag mới
     */
    @PostMapping("/create")
    public String createTag(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            RedirectAttributes redirectAttributes) {
        
        try {
            // Check if tag already exists
            if (tagService.findByName(name.trim().toLowerCase()).isPresent()) {
                throw new RuntimeException("Thẻ '" + name + "' đã tồn tại!");
            }
            
            Tag newTag = new Tag();
            newTag.setName(name.trim().toLowerCase());
            newTag.setDescription(description);
            newTag.setQuestionCount(0);
            tagService.save(newTag);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "✅ Đã tạo thẻ mới!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "❌ Lỗi: " + e.getMessage());
        }
        
        return "redirect:/admin/tags";
    }
}

