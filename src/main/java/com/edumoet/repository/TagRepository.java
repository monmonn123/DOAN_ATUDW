package com.edumoet.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.edumoet.entity.Tag;

import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    
    Optional<Tag> findByName(String name);
    
    Boolean existsByName(String name);
    
    @Query("SELECT t FROM Tag t ORDER BY t.questionCount DESC")
    Page<Tag> findAllByOrderByQuestionCountDesc(Pageable pageable);
    
    @Query("SELECT t FROM Tag t ORDER BY t.name ASC")
    Page<Tag> findAllByOrderByNameAsc(Pageable pageable);
    
    @Query("SELECT t FROM Tag t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Tag> searchTags(@Param("search") String search, Pageable pageable);
}

