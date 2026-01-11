package com.termux.studio.rooms

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.termux.MainActivity
import com.termux.R
import com.termux.studio.BaseRoomActivity

/**
 * TerminalRoomActivity - Raw Terminal Access
 *
 * Priority #3 Room: Full terminal with Claude Code access
 *
 * This room provides a direct link to the full terminal (MainActivity)
 * with quick actions and session management.
 *
 * Features:
 * - Launch full terminal
 * - Quick commands (claude, pkg install, etc.)
 * - Session history
 * - Command snippets
 */
class TerminalRoomActivity : BaseRoomActivity() {

    override val roomName = "Terminal"
    override val roomIcon = R.drawable.ic_terminal

    override fun onRoomCreated() {
        setupQuickActions()
        setupCommandSnippets()

        // Hide file tree for terminal room
        leftPanel.visibility = View.GONE
        isLeftPanelVisible = false

        // Show launch button in center
        showTerminalLauncher()
    }

    private fun showTerminalLauncher() {
        mainContent.removeAllViews()

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            setPadding(32, 32, 32, 32)
        }

        // Terminal icon/title
        val title = TextView(this).apply {
            text = "Terminal"
            textSize = 28f
            setTextColor(resources.getColor(R.color.text_primary, null))
            gravity = android.view.Gravity.CENTER
        }
        container.addView(title)

        val subtitle = TextView(this).apply {
            text = "Full terminal access with Claude Code"
            textSize = 14f
            setTextColor(resources.getColor(R.color.text_secondary, null))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 8, 0, 32)
        }
        container.addView(subtitle)

        // Launch button
        val launchButton = Button(this).apply {
            text = "Open Terminal"
            setBackgroundColor(resources.getColor(R.color.primary, null))
            setTextColor(resources.getColor(R.color.on_primary, null))
            setPadding(48, 24, 48, 24)
            setOnClickListener {
                openFullTerminal()
            }
        }
        container.addView(launchButton)

        // Quick actions section
        val quickActionsTitle = TextView(this).apply {
            text = "Quick Actions"
            textSize = 16f
            setTextColor(resources.getColor(R.color.text_primary, null))
            setPadding(0, 48, 0, 16)
        }
        container.addView(quickActionsTitle)

        // Quick action buttons
        val quickActions = listOf(
            "Launch Claude" to "claude",
            "Update Packages" to "pkg update && pkg upgrade -y",
            "Install Node.js" to "pkg install nodejs-lts -y",
            "Install Python" to "pkg install python -y",
            "System Info" to "termux-info"
        )

        quickActions.forEach { (name, command) ->
            val btn = Button(this).apply {
                text = name
                setBackgroundColor(resources.getColor(R.color.surface, null))
                setTextColor(resources.getColor(R.color.text_primary, null))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 8, 0, 0)
                }
                setOnClickListener {
                    openTerminalWithCommand(command)
                }
            }
            container.addView(btn)
        }

        mainContent.addView(container)
    }

    private fun setupQuickActions() {
        // Already handled in showTerminalLauncher
    }

    private fun setupCommandSnippets() {
        // Common commands shown in console
        appendToConsole("=== Terminal Room ===")
        appendToConsole("")
        appendToConsole("Quick Commands:")
        appendToConsole("  claude          - Start Claude Code AI")
        appendToConsole("  pkg install X   - Install package X")
        appendToConsole("  pkg search X    - Search for packages")
        appendToConsole("  pkg update      - Update package list")
        appendToConsole("  termux-info     - Show system info")
        appendToConsole("")
        appendToConsole("Tap 'Open Terminal' to start.")
    }

    private fun openFullTerminal() {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("room", "terminal")
        startActivity(intent)
    }

    private fun openTerminalWithCommand(command: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("room", "terminal")
        intent.putExtra("initial_command", command)
        startActivity(intent)
    }

    override fun onRunClicked() {
        openFullTerminal()
    }

    override fun onSaveClicked() {
        showToast("Nothing to save in Terminal room")
    }
}
