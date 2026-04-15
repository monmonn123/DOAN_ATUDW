package com.edumoet.service.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.edumoet.entity.ChatbotConversation;
import com.edumoet.entity.ChatbotMessage;
import com.edumoet.entity.User;
import com.edumoet.repository.ChatbotConversationRepository;
import com.edumoet.repository.ChatbotMessageRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chatbot Service với AI Engine
 * Hỗ trợ tiếng Việt với pattern matching thông minh
 */
@Service
@Transactional
public class ChatbotService {
    
    @Autowired
    private ChatbotConversationRepository conversationRepository;
    
    @Autowired
    private ChatbotMessageRepository messageRepository;
    
    @Autowired
    private OpenAIService openAIService;
    
    @Value("${chatbot.openai.enabled:false}")
    private boolean openaiEnabled;
    
    @Value("${chatbot.openai.api-key:}")
    private String openaiApiKey;
    
    // Knowledge Base - FAQ tiếng Việt
    private static final Map<String, List<String>> KNOWLEDGE_BASE = new HashMap<>();
    
    static {
        // Chào hỏi
        KNOWLEDGE_BASE.put("greeting", Arrays.asList(
            "xin chào", "xin chao", "chào bạn", "chao ban", 
            "hello", "hi", "hey", "chào", "chao",
            "hế nhô", "he nho", "hê lô", "he lo"
        ));
        
        // Hỏi về website
        KNOWLEDGE_BASE.put("about_site", Arrays.asList(
            "trang web này là gì", "website này về gì", "đây là gì", "giới thiệu"
        ));
        
        // Hỏi cách đặt câu hỏi
        KNOWLEDGE_BASE.put("how_to_ask", Arrays.asList(
            "làm sao đặt câu hỏi", "cách đặt câu hỏi", "hỏi câu hỏi", "tạo câu hỏi",
            "post câu hỏi", "đăng câu hỏi"
        ));
        
        // Hỏi cách trả lời
        KNOWLEDGE_BASE.put("how_to_answer", Arrays.asList(
            "làm sao trả lời", "cách trả lời", "answer", "câu trả lời"
        ));
        
        // Hỏi về điểm reputation
        KNOWLEDGE_BASE.put("about_reputation", Arrays.asList(
            "điểm là gì", "reputation", "rep", "danh tiếng", "tích lũy điểm", "kiếm điểm"
        ));
        
        // Hỏi về tags
        KNOWLEDGE_BASE.put("about_tags", Arrays.asList(
            "tag là gì", "thẻ là gì", "hashtag", "phân loại"
        ));
        
        // Hỏi về badges
        KNOWLEDGE_BASE.put("about_badges", Arrays.asList(
            "huy hiệu", "badge", "thành tích"
        ));
        
        // Hỏi về tài khoản
        KNOWLEDGE_BASE.put("account_help", Arrays.asList(
            "quên mật khẩu", "đổi mật khẩu", "tài khoản", "profile", "hồ sơ"
        ));
        
        // Hỏi về search
        KNOWLEDGE_BASE.put("search_help", Arrays.asList(
            "tìm kiếm", "search", "làm sao tìm", "tìm câu hỏi"
        ));
        
        // Cảm ơn
        KNOWLEDGE_BASE.put("thanks", Arrays.asList(
            "cảm ơn", "thank", "thanks", "cám ơn"
        ));
        
        // Tạm biệt
        KNOWLEDGE_BASE.put("goodbye", Arrays.asList(
            "tạm biệt", "bye", "goodbye", "hẹn gặp lại"
        ));
    }
    
    // Responses tiếng Việt
    private static final Map<String, String[]> RESPONSES = new HashMap<>();
    
    static {
        RESPONSES.put("greeting", new String[]{
            "Xin chào! 👋 Tôi là trợ lý ảo của EDUMOET. Tôi có thể giúp gì cho bạn?",
            "Chào bạn! 😊 Bạn cần hỗ trợ gì không?",
            "Hello! Rất vui được hỗ trợ bạn!"
        });
        
        RESPONSES.put("about_site", new String[]{
            "EDUMOET là nền tảng hỏi đáp về lập trình và công nghệ. Bạn có thể đặt câu hỏi, trả lời câu hỏi của người khác và tích lũy điểm danh tiếng! 🎓",
            "Đây là cộng đồng học tập và chia sẻ kiến thức lập trình. Bạn có thể tìm kiếm câu trả lời hoặc đóng góp kiến thức của mình! 💡"
        });
        
        RESPONSES.put("how_to_ask", new String[]{
            "Để đặt câu hỏi:\n1. Nhấn nút 'Đặt câu hỏi' ở góc trên\n2. Viết tiêu đề rõ ràng\n3. Mô tả chi tiết vấn đề\n4. Thêm code (nếu có)\n5. Chọn tags phù hợp\n6. Nhấn 'Đăng câu hỏi' ✅",
            "Bạn muốn đặt câu hỏi? Hãy:\n• Viết tiêu đề ngắn gọn\n• Giải thích vấn đề cụ thể\n• Đính kèm code lỗi\n• Gắn thẻ liên quan\nNhấn 'Đặt câu hỏi' để bắt đầu! 🚀"
        });
        
        RESPONSES.put("how_to_answer", new String[]{
            "Để trả lời câu hỏi:\n1. Mở câu hỏi bạn muốn trả lời\n2. Viết câu trả lời chi tiết\n3. Thêm code mẫu nếu cần\n4. Nhấn 'Gửi câu trả lời'\nNếu câu trả lời hay, bạn sẽ được vote up! 👍",
            "Trả lời câu hỏi rất đơn giản! Mở câu hỏi, viết giải pháp của bạn, thêm code example và nhấn submit. Hãy trả lời chất lượng để nhận upvotes! ⭐"
        });
        
        RESPONSES.put("about_reputation", new String[]{
            "Điểm danh tiếng (Reputation) thể hiện đóng góp của bạn:\n• +10 điểm: Câu hỏi được upvote\n• +10 điểm: Câu trả lời được upvote\n• +15 điểm: Câu trả lời được chấp nhận\n• -2 điểm: Câu hỏi/trả lời bị downvote\nCàng nhiều điểm, bạn càng có nhiều đặc quyền! 🏆",
            "Reputation = điểm uy tín của bạn. Đặt câu hỏi hay, trả lời chất lượng để được upvote và tăng điểm. Điểm cao = nhiều quyền lợi! 📈"
        });
        
        RESPONSES.put("about_tags", new String[]{
            "Tags (thẻ) giúp phân loại câu hỏi theo chủ đề. Ví dụ: java, python, javascript, spring-boot...\nBạn nên thêm 1-5 tags phù hợp khi đặt câu hỏi để người khác dễ tìm thấy! 🏷️",
            "Thẻ giúp tổ chức câu hỏi theo chủ đề. Chọn tags chính xác để câu hỏi của bạn tiếp cận đúng chuyên gia! 🎯"
        });
        
        RESPONSES.put("about_badges", new String[]{
            "Huy hiệu (Badges) là thành tích bạn đạt được:\n🥉 Bronze: Thành tích cơ bản\n🥈 Silver: Thành tích trung cấp\n🥇 Gold: Thành tích cao cấp\nHoàn thành nhiệm vụ để thu thập badges! 🏅",
            "Badges là phần thưởng cho những cột mốc quan trọng. Tích cực đóng góp để mở khóa nhiều huy hiệu nhé! 🎖️"
        });
        
        RESPONSES.put("account_help", new String[]{
            "Quên mật khẩu? Nhấn 'Quên mật khẩu' ở trang login và làm theo hướng dẫn.\nĐổi thông tin cá nhân: Vào Profile → Edit Profile.\nCần hỗ trợ thêm? Liên hệ admin! 👤",
            "Vấn đề về tài khoản:\n• Quên MK: Dùng 'Forgot Password'\n• Đổi thông tin: Vào Settings\n• Xóa tài khoản: Liên hệ support\nTôi có thể giúp gì thêm? 🔐"
        });
        
        RESPONSES.put("search_help", new String[]{
            "Tìm kiếm câu hỏi:\n1. Dùng thanh tìm kiếm ở trên cùng\n2. Gõ từ khóa liên quan\n3. Lọc theo tags nếu cần\n4. Sắp xếp kết quả theo: mới nhất, vote nhiều nhất...\nTip: Dùng tag:java để tìm trong tag cụ thể! 🔍",
            "Để tìm kiếm hiệu quả:\n• Gõ từ khóa vào search box\n• Kết hợp với tag filters\n• Sort theo relevance\nHãy thử tìm kiếm ngay! 🎯"
        });
        
        RESPONSES.put("thanks", new String[]{
            "Không có chi! 😊 Tôi luôn sẵn sàng giúp đỡ!",
            "Rất vui được giúp bạn! Có câu hỏi gì cứ hỏi nhé! 🌟",
            "You're welcome! Happy coding! 💻"
        });
        
        RESPONSES.put("goodbye", new String[]{
            "Tạm biệt! Chúc bạn coding vui vẻ! 👋",
            "Hẹn gặp lại! Nếu cần hỗ trợ, tôi luôn ở đây! 😊",
            "Bye bye! See you soon! 🚀"
        });
        
        RESPONSES.put("fallback", new String[]{
            "Xin lỗi, tôi chưa hiểu câu hỏi của bạn. Bạn có thể hỏi về:\n• Cách đặt câu hỏi\n• Cách trả lời\n• Điểm reputation\n• Tags và badges\n• Tài khoản\n• Tìm kiếm\nHoặc liên hệ admin để được hỗ trợ tốt hơn! 🤔",
            "Tôi chưa rõ ý bạn lắm 😅 Bạn muốn hỏi về:\n✓ Hướng dẫn sử dụng?\n✓ Tính năng của web?\n✓ Điểm và huy hiệu?\nHãy nói rõ hơn giúp tôi nhé!",
            "Hmm, câu hỏi này hơi khó với tôi. Bạn có thể:\n1. Hỏi lại bằng cách khác\n2. Xem phần FAQ\n3. Liên hệ support team\nTôi xin lỗi vì chưa giúp được bạn! 🙏"
        });
    }
    
    /**
     * Tạo hoặc lấy conversation hiện tại
     */
    public ChatbotConversation getOrCreateConversation(String sessionId, User user) {
        Optional<ChatbotConversation> existing = conversationRepository.findBySessionId(sessionId);
        
        if (existing.isPresent()) {
            return existing.get();
        }
        
        ChatbotConversation conversation = new ChatbotConversation();
        conversation.setSessionId(sessionId);
        conversation.setUser(user);
        return conversationRepository.save(conversation);
    }
    
    /**
     * Xử lý tin nhắn từ user và trả lời tự động
     */
    public ChatbotMessage processUserMessage(ChatbotConversation conversation, String userMessage) {
        long startTime = System.currentTimeMillis();
        
        // Lưu tin nhắn của user
        ChatbotMessage userMsg = new ChatbotMessage(conversation, userMessage, true);
        messageRepository.save(userMsg);
        
        String botResponse;
        String intent;
        String responseType;
        double confidence;
        
        // Try OpenAI first if enabled
        if (openAIService.isEnabled()) {
            try {
                // Get conversation history for context
                List<ChatbotMessage> history = getConversationHistory(conversation);
                List<String> historyTexts = new ArrayList<>();
                for (ChatbotMessage msg : history) {
                    if (msg.isFromUser()) {
                        historyTexts.add(msg.getContent());
                    }
                }
                
                // Generate response using ChatGPT
                botResponse = openAIService.generateResponse(userMessage, historyTexts);
                
                if (botResponse != null && !botResponse.isEmpty()) {
                    intent = "openai_response";
                    responseType = "openai";
                    confidence = 0.95;
                    System.out.println("✅ Using OpenAI response");
                } else {
                    // Fallback to pattern matching
                    intent = detectIntent(userMessage);
                    botResponse = generateResponse(intent, userMessage);
                    responseType = "pattern_match";
                    confidence = calculateConfidence(intent);
                    System.out.println("⚠️ OpenAI failed, using pattern matching");
                }
            } catch (Exception e) {
                // Fallback to pattern matching on error
                System.err.println("❌ OpenAI error: " + e.getMessage());
                intent = detectIntent(userMessage);
                botResponse = generateResponse(intent, userMessage);
                responseType = "pattern_match";
                confidence = calculateConfidence(intent);
            }
        } else {
            // Use pattern matching
            intent = detectIntent(userMessage);
            botResponse = generateResponse(intent, userMessage);
            responseType = "pattern_match";
            confidence = calculateConfidence(intent);
        }
        
        // Lưu tin nhắn của bot
        ChatbotMessage botMsg = new ChatbotMessage(conversation, botResponse, false);
        botMsg.setIntent(intent);
        botMsg.setResponseType(responseType);
        botMsg.setConfidence(confidence);
        botMsg.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        
        messageRepository.save(botMsg);
        
        return botMsg;
    }
    
    /**
     * Phát hiện intent từ tin nhắn user
     */
    private String detectIntent(String message) {
        String normalizedMessage = normalizeVietnamese(message.toLowerCase().trim());
        
        int maxScore = 0;
        String bestIntent = "fallback";
        
        for (Map.Entry<String, List<String>> entry : KNOWLEDGE_BASE.entrySet()) {
            int score = 0;
            for (String keyword : entry.getValue()) {
                String normalizedKeyword = normalizeVietnamese(keyword.toLowerCase());
                
                // Exact match
                if (normalizedMessage.contains(normalizedKeyword)) {
                    score += 10;
                }
                // Whole word match (better accuracy)
                if (normalizedMessage.equals(normalizedKeyword)) {
                    score += 20;
                }
                // Fuzzy matching
                if (fuzzyMatch(normalizedMessage, normalizedKeyword)) {
                    score += 5;
                }
            }
            
            if (score > maxScore) {
                maxScore = score;
                bestIntent = entry.getKey();
            }
        }
        
        return maxScore > 3 ? bestIntent : "fallback";
    }
    
    /**
     * Tạo response dựa trên intent
     */
    private String generateResponse(String intent, String userMessage) {
        String[] responses = RESPONSES.getOrDefault(intent, RESPONSES.get("fallback"));
        Random random = new Random();
        return responses[random.nextInt(responses.length)];
    }
    
    /**
     * Chuẩn hóa tiếng Việt (bỏ dấu)
     */
    private String normalizeVietnamese(String text) {
        String normalized = text;
        normalized = normalized.replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a");
        normalized = normalized.replaceAll("[èéẹẻẽêềếệểễ]", "e");
        normalized = normalized.replaceAll("[ìíịỉĩ]", "i");
        normalized = normalized.replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o");
        normalized = normalized.replaceAll("[ùúụủũưừứựửữ]", "u");
        normalized = normalized.replaceAll("[ỳýỵỷỹ]", "y");
        normalized = normalized.replaceAll("đ", "d");
        return normalized;
    }
    
    /**
     * Fuzzy matching để xử lý lỗi chính tả
     */
    private boolean fuzzyMatch(String text, String keyword) {
        // Simple Levenshtein-like check
        if (keyword.length() < 4) return false;
        
        String pattern = ".*" + keyword.substring(0, 3) + ".*";
        return text.matches(pattern);
    }
    
    /**
     * Tính confidence score
     */
    private double calculateConfidence(String intent) {
        return "fallback".equals(intent) ? 0.3 : 0.8;
    }
    
    /**
     * Lấy lịch sử chat
     */
    public List<ChatbotMessage> getConversationHistory(ChatbotConversation conversation) {
        return messageRepository.findByConversationOrderByCreatedAtAsc(conversation);
    }
    
    /**
     * Kết thúc conversation
     */
    public void endConversation(ChatbotConversation conversation) {
        conversation.setActive(false);
        conversation.setEndedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
    }
    
    /**
     * Lấy thống kê chatbot
     */
    public Map<String, Object> getChatbotStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalConversations", conversationRepository.count());
        stats.put("totalMessages", messageRepository.count());
        stats.put("averageProcessingTime", messageRepository.getAverageProcessingTime());
        stats.put("commonIntents", messageRepository.findMostCommonIntents());
        stats.put("openaiEnabled", openAIService.isEnabled());
        stats.put("openaiModel", openAIService.getModelName());
        return stats;
    }
}

