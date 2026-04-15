package com.edumoet.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "NVARCHAR(250)")
    private String type; // SYSTEM, COMMENT, ANSWER, VOTE, MESSAGE, EVENT

    @Column(columnDefinition = "NVARCHAR(MAX)", nullable = false)
    private String message;

    @Column(columnDefinition = "NVARCHAR(500)")
    private String link;

    @Column(nullable = false)
    private Boolean isRead = false;

    @Column(nullable = false)
    private Boolean isGlobal = false; // For system-wide notifications

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime readAt;

    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public void setRead(Boolean read) {
        isRead = read;
    }

    public void setGlobal(Boolean global) {
        isGlobal = global;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Boolean getIsRead() {
		return isRead;
	}

	public void setIsRead(Boolean isRead) {
		this.isRead = isRead;
	}

	public Boolean getIsGlobal() {
		return isGlobal;
	}

	public void setIsGlobal(Boolean isGlobal) {
		this.isGlobal = isGlobal;
	}

	public User getUser() {
		return user;
	}

	public String getType() {
		return type;
	}

	public String getMessage() {
		return message;
	}

	public String getLink() {
		return link;
	}

	public User getSender() {
		return sender;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getReadAt() {
		return readAt;
	}
}