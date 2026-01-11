package com.mobilecli.engine.terminal

import android.util.Log
import com.mobilecli.engine.MobileCLIEngine
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Default implementation of TerminalBridge
 */
class TerminalBridgeImpl(
    private val engine: MobileCLIEngine
) : TerminalBridge {

    companion object {
        private const val TAG = "TerminalBridge"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sessions = ConcurrentHashMap<String, ProcessSession>()
    private var currentDirectory: String = engine.homeDir.absolutePath

    private data class ProcessSession(
        val id: String,
        val name: String,
        val process: Process,
        val startTime: Long
    )

    override suspend fun execute(command: String, timeout: Long): CommandResult {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()

            try {
                val processBuilder = createProcessBuilder(command)
                val process = processBuilder.start()

                val stdout = StringBuilder()
                val stderr = StringBuilder()

                // Read output streams
                val stdoutJob = scope.launch {
                    BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                        reader.forEachLine { stdout.appendLine(it) }
                    }
                }

                val stderrJob = scope.launch {
                    BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                        reader.forEachLine { stderr.appendLine(it) }
                    }
                }

                // Wait for completion with optional timeout
                val exitCode = if (timeout > 0) {
                    withTimeoutOrNull(timeout) {
                        stdoutJob.join()
                        stderrJob.join()
                        process.waitFor()
                    } ?: run {
                        process.destroyForcibly()
                        -1 // Timeout
                    }
                } else {
                    stdoutJob.join()
                    stderrJob.join()
                    process.waitFor()
                }

                val duration = System.currentTimeMillis() - startTime

                CommandResult(
                    stdout = stdout.toString().trimEnd(),
                    stderr = stderr.toString().trimEnd(),
                    exitCode = exitCode,
                    command = command,
                    durationMs = duration
                )
            } catch (e: Exception) {
                Log.e(TAG, "Command execution failed: $command", e)
                CommandResult(
                    stdout = "",
                    stderr = e.message ?: "Unknown error",
                    exitCode = -1,
                    command = command,
                    durationMs = System.currentTimeMillis() - startTime
                )
            }
        }
    }

    override fun executeStreaming(command: String, onOutput: (String) -> Unit): Job {
        return scope.launch {
            try {
                val processBuilder = createProcessBuilder(command)
                val process = processBuilder.redirectErrorStream(true).start()

                BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        withContext(Dispatchers.Main) {
                            onOutput(line!!)
                        }
                    }
                }

                process.waitFor()
            } catch (e: Exception) {
                Log.e(TAG, "Streaming execution failed: $command", e)
                withContext(Dispatchers.Main) {
                    onOutput("Error: ${e.message}")
                }
            }
        }
    }

    override suspend fun executeBatch(commands: List<String>, stopOnError: Boolean): List<CommandResult> {
        val results = mutableListOf<CommandResult>()

        for (cmd in commands) {
            val result = execute(cmd)
            results.add(result)

            if (stopOnError && !result.success) {
                break
            }
        }

        return results
    }

    override fun startSession(): SessionHandle {
        val id = UUID.randomUUID().toString()
        val name = "Session ${sessions.size + 1}"

        val processBuilder = ProcessBuilder(getShellPath())
        processBuilder.environment().putAll(engine.getEnvironment())
        processBuilder.directory(File(currentDirectory))

        val process = processBuilder.start()

        val session = ProcessSession(
            id = id,
            name = name,
            process = process,
            startTime = System.currentTimeMillis()
        )
        sessions[id] = session

        return SessionHandle(
            id = id,
            name = name,
            pid = getPid(process),
            isAlive = process.isAlive,
            cwd = currentDirectory
        )
    }

    override fun sendInput(sessionId: String, input: String) {
        sessions[sessionId]?.let { session ->
            try {
                session.process.outputStream.write("$input\n".toByteArray())
                session.process.outputStream.flush()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send input to session $sessionId", e)
            }
        }
    }

    override fun sendSignal(sessionId: String, signal: Int) {
        val session = sessions[sessionId] ?: return
        try {
            val pid = getPid(session.process)
            if (pid > 0) {
                Runtime.getRuntime().exec(arrayOf("kill", "-$signal", pid.toString()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send signal $signal to session $sessionId", e)
        }
    }

    override fun killSession(sessionId: String) {
        sessions.remove(sessionId)?.let { session ->
            try {
                session.process.destroyForcibly()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to kill session $sessionId", e)
            }
        }
    }

    override fun getSessions(): List<SessionHandle> {
        return sessions.values.map { session ->
            SessionHandle(
                id = session.id,
                name = session.name,
                pid = getPid(session.process),
                isAlive = session.process.isAlive,
                cwd = currentDirectory
            )
        }
    }

    override suspend fun hasCommand(command: String): Boolean {
        val result = execute("command -v $command")
        return result.success && result.stdout.isNotEmpty()
    }

    override fun getCurrentDirectory(): String = currentDirectory

    override fun setCurrentDirectory(path: String) {
        if (File(path).isDirectory) {
            currentDirectory = path
        }
    }

    private fun createProcessBuilder(command: String): ProcessBuilder {
        val shell = getShellPath()
        val processBuilder = ProcessBuilder(shell, "-c", command)
        processBuilder.environment().putAll(engine.getEnvironment())
        processBuilder.directory(File(currentDirectory))
        return processBuilder
    }

    private fun getShellPath(): String {
        val config = engine.getConfig()
        return if (config.shellPath.isNotEmpty()) {
            config.shellPath
        } else {
            File(engine.binDir, "bash").absolutePath
        }
    }

    private fun getPid(process: Process): Int {
        return try {
            val field = process.javaClass.getDeclaredField("pid")
            field.isAccessible = true
            field.getInt(process)
        } catch (e: Exception) {
            -1
        }
    }
}
