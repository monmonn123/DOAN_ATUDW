package com.edumoet.controller.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.edumoet.entity.Attachment;
import com.edumoet.entity.Message;
import com.edumoet.entity.User;
import com.edumoet.service.common.FileStorageService;
import com.edumoet.service.common.MessageService;
import com.edumoet.service.common.UserService;
import com.edumoet.service.common.WebSocketService;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * ✅ Message Controller — hỗ trợ chat real-time + gửi tin truyền thống
 * ĐÃ FIX: avatar hiển thị đúng từ AWS S3.
 */
@Controller
@RequestMapping("/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // ------------------------------------------------------------
    // 🧩 DTOs
    // ------------------------------------------------------------
    public static class ChatMessage {
        private String from;
        private String to;
        private String content;
        private LocalDateTime timestamp;
        private Long messageId;
        public ChatMessage() {}
        public ChatMessage(String from, String to, String content) {
            this.from = from; this.to = to; this.content = content;
            this.timestamp = LocalDateTime.now();
        }
        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
        public String getTo() { return to; }
        public void setTo(String to) { this.to = to; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        public Long getMessageId() { return messageId; }
        public void setMessageId(Long messageId) { this.messageId = messageId; }
    }

    public static class ChatPayload {
        private String recipientUsername;
        private String content;
        private String subject;
        private List<String> attachmentFilenames;
        public ChatPayload() {}
        public String getRecipientUsername() { return recipientUsername; }
        public void setRecipientUsername(String recipientUsername) { this.recipientUsername = recipientUsername; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public List<String> getAttachmentFilenames() { return attachmentFilenames; }
        public void setAttachmentFilenames(List<String> attachmentFilenames) { this.attachmentFilenames = attachmentFilenames; }
    }

    // ------------------------------------------------------------
    // 🧠 Helper: tạo URL avatar hợp lệ (S3 hoặc fallback)
    // ------------------------------------------------------------
    private String resolveAvatarUrl(String profileImage, String username, int size) {
        // Always ensure we return a valid URL, never null or empty
        if (profileImage != null && !profileImage.trim().isEmpty()) {
            String trimmed = profileImage.trim();
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                return trimmed;
            }
            return "https://tungbacket.s3.ap-southeast-1.amazonaws.com/ltWeb/avatars/" + trimmed;
        }
        // Fallback to UI Avatars service with username
        String safeUsername = (username != null && !username.trim().isEmpty()) 
            ? username.trim().replaceAll("\\s+", "+") 
            : "User";
        return "https://ui-avatars.com/api/?name=" +
                safeUsername +
                "&size=" + size +
                "&background=0D6EFD&color=fff";
    }

    // ------------------------------------------------------------
    // 📥 Inbox — danh sách hội thoại
    // ------------------------------------------------------------
    @GetMapping("/inbox")
    public String inbox(Principal principal, Model model) {
        try {
            if (principal == null) {
                return "redirect:/login";
            }
            
            User current = userService.findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<Map<String, Object>> conversations = new ArrayList<>();
            var summaries = messageService.getConversationSummaries(current);

            for (MessageService.ConversationSummary s : summaries) {
                if (s == null || s.getPartner() == null) {
                    continue; // Skip invalid summaries
                }
                
                Map<String, Object> item = new HashMap<>();
                String username = s.getPartner().getUsername() != null ? s.getPartner().getUsername() : "Unknown";
                item.put("username", username);
                item.put("displayName", username);
                item.put("avatar", resolveAvatarUrl(s.getPartner().getProfileImage(), username, 48));

                if (s.getLastMessage() != null) {
                    item.put("lastMessage", s.getLastMessage().getBody() != null ? s.getLastMessage().getBody() : "");
                    item.put("lastTimestamp", s.getLastTimestamp());
                    // Format timestamp for JavaScript (ISO 8601 format)
                    if (s.getLastTimestamp() != null) {
                        item.put("lastTimestampISO", s.getLastTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    } else {
                        item.put("lastTimestampISO", null);
                    }
                } else {
                    item.put("lastMessage", "");
                    item.put("lastTimestamp", null);
                    item.put("lastTimestampISO", null);
                }

                item.put("unread", s.getUnreadCount());
                conversations.add(item);
            }

            model.addAttribute("conversations", conversations);
            model.addAttribute("currentUser", current);
            model.addAttribute("pageTitle", "Tin nhắn");
            return "messages/inbox";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Có lỗi xảy ra khi tải hộp thư: " + e.getMessage());
            model.addAttribute("conversations", new ArrayList<>());
            return "messages/inbox";
        }
    }

    // ------------------------------------------------------------
    // 📤 Sent messages
    // ------------------------------------------------------------
    @GetMapping("/sent")
    public String sent(Principal principal,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {

        if (principal == null) throw new RuntimeException("Not authenticated");
        User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Pageable pageable = PageRequest.of(page, 20);
        var messages = messageService.getSentMessages(user, pageable);

        model.addAttribute("messages", messages);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", messages.getTotalPages());
        model.addAttribute("pageTitle", "Sent Messages");
        return "messages/sent";
    }

    // ------------------------------------------------------------
    // 📝 Compose redirect
    // ------------------------------------------------------------
    @GetMapping("/compose")
    public String compose(@RequestParam(required = false) String to, Model model) {
        return "redirect:/messages/inbox";
    }

    // ------------------------------------------------------------
    // ✉️ Send message
    // ------------------------------------------------------------
    @PostMapping("/send")
    public String sendMessage(
            @RequestParam String to,
            @RequestParam String subject,
            @RequestParam String body,
            @RequestParam(required = false) List<MultipartFile> attachments,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        try {
            if (principal == null) throw new RuntimeException("Not authenticated");

            User sender = userService.findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("Sender not found"));
            User receiver = userService.findByUsername(to)
                    .orElseThrow(() -> new RuntimeException("Recipient not found"));

            Message message = messageService.sendMessage(sender, receiver, subject, body);

            if (attachments != null && !attachments.isEmpty()) {
                for (MultipartFile file : attachments) {
                    if (!file.isEmpty()) {
                        Attachment attachment = fileStorageService.store(file);
                    }
                }
            }

            webSocketService.notifyUser(receiver,
                    "New Message",
                    "You have a new message from " + sender.getUsername(),
                    "MESSAGE");

            redirectAttributes.addFlashAttribute("successMessage", "✅ Message sent successfully!");
            return "redirect:/messages/sent";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Error: " + e.getMessage());
            return "redirect:/messages/compose?to=" + to;
        }
    }

    // ------------------------------------------------------------
    // 📄 View message
    // ------------------------------------------------------------
    @GetMapping("/{id}")
    public String viewMessage(@PathVariable Long id,
                              Principal principal,
                              Model model,
                              RedirectAttributes redirectAttributes) {

        try {
            if (principal == null) throw new RuntimeException("Not authenticated");
            Message message = messageService.getMessage(id);
            User currentUser = userService.findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!message.getReceiver().equals(currentUser)
                    && !message.getSender().equals(currentUser)) {
                redirectAttributes.addFlashAttribute("errorMessage", "You don't have permission");
                return "redirect:/messages/inbox";
            }

            if (message.getReceiver().equals(currentUser) && !message.getIsRead()) {
                messageService.markAsRead(id);
            }

            model.addAttribute("message", message);
            model.addAttribute("pageTitle", message.getSubject());
            return "messages/view";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/messages/inbox";
        }
    }

    // ------------------------------------------------------------
    // 🗑️ Delete
    // ------------------------------------------------------------
    @PostMapping("/{id}/delete")
    public String deleteMessage(@PathVariable Long id,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        try {
            if (principal == null) throw new RuntimeException("Not authenticated");
            Message message = messageService.getMessage(id);
            User currentUser = userService.findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!message.getReceiver().equals(currentUser)
                    && !message.getSender().equals(currentUser)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Permission denied");
                return "redirect:/messages/inbox";
            }

            messageService.deleteMessage(id);
            redirectAttributes.addFlashAttribute("successMessage", "✅ Message deleted!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Error: " + e.getMessage());
        }
        return "redirect:/messages/inbox";
    }

    // ------------------------------------------------------------
    // 🔍 Search users (API)
    // ------------------------------------------------------------
    @GetMapping("/api/search-users")
    @ResponseBody
    public ResponseEntity<?> searchUsers(@RequestParam(required = false, defaultValue = "") String query,
                                         Principal principal) {
        try {
            if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            List<User> users;
            if (query == null || query.trim().isEmpty()) {
                Pageable pageable = PageRequest.of(0, 50);
                users = userService.getAllUsers(pageable).getContent();
            } else if (query.trim().length() < 2) {
                return ResponseEntity.ok(Collections.emptyList());
            } else {
                users = userService.searchByUsernameOrEmail(query.trim());
            }

            String currentUsername = principal.getName();

            List<Map<String, Object>> results = users.stream()
                    .filter(u -> !u.getUsername().equals(currentUsername))
                    .limit(50)
                    .map(u -> {
                        Map<String, Object> dto = new HashMap<>();
                        dto.put("username", u.getUsername());
                        dto.put("email", u.getEmail());
                        dto.put("reputation", u.getReputation());
                        dto.put("avatar", resolveAvatarUrl(u.getProfileImage(), u.getUsername(), 40));
                        return dto;
                    })
                    .toList();

            return ResponseEntity.ok(results);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ------------------------------------------------------------
    // 📎 Download attachment
    // ------------------------------------------------------------
    @GetMapping("/attachments/{filename:.+}")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable String filename) {
        try {
            Resource resource = fileStorageService.loadAsResource(filename);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ------------------------------------------------------------
    // ✅ Mark read
    // ------------------------------------------------------------
    @PostMapping("/{id}/mark-read")
    @ResponseBody
    public ResponseEntity<?> markAsRead(@PathVariable Long id, Principal principal) {
        try {
            if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            messageService.markAsRead(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ------------------------------------------------------------
    // 💬 Get conversation (AJAX)
    // ------------------------------------------------------------
    @GetMapping("/api/conversation/{username}")
    @ResponseBody
    public ResponseEntity<?> getConversation(@PathVariable String username,
                                             @RequestParam(defaultValue = "0") int page,
                                             Principal principal) {
        try {
            if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            User currentUser = userService.findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            User partner = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Partner not found"));

            List<Message> messages = messageService.getConversationMessages(currentUser, partner);

            List<Map<String, Object>> result = messages.stream().map(msg -> {
                Map<String, Object> dto = new HashMap<>();
                dto.put("id", msg.getId());
                dto.put("from", msg.getSender().getUsername());
                dto.put("to", msg.getReceiver().getUsername());
                // Content from database is already UTF-8 encoded (configured in application.properties)
                dto.put("content", msg.getBody() != null ? msg.getBody() : "");
                dto.put("timestamp", msg.getCreatedAt());
                dto.put("isRead", msg.getIsRead());
                dto.put("isMine", msg.getSender().equals(currentUser));
                return dto;
            }).toList();

            // Ensure UTF-8 encoding in response header
            HttpHeaders headers = new HttpHeaders();
            MediaType mediaType = new MediaType("application", "json", java.nio.charset.StandardCharsets.UTF_8);
            headers.setContentType(mediaType);
            return ResponseEntity.ok().headers(headers).body(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ------------------------------------------------------------
    // 🛰️ WebSocket send
    // ------------------------------------------------------------
    @MessageMapping("/chat.send")
    public void sendChatMessage(@Payload ChatMessage chatMessage, Principal principal) {
        try {
            if (principal == null) return;

            User sender = userService.findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("Sender not found"));
            User receiver = userService.findByUsername(chatMessage.getTo())
                    .orElseThrow(() -> new RuntimeException("Receiver not found"));

            Message message = messageService.sendMessage(sender, receiver, "Chat", chatMessage.getContent());

            chatMessage.setMessageId(message.getId());
            chatMessage.setFrom(sender.getUsername());
            chatMessage.setTimestamp(message.getCreatedAt());

            messagingTemplate.convertAndSendToUser(receiver.getUsername(), "/queue/messages", chatMessage);
            messagingTemplate.convertAndSendToUser(sender.getUsername(), "/queue/messages", chatMessage);

        } catch (Exception e) {
            System.err.println("Error sending chat message: " + e.getMessage());
        }
    }
}
