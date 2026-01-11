# MobileCLI Master Reference

> **READ THIS FIRST** - Complete guide to the MobileCLI ecosystem.
> This file contains everything Claude needs to understand the project.

**Last Updated:** January 10, 2026
**Owner:** Samblamz
**Organization:** MobileDevCLI

---

## GitHub Repositories

### Your Repositories (MobileDevCLI)

| Repository | URL | Description |
|------------|-----|-------------|
| **MobileCLI-v2** | https://github.com/MobileDevCLI/MobileCLI-v2 | Main source code - Open source Android terminal with Claude Code support |
| **MobileCLI-Proprietary** | https://github.com/MobileDevCLI/MobileCLI-Proprietary | Commercial version - 100% proprietary code for sale |
| **website** | https://github.com/MobileDevCLI/website | Landing page - mobilecli.com |
| **claude-bridge** | https://github.com/MobileDevCLI/claude-bridge | Two-Claude workflow communication (BUILD_INSTRUCTIONS.md, TEST_REPORT.md) |
| **ditto-store** | https://github.com/MobileDevCLI/ditto-store | DITTO profile sharing - Community UI profiles |

### Termux Dependencies (Third-Party)

| Repository | URL | Description |
|------------|-----|-------------|
| **termux-packages** | https://github.com/termux/termux-packages | Bootstrap packages - Downloaded at runtime (~50MB) |
| **termux-app** | https://github.com/termux/termux-app | Original Termux app - Reference for compatibility |
| **termux-am** | https://github.com/termux/termux-am | Activity manager - am.apk for URL opening |
| **termux-am-socket** | https://github.com/termux/termux-am-socket | Socket-based am communication |

---

## Quick Navigation

### For New Claude Sessions

1. **Start Here:** Read this file (MASTER_REFERENCE.md)
2. **Current Status:** `progress/progress.md`
3. **Known Bugs:** `progress/bugs-and-issues.md`
4. **Security Issues:** `progress/security-audit.md`
5. **Full IP Docs:** `progress/intellectual-property.md`
6. **Core Context:** `.mobilecli/memory/context.md`

### Key Source Files

| File | Path | Purpose |
|------|------|---------|
| MainActivity.kt | `app/src/main/java/com/termux/` | Main activity, overlays, DITTO UI |
| BootstrapInstaller.kt | `app/src/main/java/com/termux/` | Bootstrap, 50+ API scripts |
| LicenseManager.kt | `app/src/main/java/com/termux/` | Supabase license verification |
| TermuxApiReceiver.kt | `app/src/main/java/com/termux/` | Handle API broadcast calls |
| TermuxService.kt | `app/src/main/java/com/termux/app/` | Background service, wake lock |
| activity_main.xml | `app/src/main/res/layout/` | Layout with welcome_overlay |
| AndroidManifest.xml | `app/src/main/` | 40+ permissions |
| build.gradle.kts | `app/` | Build config, signing (SECRETS HERE) |

---

## Project Structure

```
MobileCLI-v2-fix/
├── app/
│   ├── src/main/
│   │   ├── java/com/termux/
│   │   │   ├── MainActivity.kt         # ~2900 lines
│   │   │   ├── BootstrapInstaller.kt   # ~2800 lines
│   │   │   ├── LicenseManager.kt       # ~250 lines (CONTAINS SECRETS)
│   │   │   ├── TermuxApiReceiver.kt    # ~1500 lines
│   │   │   ├── ThirdPartyLicenses.kt   # ~180 lines
│   │   │   └── app/
│   │   │       ├── TermuxService.kt    # ~300 lines
│   │   │       └── TermuxOpenReceiver.kt
│   │   ├── res/layout/
│   │   │   └── activity_main.xml       # ~800 lines
│   │   ├── assets/
│   │   │   ├── termux-am/am.apk        # Activity manager
│   │   │   └── MOBILECLI_ENVIRONMENT.md
│   │   └── AndroidManifest.xml         # ~450 lines
│   └── build.gradle.kts                # (CONTAINS SECRETS)
├── progress/                           # Progress tracking
│   ├── progress.md                     # Current status
│   ├── bugs-and-issues.md              # Known bugs
│   ├── security-audit.md               # Security findings
│   └── intellectual-property.md        # Full IP documentation
├── .mobilecli/
│   └── memory/
│       └── context.md                  # Persistent knowledge
├── .claude/
│   └── skills/
│       ├── build-apk.md                # Build instructions
│       └── security-fix.md             # Security fixes
├── CLAUDE.md                           # Session startup instructions
├── MASTER_REFERENCE.md                 # THIS FILE
└── mobilecli-release.keystore          # Signing key (SENSITIVE)
```

---

## Current Version

**v1.6.3-welcome-fix (v95)** - January 10, 2026

### APK Location
```
/sdcard/Download/MobileCLI-v1.6.3-welcome-fix.apk
```

### Build Command
```bash
cd ~/MobileCLI-v2-fix
export JAVA_HOME=/data/data/com.termux/files/usr/lib/jvm/java-17-openjdk
./gradlew assembleUserDebug
cp app/build/outputs/apk/user/debug/app-user-debug.apk /sdcard/Download/MobileCLI-vX.X.X.apk
```

---

## Critical Security Status

| Item | Status | Action |
|------|--------|--------|
| GitHub Token | EXPOSED in HANDOFF.md | REVOKE at github.com/settings/tokens |
| Supabase Key | EXPOSED in LicenseManager.kt | Rotate in Supabase dashboard |
| Signing Key | EXPOSED in build.gradle.kts | Regenerate keystore |
| Privacy Policy | MISSING | Create before app store |
| Terms of Service | MISSING | Create before app store |

---

## What Makes MobileCLI Unique

### 15 World Firsts

1. **Self-Rebuilding Android App** - AI modifies and rebuilds its own container
2. **File-Based IPC for URL Opening** - Solved Android 10+ background restrictions
3. **DITTO Architecture** - AI controls UI at runtime via terminal commands
4. **Persistent AI Memory** - Learnings survive across sessions
5. **50+ Termux API Commands** - Built-in without separate app
6. **Profile Cloud Sync** - GitHub Gist backup for UI profiles
7. **Morphable WebView Layer** - Load custom HTML as overlay
8. **JavaScript Bridge** - HTML→Native communication
9. **Dev Tools Auto-Install** - Full Android SDK on phone
10. **Two-Claude Workflow** - BUILD and TEST Claude coordination
11. **Phone-to-Phone Bluetooth Share** - APK transfer workflow
12. **Developer Mode Toggle** - 7-tap activation like Android
13. **Welcome Overlay System** - Hide proprietary code from users
14. **Activity Dispatch System** - Native activity starts from shell
15. **Self-Documenting Codebase** - AI briefing installed at bootstrap

---

## Bootstrap Flow

```
1. App Opens
   └── Show setup_overlay (white screen)

2. Download Bootstrap (~50MB)
   └── From github.com/termux/termux-packages

3. Extract to /data/data/com.termux/files/usr/
   └── bash, coreutils, apt, node, etc.

4. Install TermuxAm
   └── am.apk + wrapper script for URL opening

5. Install 50+ API Scripts
   └── termux-clipboard, termux-camera, etc.

6. Configure npm & GitHub

7. Initialize Memory System
   └── ~/.mobilecli/memory/

8. Show AI Choice Screen
   └── Claude / Gemini / Codex cards

9. User Selects AI

10. Install AI Tools (Several Minutes)
    └── nodejs, npm install claude-code, dev tools

11. 100% Complete
    └── Clear terminal, launch AI command

12. Show welcome_overlay (4 seconds)
    └── "Welcome to MobileCLI" - hides terminal

13. Fade Out Welcome
    └── User sees Claude UI
```

---

## Working Agreements

### DO
- Work in `~/MobileCLI-v2-fix/` (safe copy)
- Update `progress/progress.md` after work
- Log bugs in `progress/bugs-and-issues.md`
- Increment version in `build.gradle.kts`
- Save APKs to `/sdcard/Download/`

### DON'T
- Modify `~/MobileCLI-v2/` (backup)
- Commit secrets to source
- Hardcode credentials
- Enable WebView file access
- Pass unsanitized input to shell

---

## Useful Commands

### Build
```bash
./gradlew assembleUserDebug    # User build (clean UX)
./gradlew assembleDevDebug     # Dev build (terminal visible)
```

### Memory System
```bash
mobilecli-memory status        # Show memory
mobilecli-memory history       # Evolution history
mobilecli-memory problems      # Solved problems
```

### UI Control
```bash
mobilecli-ui set-background "#1a1a2e"
mobilecli-ui set-text-size 32
mobilecli-ui save-profile my-setup
mobilecli-ui browser           # Profile browser UI
```

### Self-Rebuild
```bash
install-dev-tools              # One-time setup
mobilecli-rebuild              # Full rebuild
```

---

## Contact & Links

- **Website:** https://mobilecli.com
- **GitHub:** https://github.com/MobileDevCLI
- **Owner:** Samblamz
- **Email:** mobiledevcli@gmail.com

---

## File Locations Summary

| What | Where |
|------|-------|
| This reference | `MASTER_REFERENCE.md` |
| Session startup | `CLAUDE.md` |
| Current progress | `progress/progress.md` |
| Bug tracking | `progress/bugs-and-issues.md` |
| Security audit | `progress/security-audit.md` |
| Full IP docs | `progress/intellectual-property.md` |
| Persistent memory | `.mobilecli/memory/context.md` |
| Build skill | `.claude/skills/build-apk.md` |
| Security skill | `.claude/skills/security-fix.md` |

---

**With this file, any Claude session can immediately understand MobileCLI.**

**Copyright 2026 Samblamz / MobileDevCLI. All Rights Reserved.**
