# MobileCLI Persistent Memory

> This file stores critical context that must persist across Claude sessions.
> Read this file at the START of every session.

---

## What is MobileCLI?

MobileCLI is a proprietary Android terminal application that:
- Runs Claude Code, Gemini CLI, and Codex CLI on Android phones
- Uses Termux bootstrap for Linux environment
- Package name MUST be `com.termux` (hardcoded RUNPATH in binaries)
- targetSdk MUST be 28 (Android 10+ blocks exec from app data)

**Owner:** Samblamz
**Organization:** MobileDevCLI
**Website:** https://mobilecli.com
**GitHub:** https://github.com/MobileDevCLI

---

## Current State (Updated 2026-01-10)

### Version
- **Current:** 1.6.3-welcome-fix (v95)
- **APK Location:** `/sdcard/Download/MobileCLI-v1.6.3-welcome-fix.apk`

### Active Issues
1. CRITICAL: Exposed secrets need rotation
2. CRITICAL: Missing Privacy Policy for app store
3. HIGH: Security vulnerabilities in WebView and command execution

### Recent Changes
- Added welcome_overlay to fix terminal flash
- Completed comprehensive security audit
- Created progress tracking system

---

## Key Technical Facts

### Bootstrap Flow
1. Download bootstrap (~50MB from GitHub)
2. Extract to /data/data/com.termux/files/usr/
3. Show AI choice screen (Claude/Gemini/Codex)
4. Download AI tools + dev tools (several minutes)
5. Hit 100%, show welcome overlay
6. Launch selected AI
7. Fade out welcome overlay

### Critical Paths
```
/data/data/com.termux/files/home/    # HOME directory
/data/data/com.termux/files/usr/     # PREFIX (binaries, libs)
/sdcard/Download/                     # User-accessible files
```

### Key Source Files
```
app/src/main/java/com/termux/
├── MainActivity.kt          # Main activity, overlays
├── BootstrapInstaller.kt    # Bootstrap, API scripts
├── LicenseManager.kt        # Supabase license checking
├── ThirdPartyLicenses.kt    # License attribution
└── app/
    └── TermuxService.kt     # Background service
```

---

## Security Status

### COMPROMISED (Assume Leaked)
- GitHub token in HANDOFF.md
- Supabase API key in LicenseManager.kt
- Signing keystore + passwords in build.gradle.kts

### VULNERABLE
- Command injection in MobileAppsRoomActivity
- WebView file access enabled
- JavaScript injection via INJECT_JS
- 10+ exported components without protection

### MISSING
- Privacy Policy
- Terms of Service
- GDPR/CCPA compliance
- In-app legal screen

---

## Working Agreements

### When Making Changes
1. NEVER modify ~/MobileCLI-v2/ directly (backup)
2. Work in ~/MobileCLI-v2-fix/ for changes
3. Update version in build.gradle.kts
4. Update progress/progress.md after completing work
5. Copy APK to /sdcard/Download/ with version name

### When Starting a Session
1. Read this file first
2. Read progress/progress.md for current status
3. Read progress/bugs-and-issues.md for known issues
4. Ask user what they want to work on

### Build Commands
```bash
cd ~/MobileCLI-v2-fix
export JAVA_HOME=/data/data/com.termux/files/usr/lib/jvm/java-17-openjdk
./gradlew assembleUserDebug
cp app/build/outputs/apk/user/debug/app-user-debug.apk /sdcard/Download/MobileCLI-vX.X.X.apk
```

---

## Owner Preferences

- Save files to `/sdcard/Download/` for easy access
- Use `termux-toast` for quick notifications
- Test on same device (dev phone = test phone)
- Protect proprietary code from being visible to users
- Professional app quality for commercial sale
