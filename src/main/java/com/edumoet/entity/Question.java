package com.edumoet.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, columnDefinition = "NVARCHAR(250)")
    private String title;

    @NotBlank
    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String body;

    @Column(nullable = false)
    private Integer views = 0;

    @Column(nullable = false)
    private Integer votes = 0;

    @Column(nullable = false)
    private Integer answerCount = 0;

    @Column(nullable = false)
    private Boolean isPinned = false;

    @Column(nullable = false)
    private Boolean isLocked = false;

    @Column(nullable = false)
    private Boolean isApproved = false;  // For moderation - default needs approval

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Answer> answers = new ArrayList<>();

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ImageAttachment> images = new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "question_tags",
        joinColumns = @JoinColumn(name = "question_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    @Transient
    private String tagString; // For form binding

    public void setTagString(String tagString) {
        this.tagString = tagString;
    }

    public String getTagString() {
        if (tagString != null) {
            return tagString;
        }
        return tags.stream()
                  .map(Tag::getName)
                  .reduce((a, b) -> a + " " + b)
                  .orElse("");
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_answer_id")
    private Answer acceptedAnswer;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void incrementViews() {
        this.views++;
    }

    public void upvote() {
        this.votes++;
    }

    public void downvote() {
        this.votes--;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Question)) return false;
        Question question = (Question) o;
        return id != null && id.equals(question.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public Integer getViews() {
		return views;
	}

	public void setViews(Integer views) {
		this.views = views;
	}

	public Integer getVotes() {
		return votes;
	}

	public void setVotes(Integer votes) {
		this.votes = votes;
	}

	public Integer getAnswerCount() {
		return answerCount;
	}

	public void setAnswerCount(Integer answerCount) {
		this.answerCount = answerCount;
	}

	public Boolean getIsPinned() {
		return isPinned;
	}

	public void setIsPinned(Boolean isPinned) {
		this.isPinned = isPinned;
	}

	public Boolean getIsLocked() {
		return isLocked;
	}

	public void setIsLocked(Boolean isLocked) {
		this.isLocked = isLocked;
	}

	public Boolean getIsApproved() {
		return isApproved;
	}

	public void setIsApproved(Boolean isApproved) {
		this.isApproved = isApproved;
	}

	public User getAuthor() {
		return author;
	}

	public void setAuthor(User author) {
		this.author = author;
	}

	public List<Answer> getAnswers() {
		return answers;
	}

	public void setAnswers(List<Answer> answers) {
		this.answers = answers;
	}

	public List<ImageAttachment> getImages() {
		return images;
	}

	public void setImages(List<ImageAttachment> images) {
		this.images = images;
	}

	public Set<Tag> getTags() {
		return tags;
	}

	public void setTags(Set<Tag> tags) {
		this.tags = tags;
	}

	public Answer getAcceptedAnswer() {
		return acceptedAnswer;
	}

	public void setAcceptedAnswer(Answer acceptedAnswer) {
		this.acceptedAnswer = acceptedAnswer;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}