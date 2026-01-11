package com.termux

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * SetupWizardActivity - Clean first-launch experience
 *
 * Shows a beautiful setup wizard instead of raw terminal output.
 * Steps:
 * 1. Welcome screen
 * 2. Bootstrap installation (with progress)
 * 3. AI CLI selection
 * 4. Installation progress
 * 5. Ready screen
 */
class SetupWizardActivity : AppCompatActivity() {

    private lateinit var bootstrapInstaller: BootstrapInstaller

    // UI elements
    private lateinit var welcomeSection: LinearLayout
    private lateinit var setupSection: LinearLayout
    private lateinit var aiChoiceSection: LinearLayout
    private lateinit var installingSection: LinearLayout
    private lateinit var readySection: LinearLayout

    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var statusText: TextView

    private lateinit var aiRadioGroup: RadioGroup

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 1002

        // All permissions we want to request upfront
        private val ALL_PERMISSIONS = arrayOf(
            // Storage
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            // Location
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            // Camera & Microphone
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            // Phone & SMS
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            // Contacts
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            // Sensors
            Manifest.permission.BODY_SENSORS
        )

        fun isSetupComplete(context: Context): Boolean {
            val prefs = context.getSharedPreferences("mobilecli", Context.MODE_PRIVATE)
            return prefs.getBoolean("setup_wizard_complete", false)
        }

        fun markSetupComplete(context: Context) {
            val prefs = context.getSharedPreferences("mobilecli", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("setup_wizard_complete", true).apply()
        }
    }

    private var permissionsRequested = false
    private var overlayPermissionRequested = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If setup is already complete, go directly to HomeActivity
        if (isSetupComplete(this)) {
            startHomeActivity()
            return
        }

        setContentView(R.layout.activity_setup_wizard)

        bootstrapInstaller = BootstrapInstaller(this)

        // Initialize UI elements
        initializeUI()

        // Show welcome screen
        showWelcome()
    }

    private fun initializeUI() {
        welcomeSection = findViewById(R.id.welcome_section)
        setupSection = findViewById(R.id.setup_section)
        aiChoiceSection = findViewById(R.id.ai_choice_section)
        installingSection = findViewById(R.id.installing_section)
        readySection = findViewById(R.id.ready_section)

        progressBar = findViewById(R.id.setup_progress)
        progressText = findViewById(R.id.progress_text)
        statusText = findViewById(R.id.status_text)

        aiRadioGroup = findViewById(R.id.ai_radio_group)

        // Welcome button
        findViewById<Button>(R.id.btn_get_started).setOnClickListener {
            startSetup()
        }

        // AI choice continue button
        findViewById<Button>(R.id.btn_continue_ai).setOnClickListener {
            val selectedId = aiRadioGroup.checkedRadioButtonId
            val aiChoice = when (selectedId) {
                R.id.radio_claude -> "claude"
                R.id.radio_gemini -> "gemini"
                R.id.radio_codex -> "codex"
                R.id.radio_all -> "all"
                R.id.radio_none -> "none"
                else -> "claude"
            }
            installAI(aiChoice)
        }

        // Skip AI button
        findViewById<Button>(R.id.btn_skip_ai).setOnClickListener {
            finishSetup()
        }

        // Ready button
        findViewById<Button>(R.id.btn_launch).setOnClickListener {
            markSetupComplete(this)
            startHomeActivity()
        }
    }

    private fun showWelcome() {
        welcomeSection.visibility = View.VISIBLE
        setupSection.visibility = View.GONE
        aiChoiceSection.visibility = View.GONE
        installingSection.visibility = View.GONE
        readySection.visibility = View.GONE

        // First, check and request overlay permission (critical for URL opening)
        if (!overlayPermissionRequested) {
            overlayPermissionRequested = true
            checkAndRequestOverlayPermission()
        }

        // Then request regular permissions
        if (!permissionsRequested) {
            permissionsRequested = true
            requestAllPermissions()
        }
    }

    /**
     * Check and request "Draw over other apps" permission.
     * This is CRITICAL for opening URLs from shell commands.
     * Without this, Android 10+ blocks background activity starts.
     */
    private fun checkAndRequestOverlayPermission() {
        // Only needed for Android M (6.0) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                // Show explanation dialog
                AlertDialog.Builder(this)
                    .setTitle("Permission Required")
                    .setMessage(
                        "MobileCLI needs the \"Display over other apps\" permission to open URLs " +
                        "(like browser links) from the terminal.\n\n" +
                        "This is required for Claude Code authentication and other web links to work.\n\n" +
                        "Please enable this permission on the next screen."
                    )
                    .setPositiveButton("Grant Permission") { _, _ ->
                        // Open the system settings for overlay permission
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                        startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
                    }
                    .setNegativeButton("Skip") { dialog, _ ->
                        dialog.dismiss()
                        Toast.makeText(
                            this,
                            "URL opening may not work without this permission",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "Permission granted! URL opening will work.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Permission denied. URL opening may not work.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Request all permissions at once so user doesn't have to grant them one by one later
     */
    private fun requestAllPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        for (permission in ALL_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }

        // Add notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Count granted permissions
            val granted = grantResults.count { it == PackageManager.PERMISSION_GRANTED }
            val total = grantResults.size

            // Show a toast with permission status
            if (total > 0) {
                val message = if (granted == total) {
                    "All permissions granted!"
                } else {
                    "$granted of $total permissions granted. Some features may be limited."
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showSetup() {
        welcomeSection.visibility = View.GONE
        setupSection.visibility = View.VISIBLE
        aiChoiceSection.visibility = View.GONE
        installingSection.visibility = View.GONE
        readySection.visibility = View.GONE
    }

    private fun showAIChoice() {
        welcomeSection.visibility = View.GONE
        setupSection.visibility = View.GONE
        aiChoiceSection.visibility = View.VISIBLE
        installingSection.visibility = View.GONE
        readySection.visibility = View.GONE
    }

    private fun showInstalling() {
        welcomeSection.visibility = View.GONE
        setupSection.visibility = View.GONE
        aiChoiceSection.visibility = View.GONE
        installingSection.visibility = View.VISIBLE
        readySection.visibility = View.GONE
    }

    private fun showReady() {
        welcomeSection.visibility = View.GONE
        setupSection.visibility = View.GONE
        aiChoiceSection.visibility = View.GONE
        installingSection.visibility = View.GONE
        readySection.visibility = View.VISIBLE
    }

    private fun startSetup() {
        showSetup()

        lifecycleScope.launch {
            try {
                if (bootstrapInstaller.isInstalled()) {
                    // Already installed, skip to AI choice
                    updateProgress(100, "Environment ready!")
                    delay(500)
                    showAIChoice()
                } else {
                    // Install bootstrap
                    installBootstrap()
                }
            } catch (e: Exception) {
                updateProgress(0, "Error: ${e.message}")
            }
        }
    }

    private suspend fun installBootstrap() {
        withContext(Dispatchers.Main) {
            updateProgress(0, "Preparing environment...")
        }

        // Set up progress callback
        bootstrapInstaller.onProgress = { progress, message ->
            lifecycleScope.launch(Dispatchers.Main) {
                updateProgress(progress, message)
            }
        }

        withContext(Dispatchers.IO) {
            try {
                // Install bootstrap (handles download, extract, permissions)
                val success = bootstrapInstaller.install()

                withContext(Dispatchers.Main) {
                    if (success) {
                        updateProgress(100, "Environment ready!")
                        delay(500)
                        showAIChoice()
                    } else {
                        updateProgress(0, "Setup failed")
                        statusText.text = "Please try again or contact support"
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    updateProgress(0, "Setup failed: ${e.message}")
                    statusText.text = "Please try again or contact support"
                }
            }
        }
    }

    private fun updateProgress(percent: Int, message: String) {
        progressBar.progress = percent
        progressText.text = message
    }

    private fun installAI(choice: String) {
        // Save the AI selection so MainActivity doesn't ask again
        val prefs = getSharedPreferences("mobilecli", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("selected_ai", choice)
            .putBoolean("ai_setup_complete", true)
            .apply()

        if (choice == "none") {
            finishSetup()
            return
        }

        showInstalling()

        val (name, command) = when (choice) {
            "claude" -> "Claude Code" to "pkg update -y && pkg install nodejs-lts -y && npm install -g @anthropic-ai/claude-code"
            "gemini" -> "Gemini CLI" to "pkg update -y && pkg install nodejs-lts -y && npm install -g @google/generative-ai-cli"
            "codex" -> "Codex CLI" to "pkg update -y && pkg install nodejs-lts -y && npm install -g @openai/codex"
            "all" -> "All AI Tools" to "pkg update -y && pkg install nodejs-lts -y && npm install -g @anthropic-ai/claude-code @google/generative-ai-cli @openai/codex"
            else -> "Claude Code" to "pkg update -y && pkg install nodejs-lts -y && npm install -g @anthropic-ai/claude-code"
        }

        statusText.text = "Installing $name...\nThis may take a few minutes."
        progressBar.isIndeterminate = true

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Run the installation command
                val process = Runtime.getRuntime().exec(
                    arrayOf(
                        File(bootstrapInstaller.binDir, "bash").absolutePath,
                        "-c",
                        command
                    ),
                    bootstrapInstaller.getEnvironment(),
                    bootstrapInstaller.homeDir
                )

                // Wait for completion (with timeout)
                val completed = process.waitFor()

                withContext(Dispatchers.Main) {
                    progressBar.isIndeterminate = false
                    if (completed == 0) {
                        progressBar.progress = 100
                        statusText.text = "$name installed successfully!"
                        delay(1000)
                        finishSetup()
                    } else {
                        statusText.text = "Installation completed. You can install more tools later."
                        delay(1500)
                        finishSetup()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.isIndeterminate = false
                    statusText.text = "Installation issue: ${e.message}\nYou can install manually later."
                    delay(2000)
                    finishSetup()
                }
            }
        }
    }

    private fun finishSetup() {
        showReady()

        // Set the hint text based on what was installed
        val hintText = findViewById<TextView>(R.id.hint_text)
        hintText.text = "Type 'claude', 'gemini', or 'codex' to start your AI assistant.\n\nOr use the terminal for any development task!"
    }

    private fun startHomeActivity() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
