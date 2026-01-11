# IP Protection Analysis: Claude Code vs MobileCLI

> Analysis of how Anthropic's Claude Code app protects its source code compared to MobileCLI
> **Date:** 2026-01-10

---

## Executive Summary

Claude Code (Anthropic) uses a fundamentally different architecture than MobileCLI that makes IP protection inherent. MobileCLI's terminal-based approach requires additional measures to hide proprietary implementation details.

---

## Claude Code App Analysis

### APK Location
```
/data/app/~~Eew2xC1CL_x56GONWZVnUg==/com.anthropic.claude-EuJdrXdw7foBsw44oJ4dRA==/base.apk
```

### Architecture
| Component | Description |
|-----------|-------------|
| Size | 21MB (base APK) |
| Code | ~16.5MB compiled DEX bytecode |
| UI | Native Jetpack Compose |
| Language | Kotlin/Java |
| Shell Scripts | **NONE** |
| Visible Terminal | **NONE** |

### How Claude Code Protects IP

1. **Native Android App** - All logic compiled to DEX bytecode
   - Not human-readable without decompilation tools
   - Proguard/R8 obfuscation likely applied
   - No plain-text source code in APK

2. **No Terminal Exposure**
   - Users interact with native UI only
   - No shell commands visible to users
   - All API calls happen internally

3. **Self-Contained**
   - Assets bundled in APK (fonts, resources)
   - No external script downloads visible
   - No package manager commands shown

4. **Bootstrap Hidden**
   - Uses internal "BootstrapConfig" classes
   - Bootstrap is native code, not shell scripts
   - Login/auth handled through native UI

### Decompilation Resistance
While DEX can be decompiled (with tools like jadx), it results in:
- Obfuscated class/method names (a, b, c, etc.)
- Complex control flow
- Requires significant effort to understand
- Most users won't attempt this

---

## MobileCLI Current State

### Architecture
| Component | Description |
|-----------|-------------|
| Base | Terminal emulator (Termux fork) |
| UI | Terminal + WebView overlays |
| Installation | Shell commands visible to user |
| Scripts | 50+ shell scripts in plain text |

### IP Exposure Points

#### 1. Initial Bootstrap (PARTIALLY FIXED)
**File:** `BootstrapInstaller.kt`
- Downloads from known URLs (exposed)
- Extracts packages (hidden by overlay now)
- Runs scripts (hidden by overlay now)
- **Fix Status:** v1.6.3 added welcome_overlay

#### 2. AI Installation from Settings (EXPOSED)
**File:** `MainActivity.kt` lines 2662-2710
```kotlin
// User sees this in terminal:
"pkg update -y && pkg upgrade -y && pkg install nodejs-lts -y && npm install -g @anthropic-ai/claude-code"
```
- Full command visible
- Package URLs visible
- Download progress visible
- **Fix Status:** NOT FIXED

#### 3. Dev Tools Installation (EXPOSED)
**File:** `BootstrapInstaller.kt` line 1454-1455
```kotlin
pkg update -y
pkg install -y git openjdk-17 gradle aapt aapt2 apksigner d8 dx coreutils zip unzip
```
- All tools listed in plain text
- Shows exact tech stack

#### 4. Shell Scripts (EXPOSED)
- All 50+ termux-* scripts readable
- All mobilecli-* scripts readable
- Users can `cat` any script to see source

---

## Recommendations

### Option 1: Installation Overlay (Quick Fix)
**Effort:** Low | **Protection:** Medium

Show a full-screen overlay during AI installation (like initial bootstrap):

```kotlin
private fun installAI(tool: String) {
    // Show installation overlay
    showInstallationOverlay()

    // Run command with suppressed output
    val silentCmd = "$autoCmd > /tmp/install.log 2>&1"
    session?.write(silentCmd.toByteArray())

    // Monitor completion and hide overlay
    monitorInstallation { hideInstallationOverlay() }
}
```

**Pros:**
- Minimal code changes
- Consistent with existing overlay system
- Can show friendly progress UI

**Cons:**
- Users can still access terminal later
- Scripts still readable if user knows where to look

### Option 2: Silent Installation (Medium Fix)
**Effort:** Medium | **Protection:** Medium-High

Redirect all output to log file, show only progress indicator:

```kotlin
val silentCmd = """
    {
        pkg update -y &&
        pkg install -y nodejs-lts &&
        npm install -g @anthropic-ai/claude-code
    } > ~/.mobilecli/install.log 2>&1 &

    # Show spinner while installing
    while pgrep -f "npm install" > /dev/null; do
        printf '\rInstalling... '
        sleep 1
    done
    echo 'Done!'
"""
```

**Pros:**
- Commands not visible in terminal
- User sees friendly "Installing..." message
- Log available for debugging

**Cons:**
- Commands still in APK source code
- Determined user could find log file

### Option 3: Native Installation (Best Protection)
**Effort:** High | **Protection:** High

Move installation logic to native Kotlin code:

```kotlin
class AIInstaller(private val context: Context) {
    private val packageManager = TermuxPackageManager(context)
    private val npmManager = NpmManager(context)

    suspend fun installClaude(): Result<Unit> = withContext(Dispatchers.IO) {
        packageManager.update()
        packageManager.install("nodejs-lts")
        npmManager.installGlobal("@anthropic-ai/claude-code")
        Result.success(Unit)
    }
}
```

**Pros:**
- No visible shell commands
- Logic compiled into DEX (obfuscated)
- Professional architecture

**Cons:**
- Significant development effort
- Need to implement package management in Kotlin
- Testing complexity

### Option 4: Background Service (Best UX)
**Effort:** High | **Protection:** High

Run installation in a background service with notification progress:

```kotlin
class InstallationService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createProgressNotification()
        startForeground(INSTALL_NOTIFICATION_ID, notification)

        CoroutineScope(Dispatchers.IO).launch {
            // Run installation silently
            executeInstallation()

            // Show completion
            showCompletionNotification()
            stopSelf()
        }

        return START_NOT_STICKY
    }
}
```

**Pros:**
- Best user experience
- Installation continues if app backgrounded
- No terminal visible at all

**Cons:**
- Complex implementation
- Need to handle service lifecycle
- Requires additional permissions

---

## Comparison Matrix

| Aspect | Claude Code | MobileCLI Current | MobileCLI Recommended |
|--------|-------------|-------------------|----------------------|
| Installation Visible | No | Yes | No (with overlay) |
| Commands in Terminal | No | Yes | Redirected to log |
| Source Code Readable | Compiled DEX | Plain Kotlin | Compiled DEX |
| Scripts Readable | N/A | Yes (50+ scripts) | Consider obfuscation |
| Decompilation Effort | High | Low | Medium-High |
| User Can Copy Method | No | Yes | No (with fix) |

---

## Immediate Action Items

### Quick Wins (This Week)
1. **Add installation overlay** to `installAI()` function
2. **Redirect command output** to log file
3. **Show progress UI** during installation

### Medium-Term (This Month)
4. **Create InstallationOverlay** component matching setup_overlay style
5. **Add progress callbacks** to monitor installation
6. **Test all AI installation paths**

### Long-Term (Future)
7. **Consider native installation** for maximum protection
8. **Evaluate background service** approach
9. **Document proprietary patterns** in internal-only files

---

## Conclusion

Claude Code's IP protection is architectural - it's a native app with no terminal exposure. MobileCLI is inherently a terminal app, so perfect protection is impossible. However, with the overlay approach (Option 1 + 2), you can achieve "good enough" protection where:

- 99% of users won't see installation details
- Terminal is hidden during sensitive operations
- Only power users who specifically hunt for info can find it
- And power users can always decompile DEX anyway

**Recommendation:** Implement Option 1 (Installation Overlay) immediately, with Option 2 (Silent Installation) as enhancement.

---

**Analysis by:** Claude Code AI
**Date:** 2026-01-10
