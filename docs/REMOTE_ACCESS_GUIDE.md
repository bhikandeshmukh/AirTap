# AirTap Remote Access Guide

## Overview

AirTap works on your local WiFi network by default. To access your phone from anywhere on the internet, you have several options.

---

## Option 1: Cloudflare Tunnel (Recommended)

**Pros:** Free, secure, no port forwarding, automatic HTTPS
**Cons:** Requires a PC/server to run cloudflared

### Setup Steps

1. **Install cloudflared on your PC/server:**

   **Windows:**
   ```powershell
   winget install cloudflare.cloudflared
   ```

   **macOS:**
   ```bash
   brew install cloudflared
   ```

   **Linux:**
   ```bash
   curl -L https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64 -o cloudflared
   chmod +x cloudflared
   sudo mv cloudflared /usr/local/bin/
   ```

2. **Start the tunnel:**
   ```bash
   cloudflared tunnel --url http://PHONE_IP:8080
   ```
   
   Replace `PHONE_IP` with your phone's IP address shown in AirTap.

3. **Get your public URL:**
   Cloudflared will output something like:
   ```
   https://random-name.trycloudflare.com
   ```

4. **Access from anywhere:**
   Open that URL in any browser worldwide!

### For Permanent Setup (Optional)

1. Create a Cloudflare account at https://dash.cloudflare.com
2. Add a domain (free domains work too)
3. Create a named tunnel:
   ```bash
   cloudflared tunnel login
   cloudflared tunnel create airtap
   cloudflared tunnel route dns airtap phone.yourdomain.com
   cloudflared tunnel run --url http://PHONE_IP:8080 airtap
   ```

---

## Option 2: ngrok

**Pros:** Very easy setup, free tier available
**Cons:** URL changes on free tier, bandwidth limits

### Setup Steps

1. **Sign up at https://ngrok.com** (free)

2. **Install ngrok:**
   ```bash
   # Windows (PowerShell)
   choco install ngrok
   
   # macOS
   brew install ngrok
   
   # Linux
   snap install ngrok
   ```

3. **Authenticate:**
   ```bash
   ngrok config add-authtoken YOUR_AUTH_TOKEN
   ```

4. **Start tunnel:**
   ```bash
   ngrok http PHONE_IP:8080
   ```

5. **Use the provided URL** (e.g., `https://abc123.ngrok.io`)

---

## Option 3: Port Forwarding (Advanced)

**Pros:** Direct connection, no third-party service
**Cons:** Requires router access, security risks, dynamic IP issues

### Setup Steps

1. **Find your phone's local IP** (shown in AirTap)

2. **Access your router** (usually http://192.168.1.1)

3. **Add port forwarding rule:**
   - External Port: 8080 (or any port)
   - Internal IP: Your phone's IP
   - Internal Port: 8080
   - Protocol: TCP

4. **Find your public IP:**
   Visit https://whatismyip.com

5. **Access via:** `http://YOUR_PUBLIC_IP:8080`

### Dynamic DNS (Recommended with Port Forwarding)

Since home IPs change, use a Dynamic DNS service:

1. Sign up at https://www.noip.com (free)
2. Create a hostname (e.g., `myphone.ddns.net`)
3. Install their update client on your router or PC
4. Access via: `http://myphone.ddns.net:8080`

---

## Option 4: Tailscale VPN

**Pros:** Secure, works through NAT, free for personal use
**Cons:** Requires Tailscale app on both devices

### Setup Steps

1. **Install Tailscale on your phone:**
   Download from Play Store

2. **Install Tailscale on your PC:**
   Download from https://tailscale.com

3. **Sign in on both devices** with same account

4. **Get your phone's Tailscale IP:**
   Usually `100.x.x.x`

5. **Access AirTap via Tailscale IP:**
   `http://100.x.x.x:8080`

---

## Security Recommendations

1. **Always use a strong password** in AirTap settings
2. **Prefer Cloudflare Tunnel or Tailscale** for automatic encryption
3. **Don't expose port 8080 directly** to the internet without HTTPS
4. **Change the default port** in settings for obscurity
5. **Disable remote access** when not needed

---

## Troubleshooting

### Can't connect from browser?
- Ensure phone and PC are on same WiFi (for local access)
- Check if AirTap server is running
- Try disabling firewall temporarily
- Verify the IP address is correct

### Tunnel not working?
- Check if cloudflared/ngrok is running
- Verify the phone IP hasn't changed
- Restart the tunnel

### Slow transfer speeds?
- Use local network when possible
- Cloudflare Tunnel is faster than ngrok free tier
- Check your internet upload speed

---

## Quick Reference

| Method | Difficulty | Cost | Security | Speed |
|--------|-----------|------|----------|-------|
| Cloudflare Tunnel | Easy | Free | ⭐⭐⭐⭐⭐ | Fast |
| ngrok | Very Easy | Free/Paid | ⭐⭐⭐⭐ | Medium |
| Port Forwarding | Hard | Free | ⭐⭐ | Fast |
| Tailscale | Easy | Free | ⭐⭐⭐⭐⭐ | Fast |
