package com.edumoet.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "image_attachments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ImageAttachment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, columnDefinition = "NVARCHAR(250)")
    private String fileName;
    
    @Column(nullable = false, columnDefinition = "NVARCHAR(100)")
    private String contentType;
    
    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String path;
    
    @ManyToOne
    @JoinColumn(name = "question_id")
    private Question question;
    
    @ManyToOne
    @JoinColumn(name = "answer_id")
    private Answer answer;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User uploadedBy;
    
    @CreatedDate
    private LocalDateTime createdAt;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Question getQuestion() {
		return question;
	}

	public void setQuestion(Question question) {
		this.question = question;
	}

	public Answer getAnswer() {
		return answer;
	}

	public void setAnswer(Answer answer) {
		this.answer = answer;
	}

	public User getUploadedBy() {
		return uploadedBy;
	}

	public void setUploadedBy(User uploadedBy) {
		this.uploadedBy = uploadedBy;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
}