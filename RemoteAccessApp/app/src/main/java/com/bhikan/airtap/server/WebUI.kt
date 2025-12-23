package com.bhikan.airtap.server

fun getIndexHtml(): String = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AirTap</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #1a1a2e; color: #eee; min-height: 100vh; }
        .container { max-width: 1200px; margin: 0 auto; padding: 20px; }
        .header { display: flex; justify-content: space-between; align-items: center; padding: 20px 0; border-bottom: 1px solid #333; }
        .header h1 { color: #4ecca3; }
        .tabs { display: flex; gap: 10px; margin: 20px 0; }
        .tab { padding: 12px 24px; background: #16213e; border: none; color: #888; border-radius: 8px; cursor: pointer; font-size: 14px; }
        .tab.active { background: #4ecca3; color: #1a1a2e; }
        .tab-content { display: none; }
        .tab-content.active { display: block; }
        .card { background: #16213e; border-radius: 12px; padding: 20px; margin-bottom: 15px; }
        .login-form input { width: 100%; padding: 12px; margin: 10px 0; border: none; border-radius: 8px; background: #1a1a2e; color: #fff; }
        .btn { background: #4ecca3; color: #1a1a2e; border: none; padding: 10px 20px; border-radius: 8px; cursor: pointer; font-weight: bold; }
        .btn:hover { background: #3db892; }
        .btn-danger { background: #e74c3c; color: #fff; }
        .btn-sm { padding: 6px 12px; font-size: 12px; }
        .hidden { display: none; }
        .file-list, .notification-list, .sms-list { list-style: none; }
        .file-item, .notification-item, .sms-item { display: flex; align-items: center; padding: 12px; border-bottom: 1px solid #333; cursor: pointer; }
        .file-item:hover, .notification-item:hover, .sms-item:hover { background: #1a1a2e; }
        .icon { width: 40px; font-size: 24px; }
        .app-icon { width: 32px; height: 32px; border-radius: 6px; margin-right: 12px; }
        .content { flex: 1; overflow: hidden; }
        .title { font-weight: bold; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
        .subtitle { color: #888; font-size: 13px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
        .time { color: #666; font-size: 12px; margin-left: 10px; }
        .badge { background: #e74c3c; color: #fff; padding: 2px 8px; border-radius: 10px; font-size: 11px; }
        .toolbar { display: flex; gap: 10px; margin-bottom: 20px; flex-wrap: wrap; }
        .breadcrumb { display: flex; gap: 8px; margin-bottom: 20px; flex-wrap: wrap; }
        .breadcrumb span { color: #4ecca3; cursor: pointer; }
        .upload-zone { border: 2px dashed #4ecca3; border-radius: 12px; padding: 40px; text-align: center; margin: 20px 0; }
        #status { padding: 10px; border-radius: 8px; margin-bottom: 20px; }
        .success { background: #2ecc71; }
        .error { background: #e74c3c; }
        .chat-container { display: flex; flex-direction: column; height: 60vh; }
        .chat-messages { flex: 1; overflow-y: auto; padding: 10px; }
        .message { max-width: 70%; padding: 10px 15px; border-radius: 15px; margin: 5px 0; }
        .message.sent { background: #4ecca3; color: #1a1a2e; margin-left: auto; border-bottom-right-radius: 5px; }
        .message.received { background: #16213e; border-bottom-left-radius: 5px; }
        .chat-input { display: flex; gap: 10px; padding: 10px; background: #16213e; border-radius: 8px; }
        .chat-input input { flex: 1; padding: 12px; border: none; border-radius: 8px; background: #1a1a2e; color: #fff; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>📱 AirTap</h1>
            <button class="btn btn-danger hidden" id="logoutBtn" onclick="logout()">Logout</button>
        </div>
        <div id="status" class="hidden"></div>
        <div id="loginSection" class="card">
            <h2>🔐 Login</h2>
            <input type="password" id="password" placeholder="Enter password" onkeypress="if(event.key==='Enter')login()">
            <button class="btn" onclick="login()">Connect</button>
        </div>
        <div id="mainSection" class="hidden">
            <div class="tabs">
                <button class="tab active" onclick="showTab('files')">📁 Files</button>
                <button class="tab" onclick="showTab('notifications')">🔔 Notifications</button>
                <button class="tab" onclick="showTab('sms')">💬 SMS</button>
                <button class="tab" onclick="showTab('screen')">🖥️ Screen</button>
                <button class="tab" onclick="showTab('control')">🎮 Control</button>
            </div>
""".trimIndent()


fun getIndexHtmlPart2(): String = """
            <!-- Files Tab -->
            <div id="filesTab" class="tab-content active">
                <div class="toolbar">
                    <button class="btn" onclick="goUp()">⬆️ Up</button>
                    <button class="btn" onclick="refresh()">🔄 Refresh</button>
                    <button class="btn" onclick="showUpload()">📤 Upload</button>
                    <button class="btn" onclick="createFolder()">📁 New Folder</button>
                </div>
                <div class="breadcrumb" id="breadcrumb"></div>
                <div class="upload-zone hidden" id="uploadZone">
                    <input type="file" id="fileInput" multiple onchange="uploadFiles()">
                    <p>Click or drag files here</p>
                </div>
                <ul class="file-list" id="fileList"></ul>
            </div>
            <!-- Notifications Tab -->
            <div id="notificationsTab" class="tab-content">
                <div class="toolbar">
                    <button class="btn" onclick="loadNotifications()">🔄 Refresh</button>
                    <button class="btn btn-danger" onclick="dismissAllNotifications()">🗑️ Clear All</button>
                </div>
                <ul class="notification-list" id="notificationList"></ul>
            </div>
            <!-- SMS Tab -->
            <div id="smsTab" class="tab-content">
                <div id="smsConversations">
                    <div class="toolbar">
                        <button class="btn" onclick="loadConversations()">🔄 Refresh</button>
                        <button class="btn" onclick="showNewSms()">✉️ New Message</button>
                    </div>
                    <ul class="sms-list" id="smsList"></ul>
                </div>
                <div id="smsChat" class="hidden">
                    <div class="toolbar">
                        <button class="btn" onclick="backToConversations()">← Back</button>
                        <span id="chatTitle" style="flex:1;text-align:center;font-weight:bold;"></span>
                    </div>
                    <div class="chat-container">
                        <div class="chat-messages" id="chatMessages"></div>
                        <div class="chat-input">
                            <input type="text" id="smsInput" placeholder="Type a message..." onkeypress="if(event.key==='Enter')sendSms()">
                            <button class="btn" onclick="sendSms()">Send</button>
                        </div>
                    </div>
                </div>
            </div>
            <!-- Screen Mirror Tab -->
            <div id="screenTab" class="tab-content">
                <div class="card" style="text-align:center;">
                    <h3>🖥️ Screen Mirror</h3>
                    <p id="screenStatus" style="margin:15px 0;color:#888;">Checking status...</p>
                    <div id="screenContainer" style="margin:20px auto;max-width:400px;">
                        <img id="screenImage" style="width:100%;border-radius:12px;display:none;" />
                    </div>
                    <button class="btn" onclick="toggleScreenMirror()" id="screenBtn">Start Viewing</button>
                    <p style="margin-top:15px;font-size:12px;color:#666;">
                        Start screen mirroring from the AirTap app on your phone first.
                    </p>
                </div>
            </div>
            <!-- Remote Control Tab -->
            <div id="controlTab" class="tab-content">
                <div class="card">
                    <h3>🎮 Remote Control</h3>
                    <p id="controlStatus" style="margin:15px 0;color:#888;">Checking status...</p>
                    <div style="margin:20px 0;">
                        <p style="font-size:13px;color:#888;margin-bottom:15px;">Navigation Buttons</p>
                        <div class="toolbar" style="justify-content:center;">
                            <button class="btn" onclick="controlAction('back')">◀️ Back</button>
                            <button class="btn" onclick="controlAction('home')">🏠 Home</button>
                            <button class="btn" onclick="controlAction('recents')">📋 Recents</button>
                        </div>
                    </div>
                    <div style="margin:20px 0;">
                        <p style="font-size:13px;color:#888;margin-bottom:15px;">Quick Actions</p>
                        <div class="toolbar" style="justify-content:center;flex-wrap:wrap;">
                            <button class="btn" onclick="controlAction('notifications')">🔔 Notifications</button>
                            <button class="btn" onclick="controlAction('quicksettings')">⚙️ Quick Settings</button>
                            <button class="btn" onclick="controlAction('screenshot')">📸 Screenshot</button>
                            <button class="btn btn-danger" onclick="controlAction('lock')">🔒 Lock</button>
                        </div>
                    </div>
                    <div style="margin:20px 0;padding:20px;background:#1a1a2e;border-radius:12px;">
                        <p style="font-size:13px;color:#888;margin-bottom:15px;">Touch Control (use with Screen tab)</p>
                        <div id="touchPad" style="width:100%;height:200px;background:#16213e;border-radius:8px;border:2px solid #333;position:relative;cursor:crosshair;">
                            <p style="position:absolute;top:50%;left:50%;transform:translate(-50%,-50%);color:#555;font-size:12px;">Click to tap • Drag to swipe</p>
                        </div>
                        <p style="font-size:11px;color:#666;margin-top:10px;">Coordinates are mapped to phone screen</p>
                    </div>
                    <p style="font-size:12px;color:#666;margin-top:15px;">
                        ⚠️ Enable Accessibility Service in phone Settings → Accessibility → AirTap
                    </p>
                </div>
            </div>
        </div>
    </div>
""".trimIndent()


fun getIndexHtmlScript(): String = """
    <script>
        let token = localStorage.getItem('token');
        let currentPath = '';
        let currentThreadId = null;
        let currentAddress = '';
        let ws = null;
        
        if (token) checkAuth();
        
        async function checkAuth() {
            try {
                const res = await fetch('/api/files?path=', { headers: { 'Authorization': 'Bearer ' + token }});
                if (res.ok) showMain();
                else showLogin();
            } catch { showLogin(); }
        }
        
        function showLogin() {
            document.getElementById('loginSection').classList.remove('hidden');
            document.getElementById('mainSection').classList.add('hidden');
            document.getElementById('logoutBtn').classList.add('hidden');
        }
        
        function showMain() {
            document.getElementById('loginSection').classList.add('hidden');
            document.getElementById('mainSection').classList.remove('hidden');
            document.getElementById('logoutBtn').classList.remove('hidden');
            loadFiles('');
            connectWebSocket();
        }
        
        async function login() {
            const password = document.getElementById('password').value;
            try {
                const res = await fetch('/api/login', { method: 'POST', body: new URLSearchParams({ password }) });
                const data = await res.json();
                if (data.token) {
                    token = data.token;
                    localStorage.setItem('token', token);
                    showMain();
                    showStatus('Connected!', 'success');
                } else showStatus('Invalid password', 'error');
            } catch { showStatus('Connection failed', 'error'); }
        }
        
        async function logout() {
            await fetch('/api/logout', { method: 'POST', headers: { 'Authorization': 'Bearer ' + token }});
            localStorage.removeItem('token');
            token = null;
            if (ws) ws.close();
            showLogin();
        }
        
        function connectWebSocket() {
            const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
            ws = new WebSocket(protocol + '//' + location.host + '/ws');
            ws.onmessage = (e) => {
                const msg = JSON.parse(e.data);
                if (msg.type === 'notification') {
                    showStatus('New notification: ' + JSON.parse(msg.data).title, 'success');
                    loadNotifications();
                } else if (msg.type === 'sms') {
                    showStatus('New SMS received', 'success');
                    if (currentThreadId) loadThread(currentThreadId, currentAddress);
                    else loadConversations();
                }
            };
            ws.onclose = () => setTimeout(connectWebSocket, 3000);
        }
        
        function showTab(tab) {
            document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
            document.querySelectorAll('.tab-content').forEach(t => t.classList.remove('active'));
            document.querySelector('.tab[onclick*="' + tab + '"]').classList.add('active');
            document.getElementById(tab + 'Tab').classList.add('active');
            if (tab === 'notifications') loadNotifications();
            else if (tab === 'sms') loadConversations();
            else if (tab === 'screen') checkScreenStatus();
            else if (tab === 'control') checkControlStatus();
        }
        
        // Files
        async function loadFiles(path) {
            currentPath = path;
            const res = await fetch('/api/files?path=' + encodeURIComponent(path), { headers: { 'Authorization': 'Bearer ' + token }});
            const data = await res.json();
            renderFiles(data);
        }
        
        function renderFiles(data) {
            const list = document.getElementById('fileList');
            const bc = document.getElementById('breadcrumb');
            const parts = data.currentPath.split('/').filter(p => p);
            bc.innerHTML = '<span onclick="loadFiles(\\'\\')">🏠</span>';
            let pathSoFar = '';
            parts.forEach(p => { pathSoFar += '/' + p; bc.innerHTML += ' / <span onclick="loadFiles(\\'' + pathSoFar + '\\')">' + p + '</span>'; });
            list.innerHTML = data.files.map(f => '<li class="file-item" ondblclick="' + (f.isDirectory ? "loadFiles('" + f.path + "')" : "downloadFile('" + f.path + "')") + '"><span class="icon">' + (f.isDirectory ? '📁' : getIcon(f.extension)) + '</span><div class="content"><div class="title">' + f.name + '</div><div class="subtitle">' + (f.isDirectory ? '' : formatSize(f.size)) + '</div></div><button class="btn btn-sm btn-danger" onclick="event.stopPropagation();deleteItem(\\'' + f.path + '\\')">🗑️</button></li>').join('');
        }
        
        function getIcon(ext) { return {jpg:'🖼️',jpeg:'🖼️',png:'🖼️',gif:'🖼️',mp4:'🎬',mp3:'🎵',pdf:'📄',zip:'📦',apk:'📱'}[ext] || '📄'; }
        function formatSize(b) { if(!b)return'0 B';const k=1024,s=['B','KB','MB','GB'],i=Math.floor(Math.log(b)/Math.log(k));return(b/Math.pow(k,i)).toFixed(1)+' '+s[i]; }
        function goUp() { const p=currentPath.split('/').filter(x=>x);p.pop();loadFiles(p.length?'/'+p.join('/'):''); }
        function refresh() { loadFiles(currentPath); }
        function showUpload() { document.getElementById('uploadZone').classList.toggle('hidden'); }
        async function uploadFiles() { const files=document.getElementById('fileInput').files;for(const f of files){const fd=new FormData();fd.append('file',f);await fetch('/api/files/upload?path='+encodeURIComponent(currentPath),{method:'POST',headers:{'Authorization':'Bearer '+token},body:fd});}showStatus('Uploaded!','success');refresh();document.getElementById('uploadZone').classList.add('hidden'); }
        function downloadFile(p) { window.open('/api/files/download?path='+encodeURIComponent(p)+'&token='+token); }
        async function deleteItem(p) { if(!confirm('Delete?'))return;await fetch('/api/files?path='+encodeURIComponent(p),{method:'DELETE',headers:{'Authorization':'Bearer '+token}});refresh(); }
        async function createFolder() { const n=prompt('Folder name:');if(!n)return;await fetch('/api/files/mkdir',{method:'POST',headers:{'Authorization':'Bearer '+token,'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams({path:currentPath,name:n})});refresh(); }
""".trimIndent()


fun getIndexHtmlScript2(): String = """
        // Notifications
        async function loadNotifications() {
            const res = await fetch('/api/notifications', { headers: { 'Authorization': 'Bearer ' + token }});
            const data = await res.json();
            const list = document.getElementById('notificationList');
            if (data.notifications.length === 0) {
                list.innerHTML = '<li class="notification-item"><div class="content"><div class="title">No notifications</div></div></li>';
                return;
            }
            list.innerHTML = data.notifications.map(n => '<li class="notification-item"><img class="app-icon" src="' + (n.iconBase64 || 'data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22/>') + '" onerror="this.style.display=\\'none\\'"><div class="content"><div class="title">' + n.appName + '</div><div class="subtitle"><b>' + n.title + '</b> - ' + n.text + '</div></div><span class="time">' + formatTime(n.timestamp) + '</span>' + (n.isClearable ? '<button class="btn btn-sm" onclick="dismissNotification(\\'' + n.id + '\\')">✕</button>' : '') + '</li>').join('');
        }
        
        async function dismissNotification(id) {
            await fetch('/api/notifications/dismiss', { method: 'POST', headers: { 'Authorization': 'Bearer ' + token, 'Content-Type': 'application/x-www-form-urlencoded' }, body: new URLSearchParams({ id }) });
            loadNotifications();
        }
        
        async function dismissAllNotifications() {
            await fetch('/api/notifications/dismiss-all', { method: 'POST', headers: { 'Authorization': 'Bearer ' + token }});
            loadNotifications();
        }
        
        // SMS
        async function loadConversations() {
            document.getElementById('smsConversations').classList.remove('hidden');
            document.getElementById('smsChat').classList.add('hidden');
            currentThreadId = null;
            const res = await fetch('/api/sms/conversations', { headers: { 'Authorization': 'Bearer ' + token }});
            const data = await res.json();
            const list = document.getElementById('smsList');
            if (data.conversations.length === 0) {
                list.innerHTML = '<li class="sms-item"><div class="content"><div class="title">No messages</div></div></li>';
                return;
            }
            list.innerHTML = data.conversations.map(c => '<li class="sms-item" onclick="loadThread(' + c.threadId + ',\\'' + c.address + '\\')"><span class="icon">💬</span><div class="content"><div class="title">' + (c.contactName || c.address) + (c.unreadCount > 0 ? ' <span class="badge">' + c.unreadCount + '</span>' : '') + '</div><div class="subtitle">' + c.lastMessage.substring(0, 50) + '</div></div><span class="time">' + formatTime(c.lastTimestamp) + '</span></li>').join('');
        }
        
        async function loadThread(threadId, address) {
            currentThreadId = threadId;
            currentAddress = address;
            document.getElementById('smsConversations').classList.add('hidden');
            document.getElementById('smsChat').classList.remove('hidden');
            const res = await fetch('/api/sms/thread/' + threadId, { headers: { 'Authorization': 'Bearer ' + token }});
            const data = await res.json();
            document.getElementById('chatTitle').textContent = data.contactName || data.address;
            const container = document.getElementById('chatMessages');
            container.innerHTML = data.messages.map(m => '<div class="message ' + (m.type === 2 ? 'sent' : 'received') + '">' + m.body + '<div style="font-size:10px;color:#666;margin-top:4px;">' + formatTime(m.timestamp) + '</div></div>').join('');
            container.scrollTop = container.scrollHeight;
            fetch('/api/sms/mark-read/' + threadId, { method: 'POST', headers: { 'Authorization': 'Bearer ' + token }});
        }
        
        function backToConversations() { loadConversations(); }
        
        async function sendSms() {
            const input = document.getElementById('smsInput');
            const message = input.value.trim();
            if (!message) return;
            await fetch('/api/sms/send', { method: 'POST', headers: { 'Authorization': 'Bearer ' + token, 'Content-Type': 'application/x-www-form-urlencoded' }, body: new URLSearchParams({ address: currentAddress, message }) });
            input.value = '';
            setTimeout(() => loadThread(currentThreadId, currentAddress), 500);
        }
        
        function showNewSms() {
            const address = prompt('Phone number:');
            if (!address) return;
            currentAddress = address;
            currentThreadId = 0;
            document.getElementById('smsConversations').classList.add('hidden');
            document.getElementById('smsChat').classList.remove('hidden');
            document.getElementById('chatTitle').textContent = address;
            document.getElementById('chatMessages').innerHTML = '';
        }
        
        function formatTime(ts) {
            const d = new Date(ts);
            const now = new Date();
            if (d.toDateString() === now.toDateString()) return d.toLocaleTimeString([], {hour:'2-digit',minute:'2-digit'});
            return d.toLocaleDateString();
        }
        
        function showStatus(msg, type) {
            const el = document.getElementById('status');
            el.textContent = msg;
            el.className = type;
            el.classList.remove('hidden');
            setTimeout(() => el.classList.add('hidden'), 3000);
        }
        
        // Screen Mirror
        let screenInterval = null;
        
        async function checkScreenStatus() {
            try {
                const res = await fetch('/api/screen/status', { headers: { 'Authorization': 'Bearer ' + token }});
                const data = await res.json();
                document.getElementById('screenStatus').textContent = data.message;
                if (data.streaming) {
                    document.getElementById('screenBtn').textContent = 'Start Viewing';
                } else {
                    document.getElementById('screenBtn').textContent = 'Check Again';
                    document.getElementById('screenImage').style.display = 'none';
                }
            } catch (e) {
                document.getElementById('screenStatus').textContent = 'Error checking status';
            }
        }
        
        function toggleScreenMirror() {
            const img = document.getElementById('screenImage');
            if (screenInterval) {
                clearInterval(screenInterval);
                screenInterval = null;
                img.style.display = 'none';
                document.getElementById('screenBtn').textContent = 'Start Viewing';
            } else {
                img.style.display = 'block';
                document.getElementById('screenBtn').textContent = 'Stop Viewing';
                refreshScreen();
                screenInterval = setInterval(refreshScreen, 150);
            }
        }
        
        async function refreshScreen() {
            const img = document.getElementById('screenImage');
            try {
                const res = await fetch('/api/screen/frame?t=' + Date.now(), { headers: { 'Authorization': 'Bearer ' + token }});
                if (res.ok) {
                    const blob = await res.blob();
                    img.src = URL.createObjectURL(blob);
                    document.getElementById('screenStatus').textContent = 'Screen mirroring active';
                } else {
                    document.getElementById('screenStatus').textContent = 'Screen capture not active on phone';
                }
            } catch (e) {
                document.getElementById('screenStatus').textContent = 'Connection error';
            }
        }
        
        // Remote Control
        let phoneWidth = 1080, phoneHeight = 1920;
        
        async function checkControlStatus() {
            try {
                const res = await fetch('/api/control/status', { headers: { 'Authorization': 'Bearer ' + token }});
                const data = await res.json();
                document.getElementById('controlStatus').textContent = data.message;
                document.getElementById('controlStatus').style.color = data.enabled ? '#4ecca3' : '#e74c3c';
                if (data.enabled) initTouchPad();
            } catch (e) {
                document.getElementById('controlStatus').textContent = 'Error checking status';
            }
        }
        
        async function controlAction(action) {
            try {
                const res = await fetch('/api/control/' + action, { method: 'POST', headers: { 'Authorization': 'Bearer ' + token }});
                const data = await res.json();
                if (data.success) showStatus(action + ' executed', 'success');
                else showStatus('Action failed - enable Accessibility', 'error');
            } catch (e) {
                showStatus('Connection error', 'error');
            }
        }
        
        function initTouchPad() {
            const pad = document.getElementById('touchPad');
            let startX, startY, isDragging = false;
            
            pad.onmousedown = (e) => {
                const rect = pad.getBoundingClientRect();
                startX = e.clientX - rect.left;
                startY = e.clientY - rect.top;
                isDragging = true;
            };
            
            pad.onmouseup = async (e) => {
                if (!isDragging) return;
                isDragging = false;
                const rect = pad.getBoundingClientRect();
                const endX = e.clientX - rect.left;
                const endY = e.clientY - rect.top;
                
                const scaleX = phoneWidth / rect.width;
                const scaleY = phoneHeight / rect.height;
                
                const pStartX = Math.round(startX * scaleX);
                const pStartY = Math.round(startY * scaleY);
                const pEndX = Math.round(endX * scaleX);
                const pEndY = Math.round(endY * scaleY);
                
                const dist = Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2));
                
                if (dist < 10) {
                    await fetch('/api/control/tap', { method: 'POST', headers: { 'Authorization': 'Bearer ' + token, 'Content-Type': 'application/x-www-form-urlencoded' }, body: new URLSearchParams({ x: pStartX, y: pStartY }) });
                    showStatus('Tap at ' + pStartX + ',' + pStartY, 'success');
                } else {
                    await fetch('/api/control/swipe', { method: 'POST', headers: { 'Authorization': 'Bearer ' + token, 'Content-Type': 'application/x-www-form-urlencoded' }, body: new URLSearchParams({ startX: pStartX, startY: pStartY, endX: pEndX, endY: pEndY }) });
                    showStatus('Swipe performed', 'success');
                }
            };
            
            pad.onmouseleave = () => { isDragging = false; };
        }
    </script>
</body>
</html>
""".trimIndent()

fun getFullIndexHtml(): String = getIndexHtml() + getIndexHtmlPart2() + getIndexHtmlScript() + getIndexHtmlScript2()
