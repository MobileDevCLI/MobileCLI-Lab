# MobileCLI Studio - Master Plan

**Created:** January 7, 2026
**Author:** Claude Code (Opus 4.5) running in Termux
**Project:** Transform MobileCLI Games into MobileCLI Studio
**Status:** PLANNING PHASE

---

## Executive Summary

MobileCLI Studio is a comprehensive development platform for Android that provides specialized "rooms" for different types of developers. Unlike traditional IDEs that require a desktop computer, MobileCLI Studio runs entirely on an Android phone with Claude Code AI integration.

**Core Innovation:** Build Android APKs directly on an Android phone - no computer needed.

---

## Room Priority Order

| Priority | Room | Target Users | Core Value |
|----------|------|--------------|------------|
| 1 | Mobile Apps | Android developers | Build APKs on phone |
| 2 | Web Dev | Frontend developers | Live preview, HTML/CSS/JS |
| 3 | Terminal | Power users | Raw Claude Code access |
| 4 | Data Science | Data analysts | Python, notebooks, charts |
| 5 | API/Backend | Backend developers | REST testing, DB viewer |
| 6 | AI/ML | ML engineers | Model training, prompts |

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                      MobileCLI Studio                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                   HOME SCREEN                            │   │
│  │                                                          │   │
│  │   ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐        │   │
│  │   │ Mobile │  │  Web   │  │Terminal│  │  Data  │        │   │
│  │   │  Apps  │  │  Dev   │  │        │  │Science │        │   │
│  │   └────────┘  └────────┘  └────────┘  └────────┘        │   │
│  │                                                          │   │
│  │   ┌────────┐  ┌────────┐                                │   │
│  │   │  API   │  │ AI/ML  │                                │   │
│  │   │Backend │  │        │                                │   │
│  │   └────────┘  └────────┘                                │   │
│  │                                                          │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│                              ▼                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                   ROOM (Selected)                        │   │
│  │                                                          │   │
│  │  ┌─────────┬─────────────────────────────┬──────────┐   │   │
│  │  │  Left   │         Center              │  Right   │   │   │
│  │  │  Panel  │        VIEWPORT             │  Panel   │   │   │
│  │  │         │                             │          │   │   │
│  │  │         │                             │          │   │   │
│  │  └─────────┴─────────────────────────────┴──────────┘   │   │
│  │  ┌─────────────────────────────────────────────────────┐│   │
│  │  │              Bottom Panel (Console/Output)          ││   │
│  │  └─────────────────────────────────────────────────────┘│   │
│  │                                                          │   │
│  │  [Claude Terminal - Always accessible via slide/button]  │   │
│  │                                                          │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│                     CORE SERVICES                               │
│  - TermuxService (background processing)                        │
│  - BootstrapInstaller (environment setup)                       │
│  - ProjectManager (file/project handling)                       │
│  - SettingsManager (preferences, API keys)                      │
│  - BuildSystem (Gradle, compilation)                            │
└─────────────────────────────────────────────────────────────────┘
```

---

## UI Design Principles (Based on Godot/Unity)

### Standard Layout Pattern

All rooms follow the same base layout for consistency:

```
┌──────────────────────────────────────────────────────────────────┐
│ [≡] Room Name    [Tab1] [Tab2] [Tab3]         [Settings] [Claude]│
├────────────┬─────────────────────────────────┬───────────────────┤
│            │                                 │                   │
│   LEFT     │          VIEWPORT               │      RIGHT        │
│   PANEL    │         (Main Work Area)        │      PANEL        │
│            │                                 │                   │
│  - Tree    │                                 │   - Properties    │
│  - Files   │                                 │   - Inspector     │
│  - Assets  │                                 │   - Details       │
│            │                                 │                   │
├────────────┴─────────────────────────────────┴───────────────────┤
│  [Console] [Output] [Problems] [Terminal]                        │
│  > Build started...                                              │
│  > Compiling resources...                                        │
└──────────────────────────────────────────────────────────────────┘
```

### Design Requirements

1. **Resizable Panels** - Drag edges to resize
2. **Collapsible Panels** - Double-tap header to collapse
3. **Tab Support** - Multiple items in same panel area
4. **Dark Theme** - Professional dark colors
5. **Touch Optimized** - 48dp minimum touch targets
6. **Keyboard Support** - External keyboard shortcuts

### Color Palette

| Element | Color | Hex |
|---------|-------|-----|
| Background | Dark Blue-Gray | #1a1a2e |
| Panel Background | Darker | #16213e |
| Surface | Dark | #0f0f1a |
| Primary | Coral/Red | #e94560 |
| Secondary | Blue | #2196F3 |
| Success | Green | #4CAF50 |
| Warning | Orange | #FF9800 |
| Text Primary | White | #ffffff |
| Text Secondary | Gray | #888888 |
| Borders | Dark Gray | #333333 |

---

## Room 1: Mobile Apps (APK Builder)

### Purpose
Build, edit, and compile Android APKs directly on the phone.

### Reference
- Godot Android Editor
- Android Studio (simplified)

### Layout

```
┌──────────────────────────────────────────────────────────────────┐
│ [≡] Mobile Apps   [Project] [Build] [Run]    [⚙] [Claude]       │
├────────────────┬──────────────────────────┬──────────────────────┤
│ PROJECT FILES  │      EDITOR              │ COMPONENT TREE       │
│                │                          │                      │
│ ▼ MyApp/       │  activity_main.xml       │ ▼ LinearLayout       │
│   ▼ app/       │  ┌──────────────────┐    │   ├─ TextView        │
│     ▼ src/     │  │                  │    │   ├─ EditText        │
│       main/    │  │   [Preview]      │    │   └─ Button          │
│       ▼ java/  │  │                  │    │                      │
│       ▼ res/   │  └──────────────────┘    ├──────────────────────┤
│     build.     │                          │ PROPERTIES           │
│   ▼ gradle/    │  [Code] [Design] [Split] │                      │
│                │                          │ id: @+id/myButton    │
│                │                          │ text: "Click Me"     │
│                │                          │ onClick: handleClick │
├────────────────┴──────────────────────────┴──────────────────────┤
│ [Console] [Build Output] [Logcat] [Problems]                     │
│ > ./gradlew assembleDebug                                        │
│ > BUILD SUCCESSFUL in 45s                                        │
│ > APK: app/build/outputs/apk/debug/app-debug.apk                │
└──────────────────────────────────────────────────────────────────┘
```

### Buttons & Functions

#### Top Bar
| Button | Function | Implementation |
|--------|----------|----------------|
| ≡ Menu | Open navigation drawer | DrawerLayout.openDrawer() |
| Project | Switch between open projects | ProjectManager.showProjectPicker() |
| Build | Build APK | Bash: ./gradlew assembleDebug |
| Run | Install & run on device | Bash: adb install + am start |
| ⚙ Settings | Open settings | SettingsActivity |
| Claude | Toggle Claude terminal panel | SlidingPaneLayout.open() |

#### Left Panel (Project Files)
| Feature | Function | Implementation |
|---------|----------|----------------|
| File tree | Navigate project | RecyclerView + TreeAdapter |
| Create file | New file dialog | AlertDialog + File.createNewFile() |
| Delete file | Delete confirmation | AlertDialog + File.delete() |
| Rename | Rename dialog | AlertDialog + File.renameTo() |
| Refresh | Reload file tree | TreeAdapter.refresh() |

#### Center Panel (Editor)
| Feature | Function | Implementation |
|---------|----------|----------------|
| Code view | Syntax highlighted editor | WebView + Monaco/CodeMirror |
| Design view | Visual layout preview | WebView + layout rendering |
| Split view | Code + Preview side by side | LinearLayout horizontal |
| Find/Replace | Search in file | Editor search API |
| Undo/Redo | Edit history | Editor history API |
| Save | Save file | File.writeText() |

#### Right Panel (Component Tree + Properties)
| Feature | Function | Implementation |
|---------|----------|----------------|
| Component tree | XML element hierarchy | RecyclerView + TreeAdapter |
| Select component | Highlight in preview | Click listener |
| Properties list | Editable attributes | RecyclerView + PropertyAdapter |
| Edit property | Change value | EditText + XML update |

#### Bottom Panel (Console)
| Tab | Function | Implementation |
|-----|----------|----------------|
| Console | General output | TextView append |
| Build Output | Gradle output | Process output stream |
| Logcat | Android logs | Bash: adb logcat |
| Problems | Errors/warnings | Parsed build output |

### Keyboard Shortcuts
| Shortcut | Action |
|----------|--------|
| Ctrl+S | Save file |
| Ctrl+B | Build project |
| Ctrl+R | Run project |
| Ctrl+F | Find in file |
| Ctrl+Z | Undo |
| Ctrl+Shift+Z | Redo |
| Ctrl+N | New file |
| Ctrl+O | Open file |

---

## Room 2: Web Dev

### Purpose
Build websites with live preview, HTML/CSS/JS editing.

### Reference
- VS Code web view
- CodePen
- Brackets

### Layout

```
┌──────────────────────────────────────────────────────────────────┐
│ [≡] Web Dev   [HTML] [CSS] [JS] [Preview]   [⚙] [Claude]        │
├────────────────┬──────────────────────────┬──────────────────────┤
│ FILES          │      EDITOR              │ LIVE PREVIEW         │
│                │                          │                      │
│ ▼ mysite/      │  index.html              │ ┌──────────────────┐ │
│   index.html   │  <!DOCTYPE html>         │ │                  │ │
│   style.css    │  <html>                  │ │  Hello World!    │ │
│   script.js    │    <head>                │ │                  │ │
│   ▼ assets/    │      <title>My Site      │ │  [Button]        │ │
│     logo.png   │    </head>               │ │                  │ │
│                │    <body>                │ └──────────────────┘ │
│                │      <h1>Hello World     │                      │
│                │                          │ [Mobile] [Tablet]    │
│                │                          │ [Desktop] [Custom]   │
├────────────────┴──────────────────────────┴──────────────────────┤
│ [Console] [Network] [Elements] [Server]                          │
│ > Server running on http://localhost:8080                        │
│ > GET /index.html 200 OK                                         │
└──────────────────────────────────────────────────────────────────┘
```

### Buttons & Functions

#### Top Bar
| Button | Function | Implementation |
|--------|----------|----------------|
| HTML/CSS/JS tabs | Switch editor file type | ViewPager or tab layout |
| Preview | Toggle live preview | WebView reload |
| Server | Start/stop local server | Bash: npx http-server |

#### Features
| Feature | Function | Implementation |
|---------|----------|----------------|
| Live preview | Auto-reload on save | WebView + file watcher |
| Device preview | Different screen sizes | WebView width/height |
| Console | Browser console output | WebView console API |
| Network | HTTP request log | WebView network API |
| Emmet support | HTML abbreviations | Editor plugin |

---

## Room 3: Terminal

### Purpose
Raw Claude Code terminal access for power users.

### Layout

```
┌──────────────────────────────────────────────────────────────────┐
│ [≡] Terminal   [+New] [Sessions▼]             [⚙] [Fullscreen]  │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  user@mobilecli:~$ claude                                        │
│                                                                  │
│  ╭─────────────────────────────────────────────────────────╮    │
│  │ Claude Code                                              │    │
│  │ Anthropic's AI-powered development assistant             │    │
│  │                                                          │    │
│  │ What would you like to build today?                      │    │
│  │                                                          │    │
│  ╰─────────────────────────────────────────────────────────╯    │
│                                                                  │
│                                                                  │
│                                                                  │
├──────────────────────────────────────────────────────────────────┤
│ [Ctrl] [Alt] [Tab] [↑] [↓] [←] [→] [Esc] [≡]                    │
└──────────────────────────────────────────────────────────────────┘
```

### Features
| Feature | Function | Implementation |
|---------|----------|----------------|
| Multiple sessions | Tab support | TerminalSession[] |
| Session naming | Custom tab names | Session.setTitle() |
| Copy/Paste | Clipboard access | TerminalView text selection |
| Extra keys row | Special keys | ExtraKeysView |
| Fullscreen | Hide UI | Immersive mode |
| Font size | Pinch zoom | ScaleGestureDetector |

---

## Room 4: Data Science

### Purpose
Python development, notebooks, data visualization.

### Layout

```
┌──────────────────────────────────────────────────────────────────┐
│ [≡] Data Science   [Notebook] [Script] [Data]  [⚙] [Claude]     │
├────────────────┬──────────────────────────┬──────────────────────┤
│ FILES          │      NOTEBOOK            │ VARIABLES            │
│                │                          │                      │
│ ▼ project/     │ [1] import pandas as pd  │ df: DataFrame        │
│   data.csv     │     import numpy as np   │   shape: (1000, 5)   │
│   analysis.py  │ [Run] [2.3s]            │   columns: [a,b,c]   │
│   notebook.py  │                          │                      │
│                │ [2] df = pd.read_csv()   │ x: ndarray           │
│                │     df.head()            │   shape: (100,)      │
│                │ [Run] [0.5s]            │                      │
│                │ ┌──────────────────────┐ │ y: ndarray           │
│                │ │   a    b    c        │ │   shape: (100,)      │
│                │ │ 0 1.2  3.4  5.6      │ │                      │
│                │ │ 1 2.3  4.5  6.7      │ │                      │
│                │ └──────────────────────┘ │                      │
├────────────────┴──────────────────────────┴──────────────────────┤
│ [Console] [Plots] [Data Preview]                                 │
│ >>> df.describe()                                                │
└──────────────────────────────────────────────────────────────────┘
```

### Features
| Feature | Function | Implementation |
|---------|----------|----------------|
| Notebook cells | Code + output blocks | RecyclerView + WebView |
| Run cell | Execute Python | Bash: python3 |
| Variables panel | Inspect data | Parse Python output |
| Data preview | Table view | RecyclerView table |
| Plots | matplotlib/plotly | WebView charts |
| CSV viewer | Spreadsheet view | RecyclerView grid |

---

## Room 5: API/Backend

### Purpose
REST API testing, database viewing, server management.

### Layout

```
┌──────────────────────────────────────────────────────────────────┐
│ [≡] API Backend   [Requests] [Database] [Server] [⚙] [Claude]   │
├────────────────┬──────────────────────────┬──────────────────────┤
│ COLLECTIONS    │      REQUEST             │ RESPONSE             │
│                │                          │                      │
│ ▼ My API       │ GET ▼ [https://api...]  │ Status: 200 OK       │
│   Get Users    │ [Send]                   │ Time: 234ms          │
│   Create User  │                          │ Size: 1.2 KB         │
│   Update User  │ Headers  Body  Auth      │                      │
│   Delete User  │ ──────────────────       │ {                    │
│                │ Content-Type: json       │   "users": [         │
│ ▼ Saved        │ Authorization: Bearer    │     {                │
│   Login        │                          │       "id": 1,       │
│   Get Profile  │ Body (JSON):             │       "name": "John" │
│                │ {                        │     }                │
│                │   "name": "John"         │   ]                  │
│                │ }                        │ }                    │
├────────────────┴──────────────────────────┴──────────────────────┤
│ [History] [Environment Variables] [Console]                      │
│ POST /users - 201 Created - 156ms                                │
└──────────────────────────────────────────────────────────────────┘
```

### Features
| Feature | Function | Implementation |
|---------|----------|----------------|
| HTTP methods | GET/POST/PUT/DELETE | Dropdown + curl |
| Headers | Request headers | Key-value editor |
| Body | JSON/form data | Monaco editor |
| Auth | Bearer/Basic/API key | Auth type picker |
| Response | Pretty JSON | JSON formatter |
| History | Past requests | SQLite storage |
| Collections | Saved requests | File-based |
| Environment | Variables | Key-value store |

---

## Room 6: AI/ML

### Purpose
AI model interaction, prompt engineering, training.

### Layout

```
┌──────────────────────────────────────────────────────────────────┐
│ [≡] AI/ML   [Chat] [Prompts] [Models] [Training] [⚙] [Claude]   │
├────────────────┬──────────────────────────┬──────────────────────┤
│ PROMPTS        │      CHAT                │ MODEL INFO           │
│                │                          │                      │
│ ▼ Saved        │ System:                  │ Model: Claude 3.5    │
│   Code Review  │ You are a helpful...     │ Tokens: 2,453        │
│   Summarize    │                          │ Cost: $0.02          │
│   Translate    │ ──────────────────       │                      │
│   Debug        │ User: Help me debug      │ ────────────────     │
│                │ this function...         │ Parameters:          │
│ ▼ Templates    │                          │ Temperature: 0.7     │
│   System       │ Assistant: I'd be        │ Max tokens: 4096     │
│   User         │ happy to help. Let me    │ Top P: 1.0           │
│                │ analyze the code...      │                      │
│                │                          │ API Key: ****        │
│                │ [Type message...]  [↑]   │ [Configure]          │
├────────────────┴──────────────────────────┴──────────────────────┤
│ [Token Count] [Export Chat] [Clear]                              │
│ Total: 2,453 tokens | Estimated cost: $0.02                      │
└──────────────────────────────────────────────────────────────────┘
```

### Features
| Feature | Function | Implementation |
|---------|----------|----------------|
| Chat interface | Conversation view | RecyclerView messages |
| Prompt templates | Saved prompts | File storage |
| Model selection | Claude/GPT/etc | API switching |
| Token counter | Usage tracking | Tokenizer |
| Cost estimation | API cost | Token * rate |
| Export | Save conversation | File export |
| API key management | Multiple keys | Secure storage |

---

## Settings Architecture

### Global Settings
| Setting | Type | Default | Storage |
|---------|------|---------|---------|
| Theme | enum | dark | SharedPreferences |
| Font size | int | 14 | SharedPreferences |
| Auto-save | bool | true | SharedPreferences |
| Claude API key | string | null | EncryptedPreferences |
| GitHub token | string | null | EncryptedPreferences |
| Default room | enum | terminal | SharedPreferences |

### Per-Room Settings
Each room has its own settings stored in:
`~/.mobilecli-studio/settings/{room_name}.json`

---

## File Structure

```
MobileCLI-Studio/
├── app/
│   ├── src/main/
│   │   ├── java/com/termux/
│   │   │   ├── MainActivity.kt              # Entry point
│   │   │   ├── HomeActivity.kt              # Room launcher
│   │   │   ├── BootstrapInstaller.kt        # Environment setup
│   │   │   ├── TermuxApiReceiver.kt         # API commands
│   │   │   ├── TermuxService.kt             # Background service
│   │   │   │
│   │   │   ├── core/
│   │   │   │   ├── ProjectManager.kt        # Project handling
│   │   │   │   ├── SettingsManager.kt       # Settings
│   │   │   │   ├── BuildSystem.kt           # Compilation
│   │   │   │   ├── FileWatcher.kt           # File changes
│   │   │   │   └── ThemeManager.kt          # UI theming
│   │   │   │
│   │   │   ├── rooms/
│   │   │   │   ├── BaseRoomActivity.kt      # Common room logic
│   │   │   │   ├── MobileAppsRoom.kt        # APK builder
│   │   │   │   ├── WebDevRoom.kt            # Web development
│   │   │   │   ├── TerminalRoom.kt          # Terminal
│   │   │   │   ├── DataScienceRoom.kt       # Python/data
│   │   │   │   ├── ApiBackendRoom.kt        # API testing
│   │   │   │   └── AiMlRoom.kt              # AI/ML
│   │   │   │
│   │   │   ├── ui/
│   │   │   │   ├── PanelLayout.kt           # Resizable panels
│   │   │   │   ├── TreeView.kt              # File tree
│   │   │   │   ├── TabLayout.kt             # Tab bar
│   │   │   │   ├── CodeEditor.kt            # WebView editor
│   │   │   │   ├── PropertyEditor.kt        # Key-value editor
│   │   │   │   └── ConsoleView.kt           # Output panel
│   │   │   │
│   │   │   └── adapters/
│   │   │       ├── FileTreeAdapter.kt
│   │   │       ├── PropertyAdapter.kt
│   │   │       └── ConsoleAdapter.kt
│   │   │
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   ├── activity_main.xml        # Existing
│   │   │   │   ├── activity_home.xml        # Room launcher
│   │   │   │   ├── room_base.xml            # Base room layout
│   │   │   │   ├── room_mobile_apps.xml
│   │   │   │   ├── room_web_dev.xml
│   │   │   │   ├── room_terminal.xml
│   │   │   │   ├── room_data_science.xml
│   │   │   │   ├── room_api_backend.xml
│   │   │   │   ├── room_ai_ml.xml
│   │   │   │   ├── panel_file_tree.xml
│   │   │   │   ├── panel_properties.xml
│   │   │   │   └── panel_console.xml
│   │   │   │
│   │   │   └── values/
│   │   │       ├── strings.xml
│   │   │       ├── colors.xml
│   │   │       ├── dimens.xml
│   │   │       └── themes.xml
│   │   │
│   │   └── AndroidManifest.xml
│   │
│   └── build.gradle.kts
│
├── STUDIO_MASTER_PLAN.md                    # This file
├── CLAUDE.md                                # AI instructions
├── ROOM_SPECIFICATIONS.md                   # Detailed room specs
└── UI_COMPONENTS.md                         # Component library
```

---

## Implementation Phases

### Phase 1: Foundation (Current)
- [x] MobileCLI base working
- [x] Terminal functional
- [x] Claude Code integration
- [x] Self-rebuild capability
- [ ] Create STUDIO_MASTER_PLAN.md (this file)
- [ ] Audit Godot UI patterns
- [ ] Design component library

### Phase 2: Home Screen
- [ ] Create activity_home.xml
- [ ] Room selection grid
- [ ] Recent projects list
- [ ] Settings access
- [ ] Claude quick access

### Phase 3: Room Base
- [ ] Create BaseRoomActivity.kt
- [ ] Resizable panel system
- [ ] Tab system
- [ ] Console panel
- [ ] Claude slide-out

### Phase 4: Room 1 - Mobile Apps
- [ ] Project file tree
- [ ] Code editor (WebView + Monaco)
- [ ] Layout preview
- [ ] Component tree
- [ ] Properties panel
- [ ] Build system integration
- [ ] Logcat viewer

### Phase 5: Room 2 - Web Dev
- [ ] File tree
- [ ] HTML/CSS/JS editor
- [ ] Live preview
- [ ] Device size switcher
- [ ] Local server

### Phase 6: Room 3 - Terminal
- [ ] Fullscreen terminal
- [ ] Multi-session tabs
- [ ] Extra keys
- [ ] Settings

### Phase 7: Remaining Rooms
- [ ] Data Science
- [ ] API/Backend
- [ ] AI/ML

### Phase 8: Polish
- [ ] All buttons functional
- [ ] Keyboard shortcuts
- [ ] Error handling
- [ ] Performance optimization
- [ ] Documentation

---

## Next Steps

1. **Deep audit Godot Android app** - Understand every UI pattern
2. **Create UI component library** - Reusable panels, trees, editors
3. **Build home screen** - Room selection
4. **Build base room** - Common layout for all rooms
5. **Build Room 1 (Mobile Apps)** - Full APK builder
6. **Iterate through remaining rooms**

---

## Notes

- All rooms share the same Claude terminal integration
- Projects are stored in ~/projects/{room}/{project_name}/
- Settings are per-room but some are global
- Every button must have a function - no dead UI
- Touch targets minimum 48dp
- Follow Material Design 3 guidelines
- Support external keyboard shortcuts

---

**Document Status:** DRAFT - In Progress
**Last Updated:** January 7, 2026
