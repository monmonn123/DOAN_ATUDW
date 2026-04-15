package com.edumoet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.edumoet.entity.ChatbotConversation;
import com.edumoet.entity.ChatbotMessage;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatbotMessageRepository extends JpaRepository<ChatbotMessage, Long> {
    
    List<ChatbotMessage> findByConversationOrderByCreatedAtAsc(ChatbotConversation conversation);
    
    @Query("SELECT m FROM ChatbotMessage m WHERE m.conversation = :conversation ORDER BY m.createdAt DESC")
    List<ChatbotMessage> findLatestMessagesByConversation(@Param("conversation") ChatbotConversation conversation);
    
    @Query("SELECT COUNT(m) FROM ChatbotMessage m WHERE m.isFromUser = true AND m.createdAt >= :from AND m.createdAt <= :to")
    long countUserMessagesByDateRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
    
    @Query("SELECT m.intent, COUNT(m) FROM ChatbotMessage m WHERE m.intent IS NOT NULL GROUP BY m.intent ORDER BY COUNT(m) DESC")
    List<Object[]> findMostCommonIntents();
    
    @Query("SELECT AVG(m.processingTimeMs) FROM ChatbotMessage m WHERE m.processingTimeMs IS NOT NULL")
    Double getAverageProcessingTime();
}

