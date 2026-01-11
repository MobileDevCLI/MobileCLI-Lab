package com.termux.activities

import android.content.Context
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.util.Properties

/**
 * Settings Activity for MobileCLI.
 * Provides a proper UI for configuring terminal settings.
 * Settings are saved to ~/.termux/termux.properties
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var properties: Properties
    private lateinit var propertiesFile: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create UI programmatically
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(0xFF1a1a1a.toInt())
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        scrollView.addView(container)
        setContentView(scrollView)

        // Set title
        title = "MobileCLI Settings"

        // Load properties
        val termuxDir = File(File(filesDir, "home"), ".termux")
        termuxDir.mkdirs()
        propertiesFile = File(termuxDir, "termux.properties")
        loadProperties()

        // Add settings sections
        addSectionHeader(container, "Appearance")
        addFullscreenSetting(container)
        addTextSizeSetting(container)

        addSectionHeader(container, "Keyboard")
        addBackKeySetting(container)
        addHideKeyboardSetting(container)

        addSectionHeader(container, "Terminal")
        addCursorStyleSetting(container)
        addCursorBlinkSetting(container)
        addScrollbackSetting(container)

        addSectionHeader(container, "Bell")
        addBellSetting(container)

        addSectionHeader(container, "External Apps")
        addExternalAppsSetting(container)

        // Save button
        addSaveButton(container)
    }

    private fun loadProperties() {
        properties = Properties()
        try {
            if (propertiesFile.exists()) {
                propertiesFile.inputStream().use { properties.load(it) }
            }
        } catch (e: Exception) {
            // Use defaults
        }
    }

    private fun saveProperties() {
        try {
            propertiesFile.parentFile?.mkdirs()
            propertiesFile.outputStream().use { stream ->
                properties.store(stream, "MobileCLI Settings")
            }
            Toast.makeText(this, "Settings saved. Restart app to apply.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to save settings: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addSectionHeader(container: LinearLayout, title: String) {
        container.addView(TextView(this).apply {
            text = title
            textSize = 18f
            setTextColor(0xFF4CAF50.toInt())
            setPadding(0, 32, 0, 16)
        })
    }

    private fun addFullscreenSetting(container: LinearLayout) {
        val switch = Switch(this).apply {
            text = "Fullscreen Mode"
            setTextColor(0xFFFFFFFF.toInt())
            isChecked = properties.getProperty("fullscreen", "false") == "true"
            setOnCheckedChangeListener { _, isChecked ->
                properties.setProperty("fullscreen", if (isChecked) "true" else "false")
            }
        }
        container.addView(switch)
        addDescription(container, "Hide status bar and navigation")
    }

    private fun addTextSizeSetting(container: LinearLayout) {
        val label = TextView(this).apply {
            text = "Default Text Size"
            setTextColor(0xFFFFFFFF.toInt())
        }
        container.addView(label)

        val seekBar = SeekBar(this).apply {
            max = 42  // 14-56
            progress = (properties.getProperty("default-text-size", "28").toIntOrNull() ?: 28) - 14
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    label.text = "Default Text Size: ${progress + 14}"
                    properties.setProperty("default-text-size", (progress + 14).toString())
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        container.addView(seekBar)
    }

    private fun addBackKeySetting(container: LinearLayout) {
        val switch = Switch(this).apply {
            text = "Back Key = Escape"
            setTextColor(0xFFFFFFFF.toInt())
            isChecked = properties.getProperty("back-key", "back") == "escape"
            setOnCheckedChangeListener { _, isChecked ->
                properties.setProperty("back-key", if (isChecked) "escape" else "back")
            }
        }
        container.addView(switch)
        addDescription(container, "Use back button as escape key")
    }

    private fun addHideKeyboardSetting(container: LinearLayout) {
        val switch = Switch(this).apply {
            text = "Hide Keyboard on Startup"
            setTextColor(0xFFFFFFFF.toInt())
            isChecked = properties.getProperty("hide-soft-keyboard-on-startup", "false") == "true"
            setOnCheckedChangeListener { _, isChecked ->
                properties.setProperty("hide-soft-keyboard-on-startup", if (isChecked) "true" else "false")
            }
        }
        container.addView(switch)
    }

    private fun addCursorStyleSetting(container: LinearLayout) {
        val label = TextView(this).apply {
            text = "Cursor Style: ${properties.getProperty("terminal-cursor-style", "block")}"
            setTextColor(0xFFFFFFFF.toInt())
            setOnClickListener {
                val styles = arrayOf("block", "underline", "bar")
                val current = properties.getProperty("terminal-cursor-style", "block")
                val currentIndex = styles.indexOf(current)
                val nextIndex = (currentIndex + 1) % styles.size
                properties.setProperty("terminal-cursor-style", styles[nextIndex])
                text = "Cursor Style: ${styles[nextIndex]}"
            }
        }
        container.addView(label)
        addDescription(container, "Tap to cycle: block, underline, bar")
    }

    private fun addCursorBlinkSetting(container: LinearLayout) {
        val switch = Switch(this).apply {
            text = "Cursor Blink"
            setTextColor(0xFFFFFFFF.toInt())
            isChecked = (properties.getProperty("terminal-cursor-blink-rate", "500").toIntOrNull() ?: 500) > 0
            setOnCheckedChangeListener { _, isChecked ->
                properties.setProperty("terminal-cursor-blink-rate", if (isChecked) "500" else "0")
            }
        }
        container.addView(switch)
    }

    private fun addScrollbackSetting(container: LinearLayout) {
        val label = TextView(this).apply {
            val rows = properties.getProperty("terminal-transcript-rows", "2000")
            text = "Scrollback Buffer: $rows lines"
            setTextColor(0xFFFFFFFF.toInt())
        }
        container.addView(label)

        val seekBar = SeekBar(this).apply {
            max = 8  // 500, 1000, 2000, 4000, 8000, 16000, 32000, 64000, unlimited
            val rows = properties.getProperty("terminal-transcript-rows", "2000").toIntOrNull() ?: 2000
            progress = when {
                rows <= 500 -> 0
                rows <= 1000 -> 1
                rows <= 2000 -> 2
                rows <= 4000 -> 3
                rows <= 8000 -> 4
                rows <= 16000 -> 5
                rows <= 32000 -> 6
                rows <= 64000 -> 7
                else -> 8
            }
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    val values = listOf(500, 1000, 2000, 4000, 8000, 16000, 32000, 64000, 100000)
                    val value = values[progress]
                    label.text = "Scrollback Buffer: $value lines"
                    properties.setProperty("terminal-transcript-rows", value.toString())
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        container.addView(seekBar)
    }

    private fun addBellSetting(container: LinearLayout) {
        val label = TextView(this).apply {
            text = "Bell: ${properties.getProperty("bell-character", "vibrate")}"
            setTextColor(0xFFFFFFFF.toInt())
            setOnClickListener {
                val options = arrayOf("vibrate", "beep", "ignore")
                val current = properties.getProperty("bell-character", "vibrate")
                val currentIndex = options.indexOf(current)
                val nextIndex = (currentIndex + 1) % options.size
                properties.setProperty("bell-character", options[nextIndex])
                text = "Bell: ${options[nextIndex]}"
            }
        }
        container.addView(label)
        addDescription(container, "Tap to cycle: vibrate, beep, ignore")
    }

    private fun addExternalAppsSetting(container: LinearLayout) {
        val switch = Switch(this).apply {
            text = "Allow External Apps"
            setTextColor(0xFFFFFFFF.toInt())
            isChecked = properties.getProperty("allow-external-apps", "true") == "true"
            setOnCheckedChangeListener { _, isChecked ->
                properties.setProperty("allow-external-apps", if (isChecked) "true" else "false")
            }
        }
        container.addView(switch)
        addDescription(container, "Required for Claude Code OAuth and URL opening")
    }

    private fun addDescription(container: LinearLayout, text: String) {
        container.addView(TextView(this).apply {
            this.text = text
            textSize = 12f
            setTextColor(0xFF888888.toInt())
            setPadding(0, 0, 0, 16)
        })
    }

    private fun addSaveButton(container: LinearLayout) {
        container.addView(android.widget.Button(this).apply {
            text = "Save Settings"
            setBackgroundColor(0xFF4CAF50.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(32, 16, 32, 16)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = 48
            layoutParams = params
            setOnClickListener { saveProperties() }
        })
    }
}
