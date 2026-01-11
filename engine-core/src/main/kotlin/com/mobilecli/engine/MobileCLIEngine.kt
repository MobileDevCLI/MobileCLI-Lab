package com.mobilecli.engine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * MobileCLI Engine - The main entry point for the SDK
 *
 * Initialize this in your Application class:
 * ```kotlin
 * MobileCLIEngine.initialize(this, EngineConfig())
 * ```
 *
 * Then access modules:
 * ```kotlin
 * val engine = MobileCLIEngine.getInstance()
 * engine.terminal.execute("ls -la")
 * ```
 */
class MobileCLIEngine private constructor(
    private val context: Context,
    private val config: EngineConfig
) {
    companion object {
        private const val TAG = "MobileCLIEngine"

        @Volatile
        private var instance: MobileCLIEngine? = null

        /**
         * Initialize the MobileCLI Engine
         * Call this once in your Application.onCreate()
         */
        fun initialize(context: Context, config: EngineConfig = EngineConfig()): MobileCLIEngine {
            return instance ?: synchronized(this) {
                instance ?: MobileCLIEngine(context.applicationContext, config).also {
                    instance = it
                    it.bootstrap()
                }
            }
        }

        /**
         * Get the initialized engine instance
         * @throws IllegalStateException if not initialized
         */
        fun getInstance(): MobileCLIEngine {
            return instance ?: throw IllegalStateException(
                "MobileCLIEngine not initialized. Call MobileCLIEngine.initialize() first."
            )
        }

        /**
         * Check if engine is initialized
         */
        fun isInitialized(): Boolean = instance != null
    }

    // Coroutine scope for background operations
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Engine state
    private var isBootstrapped = false
    private val lifecycleCallbacks = mutableListOf<EngineLifecycleCallback>()

    // Directory paths
    val filesDir: java.io.File get() = context.filesDir
    val prefixDir: java.io.File get() = java.io.File(filesDir, "usr")
    val homeDir: java.io.File get() = java.io.File(filesDir, "home")
    val binDir: java.io.File get() = java.io.File(prefixDir, "bin")
    val libDir: java.io.File get() = java.io.File(prefixDir, "lib")

    /**
     * Bootstrap the engine - extract Termux environment if needed
     */
    private fun bootstrap() {
        Log.i(TAG, "Bootstrapping MobileCLI Engine...")

        engineScope.launch {
            try {
                // Create required directories
                homeDir.mkdirs()
                prefixDir.mkdirs()
                binDir.mkdirs()
                libDir.mkdirs()

                // Check if bootstrap is needed
                val versionFile = java.io.File(prefixDir, ".mobilecli_sdk_version")
                val needsBootstrap = !versionFile.exists() ||
                        versionFile.readText().trim() != BuildConfig.SDK_VERSION

                if (needsBootstrap) {
                    Log.i(TAG, "Bootstrap needed, extracting environment...")
                    // Bootstrap will be handled by BootstrapManager
                    notifyLifecycleCallback { it.onBootstrapStarted() }
                    // TODO: Extract bootstrap from assets
                    versionFile.writeText(BuildConfig.SDK_VERSION)
                    notifyLifecycleCallback { it.onBootstrapCompleted() }
                }

                isBootstrapped = true
                Log.i(TAG, "MobileCLI Engine bootstrapped successfully")
                notifyLifecycleCallback { it.onEngineReady() }

            } catch (e: Exception) {
                Log.e(TAG, "Bootstrap failed", e)
                notifyLifecycleCallback { it.onError(e) }
            }
        }
    }

    /**
     * Get the application context
     */
    fun getContext(): Context = context

    /**
     * Get the engine configuration
     */
    fun getConfig(): EngineConfig = config

    /**
     * Check if engine is ready
     */
    fun isReady(): Boolean = isBootstrapped

    /**
     * Add a lifecycle callback
     */
    fun addLifecycleCallback(callback: EngineLifecycleCallback) {
        lifecycleCallbacks.add(callback)
    }

    /**
     * Remove a lifecycle callback
     */
    fun removeLifecycleCallback(callback: EngineLifecycleCallback) {
        lifecycleCallbacks.remove(callback)
    }

    private fun notifyLifecycleCallback(action: (EngineLifecycleCallback) -> Unit) {
        lifecycleCallbacks.forEach { action(it) }
    }

    /**
     * Shutdown the engine
     */
    fun shutdown() {
        Log.i(TAG, "Shutting down MobileCLI Engine...")
        notifyLifecycleCallback { it.onShutdown() }
        // Clean up resources
    }

    /**
     * Get environment variables for shell execution
     */
    fun getEnvironment(): Map<String, String> {
        val uid = android.os.Process.myUid()
        val pid = android.os.Process.myPid()

        return mapOf(
            "HOME" to homeDir.absolutePath,
            "PREFIX" to prefixDir.absolutePath,
            "PATH" to "${binDir.absolutePath}:/system/bin:/system/xbin",
            "LD_LIBRARY_PATH" to libDir.absolutePath,
            "TMPDIR" to java.io.File(prefixDir, "tmp").absolutePath,
            "PWD" to homeDir.absolutePath,
            "TERM" to "xterm-256color",
            "COLORTERM" to "truecolor",
            "LANG" to "en_US.UTF-8",
            "SHELL" to "${binDir.absolutePath}/bash",
            "USER" to "u0_a${uid % 100000}",
            "TERMUX_VERSION" to "0.118.0",
            "MOBILECLI_SDK_VERSION" to BuildConfig.SDK_VERSION
        )
    }
}

/**
 * Build configuration - generated at build time
 */
object BuildConfig {
    const val SDK_VERSION = "1.0.0"
    const val DEBUG = true
}
