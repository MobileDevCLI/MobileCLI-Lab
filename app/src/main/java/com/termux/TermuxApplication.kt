package com.termux

import android.app.Application
import android.util.Log

/**
 * Application class for MobileCLI.
 * Provides app-wide initialization and lifecycle management.
 * This matches real Termux's TermuxApplication.
 */
class TermuxApplication : Application() {

    companion object {
        private const val TAG = "TermuxApplication"

        @Volatile
        private var instance: TermuxApplication? = null

        fun getInstance(): TermuxApplication? = instance
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        Log.i(TAG, "MobileCLI Application starting")

        // Initialize crash handler
        setupCrashHandler()

        // Pre-create directories
        initializeDirectories()

        Log.i(TAG, "MobileCLI Application initialized")
    }

    /**
     * Setup global crash handler for better error reporting.
     */
    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Uncaught exception in thread ${thread.name}", throwable)

            // Log to file for debugging
            try {
                val crashFile = java.io.File(filesDir, "crash.log")
                val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
                    .format(java.util.Date())
                crashFile.appendText("=== Crash at $timestamp ===\n")
                crashFile.appendText("Thread: ${thread.name}\n")
                crashFile.appendText("Exception: ${throwable.message}\n")
                crashFile.appendText(throwable.stackTraceToString())
                crashFile.appendText("\n\n")
            } catch (e: Exception) {
                // Ignore logging errors
            }

            // Call default handler
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    /**
     * Pre-create essential directories.
     */
    private fun initializeDirectories() {
        try {
            // Create Termux directory structure
            val files = filesDir  // /data/data/com.termux/files
            val usr = java.io.File(files, "usr")
            val home = java.io.File(files, "home")
            val termuxDir = java.io.File(home, ".termux")
            val bootDir = java.io.File(termuxDir, "boot")

            usr.mkdirs()
            home.mkdirs()
            termuxDir.mkdirs()
            bootDir.mkdirs()

            // Create default termux.properties if it doesn't exist
            val propertiesFile = java.io.File(termuxDir, "termux.properties")
            if (!propertiesFile.exists()) {
                propertiesFile.writeText(DEFAULT_TERMUX_PROPERTIES)
                Log.i(TAG, "Created default termux.properties")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize directories", e)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        instance = null
        Log.i(TAG, "MobileCLI Application terminated")
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Log.w(TAG, "Low memory warning")
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_MODERATE) {
            Log.w(TAG, "Trim memory level: $level")
        }
    }
}

/**
 * Default termux.properties content.
 * Users can customize this file at ~/.termux/termux.properties
 */
private const val DEFAULT_TERMUX_PROPERTIES = """
# MobileCLI Terminal Properties
# Documentation: https://wiki.termux.com/wiki/Terminal_Settings

### Keyboard Settings ###

# Extra keys configuration (JSON format)
# Default keys: ESC, CTRL, ALT, TAB, -, /, |, HOME, UP, END, PGUP
# extra-keys = [['ESC','/','-','HOME','UP','END','PGUP'],['TAB','CTRL','ALT','LEFT','DOWN','RIGHT','PGDN']]

# Back key behavior: "back" (default) or "escape"
# back-key = back

### Appearance ###

# Use fullscreen mode
# fullscreen = false

# Hide soft keyboard on startup
# hide-soft-keyboard-on-startup = false

### Terminal Settings ###

# Terminal transcript rows (scrollback buffer)
# terminal-transcript-rows = 2000

# Cursor blink rate in ms (0 = no blink)
# terminal-cursor-blink-rate = 500

# Cursor style: "block", "underline", or "bar"
# terminal-cursor-style = block

### Bell Settings ###

# Bell character behavior: "vibrate", "beep", "ignore"
# bell-character = vibrate

### URL/External App Settings ###

# Allow external apps to open URLs and files
# This must be true for Claude Code OAuth to work!
allow-external-apps = true

### Session Settings ###

# Default working directory
# default-working-directory = /data/data/com.termux/files/home

"""
