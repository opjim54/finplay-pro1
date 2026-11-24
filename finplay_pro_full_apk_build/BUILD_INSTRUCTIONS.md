# FinPlay Pro - Comprehensive APK Build & Signing Guide

This guide provides technical instructions for configuring your development environment, compiling debug APKs, and generating signed release APKs for distribution.

---

## 1. System Requirements

- **Operating System**: Windows 10/11, macOS (Intel/Apple Silicon), or Linux (Ubuntu 20.04+).
- **JDK Version**: Java 17 or Java 21 (Temurin, Corretto, or Android Studio bundled JBR).
- **Android SDK**: API Level 36 (Android 15+ compatible, minimum Android 7.0 / API 24).
- **Build System**: Gradle 9.3.1 (defined in `gradle/wrapper/gradle-wrapper.properties`).

---

## 2. Environment Setup

### Setting `JAVA_HOME`
- **Windows**:
  - If Android Studio is installed at standard location:
    ```cmd
    set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
    set PATH=%JAVA_HOME%\bin;%PATH%
    ```
- **Linux/macOS**:
  ```bash
  export JAVA_HOME=/path/to/jdk-17
  export PATH=$JAVA_HOME/bin:$PATH
  ```

### Setting Android SDK (`local.properties`)
Create or verify `local.properties` in the root of `finplay_pro_full_apk_build`:
- **Windows**:
  ```properties
  sdk.dir=C\:\\Users\\<YourUsername>\\AppData\\Local\\Android\\Sdk
  ```
- **macOS**:
  ```properties
  sdk.dir=/Users/<YourUsername>/Library/Android/sdk
  ```
- **Linux**:
  ```properties
  sdk.dir=/home/<YourUsername>/Android/Sdk
  ```

---

## 3. Building Debug APK

Debug APKs are unsigned or signed with the default debug key and can be installed directly onto any Android device via USB debugging or direct download:

```cmd
# Windows
gradlew.bat assembleDebug

# Linux / macOS
./gradlew assembleDebug
```

**Artifact Location**:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 4. Building Signed Release APK

For production distribution (e.g. Google Play Store or direct APK distribution), generate a signed APK with your release keystore:

### Step 1: Generate a Keystore (if you don't already have one)
```cmd
keytool -genkey -v -keystore my-upload-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
```

### Step 2: Set Environment Variables
```cmd
set KEYSTORE_PATH=C:\path\to\my-upload-key.jks
set STORE_PASSWORD=your_store_password
set KEY_ALIAS=upload
set KEY_PASSWORD=your_key_password
```

### Step 3: Run the Release Build
```cmd
# Windows
gradlew.bat assembleRelease

# Linux / macOS
./gradlew assembleRelease
```

**Artifact Location**:
`app/build/outputs/apk/release/app-release.apk`

---

## 5. Troubleshooting Common Issues

### Issue: `SDK location not found`
- **Solution**: Ensure `local.properties` contains the correct absolute path to your Android SDK (`sdk.dir=...`).

### Issue: `Unsupported class file major version`
- **Solution**: Ensure your active JDK is version 17 or 21. If using Android Studio, verify under **Settings -> Build, Execution, Deployment -> Build Tools -> Gradle -> Gradle JDK**.

### Issue: `WebView black screen or WebGL crash`
- **Solution**: The 3D Warland engine has been optimized with 1024x1024 shadow maps, hardware instancing, and capped device pixel ratio (`max 1.5x`). Ensure Android System WebView is updated to latest version on older devices.
