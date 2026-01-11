# MobileCLI - Product Requirements Document (PRD)

**Version:** 1.6.2
**Date:** January 10, 2026
**Author:** MobileDevCLI
**Status:** Production Ready

---

## 1. Product Overview

### 1.1 What is MobileCLI?

MobileCLI is an Android terminal application that runs AI assistants (Claude Code, Gemini CLI, Codex) directly on a mobile device. It provides a full Linux-like environment powered by Termux libraries, enabling users to interact with AI coding assistants without a desktop computer.

### 1.2 Key Achievement

MobileCLI is the first documented case of:
- An Android app built entirely on an Android phone
- Using AI (Claude Code) for development
- Where the resulting app can run the AI that built it
- Creating a self-referential AI development loop

---

## 2. Product Goals

### 2.1 Primary Goals

| Goal | Description |
|------|-------------|
| **Mobile AI Access** | Enable Claude Code and other AI assistants on Android |
| **Professional UX** | Clean, polished user experience without exposing technical details |
| **Self-Modifying** | Allow the app to rebuild itself from within |
| **Termux Compatible** | Maintain compatibility with Termux binaries and packages |

### 2.2 Non-Goals

- Desktop/laptop development environment
- iOS support
- GUI-based IDE features

---

## 3. User Personas

### 3.1 Primary User: Mobile Developer

- **Profile:** Developer who wants AI coding help on their phone
- **Needs:** Quick access to Claude Code without a laptop
- **Pain Points:** Carrying laptop everywhere, limited mobile tools

### 3.2 Secondary User: Power User

- **Profile:** Technical user comfortable with terminals
- **Needs:** Full terminal access with AI enhancement
- **Pain Points:** Standard terminal apps lack AI integration

---

## 4. Features

### 4.1 Core Features

| Feature | Description | Status |
|---------|-------------|--------|
| **Terminal Emulator** | Full terminal with Termux libraries | Complete |
| **AI Integration** | Claude Code, Gemini CLI, Codex support | Complete |
| **Multi-Session** | Up to 10 concurrent terminal sessions | Complete |
| **Setup Overlay** | Clean loading screen during bootstrap | Complete |
| **Developer Mode** | Hidden mode for full terminal visibility | Complete |

### 4.2 Bootstrap System

The app bootstraps a complete Linux environment:
- Package manager (pkg)
- Node.js runtime
- Python environment
- Build tools (Java, Gradle, aapt)
- AI assistants (npm packages)

### 4.3 Termux API

50+ built-in API commands:
- Clipboard access
- Notifications
- Camera/sensors
- URL opening
- System information

---

## 5. Technical Requirements

### 5.1 Package Name

**MUST be `com.termux`**

Termux binaries have hardcoded RUNPATH `/data/data/com.termux/files/usr/lib`. Any other package name causes library linking failures.

### 5.2 Target SDK

**MUST be 28 or lower**

Android 10+ (API 29+) blocks exec() from app data directories. targetSdk=28 allows binary execution.

### 5.3 Environment Variables

Required variables that must match real Termux:
- HOME
- PREFIX
- PATH
- LD_LIBRARY_PATH
- LD_PRELOAD
- TMPDIR
- All TERMUX_APP__* variables

---

## 6. User Experience Requirements

### 6.1 First Launch Flow

1. User opens app
2. Setup Wizard appears with 5 stages:
   - Welcome
   - Setup (bootstrap)
   - AI Choice (Claude/Gemini/Codex/Terminal)
   - Installing
   - Ready
3. AI launches automatically
4. User sees AI welcome message

### 6.2 Subsequent Launch Flow

1. User opens app
2. Setup overlay appears briefly
3. Environment loads
4. AI command runs (if configured)
5. Overlay hides AFTER AI starts rendering
6. User sees AI interface

### 6.3 UX Requirement: IP Protection

**CRITICAL:** Users must NEVER see:
- Bootstrap commands
- Installation scripts
- Terminal output before AI loads
- Any proprietary code

The setup overlay must remain visible until the AI's UI is ready.

---

## 7. Build Specifications

### 7.1 Build Variants

| Variant | Purpose | DEV_MODE |
|---------|---------|----------|
| **user** | End users | OFF |
| **dev** | Developers | ON |

### 7.2 Build Types

| Type | Signing | MinifyEnabled |
|------|---------|---------------|
| **debug** | Debug keystore | false |
| **release** | Release keystore | false |

### 7.3 Output APKs

- `app-user-release.apk` - Production user build
- `app-user-debug.apk` - Debug user build
- `app-dev-release.apk` - Production dev build
- `app-dev-debug.apk` - Debug dev build

---

## 8. Success Metrics

| Metric | Target |
|--------|--------|
| **Bootstrap Success** | >95% |
| **AI Launch Success** | >99% |
| **Session Stability** | No crashes |
| **IP Protection** | 0 exposure incidents |

---

## 9. Milestones

| Version | Milestone | Date |
|---------|-----------|------|
| v10 | First working version | Jan 5, 2026 |
| v19 | Screen sizing fix | Jan 5, 2026 |
| v55 | URL opening + OAuth | Jan 6, 2026 |
| v93 | IP hiding + keyboard fix | Jan 9, 2026 |
| v94 (1.6.2) | Overlay timing fix | Jan 10, 2026 |

---

## 10. Dependencies

### 10.1 Libraries

| Library | Version | License |
|---------|---------|---------|
| terminal-view | v0.118.0 | Apache 2.0 |
| terminal-emulator | v0.118.0 | Apache 2.0 |
| AndroidX Core | 1.12.0 | Apache 2.0 |
| Material Design | 1.10.0 | Apache 2.0 |
| Kotlin Coroutines | 1.7.3 | Apache 2.0 |

### 10.2 Runtime Dependencies

- Bootstrap archive (Termux packages)
- am.apk (Termux Activity Manager)
- Node.js (for AI CLIs)

---

## 11. Security Considerations

### 11.1 Permissions

| Permission | Purpose |
|------------|---------|
| INTERNET | Package downloads, AI API calls |
| WRITE_EXTERNAL_STORAGE | Save files to /sdcard |
| WAKE_LOCK | Keep CPU awake for long tasks |
| CAMERA | termux-camera-photo API |
| VIBRATE | termux-vibrate API |
| SYSTEM_ALERT_WINDOW | URL opening from shell |

### 11.2 Android 14+ Security

am.apk must be read-only (chmod 0400) to prevent DEX security exceptions.

---

## 12. Future Roadmap

| Feature | Priority | Status |
|---------|----------|--------|
| Home screen widgets | Medium | Not started |
| Tasker integration | Low | Partial |
| Multiple AI providers | Low | Complete |
| Self-rebuild command | Medium | Complete |

---

## 13. Contact

- **GitHub:** https://github.com/MobileDevCLI
- **Repository:** https://github.com/MobileDevCLI/MobileCLI-v2
- **Website:** https://mobilecli.com

---

*This document is maintained as part of the MobileCLI project.*
