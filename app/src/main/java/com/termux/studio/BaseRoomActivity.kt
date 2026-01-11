package com.termux.studio

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.termux.HomeActivity
import com.termux.R
import com.termux.activities.SettingsActivity

/**
 * BaseRoomActivity - Foundation for all MobileCLI Studio rooms
 *
 * This abstract activity provides the common framework that all development
 * rooms share: resizable panels, file tree, console, toolbar, and tabs.
 *
 * Layout structure:
 * ┌─────────────────────────────────────────────────────────────┐
 * │ Toolbar (room name, menu, back)                             │
 * ├──────────┬────────────────────────────────┬────────────────┤
 * │ File     │ Main Content Area              │ Properties     │
 * │ Tree     │ (Editor/Terminal/Canvas)       │ Panel          │
 * │ Panel    │                                │ (optional)     │
 * │          │                                │                │
 * ├──────────┴────────────────────────────────┴────────────────┤
 * │ Tab Bar (open files/sessions)                               │
 * ├─────────────────────────────────────────────────────────────┤
 * │ Bottom Panel (Console/Output/Logs)                          │
 * └─────────────────────────────────────────────────────────────┘
 *
 * Each room extends this and provides its specific content.
 */
abstract class BaseRoomActivity : AppCompatActivity() {

    // Layout components
    protected lateinit var toolbar: Toolbar
    protected lateinit var drawerLayout: DrawerLayout
    protected lateinit var leftPanel: LinearLayout
    protected lateinit var mainContent: FrameLayout
    protected lateinit var rightPanel: LinearLayout
    protected lateinit var bottomPanel: LinearLayout
    protected lateinit var tabBar: RecyclerView
    protected lateinit var consoleOutput: TextView

    // Panel states
    protected var isLeftPanelVisible = true
    protected var isRightPanelVisible = false
    protected var isBottomPanelVisible = true

    // Panel dimensions (dp)
    protected var leftPanelWidth = 200
    protected var rightPanelWidth = 200
    protected var bottomPanelHeight = 150

    // Abstract properties - each room must define these
    abstract val roomName: String
    abstract val roomIcon: Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base_room)

        initializeViews()
        setupToolbar()
        setupPanels()
        setupTabBar()
        onRoomCreated()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.room_toolbar)
        drawerLayout = findViewById(R.id.room_drawer_layout)
        leftPanel = findViewById(R.id.left_panel)
        mainContent = findViewById(R.id.main_content)
        rightPanel = findViewById(R.id.right_panel)
        bottomPanel = findViewById(R.id.bottom_panel)
        tabBar = findViewById(R.id.tab_bar)
        consoleOutput = findViewById(R.id.console_output)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = roomName
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu)
        }

        // Toggle left panel with menu button
        toolbar.setNavigationOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }
    }

    private fun setupPanels() {
        // Set initial panel visibility
        updatePanelVisibility()

        // Bottom panel header click to expand/collapse
        findViewById<View>(R.id.bottom_panel_header)?.setOnClickListener {
            toggleBottomPanel()
        }
    }

    private fun setupTabBar() {
        tabBar.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        // Adapter will be set by subclass
    }

    // Panel visibility controls
    fun toggleLeftPanel() {
        isLeftPanelVisible = !isLeftPanelVisible
        updatePanelVisibility()
    }

    fun toggleRightPanel() {
        isRightPanelVisible = !isRightPanelVisible
        updatePanelVisibility()
    }

    fun toggleBottomPanel() {
        isBottomPanelVisible = !isBottomPanelVisible
        updatePanelVisibility()
    }

    private fun updatePanelVisibility() {
        leftPanel.visibility = if (isLeftPanelVisible) View.VISIBLE else View.GONE
        rightPanel.visibility = if (isRightPanelVisible) View.VISIBLE else View.GONE
        bottomPanel.visibility = if (isBottomPanelVisible) View.VISIBLE else View.GONE
    }

    // Console output methods
    fun appendToConsole(text: String) {
        runOnUiThread {
            consoleOutput.append(text + "\n")
            // Auto-scroll to bottom
            val scrollView = consoleOutput.parent as? android.widget.ScrollView
            scrollView?.fullScroll(View.FOCUS_DOWN)
        }
    }

    fun clearConsole() {
        runOnUiThread {
            consoleOutput.text = ""
        }
    }

    fun setConsoleText(text: String) {
        runOnUiThread {
            consoleOutput.text = text
        }
    }

    // Menu handling
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.room_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_home -> {
                goToHome()
                true
            }
            R.id.action_settings -> {
                openSettings()
                true
            }
            R.id.action_toggle_left_panel -> {
                toggleLeftPanel()
                true
            }
            R.id.action_toggle_right_panel -> {
                toggleRightPanel()
                true
            }
            R.id.action_toggle_bottom_panel -> {
                toggleBottomPanel()
                true
            }
            R.id.action_run -> {
                onRunClicked()
                true
            }
            R.id.action_save -> {
                onSaveClicked()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Navigation
    protected fun goToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    protected fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    override fun onBackPressed() {
        when {
            drawerLayout.isDrawerOpen(GravityCompat.START) -> {
                drawerLayout.closeDrawer(GravityCompat.START)
            }
            else -> {
                // Go back to home instead of exiting
                goToHome()
            }
        }
    }

    // Toast helper
    protected fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Abstract methods for subclasses to implement
    abstract fun onRoomCreated()

    // Optional overrides for room-specific actions
    open fun onRunClicked() {
        showToast("Run not implemented for $roomName")
    }

    open fun onSaveClicked() {
        showToast("Save not implemented for $roomName")
    }

    open fun onNewFileClicked() {
        showToast("New file not implemented for $roomName")
    }

    open fun onOpenFileClicked() {
        showToast("Open file not implemented for $roomName")
    }

    // File tree interface - subclasses implement file browsing
    open fun refreshFileTree() {
        // Override in subclass
    }

    open fun navigateToPath(path: String) {
        // Override in subclass
    }

    // Property panel interface
    open fun showProperties(item: Any?) {
        // Override in subclass
    }
}
