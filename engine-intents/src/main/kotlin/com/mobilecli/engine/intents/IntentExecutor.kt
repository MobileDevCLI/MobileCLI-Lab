package com.mobilecli.engine.intents

import android.content.Intent
import android.net.Uri

/**
 * Interface for Android Intent execution from shell
 *
 * This module allows shell scripts to launch activities, share content,
 * and interact with other apps - bypassing Android's security restrictions
 * that normally block shell→activity calls.
 *
 * Example usage:
 * ```kotlin
 * val engine = MobileCLIEngine.getInstance()
 * val intents = engine.getIntentExecutor()
 *
 * // Open URL
 * intents.openUrl("https://google.com")
 *
 * // Share text
 * intents.shareText("Hello from MobileCLI!")
 *
 * // Open another app
 * intents.openApp("com.android.settings")
 * ```
 */
interface IntentExecutor {
    /**
     * Open a URL in the default browser
     * @param url The URL to open
     * @return true if intent was sent successfully
     */
    fun openUrl(url: String): Boolean

    /**
     * Share text via the system share sheet
     * @param text The text to share
     * @param title Optional title for the share dialog
     * @return true if intent was sent successfully
     */
    fun shareText(text: String, title: String = "Share"): Boolean

    /**
     * Share a file via the system share sheet
     * @param filePath Path to the file
     * @param mimeType MIME type of the file
     * @param title Optional title for the share dialog
     * @return true if intent was sent successfully
     */
    fun shareFile(filePath: String, mimeType: String, title: String = "Share"): Boolean

    /**
     * Open another app by package name
     * @param packageName The app's package name
     * @return true if app was launched
     */
    fun openApp(packageName: String): Boolean

    /**
     * Start an activity with a custom Intent
     * @param intent The Intent to start
     * @return true if activity was started
     */
    fun startActivity(intent: Intent): Boolean

    /**
     * Send a broadcast
     * @param intent The broadcast Intent
     * @return true if broadcast was sent
     */
    fun sendBroadcast(intent: Intent): Boolean

    /**
     * View a file with the default handler
     * @param filePath Path to the file
     * @param mimeType MIME type of the file
     * @return true if file was opened
     */
    fun viewFile(filePath: String, mimeType: String): Boolean

    /**
     * Make a phone call (requires CALL_PHONE permission)
     * @param phoneNumber The number to call
     * @return true if dialer was opened
     */
    fun dialNumber(phoneNumber: String): Boolean

    /**
     * Send an email
     * @param to Recipient email address
     * @param subject Email subject
     * @param body Email body
     * @return true if email app was opened
     */
    fun sendEmail(to: String, subject: String, body: String): Boolean

    /**
     * Open app settings
     * @param packageName Optional: specific app's settings (default: this app)
     * @return true if settings were opened
     */
    fun openSettings(packageName: String? = null): Boolean
}

/**
 * Result of an Intent execution
 */
data class IntentResult(
    /**
     * Whether the intent was sent successfully
     */
    val success: Boolean,

    /**
     * Error message if failed
     */
    val error: String? = null,

    /**
     * The Intent that was executed
     */
    val intent: Intent? = null
)
