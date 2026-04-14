package com.edumoet.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.edumoet.entity.Question;
import com.edumoet.entity.Tag;
import com.edumoet.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    
    @Query("SELECT DISTINCT q FROM Question q LEFT JOIN FETCH q.tags LEFT JOIN FETCH q.author WHERE q.isApproved = true ORDER BY q.createdAt DESC")
    List<Question> findAllWithTagsAndAuthor();
    
    @Query("SELECT q FROM Question q LEFT JOIN FETCH q.tags LEFT JOIN FETCH q.author WHERE q.id = :id")
    Optional<Question> findByIdWithTagsAndAuthor(@Param("id") Long id);
    
    @Query("SELECT q FROM Question q WHERE q.isApproved = true ORDER BY q.createdAt DESC")
    Page<Question> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    @Query("SELECT q FROM Question q WHERE q.isApproved = true ORDER BY q.votes DESC")
    Page<Question> findAllByOrderByVotesDesc(Pageable pageable);
    
    @Query("SELECT q FROM Question q WHERE q.isApproved = true ORDER BY q.answerCount DESC")
    Page<Question> findAllByOrderByAnswersDesc(Pageable pageable);
    
    Page<Question> findByAuthor(User author, Pageable pageable);
    
    Page<Question> findByAuthorAndIsApproved(User author, Boolean isApproved, Pageable pageable);
    
    List<Question> findByAuthor(User author);
    
    Page<Question> findByTagsIn(List<Tag> tags, Pageable pageable);
    
    @Query("SELECT q FROM Question q WHERE q.isApproved = true AND (LOWER(q.title) LIKE LOWER(CONCAT('%', :search, '%')) OR q.body LIKE CONCAT('%', :search, '%'))")
    Page<Question> searchQuestions(@Param("search") String search, Pageable pageable);
    
    @Query("SELECT q FROM Question q JOIN q.tags t WHERE q.isApproved = true AND t = :tag ORDER BY q.createdAt DESC")
    Page<Question> findByTag(@Param("tag") Tag tag, Pageable pageable);
    
    @Query("SELECT COUNT(q) FROM Question q WHERE q.author = :author")
    Long countByAuthor(@Param("author") User author);

    // Moderation
    Page<Question> findByIsApproved(Boolean isApproved, Pageable pageable);
    
    List<Question> findByIsApprovedOrderByCreatedAtDesc(Boolean isApproved);
    
    List<Question> findByIsApprovedOrderByVotesDesc(Boolean isApproved, Pageable pageable);
    
    long countByIsApproved(Boolean isApproved);
    
    // Public view (approved and not locked)
    Page<Question> findByIsApprovedAndIsLocked(Boolean isApproved, Boolean isLocked, Pageable pageable);
    
    @Query("SELECT q FROM Question q WHERE q.isApproved = :isApproved AND q.isLocked = :isLocked ORDER BY q.votes DESC")
    Page<Question> findByIsApprovedAndIsLockedOrderByVotesDesc(
        @Param("isApproved") Boolean isApproved, 
        @Param("isLocked") Boolean isLocked, 
        Pageable pageable);
    
    // Delete operations
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM question_tags WHERE question_id = :questionId", nativeQuery = true)
    void deleteQuestionTags(@Param("questionId") Long questionId);
}

