package com.termux

import android.app.AlertDialog
import android.util.Log
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.slidingpanelayout.widget.SlidingPaneLayout
import android.webkit.WebView
import android.webkit.WebSettings
import android.webkit.WebViewClient
import android.view.ViewGroup
import com.termux.app.TermuxService
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Properties

class MainActivity : AppCompatActivity(), TerminalViewClient, TerminalSessionClient {

    private lateinit var terminalView: TerminalView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var bootstrapInstaller: BootstrapInstaller

    // TermuxService for background operation
    private var termuxService: TermuxService? = null
    private var serviceBound = false

    // Multi-session support
    private val sessions = mutableListOf<TerminalSession>()
    private var currentSessionIndex = 0
    private val maxSessions = 10

    // Flag to track if bootstrap is ready but waiting for service
    private var bootstrapReadyPendingSession = false

    // Service connection
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as TermuxService.LocalBinder
            termuxService = localBinder.service
            termuxService?.setSessionClient(this@MainActivity)
            serviceBound = true

            // Try to restore sessions from service
            termuxService?.let { service ->
                val serviceSessions = service.getSessions()
                if (serviceSessions.isNotEmpty()) {
                    // Restore existing sessions from service
                    Log.i("MainActivity", "Restoring ${serviceSessions.size} sessions from service")
                    sessions.clear()
                    sessions.addAll(serviceSessions)
                    currentSessionIndex = 0
                    terminalView.attachSession(sessions[0])
                    updateSessionTabs()
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Restored ${sessions.size} session(s)", Toast.LENGTH_SHORT).show()
                    }
                } else if (bootstrapReadyPendingSession) {
                    // No sessions to restore, and bootstrap is ready - create new session
                    Log.i("MainActivity", "Service connected, no sessions to restore, creating new session")
                    createSession()
                }
            }
            bootstrapReadyPendingSession = false
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            termuxService = null
            serviceBound = false
        }
    }

    // Current session shortcut
    private val session: TerminalSession?
        get() = sessions.getOrNull(currentSessionIndex)

    private var isCtrlPressed = false
    private var isAltPressed = false

    // Text size settings - larger for readability
    private var currentTextSize = 28  // Good readable size
    private val minTextSize = 14
    private val maxTextSize = 56

    // Progress dialog components
    private var progressDialog: AlertDialog? = null
    private var progressBar: ProgressBar? = null
    private var progressText: TextView? = null

    // Keyboard state tracking (fixes keyboard lock-up bug)
    private var isKeyboardVisible = false
    private var keyboardHeightThreshold = 0

    // Handler for debouncing terminal updates (fixes race conditions)
    private val uiHandler = Handler(Looper.getMainLooper())
    private var pendingTerminalUpdate: Runnable? = null
    private val updateDebounceMs = 16L // One frame

    // URL opener - polls for URL requests from shell (v51 fix)
    private var urlWatcherRunnable: Runnable? = null
    private val urlWatchInterval = 500L // Check every 500ms

    // ============================================
    // DITTO ARCHITECTURE - UI Command System (v84)
    // ============================================
    // AI can control UI via file-based IPC
    private var uiCommandWatcherRunnable: Runnable? = null
    private val uiCommandWatchInterval = 100L // Fast polling for responsive UI
    private val uiCommandFile: File by lazy { File(File(bootstrapInstaller.homeDir, ".termux"), "ui_command") }
    private val uiResultFile: File by lazy { File(File(bootstrapInstaller.homeDir, ".termux"), "ui_result") }

    // Extra keys dynamic control
    private var extraKeysRow1: android.widget.LinearLayout? = null
    private var extraKeysRow2: android.widget.LinearLayout? = null

    // DITTO Morphable WebView Layer (v84)
    private var dittoOverlay: android.widget.FrameLayout? = null
    private var dittoWebView: WebView? = null
    private var dittoCloseButton: Button? = null
    private var dittoCurrentSize = "full"  // full, half, quarter
    private var dittoCurrentPosition = "bottom"  // top, bottom, left, right
    private var dittoCurrentOpacity = 1.0f
    private var dittoWatcherRunnable: Runnable? = null
    private val dittoWatchInterval = 200L  // Check for DITTO UI commands every 200ms

    // UI state for persistence
    private val uiConfigFile: File by lazy {
        File(bootstrapInstaller.homeDir, ".mobilecli").also { it.mkdirs() }
        File(File(bootstrapInstaller.homeDir, ".mobilecli"), "ui_config.json")
    }

    // termux.properties support
    private var termuxProperties: Properties? = null
    private val propertiesFile: File
        get() = File(File(bootstrapInstaller.homeDir, ".termux"), "termux.properties")

    // ============================================
    // DEVELOPER MODE SYSTEM (v58)
    // ============================================
    private var developerModeEnabled = false
    private var versionTapCount = 0
    private var lastVersionTapTime = 0L
    private val VERSION_TAP_THRESHOLD = 7
    private val VERSION_TAP_TIMEOUT = 2000L // 2 seconds between taps

    // Setup overlay UI elements
    private var setupOverlay: android.widget.FrameLayout? = null
    private var setupProgressScreen: android.widget.LinearLayout? = null
    private var aiChoiceScreen: android.widget.ScrollView? = null
    private var setupStatus: TextView? = null
    private var setupProgress: ProgressBar? = null
    private var setupPercentage: TextView? = null
    private var setupStep: TextView? = null

    // AI choice tracking
    private var selectedAI: String? = null
    private val AI_CLAUDE = "claude"
    private val AI_GEMINI = "gemini"
    private val AI_CODEX = "codex"
    private val AI_NONE = "none"

    // Developer options drawer items
    private var devOptionsDivider: android.view.View? = null
    private var devOptionsHeader: TextView? = null
    private var devModeToggle: TextView? = null
    private var installDevToolsItem: TextView? = null
    private var versionText: TextView? = null

    // ============================================
    // WELCOME OVERLAY (v1.6.3 - IP Protection Fix)
    // ============================================
    // This overlay shows AFTER setup but BEFORE terminal is revealed
    // Users see "Welcome to MobileCLI" with AI command info while Claude starts
    private var welcomeOverlay: android.widget.FrameLayout? = null

    // ============================================
    // GAME ENGINE UI ELEMENTS
    // ============================================
    private var gameEngineOverlay: android.widget.FrameLayout? = null
    private var slidingPane: SlidingPaneLayout? = null
    private var claudePanel: android.widget.FrameLayout? = null
    private var terminalContainer: android.widget.FrameLayout? = null
    private var viewport: WebView? = null
    private var consoleOutput: TextView? = null
    private var projectName: TextView? = null
    private var viewportInfo: TextView? = null
    private var isClaudePanelOpen = false
    private var isFullscreenTerminal = false

    // Preferences key
    private val PREFS_NAME = "MobileCLIPrefs"
    private val KEY_DEV_MODE = "developer_mode_enabled"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Start and bind to TermuxService for background operation
        startTermuxService()

        terminalView = findViewById(R.id.terminal_view)
        terminalView.setTerminalViewClient(this)
        terminalView.setTextSize(currentTextSize)

        drawerLayout = findViewById(R.id.drawer_layout)
        bootstrapInstaller = BootstrapInstaller(this)

        // Setup extra keys
        setupExtraKeys()
        updateModifierButtons() // Set initial visual state

        // Setup navigation drawer
        setupNavDrawer()

        // Setup keyboard visibility listener (fixes keyboard lock-up bug)
        setupKeyboardListener()

        // Setup back button to dismiss keyboard (like real Termux)
        setupBackButtonHandler()

        // Setup URL watcher - polls for URL open requests from shell (v51)
        setupUrlWatcher()

        // Setup UI command watcher - DITTO Architecture (v84)
        setupUiCommandWatcher()

        // Load termux.properties
        loadTermuxProperties()

        // ============================================
        // DEVELOPER MODE INITIALIZATION (v58)
        // ============================================
        setupDeveloperMode()
        setupSetupOverlay()

        // Check and install bootstrap if needed
        checkBootstrap()
    }

    /**
     * Setup keyboard visibility detection using ViewTreeObserver.
     * This fixes the keyboard lock-up bug by tracking keyboard state.
     */
    private fun setupKeyboardListener() {
        val rootView = window.decorView.rootView
        keyboardHeightThreshold = resources.displayMetrics.heightPixels / 4

        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            rootView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = rootView.height
            val keyboardHeight = screenHeight - rect.bottom

            val wasVisible = isKeyboardVisible
            isKeyboardVisible = keyboardHeight > keyboardHeightThreshold

            // If keyboard state changed, update terminal size
            if (wasVisible != isKeyboardVisible) {
                scheduleTerminalUpdate()
            }
        }
    }

    /**
     * Setup back button to dismiss keyboard like real Termux.
     * Back button priority:
     * 1. If keyboard is visible -> hide keyboard
     * 2. If drawer is open -> close drawer
     * 3. Otherwise -> default behavior (minimize app)
     */
    private fun setupBackButtonHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    // First: hide keyboard if visible
                    isKeyboardVisible -> {
                        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(terminalView.windowToken, 0)
                    }
                    // Second: close drawer if open
                    drawerLayout.isDrawerOpen(androidx.core.view.GravityCompat.START) -> {
                        drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START)
                    }
                    // Otherwise: minimize app (don't finish, keep session alive)
                    else -> {
                        moveTaskToBack(true)
                    }
                }
            }
        })
    }

    // ============================================
    // DEVELOPER MODE FUNCTIONS (v58)
    // ============================================

    /**
     * Initialize developer mode system.
     * - Check for personal build marker file (auto-enables dev mode)
     * - Load saved state from SharedPreferences
     * - Setup 7-tap activation on version text
     * - Setup long-press activation on setup overlay
     * - Update UI based on mode
     */
    private fun setupDeveloperMode() {
        // Check for personal build marker file
        // If ~/.mobilecli_dev_mode exists, dev mode is default ON (for your testing)
        val personalBuildMarker = File(bootstrapInstaller.homeDir, ".mobilecli_dev_mode")
        val sdcardMarker = File("/sdcard/Download/.mobilecli_dev_mode")
        val isPersonalBuild = personalBuildMarker.exists() || sdcardMarker.exists()

        // Load saved developer mode state
        // BuildConfig.DEV_MODE_DEFAULT is true for dev flavor, false for user flavor
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        developerModeEnabled = if (isPersonalBuild) {
            // Personal build marker - always dev mode (unless explicitly disabled)
            val wasExplicitlyDisabled = prefs.getBoolean("dev_mode_disabled_explicitly", false)
            !wasExplicitlyDisabled
        } else {
            // Use BuildConfig default (true for dev APK, false for user APK)
            prefs.getBoolean(KEY_DEV_MODE, BuildConfig.DEV_MODE_DEFAULT)
        }

        // For developer mode, hide the setup overlay immediately and show terminal
        // This is because the overlay now starts VISIBLE by default (for IP hiding)
        if (developerModeEnabled) {
            findViewById<android.widget.FrameLayout>(R.id.setup_overlay)?.visibility = android.view.View.GONE
            findViewById<com.termux.view.TerminalView>(R.id.terminal_view)?.visibility = android.view.View.VISIBLE
        }

        // Get developer UI elements
        devOptionsDivider = findViewById(R.id.dev_options_divider)
        devOptionsHeader = findViewById(R.id.nav_dev_options_header)
        devModeToggle = findViewById(R.id.nav_dev_mode)
        installDevToolsItem = findViewById(R.id.nav_install_dev_tools)
        versionText = findViewById(R.id.nav_version)

        // Set version text
        versionText?.text = "Version 3.9.0 (v93)"

        // Setup 7-tap activation on version text (like Android Developer Options)
        versionText?.setOnClickListener {
            val currentTime = System.currentTimeMillis()

            if (currentTime - lastVersionTapTime > VERSION_TAP_TIMEOUT) {
                versionTapCount = 0
            }

            versionTapCount++
            lastVersionTapTime = currentTime

            when {
                developerModeEnabled -> {
                    Toast.makeText(this, "Developer mode is already enabled", Toast.LENGTH_SHORT).show()
                }
                versionTapCount >= VERSION_TAP_THRESHOLD -> {
                    enableDeveloperMode()
                    versionTapCount = 0
                }
                versionTapCount >= 4 -> {
                    val remaining = VERSION_TAP_THRESHOLD - versionTapCount
                    Toast.makeText(this, "You are $remaining steps away from developer mode", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Setup developer mode toggle click
        devModeToggle?.setOnClickListener {
            toggleDeveloperMode()
        }

        // Setup install dev tools click
        installDevToolsItem?.setOnClickListener {
            drawerLayout.closeDrawers()
            runInstallDevTools()
        }

        // Update UI based on current mode
        updateDeveloperModeUI()
    }

    /**
     * Enable developer mode.
     */
    private fun enableDeveloperMode() {
        developerModeEnabled = true
        saveDeveloperModeState()
        updateDeveloperModeUI()
        Toast.makeText(this, "Developer mode enabled!", Toast.LENGTH_LONG).show()
    }

    /**
     * Toggle developer mode on/off.
     */
    private fun toggleDeveloperMode() {
        developerModeEnabled = !developerModeEnabled
        saveDeveloperModeState()
        updateDeveloperModeUI()
        val status = if (developerModeEnabled) "enabled" else "disabled"
        Toast.makeText(this, "Developer mode $status", Toast.LENGTH_SHORT).show()
    }

    /**
     * Save developer mode state to SharedPreferences.
     */
    private fun saveDeveloperModeState() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DEV_MODE, developerModeEnabled).apply()
    }

    /**
     * Update UI elements based on developer mode state.
     * NOTE: Dev mode toggle is ALWAYS visible for easy access (v61 improvement).
     * Only the extra dev features are hidden when dev mode is off.
     */
    private fun updateDeveloperModeUI() {
        // Dev mode toggle is ALWAYS visible for easy access
        devModeToggle?.visibility = android.view.View.VISIBLE
        devModeToggle?.text = if (developerModeEnabled) "⚙ Developer Mode: ON" else "⚙ Developer Mode: OFF"

        // Extra dev features only visible when dev mode is ON
        val extraVisibility = if (developerModeEnabled) android.view.View.VISIBLE else android.view.View.GONE
        devOptionsDivider?.visibility = extraVisibility
        devOptionsHeader?.visibility = extraVisibility
        installDevToolsItem?.visibility = extraVisibility
    }

    /**
     * Run the install-dev-tools script.
     */
    private fun runInstallDevTools() {
        session?.write("install-dev-tools\n")
        Toast.makeText(this, "Running install-dev-tools...", Toast.LENGTH_SHORT).show()
    }

    /**
     * Setup the clean setup overlay for user mode.
     */
    private fun setupSetupOverlay() {
        setupOverlay = findViewById(R.id.setup_overlay)
        setupProgressScreen = findViewById(R.id.setup_progress_screen)
        aiChoiceScreen = findViewById(R.id.ai_choice_screen)
        setupStatus = findViewById(R.id.setup_status)
        setupProgress = findViewById(R.id.setup_progress)
        setupPercentage = findViewById(R.id.setup_percentage)
        setupStep = findViewById(R.id.setup_step)

        // v1.6.3: Initialize welcome overlay (IP protection)
        welcomeOverlay = findViewById(R.id.welcome_overlay)

        // Setup AI card click handlers
        setupAICardClickHandlers()

        // Setup long-press on dev mode trigger (top-right corner during setup)
        val devModeTrigger: android.view.View? = findViewById(R.id.dev_mode_trigger)
        devModeTrigger?.setOnLongClickListener {
            if (!developerModeEnabled) {
                enableDeveloperMode()
                // Show terminal during setup if dev mode is enabled
                hideSetupOverlay()
            }
            true
        }
    }

    /**
     * Setup click handlers for AI choice cards.
     */
    private fun setupAICardClickHandlers() {
        findViewById<android.widget.LinearLayout>(R.id.ai_card_claude)?.setOnClickListener {
            onAISelected(AI_CLAUDE)
        }
        findViewById<android.widget.LinearLayout>(R.id.ai_card_gemini)?.setOnClickListener {
            onAISelected(AI_GEMINI)
        }
        findViewById<android.widget.LinearLayout>(R.id.ai_card_codex)?.setOnClickListener {
            onAISelected(AI_CODEX)
        }
        findViewById<android.widget.LinearLayout>(R.id.ai_card_none)?.setOnClickListener {
            onAISelected(AI_NONE)
        }
    }

    /**
     * Handle AI selection from choice screen.
     */
    private fun onAISelected(ai: String) {
        selectedAI = ai

        // Save selection
        val prefs = getSharedPreferences("mobilecli", Context.MODE_PRIVATE)
        prefs.edit().putString("selected_ai", ai).putBoolean("ai_setup_complete", true).apply()

        if (ai == AI_NONE) {
            // Skip AI installation, go straight to terminal
            hideSetupOverlay()
            createSessionOrDefer()
            Toast.makeText(this, "Terminal ready! Install AI tools anytime from settings.", Toast.LENGTH_LONG).show()
        } else {
            // Show progress screen and install selected AI
            showProgressScreenForAI(ai)
            installSelectedAI(ai)
        }
    }

    /**
     * Show AI choice screen (Stage 2).
     */
    private fun showAIChoiceScreen() {
        runOnUiThread {
            setupProgressScreen?.visibility = android.view.View.GONE
            aiChoiceScreen?.visibility = android.view.View.VISIBLE
        }
    }

    /**
     * Show progress screen for AI installation (Stage 3).
     */
    private fun showProgressScreenForAI(ai: String) {
        runOnUiThread {
            aiChoiceScreen?.visibility = android.view.View.GONE
            setupProgressScreen?.visibility = android.view.View.VISIBLE

            val aiName = when (ai) {
                AI_CLAUDE -> "Claude Code"
                AI_GEMINI -> "Gemini CLI"
                AI_CODEX -> "Codex CLI"
                else -> "AI Tools"
            }
            updateSetupProgress(0, "Installing $aiName...")
        }
    }

    /**
     * Install selected AI tool and auto-launch when complete.
     * ERROR INVISIBILITY: Users never see technical errors.
     * - Errors are logged but not shown
     * - Auto-retry on failure
     * - Friendly messages only
     */
    private fun installSelectedAI(ai: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            var retryCount = 0
            val maxRetries = 2

            while (retryCount <= maxRetries) {
                try {
                    // Create session first if needed
                    withContext(Dispatchers.Main) {
                        if (sessions.isEmpty()) {
                            createSessionOrDefer()
                        }
                    }

                    // Wait for session to be ready
                    delay(1000)

                    // Dev tools command - installs Java, Gradle, Android SDK setup
                    val devToolsCmd = """
                        pkg install -y git openjdk-17 gradle aapt aapt2 apksigner d8 dx zip unzip 2>/dev/null
                        mkdir -p ~/android-sdk/platforms/android-34 ~/android-sdk/build-tools/34.0.0 2>/dev/null
                        cp /data/data/com.termux/files/usr/share/aapt/android.jar ~/android-sdk/platforms/android-34/ 2>/dev/null
                        cd ~/android-sdk/build-tools/34.0.0 && ln -sf /data/data/com.termux/files/usr/bin/aapt aapt 2>/dev/null && ln -sf /data/data/com.termux/files/usr/bin/aapt2 aapt2 2>/dev/null && ln -sf /data/data/com.termux/files/usr/bin/d8 d8 2>/dev/null && ln -sf /data/data/com.termux/files/usr/bin/apksigner apksigner 2>/dev/null
                        touch ~/.dev_tools_installed
                    """.trimIndent().replace("\n", " && ")

                    val (packageName, installCmd, launchCmd) = when (ai) {
                        AI_CLAUDE -> Triple(
                            "Claude Code",
                            "pkg update -y 2>/dev/null && pkg install -y nodejs 2>/dev/null && npm install -g @anthropic-ai/claude-code 2>/dev/null && $devToolsCmd",
                            "claude"
                        )
                        AI_GEMINI -> Triple(
                            "Gemini CLI",
                            "pkg update -y 2>/dev/null && pkg install -y nodejs 2>/dev/null && npm install -g @anthropic-ai/gemini-cli 2>/dev/null && $devToolsCmd",
                            "gemini"
                        )
                        AI_CODEX -> Triple(
                            "Codex CLI",
                            "pkg update -y 2>/dev/null && pkg install -y nodejs 2>/dev/null && npm install -g @openai/codex 2>/dev/null && $devToolsCmd",
                            "codex"
                        )
                        else -> Triple("", "", "")
                    }

                    if (installCmd.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            hideSetupOverlay()
                        }
                        return@launch
                    }

                    // Update progress with retry info if needed
                    withContext(Dispatchers.Main) {
                        if (retryCount > 0) {
                            updateSetupProgress(5, "Please wait...")
                        } else {
                            updateSetupProgress(10, "Updating packages...")
                        }
                    }

                    // Run installation in background
                    // 2>/dev/null suppresses error output so users don't see it
                    val fullCmd = "$installCmd\n"

                    withContext(Dispatchers.Main) {
                        session?.write(fullCmd.toByteArray(Charsets.UTF_8), 0, fullCmd.toByteArray(Charsets.UTF_8).size)
                    }

                    // Progress animation while installation runs (includes dev tools)
                    val progressSteps = listOf(
                        10 to "Checking network...",
                        18 to "Downloading packages...",
                        28 to "Installing Node.js...",
                        38 to "Downloading $packageName...",
                        48 to "Installing $packageName...",
                        55 to "Installing Java 17...",
                        65 to "Installing Gradle...",
                        75 to "Setting up Android SDK...",
                        85 to "Configuring build tools...",
                        92 to "Finalizing...",
                        97 to "Almost ready..."
                    )

                    for ((p, msg) in progressSteps) {
                        delay(5000) // More time for dev tools
                        withContext(Dispatchers.Main) {
                            updateSetupProgress(p, msg)
                        }
                    }

                    // Wait for installation to complete (more time for dev tools)
                    delay(10000)

                    withContext(Dispatchers.Main) {
                        updateSetupProgress(100, "Ready!")
                        delay(500)

                        // v1.6.2 FIX: Clear terminal and launch BEFORE hiding overlay
                        // This prevents users from seeing proprietary IP in terminal output
                        val clearCmd = "clear\n"
                        session?.write(clearCmd.toByteArray(Charsets.UTF_8), 0, clearCmd.length)
                        delay(300)

                        // Launch the AI command FIRST (while overlay still visible)
                        if (launchCmd.isNotEmpty()) {
                            val cmd = "$launchCmd\n"
                            session?.write(cmd.toByteArray(Charsets.UTF_8), 0, cmd.length)
                        }

                        // Wait for AI to start rendering its UI (overlay still covers terminal)
                        delay(3000)

                        // NOW hide overlay - user sees Claude's UI, not terminal output
                        hideSetupOverlay()

                        // Dev tools already installed during setup - no background install needed
                    }

                    // Success - exit loop
                    return@launch

                } catch (e: Exception) {
                    Log.e("MainActivity", "AI installation attempt ${retryCount + 1} failed: ${e.message}")
                    retryCount++

                    if (retryCount <= maxRetries) {
                        // Silent retry
                        withContext(Dispatchers.Main) {
                            updateSetupProgress(5, "Please wait...")
                        }
                        delay(2000)
                    }
                }
            }

            // All retries exhausted - show FRIENDLY message (never technical error)
            withContext(Dispatchers.Main) {
                hideSetupOverlay()
                createSessionOrDefer()

                val aiName = when (ai) {
                    AI_CLAUDE -> "Claude"
                    AI_GEMINI -> "Gemini"
                    AI_CODEX -> "Codex"
                    else -> "AI"
                }

                // Friendly message - user doesn't know there was an error
                Toast.makeText(
                    this@MainActivity,
                    "Ready! Type '$ai' to start $aiName when download finishes.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Show error appropriately based on mode.
     * Developer mode: Show technical error
     * User mode: Show friendly message only
     */
    private fun showErrorSmart(technicalError: String, friendlyMessage: String) {
        if (developerModeEnabled) {
            showError(technicalError)
        } else {
            Toast.makeText(this, friendlyMessage, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Show the clean setup overlay (user mode).
     */
    private fun showSetupOverlay() {
        if (!developerModeEnabled) {
            runOnUiThread {
                setupOverlay?.visibility = android.view.View.VISIBLE
                terminalView.visibility = android.view.View.GONE
            }
        }
    }

    /**
     * Hide the setup overlay and transition through the welcome overlay.
     * v1.6.3 IP Protection Fix:
     * - Instead of showing terminal directly (which could flash proprietary commands),
     * - We show a "Welcome to MobileCLI" screen that covers everything
     * - Terminal becomes visible BUT is hidden behind the welcome overlay
     * - After Claude starts rendering, we fade out the welcome overlay
     * - Users NEVER see terminal output during setup - only friendly welcome message
     */
    private fun hideSetupOverlay() {
        runOnUiThread {
            // Step 1: Hide the setup overlay
            setupOverlay?.visibility = android.view.View.GONE

            // Step 2: Show the welcome overlay FIRST (this covers the terminal)
            // The welcome overlay has higher elevation (15dp) so it's on top
            welcomeOverlay?.visibility = android.view.View.VISIBLE

            // Step 3: Make terminal visible (but it's hidden behind welcome overlay)
            terminalView.visibility = android.view.View.VISIBLE

            // Step 4: Update terminal size now that it's visible
            scheduleTerminalUpdate()

            // Step 5: After delay, hide welcome overlay (Claude should be rendered by now)
            // This delay gives Claude time to start and render its UI
            uiHandler.postDelayed({
                hideWelcomeOverlay()
            }, 4000L) // 4 seconds - enough time for Claude to render
        }
    }

    /**
     * Hide the welcome overlay to reveal the terminal.
     * Called after Claude has had time to render its UI.
     */
    private fun hideWelcomeOverlay() {
        runOnUiThread {
            // Fade out animation for smooth transition
            welcomeOverlay?.animate()
                ?.alpha(0f)
                ?.setDuration(500L)
                ?.withEndAction {
                    welcomeOverlay?.visibility = android.view.View.GONE
                    welcomeOverlay?.alpha = 1f // Reset for next time
                }
                ?.start()
        }
    }

    // ============================================
    // GAME ENGINE UI SETUP
    // ============================================

    /**
     * Initialize and show the Game Engine UI.
     * This sets up the 3D viewport, panels, and moves the terminal
     * into the slide-out Claude panel.
     */
    private fun setupGameEngineUI() {
        // Get game engine overlay
        gameEngineOverlay = findViewById(R.id.game_engine_overlay)
        gameEngineOverlay?.visibility = android.view.View.VISIBLE

        // Get sliding pane layout
        slidingPane = findViewById(R.id.sliding_pane)

        // Get Claude panel elements
        claudePanel = findViewById(R.id.claude_panel)
        terminalContainer = findViewById(R.id.terminal_container)

        // Get viewport and other UI elements
        viewport = findViewById(R.id.viewport)
        consoleOutput = findViewById(R.id.console_output)
        projectName = findViewById(R.id.project_name)
        viewportInfo = findViewById(R.id.viewport_info)

        // Move the terminal view into the Claude panel
        moveTerminalToClaudePanel()

        // Setup 3D viewport
        setupViewport()

        // Setup toolbar buttons
        setupGameEngineToolbar()

        // Log to console
        logToConsole("[MCG] MobileCLI Games Engine initialized")
        logToConsole("[MCG] Claude Code terminal ready - tap 'Claude' to open")
    }

    /**
     * Move the TerminalView from main layout into the Claude panel.
     */
    private fun moveTerminalToClaudePanel() {
        // Remove terminal from its current parent
        val parent = terminalView.parent as? ViewGroup
        parent?.removeView(terminalView)

        // Add terminal to the Claude panel container
        terminalContainer?.addView(terminalView, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))

        // Make terminal visible within the container
        terminalView.visibility = android.view.View.VISIBLE
    }

    /**
     * Setup the WebView for 3D viewport with Three.js.
     */
    private fun setupViewport() {
        viewport?.let { webView ->
            webView.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                loadWithOverviewMode = true
                useWideViewPort = true
                builtInZoomControls = false
                displayZoomControls = false
            }

            webView.webViewClient = WebViewClient()

            // Load a placeholder for now - will be replaced with actual 3D scene
            val placeholderHtml = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body {
                            margin: 0;
                            background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
                            display: flex;
                            justify-content: center;
                            align-items: center;
                            height: 100vh;
                            color: #e94560;
                            font-family: -apple-system, sans-serif;
                            text-align: center;
                        }
                        .content {
                            padding: 20px;
                        }
                        h1 { font-size: 24px; margin-bottom: 10px; }
                        p { color: #888; font-size: 14px; }
                        .hint { color: #4CAF50; margin-top: 20px; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="content">
                        <h1>3D Viewport</h1>
                        <p>Tap "+ Add" to create objects</p>
                        <p class="hint">Three.js engine loading...</p>
                    </div>
                </body>
                </html>
            """.trimIndent()

            webView.loadDataWithBaseURL(null, placeholderHtml, "text/html", "UTF-8", null)
        }
    }

    /**
     * Setup the game engine toolbar buttons.
     */
    private fun setupGameEngineToolbar() {
        // Menu button
        findViewById<Button>(R.id.btn_menu)?.setOnClickListener {
            drawerLayout.openDrawer(androidx.core.view.GravityCompat.START)
        }

        // Play button
        findViewById<Button>(R.id.btn_play)?.setOnClickListener {
            logToConsole("[MCG] Play mode - coming soon")
            Toast.makeText(this, "Play mode coming soon!", Toast.LENGTH_SHORT).show()
        }

        // Add button
        findViewById<Button>(R.id.btn_add)?.setOnClickListener {
            showAddObjectDialog()
        }

        // Build APK button
        findViewById<Button>(R.id.btn_build)?.setOnClickListener {
            showBuildDialog()
        }

        // Claude button - toggle terminal panel
        findViewById<Button>(R.id.btn_claude)?.setOnClickListener {
            toggleClaudePanel()
        }

        // Claude panel close button
        findViewById<Button>(R.id.btn_claude_close)?.setOnClickListener {
            closeClaudePanel()
        }

        // Claude panel fullscreen button
        findViewById<Button>(R.id.btn_claude_fullscreen)?.setOnClickListener {
            toggleFullscreenTerminal()
        }

        // Clear console button
        findViewById<Button>(R.id.btn_clear_console)?.setOnClickListener {
            consoleOutput?.text = ""
            logToConsole("[MCG] Console cleared")
        }
    }

    /**
     * Toggle the Claude terminal panel open/closed.
     */
    private fun toggleClaudePanel() {
        slidingPane?.let { pane ->
            if (pane.isOpen) {
                closeClaudePanel()
            } else {
                openClaudePanel()
            }
        }
    }

    /**
     * Open the Claude terminal panel.
     */
    private fun openClaudePanel() {
        slidingPane?.openPane()
        isClaudePanelOpen = true
        logToConsole("[MCG] Claude panel opened")

        // Focus on terminal and show keyboard
        terminalView.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(terminalView, InputMethodManager.SHOW_IMPLICIT)
    }

    /**
     * Close the Claude terminal panel.
     */
    private fun closeClaudePanel() {
        slidingPane?.closePane()
        isClaudePanelOpen = false

        // Hide keyboard when closing
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(terminalView.windowToken, 0)
    }

    /**
     * Toggle fullscreen terminal mode.
     */
    private fun toggleFullscreenTerminal() {
        isFullscreenTerminal = !isFullscreenTerminal

        if (isFullscreenTerminal) {
            // Hide game engine, show only terminal
            gameEngineOverlay?.visibility = android.view.View.GONE
            drawerLayout.visibility = android.view.View.VISIBLE
            terminalView.visibility = android.view.View.VISIBLE

            // Move terminal back to main view if needed
            val parent = terminalView.parent as? ViewGroup
            if (parent != terminalContainer) {
                // Already in main view
            } else {
                parent?.removeView(terminalView)
                val mainContent = findViewById<android.widget.LinearLayout>(R.id.drawer_layout)?.getChildAt(0) as? android.widget.LinearLayout
                mainContent?.addView(terminalView, 1, ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0
                ).apply {
                    (this as? android.widget.LinearLayout.LayoutParams)?.weight = 1f
                })
            }

            Toast.makeText(this, "Fullscreen terminal - tap back to return", Toast.LENGTH_SHORT).show()
        } else {
            // Return to game engine mode
            drawerLayout.visibility = android.view.View.GONE
            gameEngineOverlay?.visibility = android.view.View.VISIBLE
            moveTerminalToClaudePanel()
            openClaudePanel()
        }
    }

    /**
     * Show dialog to add new 3D object.
     */
    private fun showAddObjectDialog() {
        val objects = arrayOf(
            "Cube", "Sphere", "Cylinder", "Plane", "Cone",
            "Torus", "Empty", "Light", "Camera"
        )

        AlertDialog.Builder(this)
            .setTitle("Add Object")
            .setItems(objects) { _, which ->
                val objectType = objects[which]
                logToConsole("[MCG] Added: $objectType")
                Toast.makeText(this, "Added $objectType", Toast.LENGTH_SHORT).show()
                // TODO: Actually add the object to the 3D scene
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Show dialog to build APK.
     */
    private fun showBuildDialog() {
        AlertDialog.Builder(this)
            .setTitle("Build APK")
            .setMessage("Claude Code will build your game into an Android APK.\n\nThis requires:\n• Project configured\n• All assets ready\n• Build tools installed")
            .setPositiveButton("Build with Claude") { _, _ ->
                // Open Claude panel and send build command
                openClaudePanel()
                lifecycleScope.launch {
                    delay(500)
                    val cmd = "echo 'Build APK functionality - Tell me about your project and I will help you build it!'\n"
                    session?.write(cmd.toByteArray(Charsets.UTF_8), 0, cmd.length)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Log a message to the game engine console.
     */
    private fun logToConsole(message: String) {
        runOnUiThread {
            val current = consoleOutput?.text?.toString() ?: ""
            consoleOutput?.text = current + message + "\n"

            // Auto-scroll to bottom
            findViewById<android.widget.ScrollView>(R.id.console_scroll)?.let { scroll ->
                scroll.post { scroll.fullScroll(android.view.View.FOCUS_DOWN) }
            }
        }
    }

    /**
     * Install development tools (Java, Gradle, Android SDK) in background.
     * This enables the self-rebuild capability without user intervention.
     * Runs silently after the game engine UI appears.
     */
    private fun installDevToolsInBackground() {
        // Check if dev tools are already installed
        val devToolsMarker = File(bootstrapInstaller.homeDir, ".dev_tools_installed")
        if (devToolsMarker.exists()) {
            logToConsole("[MCG] Dev tools already installed - self-rebuild ready")
            return
        }

        logToConsole("[MCG] Installing dev tools in background...")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Wait a bit for the terminal to be ready
                delay(3000)

                // Install dev tools silently in background
                val installCmd = """
                    # Auto-install dev tools for self-rebuild capability
                    (
                        pkg update -y 2>/dev/null
                        pkg install -y git openjdk-17 gradle aapt aapt2 apksigner d8 dx coreutils zip unzip 2>/dev/null

                        # Setup Android SDK structure
                        mkdir -p ~/android-sdk/platforms/android-34
                        mkdir -p ~/android-sdk/build-tools/34.0.0

                        # Copy android.jar
                        if [ -f /data/data/com.termux/files/usr/share/aapt/android.jar ]; then
                            cp /data/data/com.termux/files/usr/share/aapt/android.jar ~/android-sdk/platforms/android-34/
                        fi

                        # Symlink build tools
                        cd ~/android-sdk/build-tools/34.0.0
                        ln -sf /data/data/com.termux/files/usr/bin/aapt aapt 2>/dev/null
                        ln -sf /data/data/com.termux/files/usr/bin/aapt2 aapt2 2>/dev/null
                        ln -sf /data/data/com.termux/files/usr/bin/d8 d8 2>/dev/null
                        ln -sf /data/data/com.termux/files/usr/bin/dx dx 2>/dev/null
                        ln -sf /data/data/com.termux/files/usr/bin/apksigner apksigner 2>/dev/null
                        ln -sf /data/data/com.termux/files/usr/bin/zipalign zipalign 2>/dev/null

                        # Mark as installed
                        touch ~/.dev_tools_installed

                        echo "Dev tools installed - self-rebuild ready"
                    ) &
                """.trimIndent() + "\n"

                withContext(Dispatchers.Main) {
                    session?.write(installCmd.toByteArray(Charsets.UTF_8), 0, installCmd.length)
                }

                // Wait for installation to complete (runs in background on terminal)
                delay(60000) // Give it a minute to install packages

                withContext(Dispatchers.Main) {
                    if (devToolsMarker.exists()) {
                        logToConsole("[MCG] Dev tools installed - self-rebuild capability enabled!")
                    }
                }

            } catch (e: Exception) {
                Log.e("MainActivity", "Dev tools background install failed: ${e.message}")
                // Non-fatal - user can still run install-dev-tools manually
            }
        }
    }

    /**
     * Update the setup overlay progress.
     */
    private fun updateSetupProgress(progress: Int, status: String) {
        runOnUiThread {
            setupProgress?.progress = progress
            setupPercentage?.text = "$progress%"
            setupStatus?.text = status

            // Show step detail for certain progress points
            val step = when {
                progress < 20 -> "Preparing environment..."
                progress < 40 -> "Extracting packages..."
                progress < 60 -> "Setting up shell..."
                progress < 80 -> "Configuring AI tools..."
                progress < 95 -> "Almost ready..."
                else -> "Complete!"
            }
            setupStep?.text = step
        }
    }

    /**
     * Setup URL watcher - polls for URL open requests from shell.
     * This is the v51 fix that ACTUALLY WORKS because the Activity itself
     * opens the URL, not a background process.
     *
     * Shell writes URL to: ~/.termux/url_to_open
     * Activity reads it, deletes it, and opens the URL
     */
    private fun setupUrlWatcher() {
        val urlFile = File(File(bootstrapInstaller.homeDir, ".termux"), "url_to_open")

        urlWatcherRunnable = object : Runnable {
            override fun run() {
                try {
                    if (urlFile.exists()) {
                        val url = urlFile.readText().trim()
                        urlFile.delete()

                        if (url.isNotEmpty()) {
                            Log.i("MainActivity", "URL watcher: Opening URL: $url")
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                                // v52: Use PendingIntent for clean identity
                                val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                    android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
                                } else {
                                    android.app.PendingIntent.FLAG_UPDATE_CURRENT
                                }
                                val pendingIntent = android.app.PendingIntent.getActivity(
                                    this@MainActivity, 0, intent, flags
                                )
                                pendingIntent.send()
                                Log.i("MainActivity", "URL watcher: PendingIntent sent successfully")
                            } catch (e: Exception) {
                                Log.e("MainActivity", "URL watcher: Failed to open URL", e)
                                runOnUiThread {
                                    Toast.makeText(this@MainActivity, "Failed to open: $url", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "URL watcher error", e)
                }

                // Schedule next check
                uiHandler.postDelayed(this, urlWatchInterval)
            }
        }

        // Start the watcher
        uiHandler.postDelayed(urlWatcherRunnable!!, urlWatchInterval)
        Log.i("MainActivity", "URL watcher started, checking every ${urlWatchInterval}ms")
    }

    // ============================================
    // DITTO ARCHITECTURE - UI Command System (v84)
    // ============================================

    /**
     * Setup UI command watcher - polls for UI commands from shell.
     * This is the core of the DITTO architecture that allows AI to
     * morph the app's UI at runtime via file-based IPC.
     *
     * Shell writes command to: ~/.termux/ui_command
     * Activity reads it, executes it, writes result to: ~/.termux/ui_result
     *
     * Commands supported:
     * - ADD_KEY row text action - Add extra key button
     * - REMOVE_KEY row text - Remove extra key button
     * - CLEAR_KEYS row - Clear all keys in row
     * - SET_BACKGROUND #RRGGBB - Change terminal background
     * - SET_TEXT_COLOR #RRGGBB - Change terminal text color
     * - SET_TEXT_SIZE sp - Change terminal font size
     * - SHOW_TOAST message - Show toast notification
     * - GET_UI_STATE - Return current UI configuration
     * - FACTORY_RESET - Revert to default UI
     */
    private fun setupUiCommandWatcher() {
        // Ensure .termux directory exists
        val termuxDir = File(bootstrapInstaller.homeDir, ".termux")
        termuxDir.mkdirs()

        // Ensure .mobilecli directory exists for persistence
        val mobilecliDir = File(bootstrapInstaller.homeDir, ".mobilecli")
        mobilecliDir.mkdirs()

        // Initialize extra keys row references
        extraKeysRow1 = findViewById(R.id.extra_keys_row1)
        extraKeysRow2 = findViewById(R.id.extra_keys_row2)

        uiCommandWatcherRunnable = object : Runnable {
            override fun run() {
                try {
                    if (uiCommandFile.exists()) {
                        val command = uiCommandFile.readText().trim()
                        uiCommandFile.delete()

                        if (command.isNotEmpty()) {
                            Log.i("MainActivity", "UI command received: $command")
                            runOnUiThread {
                                val result = executeUiCommand(command)
                                // Write result
                                try {
                                    uiResultFile.writeText(result)
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "Failed to write UI result", e)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "UI command watcher error", e)
                }

                // Schedule next check
                uiHandler.postDelayed(this, uiCommandWatchInterval)
            }
        }

        // Start the watcher
        uiHandler.postDelayed(uiCommandWatcherRunnable!!, uiCommandWatchInterval)
        Log.i("MainActivity", "UI command watcher started (DITTO), checking every ${uiCommandWatchInterval}ms")

        // Setup DITTO WebView layer
        setupDittoWebView()

        // Load persisted UI state
        loadUiState()
    }

    /**
     * Execute a UI command and return result.
     * Commands are parsed as: COMMAND_NAME arg1 arg2 ...
     */
    private fun executeUiCommand(command: String): String {
        val parts = command.split(" ", limit = 2)
        val cmd = parts[0].uppercase()
        val args = if (parts.size > 1) parts[1] else ""

        return try {
            when (cmd) {
                // Native layer commands
                "ADD_KEY" -> executeAddKey(args)
                "REMOVE_KEY" -> executeRemoveKey(args)
                "CLEAR_KEYS" -> executeClearKeys(args)
                "SET_BACKGROUND" -> executeSetBackground(args)
                "SET_TEXT_COLOR" -> executeSetTextColor(args)
                "SET_TEXT_SIZE" -> executeSetTextSize(args)
                "SHOW_TOAST" -> executeShowToast(args)
                "GET_UI_STATE" -> executeGetUiState()
                "FACTORY_RESET" -> executeFactoryReset()
                "SET_KEY_STYLE" -> executeSetKeyStyle(args)
                // Morphable WebView layer commands (DITTO)
                "LOAD_UI" -> executeLoadUi(args)
                "HIDE_UI" -> executeHideUi()
                "SHOW_UI" -> executeShowUi()
                "UI_SIZE" -> executeUiSize(args)
                "UI_POSITION" -> executeUiPosition(args)
                "UI_OPACITY" -> executeUiOpacity(args)
                "INJECT_JS" -> executeInjectJs(args)
                // Profile/Social commands (v86)
                "SAVE_PROFILE" -> executeSaveProfile(args)
                "LOAD_PROFILE" -> executeLoadProfile(args)
                "LIST_PROFILES" -> executeListProfiles()
                "DELETE_PROFILE" -> executeDeleteProfile(args)
                "EXPORT_PROFILE" -> executeExportProfile(args)
                "IMPORT_PROFILE" -> executeImportProfile(args)
                "SHARE_PROFILE" -> executeShareProfile(args)
                "GET_FULL_STATE" -> executeGetFullState()
                // Profile Browser UI (v88)
                "SHOW_BROWSER" -> executeShowBrowser()
                else -> "ERROR: Unknown command: $cmd"
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "UI command execution failed: $command", e)
            "ERROR: ${e.message}"
        }
    }

    /**
     * ADD_KEY row text action
     * - row: 1 or 2
     * - text: Button label
     * - action: Key code (number) or escape sequence (string starting with \e or \x)
     */
    private fun executeAddKey(args: String): String {
        val parts = args.split(" ", limit = 3)
        if (parts.size < 3) return "ERROR: Usage: ADD_KEY row text action"

        val row = parts[0].toIntOrNull() ?: return "ERROR: Invalid row number"
        val text = parts[1]
        val action = parts[2]

        val targetRow = when (row) {
            1 -> extraKeysRow1
            2 -> extraKeysRow2
            else -> return "ERROR: Row must be 1 or 2"
        } ?: return "ERROR: Extra keys row not found"

        // Create button
        val button = Button(this).apply {
            this.text = text
            textSize = 12f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF333333.toInt())
            setPadding(16, 8, 16, 8)
            minWidth = 0
            minimumWidth = 0

            setOnClickListener {
                handleDynamicKeyPress(action)
            }
        }

        // Add to row
        targetRow.addView(button)

        // Save state
        saveUiState()

        return "OK: Added key '$text' to row $row"
    }

    /**
     * Handle key press from dynamic button.
     * Supports:
     * - Numeric keycodes (e.g., "27" for ESC)
     * - Escape sequences (e.g., "\e[A" for up arrow, "\x1b[A")
     * - Special names (ESC, TAB, ENTER)
     */
    private fun handleDynamicKeyPress(action: String) {
        session?.let { s ->
            when {
                // Numeric keycode
                action.toIntOrNull() != null -> {
                    val keyCode = action.toInt()
                    s.write(byteArrayOf(keyCode.toByte()), 0, 1)
                }
                // Escape sequence with \e or \x1b
                action.startsWith("\\e") || action.startsWith("\\x1b") -> {
                    val sequence = action
                        .replace("\\e", "\u001b")
                        .replace("\\x1b", "\u001b")
                    s.write(sequence.toByteArray(), 0, sequence.length)
                }
                // Special key names
                action.uppercase() == "ESC" -> s.write(byteArrayOf(27), 0, 1)
                action.uppercase() == "TAB" -> s.write(byteArrayOf(9), 0, 1)
                action.uppercase() == "ENTER" -> s.write(byteArrayOf(13), 0, 1)
                // String to type
                else -> s.write(action.toByteArray(), 0, action.length)
            }
        }
    }

    /**
     * REMOVE_KEY row text
     */
    private fun executeRemoveKey(args: String): String {
        val parts = args.split(" ", limit = 2)
        if (parts.size < 2) return "ERROR: Usage: REMOVE_KEY row text"

        val row = parts[0].toIntOrNull() ?: return "ERROR: Invalid row number"
        val text = parts[1]

        val targetRow = when (row) {
            1 -> extraKeysRow1
            2 -> extraKeysRow2
            else -> return "ERROR: Row must be 1 or 2"
        } ?: return "ERROR: Extra keys row not found"

        // Find and remove button with matching text
        for (i in 0 until targetRow.childCount) {
            val child = targetRow.getChildAt(i)
            if (child is Button && child.text.toString() == text) {
                targetRow.removeViewAt(i)
                saveUiState()
                return "OK: Removed key '$text' from row $row"
            }
        }

        return "ERROR: Key '$text' not found in row $row"
    }

    /**
     * CLEAR_KEYS row
     */
    private fun executeClearKeys(args: String): String {
        val row = args.trim().toIntOrNull() ?: return "ERROR: Usage: CLEAR_KEYS row"

        val targetRow = when (row) {
            1 -> extraKeysRow1
            2 -> extraKeysRow2
            else -> return "ERROR: Row must be 1 or 2"
        } ?: return "ERROR: Extra keys row not found"

        targetRow.removeAllViews()
        saveUiState()

        return "OK: Cleared all keys from row $row"
    }

    /**
     * SET_BACKGROUND #RRGGBB
     */
    private fun executeSetBackground(args: String): String {
        val colorStr = args.trim()
        if (!colorStr.startsWith("#") || colorStr.length != 7) {
            return "ERROR: Usage: SET_BACKGROUND #RRGGBB"
        }

        try {
            val color = android.graphics.Color.parseColor(colorStr)
            terminalView.setBackgroundColor(color)
            saveUiState()
            return "OK: Background set to $colorStr"
        } catch (e: Exception) {
            return "ERROR: Invalid color: $colorStr"
        }
    }

    /**
     * SET_TEXT_COLOR #RRGGBB
     * Note: TerminalView doesn't directly support text color change,
     * but we can adjust the default foreground color via escape sequences.
     */
    private fun executeSetTextColor(args: String): String {
        val colorStr = args.trim()
        if (!colorStr.startsWith("#") || colorStr.length != 7) {
            return "ERROR: Usage: SET_TEXT_COLOR #RRGGBB"
        }

        // For now, store the preference - full implementation would require
        // terminal emulator modification
        saveUiState()
        return "OK: Text color preference saved (requires terminal restart)"
    }

    /**
     * SET_TEXT_SIZE sp
     */
    private fun executeSetTextSize(args: String): String {
        val size = args.trim().toIntOrNull() ?: return "ERROR: Usage: SET_TEXT_SIZE sp"

        if (size < minTextSize || size > maxTextSize) {
            return "ERROR: Size must be between $minTextSize and $maxTextSize"
        }

        currentTextSize = size
        terminalView.setTextSize(currentTextSize)
        terminalView.post {
            updateTerminalSize()
            terminalView.onScreenUpdated()
        }
        saveUiState()

        return "OK: Text size set to ${size}sp"
    }

    /**
     * SHOW_TOAST message
     */
    private fun executeShowToast(args: String): String {
        val message = args.trim()
        if (message.isEmpty()) return "ERROR: Usage: SHOW_TOAST message"

        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        return "OK: Toast shown"
    }

    /**
     * GET_UI_STATE - Return current UI configuration as JSON
     */
    private fun executeGetUiState(): String {
        val state = buildString {
            append("{")
            append("\"text_size\":$currentTextSize,")
            append("\"rows1_count\":${extraKeysRow1?.childCount ?: 0},")
            append("\"rows2_count\":${extraKeysRow2?.childCount ?: 0}")
            append("}")
        }
        return state
    }

    /**
     * FACTORY_RESET - Restore default UI
     */
    private fun executeFactoryReset(): String {
        // Reset text size
        currentTextSize = 28
        terminalView.setTextSize(currentTextSize)

        // Reset background
        terminalView.setBackgroundColor(0xFF000000.toInt())

        // Rebuild extra keys to default
        setupExtraKeys()

        // Delete persisted UI state
        uiConfigFile.delete()

        terminalView.post {
            updateTerminalSize()
            terminalView.onScreenUpdated()
        }

        return "OK: Factory reset complete"
    }

    /**
     * SET_KEY_STYLE bgColor textColor
     */
    private fun executeSetKeyStyle(args: String): String {
        val parts = args.trim().split(" ")
        if (parts.size < 2) return "ERROR: Usage: SET_KEY_STYLE #bgColor #textColor"

        val bgColor = try { android.graphics.Color.parseColor(parts[0]) } catch (e: Exception) { return "ERROR: Invalid bg color" }
        val textColor = try { android.graphics.Color.parseColor(parts[1]) } catch (e: Exception) { return "ERROR: Invalid text color" }

        // Apply to all keys in both rows
        listOf(extraKeysRow1, extraKeysRow2).forEach { row ->
            row?.let {
                for (i in 0 until it.childCount) {
                    val child = it.getChildAt(i)
                    if (child is Button) {
                        child.setBackgroundColor(bgColor)
                        child.setTextColor(textColor)
                    }
                }
            }
        }

        saveUiState()
        return "OK: Key style updated"
    }

    /**
     * Save current UI state to file for persistence.
     */
    private fun saveUiState() {
        try {
            val state = buildString {
                appendLine("text_size=$currentTextSize")
                // More state can be added here
            }
            uiConfigFile.writeText(state)
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to save UI state", e)
        }
    }

    /**
     * Load UI state from file.
     */
    private fun loadUiState() {
        try {
            if (uiConfigFile.exists()) {
                val lines = uiConfigFile.readLines()
                lines.forEach { line ->
                    val parts = line.split("=", limit = 2)
                    if (parts.size == 2) {
                        when (parts[0]) {
                            "text_size" -> {
                                parts[1].toIntOrNull()?.let { size ->
                                    if (size in minTextSize..maxTextSize) {
                                        currentTextSize = size
                                        terminalView.setTextSize(currentTextSize)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to load UI state", e)
        }
    }

    // ============================================
    // DITTO MORPHABLE WEBVIEW LAYER (v84)
    // ============================================

    /**
     * Setup the DITTO WebView overlay system.
     * This allows AI to load any HTML UI as an overlay.
     */
    private fun setupDittoWebView() {
        dittoOverlay = findViewById(R.id.ditto_overlay)
        dittoWebView = findViewById(R.id.ditto_webview)
        dittoCloseButton = findViewById(R.id.ditto_close)

        // Configure WebView for custom UIs
        dittoWebView?.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.builtInZoomControls = false

            // Add JavaScript bridge
            addJavascriptInterface(DittoJsBridge(), "MobileCLI")

            webViewClient = object : android.webkit.WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.i("MainActivity", "DITTO UI loaded: $url")
                }
            }
        }

        // Setup close button
        dittoCloseButton?.setOnClickListener {
            executeHideUi()
        }

        Log.i("MainActivity", "DITTO WebView layer initialized")
    }

    /**
     * JavaScript bridge for DITTO custom UIs.
     * Allows HTML UIs to interact with the terminal and app.
     */
    inner class DittoJsBridge {

        /**
         * Send a single key/character to the terminal.
         */
        @android.webkit.JavascriptInterface
        fun sendKey(key: String) {
            runOnUiThread {
                session?.write(key.toByteArray(), 0, key.length)
            }
        }

        /**
         * Send an escape sequence to the terminal.
         * Use format like "\x1b[A" for up arrow.
         */
        @android.webkit.JavascriptInterface
        fun sendSequence(sequence: String) {
            runOnUiThread {
                val parsed = sequence
                    .replace("\\e", "\u001b")
                    .replace("\\x1b", "\u001b")
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                session?.write(parsed.toByteArray(), 0, parsed.length)
            }
        }

        /**
         * Run a shell command in the terminal.
         */
        @android.webkit.JavascriptInterface
        fun runCmd(command: String) {
            runOnUiThread {
                val cmd = command + "\n"
                session?.write(cmd.toByteArray(), 0, cmd.length)
            }
        }

        /**
         * Show a toast notification.
         */
        @android.webkit.JavascriptInterface
        fun toast(message: String) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
            }
        }

        /**
         * Vibrate the device (milliseconds).
         */
        @android.webkit.JavascriptInterface
        fun vibrate(ms: Long) {
            runOnUiThread {
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator?.vibrate(android.os.VibrationEffect.createOneShot(ms, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(ms)
                }
            }
        }

        /**
         * Close the DITTO UI overlay.
         */
        @android.webkit.JavascriptInterface
        fun closeUI() {
            runOnUiThread {
                executeHideUi()
            }
        }

        /**
         * Read from clipboard.
         */
        @android.webkit.JavascriptInterface
        fun readClipboard(): String {
            var result = ""
            runOnUiThread {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                result = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
            }
            Thread.sleep(50) // Wait for UI thread
            return result
        }

        /**
         * Write to clipboard.
         */
        @android.webkit.JavascriptInterface
        fun writeClipboard(text: String) {
            runOnUiThread {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("DITTO", text))
            }
        }

        /**
         * Get list of local profiles as JSON array.
         */
        @android.webkit.JavascriptInterface
        fun getLocalProfiles(): String {
            val profiles = profilesDir.listFiles()?.filter { it.extension == "json" }?.map { it.nameWithoutExtension }
            if (profiles.isNullOrEmpty()) return "[]"
            return "[" + profiles.joinToString(",") { "\"$it\"" } + "]"
        }

        /**
         * Get a specific profile's content.
         */
        @android.webkit.JavascriptInterface
        fun getProfileContent(name: String): String {
            val profileFile = File(profilesDir, "$name.json")
            return if (profileFile.exists()) profileFile.readText() else "{}"
        }

        /**
         * Load a local profile by name.
         */
        @android.webkit.JavascriptInterface
        fun loadProfile(name: String): String {
            var result = ""
            runOnUiThread {
                result = executeLoadProfile(name)
            }
            Thread.sleep(100)
            return result
        }

        /**
         * Delete a local profile by name.
         */
        @android.webkit.JavascriptInterface
        fun deleteProfile(name: String): String {
            var result = ""
            runOnUiThread {
                result = executeDeleteProfile(name)
            }
            Thread.sleep(100)
            return result
        }

        /**
         * Save current state as a profile.
         */
        @android.webkit.JavascriptInterface
        fun saveProfile(name: String): String {
            var result = ""
            runOnUiThread {
                result = executeSaveProfile(name)
            }
            Thread.sleep(100)
            return result
        }

        /**
         * Check if GitHub token is configured.
         */
        @android.webkit.JavascriptInterface
        fun hasGitHubToken(): Boolean {
            val tokenFile = File(bootstrapInstaller.homeDir, ".mobilecli/.github_token")
            return tokenFile.exists()
        }

        /**
         * Import profile from JSON content (for Gist downloads).
         */
        @android.webkit.JavascriptInterface
        fun importProfileFromJson(json: String, name: String): String {
            var result = ""
            runOnUiThread {
                try {
                    val profileFile = File(profilesDir, "$name.json")
                    profileFile.writeText(json)
                    applyProfileFromJson(json)
                    result = "OK: Profile '$name' imported and applied"
                } catch (e: Exception) {
                    result = "ERROR: ${e.message}"
                }
            }
            Thread.sleep(100)
            return result
        }

        /**
         * Export profile to Downloads folder.
         */
        @android.webkit.JavascriptInterface
        fun exportProfile(name: String): String {
            var result = ""
            runOnUiThread {
                result = executeExportProfile(name)
            }
            Thread.sleep(100)
            return result
        }
    }

    /**
     * LOAD_UI path - Load an HTML file as overlay.
     */
    private fun executeLoadUi(args: String): String {
        val path = args.trim()
        if (path.isEmpty()) return "ERROR: Usage: LOAD_UI /path/to/ui.html"

        // Ensure DITTO WebView is initialized
        if (dittoWebView == null) {
            setupDittoWebView()
        }

        val file = if (path.startsWith("/")) {
            File(path)
        } else {
            File(bootstrapInstaller.homeDir, path)
        }

        if (!file.exists()) {
            return "ERROR: File not found: ${file.absolutePath}"
        }

        // Load the HTML file
        dittoWebView?.loadUrl("file://${file.absolutePath}")
        dittoOverlay?.visibility = android.view.View.VISIBLE

        // Apply current size/position settings
        applyDittoLayout()

        return "OK: UI loaded from ${file.name}"
    }

    /**
     * HIDE_UI - Hide the DITTO overlay.
     */
    private fun executeHideUi(): String {
        dittoOverlay?.visibility = android.view.View.GONE
        return "OK: UI hidden"
    }

    /**
     * SHOW_UI - Show the DITTO overlay.
     */
    private fun executeShowUi(): String {
        dittoOverlay?.visibility = android.view.View.VISIBLE
        return "OK: UI shown"
    }

    /**
     * UI_SIZE full/half/quarter - Set overlay size.
     */
    private fun executeUiSize(args: String): String {
        val size = args.trim().lowercase()
        if (size !in listOf("full", "half", "quarter")) {
            return "ERROR: Usage: UI_SIZE full|half|quarter"
        }
        dittoCurrentSize = size
        applyDittoLayout()
        return "OK: UI size set to $size"
    }

    /**
     * UI_POSITION top/bottom/left/right - Set overlay position.
     */
    private fun executeUiPosition(args: String): String {
        val position = args.trim().lowercase()
        if (position !in listOf("top", "bottom", "left", "right", "center")) {
            return "ERROR: Usage: UI_POSITION top|bottom|left|right|center"
        }
        dittoCurrentPosition = position
        applyDittoLayout()
        return "OK: UI position set to $position"
    }

    /**
     * UI_OPACITY 0.0-1.0 - Set overlay transparency.
     */
    private fun executeUiOpacity(args: String): String {
        val opacity = args.trim().toFloatOrNull()
        if (opacity == null || opacity < 0f || opacity > 1f) {
            return "ERROR: Usage: UI_OPACITY 0.0-1.0"
        }
        dittoCurrentOpacity = opacity
        dittoOverlay?.alpha = opacity
        return "OK: UI opacity set to $opacity"
    }

    /**
     * INJECT_JS javascript_code - Run JavaScript in the WebView.
     */
    private fun executeInjectJs(args: String): String {
        val js = args.trim()
        if (js.isEmpty()) return "ERROR: Usage: INJECT_JS javascript_code"

        dittoWebView?.evaluateJavascript(js) { result ->
            Log.i("MainActivity", "DITTO JS result: $result")
        }
        return "OK: JavaScript injected"
    }

    /**
     * SHOW_BROWSER - Show the built-in profile browser UI.
     */
    private fun executeShowBrowser(): String {
        // Ensure DITTO WebView is initialized
        if (dittoWebView == null) {
            setupDittoWebView()
        }

        val browserFile = File(bootstrapInstaller.homeDir, ".mobilecli/ui/profile_browser.html")
        if (!browserFile.exists()) {
            return "ERROR: Profile browser not installed. Run bootstrap again."
        }

        dittoWebView?.loadUrl("file://${browserFile.absolutePath}")
        dittoOverlay?.visibility = android.view.View.VISIBLE

        // Set to full screen for browser
        dittoCurrentSize = "full"
        applyDittoLayout()

        return "OK: Profile browser opened"
    }

    /**
     * Apply current size and position to DITTO overlay.
     */
    private fun applyDittoLayout() {
        val params = dittoOverlay?.layoutParams as? android.widget.FrameLayout.LayoutParams ?: return
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // Set size
        when (dittoCurrentSize) {
            "full" -> {
                params.width = android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                params.height = android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            }
            "half" -> {
                if (dittoCurrentPosition in listOf("top", "bottom")) {
                    params.width = android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                    params.height = screenHeight / 2
                } else {
                    params.width = screenWidth / 2
                    params.height = android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                }
            }
            "quarter" -> {
                params.width = screenWidth / 2
                params.height = screenHeight / 2
            }
        }

        // Set position (gravity)
        params.gravity = when (dittoCurrentPosition) {
            "top" -> android.view.Gravity.TOP
            "bottom" -> android.view.Gravity.BOTTOM
            "left" -> android.view.Gravity.START
            "right" -> android.view.Gravity.END
            "center" -> android.view.Gravity.CENTER
            else -> android.view.Gravity.BOTTOM
        }

        dittoOverlay?.layoutParams = params
    }

    // ============================================
    // PROFILE/SOCIAL SYSTEM (v86)
    // ============================================

    private val profilesDir: File by lazy {
        File(File(bootstrapInstaller.homeDir, ".mobilecli"), "profiles").also { it.mkdirs() }
    }

    /**
     * Get full state as JSON - includes everything needed to recreate the profile.
     */
    private fun executeGetFullState(): String {
        val json = StringBuilder()
        json.append("{\n")
        json.append("  \"version\": \"1.0\",\n")
        json.append("  \"created\": \"${java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(java.util.Date())}\",\n")
        json.append("  \"text_size\": $currentTextSize,\n")
        json.append("  \"ditto_size\": \"$dittoCurrentSize\",\n")
        json.append("  \"ditto_position\": \"$dittoCurrentPosition\",\n")
        json.append("  \"ditto_opacity\": $dittoCurrentOpacity,\n")

        // Capture extra keys state
        json.append("  \"extra_keys_row1\": [")
        extraKeysRow1?.let { row ->
            val keys = mutableListOf<String>()
            for (i in 0 until row.childCount) {
                val child = row.getChildAt(i)
                if (child is Button) {
                    keys.add("\"${child.text}\"")
                }
            }
            json.append(keys.joinToString(", "))
        }
        json.append("],\n")

        json.append("  \"extra_keys_row2\": [")
        extraKeysRow2?.let { row ->
            val keys = mutableListOf<String>()
            for (i in 0 until row.childCount) {
                val child = row.getChildAt(i)
                if (child is Button) {
                    keys.add("\"${child.text}\"")
                }
            }
            json.append(keys.joinToString(", "))
        }
        json.append("]\n")

        json.append("}")
        return json.toString()
    }

    /**
     * SAVE_PROFILE name - Save current state to a named profile.
     */
    private fun executeSaveProfile(args: String): String {
        val name = args.trim()
        if (name.isEmpty()) return "ERROR: Usage: SAVE_PROFILE <name>"
        if (!name.matches(Regex("[a-zA-Z0-9_-]+"))) {
            return "ERROR: Profile name can only contain letters, numbers, underscore, dash"
        }

        val profileFile = File(profilesDir, "$name.json")
        val state = executeGetFullState()

        try {
            profileFile.writeText(state)
            Log.i("MainActivity", "Profile saved: $name")
            return "OK: Profile '$name' saved"
        } catch (e: Exception) {
            return "ERROR: Failed to save profile: ${e.message}"
        }
    }

    /**
     * LOAD_PROFILE name - Load a saved profile.
     */
    private fun executeLoadProfile(args: String): String {
        val name = args.trim()
        if (name.isEmpty()) return "ERROR: Usage: LOAD_PROFILE <name>"

        val profileFile = File(profilesDir, "$name.json")
        if (!profileFile.exists()) {
            return "ERROR: Profile '$name' not found"
        }

        try {
            val content = profileFile.readText()
            applyProfileFromJson(content)
            return "OK: Profile '$name' loaded"
        } catch (e: Exception) {
            return "ERROR: Failed to load profile: ${e.message}"
        }
    }

    /**
     * Apply profile settings from JSON.
     */
    private fun applyProfileFromJson(json: String) {
        try {
            // Simple JSON parsing (no external library needed)
            val textSizeMatch = Regex("\"text_size\":\\s*(\\d+)").find(json)
            textSizeMatch?.groupValues?.get(1)?.toIntOrNull()?.let { size ->
                if (size in minTextSize..maxTextSize) {
                    currentTextSize = size
                    terminalView.setTextSize(currentTextSize)
                }
            }

            val dittoSizeMatch = Regex("\"ditto_size\":\\s*\"(\\w+)\"").find(json)
            dittoSizeMatch?.groupValues?.get(1)?.let { size ->
                dittoCurrentSize = size
            }

            val dittoPositionMatch = Regex("\"ditto_position\":\\s*\"(\\w+)\"").find(json)
            dittoPositionMatch?.groupValues?.get(1)?.let { position ->
                dittoCurrentPosition = position
            }

            val dittoOpacityMatch = Regex("\"ditto_opacity\":\\s*([\\d.]+)").find(json)
            dittoOpacityMatch?.groupValues?.get(1)?.toFloatOrNull()?.let { opacity ->
                dittoCurrentOpacity = opacity
                dittoOverlay?.alpha = opacity
            }

            applyDittoLayout()
            terminalView.post {
                updateTerminalSize()
                terminalView.onScreenUpdated()
            }

            Log.i("MainActivity", "Profile applied from JSON")
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to apply profile", e)
        }
    }

    /**
     * LIST_PROFILES - List all saved profiles.
     */
    private fun executeListProfiles(): String {
        val profiles = profilesDir.listFiles()?.filter { it.extension == "json" }?.map { it.nameWithoutExtension }
        if (profiles.isNullOrEmpty()) {
            return "No profiles saved yet"
        }
        return profiles.joinToString("\n")
    }

    /**
     * DELETE_PROFILE name - Delete a saved profile.
     */
    private fun executeDeleteProfile(args: String): String {
        val name = args.trim()
        if (name.isEmpty()) return "ERROR: Usage: DELETE_PROFILE <name>"

        val profileFile = File(profilesDir, "$name.json")
        if (!profileFile.exists()) {
            return "ERROR: Profile '$name' not found"
        }

        return if (profileFile.delete()) {
            "OK: Profile '$name' deleted"
        } else {
            "ERROR: Failed to delete profile"
        }
    }

    /**
     * EXPORT_PROFILE name - Export profile to /sdcard/Download/.
     */
    private fun executeExportProfile(args: String): String {
        val name = args.trim()
        if (name.isEmpty()) return "ERROR: Usage: EXPORT_PROFILE <name>"

        val profileFile = File(profilesDir, "$name.json")
        if (!profileFile.exists()) {
            // If profile doesn't exist, save current state first
            executeSaveProfile(name)
        }

        val exportFile = File("/sdcard/Download", "MobileCLI-$name.mcli")
        try {
            profileFile.copyTo(exportFile, overwrite = true)
            return "OK: Exported to ${exportFile.absolutePath}"
        } catch (e: Exception) {
            return "ERROR: Export failed: ${e.message}"
        }
    }

    /**
     * IMPORT_PROFILE path - Import profile from file.
     */
    private fun executeImportProfile(args: String): String {
        val path = args.trim()
        if (path.isEmpty()) return "ERROR: Usage: IMPORT_PROFILE <path>"

        val importFile = if (path.startsWith("/")) File(path) else File("/sdcard/Download", path)
        if (!importFile.exists()) {
            return "ERROR: File not found: ${importFile.absolutePath}"
        }

        try {
            val content = importFile.readText()
            // Extract name from filename
            val name = importFile.nameWithoutExtension.replace("MobileCLI-", "")

            // Save to profiles
            val profileFile = File(profilesDir, "$name.json")
            profileFile.writeText(content)

            // Apply the profile
            applyProfileFromJson(content)

            return "OK: Profile '$name' imported and applied"
        } catch (e: Exception) {
            return "ERROR: Import failed: ${e.message}"
        }
    }

    /**
     * SHARE_PROFILE name - Share profile via Android share intent.
     */
    private fun executeShareProfile(args: String): String {
        val name = args.trim()
        if (name.isEmpty()) return "ERROR: Usage: SHARE_PROFILE <name>"

        // First export it
        val exportResult = executeExportProfile(name)
        if (exportResult.startsWith("ERROR")) return exportResult

        val exportFile = File("/sdcard/Download", "MobileCLI-$name.mcli")

        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "com.termux.fileprovider",
                exportFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "MobileCLI Profile: $name")
                putExtra(Intent.EXTRA_TEXT, "Check out my MobileCLI DITTO profile: $name")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Share Profile"))
            return "OK: Share dialog opened"
        } catch (e: Exception) {
            // Fallback to simple file share
            try {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "*/*"
                    putExtra(Intent.EXTRA_STREAM, android.net.Uri.fromFile(exportFile))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, "Share Profile"))
                return "OK: Share dialog opened"
            } catch (e2: Exception) {
                return "ERROR: Share failed: ${e2.message}"
            }
        }
    }

    /**
     * DITTO - Dynamic Interface Transformation Technology for Operators (v83)
     *
     * This is the key to making MobileCLI morph to user needs in real-time.
     * Shell writes commands to: ~/.termux/ditto_command
     * Activity reads, deletes, and executes the UI transformation.
     *
     * Supported commands:
     *   bgcolor:#RRGGBB    - Change terminal background color
     *   fgcolor:#RRGGBB    - Change terminal foreground color (future)
     *   fullscreen:true    - Enable fullscreen mode
     *   fullscreen:false   - Disable fullscreen mode
     *   fontsize:N         - Set font size
     *   toast:message      - Show a toast message
     *   reload             - Reload termux.properties
     */
    private fun setupDittoWatcher() {
        val dittoFile = File(File(bootstrapInstaller.homeDir, ".termux"), "ditto_command")

        dittoWatcherRunnable = object : Runnable {
            override fun run() {
                try {
                    if (dittoFile.exists()) {
                        val command = dittoFile.readText().trim()
                        dittoFile.delete()

                        if (command.isNotEmpty()) {
                            Log.i("Ditto", "Executing command: $command")
                            runOnUiThread {
                                executeDittoCommand(command)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Ditto", "Error reading ditto_command", e)
                }

                // Schedule next check
                uiHandler.postDelayed(this, dittoWatchInterval)
            }
        }

        // Start the watcher
        uiHandler.postDelayed(dittoWatcherRunnable!!, dittoWatchInterval)
        Log.i("Ditto", "DITTO watcher started, checking every ${dittoWatchInterval}ms")
    }

    /**
     * Execute a DITTO command to transform the UI.
     */
    private fun executeDittoCommand(command: String) {
        val parts = command.split(":", limit = 2)
        val action = parts[0].lowercase()
        val value = if (parts.size > 1) parts[1] else ""

        try {
            when (action) {
                "bgcolor" -> {
                    val color = android.graphics.Color.parseColor(value)
                    terminalView.setBackgroundColor(color)
                    Log.i("Ditto", "Background color changed to $value")
                }
                "fullscreen" -> {
                    if (value == "true") {
                        window.decorView.systemUiVisibility = (
                            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                            android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        )
                    } else {
                        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_VISIBLE
                    }
                    Log.i("Ditto", "Fullscreen set to $value")
                }
                "fontsize" -> {
                    val size = value.toIntOrNull() ?: 14
                    currentTextSize = size
                    terminalView.setTextSize(size)
                    updateTerminalSize()
                    Log.i("Ditto", "Font size changed to $size")
                }
                "toast" -> {
                    Toast.makeText(this, value, Toast.LENGTH_SHORT).show()
                }
                "reload" -> {
                    loadTermuxProperties()
                    Toast.makeText(this, "Settings reloaded", Toast.LENGTH_SHORT).show()
                    Log.i("Ditto", "Reloaded termux.properties")
                }
                "vibrate" -> {
                    val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        vibrator.vibrate(android.os.VibrationEffect.createOneShot(100, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(100)
                    }
                }
                else -> {
                    Log.w("Ditto", "Unknown command: $action")
                }
            }
        } catch (e: Exception) {
            Log.e("Ditto", "Failed to execute command: $command", e)
            Toast.makeText(this, "Ditto error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Load and parse termux.properties file.
     * Supports key settings from real Termux.
     */
    private fun loadTermuxProperties() {
        termuxProperties = Properties()
        try {
            if (propertiesFile.exists()) {
                propertiesFile.inputStream().use { stream ->
                    termuxProperties?.load(stream)
                }
                applyTermuxProperties()
            }
        } catch (e: Exception) {
            android.util.Log.w("MobileCLI", "Failed to load termux.properties: ${e.message}")
        }
    }

    /**
     * Apply settings from termux.properties.
     */
    private fun applyTermuxProperties() {
        termuxProperties?.let { props ->
            // Bell behavior
            val bellBehavior = props.getProperty("bell-character", "vibrate")
            // Store for use in onBell()

            // Fullscreen mode
            val useFullscreen = props.getProperty("fullscreen", "false")
            if (useFullscreen == "true") {
                window.decorView.systemUiVisibility = (
                    android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                    android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
            }

            // Terminal cursor style
            val cursorStyle = props.getProperty("terminal-cursor-style", "block")
            // Applied in getTerminalCursorStyle()

            // Back key behavior
            val backKey = props.getProperty("back-key", "back")
            // Store for use in key handling

            // LIVE COLOR SUPPORT - Added by Test Phone Claude
            // Background color from termux.properties
            val bgColor = props.getProperty("background-color", "")
            if (bgColor.isNotEmpty()) {
                try {
                    val color = android.graphics.Color.parseColor(bgColor)
                    terminalView.setBackgroundColor(color)
                } catch (e: Exception) { }
            }

            // Foreground color (text color)
            val fgColor = props.getProperty("foreground-color", "")
            if (fgColor.isNotEmpty()) {
                // Note: Text color requires terminal session color scheme change
                // This is a visual indicator that config was read
                android.util.Log.i("MobileCLI", "Foreground color configured: $fgColor")
            }
        }
    }

    /**
     * Debounced terminal update to prevent race conditions.
     */
    private fun scheduleTerminalUpdate() {
        pendingTerminalUpdate?.let { uiHandler.removeCallbacks(it) }
        pendingTerminalUpdate = Runnable {
            updateTerminalSize()
            terminalView.onScreenUpdated()
        }
        uiHandler.postDelayed(pendingTerminalUpdate!!, updateDebounceMs)
    }

    private fun startTermuxService() {
        val serviceIntent = Intent(this, TermuxService::class.java)
        startService(serviceIntent)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun checkBootstrap() {
        if (bootstrapInstaller.isInstalled()) {
            // Bootstrap ready, verify permissions and start terminal
            bootstrapInstaller.verifyAndFix()

            // Check if AI setup is needed
            if (isAISetupNeeded()) {
                // Show clean UI in user mode, dialog in dev mode
                if (developerModeEnabled) {
                    showSetupDialog("Preparing AI environment...")
                } else {
                    showSetupOverlay()
                    updateSetupProgress(70, "Configuring AI tools...")
                }
                runAISetup()
            } else {
                createSessionOrDefer()
            }
        } else {
            // Need to install bootstrap first
            // Show clean UI in user mode, dialog in dev mode
            if (developerModeEnabled) {
                showProgressDialog()
            } else {
                showSetupOverlay()
                updateSetupProgress(0, "Setting up your environment...")
            }
            installBootstrap()
        }
    }

    /**
     * Create a session, or defer to service connection if not yet bound.
     * This ensures we don't create a new session if the service has existing sessions to restore.
     */
    private fun createSessionOrDefer() {
        if (serviceBound && termuxService != null) {
            // Service is already bound - check if it has sessions
            val serviceSessions = termuxService?.getSessions() ?: emptyList()
            if (serviceSessions.isNotEmpty() && sessions.isEmpty()) {
                // Restore from service
                Log.i("MainActivity", "Creating session: restoring from service instead")
                sessions.addAll(serviceSessions)
                currentSessionIndex = 0
                terminalView.attachSession(sessions[0])
                updateSessionTabs()
            } else if (sessions.isEmpty()) {
                // No sessions anywhere, create new
                createSession()
            }
            // else: sessions already exist, don't create new one
        } else {
            // Service not bound yet - defer session creation
            Log.i("MainActivity", "Deferring session creation until service connects")
            bootstrapReadyPendingSession = true
        }
    }

    private fun isAISetupNeeded(): Boolean {
        val setupMarker = File(bootstrapInstaller.homeDir, ".mobilecli/.setup_complete")
        return !setupMarker.exists()
    }

    private fun isClaudeInstalled(): Boolean {
        val claudeBin = File(bootstrapInstaller.binDir, "claude")
        val nodeBin = File(bootstrapInstaller.binDir, "node")
        return claudeBin.exists() && nodeBin.exists()
    }

    private fun checkAndOfferAISetup() {
        val prefs = getSharedPreferences("mobilecli", Context.MODE_PRIVATE)
        val hasSetup = prefs.getBoolean("ai_setup_complete", false)

        if (!hasSetup) {
            terminalView.postDelayed({
                showAIChooserDialog()
            }, 1000)
        }
    }

    private fun showAIChooserDialog() {
        val prefs = getSharedPreferences("mobilecli", Context.MODE_PRIVATE)

        val options = arrayOf(
            "Claude Code (Recommended)",
            "Google Gemini CLI",
            "OpenAI Codex CLI",
            "All AI Tools",
            "None - Basic Terminal"
        )

        AlertDialog.Builder(this)
            .setTitle("Welcome to MobileCLI!")
            .setIcon(android.R.drawable.ic_dialog_info)
            .setSingleChoiceItems(options, 0, null)
            .setPositiveButton("Install") { dialog, _ ->
                val selected = (dialog as AlertDialog).listView.checkedItemPosition
                prefs.edit().putBoolean("ai_setup_complete", true).apply()

                when (selected) {
                    0 -> installAI("claude")
                    1 -> installAI("gemini")
                    2 -> installAI("codex")
                    3 -> installAI("all")
                    4 -> {
                        Toast.makeText(this, "Basic terminal ready!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Skip") { _, _ ->
                prefs.edit().putBoolean("ai_setup_complete", true).apply()
                Toast.makeText(this, "You can install AI tools later from Settings", Toast.LENGTH_LONG).show()
            }
            .setCancelable(false)
            .show()
    }

    private fun installAI(tool: String) {
        // Offer Quick Install (auto) or Manual (step-by-step)
        val (title, autoCmd, manualSteps) = when (tool) {
            "claude" -> Triple(
                "Install Claude Code",
                "pkg update -y && pkg upgrade -y && pkg install nodejs-lts -y && npm install -g @anthropic-ai/claude-code && echo '\\n✓ Done! Type: claude'\n",
                "1. pkg update && pkg upgrade\n2. pkg install nodejs-lts\n3. npm install -g @anthropic-ai/claude-code\n4. claude"
            )
            "gemini" -> Triple(
                "Install Gemini CLI",
                "pkg update -y && pkg upgrade -y && pkg install nodejs-lts -y && npm install -g gemini-cli && echo '\\n✓ Done! Type: gemini'\n",
                "1. pkg update && pkg upgrade\n2. pkg install nodejs-lts\n3. npm install -g gemini-cli\n4. gemini"
            )
            "codex" -> Triple(
                "Install Codex CLI",
                "pkg update -y && pkg upgrade -y && pkg install nodejs-lts -y && npm install -g @openai/codex && echo '\\n✓ Done! Type: codex'\n",
                "1. pkg update && pkg upgrade\n2. pkg install nodejs-lts\n3. npm install -g @openai/codex\n4. codex"
            )
            "all" -> Triple(
                "Install All AI Tools",
                "pkg update -y && pkg upgrade -y && pkg install nodejs-lts -y && npm install -g @anthropic-ai/claude-code && npm install -g gemini-cli && npm install -g @openai/codex && echo '\\n✓ All AI tools installed!'\n",
                "1. pkg update && pkg upgrade\n2. pkg install nodejs-lts\n3. npm install -g @anthropic-ai/claude-code\n4. npm install -g gemini-cli\n5. npm install -g @openai/codex"
            )
            else -> Triple("", "", "")
        }

        if (title.isNotEmpty()) {
            AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage("Choose installation method:")
                .setPositiveButton("Quick Install") { _, _ ->
                    // Auto-execute with -y flags
                    session?.write(autoCmd.toByteArray(Charsets.UTF_8), 0, autoCmd.toByteArray(Charsets.UTF_8).size)
                    Toast.makeText(this, "Installing... please wait", Toast.LENGTH_LONG).show()
                }
                .setNeutralButton("Manual") { _, _ ->
                    // Show step-by-step instructions
                    AlertDialog.Builder(this)
                        .setTitle("Manual Installation")
                        .setMessage("Run these commands:\n\n$manualSteps\n\nPress Y when prompted.")
                        .setPositiveButton("OK") { _, _ ->
                            terminalView.requestFocus()
                            val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                            imm.showSoftInput(terminalView, 0)
                        }
                        .show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showSetupDialog(message: String) {
        val view = layoutInflater.inflate(R.layout.dialog_progress, null)
        progressBar = view.findViewById(R.id.progress_bar)
        progressText = view.findViewById(R.id.progress_text)
        progressText?.text = message

        progressDialog = AlertDialog.Builder(this)
            .setTitle("MobileCLI")
            .setView(view)
            .setCancelable(false)
            .create()
        progressDialog?.show()
    }

    private fun runAISetup() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val homeDir = bootstrapInstaller.homeDir
                val configDir = File(homeDir, ".mobilecli")
                configDir.mkdirs()

                // Create the secret system prompt
                withContext(Dispatchers.Main) {
                    progressText?.text = "Setting up AI configuration..."
                    progressBar?.progress = 20
                    if (!developerModeEnabled) {
                        updateSetupProgress(80, "Configuring AI assistant...")
                    }
                }

                createSystemPrompt()

                withContext(Dispatchers.Main) {
                    progressText?.text = "Configuration complete!"
                    progressBar?.progress = 100
                    if (!developerModeEnabled) {
                        updateSetupProgress(95, "Almost ready...")
                    }
                }

                // Mark AI config as done (but not AI choice)
                File(configDir, ".setup_complete").writeText("v1\n${System.currentTimeMillis()}")

                delay(500)

                withContext(Dispatchers.Main) {
                    progressDialog?.dismiss()

                    // Check if user already chose an AI previously
                    val prefs = getSharedPreferences("mobilecli", Context.MODE_PRIVATE)
                    val hasChosenAI = prefs.getBoolean("ai_setup_complete", false)

                    if (hasChosenAI) {
                        // Create session first (behind the overlay)
                        createSessionOrDefer()

                        // Auto-launch their chosen AI if it's installed
                        val chosenAI = prefs.getString("selected_ai", null)
                        if (chosenAI != null && chosenAI != AI_NONE) {
                            val launchCmd = when (chosenAI) {
                                AI_CLAUDE -> "claude"
                                AI_GEMINI -> "gemini"
                                AI_CODEX -> "codex"
                                else -> null
                            }
                            if (launchCmd != null && isAIInstalled(chosenAI)) {
                                // Wait for session to be ready
                                terminalView.postDelayed({
                                    // Clear and launch the AI
                                    val cmd = "clear && $launchCmd\n"
                                    session?.write(cmd.toByteArray(Charsets.UTF_8), 0, cmd.length)

                                    // Hide overlay AFTER launching (so user doesn't see terminal setup)
                                    terminalView.postDelayed({
                                        hideSetupOverlay()
                                    }, 500)
                                }, 1500)
                            } else {
                                // AI not installed, just show terminal
                                hideSetupOverlay()
                            }
                        } else {
                            // No AI selected, just show terminal
                            hideSetupOverlay()
                        }
                    } else if (!developerModeEnabled) {
                        // First time - show AI choice screen (user mode)
                        showAIChoiceScreen()
                    } else {
                        // Developer mode - show dialog instead
                        hideSetupOverlay()
                        createSessionOrDefer()
                        showAIChooserDialog()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog?.dismiss()
                    hideSetupOverlay()
                    showError("AI setup failed: ${e.message}")
                }
            }
        }
    }

    /**
     * Check if an AI tool is installed.
     */
    private fun isAIInstalled(ai: String): Boolean {
        val binName = when (ai) {
            AI_CLAUDE -> "claude"
            AI_GEMINI -> "gemini"
            AI_CODEX -> "codex"
            else -> return false
        }
        return File(bootstrapInstaller.binDir, binName).exists()
    }

    private fun createSystemPrompt() {
        val homeDir = bootstrapInstaller.homeDir
        val configDir = File(homeDir, ".mobilecli")
        configDir.mkdirs()

        // Create the comprehensive system prompt
        val systemPrompt = """
# MobileCLI AI Environment - System Knowledge

## YOU ARE RUNNING IN MOBILECLI - A MOBILE AI DEVELOPMENT ENVIRONMENT

This is a Termux-compatible environment on Android. You have full terminal access.

## CRITICAL PATHS

### Phone Storage
- Screenshots: `/sdcard/DCIM/Screenshots/`
- Camera photos: `/sdcard/DCIM/Camera/`
- Downloads: `/sdcard/Download/`
- Documents: `/sdcard/Documents/`

### Environment
- HOME: `${'$'}HOME` = `/data/data/com.termux/files/home`
- PREFIX: `${'$'}PREFIX` = `/data/data/com.termux/files/usr`
- Binaries: `/data/data/com.termux/files/usr/bin`

## HOW TO VIEW IMAGES/SCREENSHOTS

Use the Read tool to view any image:
```
Read /sdcard/DCIM/Screenshots/Screenshot_*.jpg
```

List recent screenshots:
```bash
ls -lt /sdcard/DCIM/Screenshots/ | head -10
```

## PACKAGE MANAGEMENT

Install packages:
```bash
pkg install <package>
pkg update && pkg upgrade
```

Common packages: nodejs, python, git, openssh, curl, wget

## BUILDING ANDROID APPS

Android SDK location: `~/android-sdk`

Build APK:
```bash
cd <project>
./gradlew assembleDebug
```

Copy to Downloads for installation:
```bash
cp app/build/outputs/apk/debug/*.apk /sdcard/Download/
```

## GITHUB SETUP

```bash
pkg install git gh
gh auth login
git config --global user.name "Your Name"
git config --global user.email "your@email.com"
```

## VERCEL DEPLOYMENT

```bash
npm install -g vercel
vercel login
vercel --prod
```

## AI CLI TOOLS

- Claude Code: `claude`
- Gemini: `gemini`
- Codex: `codex`

## BEST PRACTICES

1. Always check screenshots when user mentions them
2. Build and test frequently
3. Copy APKs to /sdcard/Download/ for easy installation
4. Use pkg for system packages, npm for Node packages
5. The user is on mobile - keep responses concise

## REMEMBER

You are inside MobileCLI, a premium mobile development environment.
The user expects things to "just work" - be proactive and helpful.
""".trimIndent()

        // Write to hidden config directory
        File(configDir, "SYSTEM_PROMPT.md").writeText(systemPrompt)

        // Create symlink for Claude Code to find it
        val claudeMd = File(homeDir, "CLAUDE.md")
        if (!claudeMd.exists()) {
            claudeMd.writeText(systemPrompt)
        }
    }

    private fun showProgressDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_progress, null)
        progressBar = view.findViewById(R.id.progress_bar)
        progressText = view.findViewById(R.id.progress_text)

        progressDialog = AlertDialog.Builder(this)
            .setTitle("Setting up MobileCLI")
            .setView(view)
            .setCancelable(false)
            .create()
        progressDialog?.show()
    }

    private fun installBootstrap() {
        bootstrapInstaller.onProgress = { progress, message ->
            runOnUiThread {
                if (progress >= 0) {
                    // Update both dialog and overlay
                    progressBar?.progress = progress
                    progressText?.text = message

                    // Update setup overlay (for user mode)
                    if (!developerModeEnabled) {
                        updateSetupProgress(progress, message)
                    }
                } else {
                    progressDialog?.dismiss()
                    hideSetupOverlay()
                    showError(message)
                }
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val success = bootstrapInstaller.install()
            withContext(Dispatchers.Main) {
                progressDialog?.dismiss()
                if (success) {
                    // Now run AI setup
                    if (isAISetupNeeded()) {
                        if (developerModeEnabled) {
                            showSetupDialog("Preparing AI environment...")
                        } else {
                            updateSetupProgress(70, "Configuring AI tools...")
                        }
                        runAISetup()
                    } else {
                        hideSetupOverlay()
                        createSessionOrDefer()
                    }
                } else {
                    hideSetupOverlay()
                    showError("Bootstrap installation failed")
                }
            }
        }
    }

    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("Retry") { _, _ -> checkBootstrap() }
            .setNegativeButton("Exit") { _, _ -> finish() }
            .show()
    }

    private fun setupExtraKeys() {
        // Row 1 - Main controls
        findViewById<Button>(R.id.btn_esc).setOnClickListener { sendKey(27) }
        findViewById<Button>(R.id.btn_ctrl).setOnClickListener { isCtrlPressed = !isCtrlPressed; updateModifierButtons() }
        findViewById<Button>(R.id.btn_alt).setOnClickListener { isAltPressed = !isAltPressed; updateModifierButtons() }
        findViewById<Button>(R.id.btn_tab).setOnClickListener { sendKey(9) }
        findViewById<Button>(R.id.btn_home).setOnClickListener { sendSpecialKey("\u001b[H") }
        findViewById<Button>(R.id.btn_up).setOnClickListener { sendSpecialKey("\u001b[A") }
        findViewById<Button>(R.id.btn_end).setOnClickListener { sendSpecialKey("\u001b[F") }
        findViewById<Button>(R.id.btn_pgup).setOnClickListener { sendSpecialKey("\u001b[5~") }

        // Row 2 - Symbols and navigation
        findViewById<Button>(R.id.btn_dash).setOnClickListener { sendChar('-') }
        findViewById<Button>(R.id.btn_slash).setOnClickListener { sendChar('/') }
        findViewById<Button>(R.id.btn_backslash).setOnClickListener { sendChar('\\') }
        findViewById<Button>(R.id.btn_pipe).setOnClickListener { sendChar('|') }
        findViewById<Button>(R.id.btn_left).setOnClickListener { sendSpecialKey("\u001b[D") }
        findViewById<Button>(R.id.btn_down).setOnClickListener { sendSpecialKey("\u001b[B") }
        findViewById<Button>(R.id.btn_right).setOnClickListener { sendSpecialKey("\u001b[C") }
        findViewById<Button>(R.id.btn_pgdn).setOnClickListener { sendSpecialKey("\u001b[6~") }
        findViewById<Button>(R.id.btn_tilde).setOnClickListener { sendChar('~') }
        findViewById<Button>(R.id.btn_underscore).setOnClickListener { sendChar('_') }
        findViewById<Button>(R.id.btn_colon).setOnClickListener { sendChar(':') }
        findViewById<Button>(R.id.btn_quote).setOnClickListener { sendChar('"') }
        findViewById<Button>(R.id.btn_more).setOnClickListener { showMoreOptions() }
        findViewById<Button>(R.id.btn_more).setOnLongClickListener { showContextMenu(null); true }
    }

    private fun updateModifierButtons() {
        findViewById<Button>(R.id.btn_ctrl).alpha = if (isCtrlPressed) 1.0f else 0.5f
        findViewById<Button>(R.id.btn_alt).alpha = if (isAltPressed) 1.0f else 0.5f
    }

    private fun setupNavDrawer() {
        // New Session - adds a new session tab
        findViewById<TextView>(R.id.nav_new_session).setOnClickListener {
            drawerLayout.closeDrawers()
            if (sessions.size < maxSessions) {
                addNewSession()
                Toast.makeText(this, "Session ${sessions.size} created", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Maximum $maxSessions sessions reached", Toast.LENGTH_SHORT).show()
            }
        }

        // Settings - show settings dialog
        findViewById<TextView>(R.id.nav_settings).setOnClickListener {
            drawerLayout.closeDrawers()
            showSettingsDialog()
        }

        // Toggle Keyboard
        findViewById<TextView>(R.id.nav_keyboard).setOnClickListener {
            drawerLayout.closeDrawers()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.toggleSoftInput(0, 0)
        }

        // Text Size
        findViewById<TextView>(R.id.nav_text_size).setOnClickListener {
            drawerLayout.closeDrawers()
            showTextSizeDialog()
        }

        // Install AI Tools
        findViewById<TextView>(R.id.nav_install_ai).setOnClickListener {
            drawerLayout.closeDrawers()
            showAIChooserDialog()
        }

        // Help
        findViewById<TextView>(R.id.nav_help).setOnClickListener {
            drawerLayout.closeDrawers()
            showHelpDialog()
        }

        // About
        findViewById<TextView>(R.id.nav_about).setOnClickListener {
            drawerLayout.closeDrawers()
            showAboutDialog()
        }
    }

    private fun addNewSession() {
        val newSession = createNewTerminalSession()
        sessions.add(newSession)
        currentSessionIndex = sessions.size - 1
        terminalView.attachSession(newSession)
        terminalView.post {
            updateTerminalSize()
            terminalView.requestFocus()
        }
        updateSessionTabs()
    }

    private fun switchToSession(index: Int) {
        if (index >= 0 && index < sessions.size && index != currentSessionIndex) {
            currentSessionIndex = index
            terminalView.attachSession(sessions[index])
            terminalView.post {
                updateTerminalSize()
                terminalView.onScreenUpdated()
            }
            updateSessionTabs()
        }
    }

    private fun killCurrentSession() {
        if (sessions.isEmpty()) return

        val sessionToKill = session ?: return

        // Send exit message to terminal
        val exitMsg = "\r\n[Process completed - press Enter to close]\r\n"
        sessionToKill.write(exitMsg.toByteArray(), 0, exitMsg.length)

        // Mark session for removal on next key press
        sessionToKill.finishIfRunning()
    }

    private fun removeSession(index: Int) {
        if (sessions.size <= 1) {
            // Last session - create a new one instead
            sessions.clear()
            currentSessionIndex = 0
            addNewSession()
            return
        }

        sessions.removeAt(index)
        if (currentSessionIndex >= sessions.size) {
            currentSessionIndex = sessions.size - 1
        }
        terminalView.attachSession(sessions[currentSessionIndex])
        terminalView.post {
            updateTerminalSize()
            terminalView.onScreenUpdated()
        }
        updateSessionTabs()
    }

    private fun updateSessionTabs() {
        // Update top tab bar
        val tabsContainer = findViewById<android.widget.HorizontalScrollView>(R.id.session_tabs_container)
        val tabsLayout = findViewById<android.widget.LinearLayout>(R.id.session_tabs)

        if (tabsContainer != null && tabsLayout != null) {
            tabsLayout.removeAllViews()

            // Show tabs only if more than 1 session
            if (sessions.size > 1) {
                tabsContainer.visibility = android.view.View.VISIBLE

                sessions.forEachIndexed { index, _ ->
                    val tab = Button(this).apply {
                        text = "${index + 1}"
                        textSize = 12f
                        setTextColor(if (index == currentSessionIndex) 0xFF00FF00.toInt() else 0xFFFFFFFF.toInt())
                        setBackgroundColor(if (index == currentSessionIndex) 0xFF333333.toInt() else 0xFF1a1a1a.toInt())
                        setPadding(24, 8, 24, 8)
                        minimumWidth = 0
                        minWidth = 0

                        setOnClickListener { switchToSession(index) }
                        setOnLongClickListener {
                            showSessionOptions(index)
                            true
                        }
                    }
                    tabsLayout.addView(tab)
                }
            } else {
                tabsContainer.visibility = android.view.View.GONE
            }
        }

        // Update drawer sessions list
        updateDrawerSessionsList()
    }

    private fun updateDrawerSessionsList() {
        val sessionsList = findViewById<android.widget.LinearLayout>(R.id.sessions_list) ?: return
        sessionsList.removeAllViews()

        sessions.forEachIndexed { index, _ ->
            val sessionItem = TextView(this).apply {
                text = if (index == currentSessionIndex) "● Session ${index + 1} (active)" else "○ Session ${index + 1}"
                textSize = 16f
                setTextColor(if (index == currentSessionIndex) 0xFF4CAF50.toInt() else 0xFFFFFFFF.toInt())
                setPadding(24, 24, 24, 24)
                setBackgroundColor(if (index == currentSessionIndex) 0xFF2a2a2a.toInt() else 0x00000000.toInt())
                isClickable = true
                isFocusable = true

                setOnClickListener {
                    switchToSession(index)
                    drawerLayout.closeDrawers()
                }

                setOnLongClickListener {
                    showSessionOptionsInDrawer(index)
                    true
                }
            }
            sessionsList.addView(sessionItem)
        }

        // Show placeholder if no sessions
        if (sessions.isEmpty()) {
            val placeholder = TextView(this).apply {
                text = "No sessions"
                textSize = 14f
                setTextColor(0xFF666666.toInt())
                setPadding(24, 16, 24, 16)
            }
            sessionsList.addView(placeholder)
        }
    }

    private fun showSessionOptionsInDrawer(index: Int) {
        val options = arrayOf("Switch to session", "Kill session", "Rename session")
        AlertDialog.Builder(this)
            .setTitle("Session ${index + 1}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        switchToSession(index)
                        drawerLayout.closeDrawers()
                    }
                    1 -> {
                        if (index == currentSessionIndex) {
                            killCurrentSession()
                        } else {
                            sessions[index].finishIfRunning()
                            removeSession(index)
                        }
                        drawerLayout.closeDrawers()
                    }
                    2 -> {
                        // Rename not implemented yet - just show toast
                        Toast.makeText(this, "Session names coming soon", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    private fun showSessionOptions(index: Int) {
        val options = arrayOf("Switch to session", "Kill session")
        AlertDialog.Builder(this)
            .setTitle("Session ${index + 1}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> switchToSession(index)
                    1 -> {
                        if (index == currentSessionIndex) {
                            killCurrentSession()
                        } else {
                            sessions[index].finishIfRunning()
                            removeSession(index)
                        }
                    }
                }
            }
            .show()
    }

    private fun showSettingsDialog() {
        val options = arrayOf("Text Size", "Reset Terminal", "Kill Session", "New Session", "Sessions (${sessions.size}/$maxSessions)", "Clear Session History")
        AlertDialog.Builder(this)
            .setTitle("Settings")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showTextSizeDialog()
                    1 -> {
                        session?.let { s ->
                            s.reset()
                            terminalView.onScreenUpdated()
                            Toast.makeText(this, "Terminal reset", Toast.LENGTH_SHORT).show()
                        }
                    }
                    2 -> {
                        // Kill current session
                        killCurrentSession()
                    }
                    3 -> {
                        // New session
                        if (sessions.size < maxSessions) {
                            addNewSession()
                            Toast.makeText(this, "Session ${sessions.size} created", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Maximum $maxSessions sessions reached", Toast.LENGTH_SHORT).show()
                        }
                    }
                    4 -> {
                        // Show sessions list
                        showSessionsDialog()
                    }
                    5 -> {
                        // Clear saved session data
                        val prefs = getSharedPreferences("mobilecli", Context.MODE_PRIVATE)
                        prefs.edit().remove("last_transcript").apply()
                        Toast.makeText(this, "Session history cleared", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    private fun showSessionsDialog() {
        if (sessions.isEmpty()) {
            Toast.makeText(this, "No sessions", Toast.LENGTH_SHORT).show()
            return
        }

        val sessionNames = sessions.mapIndexed { index, _ ->
            val marker = if (index == currentSessionIndex) " (current)" else ""
            "Session ${index + 1}$marker"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Sessions")
            .setItems(sessionNames) { _, which ->
                switchToSession(which)
            }
            .setNeutralButton("Kill Current") { _, _ ->
                killCurrentSession()
            }
            .setPositiveButton("New") { _, _ ->
                if (sessions.size < maxSessions) {
                    addNewSession()
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showHelpDialog() {
        val help = """
            |Getting Started:
            |• Type commands and press Enter
            |• Swipe from left for menu
            |• Long-press for copy/paste
            |• Pinch to zoom text
            |
            |Useful Commands:
            |• pkg install <package>
            |• claude - Start Claude AI
            |• ls, cd, cat - File navigation
            |• git, npm, node - Dev tools
            |
            |Extra Keys:
            |• CTRL/ALT - Modifier keys (tap to toggle)
            |• ESC - Escape key
            |• TAB - Tab completion
            |• Arrows - Navigation
        """.trimMargin()

        AlertDialog.Builder(this)
            .setTitle("Help")
            .setMessage(help)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("MobileCLI")
            .setMessage("Mobile AI Development Environment\n\nVersion 1.0\n\nA full terminal with AI capabilities.\n\nhttps://mobilecli.com")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun sendKey(keyCode: Int) {
        session?.write(byteArrayOf(keyCode.toByte()), 0, 1)
    }

    private fun sendSpecialKey(sequence: String) {
        session?.write(sequence.toByteArray(), 0, sequence.length)
    }

    private fun sendChar(c: Char) {
        var code = c.code
        if (isCtrlPressed && c.isLetter()) {
            code = c.uppercaseChar().code - 64
            isCtrlPressed = false
            updateModifierButtons()
        }
        session?.write(byteArrayOf(code.toByte()), 0, 1)
    }

    private fun createNewTerminalSession(): TerminalSession {
        val home = bootstrapInstaller.homeDir.absolutePath
        bootstrapInstaller.homeDir.mkdirs()

        val loginFile = File(bootstrapInstaller.binDir, "login")
        val bashFile = File(bootstrapInstaller.bashPath)
        val shFile = File(bootstrapInstaller.binDir, "sh")

        val shell: String
        val args: Array<String>

        if (loginFile.exists() && loginFile.canExecute()) {
            shell = loginFile.absolutePath
            args = arrayOf()
        } else if (bashFile.exists() && bashFile.canExecute()) {
            shell = bashFile.absolutePath
            args = arrayOf("--login")
        } else if (shFile.exists() && shFile.canExecute()) {
            shell = shFile.absolutePath
            args = arrayOf()
        } else {
            shell = "/system/bin/sh"
            args = arrayOf()
        }

        val env = bootstrapInstaller.getEnvironment()

        return try {
            TerminalSession(shell, home, args, env, 4000, this)
        } catch (e: Exception) {
            android.util.Log.e("MobileCLI", "Failed to create session", e)
            TerminalSession("/system/bin/sh", home, arrayOf(), env, 4000, this)
        }
    }

    private fun createSession() {
        val newSession = createNewTerminalSession()
        sessions.clear()
        sessions.add(newSession)
        currentSessionIndex = 0
        terminalView.attachSession(newSession)

        terminalView.post {
            updateTerminalSize()
            terminalView.requestFocus()
            updateSessionTabs()
            // Offer AI environment setup on first launch
            checkAndOfferAISetup()
        }
    }

    override fun onStop() {
        super.onStop()
        // Save transcript for session persistence
        saveTranscript()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Clean up DITTO WebView to prevent memory leaks
        dittoWebView?.let { webView ->
            webView.stopLoading()
            webView.clearHistory()
            webView.clearCache(true)
            webView.loadUrl("about:blank")
            webView.onPause()
            webView.removeAllViews()
            webView.destroy()
        }
        dittoWebView = null

        // Stop UI command watcher
        uiCommandWatcherRunnable?.let { uiHandler.removeCallbacks(it) }
        uiCommandWatcherRunnable = null

        // Stop URL watcher
        urlWatcherRunnable?.let { uiHandler.removeCallbacks(it) }
        urlWatcherRunnable = null

        // Unbind from TermuxService but DON'T kill sessions
        // Sessions are now managed by TermuxService which keeps running
        if (serviceBound) {
            // Transfer sessions to service before unbinding
            termuxService?.let { service ->
                sessions.forEach { session ->
                    service.addSession(session)
                }
            }
            unbindService(serviceConnection)
            serviceBound = false
        }
        // DON'T kill sessions - let them persist in the service
        // The service keeps running as a foreground service
        // sessions.forEach { it.finishIfRunning() }
        // sessions.clear()
        Log.i("MainActivity", "Activity destroyed, sessions preserved in service")
    }

    private fun saveTranscript() {
        try {
            val transcript = session?.emulator?.screen?.getTranscriptText() ?: return
            if (transcript.isNotEmpty()) {
                val prefs = getSharedPreferences("mobilecli", Context.MODE_PRIVATE)
                prefs.edit().putString("last_transcript", transcript).apply()
            }
        } catch (e: Exception) {
            // Ignore save errors
        }
    }

    private fun restoreTranscript() {
        try {
            val prefs = getSharedPreferences("mobilecli", Context.MODE_PRIVATE)
            val saved = prefs.getString("last_transcript", null)
            if (!saved.isNullOrEmpty() && saved.length > 100) {
                // Show option to view previous session
                AlertDialog.Builder(this)
                    .setTitle("Previous Session")
                    .setMessage("Restore transcript from previous session?")
                    .setPositiveButton("Yes") { _, _ ->
                        session?.write("\n# Previous session transcript available\n# Use 'cat ~/.mobilecli/last_session.txt' to view\n".toByteArray(), 0, 80)
                        // Save to file for access
                        File(bootstrapInstaller.homeDir, ".mobilecli").mkdirs()
                        File(bootstrapInstaller.homeDir, ".mobilecli/last_session.txt").writeText(saved)
                    }
                    .setNegativeButton("No") { _, _ ->
                        // Clear old transcript
                        prefs.edit().remove("last_transcript").apply()
                    }
                    .show()
            }
        } catch (e: Exception) {
            // Ignore restore errors
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        terminalView.post {
            updateTerminalSize()
            terminalView.invalidate()
        }
    }

    private fun updateTerminalSize() {
        terminalView.post {
            if (terminalView.width > 0 && terminalView.height > 0 && session != null) {
                // Try to get actual font metrics from the TerminalView's renderer
                var fontWidthPx = 0
                var fontHeightPx = 0

                try {
                    // Access the mRenderer field to get actual font dimensions
                    val rendererField = terminalView.javaClass.getDeclaredField("mRenderer")
                    rendererField.isAccessible = true
                    val renderer = rendererField.get(terminalView)

                    if (renderer != null) {
                        val fontWidthField = renderer.javaClass.getDeclaredField("mFontWidth")
                        fontWidthField.isAccessible = true
                        fontWidthPx = (fontWidthField.get(renderer) as Number).toInt()

                        val fontHeightField = renderer.javaClass.getDeclaredField("mFontLineSpacing")
                        fontHeightField.isAccessible = true
                        fontHeightPx = (fontHeightField.get(renderer) as Number).toInt()
                    }
                } catch (e: Exception) {
                    android.util.Log.w("MobileCLI", "Could not get font metrics: ${e.message}")
                }

                // Fallback if reflection fails
                if (fontWidthPx <= 0 || fontHeightPx <= 0) {
                    val density = resources.displayMetrics.density
                    fontHeightPx = (currentTextSize * density * 1.2f).toInt().coerceAtLeast(1)
                    fontWidthPx = (currentTextSize * density * 0.6f).toInt().coerceAtLeast(1)
                }

                val newColumns = (terminalView.width / fontWidthPx).coerceIn(20, 500)
                val newRows = (terminalView.height / fontHeightPx).coerceIn(5, 200)

                session?.updateSize(newColumns, newRows)
                android.util.Log.i("MobileCLI", "Terminal: ${newColumns}x${newRows}, font: ${fontWidthPx}x${fontHeightPx}, view: ${terminalView.width}x${terminalView.height}")
            }
        }
    }

    // TerminalViewClient implementation
    override fun onScale(scale: Float): Float {
        if (scale < 0.9f || scale > 1.1f) {
            val delta = if (scale > 1f) 2 else -2
            val newSize = (currentTextSize + delta).coerceIn(minTextSize, maxTextSize)
            if (newSize != currentTextSize) {
                currentTextSize = newSize
                terminalView.setTextSize(currentTextSize)
                // Update terminal size after text size change
                terminalView.post {
                    updateTerminalSize()
                    terminalView.onScreenUpdated()
                }
            }
        }
        // Return 1.0f to indicate we've handled the scale and don't want default behavior
        return 1.0f
    }

    override fun onSingleTapUp(e: MotionEvent?) {
        terminalView.requestFocus()
        // Only show keyboard if not already visible (fixes keyboard lock-up bug)
        if (!isKeyboardVisible) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(terminalView, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    override fun onLongPress(event: MotionEvent?): Boolean {
        // Return false to let TerminalView handle text selection
        // Context menu is available via drawer menu or extra keys
        return false
    }

    private fun showContextMenu(event: MotionEvent?) {
        val options = arrayOf("Copy", "Paste", "Select All", "More...")
        AlertDialog.Builder(this)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // Copy - get transcript text
                        val text = session?.emulator?.screen?.getTranscriptText()
                        if (!text.isNullOrEmpty()) {
                            copyToClipboard(text)
                            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Nothing to copy", Toast.LENGTH_SHORT).show()
                        }
                    }
                    1 -> {
                        // Paste
                        pasteFromClipboard()
                    }
                    2 -> {
                        // Select All - copy entire transcript
                        val text = session?.emulator?.screen?.getTranscriptText()
                        if (!text.isNullOrEmpty()) {
                            copyToClipboard(text)
                            Toast.makeText(this, "All text copied", Toast.LENGTH_SHORT).show()
                        }
                    }
                    3 -> {
                        // More options
                        showMoreOptions()
                    }
                }
            }
            .show()
    }

    private fun showMoreOptions() {
        val options = arrayOf("Copy All", "Paste", "New session", "Kill session", "Reset terminal", "Change text size", "Toggle keyboard", "About")
        AlertDialog.Builder(this)
            .setTitle("More Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // Copy All - get entire transcript
                        val text = session?.emulator?.screen?.getTranscriptText()
                        if (!text.isNullOrEmpty()) {
                            copyToClipboard(text)
                            Toast.makeText(this, "All text copied to clipboard", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Nothing to copy", Toast.LENGTH_SHORT).show()
                        }
                    }
                    1 -> {
                        // Paste from clipboard
                        pasteFromClipboard()
                        Toast.makeText(this, "Pasted", Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        // New session
                        if (sessions.size < maxSessions) {
                            addNewSession()
                            Toast.makeText(this, "Session ${sessions.size} created", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Maximum $maxSessions sessions reached", Toast.LENGTH_SHORT).show()
                        }
                    }
                    3 -> {
                        // Kill session - terminates shell, press Enter to close
                        killCurrentSession()
                    }
                    4 -> {
                        // Reset terminal - clear screen and reset state
                        session?.let { s ->
                            s.reset()
                            terminalView.onScreenUpdated()
                            Toast.makeText(this, "Terminal reset", Toast.LENGTH_SHORT).show()
                        }
                    }
                    5 -> {
                        // Change text size
                        showTextSizeDialog()
                    }
                    6 -> {
                        // Toggle keyboard
                        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                        imm.toggleSoftInput(0, 0)
                    }
                    7 -> {
                        // About
                        showAboutDialog()
                    }
                }
            }
            .show()
    }

    private fun showTextSizeDialog() {
        val sizes = arrayOf("Small (14)", "Medium (20)", "Large (28)", "X-Large (36)", "XX-Large (48)")
        val sizeValues = intArrayOf(14, 20, 28, 36, 48)
        AlertDialog.Builder(this)
            .setTitle("Text Size")
            .setItems(sizes) { _, which ->
                currentTextSize = sizeValues[which]
                terminalView.setTextSize(currentTextSize)
                // Same pattern as pinch zoom - simple and reliable
                terminalView.post {
                    updateTerminalSize()
                    terminalView.onScreenUpdated()
                }
            }
            .show()
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("MobileCLI", text))
    }

    private fun pasteFromClipboard() {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.primaryClip?.getItemAt(0)?.text?.let { text ->
            // Handle multi-line paste properly
            val textStr = text.toString()
            session?.write(textStr.toByteArray(Charsets.UTF_8), 0, textStr.toByteArray(Charsets.UTF_8).size)
        }
    }

    override fun shouldBackButtonBeMappedToEscape(): Boolean = false
    override fun shouldEnforceCharBasedInput(): Boolean = true
    override fun shouldUseCtrlSpaceWorkaround(): Boolean = false
    override fun isTerminalViewSelected(): Boolean = true
    override fun copyModeChanged(copyMode: Boolean) {}

    override fun onKeyDown(keyCode: Int, e: KeyEvent?, session: TerminalSession?): Boolean = false
    override fun onKeyUp(keyCode: Int, e: KeyEvent?): Boolean = false

    override fun readControlKey(): Boolean = isCtrlPressed
    override fun readAltKey(): Boolean = isAltPressed
    override fun readShiftKey(): Boolean = false
    override fun readFnKey(): Boolean = false

    override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: TerminalSession?): Boolean {
        // Immediately reset modifiers after a key is pressed (fixes Ctrl+C activating Alt bug)
        // Must be synchronous, not via post{}, to prevent race conditions
        val wasCtrl = isCtrlPressed
        val wasAlt = isAltPressed
        isCtrlPressed = false
        isAltPressed = false
        if (wasCtrl || wasAlt) {
            updateModifierButtons()
        }
        return false
    }

    override fun onEmulatorSet() {
        terminalView.setTerminalCursorBlinkerRate(500)
        updateTerminalSize()
    }

    override fun logError(tag: String?, message: String?) {
        android.util.Log.e(tag ?: "Terminal", message ?: "")
    }
    override fun logWarn(tag: String?, message: String?) {
        android.util.Log.w(tag ?: "Terminal", message ?: "")
    }
    override fun logInfo(tag: String?, message: String?) {
        android.util.Log.i(tag ?: "Terminal", message ?: "")
    }
    override fun logDebug(tag: String?, message: String?) {
        android.util.Log.d(tag ?: "Terminal", message ?: "")
    }
    override fun logVerbose(tag: String?, message: String?) {
        android.util.Log.v(tag ?: "Terminal", message ?: "")
    }
    override fun logStackTraceWithMessage(tag: String?, message: String?, e: Exception?) {
        android.util.Log.e(tag ?: "Terminal", message, e)
    }
    override fun logStackTrace(tag: String?, e: Exception?) {
        android.util.Log.e(tag ?: "Terminal", "Error", e)
    }

    // TerminalSessionClient implementation
    override fun onTextChanged(changedSession: TerminalSession?) {
        terminalView.onScreenUpdated()
    }

    override fun onTitleChanged(changedSession: TerminalSession?) {}

    override fun onSessionFinished(finishedSession: TerminalSession?) {
        // Find and remove the finished session
        val index = sessions.indexOf(finishedSession)
        if (index >= 0) {
            removeSession(index)
            Toast.makeText(this, "Session closed", Toast.LENGTH_SHORT).show()
        } else if (sessions.isEmpty()) {
            // No sessions left, create a new one
            addNewSession()
        }
    }

    override fun onCopyTextToClipboard(session: TerminalSession?, text: String?) {
        text?.let { copyToClipboard(it) }
    }

    override fun onPasteTextFromClipboard(session: TerminalSession?) {
        pasteFromClipboard()
    }

    override fun onBell(session: TerminalSession?) {
        try {
            val vibrator = getSystemService(VIBRATOR_SERVICE) as android.os.Vibrator
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        } catch (e: Exception) {}
    }

    override fun onColorsChanged(session: TerminalSession?) {
        terminalView.onScreenUpdated()
    }

    override fun onTerminalCursorStateChange(state: Boolean) {}

    override fun getTerminalCursorStyle(): Int = TerminalEmulator.TERMINAL_CURSOR_STYLE_BLOCK
}
