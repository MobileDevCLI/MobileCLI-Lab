# MobileCLI Reference Build - v1.6.1-fix (v66)

> **THIS IS THE 99% PERFECT WORKING VERSION**
>
> DO NOT MODIFY THIS FILE OR THE APK IN THIS FOLDER.
> This is preserved as a reference point for future development.

---

## File Details

| Property | Value |
|----------|-------|
| **Filename** | `WORKING-REFERENCE-v66-DO-NOT-MODIFY.apk` |
| **Original Name** | `MobileCLI-v1.6.1-fix.apk` (filename misleading) |
| **Actual Version** | `1.8.1-dev` (versionCode 76) |
| **Bootstrap Version** | `mobilecli-v66` |
| **Size** | 6,877,419 bytes (~6.6 MB) |
| **Build Date** | January 9, 2026 |
| **Status** | 99% Perfect - Fully Functional |
| **Note** | Another AI made hard changes inside - actual version is 1.8.1 |

---

## What Works Perfectly

1. **Clean Terminal Welcome (motd)**
   - Shows: "Welcome to MobileCLI - AI-Powered Terminal"
   - Suggests: claude, gemini, or codex commands
   - NO intellectual property exposed

2. **Correct PS1 Prompt**
   - Shows: `u0_a513@mobilecli:~$`
   - Green username, blue directory
   - NOT `@mobilecli-games`

3. **Bootstrap Installation**
   - Downloads ~50MB bootstrap from GitHub
   - Extracts to `/data/data/com.termux/files/usr/`
   - Sets up all permissions correctly

4. **AI Assistants**
   - Claude Code works perfectly
   - Gemini CLI works
   - Codex CLI works

5. **Termux API (50+ commands)**
   - termux-clipboard-get/set
   - termux-notification
   - termux-open-url
   - termux-camera-photo
   - All API commands functional

6. **Development Tools**
   - Java 17 (openjdk-17)
   - Gradle build system
   - Android SDK tools (aapt2, d8, apksigner)
   - Git

7. **Self-Rebuild Capability**
   - Can rebuild itself from source
   - `mobilecli-rebuild` script
   - `install-dev-tools` script

8. **Persistent Memory System**
   - `~/.mobilecli/memory/` directory
   - evolution_history.json
   - problems_solved.json
   - capabilities.json
   - goals.json

---

## Known Minor Issues (1%)

1. **Welcome overlay timing** - Sometimes flashes briefly
2. **First launch** - May need to wait for bootstrap download

---

## Key Files in This Version

### BootstrapInstaller.java (Decompiled)
- Location: `~/v161-decompiled/sources/com/termux/BootstrapInstaller.java`
- `BOOTSTRAP_VERSION = "mobilecli-v66"`
- Creates motd at `/data/data/com.termux/files/usr/etc/motd`
- Creates .bashrc with correct PS1

### motd Content
```
Welcome to MobileCLI - AI-Powered Terminal

Type 'claude', 'gemini', or 'codex' to start an AI assistant.
Type 'pkg help' for package management.
```

### .bashrc PS1
```bash
PS1='\[\e[32m\]\u@mobilecli\[\e[0m\]:\[\e[34m\]\w\[\e[0m\]$ '
```

### CLAUDE.md (Compact Version)
- File access paths
- 50+ Termux API commands table
- Development tools list
- Quick examples

---

## How to Use This Reference

1. **If something breaks**: Install this APK to get back to working state
2. **For comparison**: Decompile and compare with new builds
3. **For documentation**: This file documents all working features

---

## Decompiled Source Location

The full decompiled source is at:
```
~/v161-decompiled/sources/com/termux/
```

Key files:
- `BootstrapInstaller.java` - Bootstrap + scripts + motd
- `MainActivity.java` - Main activity with overlays
- `TermuxService.java` - Background service

---

## Version History Leading to This

| Version | Date | Changes |
|---------|------|---------|
| v54 | Jan 6 | File-based am command system |
| v56 | Jan 6 | Persistent AI memory system |
| v58 | Jan 6 | Developer Mode + Clean Setup UI |
| v60 | Jan 6 | Autonomous Intelligence System |
| v64 | Jan 6 | AAA Professional UI |
| v65 | Jan 6 | mobilecli-share Bluetooth transfer |
| **v66** | Jan 6 | **Zero terminal flash - THIS VERSION** |

---

## IP Protection Approach

This version uses **terminal output control** instead of overlays:

1. **motd file** - Controls what bash shows on startup
2. **Clean .bashrc** - No sensitive info in prompt or aliases
3. **No IP in bootstrap output** - Silent installation

This is more elegant than overlay-based hiding because:
- Terminal never shows sensitive commands
- Users see clean, professional interface
- No race conditions with overlay timing

---

## Build Configuration (Actual APK)

```kotlin
BOOTSTRAP_VERSION = "mobilecli-v66"
applicationId = "com.termux"
versionCode = 76
versionName = "1.8.1-dev"
targetSdk = 28
minSdk = 24
compileSdk = 34
```

**Note:** The filename says v1.6.1-fix but the actual APK contains v1.8.1-dev code.
Another AI made modifications resulting in this version.

---

## Owner

- **Created by**: Samblamz / MobileDevCLI
- **Preserved on**: January 10, 2026
- **Purpose**: Reference for future development

---

**REMEMBER: This APK is the gold standard. Always compare new builds against it.**
