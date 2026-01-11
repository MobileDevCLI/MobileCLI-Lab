---
name: build-apk
description: Build MobileCLI Android APK from source code
---

# Build APK Skill

Use this skill when the user asks to build, compile, or create an APK.

## Prerequisites
- Java 17: `/data/data/com.termux/files/usr/lib/jvm/java-17-openjdk`
- Gradle: Available in PATH
- Android SDK: `~/android-sdk/`

## Build Commands

### User Build (Clean UX)
```bash
cd ~/MobileCLI-v2-fix
export JAVA_HOME=/data/data/com.termux/files/usr/lib/jvm/java-17-openjdk
./gradlew assembleUserDebug
```
Output: `app/build/outputs/apk/user/debug/app-user-debug.apk`

### Dev Build (Full Terminal)
```bash
./gradlew assembleDevDebug
```
Output: `app/build/outputs/apk/dev/debug/app-dev-debug.apk`

## Post-Build Steps

1. **Copy to Downloads:**
```bash
cp app/build/outputs/apk/user/debug/app-user-debug.apk /sdcard/Download/MobileCLI-vX.X.X.apk
```

2. **Update Version:** Edit `app/build.gradle.kts`:
```kotlin
versionCode = XX  // Increment
versionName = "X.X.X-description"
```

3. **Update Progress:** Edit `progress/progress.md` with what was changed

## Common Issues

### License not accepted
```bash
mkdir -p ~/android-sdk/licenses
echo "8933bad161af4178b1185d1a37fbf41ea5269c55" > ~/android-sdk/licenses/android-sdk-license
```

### Java version wrong
```bash
export JAVA_HOME=/data/data/com.termux/files/usr/lib/jvm/java-17-openjdk
```

### SDK path issues
```bash
rm -rf ~/android-sdk/platforms/android-34-2 ~/android-sdk/build-tools/34.0.0-2
```
