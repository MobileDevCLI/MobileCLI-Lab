package com.termux

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.termux.studio.rooms.MobileAppsRoomActivity
import com.termux.studio.rooms.WebDevRoomActivity
import com.termux.studio.rooms.TerminalRoomActivity
import com.termux.studio.rooms.DataScienceRoomActivity
import com.termux.studio.rooms.ApiBackendRoomActivity
import com.termux.studio.rooms.AiMlRoomActivity

/**
 * MobileCLI Studio Home Screen
 *
 * This is the main launcher activity after bootstrap is complete.
 * Users select a workspace ("room") from here.
 */
class HomeActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        setupDrawer()
        setupRoomCards()
        setupNavigation()
        setupRecentProjects()
        setupQuickTerminal()
    }

    private fun setupDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout)

        findViewById<View>(R.id.btn_menu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        findViewById<View>(R.id.btn_settings).setOnClickListener {
            openSettings()
        }

        // Set version in drawer
        val versionText = findViewById<TextView>(R.id.drawer_version)
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            versionText.text = "v${pInfo.versionName}"
        } catch (e: Exception) {
            versionText.text = "v3.0.0"
        }
    }

    private fun setupRoomCards() {
        // Mobile Apps Card
        findViewById<CardView>(R.id.card_mobile_apps).setOnClickListener {
            openRoom(Room.MOBILE_APPS)
        }

        // Web Dev Card
        findViewById<CardView>(R.id.card_web_dev).setOnClickListener {
            openRoom(Room.WEB_DEV)
        }

        // Terminal Card
        findViewById<CardView>(R.id.card_terminal).setOnClickListener {
            openRoom(Room.TERMINAL)
        }

        // Data Science Card
        findViewById<CardView>(R.id.card_data_science).setOnClickListener {
            openRoom(Room.DATA_SCIENCE)
        }

        // API Backend Card
        findViewById<CardView>(R.id.card_api_backend).setOnClickListener {
            openRoom(Room.API_BACKEND)
        }

        // AI/ML Card
        findViewById<CardView>(R.id.card_ai_ml).setOnClickListener {
            openRoom(Room.AI_ML)
        }
    }

    private fun setupNavigation() {
        // Drawer navigation items
        findViewById<View>(R.id.nav_mobile_apps).setOnClickListener {
            drawerLayout.closeDrawers()
            openRoom(Room.MOBILE_APPS)
        }

        findViewById<View>(R.id.nav_web_dev).setOnClickListener {
            drawerLayout.closeDrawers()
            openRoom(Room.WEB_DEV)
        }

        findViewById<View>(R.id.nav_terminal).setOnClickListener {
            drawerLayout.closeDrawers()
            openRoom(Room.TERMINAL)
        }

        findViewById<View>(R.id.nav_data_science).setOnClickListener {
            drawerLayout.closeDrawers()
            openRoom(Room.DATA_SCIENCE)
        }

        findViewById<View>(R.id.nav_api_backend).setOnClickListener {
            drawerLayout.closeDrawers()
            openRoom(Room.API_BACKEND)
        }

        findViewById<View>(R.id.nav_ai_ml).setOnClickListener {
            drawerLayout.closeDrawers()
            openRoom(Room.AI_ML)
        }

        findViewById<View>(R.id.nav_settings).setOnClickListener {
            drawerLayout.closeDrawers()
            openSettings()
        }

        findViewById<View>(R.id.nav_help).setOnClickListener {
            drawerLayout.closeDrawers()
            showHelp()
        }

        findViewById<View>(R.id.nav_about).setOnClickListener {
            drawerLayout.closeDrawers()
            showAbout()
        }
    }

    private fun setupRecentProjects() {
        val recyclerView = findViewById<RecyclerView>(R.id.recent_projects_list)
        val noRecentText = findViewById<View>(R.id.no_recent_text)

        // TODO: Load recent projects from SharedPreferences
        val recentProjects = loadRecentProjects()

        if (recentProjects.isEmpty()) {
            recyclerView.visibility = View.GONE
            noRecentText.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            noRecentText.visibility = View.GONE
            recyclerView.layoutManager = LinearLayoutManager(this)
            // TODO: Set adapter with recent projects
        }
    }

    private fun setupQuickTerminal() {
        findViewById<View>(R.id.btn_quick_terminal).setOnClickListener {
            openRoom(Room.TERMINAL)
        }
    }

    private fun openRoom(room: Room) {
        when (room) {
            Room.MOBILE_APPS -> {
                // Open Mobile Apps development room
                startActivity(Intent(this, MobileAppsRoomActivity::class.java))
            }
            Room.WEB_DEV -> {
                // Open Web Development room
                startActivity(Intent(this, WebDevRoomActivity::class.java))
            }
            Room.TERMINAL -> {
                // Open Terminal room (raw terminal access)
                startActivity(Intent(this, TerminalRoomActivity::class.java))
            }
            Room.DATA_SCIENCE -> {
                // Open Data Science room (Python notebooks)
                startActivity(Intent(this, DataScienceRoomActivity::class.java))
            }
            Room.API_BACKEND -> {
                // Open API/Backend room (REST testing)
                startActivity(Intent(this, ApiBackendRoomActivity::class.java))
            }
            Room.AI_ML -> {
                // Open AI/ML room (Device Control - 84 APIs)
                startActivity(Intent(this, AiMlRoomActivity::class.java))
            }
        }
    }

    private fun openSettings() {
        // TODO: Open settings activity
        Toast.makeText(this, "Settings coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun showHelp() {
        Toast.makeText(this, "Help documentation coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun showAbout() {
        val message = """
            MobileCLI Studio

            Build apps directly on your Android phone.
            Powered by Claude Code AI.

            Created by Samblamz
            January 2026
        """.trimIndent()

        android.app.AlertDialog.Builder(this)
            .setTitle("About MobileCLI Studio")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun loadRecentProjects(): List<RecentProject> {
        // TODO: Load from SharedPreferences
        return emptyList()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    // Room enum
    enum class Room {
        MOBILE_APPS,
        WEB_DEV,
        TERMINAL,
        DATA_SCIENCE,
        API_BACKEND,
        AI_ML
    }

    // Recent project data class
    data class RecentProject(
        val name: String,
        val path: String,
        val room: Room,
        val lastOpened: Long
    )
}
