package com.edumoet.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.edumoet.entity.Message;
import com.edumoet.entity.User;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    Page<Message> findByReceiverOrderByCreatedAtDesc(User receiver, Pageable pageable);
    
    Page<Message> findBySenderOrderByCreatedAtDesc(User sender, Pageable pageable);
    
    List<Message> findByReceiverAndIsReadFalse(User receiver);
    
    long countByReceiverAndIsReadFalse(User receiver);
    
    // For user deletion
    void deleteBySender(User sender);
    void deleteByReceiver(User receiver);
    
    // For conversations
    List<Message> findBySenderOrReceiverOrderByCreatedAtDesc(User sender, User receiver);
    long countBySenderAndReceiverAndIsReadFalse(User sender, User receiver);
    
    // Get conversation messages between two users
    @Query("SELECT m FROM Message m WHERE " +
           "(m.sender = :user1 AND m.receiver = :user2) OR " +
           "(m.sender = :user3 AND m.receiver = :user4) " +
           "ORDER BY m.createdAt ASC")
    List<Message> findConversationMessages(
        @Param("user1") User user1, 
        @Param("user2") User user2,
        @Param("user3") User user3,
        @Param("user4") User user4
    );
}
