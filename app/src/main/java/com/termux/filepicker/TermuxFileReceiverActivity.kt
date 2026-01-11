package com.termux.filepicker

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream

/**
 * Activity that receives shared files from other apps.
 * Files are saved to ~/downloads/ and can be accessed in the terminal.
 * Matches real Termux's TermuxFileReceiverActivity.
 */
class TermuxFileReceiverActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SEND -> handleSend(intent)
            Intent.ACTION_VIEW -> handleView(intent)
            else -> {
                Toast.makeText(this, "Unknown action: ${intent.action}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun handleSend(intent: Intent) {
        val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        if (uri != null) {
            saveFile(uri)
        } else {
            // Handle text share
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (text != null) {
                saveText(text)
            } else {
                Toast.makeText(this, "No content to receive", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun handleView(intent: Intent) {
        val uri = intent.data
        if (uri != null) {
            saveFile(uri)
        } else {
            Toast.makeText(this, "No file to view", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun saveFile(uri: Uri) {
        try {
            // Get filename from URI
            val filename = getFilenameFromUri(uri)

            // Create downloads directory
            val downloadsDir = File(File(filesDir, "home"), "downloads")
            downloadsDir.mkdirs()

            // Generate unique filename if needed
            var destFile = File(downloadsDir, filename)
            var counter = 1
            while (destFile.exists()) {
                val name = filename.substringBeforeLast('.')
                val ext = if (filename.contains('.')) ".${filename.substringAfterLast('.')}" else ""
                destFile = File(downloadsDir, "${name}_$counter$ext")
                counter++
            }

            // Copy content
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            Toast.makeText(
                this,
                "Saved to ~/downloads/${destFile.name}",
                Toast.LENGTH_LONG
            ).show()

            // Launch terminal (optional)
            launchTerminal("File saved: ~/downloads/${destFile.name}")

        } catch (e: Exception) {
            Toast.makeText(this, "Failed to save file: ${e.message}", Toast.LENGTH_LONG).show()
        }

        finish()
    }

    private fun saveText(text: String) {
        try {
            // Create downloads directory
            val downloadsDir = File(File(filesDir, "home"), "downloads")
            downloadsDir.mkdirs()

            // Generate filename
            val timestamp = System.currentTimeMillis()
            val destFile = File(downloadsDir, "shared_text_$timestamp.txt")

            destFile.writeText(text)

            Toast.makeText(
                this,
                "Saved to ~/downloads/${destFile.name}",
                Toast.LENGTH_LONG
            ).show()

            launchTerminal("Text saved: ~/downloads/${destFile.name}")

        } catch (e: Exception) {
            Toast.makeText(this, "Failed to save text: ${e.message}", Toast.LENGTH_LONG).show()
        }

        finish()
    }

    private fun getFilenameFromUri(uri: Uri): String {
        // Try to get filename from content resolver
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                val name = cursor.getString(nameIndex)
                if (!name.isNullOrEmpty()) {
                    return name
                }
            }
        }

        // Fallback to last path segment
        val lastSegment = uri.lastPathSegment
        if (!lastSegment.isNullOrEmpty()) {
            return lastSegment
        }

        // Final fallback
        return "received_file_${System.currentTimeMillis()}"
    }

    private fun launchTerminal(message: String) {
        try {
            val intent = Intent(this, Class.forName("com.termux.MainActivity")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("notification_message", message)
            }
            startActivity(intent)
        } catch (e: Exception) {
            // Terminal not available, ignore
        }
    }
}
