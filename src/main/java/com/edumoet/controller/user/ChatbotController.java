package com.edumoet.controller.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.edumoet.entity.ChatbotConversation;
import com.edumoet.entity.ChatbotMessage;
import com.edumoet.entity.User;
import com.edumoet.service.common.ChatbotService;
import com.edumoet.service.common.UserService;

import jakarta.servlet.http.HttpSession;
import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Chatbot Controller
 * Xử lý REST API và WebSocket cho chatbot
 */
@Controller
@RequestMapping("/chatbot")
public class ChatbotController {
    
    @Autowired
    private ChatbotService chatbotService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    /**
     * DTO cho tin nhắn chatbot
     */
    public static class ChatbotMessageDTO {
        private String content;
        private boolean isFromUser;
        private String timestamp;
        private String intent;
        private Double confidence;
        
        public ChatbotMessageDTO() {}
        
        public ChatbotMessageDTO(ChatbotMessage message) {
            this.content = message.getContent();
            this.isFromUser = message.isFromUser();
            this.timestamp = message.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm"));
            this.intent = message.getIntent();
            this.confidence = message.getConfidence();
        }
        
        // Getters and Setters
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public boolean isFromUser() { return isFromUser; }
        public void setFromUser(boolean fromUser) { isFromUser = fromUser; }
        
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        
        public String getIntent() { return intent; }
        public void setIntent(String intent) { this.intent = intent; }
        
        public Double getConfidence() { return confidence; }
        public void setConfidence(Double confidence) { this.confidence = confidence; }
    }
    
    /**
     * WebSocket endpoint: Nhận tin nhắn từ client
     */
    @MessageMapping("/chatbot/send")
    public void handleChatMessage(@Payload Map<String, String> payload, Principal principal) {
        String sessionId = payload.get("sessionId");
        String message = payload.get("message");
        
        if (sessionId == null || message == null || message.trim().isEmpty()) {
            return;
        }
        
        // Lấy user (nếu đã đăng nhập)
        User user = null;
        if (principal != null) {
            user = userService.findByUsername(principal.getName()).orElse(null);
        }
        
        // Lấy hoặc tạo conversation
        ChatbotConversation conversation = chatbotService.getOrCreateConversation(sessionId, user);
        
        // Xử lý tin nhắn và nhận response từ bot
        ChatbotMessage botResponse = chatbotService.processUserMessage(conversation, message);
        
        // Gửi response về client qua WebSocket
        ChatbotMessageDTO responseDTO = new ChatbotMessageDTO(botResponse);
        messagingTemplate.convertAndSend("/topic/chatbot/" + sessionId, responseDTO);
    }
    
    /**
     * REST API: Lấy lịch sử chat
     */
    @GetMapping("/history")
    @ResponseBody
    public ResponseEntity<List<ChatbotMessageDTO>> getChatHistory(
            @RequestParam String sessionId,
            Principal principal) {
        
        User user = null;
        if (principal != null) {
            user = userService.findByUsername(principal.getName()).orElse(null);
        }
        
        ChatbotConversation conversation = chatbotService.getOrCreateConversation(sessionId, user);
        List<ChatbotMessage> messages = chatbotService.getConversationHistory(conversation);
        
        List<ChatbotMessageDTO> dtos = messages.stream()
                .map(ChatbotMessageDTO::new)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }
    
    /**
     * REST API: Tạo session mới
     */
    @PostMapping("/session/new")
    @ResponseBody
    public ResponseEntity<Map<String, String>> createNewSession(Principal principal) {
        String sessionId = UUID.randomUUID().toString();
        
        User user = null;
        if (principal != null) {
            user = userService.findByUsername(principal.getName()).orElse(null);
        }
        
        chatbotService.getOrCreateConversation(sessionId, user);
        
        Map<String, String> response = new HashMap<>();
        response.put("sessionId", sessionId);
        response.put("message", "Session created successfully");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * REST API: Kết thúc session
     */
    @PostMapping("/session/end")
    @ResponseBody
    public ResponseEntity<Map<String, String>> endSession(@RequestParam String sessionId) {
        ChatbotConversation conversation = chatbotService.getOrCreateConversation(sessionId, null);
        chatbotService.endConversation(conversation);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Session ended successfully");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * REST API: Test chatbot (không qua WebSocket)
     */
    @PostMapping("/test")
    @ResponseBody
    public ResponseEntity<ChatbotMessageDTO> testChatbot(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        String sessionId = UUID.randomUUID().toString();
        
        ChatbotConversation conversation = chatbotService.getOrCreateConversation(sessionId, null);
        ChatbotMessage botResponse = chatbotService.processUserMessage(conversation, message);
        
        return ResponseEntity.ok(new ChatbotMessageDTO(botResponse));
    }
    
    /**
     * REST API: Lấy thống kê chatbot (Admin only)
     */
    @GetMapping("/admin/statistics")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getChatbotStatistics() {
        Map<String, Object> stats = chatbotService.getChatbotStatistics();
        return ResponseEntity.ok(stats);
    }
}

