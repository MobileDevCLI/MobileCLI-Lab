# MobileCLI v1.6.2 - Overlay Fix Release

**Release Date:** January 10, 2026
**Version Code:** 94
**APK:** `MobileCLI-v1.6.2-overlay-fix.apk`

---

## Summary

This release fixes the timing of the setup overlay to properly hide proprietary intellectual property from end users during the AI launch sequence.

---

## Problem

In previous versions (v1.6.1 and earlier), the overlay was hidden BEFORE the AI command (Claude/Gemini/Codex) was launched. This caused a brief moment where users could see the terminal output, potentially exposing:
- Bootstrap commands
- Installation scripts
- Environment setup
- Other proprietary code

---

## Solution

The overlay timing was corrected:

1. **Clear terminal** (overlay still visible)
2. **Launch AI command** (overlay still visible)
3. **Wait 3 seconds** for AI to start rendering
4. **Hide overlay** - user now sees Claude's welcome UI directly

---

## Code Changes

### MainActivity.kt (lines 658-674)

**Before:**
```kotlin
hideSetupOverlay()
delay(100)
if (launchCmd.isNotEmpty()) {
    val cmd = "$launchCmd\n"
    session?.write(cmd.toByteArray(Charsets.UTF_8), 0, cmd.length)
}
```

**After:**
```kotlin
// Launch the AI command FIRST (while overlay still visible)
if (launchCmd.isNotEmpty()) {
    val cmd = "$launchCmd\n"
    session?.write(cmd.toByteArray(Charsets.UTF_8), 0, cmd.length)
}

// Wait for AI to start rendering its UI (overlay still covers terminal)
delay(3000)

// NOW hide overlay - user sees Claude's UI, not terminal output
hideSetupOverlay()
```

---

## Testing

1. Install the APK
2. Launch MobileCLI
3. Select Claude (or other AI)
4. Observe that the loading overlay stays visible until Claude's welcome message appears
5. Verify no terminal output is visible before Claude's UI

---

## Files Changed

| File | Changes |
|------|---------|
| `app/build.gradle.kts` | Version updated to 1.6.2-overlay-fix (code 94) |
| `app/src/main/java/com/termux/MainActivity.kt` | Overlay timing fix |

---

## GitHub

- **Repository:** https://github.com/MobileDevCLI/MobileCLI-v2
- **Commit:** `bc9bd58` (v1.6.2: Fix overlay timing to hide proprietary IP)

---

## APK Location

```
/sdcard/Download/MobileCLI-v1.6.2-overlay-fix.apk
```

Size: ~5.9 MB
