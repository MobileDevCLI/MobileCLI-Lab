package com.mobilecli.engine.packages

/**
 * Interface for Termux package management
 *
 * This module wraps the pkg/apt commands for easy package installation.
 *
 * Example usage:
 * ```kotlin
 * val engine = MobileCLIEngine.getInstance()
 * val packages = engine.getPackageManager()
 *
 * // Install a package
 * packages.install("python")
 *
 * // Check if installed
 * if (packages.isInstalled("nodejs")) {
 *     println("Node.js is ready!")
 * }
 *
 * // Search packages
 * val results = packages.search("python")
 * ```
 */
interface PackageManager {
    /**
     * Update package lists
     * Equivalent to: pkg update
     * @return true if successful
     */
    suspend fun update(): Boolean

    /**
     * Upgrade all installed packages
     * Equivalent to: pkg upgrade -y
     * @return true if successful
     */
    suspend fun upgrade(): Boolean

    /**
     * Install a package
     * Equivalent to: pkg install -y <package>
     * @param packageName Name of the package
     * @param onProgress Callback for installation progress
     * @return InstallResult with success status
     */
    suspend fun install(packageName: String, onProgress: ((String) -> Unit)? = null): InstallResult

    /**
     * Install multiple packages
     * @param packages List of package names
     * @param onProgress Callback for progress
     * @return Map of package name to InstallResult
     */
    suspend fun installAll(packages: List<String>, onProgress: ((String) -> Unit)? = null): Map<String, InstallResult>

    /**
     * Uninstall a package
     * Equivalent to: pkg uninstall -y <package>
     * @param packageName Name of the package
     * @return true if successful
     */
    suspend fun uninstall(packageName: String): Boolean

    /**
     * Check if a package is installed
     * @param packageName Name of the package
     * @return true if installed
     */
    suspend fun isInstalled(packageName: String): Boolean

    /**
     * Search for packages
     * Equivalent to: pkg search <query>
     * @param query Search query
     * @return List of matching packages
     */
    suspend fun search(query: String): List<PackageInfo>

    /**
     * Get list of installed packages
     * @return List of installed packages
     */
    suspend fun listInstalled(): List<PackageInfo>

    /**
     * Get package information
     * @param packageName Name of the package
     * @return PackageInfo or null if not found
     */
    suspend fun getInfo(packageName: String): PackageInfo?

    /**
     * Clean package cache
     * Equivalent to: pkg clean
     * @return true if successful
     */
    suspend fun clean(): Boolean

    /**
     * Fix broken packages
     * Equivalent to: dpkg --configure -a
     * @return true if successful
     */
    suspend fun repair(): Boolean
}

/**
 * Result of a package installation
 */
data class InstallResult(
    /**
     * Whether installation succeeded
     */
    val success: Boolean,

    /**
     * Package that was installed
     */
    val packageName: String,

    /**
     * Error message if failed
     */
    val error: String? = null,

    /**
     * Installation output/logs
     */
    val output: String = "",

    /**
     * Time taken in milliseconds
     */
    val durationMs: Long = 0
)

/**
 * Information about a package
 */
data class PackageInfo(
    /**
     * Package name
     */
    val name: String,

    /**
     * Package version
     */
    val version: String,

    /**
     * Package description
     */
    val description: String,

    /**
     * Whether package is installed
     */
    val installed: Boolean,

    /**
     * Installed size in bytes
     */
    val size: Long = 0
)
