package com.edumoet.service.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.edumoet.entity.Message;
import com.edumoet.entity.User;
import com.edumoet.repository.MessageRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Message Service - Tin nhắn riêng
 */
@Service
@Transactional
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    /**
     * Gửi tin nhắn
     */
    public Message sendMessage(User sender, User receiver, String subject, String body) {
        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setSubject(subject);
        message.setBody(body);
        message.setIsRead(false);
        message.setCreatedAt(LocalDateTime.now());
        
        return messageRepository.save(message);
    }

    /**
     * Lấy tin nhắn đã nhận
     */
    public Page<Message> getReceivedMessages(User user, Pageable pageable) {
        return messageRepository.findByReceiverOrderByCreatedAtDesc(user, pageable);
    }

    /**
     * Lấy tin nhắn đã gửi
     */
    public Page<Message> getSentMessages(User user, Pageable pageable) {
        return messageRepository.findBySenderOrderByCreatedAtDesc(user, pageable);
    }

    /**
     * Lấy tin nhắn chưa đọc
     */
    public List<Message> getUnreadMessages(User user) {
        return messageRepository.findByReceiverAndIsReadFalse(user);
    }

    /**
     * Đánh dấu đã đọc
     */
    public void markAsRead(Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        message.setIsRead(true);
        messageRepository.save(message);
    }

    /**
     * Xóa tin nhắn
     */
    public void deleteMessage(Long messageId) {
        messageRepository.deleteById(messageId);
    }

    /**
     * Lấy 1 tin nhắn
     */
    public Message getMessage(Long id) {
        return messageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message not found"));
    }

    /**
     * Đếm tin nhắn chưa đọc
     */
    public long countUnread(User user) {
        return messageRepository.countByReceiverAndIsReadFalse(user);
    }
    
    // ================== CONVERSATION FEATURES ==================
    
    /**
     * DTO for conversation summary
     */
    public static class ConversationSummary {
        private User partner;
        private Message lastMessage;
        private long unreadCount;
        
        public ConversationSummary(User partner, Message lastMessage, long unreadCount) {
            this.partner = partner;
            this.lastMessage = lastMessage;
            this.unreadCount = unreadCount;
        }
        
        public User getPartner() {
            return partner;
        }
        
        public Message getLastMessage() {
            return lastMessage;
        }
        
        public long getUnreadCount() {
            return unreadCount;
        }
        
        public LocalDateTime getLastTimestamp() {
            return lastMessage != null ? lastMessage.getCreatedAt() : null;
        }
    }
    
    /**
     * Get messages between two users (for conversation view)
     */
    public List<Message> getConversationMessages(User user1, User user2) {
        return messageRepository.findConversationMessages(user1, user2, user2, user1);
    }
    
    /**
     * Get conversation summaries for a user
     */
    public List<ConversationSummary> getConversationSummaries(User currentUser) {
        if (currentUser == null) {
            return new java.util.ArrayList<>();
        }
        
        // Get all messages where user is sender or receiver
        List<Message> allMessages = messageRepository.findBySenderOrReceiverOrderByCreatedAtDesc(
            currentUser, currentUser);
        
        if (allMessages == null || allMessages.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        
        // Group by conversation partner and get latest message
        java.util.Map<Long, ConversationSummary> conversations = new java.util.LinkedHashMap<>();
        
        for (Message msg : allMessages) {
            if (msg == null || msg.getSender() == null || msg.getReceiver() == null) {
                continue; // Skip invalid messages
            }
            
            User partner = currentUser.equals(msg.getSender()) ? msg.getReceiver() : msg.getSender();
            if (partner == null || partner.getId() == null) {
                continue; // Skip if partner is invalid
            }
            
            Long partnerId = partner.getId();
            
            if (!conversations.containsKey(partnerId)) {
                // Count unread from this partner
                long unreadCount = messageRepository.countBySenderAndReceiverAndIsReadFalse(partner, currentUser);
                conversations.put(partnerId, new ConversationSummary(partner, msg, unreadCount));
            }
        }
        
        return new java.util.ArrayList<>(conversations.values());
    }
}

