package com.edumoet.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Chatbot Message Entity
 * Lưu từng tin nhắn trong cuộc hội thoại với chatbot
 */
@Entity
@Table(name = "chatbot_messages")
public class ChatbotMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ChatbotConversation conversation;
    
    @Lob
    @Column(name = "content", columnDefinition = "NVARCHAR(MAX)", nullable = false)
    private String content;
    
    @Column(name = "is_from_user", nullable = false)
    private boolean isFromUser;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "intent", columnDefinition = "NVARCHAR(250)")
    private String intent; // Loại câu hỏi: greeting, question_how_to, question_about, etc.
    
    @Column(name = "confidence")
    private Double confidence; // Độ tự tin của bot trong câu trả lời (0-1)
    
    @Column(name = "response_type", columnDefinition = "NVARCHAR(250)")
    private String responseType; // pattern_match, openai, manual, fallback
    
    @Column(name = "processing_time_ms")
    private Long processingTimeMs; // Thời gian xử lý (ms)
    
    // Constructors
    public ChatbotMessage() {
        this.createdAt = LocalDateTime.now();
    }
    
    public ChatbotMessage(ChatbotConversation conversation, String content, boolean isFromUser) {
        this.conversation = conversation;
        this.content = content;
        this.isFromUser = isFromUser;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public ChatbotConversation getConversation() {
        return conversation;
    }
    
    public void setConversation(ChatbotConversation conversation) {
        this.conversation = conversation;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public boolean isFromUser() {
        return isFromUser;
    }
    
    public void setFromUser(boolean fromUser) {
        isFromUser = fromUser;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getIntent() {
        return intent;
    }
    
    public void setIntent(String intent) {
        this.intent = intent;
    }
    
    public Double getConfidence() {
        return confidence;
    }
    
    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }
    
    public String getResponseType() {
        return responseType;
    }
    
    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }
    
    public Long getProcessingTimeMs() {
        return processingTimeMs;
    }
    
    public void setProcessingTimeMs(Long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }
}

