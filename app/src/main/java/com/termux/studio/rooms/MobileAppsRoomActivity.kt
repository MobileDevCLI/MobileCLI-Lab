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
import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * MobileAppsRoomActivity - Android App Development Environment
 *
 * Priority #1 Room: Build Android APKs directly on the phone
 *
 * Features:
 * - Create new Android projects from templates
 * - Browse and edit project files
 * - Build APKs with Gradle
 * - View Logcat output
 * - Sign and export APKs
 */
class MobileAppsRoomActivity : BaseRoomActivity() {

    override val roomName = "Mobile Apps"
    override val roomIcon = R.drawable.ic_mobile_apps

    // Project state
    private var currentProjectPath: String? = null
    private var currentProjectName: String = "Untitled"

    // File tree
    private lateinit var fileTreeView: RecyclerView
    private lateinit var fileTreeAdapter: FileTreeAdapter

    // Build state
    private var isBuilding = false
    private var buildProcess: Process? = null

    // Current file being edited
    private var currentEditingFile: File? = null
    private var codeEditor: EditText? = null
    private var hasUnsavedChanges = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if room was opened with a specific project
        intent.getStringExtra("project_path")?.let { path ->
            openProject(path)
        }
    }

    override fun onRoomCreated() {
        setupFileTree()
        setupCodeEditor()
        setupBuildControls()
        setupBottomPanelTabs()

        // Show project selection dialog if no project
        if (currentProjectPath == null) {
            showProjectSelectionDialog()
        }
    }

    private fun setupFileTree() {
        fileTreeView = findViewById(R.id.file_tree)
        fileTreeAdapter = FileTreeAdapter { file ->
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
            showNewFileDialog()
        }
    }

    private fun setupCodeEditor() {
        // Add a basic code editor to the main content area
        val codeEditorView = LayoutInflater.from(this)
            .inflate(R.layout.view_code_editor, mainContent, false)
        mainContent.addView(codeEditorView)

        codeEditor = codeEditorView.findViewById(R.id.code_editor_text)

        // Hide placeholder when editor has content
        findViewById<TextView>(R.id.content_placeholder)?.visibility = View.GONE

        // Track unsaved changes
        codeEditor?.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && hasUnsavedChanges) {
                // Auto-save on focus lost (optional)
            }
        }
    }

    private fun setupBuildControls() {
        // Build controls will be in the toolbar actions
        // Run button triggers build and install
    }

    private fun setupBottomPanelTabs() {
        // Console tab (default)
        findViewById<TextView>(R.id.tab_console)?.setOnClickListener {
            showConsoleTab()
        }

        // Output tab (build output)
        findViewById<TextView>(R.id.tab_output)?.setOnClickListener {
            showOutputTab()
        }

        // Problems tab (compile errors)
        findViewById<TextView>(R.id.tab_problems)?.setOnClickListener {
            showProblemsTab()
        }

        // Clear console button
        findViewById<ImageButton>(R.id.btn_clear_console)?.setOnClickListener {
            clearConsole()
        }
    }

    private fun showConsoleTab() {
        updateTabSelection(R.id.tab_console)
        // Console output is already shown by default
    }

    private fun showOutputTab() {
        updateTabSelection(R.id.tab_output)
        // Switch to build output
    }

    private fun showProblemsTab() {
        updateTabSelection(R.id.tab_problems)
        // Switch to problems list
    }

    private fun updateTabSelection(selectedTabId: Int) {
        val tabs = listOf(R.id.tab_console, R.id.tab_output, R.id.tab_problems)
        tabs.forEach { tabId ->
            findViewById<TextView>(tabId)?.apply {
                if (tabId == selectedTabId) {
                    setTextColor(resources.getColor(R.color.primary, null))
                    setBackgroundColor(resources.getColor(R.color.surface, null))
                } else {
                    setTextColor(resources.getColor(R.color.text_secondary, null))
                    setBackgroundColor(resources.getColor(android.R.color.transparent, null))
                }
            }
        }
    }

    // === Project Management ===

    private fun showProjectSelectionDialog() {
        val options = arrayOf(
            "Create New Project",
            "Open Existing Project",
            "Recent Projects",
            "Import from Template"
        )

        AlertDialog.Builder(this)
            .setTitle("Mobile Apps")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showCreateProjectDialog()
                    1 -> showOpenProjectDialog()
                    2 -> showRecentProjectsDialog()
                    3 -> showTemplatesDialog()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                goToHome()
            }
            .show()
    }

    private fun showCreateProjectDialog() {
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_create_project, null)

        val nameInput = dialogView.findViewById<EditText>(R.id.input_project_name)
        val packageInput = dialogView.findViewById<EditText>(R.id.input_package_name)
        val minSdkSpinner = dialogView.findViewById<Spinner>(R.id.spinner_min_sdk)
        val templateSpinner = dialogView.findViewById<Spinner>(R.id.spinner_template)

        // Setup spinners
        val sdkVersions = arrayOf("API 24 (Android 7.0)", "API 26 (Android 8.0)", "API 28 (Android 9.0)")
        minSdkSpinner?.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sdkVersions)

        val templates = arrayOf("Empty Activity", "Basic Activity", "Bottom Navigation", "Terminal App")
        templateSpinner?.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, templates)

        AlertDialog.Builder(this)
            .setTitle("Create New Project")
            .setView(dialogView)
            .setPositiveButton("Create") { _, _ ->
                val name = nameInput?.text?.toString() ?: "MyApp"
                val packageName = packageInput?.text?.toString() ?: "com.example.myapp"
                val minSdk = when (minSdkSpinner?.selectedItemPosition ?: 0) {
                    0 -> 24
                    1 -> 26
                    2 -> 28
                    else -> 24
                }
                val template = templateSpinner?.selectedItemPosition ?: 0

                createProject(name, packageName, minSdk, template)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createProject(name: String, packageName: String, minSdk: Int, template: Int) {
        appendToConsole("Creating project: $name")
        appendToConsole("Package: $packageName")
        appendToConsole("Min SDK: $minSdk")

        val projectsDir = File(filesDir.parentFile, "home/projects")
        projectsDir.mkdirs()

        val projectDir = File(projectsDir, name)
        if (projectDir.exists()) {
            showToast("Project '$name' already exists")
            return
        }

        try {
            // Create project structure
            projectDir.mkdirs()
            File(projectDir, "app/src/main/java/${packageName.replace('.', '/')}").mkdirs()
            File(projectDir, "app/src/main/res/layout").mkdirs()
            File(projectDir, "app/src/main/res/values").mkdirs()
            File(projectDir, "app/src/main/res/drawable").mkdirs()

            // Create build.gradle.kts
            createBuildGradle(projectDir, name, packageName, minSdk)

            // Create settings.gradle.kts
            File(projectDir, "settings.gradle.kts").writeText("""
                pluginManagement {
                    repositories {
                        google()
                        mavenCentral()
                        gradlePluginPortal()
                    }
                }
                dependencyResolutionManagement {
                    repositories {
                        google()
                        mavenCentral()
                    }
                }
                rootProject.name = "$name"
                include(":app")
            """.trimIndent())

            // Create app/build.gradle.kts
            createAppBuildGradle(File(projectDir, "app"), packageName, minSdk)

            // Create AndroidManifest.xml
            createAndroidManifest(projectDir, packageName)

            // Create MainActivity based on template
            createMainActivity(projectDir, packageName, template)

            // Create resources
            createResources(projectDir, name)

            appendToConsole("Project created successfully!")
            openProject(projectDir.absolutePath)

        } catch (e: Exception) {
            appendToConsole("Error creating project: ${e.message}")
            showToast("Failed to create project")
        }
    }

    private fun createBuildGradle(projectDir: File, name: String, packageName: String, minSdk: Int) {
        File(projectDir, "build.gradle.kts").writeText("""
            plugins {
                id("com.android.application") version "8.1.0" apply false
                id("org.jetbrains.kotlin.android") version "1.9.0" apply false
            }
        """.trimIndent())
    }

    private fun createAppBuildGradle(appDir: File, packageName: String, minSdk: Int) {
        File(appDir, "build.gradle.kts").writeText("""
            plugins {
                id("com.android.application")
                id("org.jetbrains.kotlin.android")
            }

            android {
                namespace = "$packageName"
                compileSdk = 34

                defaultConfig {
                    applicationId = "$packageName"
                    minSdk = $minSdk
                    targetSdk = 28  // Required for Termux binary execution
                    versionCode = 1
                    versionName = "1.0.0"
                }

                buildTypes {
                    release {
                        isMinifyEnabled = false
                    }
                }

                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_11
                    targetCompatibility = JavaVersion.VERSION_11
                }

                kotlinOptions {
                    jvmTarget = "11"
                }
            }

            dependencies {
                implementation("androidx.core:core-ktx:1.12.0")
                implementation("androidx.appcompat:appcompat:1.6.1")
                implementation("com.google.android.material:material:1.10.0")
            }
        """.trimIndent())
    }

    private fun createAndroidManifest(projectDir: File, packageName: String) {
        val manifestDir = File(projectDir, "app/src/main")
        File(manifestDir, "AndroidManifest.xml").writeText("""
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                <application
                    android:allowBackup="true"
                    android:icon="@mipmap/ic_launcher"
                    android:label="@string/app_name"
                    android:theme="@style/Theme.AppCompat.DayNight.DarkActionBar">

                    <activity
                        android:name=".MainActivity"
                        android:exported="true">
                        <intent-filter>
                            <action android:name="android.intent.action.MAIN" />
                            <category android:name="android.intent.category.LAUNCHER" />
                        </intent-filter>
                    </activity>
                </application>
            </manifest>
        """.trimIndent())
    }

    private fun createMainActivity(projectDir: File, packageName: String, template: Int) {
        val activityDir = File(projectDir, "app/src/main/java/${packageName.replace('.', '/')}")
        activityDir.mkdirs()

        val activityCode = when (template) {
            0 -> createEmptyActivityCode(packageName)
            1 -> createBasicActivityCode(packageName)
            2 -> createBottomNavActivityCode(packageName)
            3 -> createTerminalActivityCode(packageName)
            else -> createEmptyActivityCode(packageName)
        }

        File(activityDir, "MainActivity.kt").writeText(activityCode)
    }

    private fun createEmptyActivityCode(packageName: String): String = """
        package $packageName

        import android.os.Bundle
        import androidx.appcompat.app.AppCompatActivity

        class MainActivity : AppCompatActivity() {
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                setContentView(R.layout.activity_main)
            }
        }
    """.trimIndent()

    private fun createBasicActivityCode(packageName: String): String = """
        package $packageName

        import android.os.Bundle
        import android.widget.Button
        import android.widget.TextView
        import android.widget.Toast
        import androidx.appcompat.app.AppCompatActivity

        class MainActivity : AppCompatActivity() {
            private var counter = 0

            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                setContentView(R.layout.activity_main)

                val textView = findViewById<TextView>(R.id.textView)
                val button = findViewById<Button>(R.id.button)

                button.setOnClickListener {
                    counter++
                    textView.text = "Count: ${'$'}counter"
                    Toast.makeText(this, "Clicked!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    """.trimIndent()

    private fun createBottomNavActivityCode(packageName: String): String = """
        package $packageName

        import android.os.Bundle
        import androidx.appcompat.app.AppCompatActivity
        import com.google.android.material.bottomnavigation.BottomNavigationView

        class MainActivity : AppCompatActivity() {
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                setContentView(R.layout.activity_main)

                val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
                bottomNav.setOnItemSelectedListener { item ->
                    // Handle navigation
                    true
                }
            }
        }
    """.trimIndent()

    private fun createTerminalActivityCode(packageName: String): String = """
        package $packageName

        import android.os.Bundle
        import android.widget.EditText
        import android.widget.TextView
        import androidx.appcompat.app.AppCompatActivity
        import java.io.BufferedReader
        import java.io.InputStreamReader

        class MainActivity : AppCompatActivity() {
            private lateinit var outputText: TextView
            private lateinit var inputField: EditText

            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                setContentView(R.layout.activity_main)

                outputText = findViewById(R.id.output_text)
                inputField = findViewById(R.id.input_field)

                inputField.setOnEditorActionListener { _, _, _ ->
                    executeCommand(inputField.text.toString())
                    inputField.text.clear()
                    true
                }
            }

            private fun executeCommand(command: String) {
                try {
                    val process = Runtime.getRuntime().exec(arrayOf("/system/bin/sh", "-c", command))
                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    val output = reader.readText()
                    outputText.append("$ ${'$'}command\\n${'$'}output\\n")
                    process.waitFor()
                } catch (e: Exception) {
                    outputText.append("Error: ${'$'}{e.message}\\n")
                }
            }
        }
    """.trimIndent()

    private fun createResources(projectDir: File, appName: String) {
        // strings.xml
        File(projectDir, "app/src/main/res/values/strings.xml").writeText("""
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <string name="app_name">$appName</string>
            </resources>
        """.trimIndent())

        // colors.xml
        File(projectDir, "app/src/main/res/values/colors.xml").writeText("""
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <color name="primary">#6200EE</color>
                <color name="primary_variant">#3700B3</color>
                <color name="secondary">#03DAC6</color>
            </resources>
        """.trimIndent())

        // activity_main.xml
        File(projectDir, "app/src/main/res/layout/activity_main.xml").writeText("""
            <?xml version="1.0" encoding="utf-8"?>
            <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:orientation="vertical"
                android:gravity="center"
                android:padding="16dp">

                <TextView
                    android:id="@+id/textView"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Hello World!"
                    android:textSize="24sp" />

                <Button
                    android:id="@+id/button"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="16dp"
                    android:text="Click Me" />
            </LinearLayout>
        """.trimIndent())

        // Create minimal mipmap for launcher icon
        val mipmapDir = File(projectDir, "app/src/main/res/mipmap-hdpi")
        mipmapDir.mkdirs()
        // Would normally create an icon file here
    }

    private fun showOpenProjectDialog() {
        val projectsDir = File(filesDir.parentFile, "home/projects")
        projectsDir.mkdirs()

        val projects = projectsDir.listFiles()?.filter { it.isDirectory }?.map { it.name } ?: emptyList()

        if (projects.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("No Projects")
                .setMessage("No projects found. Create a new project first.")
                .setPositiveButton("Create New") { _, _ -> showCreateProjectDialog() }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Open Project")
            .setItems(projects.toTypedArray()) { _, which ->
                val projectPath = File(projectsDir, projects[which]).absolutePath
                openProject(projectPath)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRecentProjectsDialog() {
        val prefs = getSharedPreferences("mobilecli_studio", Context.MODE_PRIVATE)
        val recentJson = prefs.getString("recent_projects", "[]")
        // TODO: Parse and show recent projects
        showToast("Recent projects coming soon")
    }

    private fun showTemplatesDialog() {
        val templates = arrayOf(
            "Empty Activity - Minimal starting point",
            "Basic Activity - Button and text",
            "Bottom Navigation - Tab-based app",
            "Terminal App - Command line interface"
        )

        AlertDialog.Builder(this)
            .setTitle("Choose Template")
            .setItems(templates) { _, which ->
                // Will fill in the template in create dialog
                showCreateProjectDialog()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openProject(projectPath: String) {
        val projectDir = File(projectPath)
        if (!projectDir.exists()) {
            showToast("Project not found")
            return
        }

        currentProjectPath = projectPath
        currentProjectName = projectDir.name

        appendToConsole("Opened project: $currentProjectName")
        appendToConsole("Path: $projectPath")

        // Update UI
        supportActionBar?.subtitle = currentProjectName

        // Refresh file tree
        refreshFileTree()

        // Save to recent projects
        saveToRecentProjects(projectPath)
    }

    private fun saveToRecentProjects(projectPath: String) {
        val prefs = getSharedPreferences("mobilecli_studio", Context.MODE_PRIVATE)
        // TODO: Add to recent projects list
    }

    override fun refreshFileTree() {
        currentProjectPath?.let { path ->
            val projectDir = File(path)
            val files = getFileTree(projectDir, 0)
            fileTreeAdapter.updateFiles(files)
        }
    }

    private fun getFileTree(dir: File, depth: Int): List<FileTreeItem> {
        val items = mutableListOf<FileTreeItem>()

        dir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))?.forEach { file ->
            items.add(FileTreeItem(file, depth))
            if (file.isDirectory && depth < 3) {  // Limit depth for performance
                items.addAll(getFileTree(file, depth + 1))
            }
        }

        return items
    }

    // === File Operations ===

    private fun onFileClicked(file: File) {
        if (file.isDirectory) {
            // Toggle folder expansion (handled by adapter)
            return
        }

        // Check for unsaved changes
        if (hasUnsavedChanges) {
            AlertDialog.Builder(this)
                .setTitle("Unsaved Changes")
                .setMessage("Save changes to ${currentEditingFile?.name}?")
                .setPositiveButton("Save") { _, _ ->
                    saveCurrentFile()
                    openFile(file)
                }
                .setNegativeButton("Discard") { _, _ ->
                    openFile(file)
                }
                .setNeutralButton("Cancel", null)
                .show()
            return
        }

        openFile(file)
    }

    private fun openFile(file: File) {
        try {
            val content = file.readText()
            codeEditor?.setText(content)
            currentEditingFile = file
            hasUnsavedChanges = false

            // Update title
            supportActionBar?.subtitle = "${currentProjectName} - ${file.name}"

            appendToConsole("Opened: ${file.name}")
        } catch (e: Exception) {
            showToast("Failed to open file: ${e.message}")
        }
    }

    private fun saveCurrentFile() {
        currentEditingFile?.let { file ->
            try {
                val content = codeEditor?.text?.toString() ?: ""
                file.writeText(content)
                hasUnsavedChanges = false
                showToast("Saved: ${file.name}")
                appendToConsole("Saved: ${file.name}")
            } catch (e: Exception) {
                showToast("Failed to save: ${e.message}")
            }
        }
    }

    private fun showNewFileDialog() {
        val input = EditText(this)
        input.hint = "filename.kt"

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
            val newFile = File(path, "app/src/main/java/$fileName")
            try {
                newFile.parentFile?.mkdirs()
                newFile.createNewFile()
                refreshFileTree()
                openFile(newFile)
            } catch (e: Exception) {
                showToast("Failed to create file: ${e.message}")
            }
        }
    }

    // === Build Operations ===

    override fun onRunClicked() {
        buildAndInstall()
    }

    override fun onSaveClicked() {
        saveCurrentFile()
    }

    private fun buildAndInstall() {
        currentProjectPath?.let { path ->
            buildProject(path, true)
        } ?: showToast("No project open")
    }

    private fun buildProject(projectPath: String, install: Boolean = false) {
        if (isBuilding) {
            showToast("Build already in progress")
            return
        }

        isBuilding = true
        appendToConsole("\n=== Building Project ===")
        appendToConsole("Project: $currentProjectName")

        Thread {
            try {
                val homeDir = File(filesDir.parentFile, "home")
                val env = arrayOf(
                    "HOME=${homeDir.absolutePath}",
                    "JAVA_HOME=/data/data/com.termux/files/usr/lib/jvm/java-17-openjdk",
                    "ANDROID_HOME=${homeDir.absolutePath}/android-sdk",
                    "PATH=/data/data/com.termux/files/usr/bin:/data/data/com.termux/files/usr/lib/jvm/java-17-openjdk/bin:${System.getenv("PATH")}"
                )

                val command = if (install) {
                    "$projectPath/gradlew assembleDebug -p $projectPath"
                } else {
                    "$projectPath/gradlew assembleDebug -p $projectPath"
                }

                buildProcess = Runtime.getRuntime().exec(
                    arrayOf("/data/data/com.termux/files/usr/bin/bash", "-c", command),
                    env,
                    File(projectPath)
                )

                // Read output
                val reader = BufferedReader(InputStreamReader(buildProcess!!.inputStream))
                val errorReader = BufferedReader(InputStreamReader(buildProcess!!.errorStream))

                // Read in separate threads
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

                val exitCode = buildProcess!!.waitFor()

                runOnUiThread {
                    if (exitCode == 0) {
                        appendToConsole("\n=== BUILD SUCCESSFUL ===")
                        showToast("Build successful!")

                        // Copy APK to downloads
                        val apkFile = File(projectPath, "app/build/outputs/apk/debug/app-debug.apk")
                        if (apkFile.exists()) {
                            val downloadDir = File("/sdcard/Download")
                            val destFile = File(downloadDir, "$currentProjectName.apk")
                            apkFile.copyTo(destFile, overwrite = true)
                            appendToConsole("APK copied to: ${destFile.absolutePath}")
                        }
                    } else {
                        appendToConsole("\n=== BUILD FAILED (exit code: $exitCode) ===")
                        showToast("Build failed!")
                    }
                    isBuilding = false
                }

            } catch (e: Exception) {
                runOnUiThread {
                    appendToConsole("Build error: ${e.message}")
                    showToast("Build error: ${e.message}")
                    isBuilding = false
                }
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        buildProcess?.destroy()
    }

    // === File Tree Adapter ===

    data class FileTreeItem(
        val file: File,
        val depth: Int,
        var isExpanded: Boolean = false
    )

    inner class FileTreeAdapter(
        private val onFileClick: (File) -> Unit
    ) : RecyclerView.Adapter<FileTreeAdapter.ViewHolder>() {

        private var files = listOf<FileTreeItem>()

        fun updateFiles(newFiles: List<FileTreeItem>) {
            files = newFiles
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_file_tree, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = files[position]
            holder.bind(item)
        }

        override fun getItemCount() = files.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val icon: ImageView = itemView.findViewById(R.id.file_icon)
            private val name: TextView = itemView.findViewById(R.id.file_name)

            fun bind(item: FileTreeItem) {
                // Indent based on depth
                itemView.setPadding(item.depth * 24, 0, 0, 0)

                name.text = item.file.name

                // Set icon based on file type
                val iconRes = when {
                    item.file.isDirectory -> R.drawable.ic_folder
                    item.file.extension == "kt" -> R.drawable.ic_file_kotlin
                    item.file.extension == "java" -> R.drawable.ic_file_java
                    item.file.extension == "xml" -> R.drawable.ic_file_xml
                    else -> R.drawable.ic_file
                }
                icon.setImageResource(iconRes)

                itemView.setOnClickListener {
                    onFileClick(item.file)
                }
            }
        }
    }
}
