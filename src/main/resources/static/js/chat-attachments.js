// chat-attachments.js
// Handles UI interactions, WebSocket (STOMP) realtime chat, attachments upload, recall/delete, conversation list reorder.
// Endpoints used:
// - /messages/api/upload  (POST multipart files) -> returns uploaded filenames & urls
// - /messages/api/conversation/{username} (GET)
// - /messages/api/mark-read/{username} (POST)
// - /messages/api/recall/{id} (POST)
// - /messages/api/delete/{id} (POST)
// STOMP:
// - send to /app/chat.send
// - subscribe /user/queue/messages

(function(){
    'use strict';

    const CURRENT_USER = document.body.dataset.currentUser || null;
    let stompClient = null;
    let currentPartner = null;
    const conversationsCache = new Map();

    // Utilities
    const $ = s => document.querySelector(s);
    const $$ = s => Array.from(document.querySelectorAll(s));
    function escapeHtml(s){ if(!s) return ''; return String(s).replace(/[&<>"'`=\/]/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;','/':'&#x2F;','`':'&#x60;','=':'&#x3D;'}[c])); }
    function timeShort(iso){ if(!iso) return ''; const d=new Date(iso); return `${('0'+d.getHours()).slice(-2)}:${('0'+d.getMinutes()).slice(-2)}`; }
    function timeFull(iso){ if(!iso) return ''; const d=new Date(iso); return `${('0'+d.getHours()).slice(-2)}:${('0'+d.getMinutes()).slice(-2)} ${('0'+d.getDate()).slice(-2)}/${('0'+(d.getMonth()+1)).slice(-2)}`; }

    // initialize STOMP
    function connect() {
        const socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);
        stompClient.connect({}, frame => {
            console.log('STOMP connected', frame);
            stompClient.subscribe('/user/queue/messages', m => {
                try {
                    const p = JSON.parse(m.body);
                    handleIncoming(p);
                } catch(e) { console.error('Invalid payload', e); }
            });
        }, err => console.error('STOMP error', err));
    }

    // handle incoming WS payloads
    function handleIncoming(p) {
        if (!p || !p.type) return;
        if (p.type === 'CHAT') {
            const partner = (CURRENT_USER === p.senderUsername) ? p.recipientUsername : p.senderUsername;
            const s = conversationsCache.get(partner) || {username: partner, displayName: partner, avatar:'/images/default-avatar.png', lastMessage: '', lastTimestamp: null, unread:0};
            s.lastMessage = p.content;
            s.lastTimestamp = p.timestamp;
            if (p.recipientUsername === CURRENT_USER && (!currentPartner || currentPartner !== p.senderUsername)) {
                s.unread = (s.unread || 0) + 1;
            }
            conversationsCache.set(partner, s);
            renderConversationList();

            // if chat open with partner, append message & mark read
            if (currentPartner && (currentPartner === p.senderUsername || currentPartner === p.recipientUsername)) {
                appendMessageToChat({
                    id: p.id || Date.now(),
                    content: p.content,
                    senderUsername: p.senderUsername,
                    createdAt: p.timestamp,
                    attachments: p.attachments || []
                });
                if (p.recipientUsername === CURRENT_USER && p.senderUsername === currentPartner) {
                    fetch(`/messages/api/mark-read/${encodeURIComponent(currentPartner)}`, {method:'POST'}).catch(()=>{});
                    // clear unread badge locally
                    const el = document.querySelector(`#conversationsList [data-username="${CSS.escape(currentPartner)}"] .conv-unread`);
                    if (el) el.remove();
                }
            }
        } else if (p.type === 'RECALL') {
            const node = document.querySelector(`[data-message-id="${p.messageId}"] .msg-text`);
            if (node) node.textContent = '[Tin nhắn đã được thu hồi]';
            showToast('Một tin nhắn đã bị thu hồi', 'info');
        } else if (p.type === 'DELETE') {
            const node = document.querySelector(`[data-message-id="${p.messageId}"]`);
            if (node) node.remove();
            showToast('Một tin nhắn đã bị xóa', 'info');
        } else if (p.type === 'CONVERSATION_UPDATE') {
            // update summary
            const partner = p.partnerUsername;
            const s = conversationsCache.get(partner) || {username:partner, displayName:partner, avatar:'/images/default-avatar.png'};
            if (p.lastMessage) s.lastMessage = p.lastMessage;
            if (p.lastTimestamp) s.lastTimestamp = p.lastTimestamp;
            if (typeof p.unread !== 'undefined') s.unread = p.unread;
            conversationsCache.set(partner, s);
            renderConversationList();
        }
    }

    // conversation list rendering
    function renderConversationList(){
        const list = $('#conversationsList');
        if(!list) return;
        list.innerHTML = '';
        const arr = Array.from(conversationsCache.values()).sort((a,b)=>{
            const ta = a.lastTimestamp ? new Date(a.lastTimestamp).getTime() : 0;
            const tb = b.lastTimestamp ? new Date(b.lastTimestamp).getTime() : 0;
            return tb - ta;
        });
        if(arr.length === 0) { list.innerHTML = '<div class="conv-empty">Chưa có cuộc trò chuyện</div>'; return; }
        arr.forEach(s => {
            const el = document.createElement('div');
            el.className = 'conv-item';
            el.setAttribute('data-username', s.username);
            el.setAttribute('data-display', s.displayName || s.username);
            el.setAttribute('data-avatar', s.avatar || '/images/default-avatar.png');
            el.innerHTML = `<img class="conv-avatar" src="${escapeHtml(s.avatar||'/images/default-avatar.png')}"/>
                <div class="conv-main">
                    <div class="conv-row">
                        <div class="conv-name">${escapeHtml(s.displayName||s.username)}</div>
                        <div class="conv-time">${s.lastTimestamp?timeShort(s.lastTimestamp):''}</div>
                    </div>
                    <div class="conv-row conv-sub">
                        <div class="conv-preview">${escapeHtml(s.lastMessage||'')}</div>
                        ${s.unread && s.unread>0 ? `<div class="conv-unread">${s.unread}</div>` : ''}
                    </div>
                </div>`;
            list.appendChild(el);
        });
    }

    // load conversation messages for a partner
    async function loadConversation(partner) {
        const area = $('#chatMessages');
        area.innerHTML = '<div class="text-muted text-center">Đang tải...</div>';
        try {
            const res = await fetch(`/messages/api/conversation/${encodeURIComponent(partner)}?page=0&size=200`);
            if (!res.ok) throw new Error('HTTP ' + res.status);
            const json = await res.json();
            area.innerHTML = '';
            (json.messages || []).forEach(m => appendMessageToChat(m));
            area.scrollTop = area.scrollHeight;
        } catch (e) {
            console.error(e);
            area.innerHTML = '<div class="text-muted text-center">Không thể tải</div>';
        }
    }

    // append single message to chat area
    function appendMessageToChat(m) {
        const area = $('#chatMessages');
        if (!area) return;
        const isMe = (m.senderUsername === CURRENT_USER);
        const row = document.createElement('div');
        row.className = 'msg-row ' + (isMe ? 'me' : 'them');
        row.setAttribute('data-message-id', m.id || '');
        const attachmentsHtml = (m.attachments && m.attachments.length)
            ? `<div class="attachments">${m.attachments.map(a => `<div class="att"><a href="${a.url}" target="_blank">${escapeHtml(a.originalName||a.filename)}</a></div>`).join('')}</div>`
            : '';
        row.innerHTML = `${isMe ? '' : `<img class="msg-avatar" src="${escapeHtml(conversationsCache.get(m.senderUsername)?.avatar||'/images/default-avatar.png')}"/>`}
            <div class="msg-bubble ${isMe ? 'me' : 'them'}">
                <div class="msg-text">${escapeHtml(m.content)}</div>
                ${attachmentsHtml}
                <div class="msg-meta">${timeFull(m.createdAt || m.timestamp)}</div>
            </div>`;
        // right-click menu for recall/delete
        row.querySelector('.msg-bubble').addEventListener('contextmenu', ev => {
            ev.preventDefault();
            const mid = row.getAttribute('data-message-id');
            showCtxMenu(ev.pageX, ev.pageY, mid, m.senderUsername === CURRENT_USER);
        });
        area.appendChild(row);
        area.scrollTop = area.scrollHeight;
    }

    // context menu
    function showCtxMenu(x,y,messageId,isSender) {
        const old = $('#ctxMenu'); if (old) old.remove();
        const menu = document.createElement('div');
        menu.id = 'ctxMenu';
        menu.style.position = 'absolute';
        menu.style.left = x + 'px';
        menu.style.top = y + 'px';
        menu.style.background = '#fff';
        menu.style.border = '1px solid #ddd';
        menu.style.padding = '6px';
        menu.style.borderRadius = '6px';
        menu.style.zIndex = 2000;
        if (isSender) {
            const recall = document.createElement('div');
            recall.textContent = 'Thu hồi';
            recall.style.padding = '6px';
            recall.style.cursor = 'pointer';
            recall.addEventListener('click', () => { recallMessage(messageId); menu.remove(); });
            menu.appendChild(recall);
        }
        const del = document.createElement('div');
        del.textContent = 'Xóa';
        del.style.padding = '6px';
        del.style.cursor = 'pointer';
        del.addEventListener('click', () => { deleteMessage(messageId); menu.remove(); });
        menu.appendChild(del);
        document.body.appendChild(menu);
        document.addEventListener('click', () => { const e = $('#ctxMenu'); if(e) e.remove(); }, { once: true });
    }

    async function recallMessage(id) {
        try {
            const res = await fetch(`/messages/api/recall/${id}`, { method: 'POST' });
            if (!res.ok) throw new Error('HTTP ' + res.status);
            showToast('Đã thu hồi tin nhắn', 'success');
        } catch (e) { console.error(e); showToast('Thu hồi thất bại', 'danger'); }
    }

    async function deleteMessage(id) {
        try {
            const res = await fetch(`/messages/api/delete/${id}`, { method: 'POST' });
            if (!res.ok) throw new Error('HTTP ' + res.status);
            document.querySelector(`[data-message-id="${id}"]`)?.remove();
            showToast('Đã xóa', 'success');
        } catch (e) { console.error(e); showToast('Xóa thất bại', 'danger'); }
    }

    // upload files first, get filenames
    async function uploadFiles(files) {
        if (!files || files.length === 0) return [];
        const fd = new FormData();
        for (const f of files) fd.append('files', f);
        const res = await fetch('/messages/api/upload', { method: 'POST', body: fd });
        if (!res.ok) throw new Error('Upload failed');
        const json = await res.json();
        return (json.uploaded || []).map(u => u.filename);
    }

    // send message (uploads files then sends WS)
    async function sendMessage() {
        if (!currentPartner) return showToast('Chưa chọn người nhận', 'warning');
        const text = $('#messageInput').value.trim();
        const files = $('#fileInput').files;
        let uploaded = [];
        try {
            if (files && files.length > 0) {
                // show quick UI
                showToast('Đang upload file...', 'info');
                uploaded = await uploadFiles(files);
            }
            const payload = { recipientUsername: currentPartner, content: text, attachmentFilenames: uploaded };
            stompClient.send('/app/chat.send', {}, JSON.stringify(payload));
            // optimistic append
            appendMessageToChat({ id: Date.now(), content: text, senderUsername: CURRENT_USER, createdAt: new Date().toISOString(), attachments: uploaded.map(fn => ({ filename: fn, originalName: fn, url: '/messages/attachments/' + fn })) });
            $('#messageInput').value = '';
            $('#fileInput').value = '';
        } catch (e) {
            console.error(e); showToast('Gửi thất bại', 'danger');
        }
    }

    // UI bindings
    function initUI() {
        // click conversation
        $('#conversationsList').addEventListener('click', e => {
            const it = e.target.closest('.conv-item');
            if (!it) return;
            const username = it.getAttribute('data-username');
            const display = it.getAttribute('data-display');
            const avatar = it.getAttribute('data-avatar');
            openChat(username, display, avatar);
        });

        // new chat
        $('#btnNewChat').addEventListener('click', ()=> $('#newChatDrawer').classList.toggle('d-none'));
        $('#drawerClose').addEventListener('click', ()=> $('#newChatDrawer').classList.add('d-none'));
        $('#startChatBtn').addEventListener('click', ()=> {
            const name = $('#newChatUsername').value.trim();
            if (!name) return showToast('Nhập username', 'warning');
            openChat(name, name, '/images/default-avatar.png');
            $('#newChatDrawer').classList.add('d-none');
        });

        $('#sendMessageBtn').addEventListener('click', sendMessage);
        $('#messageInput').addEventListener('keydown', e => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); sendMessage(); } });

        $('#btnAttach').addEventListener('click', ()=> $('#fileInput').click());
        $('#fileInput').addEventListener('change', ()=> { const f = $('#fileInput').files; if (f && f.length) showToast(`${f.length} file đã chọn`, 'info'); });

        $('#btnOpenRight').addEventListener('click', ()=> $('#colRight').classList.toggle('d-none'));

        $('#leftSearch').addEventListener('input', e => {
            const q = (e.target.value || '').toLowerCase().trim();
            $$('.conv-item').forEach(it => {
                const name = (it.getAttribute('data-display') || '').toLowerCase();
                const prev = (it.querySelector('.conv-preview')?.textContent || '').toLowerCase();
                it.style.display = (!q || name.includes(q) || prev.includes(q)) ? '' : 'none';
            });
        });
    }

    function openChat(username, display, avatar) {
        currentPartner = username;
        $('#chatPlaceholder').classList.add('d-none');
        $('#chatPanel').classList.remove('d-none');
        $('#colRight').classList.remove('d-none');
        $('#partnerName').textContent = display || username;
        $('#partnerAvatar').src = avatar || '/images/default-avatar.png';
        $('#infoAvatar').src = avatar || '/images/default-avatar.png';
        $('#infoName').textContent = display || username;
        $('#infoUsername').textContent = '@' + username;
        // load messages
        loadConversation(username);
        // mark read server side
        fetch(`/messages/api/mark-read/${encodeURIComponent(username)}`, { method: 'POST' }).catch(()=>{});
        // clear local badge
        const el = document.querySelector(`#conversationsList [data-username="${CSS.escape(username)}"] .conv-unread`);
        if (el) el.remove();
    }

    // show toast
    function showToast(msg, type='primary') {
        const t = document.createElement('div');
        t.className = `toast align-items-center text-bg-${type} border-0`;
        t.style.minWidth = '200px';
        t.style.position = 'fixed';
        t.style.right = '16px';
        t.style.bottom = '16px';
        t.style.zIndex = 9999;
        t.innerHTML = `<div class="d-flex"><div class="toast-body">${escapeHtml(msg)}</div><button type="button" class="btn-close btn-close-white me-2 m-auto"></button></div>`;
        document.body.appendChild(t);
        setTimeout(()=> t.remove(), 2500);
    }

    // boot
    document.addEventListener('DOMContentLoaded', () => {
        initUI();
        // initial: load conversation summaries (if backend endpoint exists). We try best-effort minimal UI.
        // Attempt to fetch summaries
        fetch('/messages/api/conversations/summary').then(r => r.ok ? r.json() : []).then(data => {
            if (Array.isArray(data)) {
                data.forEach(s => conversationsCache.set(s.username, s));
                renderConversationList();
            }
        }).catch(()=>{});
        connect();
    });

})();

