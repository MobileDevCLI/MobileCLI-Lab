package com.termux.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.io.File

/**
 * BootReceiver - Runs scripts from ~/.termux/boot/ when device boots.
 *
 * This is the MobileCLI equivalent of Termux:Boot.
 * Scripts are executed in alphanumeric order.
 *
 * Usage:
 * 1. Create scripts in ~/.termux/boot/
 * 2. Make them executable: chmod +x ~/.termux/boot/your_script.sh
 * 3. Scripts run automatically on device boot
 *
 * Example scripts:
 * - 01-start-ssh.sh - Start SSH server
 * - 02-wake-lock.sh - Acquire wake lock
 * - 03-sync.sh - Start background sync
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        Log.i(TAG, "Boot completed, checking for boot scripts...")

        // Boot scripts location
        val homeDir = File(context.filesDir, "home")
        val bootDir = File(homeDir, ".termux/boot")

        if (!bootDir.exists() || !bootDir.isDirectory) {
            Log.i(TAG, "No boot directory at ${bootDir.absolutePath}")
            return
        }

        // Get executable scripts, sorted by name
        val scripts = bootDir.listFiles()
            ?.filter { it.isFile && it.canExecute() }
            ?.sortedBy { it.name }
            ?: emptyList()

        if (scripts.isEmpty()) {
            Log.i(TAG, "No executable scripts in boot directory")
            return
        }

        Log.i(TAG, "Found ${scripts.size} boot scripts to execute")

        // Execute each script
        scripts.forEach { script ->
            executeScript(context, script)
        }
    }

    private fun executeScript(context: Context, script: File) {
        try {
            Log.i(TAG, "Executing boot script: ${script.name}")

            val homeDir = File(context.filesDir, "home")
            val prefixDir = File(context.filesDir, "usr")
            val bashPath = File(prefixDir, "bin/bash").absolutePath

            // Build environment
            val env = arrayOf(
                "HOME=${homeDir.absolutePath}",
                "PREFIX=${prefixDir.absolutePath}",
                "PATH=${prefixDir.absolutePath}/bin:/system/bin:/system/xbin",
                "LD_LIBRARY_PATH=${prefixDir.absolutePath}/lib",
                "TMPDIR=${prefixDir.absolutePath}/tmp",
                "TERM=xterm-256color",
                "SHELL=$bashPath"
            )

            // Execute the script
            val process = Runtime.getRuntime().exec(
                arrayOf(bashPath, script.absolutePath),
                env,
                homeDir
            )

            // Wait with timeout (10 seconds)
            val completed = process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)

            if (completed) {
                val exitCode = process.exitValue()
                Log.i(TAG, "Boot script ${script.name} completed with exit code $exitCode")
            } else {
                Log.w(TAG, "Boot script ${script.name} timed out, destroying...")
                process.destroyForcibly()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error executing boot script ${script.name}", e)
        }
    }
}
