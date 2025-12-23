# PlainApp Fork - Setup Guide

## Prerequisites

1. **Android Studio** (Hedgehog 2023.1.1 or newer)
2. **JDK 17** (bundled with Android Studio)
3. **Git** installed
4. **Android SDK** (API 33+)
5. **Physical Android device** (emulator won't work for all features)

## Step 1: Fork the Repository

### Option A: GitHub Fork (Recommended for contributions)
1. Go to https://github.com/ismartcoding/plain-app
2. Click "Fork" button (top right)
3. Clone your fork:
```bash
git clone https://github.com/YOUR_USERNAME/plain-app.git
cd plain-app
```

### Option B: Direct Clone (For personal use)
```bash
git clone https://github.com/ismartcoding/plain-app.git
cd plain-app
# Remove origin and add your own
git remote remove origin
git remote add origin https://github.com/YOUR_USERNAME/your-app-name.git
```

## Step 2: Android Studio Setup

1. Open Android Studio
2. File → Open → Select the cloned `plain-app` folder
3. Wait for Gradle sync (may take 5-10 minutes first time)
4. If prompted, update Gradle plugin (recommended)

### Troubleshooting Gradle Issues

If you encounter build errors:

```groovy
// In gradle.properties, ensure:
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
```

## Step 3: Project Structure Overview

```
plain-app/
├── app/                          # Main Android app module
│   ├── src/main/
│   │   ├── java/com/ismartcoding/plain/
│   │   │   ├── api/              # GraphQL API definitions
│   │   │   ├── data/             # Data models & repositories
│   │   │   ├── db/               # Room database
│   │   │   ├── features/         # Feature modules
│   │   │   ├── helpers/          # Utility classes
│   │   │   ├── services/         # Android services
│   │   │   ├── ui/               # Jetpack Compose UI
│   │   │   ├── web/              # Ktor web server
│   │   │   └── workers/          # WorkManager workers
│   │   ├── res/                  # Resources
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── lib/                          # Shared library module
├── gradle/                       # Gradle wrapper
└── build.gradle.kts              # Root build file
```

## Step 4: Build & Run

### Debug Build
1. Connect Android device (USB debugging enabled)
2. Select device in Android Studio toolbar
3. Click Run (green play button) or `Shift+F10`

### Release Build (Signed APK)
```bash
# Generate keystore (first time only)
keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-alias

# Build release APK
./gradlew assembleRelease
```

## Step 5: Initial Configuration

### Required Permissions (already in manifest)
- `INTERNET` - Web server
- `READ_EXTERNAL_STORAGE` / `MANAGE_EXTERNAL_FILES` - File access
- `READ_SMS`, `SEND_SMS` - SMS features
- `READ_CONTACTS` - Contacts access
- `READ_CALL_LOG` - Call history
- `CAMERA` - Remote camera
- `RECORD_AUDIO` - Audio streaming
- `FOREGROUND_SERVICE` - Keep server running
- `BIND_NOTIFICATION_LISTENER_SERVICE` - Notification mirroring

### First Run Checklist
1. Grant all requested permissions
2. Enable Notification Access in Settings
3. Note the local IP address shown
4. Open browser and navigate to `https://DEVICE_IP:8443`
5. Accept self-signed certificate warning
6. Login with generated password (shown in app)

## Step 6: Rename/Rebrand the App

To make it your own:

### 1. Change Package Name
In Android Studio: Right-click package → Refactor → Rename

Or manually update:
- `app/build.gradle.kts`: `applicationId`
- All package declarations in Kotlin files
- `AndroidManifest.xml`

### 2. Change App Name
Edit `app/src/main/res/values/strings.xml`:
```xml
<string name="app_name">YourAppName</string>
```

### 3. Replace Icons
- Replace files in `app/src/main/res/mipmap-*/`
- Use Android Studio: Right-click res → New → Image Asset

### 4. Update Theme Colors
Edit `app/src/main/java/.../ui/theme/Color.kt`

## Next Steps

Once you have the app running, proceed to the development roadmap
to start customizing and adding features.
