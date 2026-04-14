package com.edumoet.service.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.edumoet.entity.Tag;
import com.edumoet.repository.TagRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class TagService {

    @Autowired
    private TagRepository tagRepository;

    public Tag createTag(Tag tag) {
        tag.setQuestionCount(0);
        return tagRepository.save(tag);
    }

    public Optional<Tag> findById(Long id) {
        return tagRepository.findById(id);
    }

    public Optional<Tag> findByName(String name) {
        return tagRepository.findByName(name);
    }

    public Page<Tag> getAllTags(Pageable pageable) {
        return tagRepository.findAll(pageable);
    }
    
    /**
     * Lấy tất cả tags không phân trang (dùng cho autocomplete)
     */
    public List<Tag> getAllTagsNoPaging() {
        return tagRepository.findAll();
    }

    public Page<Tag> getTagsByPopularity(Pageable pageable) {
        return tagRepository.findAllByOrderByQuestionCountDesc(pageable);
    }

    public Page<Tag> getTagsByName(Pageable pageable) {
        return tagRepository.findAllByOrderByNameAsc(pageable);
    }

    public Page<Tag> searchTags(String search, Pageable pageable) {
        return tagRepository.searchTags(search, pageable);
    }

    public Set<Tag> getOrCreateTags(Set<String> tagNames) {
        Set<Tag> tags = new HashSet<>();
        for (String name : tagNames) {
            // Normalize tag name by trimming and converting to lowercase
            String normalizedName = name.trim().toLowerCase();
            if (!normalizedName.isEmpty()) {
                Tag tag = tagRepository.findByName(normalizedName)
                    .orElseGet(() -> {
                        Tag newTag = new Tag();
                        newTag.setName(normalizedName);
                        newTag.setQuestionCount(0);
                        return tagRepository.save(newTag);
                    });
                tags.add(tag);
            }
        }
        return tags;
    }

    public Tag updateTag(Tag tag) {
        return tagRepository.save(tag);
    }

    @Transactional
    public Tag save(Tag tag) {
        return tagRepository.save(tag);
    }

    /**
     * Xóa thẻ
     */
    @Transactional
    public void deleteTag(Long tagId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new RuntimeException("Tag not found"));
        
        if (tag.getQuestionCount() > 0) {
            throw new RuntimeException("Không thể xóa thẻ đang được sử dụng bởi " + tag.getQuestionCount() + " câu hỏi");
        }
        
        tagRepository.deleteById(tagId);
    }

    /**
     * Gộp 2 thẻ: chuyển tất cả câu hỏi từ sourceTag sang targetTag
     */
    @Transactional
    public void mergeTags(Long sourceTagId, Long targetTagId) {
        if (sourceTagId.equals(targetTagId)) {
            throw new RuntimeException("Không thể gộp thẻ với chính nó");
        }
        
        Tag sourceTag = tagRepository.findById(sourceTagId)
                .orElseThrow(() -> new RuntimeException("Source tag not found"));
        Tag targetTag = tagRepository.findById(targetTagId)
                .orElseThrow(() -> new RuntimeException("Target tag not found"));
        
        // Di chuyển tất cả questions từ source sang target
        sourceTag.getQuestions().forEach(question -> {
            question.getTags().remove(sourceTag);
            question.getTags().add(targetTag);
        });
        
        // Cập nhật question count
        targetTag.setQuestionCount(targetTag.getQuestionCount() + sourceTag.getQuestionCount());
        sourceTag.setQuestionCount(0);
        
        // Xóa source tag
        tagRepository.delete(sourceTag);
        tagRepository.save(targetTag);
    }
    
    /**
     * Count total tags
     */
    public long count() {
        return tagRepository.count();
    }
}