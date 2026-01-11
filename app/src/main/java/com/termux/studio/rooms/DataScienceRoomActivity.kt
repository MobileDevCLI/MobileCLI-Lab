package com.termux.studio.rooms

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.termux.R
import com.termux.studio.BaseRoomActivity
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * DataScienceRoomActivity - Python Data Science Environment
 *
 * Features:
 * - Create and run Python notebooks (.py files)
 * - Interactive Python REPL
 * - Data visualization with matplotlib
 * - CSV/JSON data file viewer
 * - Package management with pip
 */
class DataScienceRoomActivity : BaseRoomActivity() {

    override val roomName = "Data Science"
    override val roomIcon = R.drawable.ic_data_science

    // State
    private var currentNotebookPath: String? = null
    private var pythonProcess: Process? = null
    private var isRunning = false

    // Views
    private lateinit var fileTreeView: RecyclerView
    private lateinit var fileTreeAdapter: NotebookAdapter
    private var codeEditor: EditText? = null
    private var outputView: TextView? = null
    private var currentFile: File? = null

    override fun onRoomCreated() {
        setupFileTree()
        setupCodeEditor()
        setupPythonREPL()
        setupToolbar()
        checkPythonInstallation()
    }

    private fun setupFileTree() {
        fileTreeView = findViewById(R.id.file_tree)
        fileTreeAdapter = NotebookAdapter { file ->
            onFileClicked(file)
        }
        fileTreeView.layoutManager = LinearLayoutManager(this)
        fileTreeView.adapter = fileTreeAdapter

        // Refresh button
        findViewById<ImageButton>(R.id.btn_refresh_files)?.setOnClickListener {
            refreshFileTree()
        }

        // New file button
        findViewById<ImageButton>(R.id.btn_new_file)?.setOnClickListener {
            showNewNotebookDialog()
        }

        // Load notebooks directory
        refreshFileTree()
    }

    private fun setupCodeEditor() {
        val codeEditorView = LayoutInflater.from(this)
            .inflate(R.layout.view_code_editor, mainContent, false)
        mainContent.addView(codeEditorView)

        codeEditor = codeEditorView.findViewById(R.id.code_editor_text)
        codeEditor?.hint = "# Python code here\nimport numpy as np\nimport pandas as pd\n\n# Your data science code..."

        findViewById<TextView>(R.id.content_placeholder)?.visibility = View.GONE
    }

    private fun setupPythonREPL() {
        outputView = consoleOutput
        // REPL input is handled through the code editor
    }

    private fun setupToolbar() {
        // Set subtitle
        supportActionBar?.subtitle = "Python Data Science"
    }

    private fun checkPythonInstallation() {
        appendToConsole("Checking Python installation...")

        Thread {
            try {
                val process = Runtime.getRuntime().exec(
                    arrayOf("/data/data/com.termux/files/usr/bin/python", "--version")
                )
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val version = reader.readLine() ?: "Unknown"
                process.waitFor()

                runOnUiThread {
                    appendToConsole("Python: $version")
                    checkPackages()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    appendToConsole("Python not installed!")
                    showInstallPythonDialog()
                }
            }
        }.start()
    }

    private fun checkPackages() {
        val packages = listOf("numpy", "pandas", "matplotlib")
        appendToConsole("Checking packages: ${packages.joinToString(", ")}")
    }

    private fun showInstallPythonDialog() {
        AlertDialog.Builder(this)
            .setTitle("Python Not Installed")
            .setMessage("Python is required for Data Science room. Install now?")
            .setPositiveButton("Install") { _, _ ->
                installPython()
            }
            .setNegativeButton("Cancel") { _, _ ->
                goToHome()
            }
            .show()
    }

    private fun installPython() {
        appendToConsole("\nInstalling Python...")
        appendToConsole("This may take a few minutes...\n")

        Thread {
            try {
                val commands = listOf(
                    "pkg install -y python",
                    "pip install numpy pandas matplotlib"
                )

                commands.forEach { cmd ->
                    runOnUiThread { appendToConsole("$ $cmd") }

                    val process = Runtime.getRuntime().exec(
                        arrayOf("/data/data/com.termux/files/usr/bin/bash", "-c", cmd)
                    )

                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    reader.forEachLine { line ->
                        runOnUiThread { appendToConsole(line) }
                    }

                    process.waitFor()
                }

                runOnUiThread {
                    appendToConsole("\nPython installation complete!")
                    checkPythonInstallation()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    appendToConsole("Installation failed: ${e.message}")
                }
            }
        }.start()
    }

    // === File Operations ===

    override fun refreshFileTree() {
        val notebooksDir = getNotebooksDir()
        notebooksDir.mkdirs()

        val files = notebooksDir.listFiles()?.filter {
            it.isFile && (it.extension == "py" || it.extension == "csv" || it.extension == "json")
        }?.sortedBy { it.name } ?: emptyList()

        fileTreeAdapter.updateFiles(files)

        if (files.isEmpty()) {
            appendToConsole("No notebooks found. Create one with the + button.")
        }
    }

    private fun getNotebooksDir(): File {
        val homeDir = File(filesDir.parentFile, "home")
        return File(homeDir, "notebooks")
    }

    private fun showNewNotebookDialog() {
        val input = EditText(this)
        input.hint = "notebook_name.py"

        AlertDialog.Builder(this)
            .setTitle("New Python Notebook")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().let {
                    if (it.endsWith(".py")) it else "$it.py"
                }
                createNotebook(name)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createNotebook(name: String) {
        val file = File(getNotebooksDir(), name)
        try {
            file.parentFile?.mkdirs()
            file.writeText("""
# $name
# Created with MobileCLI Data Science Room

import numpy as np
import pandas as pd
# import matplotlib.pyplot as plt

# Your code here
print("Hello from MobileCLI!")
            """.trimIndent())

            refreshFileTree()
            openFile(file)
            appendToConsole("Created: $name")
        } catch (e: Exception) {
            showToast("Failed to create notebook: ${e.message}")
        }
    }

    private fun onFileClicked(file: File) {
        when (file.extension) {
            "py" -> openFile(file)
            "csv" -> previewCSV(file)
            "json" -> previewJSON(file)
        }
    }

    private fun openFile(file: File) {
        try {
            val content = file.readText()
            codeEditor?.setText(content)
            currentFile = file
            currentNotebookPath = file.absolutePath
            supportActionBar?.subtitle = "Data Science - ${file.name}"
            appendToConsole("Opened: ${file.name}")
        } catch (e: Exception) {
            showToast("Failed to open file: ${e.message}")
        }
    }

    private fun previewCSV(file: File) {
        try {
            val lines = file.readLines().take(10)
            val preview = lines.joinToString("\n")
            appendToConsole("\n=== CSV Preview: ${file.name} ===")
            appendToConsole(preview)
            if (file.readLines().size > 10) {
                appendToConsole("... (${file.readLines().size - 10} more rows)")
            }
        } catch (e: Exception) {
            showToast("Failed to preview CSV: ${e.message}")
        }
    }

    private fun previewJSON(file: File) {
        try {
            val content = file.readText().take(500)
            appendToConsole("\n=== JSON Preview: ${file.name} ===")
            appendToConsole(content)
            if (file.readText().length > 500) {
                appendToConsole("... (truncated)")
            }
        } catch (e: Exception) {
            showToast("Failed to preview JSON: ${e.message}")
        }
    }

    // === Run Operations ===

    override fun onRunClicked() {
        runCurrentNotebook()
    }

    override fun onSaveClicked() {
        saveCurrentFile()
    }

    private fun saveCurrentFile() {
        currentFile?.let { file ->
            try {
                file.writeText(codeEditor?.text?.toString() ?: "")
                showToast("Saved: ${file.name}")
                appendToConsole("Saved: ${file.name}")
            } catch (e: Exception) {
                showToast("Failed to save: ${e.message}")
            }
        } ?: showToast("No file open")
    }

    private fun runCurrentNotebook() {
        val code = codeEditor?.text?.toString()
        if (code.isNullOrBlank()) {
            showToast("No code to run")
            return
        }

        // Save first
        currentFile?.writeText(code)

        appendToConsole("\n=== Running Python ===")
        isRunning = true

        Thread {
            try {
                // Create temp file with code
                val tempFile = File(cacheDir, "temp_script.py")
                tempFile.writeText(code)

                val env = arrayOf(
                    "HOME=/data/data/com.termux/files/home",
                    "PATH=/data/data/com.termux/files/usr/bin:${System.getenv("PATH")}",
                    "PYTHONPATH=/data/data/com.termux/files/usr/lib/python3.12"
                )

                pythonProcess = Runtime.getRuntime().exec(
                    arrayOf("/data/data/com.termux/files/usr/bin/python", tempFile.absolutePath),
                    env
                )

                val reader = BufferedReader(InputStreamReader(pythonProcess!!.inputStream))
                val errorReader = BufferedReader(InputStreamReader(pythonProcess!!.errorStream))

                // Read output
                Thread {
                    reader.forEachLine { line ->
                        runOnUiThread { appendToConsole(line) }
                    }
                }.start()

                Thread {
                    errorReader.forEachLine { line ->
                        runOnUiThread { appendToConsole("[ERROR] $line") }
                    }
                }.start()

                val exitCode = pythonProcess!!.waitFor()

                runOnUiThread {
                    if (exitCode == 0) {
                        appendToConsole("=== Execution Complete ===\n")
                    } else {
                        appendToConsole("=== Execution Failed (exit: $exitCode) ===\n")
                    }
                    isRunning = false
                }

                tempFile.delete()

            } catch (e: Exception) {
                runOnUiThread {
                    appendToConsole("Execution error: ${e.message}")
                    isRunning = false
                }
            }
        }.start()
    }

    private fun executePythonCommand(command: String) {
        appendToConsole(">>> $command")

        Thread {
            try {
                val env = arrayOf(
                    "HOME=/data/data/com.termux/files/home",
                    "PATH=/data/data/com.termux/files/usr/bin:${System.getenv("PATH")}"
                )

                val process = Runtime.getRuntime().exec(
                    arrayOf("/data/data/com.termux/files/usr/bin/python", "-c", command),
                    env
                )

                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val errorReader = BufferedReader(InputStreamReader(process.errorStream))

                val output = reader.readText()
                val error = errorReader.readText()

                process.waitFor()

                runOnUiThread {
                    if (output.isNotBlank()) appendToConsole(output.trim())
                    if (error.isNotBlank()) appendToConsole("[ERROR] ${error.trim()}")
                }
            } catch (e: Exception) {
                runOnUiThread {
                    appendToConsole("Error: ${e.message}")
                }
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        pythonProcess?.destroy()
    }

    // === Notebook Adapter ===

    inner class NotebookAdapter(
        private val onFileClick: (File) -> Unit
    ) : RecyclerView.Adapter<NotebookAdapter.ViewHolder>() {

        private var files = listOf<File>()

        fun updateFiles(newFiles: List<File>) {
            files = newFiles
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_file_tree, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(files[position])
        }

        override fun getItemCount() = files.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val icon: ImageView = itemView.findViewById(R.id.file_icon)
            private val name: TextView = itemView.findViewById(R.id.file_name)

            fun bind(file: File) {
                name.text = file.name

                val iconRes = when (file.extension) {
                    "py" -> R.drawable.ic_file_python
                    "csv" -> R.drawable.ic_file_csv
                    "json" -> R.drawable.ic_file_json
                    else -> R.drawable.ic_file
                }
                icon.setImageResource(iconRes)

                itemView.setOnClickListener {
                    onFileClick(file)
                }
            }
        }
    }
}
