package com.mobilecli.engine.intents

import android.content.Context
import android.content.Intent
import android.net.LocalServerSocket
import android.net.LocalSocket
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/**
 * Unix Socket Server for am (activity manager) commands
 *
 * This server allows shell scripts to execute am commands through
 * the app's context, bypassing Android's security restrictions that
 * block shell→activity calls.
 *
 * The shell script writes to a Unix socket, the app reads it and
 * executes the Intent with proper permissions.
 *
 * Socket location: /data/data/com.termux/files/apps/com.termux/termux-am/am.sock
 *
 * Protocol:
 * - Client sends: ACTION URI [--es KEY VALUE] [--ez KEY BOOL] ...
 * - Server responds: OK or ERROR: message
 */
class AmSocketServer(
    private val context: Context,
    private val intentExecutor: IntentExecutor
) {
    companion object {
        private const val TAG = "AmSocketServer"
        private const val SOCKET_NAME = "termux-am"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverSocket: LocalServerSocket? = null
    private var isRunning = false
    private val mainHandler = Handler(Looper.getMainLooper())

    // File-based IPC paths (fallback for compatibility)
    private val termuxDir: File get() = File(context.filesDir, "home/.termux")
    private val commandFile: File get() = File(termuxDir, "am_command")
    private val resultFile: File get() = File(termuxDir, "am_result")
    private val urlFile: File get() = File(termuxDir, "url_to_open")

    /**
     * Start the socket server
     */
    fun start() {
        if (isRunning) return

        isRunning = true
        Log.i(TAG, "Starting AmSocketServer...")

        // Ensure .termux directory exists
        termuxDir.mkdirs()

        // Start socket server
        scope.launch {
            startSocketServer()
        }

        // Start file watcher (fallback for older scripts)
        scope.launch {
            startFileWatcher()
        }
    }

    /**
     * Stop the socket server
     */
    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing socket", e)
        }
        scope.cancel()
        Log.i(TAG, "AmSocketServer stopped")
    }

    private suspend fun startSocketServer() {
        try {
            serverSocket = LocalServerSocket(SOCKET_NAME)
            Log.i(TAG, "Socket server listening on: $SOCKET_NAME")

            while (isRunning) {
                try {
                    val client = serverSocket?.accept() ?: break
                    handleClient(client)
                } catch (e: Exception) {
                    if (isRunning) {
                        Log.e(TAG, "Error accepting client", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start socket server", e)
        }
    }

    private fun handleClient(client: LocalSocket) {
        scope.launch {
            try {
                val reader = BufferedReader(InputStreamReader(client.inputStream))
                val writer = OutputStreamWriter(client.outputStream)

                val command = reader.readLine() ?: return@launch
                Log.d(TAG, "Received command: $command")

                val result = executeAmCommand(command)

                writer.write(result)
                writer.write("\n")
                writer.flush()

                client.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error handling client", e)
            }
        }
    }

    private suspend fun startFileWatcher() {
        Log.i(TAG, "Starting file watcher for: ${termuxDir.absolutePath}")

        while (isRunning) {
            try {
                // Check for URL file (fast path)
                if (urlFile.exists()) {
                    val url = urlFile.readText().trim()
                    urlFile.delete()

                    if (url.isNotEmpty()) {
                        Log.d(TAG, "Opening URL from file: $url")
                        withContext(Dispatchers.Main) {
                            intentExecutor.openUrl(url)
                        }
                    }
                }

                // Check for command file (general path)
                if (commandFile.exists()) {
                    val command = commandFile.readText().trim()
                    commandFile.delete()

                    if (command.isNotEmpty()) {
                        Log.d(TAG, "Executing command from file: $command")
                        val result = executeAmCommand(command)
                        resultFile.writeText(result)
                    }
                }

                delay(200) // Poll every 200ms
            } catch (e: Exception) {
                Log.e(TAG, "Error in file watcher", e)
                delay(1000)
            }
        }
    }

    private suspend fun executeAmCommand(command: String): String {
        return try {
            val parts = parseCommand(command)
            if (parts.isEmpty()) {
                return "ERROR: Empty command"
            }

            val action = parts["action"] ?: "android.intent.action.VIEW"
            val uri = parts["uri"]
            val componentName = parts["component"]

            val intent = Intent(action)

            // Set URI if provided
            if (uri != null) {
                intent.data = Uri.parse(uri)
            }

            // Set component if provided
            if (componentName != null) {
                val (pkg, cls) = componentName.split("/", limit = 2)
                intent.setClassName(pkg, if (cls.startsWith(".")) pkg + cls else cls)
            }

            // Add extras
            parts.filter { it.key.startsWith("es_") }.forEach { (key, value) ->
                intent.putExtra(key.removePrefix("es_"), value)
            }
            parts.filter { it.key.startsWith("ez_") }.forEach { (key, value) ->
                intent.putExtra(key.removePrefix("ez_"), value.toBoolean())
            }
            parts.filter { it.key.startsWith("ei_") }.forEach { (key, value) ->
                intent.putExtra(key.removePrefix("ei_"), value.toIntOrNull() ?: 0)
            }

            // Execute on main thread
            withContext(Dispatchers.Main) {
                if (intentExecutor.startActivity(intent)) {
                    "OK: Starting: Intent { act=$action ${if (uri != null) "dat=$uri" else ""} }"
                } else {
                    "ERROR: Failed to start activity"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing command: $command", e)
            "ERROR: ${e.message}"
        }
    }

    private fun parseCommand(command: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val tokens = command.split(" ").toMutableList()

        var i = 0
        while (i < tokens.size) {
            when (tokens[i]) {
                "-a" -> {
                    if (i + 1 < tokens.size) {
                        result["action"] = tokens[++i]
                    }
                }
                "-d" -> {
                    if (i + 1 < tokens.size) {
                        result["uri"] = tokens[++i].removeSurrounding("\"")
                    }
                }
                "-n" -> {
                    if (i + 1 < tokens.size) {
                        result["component"] = tokens[++i].removeSurrounding("\"")
                    }
                }
                "--es" -> {
                    if (i + 2 < tokens.size) {
                        result["es_${tokens[++i]}"] = tokens[++i].removeSurrounding("\"")
                    }
                }
                "--ez" -> {
                    if (i + 2 < tokens.size) {
                        result["ez_${tokens[++i]}"] = tokens[++i]
                    }
                }
                "--ei" -> {
                    if (i + 2 < tokens.size) {
                        result["ei_${tokens[++i]}"] = tokens[++i]
                    }
                }
                "start" -> {
                    // Skip 'start' subcommand
                }
                "--user" -> {
                    // Skip --user and its value
                    i++
                }
                else -> {
                    // Check if it's a URI
                    if (tokens[i].startsWith("http://") || tokens[i].startsWith("https://")) {
                        result["uri"] = tokens[i]
                        result["action"] = "android.intent.action.VIEW"
                    }
                }
            }
            i++
        }

        return result
    }
}
