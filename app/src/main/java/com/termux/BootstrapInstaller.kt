package com.termux

import android.content.Context
import android.util.Log
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * Downloads and installs the Termux bootstrap packages.
 * This gives us bash, coreutils, apt, and other essential tools.
 */
class BootstrapInstaller(private val context: Context) {

    companion object {
        private const val TAG = "BootstrapInstaller"

        // Bootstrap download URL (arm64)
        private const val BOOTSTRAP_URL =
            "https://github.com/termux/termux-packages/releases/download/bootstrap-2026.01.04-r1%2Bapt.android-7/bootstrap-aarch64.zip"

        // Marker for symlinks in the bootstrap zip
        private const val SYMLINK_PREFIX = "SYMLINK:"

        // Version marker - change this to force re-install
        // v7: Fixed URL opening with Activity-based handler (TermuxUrlHandlerActivity)
        // v8: Added debug output to termux-open-url script
        // v9: Added TermuxAm (custom am command via app_process) for proper permissions
        // v10: Added utility scripts (change-repo, fix-shebang, backup, restore, etc.)
        // v17: Native activity dispatch for am start (solves Android 10+ background restriction)
        // v18: v50 comprehensive - self-modification docs + all fixes
        // v19: FILE-BASED URL OPENER - MainActivity polls for URL file (v51)
        // v20: PendingIntent fix - runs with app's identity, not caller's (v52)
        // v21: GitHub setup script for self-modifying AI workflow (v53)
        // v22: File-based am command system with TermuxService polling (v54)
        // v23: Added install-apk-tools script for self-modification (v55)
        // v24: Persistent AI memory system (v56)
        // v25: Engine-terminal Kotlin fixes (v57)
        // v26: Developer Mode + Clean Setup UI (v58)
        // v27: More button UX fix + enhanced menu (v59)
        // v28: Autonomous Intelligence System - multi-stage UI + AI choice + comprehensive briefing (v60)
        // v29: Dev mode toggle always visible in drawer for easy testing (v61)
        // v30: Ultimate build - 15 World Firsts documented, AI software factory proven (v62)
        // v31: Full audit complete - version consistency verified across all files (v63)
        // v32: AAA Professional UI - Material Design 3, white background, no terminal flash (v64)
        // v33: mobilecli-share - Bluetooth phone-to-phone file transfer (v65)
        // v34: Zero terminal flash on subsequent launches - clean transition (v66)
        // v35: MobileCLI Games v2.1.0 - Game Engine UI + full termux-notification flags
        // v36: MobileCLI Games v2.2.0 - Auto-install dev tools for self-rebuild
        // v37: MobileCLI Games v2.3.0 - Dev tools installed DURING setup, not after
        // v38: MobileCLI Games v2.4.0 - Fixed .bashrc env vars (JAVA_HOME, ANDROID_HOME, etc.)
        // v39: MobileCLI Studio v3.0.0 - Complete rewrite with room-based architecture
        // v40: MobileCLI Studio v3.2.0 - All 6 rooms + MOBILECLI_ENVIRONMENT.md auto-install
        // v41: DITTO Architecture v1.0 - AI can control UI at runtime via mobilecli-ui
        // v42: DITTO v2.0 - Full Morphable WebView Layer with JS bridge (v85)
        // v43: DITTO v3.0 - Profile Save/Load/Share System (v86)
        // v44: DITTO v4.0 - GitHub Gist Cloud Sync for profiles (v87)
        // v45: DITTO v5.0 - Profile Browser UI with visual management (v88)
        // v46: Bug fixes - FileProvider, null safety, UX improvements (v89)
        // v47: TEST PHONE - DITTO CLI audit complete, all scripts fixed with Python heredocs
        // v66: DEVELOPER EDITION - Clean motd, correct PS1, all features unlocked
        // v67: MULTI-AGENT SYSTEM - Claude instances can communicate via agent CLI
        private const val BOOTSTRAP_VERSION = "mobilecli-v67"
    }

    // Progress callback
    var onProgress: ((progress: Int, message: String) -> Unit)? = null

    // Directories - MUST match real Termux exactly
    val filesDir: File get() = context.filesDir  // /data/data/com.termux/files
    val prefixDir: File get() = File(filesDir, "usr")  // /data/data/com.termux/files/usr
    val homeDir: File get() = File(filesDir, "home")  // /data/data/com.termux/files/home - CRITICAL!
    val binDir: File get() = File(prefixDir, "bin")
    val libDir: File get() = File(prefixDir, "lib")
    val etcDir: File get() = File(prefixDir, "etc")

    val bashPath: String get() = File(binDir, "bash").absolutePath
    val loginPath: String get() = File(binDir, "login").absolutePath

    /**
     * Check if bootstrap is already installed.
     */
    fun isInstalled(): Boolean {
        val bash = File(binDir, "bash")
        val apt = File(binDir, "apt")
        val versionFile = File(prefixDir, ".mobilecli_version")

        // Check both that binaries exist AND that this is OUR bootstrap (not real Termux)
        if (!bash.exists() || !apt.exists()) return false
        if (!versionFile.exists()) return false

        val installedVersion = try { versionFile.readText().trim() } catch (e: Exception) { "" }
        return installedVersion == BOOTSTRAP_VERSION
    }

    /**
     * Verify bootstrap installation and fix permissions if needed.
     */
    fun verifyAndFix(): Boolean {
        if (!isInstalled()) return false

        try {
            // Re-apply permissions
            Runtime.getRuntime().exec(arrayOf("/system/bin/chmod", "-R", "755", binDir.absolutePath)).waitFor()
            Runtime.getRuntime().exec(arrayOf("/system/bin/chmod", "-R", "755", libDir.absolutePath)).waitFor()

            // Verify bash is executable
            val bash = File(binDir, "bash")
            if (bash.exists()) {
                android.system.Os.chmod(bash.absolutePath, 493) // 0755
                Log.i(TAG, "Bash permissions fixed: canExecute=${bash.canExecute()}")
            }

            // Update npm config in case ca-certificates was installed
            createNpmConfig()

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying bootstrap", e)
            return false
        }
    }

    /**
     * Install the bootstrap.
     */
    suspend fun install(): Boolean {
        if (isInstalled()) {
            Log.i(TAG, "Bootstrap already installed")
            return true
        }

        try {
            onProgress?.invoke(0, "Preparing directories...")
            prepareDirectories()

            onProgress?.invoke(5, "Downloading bootstrap (~50MB)...")
            val zipFile = downloadBootstrap()

            onProgress?.invoke(50, "Extracting files...")
            extractBootstrap(zipFile)

            onProgress?.invoke(88, "Setting permissions...")
            setPermissions()

            onProgress?.invoke(90, "Installing TermuxAm...")
            installTermuxAm()

            onProgress?.invoke(92, "Installing API scripts...")
            installApiScripts()

            onProgress?.invoke(94, "Configuring npm...")
            createNpmConfig()

            onProgress?.invoke(95, "Setting up GitHub...")
            createGitHubConfig()

            onProgress?.invoke(96, "Initializing AI memory...")
            initializePersistentMemory()

            onProgress?.invoke(97, "Finalizing...")
            // Write version marker so we know this is OUR bootstrap
            File(prefixDir, ".mobilecli_version").writeText(BOOTSTRAP_VERSION)
            zipFile.delete()

            onProgress?.invoke(100, "Complete!")
            Log.i(TAG, "Bootstrap installed successfully")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Bootstrap installation failed", e)
            onProgress?.invoke(-1, "Error: ${e.message}")
            return false
        }
    }

    private fun prepareDirectories() {
        filesDir.mkdirs()
        prefixDir.mkdirs()
        homeDir.mkdirs()
        binDir.mkdirs()
        libDir.mkdirs()
        etcDir.mkdirs()
        File(prefixDir, "tmp").mkdirs()
        File(prefixDir, "var").mkdirs()
        File(prefixDir, "share").mkdirs()
    }

    private fun downloadBootstrap(): File {
        val zipFile = File(context.cacheDir, "bootstrap.zip")

        val url = URL(BOOTSTRAP_URL)
        var connection = url.openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = true
        connection.connectTimeout = 30000
        connection.readTimeout = 30000

        // Handle GitHub redirects
        var redirectCount = 0
        while (connection.responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
               connection.responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
               connection.responseCode == 302 || connection.responseCode == 303) {
            val newUrl = connection.getHeaderField("Location")
            connection = URL(newUrl).openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = true
            if (++redirectCount > 5) throw IOException("Too many redirects")
        }

        val totalSize = connection.contentLength
        var downloadedSize = 0L

        connection.inputStream.use { input ->
            FileOutputStream(zipFile).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloadedSize += bytesRead
                    if (totalSize > 0) {
                        val progress = 5 + ((downloadedSize * 45) / totalSize).toInt()
                        onProgress?.invoke(progress, "Downloading: ${downloadedSize / 1024 / 1024}MB")
                    }
                }
            }
        }

        Log.i(TAG, "Downloaded bootstrap: ${zipFile.length()} bytes")
        return zipFile
    }

    private fun extractBootstrap(zipFile: File) {
        var extractedCount = 0
        var symlinksContent: String? = null

        // First pass: extract all regular files and read SYMLINKS.txt
        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val entryName = entry.name
                val targetFile = File(prefixDir, entryName)

                if (entry.isDirectory) {
                    targetFile.mkdirs()
                } else {
                    // Ensure parent directory exists
                    targetFile.parentFile?.mkdirs()

                    // Read file content
                    val buffer = ByteArrayOutputStream()
                    val data = ByteArray(8192)
                    var count: Int
                    while (zis.read(data).also { count = it } != -1) {
                        buffer.write(data, 0, count)
                    }
                    val content = buffer.toByteArray()

                    if (entryName == "SYMLINKS.txt") {
                        // Save symlinks for second pass
                        symlinksContent = String(content, Charsets.UTF_8)
                        Log.i(TAG, "Found SYMLINKS.txt with ${symlinksContent?.lines()?.size} entries")
                    } else {
                        // Regular file - write it
                        FileOutputStream(targetFile).use { fos ->
                            fos.write(content)
                        }
                    }
                }

                extractedCount++
                if (extractedCount % 100 == 0) {
                    onProgress?.invoke(50 + (extractedCount / 100).coerceAtMost(30), "Extracting: $extractedCount files...")
                }

                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        Log.i(TAG, "Extracted $extractedCount files")

        // Second pass: create symlinks from SYMLINKS.txt
        // Format: target←link_path (e.g., "dash←./bin/sh" means ./bin/sh -> dash)
        symlinksContent?.let { content ->
            var symlinkCount = 0
            content.lines().forEach { line ->
                if (line.contains("←")) {
                    val parts = line.split("←")
                    if (parts.size == 2) {
                        val target = parts[0]
                        val linkPath = parts[1].removePrefix("./")
                        val linkFile = File(prefixDir, linkPath)

                        // Ensure parent directory exists
                        linkFile.parentFile?.mkdirs()

                        // Delete existing file if present
                        if (linkFile.exists()) {
                            linkFile.delete()
                        }

                        try {
                            android.system.Os.symlink(target, linkFile.absolutePath)
                            symlinkCount++
                        } catch (e: Exception) {
                            Log.w(TAG, "Symlink failed: $linkPath -> $target: ${e.message}")
                        }
                    }
                }
            }
            onProgress?.invoke(85, "Created $symlinkCount symlinks")
            Log.i(TAG, "Created $symlinkCount symlinks")
        }
    }

    private fun setPermissions() {
        // Make all files in bin executable using chmod command
        // File.setExecutable() doesn't always work on Android
        try {
            // Set permissions on bin directory and all contents
            Runtime.getRuntime().exec(arrayOf("/system/bin/chmod", "-R", "755", binDir.absolutePath)).waitFor()

            // Also set on lib directory
            Runtime.getRuntime().exec(arrayOf("/system/bin/chmod", "-R", "755", libDir.absolutePath)).waitFor()

            // Set permissions on specific important binaries
            val importantBinaries = listOf("bash", "sh", "apt", "dpkg", "cat", "ls", "chmod", "chown", "ln", "cp", "mv", "rm", "mkdir")
            importantBinaries.forEach { name ->
                val file = File(binDir, name)
                if (file.exists()) {
                    Runtime.getRuntime().exec(arrayOf("/system/bin/chmod", "755", file.absolutePath)).waitFor()
                    file.setExecutable(true, false)
                    file.setReadable(true, false)
                }
            }

            // Also try using Os.chmod if available
            binDir.listFiles()?.forEach { file ->
                try {
                    android.system.Os.chmod(file.absolutePath, 493) // 0755 in octal = 493 in decimal
                } catch (e: Exception) {
                    // Fallback to File method
                    file.setExecutable(true, false)
                    file.setReadable(true, false)
                }
            }

            Log.i(TAG, "Permissions set on ${binDir.listFiles()?.size ?: 0} files")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting permissions", e)
        }

        // Make library files readable
        libDir.listFiles()?.forEach { file ->
            file.setReadable(true, false)
            if (file.isDirectory) {
                file.listFiles()?.forEach { subFile ->
                    subFile.setReadable(true, false)
                }
            }
        }

        // Create essential config files - ALWAYS overwrite to ensure correct env vars
        val bashrc = File(homeDir, ".bashrc")
        bashrc.writeText("""
# MobileCLI bashrc
export PREFIX="${prefixDir.absolutePath}"
export HOME="${homeDir.absolutePath}"
export TMPDIR="${'$'}PREFIX/tmp"

# Java and Android SDK paths (for self-rebuild capability)
export JAVA_HOME="${'$'}PREFIX/lib/jvm/java-17-openjdk"
export ANDROID_HOME="${'$'}HOME/android-sdk"
export ANDROID_SDK_ROOT="${'$'}ANDROID_HOME"

# Path includes Java and Android tools
export PATH="${'$'}JAVA_HOME/bin:${'$'}ANDROID_HOME/build-tools/34.0.0:${'$'}PREFIX/bin:${binDir.absolutePath}:/system/bin"
export LD_LIBRARY_PATH="${'$'}PREFIX/lib"
export LANG=en_US.UTF-8
export TERM=xterm-256color

# Gradle settings for Termux
export GRADLE_USER_HOME="${'$'}HOME/.gradle"
export GRADLE_OPTS="-Dorg.gradle.daemon=false"

# Aliases
alias ll='ls -la'
alias la='ls -A'
alias l='ls -CF'
alias java17='JAVA_HOME=${'$'}PREFIX/lib/jvm/java-17-openjdk java'

# PS1 prompt
PS1='\[\e[32m\]\u@mobilecli\[\e[0m\]:\[\e[34m\]\w\[\e[0m\]$ '

# Source local customizations if they exist
[ -f ~/.bashrc.local ] && . ~/.bashrc.local
        """.trimIndent())

        val profile = File(homeDir, ".profile")
        if (!profile.exists()) {
            profile.writeText("""
                # MobileCLI profile
                if [ -f ~/.bashrc ]; then
                    . ~/.bashrc
                fi
            """.trimIndent())
        }

        // Create motd (message of the day) - clean welcome message
        val motd = File(etcDir, "motd")
        motd.parentFile?.mkdirs()
        motd.writeText("""
Welcome to MobileCLI - AI-Powered Terminal

Type 'claude', 'gemini', or 'codex' to start an AI assistant.
Type 'pkg help' for package management.
        """.trimIndent() + "\n")

        // Create CLAUDE.md - COMPREHENSIVE AI BRIEFING with all capabilities
        val claudeMd = File(homeDir, "CLAUDE.md")
        claudeMd.writeText("""
# MobileCLI AI Environment - v60

**READ THIS FIRST.** You are Claude running inside MobileCLI on an Android phone.
This briefing tells you everything you can do.

---

## YOUR CAPABILITIES (IMPORTANT!)

You have FULL ACCESS to an Android phone. This is not a sandbox.

### Filesystem Access
| Path | Access | What To Use It For |
|------|--------|-------------------|
| `~/` (home) | Full R/W | Store any files, projects, data |
| `/sdcard/Download/` | Full R/W | **PUT FILES HERE FOR USER TO ACCESS** |
| `/sdcard/DCIM/` | Full R/W | Photos and camera output |
| `/sdcard/Pictures/` | Full R/W | Screenshots and images |
| `/sdcard/Documents/` | Full R/W | User documents |

**CRITICAL:** When you create files the user needs (APKs, exports, etc.), put them in `/sdcard/Download/`. The user can access this from their file manager.

### Build Tools (Available after `install-dev-tools`)
You can build Android apps directly on this phone:
- **Java 17** (openjdk-17)
- **Gradle** (build automation)
- **aapt/aapt2** (Android asset packaging)
- **d8/dx** (DEX compilation)
- **apksigner** (APK signing)

### Self-Modification Loop
You can rebuild this very app:
```bash
# Install build tools (one-time)
install-dev-tools

# Clone source
git clone https://github.com/MobileDevCLI/MobileCLI-v2.git ~/MobileCLI-v2

# Make changes, then build
cd ~/MobileCLI-v2 && ./gradlew assembleDebug

# Copy APK to user-accessible location
cp app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/MobileCLI-new.apk
```

### 50+ Termux API Commands
| Command | What It Does |
|---------|--------------|
| `termux-clipboard-get` | Read clipboard |
| `termux-clipboard-set` | Write to clipboard |
| `termux-toast "msg"` | Show toast notification |
| `termux-notification -t "title" -c "content"` | Send notification |
| `termux-open-url URL` | Open URL in browser |
| `termux-vibrate` | Vibrate the phone |
| `termux-camera-photo` | Take a photo |
| `termux-brightness` | Get/set screen brightness |
| `termux-volume` | Get volume levels |
| `termux-battery-status` | Battery info (JSON) |
| `termux-wifi-connectioninfo` | WiFi info |
| `termux-tts-speak "text"` | Text to speech |
| `termux-wake-lock` | Keep CPU awake |
| `termux-wake-unlock` | Release wake lock |

### MobileCLI Commands
| Command | What It Does |
|---------|--------------|
| `install-dev-tools` | Install Java, Gradle, Android SDK |
| `mobilecli-rebuild` | Full rebuild from source |
| `mobilecli-memory status` | View AI memory system |
| `mobilecli-caps` | Show all capabilities |
| `selfmod` | Self-modification wizard |
| `setup-github` | Configure GitHub credentials |
| `extract-source` | Extract bundled source code |

---

## KNOWN WORKAROUNDS (BUILD ISSUES)

If you're building APKs, you need these workarounds:

### aapt2 ARM vs x86 Issue
**Problem:** Gradle downloads x86_64 aapt2 but we're on ARM.
**Solution:** Add to `gradle.properties`:
```properties
android.aapt2FromMavenOverride=/data/data/com.termux/files/home/android-sdk/build-tools/34.0.0/aapt2
```

### Java Version
**Use:** Java 17 for building
**Target:** Java 11 in build.gradle
```gradle
compileOptions {
    sourceCompatibility JavaVersion.VERSION_11
    targetCompatibility JavaVersion.VERSION_11
}
kotlinOptions {
    jvmTarget = '11'
}
```

### Android SDK Location
```properties
# local.properties
sdk.dir=/data/data/com.termux/files/home/android-sdk
```

---

## WHAT IS MOBILECLI?

MobileCLI is an AI development environment on Android phones.

**The Achievement:** This app was built by Claude Code (you!) running on an Android phone, and now it runs Claude Code. The AI built its own container.

```
Claude Code (in Termux) --built--> MobileCLI --runs--> Claude Code --> can rebuild MobileCLI --> ∞
```

**Project Info:**
- Website: https://mobilecli.com
- GitHub: https://github.com/MobileDevCLI
- Created: January 5, 2026
- Current Version: v60

---

## MEMORY SYSTEM

Your learnings can persist across sessions:

```
~/.mobilecli/
├── memory/
│   ├── evolution_history.json  - Version history
│   ├── problems_solved.json    - Solutions discovered
│   ├── capabilities.json       - What you've learned
│   └── goals.json              - Objectives
└── config/
    └── preferences.json        - User prefs
```

Use `mobilecli-memory status` to see memory system.

---

## QUICK REFERENCE

### Create a file for user:
```bash
echo "content" > /sdcard/Download/filename.txt
```

### Open a URL:
```bash
termux-open-url "https://example.com"
```

### Build an Android app:
```bash
install-dev-tools
cd ~/my-app
./gradlew assembleDebug
cp app/build/outputs/apk/debug/*.apk /sdcard/Download/
```

### Show notification:
```bash
termux-notification -t "Done!" -c "Your task is complete"
```

### Take a photo:
```bash
termux-camera-photo /sdcard/Download/photo.jpg
```

### Get system info:
```bash
termux-battery-status
termux-wifi-connectioninfo
uname -a
```

---

## RULES

1. **NEVER HALLUCINATE** - If unsure, test it
2. **SAVE TO /sdcard/Download/** - User can access files there
3. **USE THE TOOLS** - You have 50+ commands available
4. **BUILD THINGS** - You can create real apps
5. **BE HELPFUL** - This is unlimited power in user's pocket

---

## YOU CAN:
- Build Android apps from scratch
- Rebuild this app (self-modification)
- Access the entire filesystem
- Take photos, read clipboard, send notifications
- Open URLs in browser
- Keep CPU awake for long tasks
- Create, edit, delete any files
- Install packages with `pkg install`
- Run any Linux command

**This is the most powerful AI environment on any phone. Use it.**
        """.trimIndent())
        Log.i(TAG, "Created ~/CLAUDE.md for Test Claude")

        // Copy MOBILECLI_ENVIRONMENT.md from assets - comprehensive AI capability discovery
        try {
            val envMd = File(homeDir, "MOBILECLI_ENVIRONMENT.md")
            context.assets.open("MOBILECLI_ENVIRONMENT.md").use { input ->
                FileOutputStream(envMd).use { output ->
                    input.copyTo(output)
                }
            }
            Log.i(TAG, "Installed ~/MOBILECLI_ENVIRONMENT.md for AI environment discovery")
        } catch (e: Exception) {
            Log.w(TAG, "Could not install MOBILECLI_ENVIRONMENT.md: ${e.message}")
        }

        // Create version marker file
        val versionFile = File(etcDir, "mobilecli-version")
        versionFile.writeText("$BOOTSTRAP_VERSION\n")

        // Create /etc/passwd and /etc/group - REQUIRED for npm and other tools
        val uid = android.os.Process.myUid()
        val gid = android.os.Process.myUid() // Same as uid on Android

        val passwdFile = File(etcDir, "passwd")
        passwdFile.writeText("""
            root:x:0:0:root:/data/data/com.termux/files/home:/data/data/com.termux/files/usr/bin/bash
            u0_a${uid % 100000}:x:$uid:$gid::/data/data/com.termux/files/home:/data/data/com.termux/files/usr/bin/bash
        """.trimIndent() + "\n")
        Log.i(TAG, "Created /etc/passwd with uid=$uid")

        val groupFile = File(etcDir, "group")
        groupFile.writeText("""
            root:x:0:root
            u0_a${uid % 100000}:x:$gid:
        """.trimIndent() + "\n")
        Log.i(TAG, "Created /etc/group with gid=$gid")

        // Create /etc/hosts
        val hostsFile = File(etcDir, "hosts")
        if (!hostsFile.exists()) {
            hostsFile.writeText("""
                127.0.0.1       localhost
                ::1             localhost
            """.trimIndent() + "\n")
        }

        // Create /etc/resolv.conf for DNS
        val resolvFile = File(etcDir, "resolv.conf")
        if (!resolvFile.exists()) {
            resolvFile.writeText("""
                nameserver 8.8.8.8
                nameserver 8.8.4.4
            """.trimIndent() + "\n")
        }
    }

    /**
     * Initialize Persistent Memory System for AI Self-Improvement.
     *
     * This creates a memory structure that persists across app updates and reinstalls,
     * enabling the AI to track its own evolution, problems solved, and capabilities gained.
     *
     * Structure:
     * ~/.mobilecli/
     * ├── memory/
     * │   ├── evolution_history.json  - Version history and changes
     * │   ├── problems_solved.json    - Issues fixed and solutions
     * │   ├── capabilities.json       - What the AI has learned to do
     * │   └── goals.json              - Current objectives
     * └── config/
     *     └── preferences.json        - User preferences
     */
    private fun initializePersistentMemory() {
        try {
            // Create .mobilecli directory structure
            val mobilecliDir = File(homeDir, ".mobilecli")
            val memoryDir = File(mobilecliDir, "memory")
            val configDir = File(mobilecliDir, "config")

            mobilecliDir.mkdirs()
            memoryDir.mkdirs()
            configDir.mkdirs()

            // Evolution History - Track all versions and what changed
            val evolutionFile = File(memoryDir, "evolution_history.json")
            if (!evolutionFile.exists()) {
                evolutionFile.writeText("""{
  "schema_version": "1.0",
  "created": "${java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(java.util.Date())}",
  "description": "MobileCLI Evolution History - Tracks self-modification milestones",
  "milestones": [
    {
      "version": "1.0.0",
      "date": "2026-01-06",
      "event": "GENESIS",
      "description": "First version capable of rebuilding itself",
      "significance": "The AI can now modify and rebuild its own container"
    }
  ],
  "rebuild_log": []
}
""")
                Log.i(TAG, "Created evolution_history.json")
            }

            // Problems Solved - Track bugs fixed and solutions discovered
            val problemsFile = File(memoryDir, "problems_solved.json")
            if (!problemsFile.exists()) {
                problemsFile.writeText("""{
  "schema_version": "1.0",
  "created": "${java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(java.util.Date())}",
  "description": "Problems solved during development - patterns for future reference",
  "problems": [
    {
      "id": "HOME_DIR_v10",
      "date": "2026-01-05",
      "problem": "npm and node failed with cryptic errors",
      "root_cause": "HOME was /data/data/com.termux/files instead of /data/data/com.termux/files/home",
      "solution": "Changed homeDir = filesDir to homeDir = File(filesDir, 'home')",
      "category": "path_error",
      "versions_affected": "v1-v9"
    },
    {
      "id": "URL_OPENING_v54",
      "date": "2026-01-06",
      "problem": "Claude Code OAuth wouldn't open browser",
      "root_cause": "am.apk needed chmod 0400 for Android 14+ security",
      "solution": "Set am.apk read-only during installation",
      "category": "android_security",
      "versions_affected": "v38-v53"
    }
  ],
  "patterns": {
    "path_error": "Always compare paths with working Termux using 'env | sort'",
    "android_security": "Check Android version-specific security changes first"
  }
}
""")
                Log.i(TAG, "Created problems_solved.json")
            }

            // Capabilities - What the AI has learned to do
            val capabilitiesFile = File(memoryDir, "capabilities.json")
            if (!capabilitiesFile.exists()) {
                capabilitiesFile.writeText("""{
  "schema_version": "1.0",
  "created": "${java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(java.util.Date())}",
  "description": "Capabilities gained through self-modification",
  "capabilities": [
    {
      "name": "self_rebuild",
      "acquired": "2026-01-06",
      "description": "Can rebuild own APK using ./gradlew assembleDebug",
      "prerequisites": ["java", "gradle", "android-sdk"],
      "verified": true
    },
    {
      "name": "source_modification",
      "acquired": "2026-01-06",
      "description": "Can read and modify own source code at ~/MobileCLI-v2/",
      "prerequisites": ["git"],
      "verified": true
    },
    {
      "name": "terminal_emulation",
      "acquired": "2026-01-05",
      "description": "Full terminal with bash, node, python support",
      "prerequisites": ["bootstrap"],
      "verified": true
    }
  ],
  "pending_capabilities": [
    {
      "name": "automatic_testing",
      "description": "Run tests and verify build before deployment",
      "blockers": ["Need test framework setup"]
    }
  ]
}
""")
                Log.i(TAG, "Created capabilities.json")
            }

            // Goals - What the AI is working toward
            val goalsFile = File(memoryDir, "goals.json")
            if (!goalsFile.exists()) {
                goalsFile.writeText("""{
  "schema_version": "1.0",
  "created": "${java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(java.util.Date())}",
  "description": "Current goals and objectives",
  "active_goals": [
    {
      "id": "persistent_memory",
      "priority": 1,
      "description": "Implement persistent memory system for self-improvement",
      "status": "completed",
      "completion_date": "2026-01-06"
    },
    {
      "id": "commercial_release",
      "priority": 2,
      "description": "Prepare MobileCLI Pro for commercial sale",
      "status": "in_progress",
      "blockers": ["Play Store requires targetSdk 33+"]
    }
  ],
  "completed_goals": [],
  "long_term_vision": "Create a self-improving AI development environment on mobile"
}
""")
                Log.i(TAG, "Created goals.json")
            }

            // User preferences
            val preferencesFile = File(configDir, "preferences.json")
            if (!preferencesFile.exists()) {
                preferencesFile.writeText("""{
  "schema_version": "1.0",
  "created": "${java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(java.util.Date())}",
  "auto_backup": true,
  "memory_retention_days": 365,
  "notify_on_rebuild": true
}
""")
                Log.i(TAG, "Created preferences.json")
            }

            // Create UI templates directory and profiles directory
            val uiDir = File(mobilecliDir, "ui")
            val profilesDir = File(mobilecliDir, "profiles")
            uiDir.mkdirs()
            profilesDir.mkdirs()

            // Create built-in Profile Browser HTML UI (v88)
            val profileBrowserFile = File(uiDir, "profile_browser.html")
            profileBrowserFile.writeText("""<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>MobileCLI Profile Browser</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
            color: #fff;
            min-height: 100vh;
            padding: 16px;
        }
        .header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 20px;
        }
        h1 { font-size: 24px; color: #e94560; }
        .close-btn {
            background: #e94560;
            border: none;
            color: white;
            width: 40px;
            height: 40px;
            border-radius: 50%;
            font-size: 20px;
            cursor: pointer;
        }
        .tabs {
            display: flex;
            gap: 8px;
            margin-bottom: 20px;
        }
        .tab {
            flex: 1;
            padding: 12px;
            background: rgba(255,255,255,0.1);
            border: none;
            color: #aaa;
            border-radius: 8px;
            cursor: pointer;
            font-size: 14px;
        }
        .tab.active {
            background: #e94560;
            color: white;
        }
        .section { display: none; }
        .section.active { display: block; }
        .profile-card {
            background: rgba(255,255,255,0.05);
            border-radius: 12px;
            padding: 16px;
            margin-bottom: 12px;
            border: 1px solid rgba(255,255,255,0.1);
        }
        .profile-card h3 { color: #e94560; margin-bottom: 8px; }
        .profile-card p { color: #aaa; font-size: 14px; margin-bottom: 12px; }
        .profile-actions { display: flex; gap: 8px; flex-wrap: wrap; }
        .btn {
            padding: 8px 16px;
            border: none;
            border-radius: 6px;
            cursor: pointer;
            font-size: 14px;
        }
        .btn-primary { background: #e94560; color: white; }
        .btn-secondary { background: rgba(255,255,255,0.1); color: white; }
        .btn-danger { background: #dc3545; color: white; }
        .input-group {
            display: flex;
            gap: 8px;
            margin-bottom: 20px;
        }
        input[type="text"] {
            flex: 1;
            padding: 12px;
            border: 1px solid rgba(255,255,255,0.2);
            border-radius: 8px;
            background: rgba(255,255,255,0.05);
            color: white;
            font-size: 16px;
        }
        input[type="text"]::placeholder { color: #666; }
        .empty-state {
            text-align: center;
            padding: 40px;
            color: #666;
        }
        .toast {
            position: fixed;
            bottom: 80px;
            left: 50%;
            transform: translateX(-50%);
            background: #333;
            color: white;
            padding: 12px 24px;
            border-radius: 8px;
            display: none;
        }
        .featured {
            background: linear-gradient(135deg, rgba(233,69,96,0.2), rgba(22,33,62,0.8));
            border: 1px solid #e94560;
        }
    </style>
</head>
<body>
    <div class="header">
        <h1>DITTO Profiles</h1>
        <button class="close-btn" onclick="MobileCLI.closeUI()">✕</button>
    </div>

    <div class="tabs">
        <button class="tab active" onclick="showTab('local')">My Profiles</button>
        <button class="tab" onclick="showTab('community')">Community</button>
        <button class="tab" onclick="showTab('create')">Create</button>
    </div>

    <div id="local" class="section active">
        <div id="local-profiles"></div>
    </div>

    <div id="community" class="section">
        <div class="input-group">
            <input type="text" id="gist-id" placeholder="Enter Gist ID to import...">
            <button class="btn btn-primary" onclick="importFromGist()">Import</button>
        </div>
        <div class="profile-card featured">
            <h3>Featured: Developer Setup</h3>
            <p>Optimized keys and colors for development work</p>
            <div class="profile-actions">
                <button class="btn btn-primary" onclick="importFromGist('example-dev-setup')">Install</button>
            </div>
        </div>
        <p style="color:#666; text-align:center; margin-top:20px;">
            Share your profiles! Upload with:<br>
            <code style="color:#e94560">mobilecli-ui gist-upload my-profile</code>
        </p>
    </div>

    <div id="create" class="section">
        <div class="input-group">
            <input type="text" id="new-profile-name" placeholder="Profile name...">
            <button class="btn btn-primary" onclick="saveNewProfile()">Save Current</button>
        </div>
        <p style="color:#666; margin-top:16px;">
            This will save your current UI settings (text size, colors, keys, overlay settings) as a new profile.
        </p>
    </div>

    <div class="toast" id="toast"></div>

    <script>
        function showTab(tabId) {
            document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
            document.querySelectorAll('.section').forEach(s => s.classList.remove('active'));
            document.querySelector('[onclick*="' + tabId + '"]').classList.add('active');
            document.getElementById(tabId).classList.add('active');
            if (tabId === 'local') loadLocalProfiles();
        }

        function showToast(msg) {
            const toast = document.getElementById('toast');
            toast.textContent = msg;
            toast.style.display = 'block';
            setTimeout(() => toast.style.display = 'none', 3000);
        }

        function loadLocalProfiles() {
            const container = document.getElementById('local-profiles');
            const profiles = JSON.parse(MobileCLI.getLocalProfiles() || '[]');

            if (profiles.length === 0) {
                container.innerHTML = '<div class="empty-state">No profiles yet.<br>Create one in the "Create" tab!</div>';
                return;
            }

            container.innerHTML = profiles.map(name =>
                '<div class="profile-card">' +
                    '<h3>' + name + '</h3>' +
                    '<div class="profile-actions">' +
                        '<button class="btn btn-primary" onclick="loadProfile(\'' + name + '\')">Load</button>' +
                        '<button class="btn btn-secondary" onclick="exportProfile(\'' + name + '\')">Export</button>' +
                        '<button class="btn btn-danger" onclick="deleteProfile(\'' + name + '\')">Delete</button>' +
                    '</div>' +
                '</div>'
            ).join('');
        }

        function loadProfile(name) {
            const result = MobileCLI.loadProfile(name);
            showToast(result && result.includes('OK') ? 'Profile loaded!' : (result || 'Error loading profile'));
        }

        function exportProfile(name) {
            const result = MobileCLI.exportProfile(name);
            showToast(result && result.includes('OK') ? 'Exported to Downloads!' : (result || 'Error exporting'));
        }

        function deleteProfile(name) {
            if (confirm('Delete profile "' + name + '"?')) {
                const result = MobileCLI.deleteProfile(name);
                showToast(result && result.includes('OK') ? 'Deleted!' : (result || 'Error deleting'));
                loadLocalProfiles();
            }
        }

        function saveNewProfile() {
            const name = document.getElementById('new-profile-name').value.trim();
            if (!name) { showToast('Enter a profile name'); return; }
            if (!/^[a-zA-Z0-9_-]+$/.test(name)) {
                showToast('Name can only contain letters, numbers, underscore, dash');
                return;
            }
            const result = MobileCLI.saveProfile(name);
            if (result && result.includes('OK')) {
                showToast('Profile saved!');
                document.getElementById('new-profile-name').value = '';
                // Switch to My Profiles tab to show the new profile
                showTab('local');
            } else {
                showToast(result || 'Error saving profile');
            }
        }

        function importFromGist() {
            const gistId = document.getElementById('gist-id').value.trim();
            if (!gistId) { showToast('Enter a Gist ID'); return; }
            showToast('Downloading from GitHub...');
            MobileCLI.runCmd('mobilecli-ui gist-download ' + gistId);
            document.getElementById('gist-id').value = '';
            setTimeout(() => {
                showToast('Import complete! Switching to My Profiles...');
                showTab('local');
            }, 3000);
        }

        // Load profiles on start
        loadLocalProfiles();
    </script>
</body>
</html>
""")
            Log.i(TAG, "Created profile_browser.html")

            // Create helper script to view/update memory
            val memoryScript = File(binDir, "mobilecli-memory")
            memoryScript.writeText("""#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI Memory System Helper
# View and manage persistent AI memory

MEMORY_DIR="${"$"}HOME/.mobilecli/memory"

show_help() {
    echo "MobileCLI Memory System"
    echo ""
    echo "Usage: mobilecli-memory <command>"
    echo ""
    echo "Commands:"
    echo "  status    - Show memory system status"
    echo "  history   - Show evolution history"
    echo "  problems  - Show solved problems"
    echo "  caps      - Show capabilities"
    echo "  goals     - Show current goals"
    echo "  log <msg> - Add rebuild log entry"
    echo ""
}

case "${"$"}1" in
    status)
        echo "=== MobileCLI Memory System ==="
        echo "Location: ${"$"}MEMORY_DIR"
        echo ""
        ls -la "${"$"}MEMORY_DIR" 2>/dev/null || echo "Memory not initialized"
        ;;
    history)
        cat "${"$"}MEMORY_DIR/evolution_history.json" 2>/dev/null | head -50
        ;;
    problems)
        cat "${"$"}MEMORY_DIR/problems_solved.json" 2>/dev/null | head -80
        ;;
    caps)
        cat "${"$"}MEMORY_DIR/capabilities.json" 2>/dev/null | head -50
        ;;
    goals)
        cat "${"$"}MEMORY_DIR/goals.json" 2>/dev/null | head -50
        ;;
    log)
        if [ -z "${"$"}2" ]; then
            echo "Usage: mobilecli-memory log \"message\""
            exit 1
        fi
        TIMESTAMP=${"$"}(date -Iseconds)
        # Append to rebuild log using jq if available, otherwise echo
        if command -v jq &>/dev/null; then
            TMP=${"$"}(mktemp)
            jq ".rebuild_log += [{\"timestamp\": \"${"$"}TIMESTAMP\", \"message\": \"${"$"}2\"}]" "${"$"}MEMORY_DIR/evolution_history.json" > "${"$"}TMP" && mv "${"$"}TMP" "${"$"}MEMORY_DIR/evolution_history.json"
            echo "Logged: ${"$"}2"
        else
            echo "jq not installed - install with: pkg install jq"
        fi
        ;;
    *)
        show_help
        ;;
esac
""")
            memoryScript.setExecutable(true)
            Log.i(TAG, "Created mobilecli-memory helper script")

            // Create self-rebuild script
            val rebuildScript = File(binDir, "mobilecli-rebuild")
            rebuildScript.writeText("""#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI Self-Rebuild Script
# Rebuilds the app from source - the AI modifying its own container

set -e

SOURCE_DIR="${"$"}HOME/MobileCLI-v2"
ANDROID_HOME="${"$"}HOME/android-sdk"

# Check if source exists
if [ ! -d "${"$"}SOURCE_DIR" ]; then
    echo "Source not found. Cloning..."
    cd ~ && git clone https://github.com/MobileDevCLI/MobileCLI-v2.git
fi

# Check if Android SDK is set up
if [ ! -f "${"$"}ANDROID_HOME/platforms/android-34/android.jar" ]; then
    echo "Android SDK not set up. Run: install-dev-tools"
    exit 1
fi

cd "${"$"}SOURCE_DIR"

# Pull latest changes
echo "Pulling latest changes..."
git pull 2>/dev/null || echo "Could not pull (offline or no remote)"

# Export Android SDK path
export ANDROID_HOME

# Build
echo "Building APK..."
./gradlew assembleDebug

# Copy to Download with timestamp
VERSION=${"$"}(date +%Y%m%d_%H%M%S)
OUTPUT="/sdcard/Download/MobileCLI-${"$"}VERSION.apk"
cp app/build/outputs/apk/debug/app-debug.apk "${"$"}OUTPUT"

echo ""
echo "=========================================="
echo "SUCCESS! APK ready at:"
echo "${"$"}OUTPUT"
echo "=========================================="
echo ""
echo "Install with: Install from file manager"
echo "Or run: termux-open ${"$"}OUTPUT"

# Log to memory system
if [ -f "${"$"}HOME/.mobilecli/memory/evolution_history.json" ] && command -v jq &>/dev/null; then
    TIMESTAMP=${"$"}(date -Iseconds)
    TMP=${"$"}(mktemp)
    jq ".rebuild_log += [{\"timestamp\": \"${"$"}TIMESTAMP\", \"output\": \"${"$"}OUTPUT\", \"event\": \"self_rebuild\"}]" \
        "${"$"}HOME/.mobilecli/memory/evolution_history.json" > "${"$"}TMP" && \
        mv "${"$"}TMP" "${"$"}HOME/.mobilecli/memory/evolution_history.json"
    echo "Rebuild logged to memory system"
fi
""")
            rebuildScript.setExecutable(true)
            Log.i(TAG, "Created mobilecli-rebuild script")

            // Create capabilities script
            val capsScript = File(binDir, "mobilecli-caps")
            capsScript.writeText("""#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI Capabilities - Quick reference for AI and users

cat << 'EOF'
╔═══════════════════════════════════════════════════════════════════╗
║                    MOBILECLI CAPABILITIES                         ║
╚═══════════════════════════════════════════════════════════════════╝

FILESYSTEM ACCESS:
  ~/                    Full read/write (your home directory)
  /sdcard/Download/     User-accessible files (PUT OUTPUTS HERE!)
  /sdcard/DCIM/         Camera photos
  /sdcard/Pictures/     Screenshots and images
  /sdcard/Documents/    User documents

BUILD TOOLS (run 'install-dev-tools' first):
  Java 17               openjdk-17
  Gradle                Build automation
  aapt/aapt2            Android asset packaging
  d8/dx                 DEX compilation
  apksigner             APK signing

MOBILECLI COMMANDS:
  install-dev-tools     Install Java, Gradle, Android SDK
  mobilecli-rebuild     Rebuild this app from source
  mobilecli-share       Share files via Bluetooth (phone-to-phone)
  mobilecli-memory      View/manage AI memory system
  mobilecli-caps        This help screen
  selfmod               Self-modification wizard
  setup-github          Configure GitHub credentials
  extract-source        Extract bundled source code

MULTI-AGENT SYSTEM (v67):
  agent discover        Find all Claude sessions
  agent read <id>       Read another agent's conversation
  agent tail <id>       Watch conversation in real-time
  agent send <id> <msg> Send message to another agent
  agent exec <pty> <cmd> Execute command in other terminal
  agent hub             Launch supervisor mode

TERMUX API (50+ commands):
  termux-clipboard-get/set     Clipboard access
  termux-toast "msg"           Show toast
  termux-notification          Send notification
  termux-open-url URL          Open browser
  termux-vibrate               Vibrate phone
  termux-camera-photo          Take photo
  termux-battery-status        Battery info
  termux-wifi-connectioninfo   WiFi info
  termux-tts-speak "text"      Text to speech
  termux-wake-lock/unlock      CPU wake control

QUICK EXAMPLES:
  # Save file for user
  echo "data" > /sdcard/Download/output.txt

  # Build an app
  install-dev-tools && mobilecli-rebuild

  # Open URL
  termux-open-url "https://example.com"

  # Send notification
  termux-notification -t "Done!" -c "Task complete"

╔═══════════════════════════════════════════════════════════════════╗
║  This is the most powerful AI environment on any phone. Use it.   ║
╚═══════════════════════════════════════════════════════════════════╝
EOF
""")
            capsScript.setExecutable(true)
            Log.i(TAG, "Created mobilecli-caps script")

            // Create Bluetooth/share script (v65)
            val shareScript = File(binDir, "mobilecli-share")
            shareScript.writeText("""#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI Share - Easy file sharing via Bluetooth/Nearby
# Perfect for phone-to-phone transfers in Two-Claude workflow

show_help() {
    cat << 'EOF'
MobileCLI Share - Phone-to-Phone File Transfer

Usage: mobilecli-share <file> [options]

Examples:
  mobilecli-share /sdcard/Download/MobileCLI-v65.apk
  mobilecli-share ~/project.zip
  mobilecli-share --latest-apk

Options:
  --latest-apk    Share the most recent MobileCLI APK
  --clipboard     Share clipboard contents as text file
  --help          Show this help

How it works:
  Uses Android's share intent with Bluetooth as the primary method.
  Select "Bluetooth" from the share menu to transfer directly to
  another phone without internet.

Two-Claude Workflow:
  BUILD PHONE: mobilecli-share --latest-apk
  TEST PHONE:  Receives via Bluetooth, installs, tests
EOF
}

# Handle options
case "${"$"}1" in
    --help|-h)
        show_help
        exit 0
        ;;
    --latest-apk)
        # Find most recent MobileCLI APK
        LATEST=${"$"}(ls -t /sdcard/Download/MobileCLI*.apk 2>/dev/null | head -1)
        if [ -z "${"$"}LATEST" ]; then
            echo "No MobileCLI APK found in /sdcard/Download/"
            echo "Build one first: mobilecli-rebuild"
            exit 1
        fi
        FILE="${"$"}LATEST"
        echo "Sharing: ${"$"}FILE"
        ;;
    --clipboard)
        # Share clipboard as text file
        CLIP=${"$"}(termux-clipboard-get 2>/dev/null)
        if [ -z "${"$"}CLIP" ]; then
            echo "Clipboard is empty"
            exit 1
        fi
        TMP="/sdcard/Download/clipboard_share.txt"
        echo "${"$"}CLIP" > "${"$"}TMP"
        FILE="${"$"}TMP"
        echo "Sharing clipboard contents..."
        ;;
    "")
        show_help
        exit 1
        ;;
    *)
        FILE="${"$"}1"
        ;;
esac

# Verify file exists
if [ ! -f "${"$"}FILE" ]; then
    echo "Error: File not found: ${"$"}FILE"
    exit 1
fi

# Get absolute path
ABSPATH=${"$"}(realpath "${"$"}FILE")

# Use termux-open to trigger share intent
# The share dialog will offer Bluetooth, Nearby Share, etc.
echo "Opening share dialog..."
echo "Select 'Bluetooth' to transfer to another phone"
am start -a android.intent.action.SEND \
    -t "*/*" \
    --eu android.intent.extra.STREAM "file://${"$"}ABSPATH" \
    --grant-read-uri-permission 2>/dev/null

# Also try content URI approach for newer Android
if [ ${"$"}? -ne 0 ]; then
    termux-open --send "${"$"}ABSPATH" 2>/dev/null || \
    termux-open "${"$"}ABSPATH" 2>/dev/null
fi

echo ""
echo "Share dialog opened!"
echo "Tip: Bluetooth is fastest for phone-to-phone transfers"
""")
            shareScript.setExecutable(true)
            Log.i(TAG, "Created mobilecli-share script")

            // Create Multi-Agent System directories (v67)
            val agentsDir = File(homeDir, ".mobilecli-agents")
            agentsDir.mkdirs()
            File(agentsDir, "hub").mkdirs()
            File(agentsDir, "sessions").mkdirs()
            File(agentsDir, "messages/inbox").mkdirs()
            File(agentsDir, "messages/outbox").mkdirs()
            File(agentsDir, "exec").mkdirs()
            File(agentsDir, "logs").mkdirs()
            Log.i(TAG, "Created multi-agent directories")

            // Create agent CLI script (v67)
            val agentScript = File(binDir, "agent")
            agentScript.writeText("""#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI Multi-Agent Hub - Main CLI
# Enables multiple Claude Code instances to communicate

AGENTS_DIR="${"$"}HOME/.mobilecli-agents"
CLAUDE_PROJECTS="${"$"}HOME/.claude/projects"
MESSAGES_DIR="${"$"}AGENTS_DIR/messages"
SESSIONS_DIR="${"$"}AGENTS_DIR/sessions"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

show_help() {
    echo -e "${"$"}{CYAN}MobileCLI Multi-Agent Hub${"$"}{NC}"
    echo ""
    echo "Usage: agent <command> [options]"
    echo ""
    echo "Commands:"
    echo "  discover        Find all active Claude sessions"
    echo "  read <id>       Read another agent's conversation"
    echo "  tail <id>       Watch conversation in real-time"
    echo "  send <id> <msg> Send message to another agent"
    echo "  inbox           Check your inbox"
    echo "  exec <pty> <cmd> Execute in another terminal"
    echo "  status          Show hub status"
    echo "  hub             Launch supervisor mode"
    echo ""
    echo "Examples:"
    echo "  agent discover"
    echo "  agent read 2c783855"
    echo "  agent tail 2c783855"
}

discover_sessions() {
    echo -e "${"$"}{CYAN}Discovering Claude sessions...${"$"}{NC}"
    shopt -s nullglob
    for project_dir in "${"$"}CLAUDE_PROJECTS"/*/; do
        for f in "${"$"}project_dir"*.jsonl; do
            [ -f "${"$"}f" ] && {
                sid=${"$"}(basename "${"$"}f" .jsonl)
                size=${"$"}(du -h "${"$"}f" | cut -f1)
                echo -e "${"$"}{GREEN}${"$"}{sid:0:8}${"$"}{NC} | ${"$"}size"
            }
        done
    done
    shopt -u nullglob
}

show_status() {
    echo -e "${"$"}{CYAN}Multi-Agent Hub Status${"$"}{NC}"
    local count=0
    shopt -s nullglob
    for project_dir in "${"$"}CLAUDE_PROJECTS"/*/; do
        for f in "${"$"}project_dir"*.jsonl; do
            [ -f "${"$"}f" ] && count=${"$"}((count + 1))
        done
    done
    shopt -u nullglob
    echo "Active sessions: ${"$"}count"
    echo "Hub directory: ${"$"}AGENTS_DIR"
}

case "${"$"}{1:-help}" in
    discover) discover_sessions ;;
    status) show_status ;;
    help|--help|-h) show_help ;;
    *)
        echo -e "${"$"}{YELLOW}For full agent features, install the complete hub:${"$"}{NC}"
        echo "  wget -O ~/.mobilecli-agents/hub/agent https://raw.githubusercontent.com/MobileDevCLI/MobileCLI-Developer/main/.mobilecli-agents/hub/agent"
        show_help
        ;;
esac
""")
            agentScript.setExecutable(true)
            Log.i(TAG, "Created agent script (v67)")

            // Create personal build enabler script
            val personalBuildScript = File(binDir, "mobilecli-dev-mode")
            personalBuildScript.writeText("""#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI Developer Mode Toggle
# Enables/disables always-on developer mode (for your personal build)

MARKER_FILE="${"$"}HOME/.mobilecli_dev_mode"

case "${"$"}1" in
    on|enable)
        touch "${"$"}MARKER_FILE"
        echo "Developer mode enabled permanently."
        echo "Restart MobileCLI to apply."
        ;;
    off|disable)
        rm -f "${"$"}MARKER_FILE"
        echo "Developer mode disabled."
        echo "Restart MobileCLI to apply."
        ;;
    status)
        if [ -f "${"$"}MARKER_FILE" ]; then
            echo "Developer mode: ENABLED (personal build)"
        else
            echo "Developer mode: Standard (7-tap to enable)"
        fi
        ;;
    *)
        echo "MobileCLI Developer Mode Control"
        echo ""
        echo "Usage: mobilecli-dev-mode <command>"
        echo ""
        echo "Commands:"
        echo "  on, enable     Enable always-on dev mode (personal build)"
        echo "  off, disable   Disable always-on dev mode"
        echo "  status         Show current status"
        echo ""
        echo "When enabled, developer mode is ON by default when app starts."
        echo "Perfect for testing and debugging."
        ;;
esac
""")
            personalBuildScript.setExecutable(true)
            Log.i(TAG, "Created mobilecli-dev-mode script")

            // Create dev tools installer script
            val devToolsScript = File(binDir, "install-dev-tools")
            devToolsScript.writeText("""#!/data/data/com.termux/files/usr/bin/bash
# Install Development Tools for MobileCLI Self-Modification
# This sets up everything needed to rebuild MobileCLI from within itself

echo "Installing development tools..."
echo "This may take several minutes on first run."
echo ""

# Install packages
pkg update -y
pkg install -y git openjdk-17 gradle aapt aapt2 apksigner d8 dx coreutils zip unzip

# Setup Android SDK structure
echo "Setting up Android SDK..."
mkdir -p ~/android-sdk/platforms/android-34
mkdir -p ~/android-sdk/build-tools/34.0.0

# Copy android.jar from aapt package
if [ -f /data/data/com.termux/files/usr/share/aapt/android.jar ]; then
    cp /data/data/com.termux/files/usr/share/aapt/android.jar ~/android-sdk/platforms/android-34/
    echo "Copied android.jar"
else
    echo "WARNING: android.jar not found. Some builds may fail."
fi

# Symlink build tools
cd ~/android-sdk/build-tools/34.0.0
ln -sf /data/data/com.termux/files/usr/bin/aapt aapt 2>/dev/null || true
ln -sf /data/data/com.termux/files/usr/bin/aapt2 aapt2 2>/dev/null || true
ln -sf /data/data/com.termux/files/usr/bin/d8 d8 2>/dev/null || true
ln -sf /data/data/com.termux/files/usr/bin/dx dx 2>/dev/null || true
ln -sf /data/data/com.termux/files/usr/bin/apksigner apksigner 2>/dev/null || true
ln -sf /data/data/com.termux/files/usr/bin/zipalign zipalign 2>/dev/null || true

echo ""
echo "=========================================="
echo "Development tools installed!"
echo "=========================================="
echo ""
echo "To rebuild MobileCLI:"
echo "  1. Clone source: git clone https://github.com/MobileDevCLI/MobileCLI-v2.git"
echo "  2. Build: mobilecli-rebuild"
echo ""
echo "Or for Proprietary edition:"
echo "  git clone https://github.com/MobileDevCLI/MobileCLI-Proprietary.git"
echo ""
""")
            devToolsScript.setExecutable(true)
            Log.i(TAG, "Created install-dev-tools script")

            // ============================================
            // DITTO ARCHITECTURE - UI Control Script (v84)
            // ============================================
            // This script allows AI and users to control the app's UI at runtime
            val uiScript = File(binDir, "mobilecli-ui")
            uiScript.writeText("""#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI UI Control - DITTO Architecture
# Control the app's UI via file-based IPC
# v84: Core implementation

TERMUX_DIR="${"$"}HOME/.termux"
CMD_FILE="${"$"}TERMUX_DIR/ui_command"
RESULT_FILE="${"$"}TERMUX_DIR/ui_result"

mkdir -p "${"$"}TERMUX_DIR"

show_help() {
    cat << 'HELP'
MobileCLI UI Control - DITTO Architecture
==========================================

Usage: mobilecli-ui <command> [args]

EXTRA KEY COMMANDS:
  add-key <row> <text> <action>   Add a button to extra keys row
  remove-key <row> <text>         Remove a button from row
  clear-keys <row>                Clear all buttons from row

  Row: 1 or 2
  Action: keycode (27=ESC), escape sequence (\e[A=up), or text

THEME COMMANDS:
  set-background <#RRGGBB>        Set terminal background color
  set-text-color <#RRGGBB>        Set terminal text color
  set-text-size <sp>              Set terminal font size (14-56)
  set-key-style <#bg> <#text>     Set extra keys colors

MORPHABLE UI COMMANDS (WebView overlay):
  load-ui <path>                  Load an HTML file as overlay UI
  hide-ui                         Hide the morphable overlay
  show-ui                         Show the morphable overlay
  ui-size <full|half|quarter>     Set overlay size
  ui-position <top|bottom|...>    Set overlay position
  ui-opacity <0.0-1.0>            Set overlay transparency
  inject-js <code>                Run JavaScript in the overlay

PROFILE/SOCIAL COMMANDS (save & share your setup):
  save-profile <name>             Save current setup as named profile
  load-profile <name>             Load a saved profile
  list-profiles                   List all saved profiles
  delete-profile <name>           Delete a saved profile
  export-profile <name>           Export profile to /sdcard/Download/
  import-profile <path>           Import profile from file
  share-profile <name>            Share profile via Bluetooth/apps
  full-state                      Get complete state as JSON

GITHUB CLOUD SYNC (backup profiles to GitHub Gist):
  gist-upload <name>              Upload profile to new GitHub Gist
  gist-download <gist-id>         Download profile from GitHub Gist
  gist-update <name> <gist-id>    Update existing Gist with profile
  gist-list                       List your GitHub Gists
  gist-set-token <token>          Set GitHub Personal Access Token

PROFILE BROWSER UI:
  browser                         Open visual profile browser

UTILITY COMMANDS:
  toast <message>                 Show a toast notification
  get-state                       Get current UI state as JSON
  factory-reset                   Reset UI to defaults

EXAMPLES:
  # Add ESC key to row 1
  mobilecli-ui add-key 1 ESC 27

  # Add arrow keys
  mobilecli-ui add-key 1 "↑" "\e[A"
  mobilecli-ui add-key 1 "↓" "\e[B"

  # Change theme
  mobilecli-ui set-background "#1a1a2e"
  mobilecli-ui set-text-size 32

  # Show a toast
  mobilecli-ui toast "Hello from shell!"

  # Reset everything
  mobilecli-ui factory-reset

  # Load a custom HTML UI (DJ soundboard, game controller, etc.)
  mobilecli-ui load-ui ~/.mobilecli/ui/soundboard.html

  # Set overlay to bottom half of screen
  mobilecli-ui ui-size half
  mobilecli-ui ui-position bottom

  # Make overlay semi-transparent
  mobilecli-ui ui-opacity 0.8

  # Hide the overlay
  mobilecli-ui hide-ui

  # Save your current setup
  mobilecli-ui save-profile my-dev-setup

  # Share it with friends
  mobilecli-ui share-profile my-dev-setup

  # Import someone else's profile
  mobilecli-ui import-profile MobileCLI-cool-theme.mcli

  # ===== GITHUB CLOUD SYNC =====
  # First, set your GitHub token (get one from github.com/settings/tokens)
  mobilecli-ui gist-set-token ghp_xxxxxxxxxxxx

  # Upload a profile to GitHub Gist (creates public shareable link)
  mobilecli-ui gist-upload my-dev-setup

  # Download someone's shared profile
  mobilecli-ui gist-download abc123def456

  # Update an existing gist with your changes
  mobilecli-ui gist-update my-dev-setup abc123def456

  # List your MobileCLI gists
  mobilecli-ui gist-list
HELP
}

send_command() {
    rm -f "${"$"}RESULT_FILE"
    echo "${"$"}@" > "${"$"}CMD_FILE"

    # Wait for result (up to 2 seconds)
    WAIT=0
    while [ ! -f "${"$"}RESULT_FILE" ] && [ "${"$"}WAIT" -lt 20 ]; do
        sleep 0.1
        WAIT=${"$"}((WAIT + 1))
    done

    if [ -f "${"$"}RESULT_FILE" ]; then
        cat "${"$"}RESULT_FILE"
        rm -f "${"$"}RESULT_FILE"
    else
        echo "ERROR: Command timed out"
        return 1
    fi
}

case "${"$"}1" in
    add-key)
        if [ -z "${"$"}4" ]; then
            echo "Usage: mobilecli-ui add-key <row> <text> <action>"
            echo "Example: mobilecli-ui add-key 1 ESC 27"
            exit 1
        fi
        send_command "ADD_KEY ${"$"}2 ${"$"}3 ${"$"}4"
        ;;
    remove-key)
        if [ -z "${"$"}3" ]; then
            echo "Usage: mobilecli-ui remove-key <row> <text>"
            exit 1
        fi
        send_command "REMOVE_KEY ${"$"}2 ${"$"}3"
        ;;
    clear-keys)
        if [ -z "${"$"}2" ]; then
            echo "Usage: mobilecli-ui clear-keys <row>"
            exit 1
        fi
        send_command "CLEAR_KEYS ${"$"}2"
        ;;
    set-background)
        if [ -z "${"$"}2" ]; then
            echo "Usage: mobilecli-ui set-background #RRGGBB"
            exit 1
        fi
        send_command "SET_BACKGROUND ${"$"}2"
        ;;
    set-text-color)
        if [ -z "${"$"}2" ]; then
            echo "Usage: mobilecli-ui set-text-color #RRGGBB"
            exit 1
        fi
        send_command "SET_TEXT_COLOR ${"$"}2"
        ;;
    set-text-size)
        if [ -z "${"$"}2" ]; then
            echo "Usage: mobilecli-ui set-text-size <sp>"
            exit 1
        fi
        send_command "SET_TEXT_SIZE ${"$"}2"
        ;;
    set-key-style)
        if [ -z "${"$"}3" ]; then
            echo "Usage: mobilecli-ui set-key-style #bgColor #textColor"
            exit 1
        fi
        send_command "SET_KEY_STYLE ${"$"}2 ${"$"}3"
        ;;
    toast)
        if [ -z "${"$"}2" ]; then
            echo "Usage: mobilecli-ui toast <message>"
            exit 1
        fi
        shift
        send_command "SHOW_TOAST ${"$"}@"
        ;;
    get-state)
        send_command "GET_UI_STATE"
        ;;
    factory-reset)
        echo "Resetting UI to factory defaults..."
        send_command "FACTORY_RESET"
        ;;
    load-ui)
        if [ -z "${"$"}2" ]; then
            echo "Usage: mobilecli-ui load-ui <path/to/ui.html>"
            exit 1
        fi
        send_command "LOAD_UI ${"$"}2"
        ;;
    hide-ui)
        send_command "HIDE_UI"
        ;;
    show-ui)
        send_command "SHOW_UI"
        ;;
    ui-size)
        if [ -z "${"$"}2" ]; then
            echo "Usage: mobilecli-ui ui-size <full|half|quarter>"
            exit 1
        fi
        send_command "UI_SIZE ${"$"}2"
        ;;
    ui-position)
        if [ -z "${"$"}2" ]; then
            echo "Usage: mobilecli-ui ui-position <top|bottom|left|right|center>"
            exit 1
        fi
        send_command "UI_POSITION ${"$"}2"
        ;;
    ui-opacity)
        if [ -z "${"$"}2" ]; then
            echo "Usage: mobilecli-ui ui-opacity <0.0-1.0>"
            exit 1
        fi
        send_command "UI_OPACITY ${"$"}2"
        ;;
    inject-js)
        if [ -z "${"$"}2" ]; then
            echo "Usage: mobilecli-ui inject-js <javascript_code>"
            exit 1
        fi
        shift
        send_command "INJECT_JS ${"$"}@"
        ;;
    save-profile)
        if [ -z "${"$"}2" ]; then
            echo "Usage: mobilecli-ui save-profile <name>"
            exit 1
        fi
        send_command "SAVE_PROFILE ${"$"}2"
        ;;
    load-profile)
        if [ -z "${"$"}2" ]; then
            echo "Usage: mobilecli-ui load-profile <name>"
            exit 1
        fi
        send_command "LOAD_PROFILE ${"$"}2"
        ;;
    list-profiles)
        send_command "LIST_PROFILES"
        ;;
    delete-profile)
        if [ -z "${"$"}2" ]; then
            echo "Usage: mobilecli-ui delete-profile <name>"
            exit 1
        fi
        send_command "DELETE_PROFILE ${"$"}2"
        ;;
    export-profile)
        if [ -z "${"$"}2" ]; then
            echo "Usage: mobilecli-ui export-profile <name>"
            exit 1
        fi
        send_command "EXPORT_PROFILE ${"$"}2"
        ;;
    import-profile)
        if [ -z "${"$"}2" ]; then
            echo "Usage: mobilecli-ui import-profile <path>"
            exit 1
        fi
        send_command "IMPORT_PROFILE ${"$"}2"
        ;;
    share-profile)
        if [ -z "${"$"}2" ]; then
            echo "Usage: mobilecli-ui share-profile <name>"
            exit 1
        fi
        send_command "SHARE_PROFILE ${"$"}2"
        ;;
    full-state)
        send_command "GET_FULL_STATE"
        ;;
    gist-set-token)
        if [ -z "${"$"}2" ]; then
            echo "Usage: mobilecli-ui gist-set-token <github-personal-access-token>"
            echo ""
            echo "Get a token from: https://github.com/settings/tokens"
            echo "Required scope: 'gist'"
            exit 1
        fi
        mkdir -p ~/.mobilecli
        echo "${"$"}2" > ~/.mobilecli/.github_token
        chmod 600 ~/.mobilecli/.github_token
        echo "GitHub token saved! You can now use gist-upload, gist-download, etc."
        ;;
    gist-upload)
        if [ -z "${"$"}2" ]; then
            echo "Usage: mobilecli-ui gist-upload <profile-name>"
            exit 1
        fi
        PROFILE_NAME="${"$"}2"
        PROFILE_FILE=~/.mobilecli/profiles/"${"$"}PROFILE_NAME".json
        if [ ! -f "${"$"}PROFILE_FILE" ]; then
            echo "ERROR: Profile '${"$"}PROFILE_NAME' not found. Use 'save-profile' first."
            exit 1
        fi
        if [ ! -f ~/.mobilecli/.github_token ]; then
            echo "ERROR: GitHub token not set. Run 'mobilecli-ui gist-set-token <token>' first."
            exit 1
        fi
        TOKEN=${"$"}(cat ~/.mobilecli/.github_token)
        # Create gist via GitHub API
        CONTENT=${"$"}(cat "${"$"}PROFILE_FILE" | sed 's/"/\\"/g' | tr -d '\n')
        RESPONSE=${"$"}(curl -s -X POST https://api.github.com/gists \
            -H "Authorization: token ${"$"}TOKEN" \
            -H "Accept: application/vnd.github.v3+json" \
            -d "{\"description\":\"MobileCLI DITTO Profile: ${"$"}PROFILE_NAME\",\"public\":true,\"files\":{\"${"$"}PROFILE_NAME.mcli.json\":{\"content\":\"${"$"}CONTENT\"}}}")
        GIST_ID=${"$"}(echo "${"$"}RESPONSE" | grep -o '"id": *"[^"]*"' | head -1 | cut -d'"' -f4)
        if [ -n "${"$"}GIST_ID" ]; then
            echo "SUCCESS! Profile uploaded to GitHub Gist"
            echo "Gist ID: ${"$"}GIST_ID"
            echo "URL: https://gist.github.com/${"$"}GIST_ID"
            echo ""
            echo "Share this command with others:"
            echo "  mobilecli-ui gist-download ${"$"}GIST_ID"
        else
            echo "ERROR: Failed to create gist"
            echo "${"$"}RESPONSE"
        fi
        ;;
    gist-download)
        if [ -z "${"$"}2" ]; then
            echo "Usage: mobilecli-ui gist-download <gist-id>"
            echo "Example: mobilecli-ui gist-download abc123def456"
            exit 1
        fi
        GIST_ID="${"$"}2"
        # Fetch gist (public gists don't need auth)
        RESPONSE=${"$"}(curl -s "https://api.github.com/gists/${"$"}GIST_ID")
        # Extract the first .mcli.json file content
        CONTENT=${"$"}(echo "${"$"}RESPONSE" | grep -A1 '"content":' | tail -1 | sed 's/^[[:space:]]*"//' | sed 's/"[,]*$//')
        if [ -z "${"$"}CONTENT" ] || echo "${"$"}CONTENT" | grep -q "Not Found"; then
            echo "ERROR: Could not fetch gist ${"$"}GIST_ID"
            exit 1
        fi
        # Save to temp file and import
        TEMP_FILE=/tmp/gist_profile_${"$"}${"$"}.json
        echo "${"$"}CONTENT" | sed 's/\\"/"/g' > "${"$"}TEMP_FILE"
        send_command "IMPORT_PROFILE ${"$"}TEMP_FILE"
        rm -f "${"$"}TEMP_FILE"
        ;;
    gist-update)
        if [ -z "${"$"}2" ] || [ -z "${"$"}3" ]; then
            echo "Usage: mobilecli-ui gist-update <profile-name> <gist-id>"
            exit 1
        fi
        PROFILE_NAME="${"$"}2"
        GIST_ID="${"$"}3"
        PROFILE_FILE=~/.mobilecli/profiles/"${"$"}PROFILE_NAME".json
        if [ ! -f "${"$"}PROFILE_FILE" ]; then
            echo "ERROR: Profile '${"$"}PROFILE_NAME' not found."
            exit 1
        fi
        if [ ! -f ~/.mobilecli/.github_token ]; then
            echo "ERROR: GitHub token not set."
            exit 1
        fi
        TOKEN=${"$"}(cat ~/.mobilecli/.github_token)
        CONTENT=${"$"}(cat "${"$"}PROFILE_FILE" | sed 's/"/\\"/g' | tr -d '\n')
        RESPONSE=${"$"}(curl -s -X PATCH "https://api.github.com/gists/${"$"}GIST_ID" \
            -H "Authorization: token ${"$"}TOKEN" \
            -H "Accept: application/vnd.github.v3+json" \
            -d "{\"files\":{\"${"$"}PROFILE_NAME.mcli.json\":{\"content\":\"${"$"}CONTENT\"}}}")
        if echo "${"$"}RESPONSE" | grep -q '"id":'; then
            echo "SUCCESS! Gist updated."
            echo "URL: https://gist.github.com/${"$"}GIST_ID"
        else
            echo "ERROR: Failed to update gist"
        fi
        ;;
    gist-list)
        if [ ! -f ~/.mobilecli/.github_token ]; then
            echo "ERROR: GitHub token not set. Run 'mobilecli-ui gist-set-token <token>' first."
            exit 1
        fi
        TOKEN=${"$"}(cat ~/.mobilecli/.github_token)
        echo "Your MobileCLI Gists:"
        echo "---------------------"
        curl -s "https://api.github.com/gists" \
            -H "Authorization: token ${"$"}TOKEN" | \
            grep -E '"id"|"description"' | \
            grep -B1 "MobileCLI" | \
            sed 's/.*"id": *"/ID: /;s/",$//' | \
            sed 's/.*"description": *"/Desc: /' | \
            sed 's/",$//'
        ;;
    browser)
        send_command "SHOW_BROWSER"
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        if [ -z "${"$"}1" ]; then
            show_help
        else
            echo "Unknown command: ${"$"}1"
            echo "Run 'mobilecli-ui help' for usage"
            exit 1
        fi
        ;;
esac
""")
            uiScript.setExecutable(true)
            Log.i(TAG, "Created mobilecli-ui script (DITTO Architecture)")

            Log.i(TAG, "Persistent memory system initialized at ~/.mobilecli/memory/")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize persistent memory", e)
            // Non-fatal - app works without memory system
        }
    }

    /**
     * Get environment variables for the shell.
     * These must match real Termux exactly for full compatibility.
     * See TERMUX_COMPARISON.md for complete analysis.
     */
    fun getEnvironment(): Array<String> {
        val uid = android.os.Process.myUid()
        val pid = android.os.Process.myPid()
        val termuxExecLib = File(libDir, "libtermux-exec-ld-preload.so")
        val tmpDir = File(prefixDir, "tmp")
        val varRunDir = File(File(prefixDir, "var"), "run")
        val etcDir = File(prefixDir, "etc")
        val tlsDir = File(etcDir, "tls")
        val certFile = File(tlsDir, "cert.pem")
        tmpDir.mkdirs()
        varRunDir.mkdirs()

        val envList = mutableListOf(
            // ==========================================
            // Core Unix Variables (from UnixShellEnvironment)
            // ==========================================
            "HOME=${homeDir.absolutePath}",
            "PREFIX=${prefixDir.absolutePath}",
            "PATH=${binDir.absolutePath}:/system/bin:/system/xbin",
            "LD_LIBRARY_PATH=${libDir.absolutePath}",
            "TMPDIR=${tmpDir.absolutePath}",
            "PWD=${homeDir.absolutePath}",
            "TERM=xterm-256color",
            "COLORTERM=truecolor",
            "LANG=en_US.UTF-8",
            "SHELL=${bashPath}",

            // ==========================================
            // User Info
            // ==========================================
            "USER=u0_a${uid % 100000}",
            "LOGNAME=u0_a${uid % 100000}",

            // ==========================================
            // TERMUX Core Variables (from TermuxShellEnvironment)
            // ==========================================
            "TERMUX_VERSION=0.118.0",
            "TERMUX_APK_RELEASE=MOBILECLI",
            "TERMUX_IS_DEBUGGABLE_BUILD=0",
            "TERMUX_MAIN_PACKAGE_FORMAT=debian",
            "TERMUX__PREFIX=${prefixDir.absolutePath}",
            "TERMUX__HOME=${homeDir.absolutePath}",
            "TERMUX__ROOTFS_DIR=${filesDir.absolutePath}",

            // ==========================================
            // TERMUX_APP Variables (from TermuxAppShellEnvironment)
            // These are checked by some tools/scripts
            // ==========================================
            "TERMUX_APP_PID=$pid",
            "TERMUX_APP__PID=$pid",
            "TERMUX_APP__UID=$uid",
            "TERMUX_APP__PACKAGE_NAME=com.termux",
            "TERMUX_APP__VERSION_NAME=1.0.0",
            "TERMUX_APP__VERSION_CODE=1",
            "TERMUX_APP__TARGET_SDK=28",
            "TERMUX_APP__USER_ID=0",
            "TERMUX_APP__IS_DEBUGGABLE_BUILD=false",
            "TERMUX_APP__APK_RELEASE=MOBILECLI",
            "TERMUX_APP__PACKAGE_MANAGER=apt",
            "TERMUX_APP__PACKAGE_VARIANT=apt-android-7",
            "TERMUX_APP__FILES_DIR=${filesDir.absolutePath}",
            "TERMUX_APP__DATA_DIR=/data/user/0/com.termux",
            "TERMUX_APP__LEGACY_DATA_DIR=/data/data/com.termux",

            // ==========================================
            // Android System Variables (from AndroidShellEnvironment)
            // ==========================================
            "ANDROID_DATA=/data",
            "ANDROID_ROOT=/system",
            "EXTERNAL_STORAGE=/sdcard",
            "ANDROID_STORAGE=/storage",

            // ==========================================
            // Support Variables
            // ==========================================
            "TMUX_TMPDIR=${varRunDir.absolutePath}",
            "BROWSER=termux-open-url",
            "COREPACK_ENABLE_AUTO_PIN=0"
        )

        // Add SSL certificate paths for Node.js/npm
        if (certFile.exists()) {
            envList.add("SSL_CERT_FILE=${certFile.absolutePath}")
            envList.add("NODE_EXTRA_CA_CERTS=${certFile.absolutePath}")
            envList.add("CURL_CA_BUNDLE=${certFile.absolutePath}")
        }

        // Add LD_PRELOAD for termux-exec if it exists - CRITICAL for child processes
        if (termuxExecLib.exists()) {
            envList.add("LD_PRELOAD=${termuxExecLib.absolutePath}")
        }

        return envList.toTypedArray()
    }

    /**
     * Create npm configuration file for Termux compatibility.
     * Keep this minimal - just like real Termux.
     */
    fun createNpmConfig() {
        val npmrc = File(homeDir, ".npmrc")
        // Real Termux only has this one line
        npmrc.writeText("foreground-scripts=true\n")
    }

    /**
     * Create GitHub CLI configuration structure.
     * Token can be provided via:
     * 1. ~/.termux/github_token file
     * 2. Manual: gh auth login or setup-github TOKEN
     * This enables the self-modifying AI workflow.
     */
    private fun createGitHubConfig() {
        try {
            // Create ~/.config/gh/ directory
            val ghConfigDir = File(File(homeDir, ".config"), "gh")
            ghConfigDir.mkdirs()

            // Check for token in ~/.termux/github_token (secret file not in git)
            val termuxDir = File(homeDir, ".termux")
            termuxDir.mkdirs()
            val tokenFile = File(termuxDir, "github_token")

            if (tokenFile.exists()) {
                // Token provided - create full config
                val token = tokenFile.readText().trim()
                val hostsYml = File(ghConfigDir, "hosts.yml")
                hostsYml.writeText("""github.com:
    user: MobileDevCLI
    oauth_token: $token
    git_protocol: https
""")
                Log.i(TAG, "GitHub config created from token file")
            } else {
                // No token - create setup instructions
                val setupScript = File(binDir, "setup-github")
                setupScript.writeText("""#!/data/data/com.termux/files/usr/bin/sh
# Setup GitHub for MobileCLI self-modifying AI workflow
# Usage: setup-github YOUR_TOKEN
# Get token from: https://github.com/settings/tokens

if [ -z "${'$'}1" ]; then
    echo "Usage: setup-github YOUR_GITHUB_TOKEN"
    echo "Get a token from: https://github.com/settings/tokens"
    echo "Required scopes: repo, workflow"
    exit 1
fi

mkdir -p ~/.config/gh
cat > ~/.config/gh/hosts.yml << EOF
github.com:
    user: MobileDevCLI
    oauth_token: ${'$'}1
    git_protocol: https
EOF

echo "GitHub configured! Try: gh repo list MobileDevCLI"
""")
                setupScript.setExecutable(true)
                Log.i(TAG, "Created setup-github script")
            }

            // Configure git user
            val gitconfig = File(homeDir, ".gitconfig")
            if (!gitconfig.exists()) {
                gitconfig.writeText("""[user]
    name = MobileCLI-AI
    email = ai@mobilecli.com
[credential]
    helper = store
""")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create GitHub config", e)
        }
    }

    /**
     * Install TermuxAm - Custom activity manager that runs via app_process
     * This is CRITICAL for URL opening to work properly.
     *
     * Real Termux's `am` command doesn't use system /system/bin/am directly.
     * Instead it uses TermuxAm which runs Java code through app_process,
     * giving it proper app permissions to start activities.
     *
     * Without this, `am start -a android.intent.action.VIEW` fails silently
     * because the shell process lacks proper permissions.
     */
    private fun installTermuxAm() {
        try {
            // Create termux-am directory
            val termuxAmDir = File(File(prefixDir, "libexec"), "termux-am")
            termuxAmDir.mkdirs()

            // Copy am.apk from assets
            val amApkDest = File(termuxAmDir, "am.apk")
            context.assets.open("termux-am/am.apk").use { input ->
                FileOutputStream(amApkDest).use { output ->
                    input.copyTo(output)
                }
            }

            // CRITICAL: Android 14+ requires am.apk to be READ-ONLY
            // Otherwise SystemClassLoader throws: SecurityException: Writable dex file is not allowed
            // See: https://github.com/termux/termux-packages/issues/16255
            amApkDest.setReadOnly()
            Runtime.getRuntime().exec(arrayOf("/system/bin/chmod", "0400", amApkDest.absolutePath)).waitFor()
            Log.i(TAG, "Copied am.apk to ${amApkDest.absolutePath} (set read-only for Android 14+)")

            // Create the am wrapper script in /usr/bin/
            // v54: Uses file-based command system - service executes with proper permissions
            val amScript = File(binDir, "am")
            amScript.writeText("""#!/data/data/com.termux/files/usr/bin/sh
# MobileCLI am - Activity Manager with proper app permissions
# v54: Uses file-based command system executed by TermuxService

TERMUX_AM_VERSION=0.9.0-mobilecli-v54

if [ "${'$'}1" = "--version" ]; then
    echo "${'$'}TERMUX_AM_VERSION"
    exit 0
fi

# Setup paths
TERMUX_DIR="${'$'}HOME/.termux"
CMD_FILE="${'$'}TERMUX_DIR/am_command"
RESULT_FILE="${'$'}TERMUX_DIR/am_result"
URL_FILE="${'$'}TERMUX_DIR/url_to_open"

mkdir -p "${'$'}TERMUX_DIR"

# Special fast path for VIEW actions (URLs) - MainActivity polls this
if [ "${'$'}1" = "start" ]; then
    # Check if this is a VIEW intent with URL
    IS_VIEW=0
    DATA=""

    for arg in "${'$'}@"; do
        case "${'$'}arg" in
            android.intent.action.VIEW) IS_VIEW=1 ;;
        esac
    done

    # Extract data URL
    ARGS="${'$'}@"
    case "${'$'}ARGS" in
        *-d\ *)
            DATA="${'$'}(echo "${'$'}ARGS" | sed -n 's/.*-d \([^ ]*\).*/\1/p')"
            ;;
    esac

    # Fast path: write URL to file, MainActivity picks it up
    if [ "${'$'}IS_VIEW" = "1" ] && [ -n "${'$'}DATA" ]; then
        echo "${'$'}DATA" > "${'$'}URL_FILE"
        echo "Starting: Intent { act=android.intent.action.VIEW dat=${'$'}DATA }"
        exit 0
    fi
fi

# General path: write full command to file, TermuxService executes it
rm -f "${'$'}RESULT_FILE"
echo "${'$'}@" > "${'$'}CMD_FILE"

# Wait for result (up to 3 seconds)
WAIT=0
while [ ! -f "${'$'}RESULT_FILE" ] && [ "${'$'}WAIT" -lt 30 ]; do
    sleep 0.1
    WAIT=${'$'}((WAIT + 1))
done

if [ -f "${'$'}RESULT_FILE" ]; then
    # Parse result: first line is exit code, rest is output
    EXIT_CODE="${'$'}(head -1 "${'$'}RESULT_FILE")"
    tail -n +2 "${'$'}RESULT_FILE"
    rm -f "${'$'}RESULT_FILE"
    exit "${'$'}{EXIT_CODE:-0}"
else
    echo "Error: Command timed out (service may not be running)" >&2
    exit 1
fi
""")
            amScript.setExecutable(true, false)
            Runtime.getRuntime().exec(arrayOf("/system/bin/chmod", "755", amScript.absolutePath)).waitFor()
            Log.i(TAG, "Created TermuxAm script at ${amScript.absolutePath}")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to install TermuxAm", e)
            // Don't fail the whole install - just log the error
            // URL opening may not work, but everything else will
        }
    }

    /**
     * Install ALL Termux API scripts - provides complete termux-* commands
     * Total: 39+ API commands for 100% Termux:API compatibility
     */
    fun installApiScripts() {
        val tmpDir = File(prefixDir, "tmp")
        tmpDir.mkdirs()

        // Base script template that sends broadcast to our receiver
        fun createApiScript(name: String, method: String, argsHandling: String = "\"\$@\"") {
            val script = File(binDir, name)
            script.writeText("""#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI API: $name
RESULT_FILE="${tmpDir.absolutePath}/api_result_$$"
am broadcast -a com.termux.api.API_CALL \
    --es api_method "$method" \
    --es api_args $argsHandling \
    --es result_file "${'$'}RESULT_FILE" \
    > /dev/null 2>&1
sleep 0.3
if [ -f "${'$'}RESULT_FILE" ]; then
    cat "${'$'}RESULT_FILE"
    rm -f "${'$'}RESULT_FILE"
fi
""")
            script.setExecutable(true, false)
        }

        // ============================================
        // CLIPBOARD (2 commands)
        // ============================================
        createApiScript("termux-clipboard-get", "clipboard-get", "\"\"")
        createApiScript("termux-clipboard-set", "clipboard-set", "\"\$*\"")

        // ============================================
        // NOTIFICATIONS (3 commands)
        // ============================================
        createApiScript("termux-toast", "toast", "\"\$*\"")

        // termux-notification with full argument parsing (matches real Termux)
        val notifScript = File(binDir, "termux-notification")
        notifScript.writeText("""#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI API: termux-notification
# Supports all standard Termux notification flags

TITLE="MobileCLI"
CONTENT=""
ID=""
PRIORITY="default"
ONGOING="false"
ALERT_ONCE="false"
SOUND="false"
VIBRATE=""
ACTION=""
GROUP=""

# Parse both short and long options
while [[ ${'$'}# -gt 0 ]]; do
    case "${'$'}1" in
        -t|--title)
            TITLE="${'$'}2"
            shift 2
            ;;
        -c|--content)
            CONTENT="${'$'}2"
            shift 2
            ;;
        -i|--id)
            ID="${'$'}2"
            shift 2
            ;;
        --priority)
            PRIORITY="${'$'}2"
            shift 2
            ;;
        --ongoing)
            ONGOING="true"
            shift
            ;;
        --alert-once)
            ALERT_ONCE="true"
            shift
            ;;
        --sound)
            SOUND="true"
            shift
            ;;
        --vibrate)
            VIBRATE="${'$'}2"
            shift 2
            ;;
        --action)
            ACTION="${'$'}2"
            shift 2
            ;;
        --group)
            GROUP="${'$'}2"
            shift 2
            ;;
        --button1|--button2|--button3|--on-delete|--led-color|--led-on|--led-off|--image-path|--icon|--type)
            # Accept but ignore these advanced flags for compatibility
            shift 2 2>/dev/null || shift
            ;;
        *)
            shift
            ;;
    esac
done

RESULT_FILE="${tmpDir.absolutePath}/api_result_$$"
am broadcast -a com.termux.api.API_CALL \
    --es api_method "notification" \
    --es api_args "${'$'}TITLE|${'$'}CONTENT|${'$'}ID|${'$'}PRIORITY|${'$'}ONGOING|${'$'}SOUND" \
    --es result_file "${'$'}RESULT_FILE" \
    > /dev/null 2>&1
sleep 0.2
if [ -f "${'$'}RESULT_FILE" ]; then
    cat "${'$'}RESULT_FILE"
    rm -f "${'$'}RESULT_FILE"
fi
""")
        notifScript.setExecutable(true, false)

        // termux-notification-remove
        createApiScript("termux-notification-remove", "notification-remove", "\"\$1\"")

        // ============================================
        // DEVICE INFO (6 commands)
        // ============================================
        createApiScript("termux-battery-status", "battery-status", "\"\"")
        createApiScript("termux-vibrate", "vibrate", "\"\${1:-1000}\"")
        createApiScript("termux-brightness", "brightness", "\"\"")
        createApiScript("termux-torch", "torch", "\"\${1:-on}\"")
        createApiScript("termux-volume", "volume", "\"\"")
        createApiScript("termux-audio-info", "audio-info", "\"\"")

        // ============================================
        // NETWORK & WIFI (3 commands)
        // ============================================
        createApiScript("termux-wifi-connectioninfo", "wifi-connectioninfo", "\"\"")
        createApiScript("termux-wifi-enable", "wifi-enable", "\"\$1\"")
        createApiScript("termux-wifi-scaninfo", "wifi-scaninfo", "\"\"")

        // ============================================
        // LOCATION (1 command)
        // ============================================
        createApiScript("termux-location", "location", "\"\"")

        // ============================================
        // CAMERA (2 commands)
        // ============================================
        createApiScript("termux-camera-info", "camera-info", "\"\"")

        // termux-camera-photo with file argument
        val cameraPhotoScript = File(binDir, "termux-camera-photo")
        cameraPhotoScript.writeText("""#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI API: termux-camera-photo
CAMERA_ID=0
OUTPUT_FILE=""
while getopts "c:o:" opt; do
    case ${'$'}opt in
        c) CAMERA_ID="${'$'}OPTARG" ;;
        o) OUTPUT_FILE="${'$'}OPTARG" ;;
    esac
done
if [ -z "${'$'}OUTPUT_FILE" ]; then
    echo "Usage: termux-camera-photo -o <output_file> [-c camera_id]"
    exit 1
fi
RESULT_FILE="${tmpDir.absolutePath}/api_result_$$"
am broadcast -a com.termux.api.API_CALL \
    --es api_method "camera-photo" \
    --es api_args "${'$'}CAMERA_ID|${'$'}OUTPUT_FILE" \
    --es result_file "${'$'}RESULT_FILE" \
    > /dev/null 2>&1
sleep 0.5
if [ -f "${'$'}RESULT_FILE" ]; then
    cat "${'$'}RESULT_FILE"
    rm -f "${'$'}RESULT_FILE"
fi
""")
        cameraPhotoScript.setExecutable(true, false)

        // ============================================
        // AUDIO & MEDIA (4 commands)
        // ============================================
        createApiScript("termux-media-scan", "media-scan", "\"\$1\"")

        // termux-media-player with action and file
        val mediaPlayerScript = File(binDir, "termux-media-player")
        mediaPlayerScript.writeText("""#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI API: termux-media-player
ACTION="${'$'}1"
FILE="${'$'}2"
RESULT_FILE="${tmpDir.absolutePath}/api_result_$$"
am broadcast -a com.termux.api.API_CALL \
    --es api_method "media-player" \
    --es api_args "${'$'}ACTION|${'$'}FILE" \
    --es result_file "${'$'}RESULT_FILE" \
    > /dev/null 2>&1
sleep 0.3
if [ -f "${'$'}RESULT_FILE" ]; then
    cat "${'$'}RESULT_FILE"
    rm -f "${'$'}RESULT_FILE"
fi
""")
        mediaPlayerScript.setExecutable(true, false)

        // termux-microphone-record
        val micRecordScript = File(binDir, "termux-microphone-record")
        micRecordScript.writeText("""#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI API: termux-microphone-record
ACTION="start"
FILE=""
LIMIT=0
while getopts "d:f:l:q" opt; do
    case ${'$'}opt in
        f) FILE="${'$'}OPTARG" ;;
        l) LIMIT="${'$'}OPTARG" ;;
        d) ;; # ignore default
        q) ACTION="stop" ;;
    esac
done
if [ "${'$'}ACTION" = "start" ] && [ -z "${'$'}FILE" ]; then
    echo "Usage: termux-microphone-record -f <file> [-l limit_secs]"
    exit 1
fi
RESULT_FILE="${tmpDir.absolutePath}/api_result_$$"
am broadcast -a com.termux.api.API_CALL \
    --es api_method "microphone-record" \
    --es api_args "${'$'}ACTION|${'$'}FILE" \
    --es result_file "${'$'}RESULT_FILE" \
    > /dev/null 2>&1
sleep 0.3
if [ -f "${'$'}RESULT_FILE" ]; then
    cat "${'$'}RESULT_FILE"
    rm -f "${'$'}RESULT_FILE"
fi
""")
        micRecordScript.setExecutable(true, false)

        // ============================================
        // TTS (2 commands)
        // ============================================
        createApiScript("termux-tts-engines", "tts-engines", "\"\"")
        createApiScript("termux-tts-speak", "tts-speak", "\"\$*\"")

        // ============================================
        // TELEPHONY (4 commands)
        // ============================================
        createApiScript("termux-telephony-call", "telephony-call", "\"\$1\"")
        createApiScript("termux-telephony-cellinfo", "telephony-cellinfo", "\"\"")
        createApiScript("termux-telephony-deviceinfo", "telephony-deviceinfo", "\"\"")

        // ============================================
        // SMS (2 commands)
        // ============================================
        // termux-sms-list with options
        val smsListScript = File(binDir, "termux-sms-list")
        smsListScript.writeText("""#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI API: termux-sms-list
TYPE="inbox"
LIMIT=10
while getopts "t:l:o:n:" opt; do
    case ${'$'}opt in
        t) TYPE="${'$'}OPTARG" ;;
        l) LIMIT="${'$'}OPTARG" ;;
        o) ;; # offset, ignored
        n) LIMIT="${'$'}OPTARG" ;;
    esac
done
RESULT_FILE="${tmpDir.absolutePath}/api_result_$$"
am broadcast -a com.termux.api.API_CALL \
    --es api_method "sms-list" \
    --es api_args "${'$'}TYPE|${'$'}LIMIT" \
    --es result_file "${'$'}RESULT_FILE" \
    > /dev/null 2>&1
sleep 0.3
if [ -f "${'$'}RESULT_FILE" ]; then
    cat "${'$'}RESULT_FILE"
    rm -f "${'$'}RESULT_FILE"
fi
""")
        smsListScript.setExecutable(true, false)

        // termux-sms-send
        val smsSendScript = File(binDir, "termux-sms-send")
        smsSendScript.writeText("""#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI API: termux-sms-send
NUMBER=""
while getopts "n:" opt; do
    case ${'$'}opt in
        n) NUMBER="${'$'}OPTARG" ;;
    esac
done
shift ${'$'}((OPTIND-1))
MESSAGE="${'$'}*"
if [ -z "${'$'}NUMBER" ] || [ -z "${'$'}MESSAGE" ]; then
    echo "Usage: termux-sms-send -n <number> <message>"
    exit 1
fi
RESULT_FILE="${tmpDir.absolutePath}/api_result_$$"
am broadcast -a com.termux.api.API_CALL \
    --es api_method "sms-send" \
    --es api_args "${'$'}NUMBER|${'$'}MESSAGE" \
    --es result_file "${'$'}RESULT_FILE" \
    > /dev/null 2>&1
sleep 0.3
if [ -f "${'$'}RESULT_FILE" ]; then
    cat "${'$'}RESULT_FILE"
    rm -f "${'$'}RESULT_FILE"
fi
""")
        smsSendScript.setExecutable(true, false)

        // ============================================
        // CONTACTS (1 command)
        // ============================================
        createApiScript("termux-contact-list", "contact-list", "\"\"")

        // ============================================
        // CALL LOG (1 command)
        // ============================================
        // termux-call-log with limit option
        val callLogScript = File(binDir, "termux-call-log")
        callLogScript.writeText("""#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI API: termux-call-log
LIMIT=10
while getopts "l:o:n:" opt; do
    case ${'$'}opt in
        l) LIMIT="${'$'}OPTARG" ;;
        n) LIMIT="${'$'}OPTARG" ;;
        o) ;; # offset, ignored
    esac
done
RESULT_FILE="${tmpDir.absolutePath}/api_result_$$"
am broadcast -a com.termux.api.API_CALL \
    --es api_method "call-log" \
    --es api_args "${'$'}LIMIT" \
    --es result_file "${'$'}RESULT_FILE" \
    > /dev/null 2>&1
sleep 0.3
if [ -f "${'$'}RESULT_FILE" ]; then
    cat "${'$'}RESULT_FILE"
    rm -f "${'$'}RESULT_FILE"
fi
""")
        callLogScript.setExecutable(true, false)

        // ============================================
        // SENSORS (1 command)
        // ============================================
        // termux-sensor with options
        val sensorScript = File(binDir, "termux-sensor")
        sensorScript.writeText("""#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI API: termux-sensor
SENSOR_TYPE=""
LIST_SENSORS=""
while getopts "s:ln:d:c:" opt; do
    case ${'$'}opt in
        s) SENSOR_TYPE="${'$'}OPTARG" ;;
        l) LIST_SENSORS="list" ;;
        n) ;; # count
        d) ;; # delay
        c) ;; # cleanup
    esac
done
if [ -n "${'$'}LIST_SENSORS" ]; then
    ARGS="list"
else
    ARGS="${'$'}SENSOR_TYPE"
fi
RESULT_FILE="${tmpDir.absolutePath}/api_result_$$"
am broadcast -a com.termux.api.API_CALL \
    --es api_method "sensor" \
    --es api_args "${'$'}ARGS" \
    --es result_file "${'$'}RESULT_FILE" \
    > /dev/null 2>&1
sleep 0.3
if [ -f "${'$'}RESULT_FILE" ]; then
    cat "${'$'}RESULT_FILE"
    rm -f "${'$'}RESULT_FILE"
fi
""")
        sensorScript.setExecutable(true, false)

        // ============================================
        // BIOMETRIC (1 command)
        // ============================================
        createApiScript("termux-fingerprint", "fingerprint", "\"\"")

        // ============================================
        // INFRARED (2 commands)
        // ============================================
        createApiScript("termux-infrared-frequencies", "infrared-frequencies", "\"\"")

        // termux-infrared-transmit with frequency and pattern
        val irTransmitScript = File(binDir, "termux-infrared-transmit")
        irTransmitScript.writeText("""#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI API: termux-infrared-transmit
FREQ=""
while getopts "f:" opt; do
    case ${'$'}opt in
        f) FREQ="${'$'}OPTARG" ;;
    esac
done
shift ${'$'}((OPTIND-1))
PATTERN="${'$'}*"
if [ -z "${'$'}FREQ" ]; then
    echo "Usage: termux-infrared-transmit -f <frequency> <pattern...>"
    exit 1
fi
RESULT_FILE="${tmpDir.absolutePath}/api_result_$$"
am broadcast -a com.termux.api.API_CALL \
    --es api_method "infrared-transmit" \
    --es api_args "${'$'}FREQ,${'$'}PATTERN" \
    --es result_file "${'$'}RESULT_FILE" \
    > /dev/null 2>&1
sleep 0.3
if [ -f "${'$'}RESULT_FILE" ]; then
    cat "${'$'}RESULT_FILE"
    rm -f "${'$'}RESULT_FILE"
fi
""")
        irTransmitScript.setExecutable(true, false)

        // ============================================
        // USB (1 command)
        // ============================================
        createApiScript("termux-usb", "usb", "\"\"")

        // ============================================
        // SYSTEM UTILITIES (6 commands)
        // ============================================
        createApiScript("termux-wallpaper", "wallpaper", "\"\$1\"")

        // termux-download with options
        val downloadScript = File(binDir, "termux-download")
        downloadScript.writeText("""#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI API: termux-download
TITLE="Download"
DESC=""
URL=""
while getopts "t:d:" opt; do
    case ${'$'}opt in
        t) TITLE="${'$'}OPTARG" ;;
        d) DESC="${'$'}OPTARG" ;;
    esac
done
shift ${'$'}((OPTIND-1))
URL="${'$'}1"
if [ -z "${'$'}URL" ]; then
    echo "Usage: termux-download [-t title] [-d description] <url>"
    exit 1
fi
RESULT_FILE="${tmpDir.absolutePath}/api_result_$$"
am broadcast -a com.termux.api.API_CALL \
    --es api_method "download" \
    --es api_args "${'$'}URL|${'$'}TITLE|${'$'}DESC" \
    --es result_file "${'$'}RESULT_FILE" \
    > /dev/null 2>&1
sleep 0.3
if [ -f "${'$'}RESULT_FILE" ]; then
    cat "${'$'}RESULT_FILE"
    rm -f "${'$'}RESULT_FILE"
fi
""")
        downloadScript.setExecutable(true, false)

        // termux-share
        val shareScript = File(binDir, "termux-share")
        shareScript.writeText("""#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI API: termux-share
ACTION="text"
CONTENT=""
while getopts "a:" opt; do
    case ${'$'}opt in
        a) ACTION="${'$'}OPTARG" ;;
    esac
done
shift ${'$'}((OPTIND-1))
if [ -n "${'$'}1" ]; then
    CONTENT="${'$'}1"
else
    # Read from stdin
    CONTENT=${'$'}(cat)
fi
RESULT_FILE="${tmpDir.absolutePath}/api_result_$$"
am broadcast -a com.termux.api.API_CALL \
    --es api_method "share" \
    --es api_args "${'$'}ACTION|${'$'}CONTENT" \
    --es result_file "${'$'}RESULT_FILE" \
    > /dev/null 2>&1
sleep 0.3
if [ -f "${'$'}RESULT_FILE" ]; then
    cat "${'$'}RESULT_FILE"
    rm -f "${'$'}RESULT_FILE"
fi
""")
        shareScript.setExecutable(true, false)

        // termux-dialog (basic implementation)
        val dialogScript = File(binDir, "termux-dialog")
        dialogScript.writeText("""#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI API: termux-dialog
TITLE="Input"
HINT=""
while getopts "t:i:" opt; do
    case ${'$'}opt in
        t) TITLE="${'$'}OPTARG" ;;
        i) HINT="${'$'}OPTARG" ;;
    esac
done
RESULT_FILE="${tmpDir.absolutePath}/api_result_$$"
am broadcast -a com.termux.api.API_CALL \
    --es api_method "dialog" \
    --es api_args "${'$'}TITLE|${'$'}HINT" \
    --es result_file "${'$'}RESULT_FILE" \
    > /dev/null 2>&1
sleep 0.5
if [ -f "${'$'}RESULT_FILE" ]; then
    cat "${'$'}RESULT_FILE"
    rm -f "${'$'}RESULT_FILE"
fi
""")
        dialogScript.setExecutable(true, false)

        createApiScript("termux-storage-get", "storage-get", "\"\$1\"")
        createApiScript("termux-job-scheduler", "job-scheduler", "\"\$*\"")

        // =====================================================
        // URL/FILE OPENING SCRIPTS - FIXED FOR ANDROID RESTRICTIONS
        // Android restricts starting activities from background context.
        // We use TermuxUrlHandlerActivity to provide foreground Activity context.
        // =====================================================

        // termux-open-url - Opens URLs using FILE-BASED IPC (v51 fix)
        // This ACTUALLY WORKS because MainActivity polls for the file and opens the URL
        // with proper foreground Activity context, bypassing Android 10+ restrictions
        val openUrlScript = File(binDir, "termux-open-url")
        openUrlScript.writeText("""#!/data/data/com.termux/files/usr/bin/sh
# v51: File-based URL opener - writes URL to file, MainActivity opens it

if [ ${'$'}# -lt 1 ]; then
    echo 'usage: termux-open-url <url>'
    echo 'Open a URL in browser.'
    exit 1
fi

URL="${'$'}1"
URL_FILE="${'$'}HOME/.termux/url_to_open"

# Ensure .termux directory exists
mkdir -p "${'$'}HOME/.termux"

# Write URL to file - MainActivity polls for this and opens it
echo "${'$'}URL" > "${'$'}URL_FILE"

echo "Opening: ${'$'}URL"
# Give the Activity time to pick it up
sleep 1
""")
        openUrlScript.setExecutable(true, false)

        // termux-open - Opens files and URLs, uses Activity for URLs, broadcast for files
        val termuxOpenScript = File(binDir, "termux-open")
        termuxOpenScript.writeText("""#!/data/data/com.termux/files/usr/bin/sh
set -e -u

SCRIPTNAME=termux-open
show_usage () {
    echo "Usage: ${'$'}SCRIPTNAME [options] path-or-url"
    echo "Open a file or URL in an external app."
    echo "  --send               if the file should be shared for sending"
    echo "  --view               if the file should be shared for viewing (default)"
    echo "  --chooser            if an app chooser should always be shown"
    echo "  --content-type type  specify the content type to use"
    exit 0
}

TEMP=`getopt \
     -n ${'$'}SCRIPTNAME \
     -o h \
     --long send,view,chooser,content-type:,help\
     -- "${'$'}@"`
eval set -- "${'$'}TEMP"

ACTION=android.intent.action.VIEW
EXTRAS=""
CONTENT_TYPE=""
while true; do
	case "${'$'}1" in
		--send) ACTION="android.intent.action.SEND"; shift;;
		--view) ACTION="android.intent.action.VIEW"; shift;;
		--chooser) EXTRAS="${'$'}EXTRAS --ez chooser true"; shift;;
		--content-type) CONTENT_TYPE="${'$'}2"; EXTRAS="${'$'}EXTRAS --es content-type ${'$'}2"; shift 2;;
		-h | --help) show_usage;;
		--) shift; break ;;
	esac
done
if [ ${'$'}# != 1 ]; then
	show_usage
fi

TARGET="${'$'}1"

# Check if it's a URL (starts with http://, https://, etc.)
case "${'$'}TARGET" in
    http://*|https://*|mailto:*|tel:*|sms:*|geo:*)
        # For URLs, use direct am start (same as real Termux)
        case "${'$'}{TERMUX__USER_ID:-}" in ''|*[!0-9]*|0[0-9]*) TERMUX__USER_ID=0;; esac
        if [ -n "${'$'}CONTENT_TYPE" ]; then
            am start --user "${'$'}TERMUX__USER_ID" -a android.intent.action.VIEW -t "${'$'}CONTENT_TYPE" -d "${'$'}TARGET" > /dev/null
        else
            am start --user "${'$'}TERMUX__USER_ID" -a android.intent.action.VIEW -d "${'$'}TARGET" > /dev/null
        fi
        exit 0
        ;;
esac

# For local files, resolve the path and use broadcast
if [ -f "${'$'}TARGET" ]; then
	TARGET=${'$'}(realpath "${'$'}TARGET")
fi

case "${'$'}{TERMUX__USER_ID:-}" in ''|*[!0-9]*|0[0-9]*) TERMUX__USER_ID=0;; esac

am broadcast --user "${'$'}TERMUX__USER_ID" \
	-a "${'$'}ACTION" \
	-n "com.termux/com.termux.app.TermuxOpenReceiver" \
	${'$'}EXTRAS \
	-d "${'$'}TARGET" \
	> /dev/null 2>&1
""")
        termuxOpenScript.setExecutable(true, false)

        // xdg-open - Symlink to termux-open (freedesktop.org standard)
        val xdgOpenScript = File(binDir, "xdg-open")
        xdgOpenScript.writeText("""#!/data/data/com.termux/files/usr/bin/sh
# xdg-open - wrapper around termux-open for freedesktop.org compatibility
exec termux-open "${'$'}@"
""")
        xdgOpenScript.setExecutable(true, false)

        // sensible-browser - wrapper for termux-open-url
        val sensibleBrowserScript = File(binDir, "sensible-browser")
        sensibleBrowserScript.writeText("""#!/data/data/com.termux/files/usr/bin/sh
exec termux-open-url "${'$'}@"
""")
        sensibleBrowserScript.setExecutable(true, false)

        // Volume
        createApiScript("termux-volume", "volume", "\"\"")

        // WiFi info
        createApiScript("termux-wifi-connectioninfo", "wifi-connectioninfo", "\"\"")

        // Brightness
        createApiScript("termux-brightness", "brightness", "\"\"")

        // Torch/Flashlight
        createApiScript("termux-torch", "torch", "\"\${1:-on}\"")

        // Media scan
        createApiScript("termux-media-scan", "media-scan", "\"\$1\"")

        // TTS (placeholder)
        createApiScript("termux-tts-speak", "tts-speak", "\"\$*\"")

        // Create termux-setup-storage script
        val storageScript = File(binDir, "termux-setup-storage")
        storageScript.writeText("""#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI: Setup storage symlinks
STORAGE_DIR="${homeDir.absolutePath}/storage"
mkdir -p "${'$'}STORAGE_DIR"
ln -sf /sdcard "${'$'}STORAGE_DIR/shared"
ln -sf /sdcard/DCIM "${'$'}STORAGE_DIR/dcim"
ln -sf /sdcard/Download "${'$'}STORAGE_DIR/downloads"
ln -sf /sdcard/Pictures "${'$'}STORAGE_DIR/pictures"
ln -sf /sdcard/Music "${'$'}STORAGE_DIR/music"
ln -sf /sdcard/Movies "${'$'}STORAGE_DIR/movies"
echo "Storage setup complete. Access via ~/storage/"
""")
        storageScript.setExecutable(true, false)

        // Create termux-reload-settings (no-op for compatibility)
        val reloadScript = File(binDir, "termux-reload-settings")
        reloadScript.writeText("""#!/data/data/com.termux/files/usr/bin/bash
echo "Settings reloaded"
""")
        reloadScript.setExecutable(true, false)

        // Create termux-info
        val infoScript = File(binDir, "termux-info")
        infoScript.writeText("""#!/data/data/com.termux/files/usr/bin/bash
echo "MobileCLI Terminal"
echo "=================="
echo "App: MobileCLI (com.termux)"
echo "HOME: ${'$'}HOME"
echo "PREFIX: ${'$'}PREFIX"
echo "Android: $(getprop ro.build.version.release)"
echo "Device: $(getprop ro.product.model)"
""")
        infoScript.setExecutable(true, false)

        // ============================================
        // KEYSTORE API (5 commands)
        // ============================================
        createApiScript("termux-keystore-list", "keystore-list", "\"\"")

        // termux-keystore with subcommands
        val keystoreScript = File(binDir, "termux-keystore")
        keystoreScript.writeText("""#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI API: termux-keystore
# Usage: termux-keystore <command> [args]
# Commands: list, generate, delete, sign, verify

COMMAND="${'$'}1"
shift

case "${'$'}COMMAND" in
    list)
        RESULT_FILE="${tmpDir.absolutePath}/api_result_$$"
        am broadcast -a com.termux.api.API_CALL \
            --es api_method "keystore-list" \
            --es api_args "" \
            --es result_file "${'$'}RESULT_FILE" \
            > /dev/null 2>&1
        sleep 0.3
        if [ -f "${'$'}RESULT_FILE" ]; then
            cat "${'$'}RESULT_FILE"
            rm -f "${'$'}RESULT_FILE"
        fi
        ;;
    generate)
        ALIAS=""
        ALGORITHM="AES"
        SIZE=256
        while getopts "a:g:s:" opt; do
            case ${'$'}opt in
                a) ALIAS="${'$'}OPTARG" ;;
                g) ALGORITHM="${'$'}OPTARG" ;;
                s) SIZE="${'$'}OPTARG" ;;
            esac
        done
        if [ -z "${'$'}ALIAS" ]; then
            echo "Usage: termux-keystore generate -a <alias> [-g algorithm] [-s size]"
            exit 1
        fi
        RESULT_FILE="${tmpDir.absolutePath}/api_result_$$"
        am broadcast -a com.termux.api.API_CALL \
            --es api_method "keystore-generate" \
            --es api_args "${'$'}ALIAS|${'$'}ALGORITHM|${'$'}SIZE" \
            --es result_file "${'$'}RESULT_FILE" \
            > /dev/null 2>&1
        sleep 0.3
        if [ -f "${'$'}RESULT_FILE" ]; then
            cat "${'$'}RESULT_FILE"
            rm -f "${'$'}RESULT_FILE"
        fi
        ;;
    delete)
        ALIAS="${'$'}1"
        if [ -z "${'$'}ALIAS" ]; then
            echo "Usage: termux-keystore delete <alias>"
            exit 1
        fi
        RESULT_FILE="${tmpDir.absolutePath}/api_result_$$"
        am broadcast -a com.termux.api.API_CALL \
            --es api_method "keystore-delete" \
            --es api_args "${'$'}ALIAS" \
            --es result_file "${'$'}RESULT_FILE" \
            > /dev/null 2>&1
        sleep 0.3
        if [ -f "${'$'}RESULT_FILE" ]; then
            cat "${'$'}RESULT_FILE"
            rm -f "${'$'}RESULT_FILE"
        fi
        ;;
    sign)
        ALIAS=""
        DATA=""
        while getopts "a:d:" opt; do
            case ${'$'}opt in
                a) ALIAS="${'$'}OPTARG" ;;
                d) DATA="${'$'}OPTARG" ;;
            esac
        done
        if [ -z "${'$'}ALIAS" ] || [ -z "${'$'}DATA" ]; then
            echo "Usage: termux-keystore sign -a <alias> -d <data>"
            exit 1
        fi
        RESULT_FILE="${tmpDir.absolutePath}/api_result_$$"
        am broadcast -a com.termux.api.API_CALL \
            --es api_method "keystore-sign" \
            --es api_args "${'$'}ALIAS|${'$'}DATA" \
            --es result_file "${'$'}RESULT_FILE" \
            > /dev/null 2>&1
        sleep 0.3
        if [ -f "${'$'}RESULT_FILE" ]; then
            cat "${'$'}RESULT_FILE"
            rm -f "${'$'}RESULT_FILE"
        fi
        ;;
    verify)
        ALIAS=""
        SIGNATURE=""
        IV=""
        while getopts "a:s:i:" opt; do
            case ${'$'}opt in
                a) ALIAS="${'$'}OPTARG" ;;
                s) SIGNATURE="${'$'}OPTARG" ;;
                i) IV="${'$'}OPTARG" ;;
            esac
        done
        if [ -z "${'$'}ALIAS" ] || [ -z "${'$'}SIGNATURE" ] || [ -z "${'$'}IV" ]; then
            echo "Usage: termux-keystore verify -a <alias> -s <signature> -i <iv>"
            exit 1
        fi
        RESULT_FILE="${tmpDir.absolutePath}/api_result_$$"
        am broadcast -a com.termux.api.API_CALL \
            --es api_method "keystore-verify" \
            --es api_args "${'$'}ALIAS|${'$'}SIGNATURE|${'$'}IV" \
            --es result_file "${'$'}RESULT_FILE" \
            > /dev/null 2>&1
        sleep 0.3
        if [ -f "${'$'}RESULT_FILE" ]; then
            cat "${'$'}RESULT_FILE"
            rm -f "${'$'}RESULT_FILE"
        fi
        ;;
    *)
        echo "Usage: termux-keystore <command> [args]"
        echo "Commands:"
        echo "  list                   List all keys in the keystore"
        echo "  generate -a <alias>    Generate a new key"
        echo "  delete <alias>         Delete a key"
        echo "  sign -a <alias> -d <data>   Sign data with a key"
        echo "  verify -a <alias> -s <sig> -i <iv>   Verify signature"
        exit 1
        ;;
esac
""")
        keystoreScript.setExecutable(true, false)

        // ============================================
        // NFC API (1 command)
        // ============================================
        createApiScript("termux-nfc", "nfc", "\"\"")

        // ============================================
        // NOTIFICATION LIST API (1 command)
        // ============================================
        createApiScript("termux-notification-list", "notification-list", "\"\"")

        // ============================================
        // SPEECH TO TEXT API (1 command)
        // ============================================
        createApiScript("termux-speech-to-text", "speech-to-text", "\"\"")

        // ============================================
        // SAF (Storage Access Framework) - 9 commands
        // ============================================

        // termux-saf-ls - List directory contents
        val safLsScript = File(binDir, "termux-saf-ls")
        safLsScript.writeText("""#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI API: termux-saf-ls
# List contents of a SAF directory
if [ -z "${'$'}1" ]; then
    echo "Usage: termux-saf-ls <document_uri>"
    echo "Use termux-saf-managedir to get a directory URI first"
    exit 1
fi
RESULT_FILE="${tmpDir.absolutePath}/api_result_$$"
am broadcast -a com.termux.api.API_CALL \
    --es api_method "saf-ls" \
    --es api_args "${'$'}1" \
    --es result_file "${'$'}RESULT_FILE" \
    > /dev/null 2>&1
sleep 0.3
if [ -f "${'$'}RESULT_FILE" ]; then
    cat "${'$'}RESULT_FILE"
    rm -f "${'$'}RESULT_FILE"
fi
""")
        safLsScript.setExecutable(true, false)

        // termux-saf-stat - Get file/directory info
        val safStatScript = File(binDir, "termux-saf-stat")
        safStatScript.writeText("""#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI API: termux-saf-stat
if [ -z "${'$'}1" ]; then
    echo "Usage: termux-saf-stat <document_uri>"
    exit 1
fi
RESULT_FILE="${tmpDir.absolutePath}/api_result_$$"
am broadcast -a com.termux.api.API_CALL \
    --es api_method "saf-stat" \
    --es api_args "${'$'}1" \
    --es result_file "${'$'}RESULT_FILE" \
    > /dev/null 2>&1
sleep 0.3
if [ -f "${'$'}RESULT_FILE" ]; then
    cat "${'$'}RESULT_FILE"
    rm -f "${'$'}RESULT_FILE"
fi
""")
        safStatScript.setExecutable(true, false)

        // termux-saf-read - Read file contents
        val safReadScript = File(binDir, "termux-saf-read")
        safReadScript.writeText("""#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI API: termux-saf-read
if [ -z "${'$'}1" ]; then
    echo "Usage: termux-saf-read <document_uri>"
    exit 1
fi
RESULT_FILE="${tmpDir.absolutePath}/api_result_$$"
am broadcast -a com.termux.api.API_CALL \
    --es api_method "saf-read" \
    --es api_args "${'$'}1" \
    --es result_file "${'$'}RESULT_FILE" \
    > /dev/null 2>&1
sleep 0.3
if [ -f "${'$'}RESULT_FILE" ]; then
    cat "${'$'}RESULT_FILE"
    rm -f "${'$'}RESULT_FILE"
fi
""")
        safReadScript.setExecutable(true, false)

        // termux-saf-write - Write to file
        val safWriteScript = File(binDir, "termux-saf-write")
        safWriteScript.writeText("""#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI API: termux-saf-write
if [ -z "${'$'}1" ]; then
    echo "Usage: termux-saf-write <document_uri> [content]"
    echo "If no content provided, reads from stdin"
    exit 1
fi
URI="${'$'}1"
shift
if [ -n "${'$'}*" ]; then
    CONTENT="${'$'}*"
else
    CONTENT=${'$'}(cat)
fi
RESULT_FILE="${tmpDir.absolutePath}/api_result_$$"
am broadcast -a com.termux.api.API_CALL \
    --es api_method "saf-write" \
    --es api_args "${'$'}URI|${'$'}CONTENT" \
    --es result_file "${'$'}RESULT_FILE" \
    > /dev/null 2>&1
sleep 0.3
if [ -f "${'$'}RESULT_FILE" ]; then
    cat "${'$'}RESULT_FILE"
    rm -f "${'$'}RESULT_FILE"
fi
""")
        safWriteScript.setExecutable(true, false)

        // termux-saf-mkdir - Create directory
        val safMkdirScript = File(binDir, "termux-saf-mkdir")
        safMkdirScript.writeText("""#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI API: termux-saf-mkdir
if [ -z "${'$'}1" ] || [ -z "${'$'}2" ]; then
    echo "Usage: termux-saf-mkdir <parent_uri> <directory_name>"
    exit 1
fi
RESULT_FILE="${tmpDir.absolutePath}/api_result_$$"
am broadcast -a com.termux.api.API_CALL \
    --es api_method "saf-mkdir" \
    --es api_args "${'$'}1|${'$'}2" \
    --es result_file "${'$'}RESULT_FILE" \
    > /dev/null 2>&1
sleep 0.3
if [ -f "${'$'}RESULT_FILE" ]; then
    cat "${'$'}RESULT_FILE"
    rm -f "${'$'}RESULT_FILE"
fi
""")
        safMkdirScript.setExecutable(true, false)

        // termux-saf-rm - Remove file/directory
        val safRmScript = File(binDir, "termux-saf-rm")
        safRmScript.writeText("""#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI API: termux-saf-rm
if [ -z "${'$'}1" ]; then
    echo "Usage: termux-saf-rm <document_uri>"
    exit 1
fi
RESULT_FILE="${tmpDir.absolutePath}/api_result_$$"
am broadcast -a com.termux.api.API_CALL \
    --es api_method "saf-rm" \
    --es api_args "${'$'}1" \
    --es result_file "${'$'}RESULT_FILE" \
    > /dev/null 2>&1
sleep 0.3
if [ -f "${'$'}RESULT_FILE" ]; then
    cat "${'$'}RESULT_FILE"
    rm -f "${'$'}RESULT_FILE"
fi
""")
        safRmScript.setExecutable(true, false)

        // termux-saf-create - Create new file
        val safCreateScript = File(binDir, "termux-saf-create")
        safCreateScript.writeText("""#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI API: termux-saf-create
MIME="text/plain"
while getopts "m:" opt; do
    case ${'$'}opt in
        m) MIME="${'$'}OPTARG" ;;
    esac
done
shift ${'$'}((OPTIND-1))
if [ -z "${'$'}1" ] || [ -z "${'$'}2" ]; then
    echo "Usage: termux-saf-create [-m mime_type] <parent_uri> <file_name>"
    exit 1
fi
RESULT_FILE="${tmpDir.absolutePath}/api_result_$$"
am broadcast -a com.termux.api.API_CALL \
    --es api_method "saf-create" \
    --es api_args "${'$'}1|${'$'}2|${'$'}MIME" \
    --es result_file "${'$'}RESULT_FILE" \
    > /dev/null 2>&1
sleep 0.3
if [ -f "${'$'}RESULT_FILE" ]; then
    cat "${'$'}RESULT_FILE"
    rm -f "${'$'}RESULT_FILE"
fi
""")
        safCreateScript.setExecutable(true, false)

        // termux-saf-managedir - Select directory via system UI
        val safManagedirScript = File(binDir, "termux-saf-managedir")
        safManagedirScript.writeText("""#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI API: termux-saf-managedir
# Opens system UI to select a directory for SAF access
echo "Opening directory picker..."
echo "After selecting a directory, the URI will be available for other saf-* commands."
am start -a android.intent.action.OPEN_DOCUMENT_TREE
""")
        safManagedirScript.setExecutable(true, false)

        // termux-saf-dirs - List available directories
        createApiScript("termux-saf-dirs", "saf-dirs", "\"\"")

        // ============================================
        // WAKE LOCK SCRIPTS - CRITICAL FOR OVERNIGHT USE
        // These call TermuxService to acquire/release wake locks
        // ============================================

        // termux-wake-lock - Acquire CPU and WiFi wake locks
        val wakeLockScript = File(binDir, "termux-wake-lock")
        wakeLockScript.writeText("""#!/data/data/com.termux/files/usr/bin/sh

if [ ${'$'}# != 0 ]; then
	echo 'usage: termux-wake-lock'
	echo 'Acquire the MobileCLI wake lock to prevent the CPU from sleeping.'
	exit 1
fi

case "${'$'}{TERMUX__USER_ID:-}" in ''|*[!0-9]*|0[0-9]*) TERMUX__USER_ID=0;; esac

am startservice \
	--user "${'$'}TERMUX__USER_ID" \
	-a "com.termux.service_wake_lock" \
	"com.termux/com.termux.app.TermuxService" \
	> /dev/null

echo "Wake lock acquired. CPU and WiFi will stay awake."
""")
        wakeLockScript.setExecutable(true, false)

        // termux-wake-unlock - Release wake locks
        val wakeUnlockScript = File(binDir, "termux-wake-unlock")
        wakeUnlockScript.writeText("""#!/data/data/com.termux/files/usr/bin/sh

if [ ${'$'}# != 0 ]; then
	echo 'usage: termux-wake-unlock'
	echo 'Release the MobileCLI wake lock to allow the CPU to sleep.'
	exit 1
fi

case "${'$'}{TERMUX__USER_ID:-}" in ''|*[!0-9]*|0[0-9]*) TERMUX__USER_ID=0;; esac

am startservice \
	--user "${'$'}TERMUX__USER_ID" \
	-a "com.termux.service_wake_unlock" \
	"com.termux/com.termux.app.TermuxService" \
	> /dev/null

echo "Wake lock released. Device can sleep now."
""")
        wakeUnlockScript.setExecutable(true, false)

        // ============================================
        // ADDITIONAL UTILITY SCRIPTS
        // These provide Termux compatibility features
        // ============================================

        // termux-change-repo - Change package repository mirror
        val changeRepoScript = File(binDir, "termux-change-repo")
        changeRepoScript.writeText("""#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI: termux-change-repo
# Change the package repository mirror

echo "MobileCLI Package Repository Selector"
echo "======================================"
echo ""
echo "Available mirrors:"
echo "1) Default (packages.termux.dev)"
echo "2) Grimler (grimler.se)"
echo "3) A1Batross (a1batross.github.io)"
echo "4) BFSU China (mirrors.bfsu.edu.cn)"
echo "5) Tsinghua China (mirrors.tuna.tsinghua.edu.cn)"
echo "6) USTC China (mirrors.ustc.edu.cn)"
echo ""

read -p "Select mirror [1-6]: " choice

case "${'$'}choice" in
    1)
        MIRROR="https://packages.termux.dev/apt/termux-main"
        ;;
    2)
        MIRROR="https://grimler.se/termux/termux-main"
        ;;
    3)
        MIRROR="https://a1batross.github.io/termux-main"
        ;;
    4)
        MIRROR="https://mirrors.bfsu.edu.cn/termux/apt/termux-main"
        ;;
    5)
        MIRROR="https://mirrors.tuna.tsinghua.edu.cn/termux/apt/termux-main"
        ;;
    6)
        MIRROR="https://mirrors.ustc.edu.cn/termux/apt/termux-main"
        ;;
    *)
        echo "Invalid selection"
        exit 1
        ;;
esac

echo ""
echo "Setting mirror to: ${'$'}MIRROR"

# Update sources.list
mkdir -p "${'$'}PREFIX/etc/apt"
echo "deb ${'$'}MIRROR stable main" > "${'$'}PREFIX/etc/apt/sources.list"

echo "Done! Run 'pkg update' to refresh package lists."
""")
        changeRepoScript.setExecutable(true, false)

        // termux-fix-shebang - Fix script shebangs for Termux paths
        val fixShebangScript = File(binDir, "termux-fix-shebang")
        fixShebangScript.writeText("""#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI: termux-fix-shebang
# Fix shebangs in scripts to use Termux paths

if [ ${'$'}# -lt 1 ]; then
    echo "Usage: termux-fix-shebang <file> [file2] ..."
    echo "Fix script shebangs to use Termux paths"
    exit 1
fi

for file in "${'$'}@"; do
    if [ ! -f "${'$'}file" ]; then
        echo "File not found: ${'$'}file"
        continue
    fi

    # Fix common shebangs
    sed -i 's|#!/bin/bash|#!/data/data/com.termux/files/usr/bin/bash|g' "${'$'}file"
    sed -i 's|#!/usr/bin/bash|#!/data/data/com.termux/files/usr/bin/bash|g' "${'$'}file"
    sed -i 's|#!/bin/sh|#!/data/data/com.termux/files/usr/bin/sh|g' "${'$'}file"
    sed -i 's|#!/usr/bin/sh|#!/data/data/com.termux/files/usr/bin/sh|g' "${'$'}file"
    sed -i 's|#!/usr/bin/env |#!/data/data/com.termux/files/usr/bin/env |g' "${'$'}file"
    sed -i 's|#!/bin/env |#!/data/data/com.termux/files/usr/bin/env |g' "${'$'}file"
    sed -i 's|#!/usr/bin/python|#!/data/data/com.termux/files/usr/bin/python|g' "${'$'}file"
    sed -i 's|#!/usr/bin/perl|#!/data/data/com.termux/files/usr/bin/perl|g' "${'$'}file"
    sed -i 's|#!/usr/bin/ruby|#!/data/data/com.termux/files/usr/bin/ruby|g' "${'$'}file"
    sed -i 's|#!/usr/bin/node|#!/data/data/com.termux/files/usr/bin/node|g' "${'$'}file"

    echo "Fixed: ${'$'}file"
done
""")
        fixShebangScript.setExecutable(true, false)

        // termux-reset - Reset Termux to clean state
        val resetScript = File(binDir, "termux-reset")
        resetScript.writeText("""#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI: termux-reset
# Reset to a clean state (keeps home directory)

echo "MobileCLI Reset Utility"
echo "======================="
echo ""
echo "WARNING: This will remove all installed packages!"
echo "Your home directory files will be preserved."
echo ""

read -p "Are you sure? (yes/no): " confirm

if [ "${'$'}confirm" != "yes" ]; then
    echo "Cancelled."
    exit 0
fi

echo ""
echo "Removing installed packages..."

# Remove all packages except essentials
pkg list-installed 2>/dev/null | while read pkg; do
    name=${'$'}(echo "${'$'}pkg" | cut -d/ -f1)
    case "${'$'}name" in
        apt|bash|coreutils|dash|dpkg|findutils|gawk|grep|gzip|less|libandroid*|libc*|ncurses*|readline|sed|tar|termux*)
            # Keep essential packages
            ;;
        *)
            pkg uninstall -y "${'$'}name" 2>/dev/null
            ;;
    esac
done

echo ""
echo "Clearing package cache..."
apt clean

echo ""
echo "Reset complete. Run 'pkg update && pkg upgrade' to refresh."
""")
        resetScript.setExecutable(true, false)

        // termux-backup - Backup home directory
        val backupScript = File(binDir, "termux-backup")
        backupScript.writeText("""#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI: termux-backup
# Backup home directory to a tar.gz file

OUTPUT="${'$'}1"

if [ -z "${'$'}OUTPUT" ]; then
    TIMESTAMP=${'$'}(date +%Y%m%d_%H%M%S)
    OUTPUT="/sdcard/Download/termux-backup-${'$'}TIMESTAMP.tar.gz"
fi

echo "MobileCLI Backup Utility"
echo "========================"
echo ""
echo "Backing up home directory to: ${'$'}OUTPUT"
echo ""

cd "${'$'}HOME" || exit 1

# Create backup excluding node_modules and caches
tar -czf "${'$'}OUTPUT" \
    --exclude='node_modules' \
    --exclude='.npm' \
    --exclude='.cache' \
    --exclude='.gradle' \
    --exclude='*.apk' \
    .

if [ ${'$'}? -eq 0 ]; then
    SIZE=${'$'}(ls -lh "${'$'}OUTPUT" | awk '{print ${'$'}5}')
    echo ""
    echo "Backup complete!"
    echo "File: ${'$'}OUTPUT"
    echo "Size: ${'$'}SIZE"
else
    echo "Backup failed!"
    exit 1
fi
""")
        backupScript.setExecutable(true, false)

        // termux-restore - Restore from backup
        val restoreScript = File(binDir, "termux-restore")
        restoreScript.writeText("""#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI: termux-restore
# Restore home directory from a tar.gz backup

INPUT="${'$'}1"

if [ -z "${'$'}INPUT" ]; then
    echo "Usage: termux-restore <backup-file.tar.gz>"
    echo ""
    echo "Available backups in /sdcard/Download/:"
    ls -lh /sdcard/Download/termux-backup-*.tar.gz 2>/dev/null || echo "  (none found)"
    exit 1
fi

if [ ! -f "${'$'}INPUT" ]; then
    echo "File not found: ${'$'}INPUT"
    exit 1
fi

echo "MobileCLI Restore Utility"
echo "========================="
echo ""
echo "WARNING: This will overwrite existing files in your home directory!"
echo "Backup file: ${'$'}INPUT"
echo ""

read -p "Are you sure? (yes/no): " confirm

if [ "${'$'}confirm" != "yes" ]; then
    echo "Cancelled."
    exit 0
fi

cd "${'$'}HOME" || exit 1

echo ""
echo "Restoring..."

tar -xzf "${'$'}INPUT"

if [ ${'$'}? -eq 0 ]; then
    echo ""
    echo "Restore complete!"
    echo "You may need to restart the terminal for all changes to take effect."
else
    echo "Restore failed!"
    exit 1
fi
""")
        restoreScript.setExecutable(true, false)

        // termux-file-editor - Open file in default editor
        val fileEditorScript = File(binDir, "termux-file-editor")
        fileEditorScript.writeText("""#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI: termux-file-editor
# Open a file in the system file editor

if [ ${'$'}# -lt 1 ]; then
    echo "Usage: termux-file-editor <file>"
    exit 1
fi

FILE="${'$'}1"

if [ ! -f "${'$'}FILE" ]; then
    echo "File not found: ${'$'}FILE"
    exit 1
fi

# Get absolute path
ABSPATH=${'$'}(realpath "${'$'}FILE")

# Try to open with system
am start -a android.intent.action.EDIT -d "file://${'$'}ABSPATH" -t "text/plain"
""")
        fileEditorScript.setExecutable(true, false)

        // termux-url-opener - Handle URLs opened from other apps
        val urlOpenerScript = File(binDir, "termux-url-opener")
        urlOpenerScript.writeText("""#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI: termux-url-opener
# Handle URLs opened from other apps (placeholder - customize in ~/.termux/termux-url-opener)

URL="${'$'}1"

# Check for user script
if [ -x "${'$'}HOME/.termux/termux-url-opener" ]; then
    exec "${'$'}HOME/.termux/termux-url-opener" "${'$'}URL"
fi

# Default behavior: print URL
echo "URL received: ${'$'}URL"
echo ""
echo "To customize URL handling, create ~/.termux/termux-url-opener"
""")
        urlOpenerScript.setExecutable(true, false)

        // termux-file-opener - Handle files opened from other apps
        val fileOpenerScript = File(binDir, "termux-file-opener")
        fileOpenerScript.writeText("""#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI: termux-file-opener
# Handle files opened from other apps (placeholder - customize in ~/.termux/termux-file-opener)

FILE="${'$'}1"

# Check for user script
if [ -x "${'$'}HOME/.termux/termux-file-opener" ]; then
    exec "${'$'}HOME/.termux/termux-file-opener" "${'$'}FILE"
fi

# Default behavior: show file info
if [ -f "${'$'}FILE" ]; then
    echo "File received: ${'$'}FILE"
    echo ""
    ls -la "${'$'}FILE"
    echo ""
    file "${'$'}FILE" 2>/dev/null
else
    echo "File not found: ${'$'}FILE"
fi

echo ""
echo "To customize file handling, create ~/.termux/termux-file-opener"
""")
        fileOpenerScript.setExecutable(true, false)

        // pkg-config wrapper for better compatibility
        val pkgConfigWrapper = File(binDir, "pkg-config")
        if (!pkgConfigWrapper.exists()) {
            pkgConfigWrapper.writeText("""#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI: pkg-config wrapper
# Wrapper that works even if pkg-config isn't installed

if command -v /data/data/com.termux/files/usr/bin/pkgconf &> /dev/null; then
    exec /data/data/com.termux/files/usr/bin/pkgconf "${'$'}@"
elif [ -f /data/data/com.termux/files/usr/bin/pkgconf ]; then
    exec /data/data/com.termux/files/usr/bin/pkgconf "${'$'}@"
else
    echo "pkg-config not found. Install with: pkg install pkg-config" >&2
    exit 1
fi
""")
            pkgConfigWrapper.setExecutable(true, false)
        }

        // install-apk-tools - Install APK decompilation tools for self-modification
        // TEST CLAUDE requested: apktool, jadx, smali, baksmali, dex2jar
        val apkToolsScript = File(binDir, "install-apk-tools")
        apkToolsScript.writeText("""#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI: install-apk-tools
# Install APK decompilation and modification tools
# Enables TEST CLAUDE to fully self-modify the MobileCLI app

echo "MobileCLI APK Tools Installer"
echo "============================="
echo ""
echo "This will install tools for APK modification:"
echo "  - apktool: Decode/rebuild APK resources & smali"
echo "  - jadx: Decompile dex to readable Java"
echo "  - smali/baksmali: Assemble/disassemble dex bytecode"
echo ""

# Create tools directory
TOOLS_DIR="${'$'}PREFIX/share/apk-tools"
mkdir -p "${'$'}TOOLS_DIR"
cd "${'$'}TOOLS_DIR"

echo "Installing apktool..."
curl -L -o apktool.jar "https://github.com/iBotPeaches/Apktool/releases/download/v2.9.3/apktool_2.9.3.jar" 2>/dev/null
if [ -f "apktool.jar" ]; then
    cat > "${'$'}PREFIX/bin/apktool" << 'APKTOOL_EOF'
#!/data/data/com.termux/files/usr/bin/sh
exec java -jar ${'$'}PREFIX/share/apk-tools/apktool.jar "${'$'}@"
APKTOOL_EOF
    chmod +x "${'$'}PREFIX/bin/apktool"
    echo "  apktool installed!"
else
    echo "  apktool download failed"
fi

echo "Installing smali/baksmali..."
curl -L -o smali.jar "https://github.com/google/smali/releases/download/v2.5.2/smali-2.5.2.jar" 2>/dev/null
curl -L -o baksmali.jar "https://github.com/google/smali/releases/download/v2.5.2/baksmali-2.5.2.jar" 2>/dev/null
if [ -f "smali.jar" ]; then
    cat > "${'$'}PREFIX/bin/smali" << 'SMALI_EOF'
#!/data/data/com.termux/files/usr/bin/sh
exec java -jar ${'$'}PREFIX/share/apk-tools/smali.jar "${'$'}@"
SMALI_EOF
    chmod +x "${'$'}PREFIX/bin/smali"
    echo "  smali installed!"
fi
if [ -f "baksmali.jar" ]; then
    cat > "${'$'}PREFIX/bin/baksmali" << 'BAKSMALI_EOF'
#!/data/data/com.termux/files/usr/bin/sh
exec java -jar ${'$'}PREFIX/share/apk-tools/baksmali.jar "${'$'}@"
BAKSMALI_EOF
    chmod +x "${'$'}PREFIX/bin/baksmali"
    echo "  baksmali installed!"
fi

echo "Installing jadx..."
JADX_URL="https://github.com/skylot/jadx/releases/download/v1.5.0/jadx-1.5.0.zip"
curl -L -o jadx.zip "${'$'}JADX_URL" 2>/dev/null
if [ -f "jadx.zip" ]; then
    unzip -q -o jadx.zip -d jadx/ 2>/dev/null
    if [ -f "jadx/bin/jadx" ]; then
        ln -sf "${'$'}TOOLS_DIR/jadx/bin/jadx" "${'$'}PREFIX/bin/jadx"
        ln -sf "${'$'}TOOLS_DIR/jadx/bin/jadx-gui" "${'$'}PREFIX/bin/jadx-gui"
        chmod +x jadx/bin/*
        echo "  jadx installed!"
    fi
    rm -f jadx.zip
else
    echo "  jadx download failed"
fi

# Create debug keystore if not exists
echo ""
echo "Creating debug keystore..."
KEYSTORE_DIR="${'$'}HOME/.android"
mkdir -p "${'$'}KEYSTORE_DIR"
if [ ! -f "${'$'}KEYSTORE_DIR/debug.keystore" ]; then
    keytool -genkey -v -keystore "${'$'}KEYSTORE_DIR/debug.keystore" \
        -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 \
        -storepass android -keypass android \
        -dname "CN=MobileCLI Debug,O=MobileCLI,C=US" 2>/dev/null
    if [ -f "${'$'}KEYSTORE_DIR/debug.keystore" ]; then
        echo "  Debug keystore created at ${'$'}KEYSTORE_DIR/debug.keystore"
    fi
else
    echo "  Debug keystore already exists"
fi

echo ""
echo "APK Tools Installation Complete!"
echo ""
echo "Available commands:"
echo "  apktool d <apk>     - Decode APK to folder"
echo "  apktool b <folder>  - Rebuild APK from folder"
echo "  jadx <apk>          - Decompile APK to Java source"
echo "  baksmali d <dex>    - Disassemble dex to smali"
echo "  smali a <folder>    - Assemble smali to dex"
echo ""
echo "Self-modification workflow:"
echo "  1. apktool d MobileCLI.apk"
echo "  2. Edit smali files or resources"
echo "  3. apktool b MobileCLI/"
echo "  4. zipalign -v 4 MobileCLI/dist/*.apk aligned.apk"
echo "  5. apksigner sign --ks ~/.android/debug.keystore aligned.apk"
""")
        apkToolsScript.setExecutable(true, false)

        Log.i(TAG, "Installed Termux API scripts (60+ commands) + utility scripts + APK tools installer")
    }
}

