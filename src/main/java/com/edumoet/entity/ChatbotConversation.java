package com.edumoet.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Chatbot Conversation Entity
 * Lưu thông tin phiên chat của user với chatbot
 */
@Entity
@Table(name = "chatbot_conversations")
public class ChatbotConversation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(name = "session_id", unique = true, nullable = false, columnDefinition = "NVARCHAR(250)")
    private String sessionId;
    
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;
    
    @Column(name = "ended_at")
    private LocalDateTime endedAt;
    
    @Column(name = "is_active")
    private boolean isActive;
    
    @Column(name = "user_ip", columnDefinition = "NVARCHAR(45)")
    private String userIp;
    
    @Column(name = "user_agent", columnDefinition = "NVARCHAR(MAX)")
    private String userAgent;
    
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ChatbotMessage> messages = new ArrayList<>();
    
    // Constructors
    public ChatbotConversation() {
        this.startedAt = LocalDateTime.now();
        this.isActive = true;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public LocalDateTime getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }
    
    public LocalDateTime getEndedAt() {
        return endedAt;
    }
    
    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    public String getUserIp() {
        return userIp;
    }
    
    public void setUserIp(String userIp) {
        this.userIp = userIp;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    public List<ChatbotMessage> getMessages() {
        return messages;
    }
    
    public void setMessages(List<ChatbotMessage> messages) {
        this.messages = messages;
    }
    
    public void addMessage(ChatbotMessage message) {
        messages.add(message);
        message.setConversation(this);
    }
}

