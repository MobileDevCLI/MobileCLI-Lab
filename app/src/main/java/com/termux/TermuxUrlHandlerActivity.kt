package com.termux

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast

/**
 * TermuxUrlHandlerActivity - Opens URLs in the browser from Activity context
 *
 * WHY THIS EXISTS:
 * Android restricts starting activities from background/service context.
 * Shell commands like `am start -a android.intent.action.VIEW` run in background
 * context and may be blocked by Android's background activity launch restrictions.
 *
 * This Activity serves as a bridge:
 * 1. Shell script calls: am start -n com.termux/.TermuxUrlHandlerActivity --es url "https://..."
 * 2. This Activity receives the intent (foreground context)
 * 3. This Activity starts the browser (allowed from foreground context)
 * 4. This Activity finishes immediately
 *
 * This is how real Termux handles URL opening - through Activity context, not shell context.
 */
class TermuxUrlHandlerActivity : Activity() {

    companion object {
        private const val TAG = "TermuxUrlHandler"
        const val EXTRA_URL = "url"
        const val EXTRA_CONTENT_TYPE = "content_type"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "TermuxUrlHandlerActivity started!")
        Log.d(TAG, "Intent: $intent")
        Log.d(TAG, "Intent extras: ${intent.extras}")

        try {
            handleIntent(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling URL intent", e)
            Toast.makeText(this, "Error opening URL: ${e.message}", Toast.LENGTH_LONG).show()
        }

        // Always finish immediately - we're just a pass-through
        finish()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            try {
                handleIntent(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error handling URL intent", e)
            }
        }
        finish()
    }

    private fun handleIntent(intent: Intent) {
        // Get URL from extras (our custom way)
        var urlString = intent.getStringExtra(EXTRA_URL)
        Log.d(TAG, "URL from EXTRA_URL: $urlString")

        // Also try getting from data URI (standard way)
        if (urlString == null) {
            urlString = intent.dataString
            Log.d(TAG, "URL from dataString: $urlString")
        }

        // Also try EXTRA_TEXT (for share intents)
        if (urlString == null) {
            urlString = intent.getStringExtra(Intent.EXTRA_TEXT)
            Log.d(TAG, "URL from EXTRA_TEXT: $urlString")
        }

        if (urlString.isNullOrBlank()) {
            Log.w(TAG, "No URL provided in intent")
            Toast.makeText(this, "No URL provided", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "Opening URL: $urlString")
        Toast.makeText(this, "Opening: $urlString", Toast.LENGTH_SHORT).show()

        // Create the view intent
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(urlString)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            // Add content type if provided
            intent.getStringExtra(EXTRA_CONTENT_TYPE)?.let { contentType ->
                setDataAndType(Uri.parse(urlString), contentType)
            }
        }

        // IMPORTANT: Use chooser to avoid Samsung Internet blocking issue
        // Samsung Internet blocks URLs from certain apps, but chooser works around this
        try {
            // Always use chooser to let user pick browser and avoid Samsung restrictions
            val chooser = Intent.createChooser(viewIntent, "Open URL with")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(chooser)
            Log.d(TAG, "Successfully opened chooser for URL: $urlString")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open chooser for URL: $urlString", e)

            // Fallback: try direct intent
            try {
                startActivity(viewIntent)
                Log.d(TAG, "Opened directly (fallback)")
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to open URL even directly", e2)
                Toast.makeText(this, "Cannot open URL: ${e2.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
