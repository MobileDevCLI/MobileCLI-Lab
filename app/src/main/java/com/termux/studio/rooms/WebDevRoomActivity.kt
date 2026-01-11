package com.termux.studio.rooms

import android.os.Bundle
import android.view.LayoutInflater
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.termux.R
import com.termux.studio.BaseRoomActivity
import java.io.File

/**
 * WebDevRoomActivity - Web Development Environment
 *
 * Priority #2 Room: Build HTML/CSS/JS websites with live preview
 *
 * Features:
 * - HTML/CSS/JS code editor
 * - Live preview in WebView
 * - File management
 * - Built-in web server
 * - Console output
 */
class WebDevRoomActivity : BaseRoomActivity() {

    override val roomName = "Web Dev"
    override val roomIcon = R.drawable.ic_web_dev

    private var webPreview: WebView? = null
    private var codeEditor: EditText? = null
    private var currentFile: File? = null
    private var currentProjectPath: String? = null
    private var isPreviewVisible = true

    override fun onRoomCreated() {
        setupFileTree()
        setupCodeEditor()
        setupWebPreview()
        setupBottomPanel()

        // Show project dialog
        showProjectDialog()
    }

    private fun setupFileTree() {
        val fileTree = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.file_tree)
        fileTree?.layoutManager = LinearLayoutManager(this)

        findViewById<ImageButton>(R.id.btn_refresh_files)?.setOnClickListener {
            refreshFileTree()
        }

        findViewById<ImageButton>(R.id.btn_new_file)?.setOnClickListener {
            showNewFileDialog()
        }
    }

    private fun setupCodeEditor() {
        val editorView = LayoutInflater.from(this)
            .inflate(R.layout.view_code_editor, mainContent, false)
        mainContent.addView(editorView)

        codeEditor = editorView.findViewById(R.id.code_editor_text)
        findViewById<TextView>(R.id.content_placeholder)?.visibility = android.view.View.GONE
    }

    private fun setupWebPreview() {
        // Create WebView for live preview
        webPreview = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = WebViewClient()
        }

        // Add preview toggle button
        rightPanel.visibility = android.view.View.VISIBLE
        rightPanel.removeAllViews()

        val previewContainer = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        val header = android.widget.TextView(this).apply {
            text = "Live Preview"
            setTextColor(resources.getColor(R.color.text_primary, null))
            setBackgroundColor(resources.getColor(R.color.surface_variant, null))
            setPadding(16, 8, 16, 8)
        }

        previewContainer.addView(header)
        previewContainer.addView(webPreview)
        rightPanel.addView(previewContainer)

        isRightPanelVisible = true
    }

    private fun setupBottomPanel() {
        // Console for web errors/logs
        findViewById<TextView>(R.id.tab_console)?.setOnClickListener {
            appendToConsole("Console ready")
        }
    }

    private fun showProjectDialog() {
        val options = arrayOf(
            "Create New Website",
            "Open Existing Project",
            "Start Empty"
        )

        AlertDialog.Builder(this)
            .setTitle("Web Development")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showCreateWebsiteDialog()
                    1 -> showOpenProjectDialog()
                    2 -> createEmptyProject()
                }
            }
            .setNegativeButton("Cancel") { _, _ -> goToHome() }
            .show()
    }

    private fun showCreateWebsiteDialog() {
        val input = EditText(this)
        input.hint = "Website name"

        AlertDialog.Builder(this)
            .setTitle("Create Website")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().ifBlank { "MyWebsite" }
                createWebsite(name)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createWebsite(name: String) {
        val projectsDir = File(filesDir.parentFile, "home/web-projects")
        projectsDir.mkdirs()

        val projectDir = File(projectsDir, name)
        projectDir.mkdirs()

        // Create index.html
        File(projectDir, "index.html").writeText("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>$name</title>
                <link rel="stylesheet" href="style.css">
            </head>
            <body>
                <h1>Welcome to $name</h1>
                <p>Edit this page to get started.</p>
                <script src="script.js"></script>
            </body>
            </html>
        """.trimIndent())

        // Create style.css
        File(projectDir, "style.css").writeText("""
            * {
                margin: 0;
                padding: 0;
                box-sizing: border-box;
            }

            body {
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                line-height: 1.6;
                padding: 20px;
                background: #1a1a2e;
                color: #ffffff;
            }

            h1 {
                color: #e94560;
                margin-bottom: 1rem;
            }
        """.trimIndent())

        // Create script.js
        File(projectDir, "script.js").writeText("""
            // JavaScript code goes here
            console.log('$name loaded successfully!');
        """.trimIndent())

        currentProjectPath = projectDir.absolutePath
        appendToConsole("Created website: $name")
        refreshFileTree()
        openFile(File(projectDir, "index.html"))
    }

    private fun createEmptyProject() {
        val projectsDir = File(filesDir.parentFile, "home/web-projects/untitled")
        projectsDir.mkdirs()
        currentProjectPath = projectsDir.absolutePath
    }

    private fun showOpenProjectDialog() {
        val projectsDir = File(filesDir.parentFile, "home/web-projects")
        projectsDir.mkdirs()

        val projects = projectsDir.listFiles()?.filter { it.isDirectory }?.map { it.name } ?: emptyList()

        if (projects.isEmpty()) {
            showToast("No projects found")
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Open Project")
            .setItems(projects.toTypedArray()) { _, which ->
                currentProjectPath = File(projectsDir, projects[which]).absolutePath
                refreshFileTree()
            }
            .show()
    }

    private fun showNewFileDialog() {
        val input = EditText(this)
        input.hint = "filename.html"

        AlertDialog.Builder(this)
            .setTitle("New File")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val fileName = input.text.toString()
                if (fileName.isNotBlank()) {
                    createNewFile(fileName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createNewFile(fileName: String) {
        currentProjectPath?.let { path ->
            val file = File(path, fileName)
            file.createNewFile()
            refreshFileTree()
            openFile(file)
        }
    }

    private fun openFile(file: File) {
        try {
            val content = file.readText()
            codeEditor?.setText(content)
            currentFile = file
            supportActionBar?.subtitle = file.name

            // Auto-refresh preview for HTML files
            if (file.extension == "html") {
                updatePreview()
            }
        } catch (e: Exception) {
            showToast("Failed to open file")
        }
    }

    private fun updatePreview() {
        currentFile?.let { file ->
            if (file.extension == "html") {
                val html = codeEditor?.text?.toString() ?: file.readText()
                webPreview?.loadDataWithBaseURL(
                    "file://${file.parentFile?.absolutePath}/",
                    html,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        }
    }

    override fun onRunClicked() {
        // Refresh preview
        updatePreview()
        showToast("Preview updated")
    }

    override fun onSaveClicked() {
        currentFile?.let { file ->
            try {
                file.writeText(codeEditor?.text?.toString() ?: "")
                showToast("Saved: ${file.name}")
                updatePreview()
            } catch (e: Exception) {
                showToast("Failed to save")
            }
        }
    }

    override fun refreshFileTree() {
        // TODO: Implement file tree refresh
        appendToConsole("File tree refreshed")
    }
}
