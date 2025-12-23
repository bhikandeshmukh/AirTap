# AirTap

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android%20%7C%20Windows%20%7C%20macOS%20%7C%20Linux-blue" alt="Platform">
  <img src="https://img.shields.io/badge/Language-Kotlin-orange" alt="Language">
  <img src="https://img.shields.io/badge/License-MIT-green" alt="License">
</p>

**AirTap** is a powerful remote access tool that lets you control your Android phone from your desktop. Access files, read notifications, send SMS, mirror screen, and remotely control your device - all from your computer.

## ✨ Features

| Feature | Description |
|---------|-------------|
| 📁 **File Manager** | Browse, upload, download, and delete files |
| 🔔 **Notifications** | View and dismiss phone notifications in real-time |
| 💬 **SMS** | Read conversations and send text messages |
| 📺 **Screen Mirror** | Live screen streaming via MJPEG |
| 🎮 **Remote Control** | Tap, swipe, and control your phone remotely |
| 🔐 **Email Auth** | Simple email-based pairing between devices |
| 👑 **Superadmin** | Access any registered device (for admins) |
| ☁️ **Cloud Sync** | Device registration via Firebase Firestore |

## 📱 Screenshots

*Coming soon*

## 🚀 Quick Start

### Android App

1. Install `AirTap.apk` on your Android phone
2. Open the app and enter your email
3. Grant required permissions (Storage, SMS, Notifications, Accessibility)
4. Start the server

### Desktop App

1. Install `AirTap-Desktop` on your computer
2. Enter your phone's IP address (shown in the app)
3. Enter the same email you used on your phone
4. Click Connect!

## 📥 Download

Download the latest release from [GitHub Releases](https://github.com/bhikandeshmukh/AirTap/releases)

| Platform | Download |
|----------|----------|
| Android | `AirTap-Release.apk` |
| Windows | `AirTap-Desktop-Setup.exe` |
| macOS | `AirTap-Desktop-macOS.dmg` |
| Linux | `AirTap-Desktop-Linux.deb` |

## 🛠️ Build from Source

### Prerequisites

- JDK 17+
- Android Studio (for Android app)
- Gradle 8.2+

### Android App

```bash
cd RemoteAccessApp
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

### Desktop App

```bash
cd AirTapDesktop

# Windows
./gradlew packageExe

# macOS
./gradlew packageDmg

# Linux
./gradlew packageDeb
```

## 🔧 Configuration

### Firebase Setup (Optional)

1. Create a Firebase project at [Firebase Console](https://console.firebase.google.com/)
2. Add Android app with package name `com.bhikan.airtap`
3. Download `google-services.json` and place in `RemoteAccessApp/app/`
4. Enable Firestore Database

### Superadmin

The superadmin email can access any registered device. Configure in:
- `AuthManager.kt` - `SUPERADMIN_EMAIL`
- `FirestoreClient.kt` - `SUPERADMIN_EMAIL`

## 📂 Project Structure

```
AirTap/
├── RemoteAccessApp/          # Android App
│   ├── app/
│   │   ├── src/main/java/com/bhikan/airtap/
│   │   │   ├── data/         # Models & Repositories
│   │   │   ├── server/       # Ktor Web Server
│   │   │   ├── service/      # Android Services
│   │   │   └── ui/           # Jetpack Compose UI
│   │   └── google-services.json
│   └── build.gradle.kts
│
├── AirTapDesktop/            # Desktop App
│   ├── src/main/kotlin/com/bhikan/airtap/desktop/
│   │   ├── api/              # HTTP & Firestore Clients
│   │   └── ui/               # Compose Desktop UI
│   └── build.gradle.kts
│
└── .github/workflows/        # CI/CD
    └── build.yml
```

## 🔒 Permissions Required

| Permission | Purpose |
|------------|---------|
| Storage | File browsing and management |
| SMS | Read and send text messages |
| Contacts | Display contact names in SMS |
| Notifications | Mirror notifications to desktop |
| Accessibility | Remote control (tap, swipe) |
| Media Projection | Screen mirroring |

## 🌐 Remote Access

To access your phone from outside your local network:

1. **Cloudflare Tunnel** (Recommended)
   ```bash
   cloudflared tunnel --url http://PHONE_IP:8080
   ```

2. **ngrok**
   ```bash
   ngrok http PHONE_IP:8080
   ```

3. **Port Forwarding** - Configure your router to forward port 8080

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Author

**Bhikan Deshmukh**
- GitHub: [@bhikandeshmukh](https://github.com/bhikandeshmukh)
- Email: thebhikandeshmukh@gmail.com

---

<p align="center">Made with ❤️ by Bhikan Deshmukh</p>
