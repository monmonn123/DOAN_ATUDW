package com.edumoet.service.common;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * OpenAI Service - Tích hợp ChatGPT API
 * Sử dụng GPT-3.5-turbo hoặc GPT-4 để trả lời thông minh hơn
 */
@Service
public class OpenAIService {
    
    @Value("${chatbot.openai.enabled:false}")
    private boolean openaiEnabled;
    
    @Value("${chatbot.openai.api-key:}")
    private String apiKey;
    
    @Value("${chatbot.openai.model:gpt-3.5-turbo}")
    private String model;
    
    @Value("${chatbot.openai.max-tokens:500}")
    private int maxTokens;
    
    @Value("${chatbot.openai.temperature:0.7}")
    private double temperature;
    
    private OpenAiService openAiService;
    
    /**
     * Initialize OpenAI service
     */
    @PostConstruct
    public void init() {
        if (openaiEnabled && apiKey != null && !apiKey.isEmpty()) {
            try {
                // Timeout 30 seconds
                this.openAiService = new OpenAiService(apiKey, Duration.ofSeconds(30));
                System.out.println("✅ OpenAI Service initialized successfully with model: " + model);
            } catch (Exception e) {
                System.err.println("❌ Failed to initialize OpenAI Service: " + e.getMessage());
                this.openaiEnabled = false;
            }
        } else {
            System.out.println("ℹ️ OpenAI Service is disabled. Using pattern matching instead.");
        }
    }
    
    /**
     * Check if OpenAI is enabled and ready
     */
    public boolean isEnabled() {
        return openaiEnabled && openAiService != null && apiKey != null && !apiKey.isEmpty();
    }
    
    /**
     * Generate response from ChatGPT
     */
    public String generateResponse(String userMessage, List<String> conversationHistory) {
        if (!isEnabled()) {
            return null;
        }
        
        try {
            // Build conversation messages
            List<ChatMessage> messages = new ArrayList<>();
            
            // System prompt - define chatbot personality in Vietnamese
            messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), 
                "Bạn là trợ lý ảo thông minh của EDUMOET - một nền tảng hỏi đáp về lập trình. " +
                "Nhiệm vụ của bạn là:\n" +
                "1. Hướng dẫn người dùng cách sử dụng website (đặt câu hỏi, trả lời, vote, v.v.)\n" +
                "2. Giải thích về hệ thống điểm reputation và huy hiệu (badges)\n" +
                "3. Hỗ trợ tìm kiếm và quản lý tài khoản\n" +
                "4. Trả lời các câu hỏi về quy định cộng đồng\n" +
                "5. Giúp đỡ với thái độ thân thiện, chuyên nghiệp\n\n" +
                "Hãy trả lời ngắn gọn (2-3 câu), rõ ràng, và sử dụng emoji phù hợp. " +
                "Nếu không biết câu trả lời, hãy khuyên người dùng liên hệ admin hoặc xem phần FAQ."
            ));
            
            // Add conversation history (optional, for context)
            if (conversationHistory != null && !conversationHistory.isEmpty()) {
                int historySize = Math.min(conversationHistory.size(), 6); // Last 6 messages
                for (int i = conversationHistory.size() - historySize; i < conversationHistory.size(); i++) {
                    String msg = conversationHistory.get(i);
                    messages.add(new ChatMessage(ChatMessageRole.USER.value(), msg));
                }
            }
            
            // Add current user message
            messages.add(new ChatMessage(ChatMessageRole.USER.value(), userMessage));
            
            // Create request
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(messages)
                    .maxTokens(maxTokens)
                    .temperature(temperature)
                    .build();
            
            // Call OpenAI API
            ChatCompletionResult result = openAiService.createChatCompletion(request);
            
            // Extract response
            if (result.getChoices() != null && !result.getChoices().isEmpty()) {
                String response = result.getChoices().get(0).getMessage().getContent();
                return response.trim();
            }
            
            return null;
            
        } catch (Exception e) {
            System.err.println("❌ OpenAI API error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Generate response without conversation history (simple mode)
     */
    public String generateResponse(String userMessage) {
        return generateResponse(userMessage, null);
    }
    
    /**
     * Test OpenAI connection
     */
    public boolean testConnection() {
        if (!isEnabled()) {
            return false;
        }
        
        try {
            String testResponse = generateResponse("Xin chào!");
            return testResponse != null && !testResponse.isEmpty();
        } catch (Exception e) {
            System.err.println("❌ OpenAI connection test failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get current model name
     */
    public String getModelName() {
        return model;
    }
    
    /**
     * Get configuration info
     */
    public String getConfigInfo() {
        if (!isEnabled()) {
            return "OpenAI is disabled";
        }
        return String.format("Model: %s, MaxTokens: %d, Temperature: %.1f", 
                           model, maxTokens, temperature);
    }
}

