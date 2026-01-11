package com.termux

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log

/**
 * Activity that dispatches intents from the terminal's `am` command.
 *
 * v52 FIX: Uses PendingIntent.send() which runs with OUR app's identity,
 * not the caller's identity. This bypasses assertPackageMatchesCallingUid().
 *
 * The problem was: startActivity() inherits the caller's UID/package identity,
 * which fails Android's security check when launching external apps.
 *
 * Solution: PendingIntent.send() always runs with the identity of the app
 * that CREATED the PendingIntent (us, com.termux), not the caller.
 */
class TermuxAmDispatcherActivity : Activity() {

    companion object {
        private const val TAG = "TermuxAmDispatcher"
        private const val REQUEST_CODE = 12345
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            val dispatchIntent = buildIntentFromExtras()

            if (dispatchIntent != null) {
                Log.i(TAG, "Dispatching via PendingIntent: action=${dispatchIntent.action}, data=${dispatchIntent.data}")

                // Add required flags
                dispatchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                // v52 FIX: Use PendingIntent which runs with OUR identity
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }

                val pendingIntent = PendingIntent.getActivity(
                    this,
                    REQUEST_CODE,
                    dispatchIntent,
                    flags
                )

                // send() runs with OUR app's identity, not the caller's
                pendingIntent.send()
                Log.i(TAG, "PendingIntent sent successfully")
            } else {
                Log.e(TAG, "Failed to build intent from extras")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dispatch intent", e)
        }

        // Always finish - we're just a pass-through
        finish()
    }

    private fun buildIntentFromExtras(): Intent? {
        val extras = intent.extras ?: return null

        // Get the action (required for most intents)
        val action = extras.getString("action")

        // Get data URI
        val dataString = extras.getString("data")
        val data = if (!dataString.isNullOrEmpty()) Uri.parse(dataString) else null

        // Get MIME type
        val type = extras.getString("type")

        // Get target component
        val targetPackage = extras.getString("package")
        val targetClass = extras.getString("class")

        // Get category
        val category = extras.getString("category")

        // Build the intent
        val dispatchIntent = Intent()

        if (!action.isNullOrEmpty()) {
            dispatchIntent.action = action
        }

        if (data != null && !type.isNullOrEmpty()) {
            dispatchIntent.setDataAndType(data, type)
        } else if (data != null) {
            dispatchIntent.data = data
        } else if (!type.isNullOrEmpty()) {
            dispatchIntent.type = type
        }

        if (!targetPackage.isNullOrEmpty() && !targetClass.isNullOrEmpty()) {
            dispatchIntent.setClassName(targetPackage, targetClass)
        } else if (!targetPackage.isNullOrEmpty()) {
            dispatchIntent.`package` = targetPackage
        }

        if (!category.isNullOrEmpty()) {
            dispatchIntent.addCategory(category)
        }

        // If we have nothing to dispatch, return null
        if (dispatchIntent.action == null && dispatchIntent.data == null &&
            dispatchIntent.component == null) {
            return null
        }

        return dispatchIntent
    }
}
