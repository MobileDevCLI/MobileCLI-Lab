package com.mobilecli.engine.packages

import android.util.Log
import com.mobilecli.engine.terminal.TerminalBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Default implementation of PackageManager using pkg/apt commands
 */
class PackageManagerImpl(
    private val terminal: TerminalBridge
) : PackageManager {

    companion object {
        private const val TAG = "PackageManager"
    }

    override suspend fun update(): Boolean {
        return withContext(Dispatchers.IO) {
            val result = terminal.execute("pkg update -y", timeout = 120000)
            if (!result.success) {
                Log.e(TAG, "Package update failed: ${result.stderr}")
            }
            result.success
        }
    }

    override suspend fun upgrade(): Boolean {
        return withContext(Dispatchers.IO) {
            val result = terminal.execute("pkg upgrade -y", timeout = 300000)
            if (!result.success) {
                Log.e(TAG, "Package upgrade failed: ${result.stderr}")
            }
            result.success
        }
    }

    override suspend fun install(packageName: String, onProgress: ((String) -> Unit)?): InstallResult {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()

            try {
                val output = StringBuilder()

                if (onProgress != null) {
                    // Streaming installation
                    val job = terminal.executeStreaming("pkg install -y $packageName") { line ->
                        output.appendLine(line)
                        onProgress(line)
                    }
                    job.join()
                } else {
                    // Simple installation
                    val result = terminal.execute("pkg install -y $packageName", timeout = 300000)
                    output.append(result.output)
                }

                val duration = System.currentTimeMillis() - startTime
                val success = isInstalled(packageName)

                InstallResult(
                    success = success,
                    packageName = packageName,
                    output = output.toString(),
                    durationMs = duration,
                    error = if (!success) "Installation may have failed" else null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Install failed for $packageName", e)
                InstallResult(
                    success = false,
                    packageName = packageName,
                    error = e.message,
                    durationMs = System.currentTimeMillis() - startTime
                )
            }
        }
    }

    override suspend fun installAll(packages: List<String>, onProgress: ((String) -> Unit)?): Map<String, InstallResult> {
        val results = mutableMapOf<String, InstallResult>()

        for (pkg in packages) {
            onProgress?.invoke("Installing $pkg...")
            results[pkg] = install(pkg, onProgress)
        }

        return results
    }

    override suspend fun uninstall(packageName: String): Boolean {
        return withContext(Dispatchers.IO) {
            val result = terminal.execute("pkg uninstall -y $packageName", timeout = 60000)
            result.success
        }
    }

    override suspend fun isInstalled(packageName: String): Boolean {
        return withContext(Dispatchers.IO) {
            val result = terminal.execute("dpkg -s $packageName 2>/dev/null | grep -q 'Status: install ok installed'")
            result.success
        }
    }

    override suspend fun search(query: String): List<PackageInfo> {
        return withContext(Dispatchers.IO) {
            val result = terminal.execute("pkg search $query")
            if (!result.success) return@withContext emptyList()

            parseSearchResults(result.stdout)
        }
    }

    override suspend fun listInstalled(): List<PackageInfo> {
        return withContext(Dispatchers.IO) {
            val result = terminal.execute("dpkg --list | grep '^ii' | awk '{print \$2, \$3}'")
            if (!result.success) return@withContext emptyList()

            result.stdout.lines()
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    val parts = line.split(" ", limit = 2)
                    if (parts.size >= 2) {
                        PackageInfo(
                            name = parts[0],
                            version = parts[1],
                            description = "",
                            installed = true
                        )
                    } else null
                }
        }
    }

    override suspend fun getInfo(packageName: String): PackageInfo? {
        return withContext(Dispatchers.IO) {
            val result = terminal.execute("apt show $packageName 2>/dev/null")
            if (!result.success) return@withContext null

            parsePackageInfo(result.stdout, packageName)
        }
    }

    override suspend fun clean(): Boolean {
        return withContext(Dispatchers.IO) {
            val result = terminal.execute("pkg clean")
            result.success
        }
    }

    override suspend fun repair(): Boolean {
        return withContext(Dispatchers.IO) {
            val result = terminal.execute("dpkg --configure -a && apt --fix-broken install -y")
            result.success
        }
    }

    private fun parseSearchResults(output: String): List<PackageInfo> {
        return output.lines()
            .filter { it.isNotBlank() && !it.startsWith("Sorting") && !it.startsWith("Full Text") }
            .mapNotNull { line ->
                // Format: "package/stable version arch"
                // or "package - description"
                val slashIndex = line.indexOf('/')
                val dashIndex = line.indexOf(" - ")

                when {
                    slashIndex > 0 -> {
                        val name = line.substring(0, slashIndex)
                        val rest = line.substring(slashIndex + 1)
                        val version = rest.split(" ").firstOrNull() ?: ""
                        PackageInfo(
                            name = name,
                            version = version,
                            description = "",
                            installed = false
                        )
                    }
                    dashIndex > 0 -> {
                        val name = line.substring(0, dashIndex).trim()
                        val description = line.substring(dashIndex + 3).trim()
                        PackageInfo(
                            name = name,
                            version = "",
                            description = description,
                            installed = false
                        )
                    }
                    else -> null
                }
            }
    }

    private fun parsePackageInfo(output: String, packageName: String): PackageInfo? {
        val lines = output.lines()
        var version = ""
        var description = ""
        var size = 0L
        var installed = false

        for (line in lines) {
            when {
                line.startsWith("Version:") -> version = line.substringAfter(":").trim()
                line.startsWith("Description:") -> description = line.substringAfter(":").trim()
                line.startsWith("Installed-Size:") -> {
                    val sizeStr = line.substringAfter(":").trim()
                    size = sizeStr.replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0
                    size *= 1024 // Convert KB to bytes
                }
                line.startsWith("Status:") && line.contains("installed") -> installed = true
            }
        }

        return if (version.isNotEmpty()) {
            PackageInfo(
                name = packageName,
                version = version,
                description = description,
                installed = installed,
                size = size
            )
        } else null
    }
}
