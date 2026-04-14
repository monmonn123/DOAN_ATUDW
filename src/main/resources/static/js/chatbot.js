/**
 * EDUMOET Chatbot Widget
 * Modern AI Chatbot with Vietnamese Support
 */

class ChatbotWidget {
    constructor() {
        this.isOpen = false;
        this.sessionId = this.getOrCreateSessionId();
        this.stompClient = null;
        this.isConnected = false;
        this.messageQueue = [];
        this.isTyping = false;
        
        this.init();
    }
    
    /**
     * Initialize chatbot
     */
    init() {
        this.createWidget();
        this.attachEventListeners();
        this.connectWebSocket();
        this.loadChatHistory();
    }
    
    /**
     * Create widget HTML
     */
    createWidget() {
        const widgetHTML = `
            <div class="chatbot-container">
                <!-- Toggle Button -->
                <button class="chatbot-toggle-btn" id="chatbotToggle">
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M12 2C6.48 2 2 6.48 2 12c0 1.54.36 3 .97 4.29L2 22l5.71-.97C9 21.64 10.46 22 12 22c5.52 0 10-4.48 10-10S17.52 2 12 2zm0 18c-1.38 0-2.67-.33-3.82-.91l-.27-.15-2.85.48.48-2.85-.15-.27C4.33 14.67 4 13.38 4 12c0-4.41 3.59-8 8-8s8 3.59 8 8-3.59 8-8 8z"/>
                        <circle cx="9" cy="12" r="1.5"/>
                        <circle cx="12" cy="12" r="1.5"/>
                        <circle cx="15" cy="12" r="1.5"/>
                    </svg>
                </button>
                
                <!-- Chat Window -->
                <div class="chatbot-window" id="chatbotWindow">
                    <!-- Header -->
                    <div class="chatbot-header">
                        <div class="chatbot-header-left">
                            <div class="chatbot-avatar">🤖</div>
                            <div class="chatbot-header-info">
                                <h3>EDUMOET Bot</h3>
                                <p class="chatbot-status">
                                    <span class="status-dot"></span>
                                    Trực tuyến
                                </p>
                            </div>
                        </div>
                        <button class="chatbot-close-btn" id="chatbotClose">
                            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <line x1="18" y1="6" x2="6" y2="18"></line>
                                <line x1="6" y1="6" x2="18" y2="18"></line>
                            </svg>
                        </button>
                    </div>
                    
                    <!-- Messages -->
                    <div class="chatbot-messages" id="chatbotMessages">
                        <div class="welcome-message">
                            <div class="welcome-message-icon">👋</div>
                            <h4>Chào mừng đến với EDUMOET!</h4>
                            <p>Tôi là trợ lý ảo. Tôi có thể giúp bạn về:<br>
                            • Cách sử dụng website<br>
                            • Đặt câu hỏi & trả lời<br>
                            • Điểm và huy hiệu<br>
                            • Và nhiều hơn nữa!</p>
                        </div>
                    </div>
                    
                    <!-- Quick Replies -->
                    <div class="quick-replies" id="quickReplies">
                        <button class="quick-reply-btn" data-message="Cách đặt câu hỏi?">📝 Cách đặt câu hỏi?</button>
                        <button class="quick-reply-btn" data-message="Điểm reputation là gì?">⭐ Điểm reputation</button>
                        <button class="quick-reply-btn" data-message="Giới thiệu trang web">ℹ️ Giới thiệu</button>
                    </div>
                    
                    <!-- Input Area -->
                    <div class="chatbot-input-area">
                        <input 
                            type="text" 
                            class="chatbot-input" 
                            id="chatbotInput" 
                            placeholder="Nhập tin nhắn..."
                            autocomplete="off"
                        />
                        <button class="chatbot-send-btn" id="chatbotSend" disabled>
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor">
                                <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/>
                            </svg>
                        </button>
                    </div>
                </div>
            </div>
        `;
        
        document.body.insertAdjacentHTML('beforeend', widgetHTML);
    }
    
    /**
     * Attach event listeners
     */
    attachEventListeners() {
        // Toggle button
        document.getElementById('chatbotToggle').addEventListener('click', () => {
            this.toggleChat();
        });
        
        // Close button
        document.getElementById('chatbotClose').addEventListener('click', () => {
            this.toggleChat();
        });
        
        // Send button
        document.getElementById('chatbotSend').addEventListener('click', () => {
            this.sendMessage();
        });
        
        // Input field
        const input = document.getElementById('chatbotInput');
        input.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                this.sendMessage();
            }
        });
        
        input.addEventListener('input', (e) => {
            const sendBtn = document.getElementById('chatbotSend');
            sendBtn.disabled = e.target.value.trim().length === 0;
        });
        
        // Quick reply buttons
        document.getElementById('quickReplies').addEventListener('click', (e) => {
            if (e.target.classList.contains('quick-reply-btn')) {
                const message = e.target.getAttribute('data-message');
                this.sendMessage(message);
            }
        });
    }
    
    /**
     * Toggle chat window
     */
    toggleChat() {
        this.isOpen = !this.isOpen;
        const window = document.getElementById('chatbotWindow');
        const toggleBtn = document.getElementById('chatbotToggle');
        
        if (this.isOpen) {
            window.classList.add('active');
            toggleBtn.classList.add('active');
            document.getElementById('chatbotInput').focus();
        } else {
            window.classList.remove('active');
            toggleBtn.classList.remove('active');
        }
    }
    
    /**
     * Connect to WebSocket
     */
    connectWebSocket() {
        const socket = new SockJS('/ws');
        this.stompClient = Stomp.over(socket);
        
        // Disable console logs
        this.stompClient.debug = null;
        
        this.stompClient.connect({}, (frame) => {
            console.log('Chatbot WebSocket connected');
            this.isConnected = true;
            
            // Subscribe to chatbot responses
            this.stompClient.subscribe('/topic/chatbot/' + this.sessionId, (message) => {
                const response = JSON.parse(message.body);
                this.displayBotMessage(response.content, response.timestamp);
            });
            
            // Process queued messages
            this.processMessageQueue();
        }, (error) => {
            console.error('WebSocket connection error:', error);
            this.isConnected = false;
            
            // Retry connection after 3 seconds
            setTimeout(() => this.connectWebSocket(), 3000);
        });
    }
    
    /**
     * Send message
     */
    sendMessage(text = null) {
        const input = document.getElementById('chatbotInput');
        const message = text || input.value.trim();
        
        if (!message) return;
        
        // Display user message
        this.displayUserMessage(message);
        
        // Clear input
        if (!text) {
            input.value = '';
            document.getElementById('chatbotSend').disabled = true;
        }
        
        // Send to server
        if (this.isConnected) {
            this.stompClient.send('/app/chatbot/send', {}, JSON.stringify({
                sessionId: this.sessionId,
                message: message
            }));
            
            // Show typing indicator
            this.showTypingIndicator();
        } else {
            // Queue message if not connected
            this.messageQueue.push(message);
            this.displayBotMessage('Đang kết nối... Vui lòng đợi giây lát.', this.getCurrentTime());
        }
    }
    
    /**
     * Display user message
     */
    displayUserMessage(message) {
        const messagesContainer = document.getElementById('chatbotMessages');
        
        // Remove welcome message if exists
        const welcomeMsg = messagesContainer.querySelector('.welcome-message');
        if (welcomeMsg) welcomeMsg.remove();
        
        const messageHTML = `
            <div class="message-bubble message-user">
                ${this.escapeHtml(message)}
                <span class="message-time">${this.getCurrentTime()}</span>
            </div>
        `;
        
        messagesContainer.insertAdjacentHTML('beforeend', messageHTML);
        this.scrollToBottom();
    }
    
    /**
     * Display bot message
     */
    displayBotMessage(message, time = null) {
        // Remove typing indicator
        this.hideTypingIndicator();
        
        const messagesContainer = document.getElementById('chatbotMessages');
        
        // Remove welcome message if exists
        const welcomeMsg = messagesContainer.querySelector('.welcome-message');
        if (welcomeMsg) welcomeMsg.remove();
        
        const messageHTML = `
            <div class="message-bubble message-bot">
                ${this.formatMessage(message)}
                <span class="message-time">${time || this.getCurrentTime()}</span>
            </div>
        `;
        
        messagesContainer.insertAdjacentHTML('beforeend', messageHTML);
        this.scrollToBottom();
    }
    
    /**
     * Show typing indicator
     */
    showTypingIndicator() {
        if (this.isTyping) return;
        this.isTyping = true;
        
        const messagesContainer = document.getElementById('chatbotMessages');
        const typingHTML = `
            <div class="typing-indicator" id="typingIndicator">
                <span></span>
                <span></span>
                <span></span>
            </div>
        `;
        
        messagesContainer.insertAdjacentHTML('beforeend', typingHTML);
        this.scrollToBottom();
    }
    
    /**
     * Hide typing indicator
     */
    hideTypingIndicator() {
        this.isTyping = false;
        const indicator = document.getElementById('typingIndicator');
        if (indicator) indicator.remove();
    }
    
    /**
     * Format message (support line breaks, emojis)
     */
    formatMessage(message) {
        return this.escapeHtml(message)
            .replace(/\n/g, '<br>')
            .replace(/•/g, '•');
    }
    
    /**
     * Escape HTML
     */
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
    
    /**
     * Get current time
     */
    getCurrentTime() {
        const now = new Date();
        return now.getHours().toString().padStart(2, '0') + ':' + 
               now.getMinutes().toString().padStart(2, '0');
    }
    
    /**
     * Scroll to bottom
     */
    scrollToBottom() {
        const messagesContainer = document.getElementById('chatbotMessages');
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }
    
    /**
     * Get or create session ID
     */
    getOrCreateSessionId() {
        let sessionId = localStorage.getItem('chatbot_session_id');
        if (!sessionId) {
            sessionId = 'session_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
            localStorage.setItem('chatbot_session_id', sessionId);
        }
        return sessionId;
    }
    
    /**
     * Load chat history
     */
    loadChatHistory() {
        fetch('/chatbot/history?sessionId=' + this.sessionId)
            .then(response => response.json())
            .then(messages => {
                if (messages.length > 0) {
                    const messagesContainer = document.getElementById('chatbotMessages');
                    const welcomeMsg = messagesContainer.querySelector('.welcome-message');
                    if (welcomeMsg) welcomeMsg.remove();
                    
                    messages.forEach(msg => {
                        if (msg.fromUser) {
                            this.displayUserMessage(msg.content);
                        } else {
                            this.displayBotMessage(msg.content, msg.timestamp);
                        }
                    });
                }
            })
            .catch(error => console.error('Error loading chat history:', error));
    }
    
    /**
     * Process message queue
     */
    processMessageQueue() {
        while (this.messageQueue.length > 0) {
            const message = this.messageQueue.shift();
            this.sendMessage(message);
        }
    }
}

// Initialize chatbot when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    // Check if SockJS and Stomp are loaded
    if (typeof SockJS !== 'undefined' && typeof Stomp !== 'undefined') {
        window.chatbot = new ChatbotWidget();
        console.log('Chatbot initialized successfully!');
    } else {
        console.error('SockJS or Stomp.js not loaded. Chatbot cannot initialize.');
    }
});

