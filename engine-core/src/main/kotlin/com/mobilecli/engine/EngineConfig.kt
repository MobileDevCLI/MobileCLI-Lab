package com.mobilecli.engine

/**
 * Configuration for MobileCLI Engine
 *
 * Example usage:
 * ```kotlin
 * val config = EngineConfig(
 *     aiProvider = AIProvider.CLAUDE,
 *     aiApiKey = "sk-ant-...",
 *     bootstrapPackages = listOf("python", "nodejs", "git")
 * )
 * MobileCLIEngine.initialize(context, config)
 * ```
 */
data class EngineConfig(
    /**
     * AI provider to use (CLAUDE, OPENAI, LOCAL, NONE)
     */
    val aiProvider: AIProvider = AIProvider.NONE,

    /**
     * API key for the AI provider
     */
    val aiApiKey: String = "",

    /**
     * Enable self-modification capabilities (requires engine-selfmod module)
     */
    val enableSelfModification: Boolean = false,

    /**
     * Packages to install during bootstrap
     */
    val bootstrapPackages: List<String> = emptyList(),

    /**
     * Enable debug logging
     */
    val debugMode: Boolean = false,

    /**
     * Custom shell path (defaults to bash)
     */
    val shellPath: String = "",

    /**
     * Terminal color scheme
     */
    val colorScheme: ColorScheme = ColorScheme.DARK,

    /**
     * Initial terminal columns (0 = auto-detect)
     */
    val terminalColumns: Int = 0,

    /**
     * Initial terminal rows (0 = auto-detect)
     */
    val terminalRows: Int = 0,

    /**
     * Enable wake lock for background operation
     */
    val enableWakeLock: Boolean = false,

    /**
     * Custom environment variables to add
     */
    val extraEnvironment: Map<String, String> = emptyMap()
)

/**
 * Supported AI providers
 */
enum class AIProvider {
    NONE,       // No AI integration
    CLAUDE,     // Anthropic Claude
    OPENAI,     // OpenAI GPT
    LOCAL       // Local LLM (future)
}

/**
 * Terminal color schemes
 */
enum class ColorScheme {
    DARK,       // Dark background, light text
    LIGHT,      // Light background, dark text
    MONOKAI,    // Monokai theme
    SOLARIZED   // Solarized theme
}
