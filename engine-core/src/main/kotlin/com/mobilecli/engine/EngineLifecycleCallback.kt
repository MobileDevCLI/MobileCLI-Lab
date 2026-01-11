package com.mobilecli.engine

/**
 * Callback interface for engine lifecycle events
 *
 * Implement this to receive notifications about engine state changes.
 *
 * Example:
 * ```kotlin
 * engine.addLifecycleCallback(object : EngineLifecycleCallback {
 *     override fun onEngineReady() {
 *         // Engine is ready to use
 *         engine.terminal.execute("echo 'Hello!'")
 *     }
 *
 *     override fun onError(error: Exception) {
 *         Log.e("MyApp", "Engine error", error)
 *     }
 * })
 * ```
 */
interface EngineLifecycleCallback {
    /**
     * Called when bootstrap process starts
     * This happens on first launch or after SDK version update
     */
    fun onBootstrapStarted() {}

    /**
     * Called with bootstrap progress updates
     * @param progress Progress percentage (0-100)
     * @param message Description of current operation
     */
    fun onBootstrapProgress(progress: Int, message: String) {}

    /**
     * Called when bootstrap process completes successfully
     */
    fun onBootstrapCompleted() {}

    /**
     * Called when engine is fully initialized and ready to use
     * You can safely call engine methods after this
     */
    fun onEngineReady() {}

    /**
     * Called when an error occurs during bootstrap or operation
     * @param error The exception that occurred
     */
    fun onError(error: Exception) {}

    /**
     * Called when engine is shutting down
     */
    fun onShutdown() {}
}

/**
 * Simple adapter class for EngineLifecycleCallback
 * Extend this if you only want to override specific methods
 */
open class EngineLifecycleAdapter : EngineLifecycleCallback
