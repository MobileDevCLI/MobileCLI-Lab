# MobileCLI Feature Plans - v33 and Beyond

**Created:** January 6, 2026
**Author:** Claude Opus 4.5

---

## Overview

This document outlines 10 major feature additions planned for MobileCLI versions 33-36, transforming it from a Termux-compatible terminal into a complete mobile development environment.

---

## v33 Features (Target: Q1 2026)

### Feature 1: MobileCLI Widget

**Purpose:** Home screen widgets for quick command execution (like Termux:Widget)

**Implementation:**

```kotlin
// File: app/src/main/java/com/termux/widget/WidgetProvider.kt
package com.termux.widget

class WidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Load scripts from ~/.termux/widgets/
        // Create RemoteViews for each widget
    }
}

// File: app/src/main/java/com/termux/widget/WidgetConfigureActivity.kt
class WidgetConfigureActivity : AppCompatActivity() {
    // Let user select which script to run
    // Show list of available scripts
}
```

**Widget Types:**
1. **Single Command Widget** - One tap runs one script
2. **Multi-Command Widget** - Expandable list of shortcuts
3. **Session Widget** - Launch directly into specific session

**User Configuration:**
```
~/.termux/widgets/
├── update.sh      # "pkg update && pkg upgrade -y"
├── claude.sh      # "claude"
├── server.sh      # "python -m http.server 8000"
└── sync.sh        # "git pull && npm install"
```

**Required Changes:**
- Add `app/src/main/res/xml/widget_provider.xml`
- Add widget layouts in `res/layout/widget_*.xml`
- Add receiver entry in AndroidManifest.xml

---

### Feature 2: Boot Service

**Purpose:** Run scripts automatically on device boot (like Termux:Boot)

**Implementation:**

```kotlin
// File: app/src/main/java/com/termux/boot/BootReceiver.kt
package com.termux.boot

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Start boot script execution service
            context.startService(Intent(context, BootScriptService::class.java))
        }
    }
}

// File: app/src/main/java/com/termux/boot/BootScriptService.kt
class BootScriptService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val bootDir = File(homeDir, ".termux/boot")
        if (bootDir.exists()) {
            bootDir.listFiles()
                ?.filter { it.isFile && it.canExecute() }
                ?.sortedBy { it.name }
                ?.forEach { script ->
                    executeScript(script)
                }
        }
        return START_NOT_STICKY
    }
}
```

**Boot Script Location:**
```
~/.termux/boot/
├── 01-start-ssh.sh     # Start SSH server
├── 02-wake-lock.sh     # Acquire wake lock
├── 03-start-sync.sh    # Start background sync
└── 99-notification.sh  # Show boot complete notification
```

**Required Changes:**
- Already have `RECEIVE_BOOT_COMPLETED` permission
- Add BootReceiver to AndroidManifest.xml
- Execute scripts in alphanumeric order

---

## v34 Features (Target: Q2 2026)

### Feature 3: Floating Terminal

**Purpose:** Picture-in-picture style floating terminal window (like Termux:Float)

**Implementation:**

```kotlin
// File: app/src/main/java/com/termux/float/FloatService.kt
package com.termux.float

class FloatService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var floatView: View
    private lateinit var terminalView: TerminalView

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createFloatingWindow()
    }

    private fun createFloatingWindow() {
        val params = WindowManager.LayoutParams(
            400, 300,  // Initial size
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        floatView = LayoutInflater.from(this)
            .inflate(R.layout.float_terminal, null)

        setupDragListener()
        setupResizeCorners()

        windowManager.addView(floatView, params)
    }
}
```

**Features:**
- Draggable window
- Resize handles in corners
- Minimize to floating bubble
- Transparency slider
- Multiple float windows support

**Required Permissions:**
- `SYSTEM_ALERT_WINDOW` (needs user grant in Settings)

---

### Feature 4: Tasker Integration

**Purpose:** Execute MobileCLI commands from Tasker/Automate (like Termux:Tasker)

**Implementation:**

```kotlin
// File: app/src/main/java/com/termux/tasker/FireReceiver.kt
package com.termux.tasker

class FireReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_FIRE = "com.termux.tasker.FIRE"
        const val EXTRA_SCRIPT = "com.termux.tasker.extra.SCRIPT"
        const val EXTRA_ARGS = "com.termux.tasker.extra.ARGS"
        const val EXTRA_WAIT = "com.termux.tasker.extra.WAIT"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return

        val script = intent.getStringExtra(EXTRA_SCRIPT) ?: return
        val args = intent.getStringExtra(EXTRA_ARGS) ?: ""
        val wait = intent.getBooleanExtra(EXTRA_WAIT, false)

        // Security: Only allow scripts in ~/.termux/tasker/
        val scriptFile = File(homeDir, ".termux/tasker/$script")
        if (!scriptFile.exists() || !scriptFile.canExecute()) {
            setResultCode(Activity.RESULT_CANCELED)
            return
        }

        executeScript(scriptFile, args, wait) { exitCode, output ->
            resultCode = exitCode
            resultData = output
        }
    }
}
```

**Tasker Actions Available:**
1. **Run Script** - Execute `~/.termux/tasker/script_name.sh`
2. **Run Command** - Execute any command (with permission)
3. **Get Output** - Return stdout to Tasker variable

**Security Model:**
- Scripts must be in `~/.termux/tasker/` directory
- First execution requires explicit user permission
- Configurable allow/deny list

---

## v35 Features (Target: Q3 2026)

### Feature 5: Built-in Code Editor

**Purpose:** Syntax-highlighted code editor integrated with terminal

**Implementation:**

```kotlin
// File: app/src/main/java/com/termux/editor/EditorActivity.kt
package com.termux.editor

class EditorActivity : AppCompatActivity() {
    private lateinit var codeView: CodeView
    private lateinit var tabLayout: TabLayout
    private lateinit var terminalView: TerminalView

    private val openFiles = mutableListOf<EditorFile>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        setupCodeView()
        setupTabs()
        setupIntegratedTerminal()
        setupSyntaxHighlighting()
    }

    private fun setupSyntaxHighlighting() {
        val highlighter = SyntaxHighlighter()
        highlighter.registerLanguage("kotlin", KotlinSyntax())
        highlighter.registerLanguage("python", PythonSyntax())
        highlighter.registerLanguage("javascript", JavaScriptSyntax())
        // ... 100+ languages
    }
}
```

**Features:**
- 100+ language syntax highlighting
- Line numbers with clickable gutter
- Find and replace (regex support)
- Multiple file tabs
- Integrated terminal at bottom (collapsible)
- Run current file button
- Git status indicators
- Minimap sidebar

**Editor Command:**
```bash
edit file.py       # Opens in editor
edit -r .          # Opens directory picker
edit --new         # New file
```

---

### Feature 6: Split Terminal Mode

**Purpose:** View multiple terminals simultaneously

**Implementation:**

```kotlin
// File: app/src/main/java/com/termux/split/SplitManager.kt
package com.termux.split

class SplitManager(private val container: ViewGroup) {
    enum class SplitMode { NONE, HORIZONTAL, VERTICAL, QUAD }

    private var currentMode = SplitMode.NONE
    private val terminals = mutableListOf<TerminalPane>()

    fun splitHorizontal() {
        currentMode = SplitMode.HORIZONTAL
        // Top/bottom split
        rebuildLayout()
    }

    fun splitVertical() {
        currentMode = SplitMode.VERTICAL
        // Left/right split
        rebuildLayout()
    }

    fun quadSplit() {
        currentMode = SplitMode.QUAD
        // 2x2 grid
        rebuildLayout()
    }
}
```

**Gestures:**
- Two-finger swipe left/right: Cycle focus
- Long-press divider: Adjust split ratio
- Three-finger pinch: Add split
- Three-finger expand: Remove split

---

### Feature 7: SSH Server GUI

**Purpose:** Easy-to-use SSH server management

**Implementation:**

```kotlin
// File: app/src/main/java/com/termux/ssh/SshServerManager.kt
package com.termux.ssh

class SshServerManager(private val context: Context) {
    private var serverProcess: Process? = null

    fun start(port: Int = 8022): Boolean {
        // Check sshd is installed
        if (!File("$PREFIX/bin/sshd").exists()) {
            installOpenSSH()
        }

        // Generate host keys if needed
        generateHostKeysIfNeeded()

        // Start sshd
        serverProcess = Runtime.getRuntime().exec("sshd -p $port")
        return true
    }

    fun getConnectionInfo(): ConnectionInfo {
        return ConnectionInfo(
            ip = getLocalIpAddress(),
            port = currentPort,
            user = System.getProperty("user.name") ?: "u0_a*",
            publicKey = getPublicKey()
        )
    }

    fun generateQRCode(): Bitmap {
        // Generate QR code with ssh://user@ip:port
    }
}
```

**Commands:**
```bash
termux-ssh-server start          # Start on default port
termux-ssh-server start -p 2222  # Custom port
termux-ssh-server stop           # Stop server
termux-ssh-server status         # Show status and connection info
termux-ssh-server qr             # Display QR code for easy connection
termux-ssh-server auth add KEY   # Add authorized key
```

---

## v36 Features (Target: Q4 2026)

### Feature 8: Scheduled Commands

**Purpose:** cron-like scheduled task execution

**Implementation:**

```kotlin
// File: app/src/main/java/com/termux/scheduler/SchedulerService.kt
package com.termux.scheduler

class SchedulerService : JobService() {
    companion object {
        private const val CRONTAB_PATH = ".termux/crontab"
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        val crontab = File(homeDir, CRONTAB_PATH)
        if (!crontab.exists()) return false

        val now = Calendar.getInstance()
        parseCrontab(crontab).filter { entry ->
            entry.matches(now)
        }.forEach { entry ->
            executeJob(entry)
        }

        return false
    }
}

// Crontab entry format
data class CrontabEntry(
    val minute: String,      // 0-59
    val hour: String,        // 0-23
    val dayOfMonth: String,  // 1-31
    val month: String,       // 1-12
    val dayOfWeek: String,   // 0-6 (Sunday=0)
    val command: String
)
```

**Crontab Format:**
```
# ~/.termux/crontab
# MIN HOUR DAY MONTH DOW COMMAND

# Every day at 8am - backup
0 8 * * * ~/scripts/backup.sh

# Every 15 minutes - sync
*/15 * * * * ~/scripts/sync.sh

# Monday at 9am - weekly report
0 9 * * 1 ~/scripts/report.sh
```

**GUI:**
- List all scheduled jobs
- Add/edit/remove jobs
- View execution history
- Enable/disable individual jobs
- One-time scheduled tasks

---

### Feature 9: Auto-Backup & Restore

**Purpose:** Automatic backup of terminal configuration and data

**Implementation:**

```kotlin
// File: app/src/main/java/com/termux/backup/BackupManager.kt
package com.termux.backup

class BackupManager(private val context: Context) {

    data class BackupManifest(
        val version: Int,
        val timestamp: Long,
        val items: List<BackupItem>
    )

    fun backup(destination: File): BackupResult {
        val items = mutableListOf<BackupItem>()

        // 1. Shell configuration
        items += backupFile(".bashrc")
        items += backupFile(".bash_profile")
        items += backupFile(".profile")
        items += backupFile(".zshrc")

        // 2. Termux configuration
        items += backupDirectory(".termux")

        // 3. SSH keys
        items += backupDirectory(".ssh")

        // 4. Git configuration
        items += backupFile(".gitconfig")

        // 5. Package list
        items += backupPackageList()

        // 6. npm global packages
        items += backupNpmGlobals()

        // 7. Custom scripts
        items += backupDirectory("bin")
        items += backupDirectory("scripts")

        // Create backup archive
        return createBackupArchive(destination, items)
    }

    fun restore(source: File): RestoreResult {
        val manifest = readManifest(source)

        manifest.items.forEach { item ->
            restoreItem(item)
        }

        // Reinstall packages
        reinstallPackages(manifest)

        return RestoreResult.SUCCESS
    }
}
```

**Backup Destinations:**
1. Local storage (`/sdcard/MobileCLI/backups/`)
2. Google Drive (via SAF)
3. Custom WebDAV server
4. Manual export/import

**Scheduled Backups:**
```
# Settings
backup_enabled: true
backup_frequency: daily  # daily, weekly, monthly
backup_time: "03:00"     # 3 AM
backup_destination: local
backup_retention: 7      # Keep last 7 backups
```

---

### Feature 10: Terminal Themes & Customization

**Purpose:** Visual customization beyond basic settings

**Implementation:**

```kotlin
// File: app/src/main/java/com/termux/theme/ThemeManager.kt
package com.termux.theme

class ThemeManager(private val context: Context) {

    // Built-in color schemes
    val colorSchemes = listOf(
        ColorScheme("Monokai", monokai),
        ColorScheme("Dracula", dracula),
        ColorScheme("Nord", nord),
        ColorScheme("Solarized Dark", solarizedDark),
        ColorScheme("Solarized Light", solarizedLight),
        ColorScheme("Gruvbox", gruvbox),
        ColorScheme("One Dark", oneDark),
        ColorScheme("Tokyo Night", tokyoNight),
        // ... 50+ schemes
    )

    // Built-in fonts
    val fonts = listOf(
        Font("Fira Code", "FiraCode-Regular.ttf"),
        Font("JetBrains Mono", "JetBrainsMono-Regular.ttf"),
        Font("Hack", "Hack-Regular.ttf"),
        Font("Source Code Pro", "SourceCodePro-Regular.ttf"),
        Font("Cascadia Code", "CascadiaCode-Regular.ttf"),
        // ... 20+ fonts
    )

    // Customization options
    var cursorStyle: CursorStyle = CursorStyle.BLOCK
    var cursorBlink: Boolean = true
    var bellStyle: BellStyle = BellStyle.VIBRATE
    var extraKeysStyle: ExtraKeysStyle = ExtraKeysStyle.ARROWS
}
```

**Theme Files Location:**
```
~/.termux/
├── colors.properties     # Color scheme
├── font.ttf             # Custom font file
├── termux.properties    # Settings
└── themes/              # Custom themes
    ├── my-theme.json
    └── imported-theme.json
```

**colors.properties Format:**
```properties
foreground=#f8f8f2
background=#282a36
cursor=#f8f8f8

color0=#21222c
color1=#ff5555
color2=#50fa7b
color3=#f1fa8c
color4=#bd93f9
color5=#ff79c6
color6=#8be9fd
color7=#f8f8f2
# ... color8-15
```

**Live Preview:**
- Real-time preview when changing colors
- Font preview with sample text
- Export/import themes as JSON
- Theme gallery (downloadable themes)

---

## Implementation Priority

| Priority | Feature | Version | Effort |
|----------|---------|---------|--------|
| 1 | Widget Support | v33 | Medium |
| 2 | Boot Service | v33 | Low |
| 3 | Theme Customization | v36 | Low |
| 4 | Auto-Backup | v36 | Medium |
| 5 | Scheduled Commands | v36 | Medium |
| 6 | Tasker Integration | v34 | Medium |
| 7 | Split Terminal | v35 | High |
| 8 | SSH Server GUI | v35 | Medium |
| 9 | Floating Terminal | v34 | High |
| 10 | Code Editor | v35 | Very High |

---

## Summary

These 10 features will transform MobileCLI from a Termux-compatible terminal into a complete mobile development platform, rivaling desktop development environments while maintaining the portability and convenience of a mobile device.

---

*Document created: January 6, 2026*
*Author: Claude Opus 4.5*
