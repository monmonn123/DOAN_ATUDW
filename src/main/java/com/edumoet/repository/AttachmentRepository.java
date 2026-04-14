package com.edumoet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.edumoet.entity.Attachment;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    Attachment findByFilename(String filename);
}