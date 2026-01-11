# MobileCLI Intellectual Property Documentation

> Complete documentation of all proprietary systems, inventions, and IP.
> Use this to rebuild MobileCLI from scratch if ever needed.

**Owner:** Samblamz
**Organization:** MobileDevCLI
**Created:** January 5, 2026
**Last Updated:** January 10, 2026

---

## Table of Contents

1. [Core Invention: The Bootstrap System](#1-core-invention-the-bootstrap-system)
2. [DITTO Architecture: AI UI Control](#2-ditto-architecture-ai-ui-control)
3. [Persistent Memory System](#3-persistent-memory-system)
4. [50+ Termux API Commands](#4-50-termux-api-commands)
5. [Self-Modification System](#5-self-modification-system)
6. [Dev Tools Installation](#6-dev-tools-installation)
7. [Profile System (Save/Share/Cloud)](#7-profile-system)
8. [40+ Android Permissions](#8-40-android-permissions)
9. [Environment Variables](#9-environment-variables)
10. [AI Briefing System](#10-ai-briefing-system)
11. [Custom Scripts & Commands](#11-custom-scripts--commands)
12. [Key Source Files](#12-key-source-files)
13. [Rebuild Instructions](#13-rebuild-instructions)

---

## 1. Core Invention: The Bootstrap System

**File:** `BootstrapInstaller.kt` (~2800 lines)

### What It Does
Downloads and installs a complete Linux environment on Android, transforming a phone into a full development machine.

### Bootstrap Flow

```
1. Download bootstrap-aarch64.zip (~50MB) from GitHub
2. Extract to /data/data/com.termux/files/usr/
3. Set permissions (chmod 755 for executables)
4. Install TermuxAm (custom activity manager)
5. Install 50+ API scripts
6. Configure npm for Termux
7. Setup GitHub credentials
8. Initialize persistent memory system
9. Write version marker (.mobilecli_version)
```

### Key Innovation: TermuxAm

Standard Android `am` command fails from shell. MobileCLI invented file-based IPC:

```bash
# Shell writes command to file
echo "start -a android.intent.action.VIEW -d https://..." > ~/.termux/am_command

# TermuxService polls and executes with app permissions
# Result written back to ~/.termux/am_result
```

This solves the Android 10+ background activity restriction.

### Critical Paths

| Path | Purpose |
|------|---------|
| `/data/data/com.termux/files/home/` | HOME directory (CRITICAL - must be /files/home/, not /files/) |
| `/data/data/com.termux/files/usr/` | PREFIX - binaries, libraries |
| `/data/data/com.termux/files/usr/bin/` | Executables (bash, node, 50+ API scripts) |
| `/data/data/com.termux/files/usr/lib/` | Libraries (libtermux-exec-ld-preload.so) |
| `/data/data/com.termux/files/usr/etc/` | Config files (passwd, group, hosts) |

### Package Name Requirement

**MUST be `com.termux`** - Termux binaries have hardcoded RUNPATH:
```
/data/data/com.termux/files/usr/lib
```
Any other package name causes library linking failures.

### targetSdk Requirement

**MUST be 28 or lower** - Android 10+ (API 29+) blocks exec() from app data directories.

---

## 2. DITTO Architecture: AI UI Control

**File:** `mobilecli-ui` script (430 lines) + `MainActivity.kt` handlers

### What It Does
Allows AI (Claude) and users to control the app's UI at runtime via shell commands.

### How It Works

```
Terminal Command → Write to ~/.termux/ui_command
MainActivity polls → Reads command → Executes → Writes to ~/.termux/ui_result
Script reads result → Returns to user
```

### Available Commands

#### Extra Key Control
```bash
mobilecli-ui add-key 1 ESC 27           # Add ESC key to row 1
mobilecli-ui add-key 1 "↑" "\e[A"       # Add up arrow
mobilecli-ui remove-key 1 ESC           # Remove key
mobilecli-ui clear-keys 1               # Clear row
```

#### Theme Control
```bash
mobilecli-ui set-background "#1a1a2e"   # Background color
mobilecli-ui set-text-color "#ffffff"   # Text color
mobilecli-ui set-text-size 32           # Font size (14-56)
mobilecli-ui set-key-style "#333" "#fff" # Key colors
```

#### Morphable WebView Overlay
```bash
mobilecli-ui load-ui ~/ui/soundboard.html  # Load custom HTML
mobilecli-ui hide-ui                        # Hide overlay
mobilecli-ui show-ui                        # Show overlay
mobilecli-ui ui-size half                   # full|half|quarter
mobilecli-ui ui-position bottom             # top|bottom|left|right|center
mobilecli-ui ui-opacity 0.8                 # 0.0-1.0
mobilecli-ui inject-js "alert('hi')"        # Run JS
```

### JavaScript Bridge

HTML loaded in overlay can call native functions:

```javascript
MobileCLI.sendKey("ls\n");      // Send keystrokes to terminal
MobileCLI.closeUI();            // Close overlay
MobileCLI.getLocalProfiles();   // Get saved profiles
MobileCLI.loadProfile("name");  // Load a profile
```

---

## 3. Persistent Memory System

**Files:** `~/.mobilecli/memory/` directory + `mobilecli-memory` script

### What It Does
Enables AI to remember across sessions - track evolution, problems solved, capabilities gained.

### Memory Structure

```
~/.mobilecli/
├── memory/
│   ├── evolution_history.json  # Version history, rebuild log
│   ├── problems_solved.json    # Bugs fixed with solutions
│   ├── capabilities.json       # What AI has learned
│   └── goals.json              # Current objectives
├── config/
│   └── preferences.json        # User preferences
├── profiles/                   # Saved UI profiles
└── ui/
    └── profile_browser.html    # Visual profile browser
```

### Commands

```bash
mobilecli-memory status    # Show memory location
mobilecli-memory history   # Show evolution history
mobilecli-memory problems  # Show solved problems
mobilecli-memory caps      # Show capabilities
mobilecli-memory goals     # Show goals
mobilecli-memory log "msg" # Add to rebuild log
```

---

## 4. 50+ Termux API Commands

**File:** `BootstrapInstaller.kt` installApiScripts() function

### Clipboard (2)
- `termux-clipboard-get` - Read clipboard
- `termux-clipboard-set` - Write clipboard

### Notifications (3)
- `termux-toast` - Show toast message
- `termux-notification` - Send notification (full flags: -t, -c, -i, --priority, --ongoing, --sound, etc.)
- `termux-notification-remove` - Remove notification

### Device Info (6)
- `termux-battery-status` - Battery info as JSON
- `termux-vibrate` - Vibrate phone
- `termux-brightness` - Screen brightness
- `termux-torch` - Flashlight on/off
- `termux-volume` - Volume levels
- `termux-audio-info` - Audio info

### Network & WiFi (3)
- `termux-wifi-connectioninfo` - WiFi connection info
- `termux-wifi-enable` - Enable/disable WiFi
- `termux-wifi-scaninfo` - WiFi scan results

### Location (1)
- `termux-location` - GPS location

### Camera (2)
- `termux-camera-info` - Camera info
- `termux-camera-photo` - Take photo (-o output, -c camera)

### Audio & Media (4)
- `termux-media-scan` - Trigger media scanner
- `termux-media-player` - Play/pause/stop media
- `termux-microphone-record` - Record audio
- `termux-tts-speak` - Text to speech

### TTS (2)
- `termux-tts-engines` - List TTS engines
- `termux-tts-speak` - Speak text

### Telephony (4)
- `termux-telephony-call` - Make phone call
- `termux-telephony-cellinfo` - Cell tower info
- `termux-telephony-deviceinfo` - Device info

### SMS (2)
- `termux-sms-list` - List SMS messages
- `termux-sms-send` - Send SMS

### Contacts (1)
- `termux-contact-list` - List contacts

### Call Log (1)
- `termux-call-log` - Get call history

### Sensors (1)
- `termux-sensor` - Read sensors

### Biometric (1)
- `termux-fingerprint` - Fingerprint auth

### Infrared (2)
- `termux-infrared-frequencies` - IR frequencies
- `termux-infrared-transmit` - Transmit IR

### USB (1)
- `termux-usb` - USB device info

### System Utilities (6)
- `termux-wallpaper` - Set wallpaper
- `termux-download` - Download file
- `termux-share` - Share content
- `termux-dialog` - Show dialog
- `termux-wake-lock` - CPU wake lock
- `termux-wake-unlock` - Release wake lock

### URL Opening (3)
- `termux-open-url` - Open URL in browser
- `termux-open` - Open file/URL
- `xdg-open` - Linux standard opener

---

## 5. Self-Modification System

### The Achievement

```
Claude Code (in Termux) → Built MobileCLI → MobileCLI runs Claude Code → ∞
```

The AI can modify and rebuild its own container.

### Commands

```bash
# Install build tools (one-time)
install-dev-tools

# Rebuild the app
mobilecli-rebuild

# Or manually:
cd ~/MobileCLI-v2
./gradlew assembleDebug
cp app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/
```

### Source Location

```
~/MobileCLI-v2/           # Open source version
~/MobileCLI-Proprietary/  # Commercial version
```

---

## 6. Dev Tools Installation

**File:** `install-dev-tools` script

### What Gets Installed

```bash
pkg install -y git openjdk-17 gradle aapt aapt2 apksigner d8 dx coreutils zip unzip
```

### Android SDK Setup

```
~/android-sdk/
├── platforms/android-34/
│   └── android.jar           # From aapt package
└── build-tools/34.0.0/
    ├── aapt → /usr/bin/aapt
    ├── aapt2 → /usr/bin/aapt2
    ├── d8 → /usr/bin/d8
    ├── dx → /usr/bin/dx
    ├── apksigner → /usr/bin/apksigner
    └── zipalign → /usr/bin/zipalign
```

### Gradle Configuration

```properties
# gradle.properties
android.aapt2FromMavenOverride=/data/data/com.termux/files/home/android-sdk/build-tools/34.0.0/aapt2
```

```properties
# local.properties
sdk.dir=/data/data/com.termux/files/home/android-sdk
```

---

## 7. Profile System

### Save & Load

```bash
mobilecli-ui save-profile my-setup    # Save current UI state
mobilecli-ui load-profile my-setup    # Load saved profile
mobilecli-ui list-profiles            # List all profiles
mobilecli-ui delete-profile my-setup  # Delete profile
```

### Export & Share

```bash
mobilecli-ui export-profile my-setup  # Export to /sdcard/Download/
mobilecli-ui share-profile my-setup   # Share via Bluetooth/apps
mobilecli-ui import-profile file.mcli # Import profile
```

### GitHub Cloud Sync

```bash
# Set token (one-time)
mobilecli-ui gist-set-token [REDACTED_TOKEN]

# Upload to GitHub Gist
mobilecli-ui gist-upload my-setup

# Download from Gist
mobilecli-ui gist-download abc123def456

# Update existing Gist
mobilecli-ui gist-update my-setup abc123def456

# List your Gists
mobilecli-ui gist-list
```

### Visual Browser

```bash
mobilecli-ui browser   # Opens profile_browser.html UI
```

---

## 8. 40+ Android Permissions

### Basic
- `INTERNET` - Network access
- `ACCESS_NETWORK_STATE` - Network status
- `WAKE_LOCK` - CPU wake lock
- `FOREGROUND_SERVICE` - Background service

### Critical
- `SYSTEM_ALERT_WINDOW` - Draw over apps (required for URL opening)
- `QUERY_ALL_PACKAGES` - Find browser apps

### Storage
- `READ_EXTERNAL_STORAGE` - Read files
- `WRITE_EXTERNAL_STORAGE` - Write files
- `MANAGE_EXTERNAL_STORAGE` - Full storage access

### Network
- `ACCESS_WIFI_STATE` - WiFi status
- `CHANGE_WIFI_STATE` - Toggle WiFi
- `ACCESS_FINE_LOCATION` - GPS location
- `ACCESS_BACKGROUND_LOCATION` - Background GPS

### Camera & Media
- `CAMERA` - Take photos
- `RECORD_AUDIO` - Record microphone
- `FLASHLIGHT` - Torch control

### Telephony & SMS
- `READ_PHONE_STATE` - Phone info
- `CALL_PHONE` - Make calls
- `READ_SMS` / `SEND_SMS` - SMS access
- `READ_CALL_LOG` - Call history

### Contacts
- `READ_CONTACTS` / `WRITE_CONTACTS`

### Sensors
- `BODY_SENSORS` - Health sensors
- `USE_BIOMETRIC` - Fingerprint

### Other
- `VIBRATE`, `NFC`, `TRANSMIT_IR`, `USB_PERMISSION`
- `REQUEST_INSTALL_PACKAGES` - Install APKs
- `RECEIVE_BOOT_COMPLETED` - Boot receiver

---

## 9. Environment Variables

**File:** `BootstrapInstaller.kt` getEnvironment() function

### Core Unix
```bash
HOME=/data/data/com.termux/files/home
PREFIX=/data/data/com.termux/files/usr
PATH=$PREFIX/bin:/system/bin:/system/xbin
LD_LIBRARY_PATH=$PREFIX/lib
TMPDIR=$PREFIX/tmp
TERM=xterm-256color
SHELL=/data/data/com.termux/files/usr/bin/bash
```

### Termux Core
```bash
TERMUX_VERSION=0.118.0
TERMUX_APK_RELEASE=MOBILECLI
TERMUX__PREFIX=/data/data/com.termux/files/usr
TERMUX__HOME=/data/data/com.termux/files/home
```

### Termux App
```bash
TERMUX_APP__PID=<pid>
TERMUX_APP__UID=<uid>
TERMUX_APP__PACKAGE_NAME=com.termux
TERMUX_APP__TARGET_SDK=28
```

### Android
```bash
ANDROID_DATA=/data
ANDROID_ROOT=/system
EXTERNAL_STORAGE=/sdcard
ANDROID_STORAGE=/storage
```

### SSL (for npm/curl)
```bash
SSL_CERT_FILE=$PREFIX/etc/tls/cert.pem
NODE_EXTRA_CA_CERTS=$PREFIX/etc/tls/cert.pem
CURL_CA_BUNDLE=$PREFIX/etc/tls/cert.pem
```

### LD_PRELOAD (Critical)
```bash
LD_PRELOAD=$PREFIX/lib/libtermux-exec-ld-preload.so
```

---

## 10. AI Briefing System

**File:** `~/CLAUDE.md` (created during bootstrap)

### What AI Learns

1. Full filesystem access paths
2. 50+ Termux API commands
3. Build tools availability
4. Self-modification capability
5. Memory system location
6. Rules and guidelines

### Key Message to AI

> "This is the most powerful AI environment on any phone. Use it."

---

## 11. Custom Scripts & Commands

### MobileCLI-Specific

| Script | Purpose |
|--------|---------|
| `mobilecli-ui` | Control UI from terminal |
| `mobilecli-memory` | Manage persistent memory |
| `mobilecli-rebuild` | Rebuild app from source |
| `mobilecli-share` | Share files via Bluetooth |
| `mobilecli-caps` | Show all capabilities |
| `mobilecli-dev-mode` | Toggle developer mode |
| `install-dev-tools` | Install Java/Gradle/SDK |
| `setup-github` | Configure GitHub credentials |

### File Locations

All scripts installed to: `/data/data/com.termux/files/usr/bin/`

---

## 12. Key Source Files

| File | Lines | Purpose |
|------|-------|---------|
| `MainActivity.kt` | ~2900 | Main activity, overlays, UI commands, DITTO |
| `BootstrapInstaller.kt` | ~2800 | Bootstrap, API scripts, memory |
| `LicenseManager.kt` | ~250 | License verification (Supabase) |
| `TermuxApiReceiver.kt` | ~1500 | Handle 50+ API calls |
| `TermuxService.kt` | ~300 | Background service, wake lock |
| `ThirdPartyLicenses.kt` | ~180 | License attribution |
| `activity_main.xml` | ~800 | Layout with overlays |
| `AndroidManifest.xml` | ~450 | Permissions, components |

---

## 13. Rebuild Instructions

### From Scratch

```bash
# 1. Install Termux
pkg update && pkg upgrade -y
pkg install -y git openjdk-17 gradle

# 2. Clone source
git clone https://github.com/MobileDevCLI/MobileCLI-v2.git
cd MobileCLI-v2

# 3. Setup Android SDK
install-dev-tools

# 4. Build
./gradlew assembleDebug

# 5. Install
cp app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/
```

### Key Requirements

1. Package name MUST be `com.termux`
2. targetSdk MUST be 28
3. HOME must be `/data/data/com.termux/files/home/` (NOT `/files/`)
4. am.apk must be chmod 0400 (read-only for Android 14+)
5. All API scripts in `/usr/bin/`

---

## Version History

| Version | Date | Milestone |
|---------|------|-----------|
| v1-v9 | Jan 5, 2026 | Initial attempts (failed) |
| v10 | Jan 5, 2026 | HOME directory fix - BREAKTHROUGH |
| v19 | Jan 5, 2026 | Screen sizing fix (reflection) |
| v54 | Jan 6, 2026 | File-based IPC for URL opening |
| v55 | Jan 6, 2026 | Self-rebuild confirmed |
| v58 | Jan 6, 2026 | Developer mode + clean UX |
| v84 | Jan 8, 2026 | DITTO Architecture v1.0 |
| v88 | Jan 9, 2026 | Profile Browser UI |
| v93 | Jan 9, 2026 | IP hiding + keyboard fix |
| v95 | Jan 10, 2026 | Welcome overlay fix |

---

## This Document

This is the complete IP documentation for MobileCLI. With this document, you can:

1. Understand every system in the app
2. Rebuild from scratch if needed
3. Train new Claude sessions on the codebase
4. Protect and defend the intellectual property

**Copyright 2026 Samblamz / MobileDevCLI. All Rights Reserved.**
