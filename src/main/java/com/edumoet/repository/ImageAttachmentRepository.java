package com.edumoet.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.edumoet.entity.Answer;
import com.edumoet.entity.ImageAttachment;
import com.edumoet.entity.Question;

import java.util.List;

public interface ImageAttachmentRepository extends JpaRepository<ImageAttachment, Long> {
    List<ImageAttachment> findByQuestion(Question question);
    List<ImageAttachment> findByAnswer(Answer answer);
}