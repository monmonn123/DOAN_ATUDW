// ========== UTF-8 Encoding Support ==========
/**
 * UTF-8 Helper Functions - Đảm bảo tất cả AJAX/Fetch requests sử dụng UTF-8
 */

// Default headers cho tất cả fetch requests
const UTF8_HEADERS = {
    'Content-Type': 'application/json; charset=UTF-8',
    'Accept': 'application/json; charset=UTF-8',
    'Accept-Charset': 'UTF-8'
};

// Wrapper function cho fetch với UTF-8 headers
function fetchUTF8(url, options = {}) {
    const headers = new Headers();
    
    // Merge UTF-8 headers với custom headers
    Object.entries(UTF8_HEADERS).forEach(([key, value]) => {
        headers.set(key, value);
    });
    
    if (options.headers) {
        Object.entries(options.headers).forEach(([key, value]) => {
            headers.set(key, value);
        });
    }
    
    return fetch(url, {
        ...options,
        headers: headers
    }).then(response => {
        // Đảm bảo response được decode đúng UTF-8
        const contentType = response.headers.get('Content-Type') || '';
        if (contentType.includes('charset')) {
            return response;
        }
        
        // Nếu không có charset trong response, thêm vào
        if (contentType.includes('text') || contentType.includes('json')) {
            const newHeaders = new Headers(response.headers);
            newHeaders.set('Content-Type', contentType + '; charset=UTF-8');
            return new Response(response.body, {
                status: response.status,
                statusText: response.statusText,
                headers: newHeaders
            });
        }
        
        return response;
    });
}

// Helper để encode URL parameters với UTF-8
function encodeUTF8Params(params) {
    return Object.keys(params).map(key => {
        return encodeURIComponent(key) + '=' + encodeURIComponent(params[key]);
    }).join('&');
}

// Helper để decode UTF-8 text
function decodeUTF8(text) {
    try {
        return decodeURIComponent(escape(text));
    } catch (e) {
        return text;
    }
}

// Override global fetch để tự động thêm UTF-8 headers
if (typeof window !== 'undefined' && window.fetch) {
    const originalFetch = window.fetch;
    window.fetch = function(url, options = {}) {
        // Nếu đã có custom headers, merge với UTF-8 headers
        if (!options.headers) {
            options.headers = {};
        }
        
        const headers = new Headers(options.headers);
        
        // Thêm UTF-8 headers nếu chưa có
        if (!headers.has('Content-Type') && options.method && options.method !== 'GET') {
            headers.set('Content-Type', 'application/json; charset=UTF-8');
        }
        if (!headers.has('Accept')) {
            headers.set('Accept', 'application/json; charset=UTF-8');
        }
        if (!headers.has('Accept-Charset')) {
            headers.set('Accept-Charset', 'UTF-8');
        }
        
        options.headers = headers;
        return originalFetch(url, options);
    };
}

// WebSocket connection handling
let stompClient = null;

function connectWebSocket() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    
    stompClient.connect({}, function(frame) {
        // Subscribe to personal notifications
        const username = document.getElementById('currentUsername').value;
        stompClient.subscribe('/user/' + username + '/queue/notifications', onNotification);
        
        // Subscribe to group notifications if in a group page
        const groupId = document.getElementById('groupId')?.value;
        if (groupId) {
            stompClient.subscribe('/topic/group.' + groupId, onGroupUpdate);
        }
    });
}

// Image handling functions
function handleImageUpload(files, previewContainer, callback) {
    Array.from(files).forEach(file => {
        // Create FormData and send to server
        const formData = new FormData();
        formData.append('file', file);
        
        fetch('/api/images/upload', {
            method: 'POST',
            body: formData
        })
        .then(response => response.json())
        .then(image => {
            // Add preview
            addImagePreview(file, previewContainer);
            // Call callback with image data
            if (callback) callback(image);
        })
        .catch(error => console.error('Error uploading image:', error));
    });
}

function addImagePreview(file, container) {
    const reader = new FileReader();
    reader.onload = function(e) {
        const preview = document.createElement('div');
        preview.className = 'position-relative d-inline-block';
        preview.innerHTML = `
            <img src="${e.target.result}" class="image-preview">
            <button type="button" class="btn-close position-absolute top-0 end-0 m-1" 
                    onclick="this.parentElement.remove()"></button>
        `;
        container.appendChild(preview);
    }
    reader.readAsDataURL(file);
}

// Group functionality
function searchUsers(query) {
        return fetchUTF8(`/api/users/search?q=${encodeURIComponent(query)}`)
        .then(response => response.json());
}

function inviteToGroup(userId, groupId) {
    return fetchUTF8(`/api/groups/${groupId}/members/${userId}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json; charset=UTF-8'
        }
    });
}

// Notification handling
function onNotification(message) {
    const notification = JSON.parse(message.body);
    showToast(notification.title, notification.message);
    updateNotificationBadge();
}

function onGroupUpdate(message) {
    const update = JSON.parse(message.body);
    if (update.type === 'NEW_POST') {
        prependNewPost(update.data);
    } else if (update.type === 'MEMBER_UPDATE') {
        updateMembersList(update.data);
    }
}

function showToast(title, message) {
    const toastContainer = document.getElementById('toastContainer');
    if (!toastContainer) {
        const container = document.createElement('div');
        container.id = 'toastContainer';
        container.className = 'toast-container';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = 'toast';
    toast.innerHTML = `
        <div class="toast-header">
            <strong class="me-auto">${title}</strong>
            <button type="button" class="btn-close" data-bs-dismiss="toast"></button>
        </div>
        <div class="toast-body">${message}</div>
    `;
    
    toastContainer.appendChild(toast);
    const bsToast = new bootstrap.Toast(toast);
    bsToast.show();
    
    toast.addEventListener('hidden.bs.toast', () => toast.remove());
}

// Initialize functionality
document.addEventListener('DOMContentLoaded', function() {
    // Connect to WebSocket
    connectWebSocket();
    
    // Initialize image upload listeners
    const imageInputs = document.querySelectorAll('input[type="file"][accept="image/*"]');
    imageInputs.forEach(input => {
        const previewContainer = document.getElementById(input.dataset.previewContainer);
        input.addEventListener('change', (e) => handleImageUpload(e.target.files, previewContainer));
    });
    
    // Initialize user search in group pages
    const userSearchInput = document.getElementById('userSearch');
    if (userSearchInput) {
        userSearchInput.addEventListener('input', debounce(function() {
            if (this.value.length < 2) return;
            
            searchUsers(this.value)
                .then(users => {
                    const resultsContainer = document.getElementById('userSearchResults');
                    resultsContainer.innerHTML = users.map(user => `
                        <div class="d-flex justify-content-between align-items-center p-2 border-bottom">
                            <span>${user.username}</span>
                            <button onclick="inviteToGroup(${user.id}, ${groupId})" 
                                    class="btn btn-sm btn-primary">Invite</button>
                        </div>
                    `).join('');
                });
        }, 300));
    }
});

// Utility functions
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func.apply(this, args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}