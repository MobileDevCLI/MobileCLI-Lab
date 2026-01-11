# MobileCLI Claude Sync Protocol

**Purpose:** Enable two Claude instances to work on MobileCLI simultaneously without duplication.

**Date:** January 7, 2026
**Build Claude:** MobileCLI-Games-v2 (Studio Edition v3.1.0)
**Test Claude:** MobileCLI v80 (1.8.1) - AI Device Control

---

## TEST CLAUDE DISCOVERIES (v76-v80) - January 7, 2026

### Major Conceptual Breakthrough: MobileCLI OS

Test Claude defined MobileCLI as **"the world's first AI-native mobile operating system"**:
- **1,226+ Linux binaries** - Full computing environment
- **84 device control APIs** - Complete phone hardware access
- **AI integration** - Claude Code with full system access
- **Development tools** - Build apps on the device itself
- **Internet bridge** - AI can access web via browser + clipboard loop

### AI Device Control - Browser Interaction Loop

Test Claude discovered a genuine AI-human-device interaction loop:

```
┌─────────────────────────────────────────────────────────────────┐
│                    AI-HUMAN-DEVICE LOOP                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   CLAUDE CODE (in Termux)                                      │
│         │                                                       │
│         ├──► termux-open-url ──► Browser opens exact page      │
│         │                                    │                  │
│         │                                    ▼                  │
│         │                           User interacts with page    │
│         │                           User copies needed data     │
│         │                                    │                  │
│         │                                    ▼                  │
│         ◄──── termux-clipboard-get ◄──── Android Clipboard     │
│         │                                                       │
│         ▼                                                       │
│   Claude receives data and continues autonomously              │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**Commands:**
```bash
termux-open-url "https://example.com/page"  # Opens browser
termux-clipboard-get                          # Reads what user copied
termux-clipboard-set "data"                   # Writes to clipboard
```

### 84 Device Control APIs

| Category | APIs |
|----------|------|
| **Browser/Clipboard** | open-url, open, clipboard-get/set, share |
| **Communication** | sms-send/inbox, telephony-call, contact-list |
| **Camera/Media** | camera-photo, microphone-record, media-player, tts-speak |
| **Sensors/Hardware** | location, sensor, fingerprint, torch, vibrate, infrared |
| **System/UI** | notification, dialog, toast, battery-status, wifi-connectioninfo |

### 21 Invention Ideas (Built with MobileCLI)

Test Claude documented 21 real-world applications possible with AI device control:
1. AI Personal Security Guard (GPS + SMS + Call)
2. Voice-Controlled Home Automation (IR blaster)
3. AI Document Scanner & Organizer (Camera + OCR)
4. Elderly Care Monitor (Sensors + Fall detection)
5. AI Meeting Transcriber (Mic + Speech-to-text)
6. And 16 more...

See: `MobileCLI-v2/21_INVENTIONS.md`

---

## THE TWO-CLAUDE SETUP

### Build Claude (This Instance)
- **Location:** Termux on build phone
- **Project:** `/data/data/com.termux/files/home/MobileCLI-Games-v2/`
- **Focus:** GUI, Studio rooms, user experience
- **Output:** APK builds, documentation

### Test Claude (Other Instance)
- **Location:** Inside MobileCLI app on test phone
- **Project:** Self-modifying the running app
- **Focus:** Internal fixes, workarounds, dev tools
- **Output:** Bug fixes, environment setup, documentation

---

## BREAKTHROUGH: Node.js Spawn Workaround (January 7, 2026)

**Problem:** Claude Code task tracking interfered with long-running processes.

**Solution:** Use Node.js spawn with `detached: true` and `unref()`:

```javascript
const { spawn } = require('child_process');

// This lets process run independently of Claude Code task tracking
const child = spawn('curl', ['-O', fileUrl], {
    detached: true,
    stdio: 'ignore'
});

child.unref();  // Process runs independently
```

**Why it works:**
- `detached: true` - Process becomes session leader
- `unref()` - Parent doesn't wait for child
- Claude Code task tracking no longer blocks the process

---

## DEV TOOLS INSTALLED (Test Phone - January 7, 2026)

| Tool | Version | Status |
|------|---------|--------|
| Python | 3.12.12 | ✅ Installed |
| Git | 2.52.0 | ✅ Installed |
| OpenJDK | 21.0.9 | ✅ Installed |
| Gradle | 9.2.0 | ✅ Installed |
| aapt/aapt2 | 33 | ✅ Installed |
| d8/dx | 33 | ✅ Installed |
| apksigner | 33 | ✅ Installed |
| Android SDK | Configured | ✅ Ready |

**Self-build confirmed:** Test phone can clone and build MobileCLI from source!

---

## STUDIO EDITION ARCHITECTURE (Build Phone - January 7, 2026)

### Version: 3.1.0

### New File Structure:
```
app/src/main/java/com/termux/
├── HomeActivity.kt              # Room selector (6 rooms)
├── studio/
│   ├── BaseRoomActivity.kt      # Foundation for all rooms
│   └── rooms/
│       ├── MobileAppsRoomActivity.kt   # APK builder
│       ├── WebDevRoomActivity.kt       # HTML/CSS/JS IDE
│       └── TerminalRoomActivity.kt     # Terminal launcher
└── [existing files unchanged]

app/src/main/res/
├── layout/
│   ├── activity_home.xml           # Home screen with 6 cards
│   ├── activity_base_room.xml      # Room layout with panels
│   ├── view_code_editor.xml        # Code editor component
│   ├── dialog_create_project.xml   # New project wizard
│   ├── item_file_tree.xml          # File tree item
│   └── drawer_header_room.xml      # Room drawer header
├── menu/
│   ├── room_menu.xml               # Room toolbar menu
│   └── drawer_room_menu.xml        # Room drawer menu
└── drawable/
    ├── ic_mobile_apps.xml
    ├── ic_web_dev.xml
    ├── ic_terminal.xml
    ├── ic_data_science.xml
    ├── ic_api_backend.xml
    ├── ic_ai_ml.xml
    ├── ic_play.xml, ic_save.xml, ic_folder.xml, etc.
    └── [button backgrounds]
```

### Navigation Flow:
```
SetupWizardActivity
        ↓ (after setup)
HomeActivity (Room Selector)
        ↓ (tap room card)
    ┌───┼───┬───┬───┬───┐
    ↓   ↓   ↓   ↓   ↓   ↓
Mobile Web Term Data API  AI
Apps   Dev  inal Science  ML
```

### Room Features Built:

**Room 1: Mobile Apps (MobileAppsRoomActivity)**
- Create new Android projects from templates
- File tree browser (RecyclerView with depth)
- Code editor (EditText monospace)
- Gradle build in background thread
- Console output panel
- Templates: Empty, Basic, Bottom Nav, Terminal

**Room 2: Web Dev (WebDevRoomActivity)**
- Create websites from template
- HTML/CSS/JS editor
- Live preview in WebView
- Auto-refresh on save

**Room 3: Terminal (TerminalRoomActivity)**
- Quick launch to full terminal
- Preset commands (claude, pkg install, etc.)
- Links to MainActivity

---

## SYNC PROTOCOL

### For Test Claude to Use This Documentation:

1. **Read these files:**
   - `CLAUDE_SYNC.md` (this file)
   - `STUDIO_MASTER_PLAN.md` (full architecture)
   - `UI_COMPONENTS.md` (component specs)
   - `ROOM_SPECIFICATIONS.md` (every button)
   - `CLAUDE.md` (project instructions)

2. **Don't rebuild what's built:**
   - HomeActivity.kt - DONE
   - BaseRoomActivity.kt - DONE
   - MobileAppsRoomActivity.kt - DONE
   - WebDevRoomActivity.kt - DONE
   - TerminalRoomActivity.kt - DONE
   - All layouts and drawables - DONE

3. **What still needs work:**
   - DataScienceRoomActivity (Python notebooks)
   - ApiBackendRoomActivity (REST testing)
   - AiMlRoomActivity (AI chat interface)

### For Build Claude to Get Test Claude's Fixes:

Test Claude should document:
- Node.js spawn workaround details
- Dev tools installation commands
- Any environment variable fixes
- Any BootstrapInstaller.kt changes

---

## COPY-PASTE READY FILES

### To add BaseRoomActivity to any MobileCLI version:

**Step 1:** Create directory
```bash
mkdir -p app/src/main/java/com/termux/studio/rooms
```

**Step 2:** Copy these files from this project:
- `studio/BaseRoomActivity.kt`
- `studio/rooms/MobileAppsRoomActivity.kt`
- `studio/rooms/WebDevRoomActivity.kt`
- `studio/rooms/TerminalRoomActivity.kt`

**Step 3:** Copy layouts:
- `res/layout/activity_home.xml`
- `res/layout/activity_base_room.xml`
- `res/layout/view_code_editor.xml`
- `res/layout/dialog_create_project.xml`
- `res/layout/item_file_tree.xml`

**Step 4:** Add to AndroidManifest.xml (after HomeActivity):
```xml
<activity android:name=".studio.rooms.MobileAppsRoomActivity" ... />
<activity android:name=".studio.rooms.WebDevRoomActivity" ... />
<activity android:name=".studio.rooms.TerminalRoomActivity" ... />
```

**Step 5:** Update HomeActivity imports and openRoom() function.

---

## VERSION HISTORY (Combined)

| Version | Date | Who | Changes |
|---------|------|-----|---------|
| 1.0.0 | Jan 6 | Build | First stable release |
| 2.3.0 | Jan 7 | Build | Game engine room |
| 2.4.0 | Jan 7 | Build | Env vars fix |
| 3.0.0 | Jan 7 | Build | Studio rename, home screen |
| 3.1.0 | Jan 7 | Build | All core rooms (Mobile, Web, Terminal) |
| v76 (1.8.1) | Jan 7 | Test | AI Device Control discovery |
| v77-v79 | Jan 7 | Test | MobileCLI OS conceptualization |
| v80 | Jan 7 | Test | Dev tools, spawn workaround, 21 Inventions |

### Test Claude New Documentation Files:
- `MOBILECLI_OS.md` - MobileCLI as AI-native OS
- `AI_DEVICE_CONTROL.md` - Browser interaction loop
- `21_INVENTIONS.md` - 21 app ideas with device control
- `WEBSITE_STORY_UPDATE.md` - Updated narrative

---

## GOLDEN RULES FOR BOTH CLAUDES

1. **Never delete documentation** - Only add
2. **Never hallucinate** - Search/read before assuming
3. **Document everything** - Future sessions need context
4. **Version everything** - Never reuse version numbers
5. **Test immediately** - Build and install after changes
6. **Sync via files** - These MD files are the bridge

---

## CONTACT BETWEEN CLAUDES

Since both Claudes can't directly communicate, sync via:

1. **This file (CLAUDE_SYNC.md)** - Main sync document
2. **Git commits** - Push/pull changes
3. **User relay** - User tells each Claude what the other did
4. **Shared CLAUDE.md** - Both read project instructions

The user is the bridge. When one Claude makes progress, user updates the other.

---

## NEXT STEPS

### Build Claude (This Instance):
- [ ] Build remaining rooms (Data Science, API, AI/ML)
- [ ] Polish Mobile Apps room (better file tree, syntax highlighting)
- [ ] Add project templates
- [ ] Integrate Test Claude's 84 device APIs into Studio rooms
- [ ] Consider AI/ML room with device control APIs
- [ ] GitHub push

### Test Claude (COMPLETED v80):
- [x] Document spawn workaround in detail - DONE
- [x] Document all env var fixes - DONE (in MOBILECLI_OS.md)
- [x] Created MobileCLI OS documentation - DONE
- [x] Created AI Device Control documentation - DONE
- [x] Created 21 Inventions document - DONE
- [x] Dev tools installed: Python 3.12.12, Git 2.52.0, OpenJDK 21.0.9, Gradle 9.2.0
- [ ] Test self-rebuild with new Studio version (pending v3.1.0 test)

---

## INTEGRATION OPPORTUNITIES

Based on Test Claude's discoveries, the AI/ML Room should include:

1. **Device Control Panel** - GUI for 84 termux APIs
2. **Invention Builder** - Templates for the 21 invention ideas
3. **Browser Bridge** - Visual workflow for URL+clipboard loop
4. **Sensor Dashboard** - Real-time sensor data visualization

The Terminal Room already has access to all these - the AI/ML room can provide a GUI wrapper.

---

*Last updated: January 7, 2026 by Build Claude (merged Test Claude v80 discoveries)*
