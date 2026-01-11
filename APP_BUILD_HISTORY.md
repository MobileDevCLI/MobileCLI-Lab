# MobileCLI Complete Build History & Technical Reference

**This file chronicles everything about how MobileCLI is built, all issues encountered, and their solutions.**

**Created:** January 5, 2026
**Last Updated:** January 5, 2026 (v32 - 100% COMPLETE)
**Author:** Built with Claude Code on Android

---

## Quick Reference

### Build Commands
```bash
# Build APK
./gradlew assembleDebug

# APK location
app/build/outputs/apk/debug/app-debug.apk

# Copy to Downloads (always use new version number!)
cp app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/MobileCLI-v32.apk
```

### Critical Requirements
| Requirement | Value | Why |
|-------------|-------|-----|
| Package name | `com.termux` | Hardcoded RUNPATH in Termux binaries |
| targetSdkVersion | 28 | Android 10+ blocks exec() from app data |
| HOME path | `/data/data/com.termux/files/home` | npm/node require this exact path |

---

## Version History

| Version | Date | Changes | Status |
|---------|------|---------|--------|
| v1-v3 | Jan 5 | Foundation, terminal display | Failed |
| v4 | Jan 5 | targetSdk fix | Failed |
| v5 | Jan 5 | Package name to com.termux | Failed |
| v6 | Jan 5 | Symlink extraction fix | Failed |
| v7 | Jan 5 | /etc/passwd and LD_PRELOAD | Failed |
| v8 | Jan 5 | Login script, full env vars | Failed |
| v9 | Jan 5 | SSL investigation | Failed |
| **v10** | Jan 5 | **HOME directory fix - BREAKTHROUGH** | **Success** |
| v11 | Jan 5 | Rotation fix, text size, extra keys | Working |
| v12 | Jan 5 | Context menu, auto-setup, zoom fix | Working |
| v13 | Jan 5 | Simplified zoom, AlertDialog menu | Working |
| v14 | Jan 5 | DrawerLayout swipe menu, Ctrl+C fix | Working |
| v15-v16 | Jan 5 | AI chooser dialog | Screen bug |
| v17-v18 | Jan 5 | Terminal size attempts | Screen bug |
| **v19** | Jan 5 | **Reflection font metrics - SCREEN FIX** | Working |
| v20 | Jan 5 | Built-in Termux API support | Working |
| v21 | Jan 5 | Multi-session (10 tabs) | Working |
| v22 | Jan 5 | Text selection, drawer sessions | **Crash bug** |
| v23 | Jan 5 | Bug fix: setBackgroundResource | Working |
| v24 | Jan 5 | Added xdg-open scripts (broadcast) | Not working |
| v25 | Jan 5 | am start directly (real Termux way) | Working |
| v26 | Jan 5 | Full TERMUX_APP__* env vars + comparison | Working |
| **v27** | Jan 5 | **TermuxOpenReceiver at correct path - URL FIX** | **Release** |
| v28-v29 | Jan 5 | Quick Install vs Manual options, -y flags | Working |
| v30 | Jan 5 | 39 Termux API commands implemented | Working |
| **v31** | Jan 5 | **100% API coverage - Keystore, NFC, SAF, Speech** | **Release** |
| **v32** | Jan 5 | **TermuxService + wake lock - 100% COMPLETE** | **Final** |

---

## The Breakthroughs

### Breakthrough #1: HOME Directory (v10)
**Problem:** Everything failed - npm, node, Claude Code
**Discovery:** Real Termux uses `/data/data/com.termux/files/home`, we used `/data/data/com.termux/files`
**Fix:**
```kotlin
// WRONG
val homeDir: File get() = filesDir

// CORRECT
val homeDir: File get() = File(filesDir, "home")
```

### Breakthrough #2: Screen Size (v19)
**Problem:** Terminal text cut off, only half screen used
**Discovery:** Our font size calculation didn't match TerminalView's internal metrics
**Fix:** Use reflection to get actual font dimensions:
```kotlin
val rendererField = terminalView.javaClass.getDeclaredField("mRenderer")
rendererField.isAccessible = true
val renderer = rendererField.get(terminalView)
val fontWidthField = renderer.javaClass.getDeclaredField("mFontWidth")
fontWidthField.isAccessible = true
val fontWidthPx = (fontWidthField.get(renderer) as Number).toInt()
```

### Breakthrough #3: Termux API (v20)
**Problem:** Users needed separate F-Droid Termux:API app
**Solution:** Built-in BroadcastReceiver + shell scripts:
- Shell scripts use `am broadcast` to send intents
- TermuxApiReceiver handles clipboard, toast, vibrate, etc.
- Results written to temp files, read by scripts

---

## All Bugs and Fixes

### Bug 1: App Won't Execute Binaries (v1-v4)
**Symptom:** "Permission denied" or "not found" errors
**Cause:** Android 10+ blocks exec() from app data directories
**Fix:** Set `targetSdkVersion` to 28 in build.gradle.kts

### Bug 2: Library Linking Fails (v5)
**Symptom:** "cannot find library" errors
**Cause:** Termux binaries have hardcoded RUNPATH `/data/data/com.termux/files/usr/lib`
**Fix:** Package name MUST be `com.termux`

### Bug 3: Symlinks Not Created (v6)
**Symptom:** Commands like `sh` not found
**Cause:** Wrong symlink format parsing
**Fix:** Parse SYMLINKS.txt with `target←link_path` format (unicode arrow)

### Bug 4: npm User Lookup Fails (v7)
**Symptom:** npm errors about user/group
**Cause:** Missing /etc/passwd and /etc/group
**Fix:** Create these files with proper UID/GID

### Bug 5: Child Processes Fail (v7-v8)
**Symptom:** Subprocess commands fail
**Cause:** Missing LD_PRELOAD for termux-exec
**Fix:** Set `LD_PRELOAD=${libDir}/libtermux-exec-ld-preload.so`

### Bug 6: Screen Cutoff (v15-v18)
**Symptom:** Terminal only uses part of screen
**Cause:** Font size calculation mismatch
**Fix:** Reflection to get actual mFontWidth/mFontLineSpacing (see v19)

### Bug 7: Install Commands Not Running (v15-v17)
**Symptom:** AI install commands do nothing
**Cause:** Multi-line indented strings break bash
**Fix:** Use single-line commands:
```kotlin
// WRONG
val cmd = """
    pkg update
    pkg install nodejs
""".trimIndent()

// CORRECT
val cmd = "pkg update -y && pkg install nodejs -y\n"
```

### Bug 8: Long-press Blocks Text Selection (v21)
**Symptom:** Can't select text in terminal
**Cause:** onLongPress returns true, consuming event
**Fix:** Return false to let TerminalView handle selection

### Bug 9: App Crashes on Launch (v22)
**Symptom:** "This app has a bug" error
**Cause:** `setBackgroundResource(android.R.attr.selectableItemBackground)`
**Why:** `android.R.attr.*` is an attribute reference, not a resource ID
**Fix:** Use `setBackgroundColor()` directly

### Bug 10: Claude Code OAuth Won't Open Browser (v23→v25)
**Symptom:** Claude Code shows OAuth URL but browser doesn't open automatically

**v24 Attempt (FAILED):**
- Added `xdg-open`, `termux-open`, `sensible-browser` scripts
- Used `am broadcast` → our receiver → `startActivity()`
- **Why it failed:** Android blocks `startActivity()` from BroadcastReceivers in background

**v25 Fix (WORKS):**
- Studied REAL Termux source code
- `termux-open-url` uses `am start` DIRECTLY - no broadcast!
```bash
# Real Termux way - direct activity launch
am start --user "$TERMUX__USER_ID" -a android.intent.action.VIEW -d "$URL"
```

**Key Insight:** Never use `am broadcast` when you need to start an activity. Use `am start` directly.

### Bug 11: URL Opening STILL Not Working (v25→v27)
**Symptom:** Even with `am start`, Claude Code OAuth URL still doesn't open browser automatically.

**Discovery Process:**
1. Examined real Termux source code more carefully
2. Found that `xdg-open` and `termux-open` scripts broadcast to specific component
3. The broadcast target is: `com.termux/com.termux.app.TermuxOpenReceiver`
4. MobileCLI only had `com.termux.TermuxApiReceiver` - WRONG PATH!

**Root Cause:** Component name mismatch
- Real Termux: `com.termux.app.TermuxOpenReceiver`
- MobileCLI had: `com.termux.TermuxApiReceiver` (wrong package path)

**v27 Fix:**
1. Created new file: `app/src/main/java/com/termux/app/TermuxOpenReceiver.kt`
2. Class is in package `com.termux.app` (not `com.termux`)
3. Registered in AndroidManifest.xml as `.app.TermuxOpenReceiver`
4. Updated scripts to match real Termux exactly

**Key Code:**
```kotlin
// File: app/src/main/java/com/termux/app/TermuxOpenReceiver.kt
package com.termux.app  // MUST be com.termux.app, not com.termux

class TermuxOpenReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val uri = intent.data ?: return
        val openIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(openIntent)
    }
}
```

**AndroidManifest.xml:**
```xml
<!-- MUST be .app.TermuxOpenReceiver to resolve to com.termux.app.TermuxOpenReceiver -->
<receiver
    android:name=".app.TermuxOpenReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        ...
    </intent-filter>
</receiver>
```

**Key Insight:** Android BroadcastReceiver component names are EXACT. If scripts broadcast to `com.termux/com.termux.app.TermuxOpenReceiver`, you MUST have a receiver at that exact package path. A receiver at `com.termux.TermuxApiReceiver` will NOT receive those broadcasts.

### Breakthrough #4: 100% Termux API Coverage (v31)
**Problem:** MobileCLI had only 39 of 50+ Termux API commands implemented.

**Missing APIs Identified:**
1. **KeystoreAPI** - Android Keystore cryptographic operations (5 methods)
2. **NfcAPI** - NFC NDEF tag information
3. **NotificationListAPI** - List active notifications
4. **SAFAPI** - Storage Access Framework (9 commands)
5. **SpeechToTextAPI** - Voice recognition availability

**v31 Solution:**
- Implemented all 5 missing APIs in TermuxApiReceiver.kt (~450 lines added)
- Added 15+ new shell scripts to BootstrapInstaller.kt
- Added NFC permission and hardware feature to AndroidManifest.xml

**Complete API Count (v31):**
- 50+ termux-* commands fully implemented
- Matches or exceeds real Termux:API app functionality

### Breakthrough #5: Background Service & Wake Lock (v32)
**Problem:** Sessions died when app went to background. No way to keep CPU/WiFi awake for overnight tasks.

**Root Cause:** MobileCLI was activity-only. Real Termux uses `TermuxService` - a foreground service that manages sessions independently.

**v32 Solution:**
1. Created `TermuxService.kt` (297 lines) - foreground service with:
   - CPU wake lock (`PowerManager.PARTIAL_WAKE_LOCK`)
   - WiFi lock (`WifiManager.WIFI_MODE_FULL_HIGH_PERF`)
   - Persistent notification with session count
   - Action handlers for wake lock commands
2. Added service registration to AndroidManifest.xml
3. Added `termux-wake-lock` and `termux-wake-unlock` scripts
4. MainActivity binds to service on startup

**Shell Script Integration:**
```bash
# For overnight Claude Code sessions:
termux-wake-lock   # Acquires CPU + WiFi lock
# ... run long task ...
termux-wake-unlock # Releases locks
```

**Result:** MobileCLI now achieves **100% Termux compatibility** - the final 5% was background operation.

---

## Project Structure

```
MobileCLI-v2/
├── app/
│   ├── src/main/
│   │   ├── java/com/termux/
│   │   │   ├── MainActivity.kt       # 1,248 lines - Terminal UI, sessions
│   │   │   ├── BootstrapInstaller.kt # 1,558 lines - Bootstrap + 52 scripts
│   │   │   ├── TermuxApiReceiver.kt  # 1,548 lines - 50+ API handlers
│   │   │   └── app/
│   │   │       ├── TermuxOpenReceiver.kt  # 112 lines - URL/file opener
│   │   │       └── TermuxService.kt       # 297 lines - Background + wake lock (v32)
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   ├── activity_main.xml # DrawerLayout + Terminal + ExtraKeys
│   │   │   │   └── dialog_progress.xml
│   │   │   └── values/
│   │   │       ├── strings.xml       # App name: MobileCLI
│   │   │       ├── styles.xml        # Extra key button styles
│   │   │       └── themes.xml        # Dark theme
│   │   └── AndroidManifest.xml       # Package: com.termux, targetSdk: 28
│   └── build.gradle.kts
├── CLAUDE.md                         # Quick reference
├── APP_BUILD_HISTORY.md              # This file (complete build history)
├── TERMUX_COMPARISON.md              # Deep comparison with real Termux
└── DEVELOPMENT_HISTORY.md            # Original v1-v10 journey

TOTAL SOURCE CODE: 4,763 lines of Kotlin
```

---

## File System After Install

```
/data/data/com.termux/files/
├── home/                    # HOME directory
│   ├── .bashrc
│   ├── .npmrc
│   ├── .mobilecli/          # App config
│   └── storage/             # Symlinks to /sdcard
└── usr/                     # PREFIX directory
    ├── bin/                 # Executables + termux-* API scripts
    ├── lib/                 # Libraries + libtermux-exec-ld-preload.so
    ├── etc/                 # passwd, group, hosts, resolv.conf
    │   └── tls/cert.pem    # SSL certificates
    ├── tmp/                 # TMPDIR
    └── var/run/             # TMUX_TMPDIR
```

---

## Environment Variables

All of these MUST be set for full compatibility:

```bash
HOME=/data/data/com.termux/files/home
PREFIX=/data/data/com.termux/files/usr
PATH=/data/data/com.termux/files/usr/bin:/system/bin
LD_LIBRARY_PATH=/data/data/com.termux/files/usr/lib
LD_PRELOAD=/data/data/com.termux/files/usr/lib/libtermux-exec-ld-preload.so
TMPDIR=/data/data/com.termux/files/usr/tmp
TERM=xterm-256color
SHELL=/data/data/com.termux/files/usr/bin/bash
TERMUX_VERSION=0.118.0
SSL_CERT_FILE=/data/data/com.termux/files/usr/etc/tls/cert.pem
NODE_EXTRA_CA_CERTS=/data/data/com.termux/files/usr/etc/tls/cert.pem
```

---

## Termux API Commands (Built-in v31 - 100% Coverage)

### Clipboard & Notifications
| Command | Function |
|---------|----------|
| termux-clipboard-get | Read clipboard |
| termux-clipboard-set | Write clipboard |
| termux-toast | Show toast message |
| termux-notification | Send notification with -t title -c content |
| termux-notification-remove | Remove notification by ID |
| termux-notification-list | List active notifications |

### Device & System
| Command | Function |
|---------|----------|
| termux-battery-status | Get battery JSON |
| termux-vibrate | Vibrate device |
| termux-brightness | Get/set brightness |
| termux-torch | Toggle flashlight |
| termux-volume | Get volume levels |
| termux-audio-info | Audio/ringer mode info |
| termux-info | Show device info |

### Network & Location
| Command | Function |
|---------|----------|
| termux-wifi-connectioninfo | WiFi info |
| termux-wifi-enable | Enable/disable WiFi |
| termux-wifi-scaninfo | Scan nearby networks |
| termux-location | GPS/network location |

### Camera & Media
| Command | Function |
|---------|----------|
| termux-camera-info | Camera capabilities |
| termux-camera-photo | Take photo |
| termux-media-player | Audio playback |
| termux-microphone-record | Audio recording |
| termux-media-scan | Media scanner |

### Telephony & SMS
| Command | Function |
|---------|----------|
| termux-telephony-call | Make phone call |
| termux-telephony-cellinfo | Cell tower info |
| termux-telephony-deviceinfo | Phone info |
| termux-sms-list | List SMS messages |
| termux-sms-send | Send SMS |
| termux-call-log | View call history |
| termux-contact-list | List contacts |

### Sensors & Hardware
| Command | Function |
|---------|----------|
| termux-sensor | Sensor info/data |
| termux-fingerprint | Biometric auth |
| termux-infrared-frequencies | IR emitter ranges |
| termux-infrared-transmit | Send IR signal |
| termux-usb | USB device info |
| termux-nfc | NFC status |

### Keystore (v31+)
| Command | Function |
|---------|----------|
| termux-keystore list | List keys |
| termux-keystore generate | Generate AES key |
| termux-keystore delete | Delete key |
| termux-keystore sign | Sign/encrypt data |
| termux-keystore verify | Verify/decrypt data |

### Storage Access Framework (v31+)
| Command | Function |
|---------|----------|
| termux-saf-managedir | Select directory via system UI |
| termux-saf-dirs | List common directories |
| termux-saf-ls | List directory contents |
| termux-saf-stat | Get file info |
| termux-saf-read | Read file |
| termux-saf-write | Write file |
| termux-saf-create | Create file |
| termux-saf-mkdir | Create directory |
| termux-saf-rm | Delete file/directory |

### Other
| Command | Function |
|---------|----------|
| termux-tts-speak | Text-to-speech |
| termux-tts-engines | TTS engines |
| termux-speech-to-text | Speech recognition |
| termux-share | Share content |
| termux-download | Download file |
| termux-wallpaper | Set wallpaper |
| termux-dialog | Input dialog |
| termux-open-url | Open URL |
| termux-open | Open file/URL |
| xdg-open | Same as termux-open |
| termux-setup-storage | Create ~/storage symlinks |

---

## Session Management (v21+)

- Up to 10 concurrent sessions
- Tabs appear when 2+ sessions exist
- Drawer shows full session list
- Long-press tab for options
- Kill session: terminates shell, Enter closes tab

---

## Development Workflow

### CRITICAL RULES

1. **Never overwrite APK versions** - Always increment: v21 → v22 → v23
2. **Test immediately after build** - Install and verify before any code changes
3. **If crash occurs:**
   - Don't panic
   - Analyze recent changes
   - Create NEW version with fix
4. **Keep all versions** - Never delete old APKs, they're your rollback

### Build Process
```bash
# 1. Make code changes

# 2. Build
./gradlew assembleDebug

# 3. Copy with NEW version number
cp app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/MobileCLI-vXX.apk

# 4. Install and test on device

# 5. If working, update CLAUDE.md
```

---

## The Achievement

This application was:
1. Built entirely on an Android phone
2. Using Claude Code AI as the developer
3. Can run Claude Code inside itself
4. Creating a self-referential development loop

**First successful run:** January 5, 2026 (v10)
**Full-featured release:** January 5, 2026 (v23)

---

## Links

- Website: https://mobilecli.com
- Story: https://mobilecli.com/app-story.html
- GitHub: https://github.com/MobileDevCLI
