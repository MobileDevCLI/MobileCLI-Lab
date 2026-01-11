# MobileCLI Lab - Claude Code Instructions

> **LAB EDITION** - Experimental sandbox for testing new inventions

---

## What is This?

This is **MobileCLI-Lab** - an isolated sandbox for experimenting with new features without risking the stable codebase.

**Main Quest:** MobileCLI-Store (the sellable product)
**Side Quests:** MobileCLI-Lab (this repo - inventions)

---

## Lab Rules

1. **Experiments stay here** until proven to work
2. **Document in LAB-NOTES.md** what you're testing
3. **Break things freely** - this is the sandbox
4. **Don't merge to Store/Developer** until verified

---

## Reference Build (99% Perfect)

If experiments break everything, install the reference APK:
- `reference/WORKING-REFERENCE-v66-DO-NOT-MODIFY.apk`

---

## Current Lab Version

| Property | Value |
|----------|-------|
| versionCode | 100 |
| versionName | 2.0.0-lab |
| Bootstrap | mobilecli-v67 |
| Build Flavor | lab |

---

## Active Experiments

### Multi-Agent System
Multiple Claude instances communicating via `~/.claude/` JSONL files.

```bash
agent discover   # Find sessions
agent read <id>  # Read conversation
agent send <id>  # Send message
agent hub        # Supervisor mode
```

See: `MULTI-AGENT.md` and `LAB-NOTES.md`

---

## Bootstrap System

### Version Marker
```kotlin
BOOTSTRAP_VERSION = "mobilecli-v66"
```

### Bootstrap URL
```
https://github.com/termux/termux-packages/releases/download/bootstrap-2026.01.04-r1+apt.android-7/bootstrap-aarch64.zip
```

### Files Created by Bootstrap

| File | Purpose |
|------|---------|
| `/data/data/com.termux/files/usr/etc/motd` | Welcome message (IP protection) |
| `/data/data/com.termux/files/home/.bashrc` | Shell config with PS1 |
| `/data/data/com.termux/files/home/CLAUDE.md` | AI briefing |
| `/data/data/com.termux/files/usr/etc/mobilecli-version` | Version marker |
| `/data/data/com.termux/files/usr/bin/mobilecli-*` | Helper scripts |

### motd Content
```
Welcome to MobileCLI - AI-Powered Terminal

Type 'claude', 'gemini', or 'codex' to start an AI assistant.
Type 'pkg help' for package management.
```

### PS1 Prompt
```bash
PS1='\[\e[32m\]\u@mobilecli\[\e[0m\]:\[\e[34m\]\w\[\e[0m\]$ '
# Shows: u0_a513@mobilecli:~$
```

---

## Directory Structure

```
MobileCLI-Store/
├── app/src/main/java/com/termux/
│   ├── BootstrapInstaller.kt    # Bootstrap + motd + scripts
│   ├── MainActivity.kt          # Main activity + overlays
│   ├── TermuxService.kt         # Background terminal service
│   └── ...
├── reference/
│   ├── WORKING-REFERENCE-v66-DO-NOT-MODIFY.apk
│   └── REFERENCE.md
├── README.md                    # Full documentation
└── CLAUDE.md                    # This file
```

---

## Build Commands

### Store Edition (IP Protected)
```bash
cd ~/MobileCLI-Store
export JAVA_HOME=/data/data/com.termux/files/usr/lib/jvm/java-17-openjdk
./gradlew assembleUserDebug
cp app/build/outputs/apk/user/debug/app-user-debug.apk /sdcard/Download/MobileCLI-Store.apk
```

### Developer Edition
```bash
cd ~/MobileCLI-Developer
./gradlew assembleDevDebug
cp app/build/outputs/apk/dev/debug/app-dev-debug.apk /sdcard/Download/MobileCLI-Developer.apk
```

---

## Key Source Files

| File | What It Does |
|------|--------------|
| `BootstrapInstaller.kt` | Downloads bootstrap, creates motd/bashrc/scripts, sets permissions |
| `MainActivity.kt` | Main activity, setup overlays, welcome screen, AI selection |
| `TermuxService.kt` | Background terminal service, command polling |
| `build.gradle.kts` | Build config, version codes, signing |

---

## IP Protection Strategy

The Store edition protects intellectual property through **terminal output control**:

1. **motd file** - Controls what bash shows on startup (clean welcome)
2. **Clean .bashrc** - PS1 shows `@mobilecli`, no sensitive aliases
3. **Silent bootstrap** - Installation output not visible to users

This is more reliable than overlay-based protection.

---

## MobileCLI Scripts (in $PREFIX/bin/)

| Script | Purpose |
|--------|---------|
| `mobilecli-memory` | View/manage persistent AI memory |
| `mobilecli-rebuild` | Rebuild app from source |
| `mobilecli-caps` | Show all capabilities |
| `mobilecli-share` | Bluetooth file transfer |
| `mobilecli-dev-mode` | Toggle developer mode |
| `install-dev-tools` | Install Java, Gradle, Android SDK |
| `setup-github` | Configure GitHub credentials |

---

## Persistent Memory System

Location: `~/.mobilecli/memory/`

| File | Purpose |
|------|---------|
| `evolution_history.json` | Self-modification milestones |
| `problems_solved.json` | Issues fixed with solutions |
| `capabilities.json` | What the AI can do |
| `goals.json` | Current objectives |

---

## Problems Solved (Reference)

| Problem | Solution | Version |
|---------|----------|---------|
| HOME directory wrong | Changed to `/data/data/com.termux/files/home` | v10 |
| OAuth wouldn't open | am.apk chmod 0400 for Android 14+ | v54 |
| npm/node errors | Fixed HOME path | v10 |
| Background restrictions | File-based IPC | v51+ |

---

## Working in This Codebase

### DO:
- Check `reference/REFERENCE.md` for working behavior
- Compare against `reference/WORKING-REFERENCE-v66-DO-NOT-MODIFY.apk`
- Save APKs to `/sdcard/Download/`
- Test on actual device

### DON'T:
- Modify the reference APK
- Change `@mobilecli` prompt to something else
- Remove motd creation
- Hardcode new credentials

---

## Owner

**Samblamz / MobileDevCLI**

- GitHub: https://github.com/MobileDevCLI
- Website: https://mobilecli.com

---

**Copyright 2026 MobileDevCLI. All Rights Reserved.**
