# AirTap Desktop

Desktop client for AirTap - Control your Android phone from your computer.

## Features

- 📁 **File Manager** - Browse, upload, download, delete files
- 🔔 **Notifications** - View and dismiss phone notifications
- 💬 **SMS** - Read and send text messages
- 🖥️ **Screen Mirror** - View your phone screen in real-time
- 🎮 **Remote Control** - Tap, swipe, and control your phone

## Requirements

- Java 17 or higher
- AirTap app running on your Android phone
- Both devices on the same network

## Build & Run

### Run from source:
```bash
cd AirTapDesktop
./gradlew run
```

### Build executable:
```bash
# Windows
./gradlew packageMsi

# macOS
./gradlew packageDmg

# Linux
./gradlew packageDeb
```

## Usage

1. Start AirTap server on your Android phone
2. Note the IP address and password shown in the app
3. Launch AirTap Desktop
4. Enter the server URL (e.g., `http://192.168.1.100:8080`)
5. Enter the password
6. Click Connect

## Project Structure

```
AirTapDesktop/
├── src/main/kotlin/com/bhikan/airtap/desktop/
│   ├── Main.kt                 # Entry point
│   ├── api/
│   │   └── AirTapClient.kt     # HTTP client for API calls
│   └── ui/
│       ├── theme/Theme.kt      # Material 3 dark theme
│       └── screens/
│           ├── ConnectScreen.kt
│           ├── MainDashboard.kt
│           ├── FilesTab.kt
│           ├── NotificationsTab.kt
│           ├── SmsTab.kt
│           ├── ScreenTab.kt
│           └── ControlTab.kt
├── build.gradle.kts
└── settings.gradle.kts
```

## Tech Stack

- Kotlin
- Compose Desktop (Jetpack Compose for Desktop)
- Material 3
- Ktor Client (HTTP & WebSocket)
- Kotlinx Serialization
