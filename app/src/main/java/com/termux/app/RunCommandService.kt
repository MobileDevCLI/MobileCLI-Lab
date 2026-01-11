package com.termux.app

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import java.io.File

/**
 * Service for executing commands from external apps (like Tasker).
 * This matches real Termux's RunCommandService for RUN_COMMAND intent.
 *
 * External apps can send:
 * - com.termux.RUN_COMMAND action
 * - com.termux.RUN_COMMAND_PATH extra (required): path to script
 * - com.termux.RUN_COMMAND_ARGUMENTS extra (optional): arguments array
 * - com.termux.RUN_COMMAND_WORKDIR extra (optional): working directory
 * - com.termux.RUN_COMMAND_BACKGROUND extra (optional): run in background
 */
class RunCommandService : Service() {

    companion object {
        private const val TAG = "RunCommandService"

        // Intent extras
        const val EXTRA_COMMAND_PATH = "com.termux.RUN_COMMAND_PATH"
        const val EXTRA_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
        const val EXTRA_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
        const val EXTRA_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
        const val EXTRA_SESSION_ACTION = "com.termux.RUN_COMMAND_SESSION_ACTION"

        // Result codes
        const val RESULT_CODE_OK = 0
        const val RESULT_CODE_FAILED = 1
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            Log.e(TAG, "Received null intent")
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val action = intent.action
        if (action != "com.termux.RUN_COMMAND") {
            Log.e(TAG, "Unknown action: $action")
            stopSelf(startId)
            return START_NOT_STICKY
        }

        // Get command path (required)
        val commandPath = intent.getStringExtra(EXTRA_COMMAND_PATH)
        if (commandPath.isNullOrEmpty()) {
            Log.e(TAG, "No command path provided")
            stopSelf(startId)
            return START_NOT_STICKY
        }

        // Get optional extras
        val arguments = intent.getStringArrayExtra(EXTRA_ARGUMENTS) ?: arrayOf()
        val workdir = intent.getStringExtra(EXTRA_WORKDIR)
            ?: "/data/data/com.termux/files/home"
        val background = intent.getBooleanExtra(EXTRA_BACKGROUND, false)

        Log.i(TAG, "Executing command: $commandPath with ${arguments.size} arguments")

        // Execute the command
        Thread {
            executeCommand(commandPath, arguments, workdir, background)
            stopSelf(startId)
        }.start()

        return START_NOT_STICKY
    }

    private fun executeCommand(
        commandPath: String,
        arguments: Array<String>,
        workdir: String,
        background: Boolean
    ) {
        try {
            val commandFile = File(commandPath)
            if (!commandFile.exists()) {
                Log.e(TAG, "Command file does not exist: $commandPath")
                return
            }

            if (!commandFile.canExecute()) {
                Log.e(TAG, "Command file is not executable: $commandPath")
                return
            }

            val workDir = File(workdir)
            if (!workDir.exists()) {
                workDir.mkdirs()
            }

            // Build command array
            val cmdArray = arrayOf(commandPath) + arguments

            // Build environment
            val env = buildEnvironment()

            // Execute
            val process = Runtime.getRuntime().exec(cmdArray, env, workDir)

            if (!background) {
                // Wait for completion
                val exitCode = process.waitFor()
                Log.i(TAG, "Command completed with exit code: $exitCode")

                // Read output for logging
                val output = process.inputStream.bufferedReader().readText()
                val error = process.errorStream.bufferedReader().readText()

                if (output.isNotEmpty()) {
                    Log.d(TAG, "Output: $output")
                }
                if (error.isNotEmpty()) {
                    Log.w(TAG, "Error: $error")
                }
            } else {
                Log.i(TAG, "Command started in background")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute command", e)
        }
    }

    private fun buildEnvironment(): Array<String> {
        val filesDir = applicationContext.filesDir
        val prefixDir = File(filesDir, "usr")
        val homeDir = File(filesDir, "home")
        val binDir = File(prefixDir, "bin")
        val libDir = File(prefixDir, "lib")
        val tmpDir = File(prefixDir, "tmp")

        return arrayOf(
            "HOME=${homeDir.absolutePath}",
            "PREFIX=${prefixDir.absolutePath}",
            "PATH=${binDir.absolutePath}:/system/bin:/system/xbin",
            "LD_LIBRARY_PATH=${libDir.absolutePath}",
            "TMPDIR=${tmpDir.absolutePath}",
            "TERM=xterm-256color",
            "LANG=en_US.UTF-8",
            "SHELL=${binDir.absolutePath}/bash"
        )
    }
}
