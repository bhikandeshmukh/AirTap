# Development Roadmap

## Overview

This roadmap outlines the phased approach to building your AirDroid-like app,
either by extending PlainApp or building from scratch.

---

## Phase 1: Basic File Transfer (HTTP Server)
**Timeline: 1-2 weeks**

### Goals
- Embedded HTTP/HTTPS server on Android
- File browsing via web interface
- Upload/download files
- Basic authentication

### Key Components
- Ktor embedded server
- File system access (SAF/MediaStore)
- JSON API endpoints
- Simple web UI

### Permissions Required
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" /> <!-- Android 11+ -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
```

### Libraries
```kotlin
// Ktor server
implementation("io.ktor:ktor-server-core:2.3.7")
implementation("io.ktor:ktor-server-netty:2.3.7")
implementation("io.ktor:ktor-server-content-negotiation:2.3.7")
implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
implementation("io.ktor:ktor-server-cors:2.3.7")
implementation("io.ktor:ktor-server-auth:2.3.7")
```

---

## Phase 2: Remote Access Over Internet
**Timeline: 1-2 weeks**

### Options

#### Option A: Dynamic DNS + Port Forwarding
- Use services like No-IP, DuckDNS
- Configure router port forwarding
- Pros: Free, direct connection
- Cons: Requires router access, security risks

#### Option B: Reverse Proxy (Recommended)
- Use ngrok, Cloudflare Tunnel, or self-hosted
- No router configuration needed
- Built-in HTTPS
- Pros: Easy setup, secure
- Cons: May have bandwidth limits

#### Option C: P2P with STUN/TURN
- WebRTC-based connection
- Works through NAT
- Pros: No server needed for data
- Cons: Complex setup

### Implementation Priority
1. Local network first (Phase 1)
2. Add Cloudflare Tunnel integration
3. Optional: Custom relay server

---

## Phase 3: Notification & SMS Mirroring
**Timeline: 2-3 weeks**

### Notifications
- NotificationListenerService
- WebSocket for real-time updates
- Action buttons support

### SMS
- ContentProvider for reading
- SmsManager for sending
- BroadcastReceiver for incoming

### Permissions
```xml
<uses-permission android:name="android.permission.READ_SMS" />
<uses-permission android:name="android.permission.SEND_SMS" />
<uses-permission android:name="android.permission.RECEIVE_SMS" />
```

### Special Setup
- User must enable Notification Access in Settings
- Default SMS app for full SMS control (optional)

---

## Phase 4: Screen Mirroring
**Timeline: 3-4 weeks**

### Technology Options

#### Option A: MJPEG Stream (Simpler)
- MediaProjection API for screen capture
- Encode frames as JPEG
- Stream via HTTP multipart
- Pros: Simple, works everywhere
- Cons: Higher bandwidth, no audio

#### Option B: WebRTC (Recommended)
- MediaProjection + VirtualDisplay
- H.264/VP8 encoding
- WebRTC for streaming
- Pros: Low latency, audio support
- Cons: More complex

### Permissions
```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />
```

### Key APIs
- `MediaProjectionManager`
- `VirtualDisplay`
- `ImageReader` or `MediaCodec`

---

## Phase 5: Remote Control
**Timeline: 3-4 weeks**

### Non-Root Options

#### Option A: Accessibility Service
- Can perform taps, swipes, text input
- Requires user to enable in Settings
- Limited but functional

#### Option B: ADB over WiFi (Requires initial setup)
- Full control like scrcpy
- User must enable ADB debugging
- Can be automated after first connection

#### Option C: Device Owner (Enterprise)
- Full control
- Requires factory reset to set up
- Best for dedicated devices

### Recommended Approach
1. Start with Accessibility Service
2. Add ADB option for power users
3. Document limitations clearly

---

## Phase 6: Security Enhancements
**Timeline: 2 weeks**

### Must-Have
- [ ] TLS/HTTPS with valid or self-signed cert
- [ ] Password authentication
- [ ] Session tokens with expiry
- [ ] Rate limiting
- [ ] Input validation

### Nice-to-Have
- [ ] Two-factor authentication
- [ ] End-to-end encryption
- [ ] Biometric unlock
- [ ] Connection whitelist
- [ ] Audit logging

---

## Phase 7: Premium Features & Monetization
**Timeline: 2-3 weeks**

### Free Tier
- Local network access only
- Basic file transfer
- Limited screen mirror time

### Premium Tier
- Remote internet access
- Unlimited screen mirroring
- Priority support
- No ads

### Implementation
- Google Play Billing Library
- License verification
- Feature flags

---

## Phase 8: Desktop Client
**Timeline: 4-6 weeks**

### Options

#### Option A: Kotlin Multiplatform (Recommended)
- Share business logic with Android
- Compose Multiplatform for UI
- Single codebase

#### Option B: Electron
- Web technologies
- Reuse web client code
- Larger app size

#### Option C: Native (Java/Kotlin)
- Best performance
- More development time
- Platform-specific code

---

## Quick Reference: All Permissions

```xml
<!-- Network -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />

<!-- Storage -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />

<!-- SMS & Calls -->
<uses-permission android:name="android.permission.READ_SMS" />
<uses-permission android:name="android.permission.SEND_SMS" />
<uses-permission android:name="android.permission.RECEIVE_SMS" />
<uses-permission android:name="android.permission.READ_CALL_LOG" />
<uses-permission android:name="android.permission.READ_CONTACTS" />

<!-- Media -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />

<!-- Services -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CAMERA" />

<!-- Special -->
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```
