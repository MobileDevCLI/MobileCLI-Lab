package com.mobilecli.engine.terminal

import kotlinx.coroutines.Job

/**
 * Interface for terminal/shell execution
 *
 * This is the main interface for running shell commands from your app.
 *
 * Example usage:
 * ```kotlin
 * val engine = MobileCLIEngine.getInstance()
 * val terminal = engine.getTerminal()
 *
 * // Simple command
 * val result = terminal.execute("ls -la")
 * println(result.stdout)
 *
 * // Streaming output
 * terminal.executeStreaming("npm install") { line ->
 *     println(line)
 * }
 *
 * // Batch commands
 * val results = terminal.executeBatch(listOf(
 *     "pkg update -y",
 *     "pkg install python -y"
 * ))
 * ```
 */
interface TerminalBridge {
    /**
     * Execute a command and wait for completion
     * @param command The shell command to execute
     * @param timeout Timeout in milliseconds (default: no timeout)
     * @return CommandResult with stdout, stderr, and exit code
     */
    suspend fun execute(command: String, timeout: Long = 0): CommandResult

    /**
     * Execute a command with streaming output
     * @param command The shell command to execute
     * @param onOutput Callback for each line of output
     * @return Job that can be cancelled
     */
    fun executeStreaming(command: String, onOutput: (String) -> Unit): Job

    /**
     * Execute multiple commands in sequence
     * @param commands List of commands to execute
     * @param stopOnError If true, stops execution on first error
     * @return List of CommandResults
     */
    suspend fun executeBatch(commands: List<String>, stopOnError: Boolean = true): List<CommandResult>

    /**
     * Start an interactive session
     * @return SessionHandle for the session
     */
    fun startSession(): SessionHandle

    /**
     * Send input to a running session
     * @param sessionId The session ID
     * @param input Text to send to stdin
     */
    fun sendInput(sessionId: String, input: String)

    /**
     * Send a signal to a session (e.g., SIGINT for Ctrl+C)
     * @param sessionId The session ID
     * @param signal The signal number (e.g., 2 for SIGINT)
     */
    fun sendSignal(sessionId: String, signal: Int)

    /**
     * Kill a session
     * @param sessionId The session ID
     */
    fun killSession(sessionId: String)

    /**
     * Get all active sessions
     * @return List of active SessionHandles
     */
    fun getSessions(): List<SessionHandle>

    /**
     * Check if a command is available
     * @param command The command name (e.g., "python", "node")
     * @return true if command exists in PATH
     */
    suspend fun hasCommand(command: String): Boolean

    /**
     * Get the current working directory
     */
    fun getCurrentDirectory(): String

    /**
     * Set the current working directory
     */
    fun setCurrentDirectory(path: String)
}

/**
 * Result of executing a command
 */
data class CommandResult(
    /**
     * Standard output from the command
     */
    val stdout: String,

    /**
     * Standard error from the command
     */
    val stderr: String,

    /**
     * Exit code (0 = success)
     */
    val exitCode: Int,

    /**
     * The command that was executed
     */
    val command: String,

    /**
     * Execution time in milliseconds
     */
    val durationMs: Long
) {
    /**
     * True if command succeeded (exit code 0)
     */
    val success: Boolean get() = exitCode == 0

    /**
     * Combined stdout and stderr
     */
    val output: String get() = if (stderr.isNotEmpty()) "$stdout\n$stderr" else stdout
}

/**
 * Handle for an interactive terminal session
 */
data class SessionHandle(
    /**
     * Unique session identifier
     */
    val id: String,

    /**
     * Session name (for display)
     */
    val name: String,

    /**
     * PID of the shell process
     */
    val pid: Int,

    /**
     * True if session is still running
     */
    val isAlive: Boolean,

    /**
     * Current working directory
     */
    val cwd: String
)
