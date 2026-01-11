# MobileCLI vs Termux Full Compatibility Audit

**Created:** January 6, 2026
**Last Updated:** January 6, 2026
**Purpose:** Track every Termux feature and whether MobileCLI implements it

---

## CRITICAL: URL Opening Issue (BLOCKING)

**Problem Discovered:** Claude Code OAuth doesn't open browser in MobileCLI but works in real Termux.

**Root Cause Analysis:**

Real Termux's `termux-open-url` does NOT just run `am start`. It uses a sophisticated system:

1. **Socket-based communication** - Termux:API uses abstract Unix sockets
2. **Activity context for intents** - URL opening goes through Activity, not shell
3. **TermuxOpenReceiver** handles broadcasts AND has nested ContentProvider
4. **File URI handling** with proper MIME type detection

MobileCLI's current approach (direct `am start` from shell) fails because:
- Android restricts background activity launches
- Shell process doesn't have proper activity context
- No ContentProvider for file sharing

---

## Audit Status Legend

| Status | Meaning |
|--------|---------|
| ✅ DONE | Fully implemented, tested working |
| ⚠️ PARTIAL | Implemented but incomplete or untested |
| ❌ MISSING | Not implemented |
| 🔴 CRITICAL | Blocking real usage (must fix) |
| 🟡 IMPORTANT | Should have for parity |
| 🟢 NICE-TO-HAVE | Can add later |

---

## 1. URL/File Opening

| Feature | Termux | MobileCLI | Status | Priority |
|---------|--------|-----------|--------|----------|
| TermuxOpenReceiver class | At `com.termux.app.TermuxOpenReceiver` | ✅ Have it | ⚠️ PARTIAL | 🔴 |
| ACTION_SEND handling | Shares files/URLs | ❌ Not handling | ❌ MISSING | 🔴 |
| ACTION_VIEW handling | Opens files/URLs | ⚠️ Basic only | ⚠️ PARTIAL | 🔴 |
| File URI resolution | `UriUtils.getUriFilePathWithFragment()` | ❌ Missing | ❌ MISSING | 🟡 |
| MIME type detection | Extension + fallback to octet-stream | ❌ Missing | ❌ MISSING | 🟡 |
| Nested ContentProvider | Handles file access requests | ❌ Missing | ❌ MISSING | 🔴 |
| FLAG_GRANT_READ_URI_PERMISSION | Set on intents | ❌ Missing | ❌ MISSING | 🔴 |
| Path validation | Validates against Termux dirs | ❌ Missing | ❌ MISSING | 🟡 |

### What Termux Does That We Don't:

```java
// TermuxOpenReceiver.java - Real implementation
// 1. For non-file URIs, delegates to external app immediately
// 2. For files:
//    - Resolves path with fragment support
//    - Validates file exists and is readable
//    - Detects MIME type from extension or intent
//    - Sets proper flags including FLAG_GRANT_READ_URI_PERMISSION
//    - For ACTION_SEND: uses EXTRA_STREAM
//    - For ACTION_VIEW: uses setDataAndType()
```

### FIX NEEDED:
MobileCLI needs to handle URL opening through Java/Activity context, not shell commands. The receiver needs to:
1. Accept broadcasts from shell scripts
2. Start activities with proper context and flags
3. Handle both URLs and files properly

---

## 2. Session Management (TermuxService)

| Feature | Termux | MobileCLI | Status | Priority |
|---------|--------|-----------|--------|----------|
| TermuxService class | Full foreground service | ✅ Have it (v32) | ⚠️ PARTIAL | 🔴 |
| Foreground sessions (mTermuxSessions) | Synchronized ArrayList | ✅ Have sessions | ✅ DONE | - |
| Background tasks (mTermuxTasks) | AppShell instances | ❌ Missing | ❌ MISSING | 🟡 |
| Pending plugin commands | Command queue | ❌ Missing | ❌ MISSING | 🟢 |
| Wake lock (PowerManager) | PARTIAL_WAKE_LOCK | ✅ Have it (v32) | ✅ DONE | - |
| WiFi lock | WIFI_MODE_FULL_HIGH_PERF | ✅ Have it (v32) | ✅ DONE | - |
| Notification with session count | Shows "1 session" etc | ⚠️ Basic only | ⚠️ PARTIAL | 🟡 |
| Notification priority change | HIGH when wake lock held | ❌ Missing | ❌ MISSING | 🟢 |
| START_NOT_STICKY | Returns from onStartCommand | ❓ Need to check | ⚠️ PARTIAL | 🟡 |
| Dual client implementation | Activity + Service clients | ❌ Missing | ❌ MISSING | 🟡 |
| Battery optimization request | On wake lock acquire | ❌ Missing | ❌ MISSING | 🟡 |

---

## 3. Intent/Broadcast Handling

| Intent Action | Termux | MobileCLI | Status | Priority |
|---------------|--------|-----------|--------|----------|
| `com.termux.service_stop` | Stop service | ❌ Missing | ❌ MISSING | 🟡 |
| `com.termux.service_wake_lock` | Acquire wake lock | ✅ Have it | ✅ DONE | - |
| `com.termux.service_wake_unlock` | Release wake lock | ✅ Have it | ✅ DONE | - |
| `com.termux.service_execute` | Execute command | ❌ Missing | ❌ MISSING | 🔴 |
| `com.termux.app.failsafe_session` | Start failsafe | ❌ Missing | ❌ MISSING | 🟡 |
| `com.termux.app.reload_style` | Reload terminal style | ❌ Missing | ❌ MISSING | 🟢 |
| `com.termux.app.request_storage_permissions` | Request perms | ❌ Missing | ❌ MISSING | 🟡 |
| `com.termux.app.OPENED` broadcast | Notify app opened | ❌ Missing | ❌ MISSING | 🟢 |

### RUN_COMMAND Extras (for external apps)

| Extra | Purpose | MobileCLI | Status |
|-------|---------|-----------|--------|
| RUN_COMMAND_PATH | Executable path | ❌ | ❌ MISSING |
| RUN_COMMAND_ARGUMENTS | String[] args | ❌ | ❌ MISSING |
| RUN_COMMAND_STDIN | Input data | ❌ | ❌ MISSING |
| RUN_COMMAND_WORKDIR | Working directory | ❌ | ❌ MISSING |
| RUN_COMMAND_RUNNER | APP_SHELL or TERMINAL | ❌ | ❌ MISSING |
| RUN_COMMAND_SESSION_ACTION | 0-3 behavior | ❌ | ❌ MISSING |
| RUN_COMMAND_PENDING_INTENT | Callback | ❌ | ❌ MISSING |

**Priority:** 🟡 IMPORTANT - Tasker/Automate integration depends on this

---

## 4. File Handling & Content Providers

| Feature | Termux | MobileCLI | Status | Priority |
|---------|--------|-----------|--------|----------|
| TermuxDocumentsProvider | SAF integration | ❌ Missing | ❌ MISSING | 🟡 |
| FileShareReceiverActivity | SEND intent | ❌ Missing | ❌ MISSING | 🟡 |
| FileViewReceiverActivity | VIEW intent | ❌ Missing | ❌ MISSING | 🟡 |
| Content Provider authority | `com.termux.files` | ❌ Missing | ❌ MISSING | 🔴 |
| File browsing in SAF | Exposes ~/home | ❌ Missing | ❌ MISSING | 🟡 |
| Symlink blocking in search | Security feature | ❌ Missing | ❌ MISSING | 🟢 |

---

## 5. Termux:API Communication

| Feature | Termux | MobileCLI | Status | Priority |
|---------|--------|-----------|--------|----------|
| Abstract Unix socket | `com.termux.api://listen` | ❌ Missing | ❌ MISSING | 🔴 |
| Socket-based API calls | Preferred method | ❌ Missing | ❌ MISSING | 🔴 |
| Fallback to am broadcast | When socket fails | ✅ Have it | ⚠️ PARTIAL | - |
| SO_PEERCRED verification | UID matching | ❌ Missing | ❌ MISSING | 🟡 |
| Bidirectional sockets | Input/output streams | ❌ Missing | ❌ MISSING | 🔴 |
| File descriptor passing | recvmsg() | ❌ Missing | ❌ MISSING | 🟡 |

### THIS IS WHY URL OPENING FAILS!

Real Termux:API scripts:
1. First try socket connection to `com.termux.api://listen`
2. Send command with 2-byte length header
3. App receives via socket, processes, returns via socket
4. If socket fails, falls back to `am broadcast`

MobileCLI:
1. Only has `am broadcast` receiver
2. Broadcast receiver can't reliably start activities (Android restriction)
3. No socket server for preferred communication path

---

## 6. Clipboard Handling

| Feature | Termux | MobileCLI | Status | Priority |
|---------|--------|-----------|--------|----------|
| termux-clipboard-get | Via socket/broadcast | ✅ Have it | ✅ DONE | - |
| termux-clipboard-set | Via socket/broadcast | ✅ Have it | ✅ DONE | - |
| Ctrl+Alt+V paste | Keyboard shortcut | ❌ Missing | ❌ MISSING | 🟡 |
| Extra keys PASTE | Button support | ✅ Have it | ✅ DONE | - |
| Context menu paste | Long-press option | ✅ Have it | ✅ DONE | - |

---

## 7. Notification Handling

| Feature | Termux | MobileCLI | Status | Priority |
|---------|--------|-----------|--------|----------|
| Notification channels | 5 channels | ⚠️ 1 channel | ⚠️ PARTIAL | 🟢 |
| termux-notification | Full command | ✅ Have it | ⚠️ PARTIAL | 🟡 |
| --button1/2/3 | Action buttons | ❌ Missing | ❌ MISSING | 🟡 |
| --image | Image attachment | ❌ Missing | ❌ MISSING | 🟢 |
| Direct reply | Android N+ | ❌ Missing | ❌ MISSING | 🟢 |
| Media controls | Play/pause/etc | ❌ Missing | ❌ MISSING | 🟢 |
| LED customization | Color/pattern | ❌ Missing | ❌ MISSING | 🟢 |

---

## 8. Shell Environment

| Feature | Termux | MobileCLI | Status | Priority |
|---------|--------|-----------|--------|----------|
| PREFIX | /data/data/com.termux/files/usr | ✅ Correct | ✅ DONE | - |
| HOME | /data/data/com.termux/files/home | ✅ Correct | ✅ DONE | - |
| TMPDIR | /data/data/com.termux/files/usr/tmp | ✅ Correct | ✅ DONE | - |
| PATH | /data/.../usr/bin | ✅ Correct | ✅ DONE | - |
| LD_LIBRARY_PATH | Removed on Android 7+ | ✅ Correct | ✅ DONE | - |
| LD_PRELOAD | libtermux-exec | ✅ Have it | ✅ DONE | - |
| TERMUX_VERSION | Version string | ✅ Have it | ✅ DONE | - |
| TERMUX_APP__* vars | 15+ app vars | ✅ Have them | ✅ DONE | - |
| Failsafe mode | Skip custom PATH | ❌ Missing | ❌ MISSING | 🟡 |
| ELF magic detection | 0x7F 'E' 'L' 'F' | ❌ Missing | ❌ MISSING | 🟢 |
| Shebang parsing | #! interpreter | ❌ Missing | ❌ MISSING | 🟢 |
| Interpreter remapping | /bin/* -> PREFIX | ❌ Missing | ❌ MISSING | 🟢 |

---

## 9. Terminal Emulator (Native Code)

| Feature | Termux | MobileCLI | Status | Priority |
|---------|--------|-----------|--------|----------|
| JNI createSubprocess | PTY creation | ✅ Via library | ✅ DONE | - |
| JNI setPtyWindowSize | Resize handling | ✅ Via library | ✅ DONE | - |
| JNI waitFor | Process wait | ✅ Via library | ✅ DONE | - |
| UTF-8 mode enable | tcsetattr | ✅ Via library | ✅ DONE | - |
| Flow control disable | Ctrl+S fix | ✅ Via library | ✅ DONE | - |
| FD cleanup | /proc/self/fd scan | ✅ Via library | ✅ DONE | - |
| Signal unblocking | After fork | ✅ Via library | ✅ DONE | - |

---

## 10. Extra Keys & Keyboard

| Feature | Termux | MobileCLI | Status | Priority |
|---------|--------|-----------|--------|----------|
| Default layout | ESC TAB CTRL ALT - DOWN UP | ✅ Have it | ✅ DONE | - |
| Custom JSON layout | extra-keys property | ❌ Missing | ❌ MISSING | 🟡 |
| Macro support | {macro: 'CTRL f d'} | ❌ Missing | ❌ MISSING | 🟡 |
| Popup keys | {key: 'HOME', popup: 'END'} | ❌ Missing | ❌ MISSING | 🟢 |
| KEYBOARD key | Toggle soft keyboard | ✅ Have it | ✅ DONE | - |
| DRAWER key | Open drawer | ✅ Have it | ✅ DONE | - |
| PASTE key | Paste clipboard | ✅ Have it | ✅ DONE | - |
| Hardware shortcuts | Ctrl+Alt combos | ❌ Missing | ❌ MISSING | 🟡 |
| Volume as Ctrl/Fn | volume-keys property | ❌ Missing | ❌ MISSING | 🟡 |

---

## 11. Configuration Properties

| Property | Purpose | MobileCLI | Status |
|----------|---------|-----------|--------|
| allow-external-apps | RUN_COMMAND | ❌ | ❌ MISSING |
| bell-behaviour | vibrate/beep/ignore | ❌ | ❌ MISSING |
| back-key-behaviour | back/escape | ❌ | ❌ MISSING |
| terminal-cursor-blink-rate | Blink speed | ❌ | ❌ MISSING |
| terminal-cursor-style | block/underline/bar | ❌ | ❌ MISSING |
| terminal-margin-* | Margins | ❌ | ❌ MISSING |
| terminal-transcript-rows | Scrollback | ❌ | ❌ MISSING |
| extra-keys | JSON layout | ❌ | ❌ MISSING |
| night-mode | Theme | ❌ | ❌ MISSING |
| use-fullscreen | Fullscreen | ❌ | ❌ MISSING |

**Priority:** 🟡 IMPORTANT - Power users expect these

---

## 12. Bootstrap & Storage

| Feature | Termux | MobileCLI | Status | Priority |
|---------|--------|-----------|--------|----------|
| Bootstrap from native lib | termux-bootstrap | ✅ From assets | ✅ DONE | - |
| SYMLINKS.txt handling | Relative paths | ✅ Have it | ✅ DONE | - |
| Executable permissions | 0700 mode | ✅ Have it | ✅ DONE | - |
| ~/storage/ symlinks | 7+ symlinks | ✅ Have it | ✅ DONE | - |
| Staging directory cleanup | Failed install cleanup | ⚠️ Basic | ⚠️ PARTIAL | 🟢 |

---

## 13. Termux:API Commands (44 APIs)

| Command | MobileCLI | Status | Priority |
|---------|-----------|--------|----------|
| termux-audio-info | ❌ | ❌ MISSING | 🟢 |
| termux-battery-status | ✅ | ✅ DONE | - |
| termux-brightness | ✅ | ⚠️ PARTIAL | 🟢 |
| termux-call-log | ❌ | ❌ MISSING | 🟢 |
| termux-camera-info | ✅ | ✅ DONE | - |
| termux-camera-photo | ✅ | ✅ DONE | - |
| termux-clipboard-get | ✅ | ✅ DONE | - |
| termux-clipboard-set | ✅ | ✅ DONE | - |
| termux-contact-list | ✅ | ✅ DONE | - |
| termux-dialog | ❌ | ❌ MISSING | 🟡 |
| termux-download | ❌ | ❌ MISSING | 🟡 |
| termux-fingerprint | ✅ | ✅ DONE | - |
| termux-infrared-frequencies | ✅ | ✅ DONE | - |
| termux-infrared-transmit | ✅ | ✅ DONE | - |
| termux-job-scheduler | ❌ | ❌ MISSING | 🟢 |
| termux-keystore | ✅ | ✅ DONE | - |
| termux-location | ✅ | ✅ DONE | - |
| termux-media-player | ❌ | ❌ MISSING | 🟡 |
| termux-media-scan | ✅ | ✅ DONE | - |
| termux-microphone-record | ✅ | ✅ DONE | - |
| termux-nfc | ✅ | ✅ DONE | - |
| termux-notification | ✅ | ⚠️ PARTIAL | 🟡 |
| termux-notification-list | ❌ | ❌ MISSING | 🟢 |
| termux-notification-remove | ✅ | ✅ DONE | - |
| termux-open-url | ⚠️ | 🔴 BROKEN | 🔴 |
| termux-open | ⚠️ | 🔴 BROKEN | 🔴 |
| termux-saf-* (11 ops) | ✅ | ✅ DONE | - |
| termux-sensor | ✅ | ✅ DONE | - |
| termux-share | ❌ | ❌ MISSING | 🟡 |
| termux-sms-inbox | ✅ | ✅ DONE | - |
| termux-sms-send | ✅ | ✅ DONE | - |
| termux-speech-to-text | ✅ | ✅ DONE | - |
| termux-storage-get | ❌ | ❌ MISSING | 🟡 |
| termux-telephony-* | ✅ | ✅ DONE | - |
| termux-toast | ✅ | ✅ DONE | - |
| termux-torch | ✅ | ✅ DONE | - |
| termux-tts-speak | ✅ | ✅ DONE | - |
| termux-tts-engines | ✅ | ✅ DONE | - |
| termux-usb | ✅ | ✅ DONE | - |
| termux-vibrate | ✅ | ✅ DONE | - |
| termux-volume | ✅ | ✅ DONE | - |
| termux-wallpaper | ✅ | ✅ DONE | - |
| termux-wifi-* | ✅ | ✅ DONE | - |

**Summary:** 35/44 APIs done, 9 missing, 2 broken (URL opening)

---

## 14. Plugin Apps

| Plugin | Purpose | MobileCLI | Priority |
|--------|---------|-----------|----------|
| Termux:Boot | Run scripts on boot | ✅ Have it | ✅ DONE |
| Termux:Widget | Home screen widgets | ❌ Missing | 🟢 |
| Termux:Float | Floating terminal | ❌ Missing | 🟢 |
| Termux:Tasker | Tasker integration | ❌ Missing | 🟡 |
| Termux:Styling | Theme customization | ❌ Missing | 🟢 |

---

## 15. Security

| Feature | Termux | MobileCLI | Status |
|---------|--------|-----------|--------|
| RUN_COMMAND permission | Custom dangerous perm | ❌ Missing | 🟡 |
| allow-external-apps check | Property validation | ❌ Missing | 🟡 |
| Shared user ID | All Termux apps share | N/A | - |
| Path validation | Symlink blocking | ❌ Missing | 🟡 |
| SO_PEERCRED check | Socket UID verify | ❌ Missing | 🟡 |

---

## CRITICAL FIXES NEEDED (Priority Order)

### 1. 🔴 URL Opening (BLOCKING CLAUDE CODE OAUTH)

**Problem:** `termux-open-url` and `termux-open` don't work

**Root Cause:**
- Android restricts starting activities from background/service context
- MobileCLI uses `am start` from shell which is background context
- Real Termux uses socket communication to Activity which has foreground context

**Solution Options:**

**Option A: Activity-Based URL Handler (Recommended)**
```kotlin
// Create TermuxUrlHandlerActivity
class TermuxUrlHandlerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent.getStringExtra("url")
        if (url != null) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
        finish()
    }
}

// termux-open-url script calls:
// am start -n com.termux/.TermuxUrlHandlerActivity --es url "$1"
```

**Option B: Socket Server (Like Real Termux)**
- Create LocalServerSocket listening on `com.termux.api://listen`
- Shell scripts connect to socket
- App processes commands with Activity context

### 2. 🔴 ContentProvider for File Sharing

**Problem:** Can't share files with other apps properly

**Solution:** Add `TermuxFileProvider` in AndroidManifest.xml

### 3. 🟡 RUN_COMMAND Service

**Problem:** Tasker/Automate can't run commands in Termux

**Solution:** Implement RunCommandService with proper extras handling

### 4. 🟡 termux.properties Support

**Problem:** No configuration file support

**Solution:** Parse `~/.termux/termux.properties` on startup

---

## Summary Statistics

| Category | Done | Partial | Missing | Total |
|----------|------|---------|---------|-------|
| URL/File Handling | 1 | 2 | 5 | 8 |
| Session Management | 4 | 2 | 4 | 10 |
| Intent Handling | 2 | 0 | 8 | 10 |
| File/Content Providers | 0 | 0 | 6 | 6 |
| API Communication | 1 | 1 | 4 | 6 |
| Clipboard | 4 | 0 | 1 | 5 |
| Notifications | 1 | 2 | 4 | 7 |
| Environment | 9 | 0 | 4 | 13 |
| Keyboard/Input | 5 | 0 | 5 | 10 |
| Config Properties | 0 | 0 | 10 | 10 |
| API Commands | 35 | 2 | 7 | 44 |
| **TOTAL** | **62** | **9** | **58** | **129** |

**Compatibility: ~48% complete** (not 100% as previously claimed)

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| v1.0 | Jan 6, 2026 | Initial audit after URL opening bug discovered |

---

## Action Items

1. [ ] **IMMEDIATE:** Fix URL opening (Activity-based handler)
2. [ ] Add ContentProvider for file sharing
3. [ ] Implement socket server for proper API communication
4. [ ] Add termux.properties parsing
5. [ ] Implement RUN_COMMAND service
6. [ ] Add missing notification features
7. [ ] Add keyboard customization support
8. [ ] Add missing API commands

---

*This document must be updated whenever new Termux features are discovered or implemented in MobileCLI.*
