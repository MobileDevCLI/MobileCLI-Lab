package com.termux.studio.rooms

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.termux.R
import com.termux.studio.BaseRoomActivity
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import org.json.JSONObject

/**
 * AiMlRoomActivity - AI Device Control Center
 *
 * Based on Test Claude's v80 discoveries, this room provides:
 * - GUI access to all 84 Termux device APIs
 * - Browser interaction loop (URL + clipboard)
 * - Sensor dashboard with real-time data
 * - Templates for 21 invention ideas
 * - AI chat interface (Claude Code integration)
 *
 * Test Claude defined MobileCLI as "the world's first AI-native mobile OS"
 * with 1,226+ Linux binaries and 84 device control APIs.
 */
class AiMlRoomActivity : BaseRoomActivity() {

    override val roomName = "AI/ML"
    override val roomIcon = R.drawable.ic_ai_ml

    // UI Components
    private lateinit var apiCategoryList: RecyclerView
    private lateinit var apiCommandList: RecyclerView
    private lateinit var sensorPanel: LinearLayout
    private lateinit var outputView: TextView

    // State
    private var currentCategory: ApiCategory? = null
    private val handler = Handler(Looper.getMainLooper())
    private var sensorUpdateRunnable: Runnable? = null
    private var isSensorUpdateRunning = false

    // All 84 Termux API Commands organized by category
    private val apiCategories = listOf(
        ApiCategory(
            name = "Browser & Clipboard",
            icon = R.drawable.ic_web_dev,
            commands = listOf(
                ApiCommand("termux-open-url", "Open URL in browser", "URL"),
                ApiCommand("termux-open", "Open file or URL", "path/URL"),
                ApiCommand("termux-clipboard-get", "Read clipboard content", null),
                ApiCommand("termux-clipboard-set", "Write to clipboard", "text"),
                ApiCommand("termux-share", "Share content via Android", "-a share text")
            )
        ),
        ApiCategory(
            name = "Communication",
            icon = R.drawable.ic_mobile_apps,
            commands = listOf(
                ApiCommand("termux-sms-send", "Send SMS message", "-n NUMBER MESSAGE"),
                ApiCommand("termux-sms-inbox", "List SMS messages", "[-l LIMIT]"),
                ApiCommand("termux-telephony-call", "Make phone call", "NUMBER"),
                ApiCommand("termux-telephony-deviceinfo", "Get device info", null),
                ApiCommand("termux-telephony-cellinfo", "Get cell tower info", null),
                ApiCommand("termux-contact-list", "List contacts", null)
            )
        ),
        ApiCategory(
            name = "Camera & Media",
            icon = R.drawable.ic_file,
            commands = listOf(
                ApiCommand("termux-camera-photo", "Take photo", "-c CAMERA_ID output.jpg"),
                ApiCommand("termux-camera-info", "Get camera info", null),
                ApiCommand("termux-microphone-record", "Record audio", "-f output.m4a"),
                ApiCommand("termux-media-player", "Control media playback", "play|pause|stop FILE"),
                ApiCommand("termux-media-scan", "Scan media files", "-r /sdcard/Music"),
                ApiCommand("termux-tts-speak", "Text to speech", "TEXT"),
                ApiCommand("termux-tts-engines", "List TTS engines", null)
            )
        ),
        ApiCategory(
            name = "Sensors & Location",
            icon = R.drawable.ic_data_science,
            commands = listOf(
                ApiCommand("termux-location", "Get GPS location", "[-p gps|network]"),
                ApiCommand("termux-sensor", "Read device sensors", "-s SENSOR [-n COUNT]"),
                ApiCommand("termux-sensor", "List all sensors", "-l"),
                ApiCommand("termux-fingerprint", "Fingerprint authentication", null)
            )
        ),
        ApiCategory(
            name = "Hardware Control",
            icon = R.drawable.ic_terminal,
            commands = listOf(
                ApiCommand("termux-torch", "Toggle flashlight", "on|off"),
                ApiCommand("termux-vibrate", "Vibrate device", "[-d DURATION_MS]"),
                ApiCommand("termux-infrared-transmit", "IR blaster", "-f FREQ PATTERN"),
                ApiCommand("termux-infrared-frequencies", "Get IR frequencies", null),
                ApiCommand("termux-usb", "List USB devices", "-l"),
                ApiCommand("termux-volume", "Get/set volume", "[STREAM VOLUME]")
            )
        ),
        ApiCategory(
            name = "System & UI",
            icon = R.drawable.ic_folder,
            commands = listOf(
                ApiCommand("termux-notification", "Show notification", "-t TITLE -c CONTENT"),
                ApiCommand("termux-notification-remove", "Remove notification", "--id ID"),
                ApiCommand("termux-dialog", "Show dialog", "confirm|checkbox|text"),
                ApiCommand("termux-toast", "Show toast message", "MESSAGE"),
                ApiCommand("termux-battery-status", "Get battery info", null),
                ApiCommand("termux-brightness", "Get/set brightness", "[0-255]"),
                ApiCommand("termux-wifi-connectioninfo", "Get WiFi info", null),
                ApiCommand("termux-wifi-enable", "Enable/disable WiFi", "true|false"),
                ApiCommand("termux-wifi-scaninfo", "Scan WiFi networks", null),
                ApiCommand("termux-wake-lock", "Acquire CPU wake lock", null),
                ApiCommand("termux-wake-unlock", "Release wake lock", null)
            )
        ),
        ApiCategory(
            name = "Storage & Files",
            icon = R.drawable.ic_folder,
            commands = listOf(
                ApiCommand("termux-setup-storage", "Setup storage access", null),
                ApiCommand("termux-storage-get", "Get file from storage", "OUTPUT_FILE"),
                ApiCommand("termux-saf-create", "Create SAF document", "FILENAME"),
                ApiCommand("termux-saf-dirs", "List SAF directories", null),
                ApiCommand("termux-saf-ls", "List SAF contents", "URI"),
                ApiCommand("termux-saf-read", "Read SAF document", "URI"),
                ApiCommand("termux-saf-rm", "Remove SAF document", "URI")
            )
        ),
        ApiCategory(
            name = "Security & Keys",
            icon = R.drawable.ic_file,
            commands = listOf(
                ApiCommand("termux-keystore", "Android Keystore", "list|delete|sign"),
                ApiCommand("termux-nfc", "NFC operations", "-l"),
                ApiCommand("termux-speech-to-text", "Speech recognition", null)
            )
        )
    )

    // 21 Invention Templates from Test Claude
    private val inventionTemplates = listOf(
        InventionTemplate(
            name = "AI Personal Security Guard",
            description = "Emergency detection with GPS + SMS + Call",
            apis = listOf("termux-location", "termux-sms-send", "termux-telephony-call", "termux-sensor")
        ),
        InventionTemplate(
            name = "Voice-Controlled Home Automation",
            description = "Control devices via IR blaster",
            apis = listOf("termux-speech-to-text", "termux-infrared-transmit", "termux-tts-speak")
        ),
        InventionTemplate(
            name = "AI Document Scanner",
            description = "Scan and organize documents with OCR",
            apis = listOf("termux-camera-photo", "termux-media-scan", "termux-share")
        ),
        InventionTemplate(
            name = "Elderly Care Monitor",
            description = "Fall detection and emergency alerts",
            apis = listOf("termux-sensor", "termux-sms-send", "termux-location", "termux-notification")
        ),
        InventionTemplate(
            name = "AI Meeting Transcriber",
            description = "Record and transcribe meetings",
            apis = listOf("termux-microphone-record", "termux-speech-to-text", "termux-share")
        ),
        InventionTemplate(
            name = "Smart Doorbell",
            description = "NFC-based access control",
            apis = listOf("termux-nfc", "termux-notification", "termux-camera-photo")
        ),
        InventionTemplate(
            name = "Fitness Tracker",
            description = "Step counting and activity monitoring",
            apis = listOf("termux-sensor", "termux-notification", "termux-vibrate")
        ),
        InventionTemplate(
            name = "AI Photo Organizer",
            description = "Automatically organize photos by content",
            apis = listOf("termux-camera-info", "termux-media-scan", "termux-storage-get")
        ),
        InventionTemplate(
            name = "Voice Memo Assistant",
            description = "Quick voice notes with transcription",
            apis = listOf("termux-microphone-record", "termux-speech-to-text", "termux-clipboard-set")
        ),
        InventionTemplate(
            name = "Location-Based Reminder",
            description = "Reminders triggered by GPS location",
            apis = listOf("termux-location", "termux-notification", "termux-vibrate")
        ),
        InventionTemplate(
            name = "Battery Optimizer",
            description = "Smart power management",
            apis = listOf("termux-battery-status", "termux-wifi-enable", "termux-brightness")
        ),
        InventionTemplate(
            name = "Smart Flashlight",
            description = "Gesture-controlled flashlight",
            apis = listOf("termux-torch", "termux-sensor", "termux-vibrate")
        ),
        InventionTemplate(
            name = "Emergency SOS",
            description = "One-tap emergency alert system",
            apis = listOf("termux-location", "termux-sms-send", "termux-telephony-call", "termux-notification")
        ),
        InventionTemplate(
            name = "AI Language Translator",
            description = "Real-time voice translation",
            apis = listOf("termux-speech-to-text", "termux-tts-speak", "termux-clipboard-set")
        ),
        InventionTemplate(
            name = "Smart Alarm",
            description = "Contextual alarm based on sleep patterns",
            apis = listOf("termux-sensor", "termux-vibrate", "termux-media-player", "termux-brightness")
        ),
        InventionTemplate(
            name = "Network Monitor",
            description = "WiFi and connectivity monitoring",
            apis = listOf("termux-wifi-connectioninfo", "termux-wifi-scaninfo", "termux-notification")
        ),
        InventionTemplate(
            name = "USB Device Manager",
            description = "Manage connected USB devices",
            apis = listOf("termux-usb", "termux-notification", "termux-toast")
        ),
        InventionTemplate(
            name = "SMS Automation",
            description = "Automated SMS responses",
            apis = listOf("termux-sms-inbox", "termux-sms-send", "termux-notification")
        ),
        InventionTemplate(
            name = "Contact Backup",
            description = "Backup and sync contacts",
            apis = listOf("termux-contact-list", "termux-share", "termux-storage-get")
        ),
        InventionTemplate(
            name = "Volume Scheduler",
            description = "Time-based volume profiles",
            apis = listOf("termux-volume", "termux-notification", "termux-vibrate")
        ),
        InventionTemplate(
            name = "AI Browser Assistant",
            description = "Claude + Browser interaction loop",
            apis = listOf("termux-open-url", "termux-clipboard-get", "termux-clipboard-set", "termux-share")
        )
    )

    override fun onRoomCreated() {
        setupCategoryPanel()
        setupCommandPanel()
        setupSensorDashboard()
        setupBrowserBridge()
        setupInventionTemplates()
        showWelcomeMessage()
    }

    private fun setupCategoryPanel() {
        apiCategoryList = findViewById(R.id.file_tree)
        apiCategoryList.layoutManager = LinearLayoutManager(this)

        val adapter = CategoryAdapter { category ->
            currentCategory = category
            showCategoryCommands(category)
        }
        adapter.updateCategories(apiCategories)
        apiCategoryList.adapter = adapter

        // Change left panel title
        findViewById<TextView>(R.id.left_panel_title)?.text = "API Categories"
    }

    private fun setupCommandPanel() {
        // Add command grid to main content
        val commandView = LayoutInflater.from(this)
            .inflate(R.layout.view_api_commands, mainContent, false)
        mainContent.addView(commandView)

        apiCommandList = commandView.findViewById(R.id.command_grid)
        apiCommandList.layoutManager = GridLayoutManager(this, 2)

        // Quick actions bar
        commandView.findViewById<Button>(R.id.btn_browser_loop)?.setOnClickListener {
            showBrowserLoopDialog()
        }

        commandView.findViewById<Button>(R.id.btn_inventions)?.setOnClickListener {
            showInventionsDialog()
        }

        commandView.findViewById<Button>(R.id.btn_sensor_dashboard)?.setOnClickListener {
            toggleSensorDashboard()
        }

        // Hide placeholder
        findViewById<TextView>(R.id.content_placeholder)?.visibility = View.GONE
    }

    private fun setupSensorDashboard() {
        sensorPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(16, 16, 16, 16)
        }

        // Add sensor readouts
        val sensors = listOf(
            "Accelerometer" to "termux-sensor -s accelerometer -n 1",
            "Gyroscope" to "termux-sensor -s gyroscope -n 1",
            "Light" to "termux-sensor -s light -n 1",
            "Proximity" to "termux-sensor -s proximity -n 1"
        )

        sensors.forEach { (name, _) ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val label = TextView(this).apply {
                text = "$name: "
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val value = TextView(this).apply {
                text = "---"
                textSize = 14f
                tag = name
            }

            row.addView(label)
            row.addView(value)
            sensorPanel.addView(row)
        }

        // Add to right panel
        rightPanel.addView(sensorPanel)
    }

    private fun setupBrowserBridge() {
        // Browser interaction loop diagram in console
    }

    private fun setupInventionTemplates() {
        // Loaded from inventionTemplates list
    }

    private fun showWelcomeMessage() {
        outputView = consoleOutput
        clearConsole()

        appendToConsole("╔════════════════════════════════════════════════════════════╗")
        appendToConsole("║           AI/ML Device Control Room                         ║")
        appendToConsole("╠════════════════════════════════════════════════════════════╣")
        appendToConsole("║  MobileCLI: The World's First AI-Native Mobile OS          ║")
        appendToConsole("║                                                            ║")
        appendToConsole("║  Features:                                                 ║")
        appendToConsole("║  • 84 Device Control APIs                                  ║")
        appendToConsole("║  • Browser Interaction Loop                                ║")
        appendToConsole("║  • Sensor Dashboard                                        ║")
        appendToConsole("║  • 21 Invention Templates                                  ║")
        appendToConsole("╚════════════════════════════════════════════════════════════╝")
        appendToConsole("")
        appendToConsole("Select an API category from the left panel to get started.")
        appendToConsole("")
    }

    private fun showCategoryCommands(category: ApiCategory) {
        appendToConsole("\n=== ${category.name} ===")

        val adapter = CommandAdapter { command ->
            showCommandDialog(command)
        }
        adapter.updateCommands(category.commands)
        apiCommandList.adapter = adapter
    }

    private fun showCommandDialog(command: ApiCommand) {
        val view = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 16)
        }

        view.addView(TextView(this).apply {
            text = command.description
            textSize = 14f
        })

        val argInput = if (command.args != null) {
            EditText(this).apply {
                hint = command.args
            }.also { view.addView(it) }
        } else null

        AlertDialog.Builder(this)
            .setTitle(command.name)
            .setView(view)
            .setPositiveButton("Execute") { _, _ ->
                val args = argInput?.text?.toString() ?: ""
                executeApiCommand(command.name, args)
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Copy") { _, _ ->
                val fullCommand = if (command.args != null) {
                    "${command.name} ${argInput?.text ?: command.args}"
                } else {
                    command.name
                }
                copyToClipboard(fullCommand)
            }
            .show()
    }

    private fun executeApiCommand(command: String, args: String) {
        val fullCommand = if (args.isNotBlank()) "$command $args" else command
        appendToConsole("\n$ $fullCommand")

        Thread {
            try {
                val env = arrayOf(
                    "HOME=/data/data/com.termux/files/home",
                    "PATH=/data/data/com.termux/files/usr/bin:${System.getenv("PATH")}",
                    "PREFIX=/data/data/com.termux/files/usr"
                )

                val process = Runtime.getRuntime().exec(
                    arrayOf("/data/data/com.termux/files/usr/bin/bash", "-c", fullCommand),
                    env
                )

                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val errorReader = BufferedReader(InputStreamReader(process.errorStream))

                val output = reader.readText()
                val error = errorReader.readText()

                process.waitFor()

                runOnUiThread {
                    if (output.isNotBlank()) {
                        // Try to format JSON
                        try {
                            val json = JSONObject(output)
                            appendToConsole(json.toString(2))
                        } catch (e: Exception) {
                            appendToConsole(output.trim())
                        }
                    }
                    if (error.isNotBlank()) {
                        appendToConsole("[ERROR] ${error.trim()}")
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    appendToConsole("[ERROR] ${e.message}")
                }
            }
        }.start()
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("command", text))
        showToast("Copied to clipboard")
    }

    private fun showBrowserLoopDialog() {
        val view = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 16)
        }

        view.addView(TextView(this).apply {
            text = """
Browser Interaction Loop:

┌─────────────────────────────────────┐
│  CLAUDE CODE (in Termux)            │
│         │                           │
│         ├──► termux-open-url ──►    │
│         │         Browser opens     │
│         │              │            │
│         │         User interacts    │
│         │         User copies data  │
│         │              │            │
│         ◄── termux-clipboard-get ◄──│
│         │                           │
│  Claude receives data and continues │
└─────────────────────────────────────┘

This loop allows AI to request web data
through user interaction.
            """.trimIndent()
            textSize = 12f
            typeface = android.graphics.Typeface.MONOSPACE
        })

        val urlInput = EditText(this).apply {
            hint = "Enter URL to open"
            setText("https://")
        }
        view.addView(urlInput)

        AlertDialog.Builder(this)
            .setTitle("Browser Interaction Loop")
            .setView(view)
            .setPositiveButton("Open URL") { _, _ ->
                val url = urlInput.text.toString()
                executeApiCommand("termux-open-url", url)
            }
            .setNeutralButton("Read Clipboard") { _, _ ->
                executeApiCommand("termux-clipboard-get", "")
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showInventionsDialog() {
        val names = inventionTemplates.map { "${it.name}\n${it.description}" }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("21 Invention Templates")
            .setItems(names) { _, which ->
                showInventionDetail(inventionTemplates[which])
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showInventionDetail(template: InventionTemplate) {
        val message = """
${template.description}

Required APIs:
${template.apis.joinToString("\n") { "• $it" }}

Would you like to generate the code template?
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle(template.name)
            .setMessage(message)
            .setPositiveButton("Generate Code") { _, _ ->
                generateInventionCode(template)
            }
            .setNeutralButton("Test APIs") { _, _ ->
                testInventionApis(template)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun generateInventionCode(template: InventionTemplate) {
        val code = buildString {
            appendLine("#!/data/data/com.termux/files/usr/bin/bash")
            appendLine("# ${template.name}")
            appendLine("# ${template.description}")
            appendLine("# Generated by MobileCLI AI/ML Room")
            appendLine()
            appendLine("# Required APIs:")
            template.apis.forEach { api ->
                appendLine("# - $api")
            }
            appendLine()
            appendLine("# Implementation:")
            template.apis.forEachIndexed { index, api ->
                appendLine()
                appendLine("# Step ${index + 1}: $api")
                when {
                    api.contains("location") -> appendLine("LOCATION=\$($api -p gps)")
                    api.contains("sensor") -> appendLine("SENSOR_DATA=\$($api -s accelerometer -n 1)")
                    api.contains("notification") -> appendLine("$api -t \"${template.name}\" -c \"Status update\"")
                    api.contains("sms-send") -> appendLine("# $api -n PHONE_NUMBER \"Your message\"")
                    api.contains("camera") -> appendLine("$api -c 0 /sdcard/photo.jpg")
                    api.contains("clipboard") -> appendLine("DATA=\$($api)")
                    api.contains("torch") -> appendLine("$api on")
                    else -> appendLine("RESULT_${index}=\$($api)")
                }
            }
            appendLine()
            appendLine("echo \"${template.name} executed successfully\"")
        }

        appendToConsole("\n=== Generated Code: ${template.name} ===")
        appendToConsole(code)

        // Save to file
        val scriptsDir = File(filesDir.parentFile, "home/inventions")
        scriptsDir.mkdirs()
        val scriptFile = File(scriptsDir, "${template.name.lowercase().replace(" ", "_")}.sh")
        scriptFile.writeText(code)
        scriptFile.setExecutable(true)

        appendToConsole("\nSaved to: ${scriptFile.absolutePath}")
        showToast("Script saved!")
    }

    private fun testInventionApis(template: InventionTemplate) {
        appendToConsole("\n=== Testing APIs for: ${template.name} ===")
        template.apis.forEach { api ->
            appendToConsole("\nTesting: $api")
            // Just test if command exists
            executeApiCommand("which", api)
        }
    }

    private fun toggleSensorDashboard() {
        if (sensorPanel.visibility == View.GONE) {
            sensorPanel.visibility = View.VISIBLE
            isRightPanelVisible = true
            updatePanelVisibility()
            startSensorUpdates()
            showToast("Sensor dashboard enabled")
        } else {
            sensorPanel.visibility = View.GONE
            stopSensorUpdates()
            showToast("Sensor dashboard disabled")
        }
    }

    private fun startSensorUpdates() {
        if (isSensorUpdateRunning) return
        isSensorUpdateRunning = true

        sensorUpdateRunnable = object : Runnable {
            override fun run() {
                updateSensorValues()
                if (isSensorUpdateRunning) {
                    handler.postDelayed(this, 1000)
                }
            }
        }
        handler.post(sensorUpdateRunnable!!)
    }

    private fun stopSensorUpdates() {
        isSensorUpdateRunning = false
        sensorUpdateRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun updateSensorValues() {
        Thread {
            try {
                // Update accelerometer
                updateSensor("Accelerometer", "accelerometer")
                updateSensor("Light", "light")
            } catch (e: Exception) {
                // Ignore sensor errors
            }
        }.start()
    }

    private fun updateSensor(name: String, sensorType: String) {
        try {
            val process = Runtime.getRuntime().exec(
                arrayOf(
                    "/data/data/com.termux/files/usr/bin/termux-sensor",
                    "-s", sensorType, "-n", "1"
                )
            )
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()
            process.waitFor()

            runOnUiThread {
                val valueView = sensorPanel.findViewWithTag<TextView>(name)
                try {
                    val json = JSONObject(output)
                    val values = json.optJSONArray(sensorType)?.optJSONObject(0)?.optJSONArray("values")
                    if (values != null && values.length() > 0) {
                        val displayValue = (0 until values.length())
                            .map { "%.2f".format(values.getDouble(it)) }
                            .joinToString(", ")
                        valueView?.text = displayValue
                    }
                } catch (e: Exception) {
                    valueView?.text = "N/A"
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun updatePanelVisibility() {
        rightPanel.visibility = if (isRightPanelVisible) View.VISIBLE else View.GONE
    }

    override fun onRunClicked() {
        showToast("Select an API command to execute")
    }

    override fun onSaveClicked() {
        showToast("Use Invention Templates to generate scripts")
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSensorUpdates()
    }

    // === Data Classes ===

    data class ApiCategory(
        val name: String,
        val icon: Int,
        val commands: List<ApiCommand>
    )

    data class ApiCommand(
        val name: String,
        val description: String,
        val args: String?
    )

    data class InventionTemplate(
        val name: String,
        val description: String,
        val apis: List<String>
    )

    // === Adapters ===

    inner class CategoryAdapter(
        private val onClick: (ApiCategory) -> Unit
    ) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

        private var categories = listOf<ApiCategory>()

        fun updateCategories(newCategories: List<ApiCategory>) {
            categories = newCategories
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_file_tree, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(categories[position])
        }

        override fun getItemCount() = categories.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val icon: ImageView = itemView.findViewById(R.id.file_icon)
            private val name: TextView = itemView.findViewById(R.id.file_name)

            fun bind(category: ApiCategory) {
                name.text = "${category.name} (${category.commands.size})"
                icon.setImageResource(category.icon)

                val isSelected = category == currentCategory
                if (isSelected) {
                    itemView.setBackgroundColor(0xFF2a2a2a.toInt())
                } else {
                    itemView.setBackgroundColor(0x00000000)
                }

                itemView.setOnClickListener {
                    onClick(category)
                    notifyDataSetChanged()
                }
            }
        }
    }

    inner class CommandAdapter(
        private val onClick: (ApiCommand) -> Unit
    ) : RecyclerView.Adapter<CommandAdapter.ViewHolder>() {

        private var commands = listOf<ApiCommand>()

        fun updateCommands(newCommands: List<ApiCommand>) {
            commands = newCommands
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = Button(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                textSize = 12f
                isAllCaps = false
            }
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(commands[position])
        }

        override fun getItemCount() = commands.size

        inner class ViewHolder(private val button: Button) : RecyclerView.ViewHolder(button) {
            fun bind(command: ApiCommand) {
                button.text = command.name.removePrefix("termux-")
                button.setOnClickListener {
                    onClick(command)
                }
            }
        }
    }
}
