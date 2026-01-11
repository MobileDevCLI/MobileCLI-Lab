# MobileCLI v93 Handoff Document
**Created:** January 9, 2026
**Purpose:** Context for Claude to resume after app reinstall

---

## WHAT WAS DONE (Session Summary)

### 1. Keyboard Overlay Fix
- **Problem:** Extra keys (ESC, CTRL, TAB, arrows) hidden BEHIND keyboard
- **Root Cause:** Running APK (v66) was MISSING `windowSoftInputMode="adjustResize"`
- **Fix:** v93 build from MobileCLI-v2 source has this setting on all 8 activities
- **Status:** FIXED in v93

### 2. IP Hiding During Bootstrap
- **Problem:** Users could see proprietary terminal code flashing during startup
- **Fix:**
  - `activity_main.xml`: setup_overlay starts `visibility="visible"`
  - `activity_main.xml`: terminal_view starts `visibility="gone"`
  - `MainActivity.kt`: Added dev mode check to show terminal immediately for developers
- **Status:** FIXED in v93

### 3. Version Bump
- Old: 1.7.1 (v66) / 3.8.0-ditto (v90)
- New: 3.9.0 (v93)

---

## APK LOCATIONS

```
/sdcard/Download/MobileCLI-v93-dev.apk   - Developer variant (7.1 MB)
/sdcard/Download/MobileCLI-v93-user.apk  - User variant (7.1 MB)
```

**User is installing:** `MobileCLI-v93-user.apk` (clean UX, IP hidden)

---

## WHAT TO TEST AFTER INSTALL

1. **Keyboard Test:** Open keyboard, verify ESC/CTRL/TAB/arrows are ABOVE keyboard, not behind
2. **IP Hiding Test:** Cold start app, verify you see loading overlay (not terminal code)
3. **Downloads Test:** Check `/sdcard/Download/` access works
4. **Termux APIs:** Test `termux-toast "hello"`, `termux-vibrate`, etc.
5. **Claude Code:** Run `claude` and verify it works

---

## GITHUB STATE

Repository: https://github.com/MobileDevCLI/MobileCLI-v2

Latest commits:
```
f69c5cd docs: Update CLAUDE.md with v93 changes
0b58560 v93: Hide IP during bootstrap - setup overlay visible by default
b274ebc Fix XML parsing error in view_api_request.xml
```

All changes pushed. Source is in sync with built APKs.

---

## KEY FILES MODIFIED

| File | Changes |
|------|---------|
| `app/src/main/res/layout/activity_main.xml` | setup_overlay visible, terminal_view gone |
| `app/src/main/java/com/termux/MainActivity.kt` | Dev mode overlay handling, version bump |
| `app/src/main/res/layout/view_api_request.xml` | Fixed XML parsing error (hint attribute) |
| `CLAUDE.md` | Added v93 documentation |

---

## BUILD COMMANDS (If Rebuild Needed)

```bash
export JAVA_HOME=/data/data/com.termux/files/usr/lib/jvm/java-17-openjdk
export ANDROID_HOME=/data/user/0/com.termux/files/home/android-sdk
export GRADLE_OPTS="-Djava.net.preferIPv4Stack=true"

cd ~/MobileCLI-v2
./gradlew assembleDebug

# Copy to Downloads
cp app/build/outputs/apk/dev/debug/app-dev-debug.apk /sdcard/Download/MobileCLI-v93-dev.apk
cp app/build/outputs/apk/user/debug/app-user-debug.apk /sdcard/Download/MobileCLI-v93-user.apk
```

---

## USER CONTEXT

- User's dev phone and test phone are the SAME device
- Termux crashed after 9 days, now running inside MobileCLI
- GitHub token: `[REDACTED_TOKEN]`
- Goal: Professional, sellable app (no Termux branding, clean UX)
- User wants app to work like Claude/ChatGPT Android apps

---

## NEXT STEPS AFTER USER RETURNS

1. Verify keyboard fix works
2. Verify IP hiding works
3. If issues found, debug and rebuild
4. If all good, consider release build (signed APK)

---

## KNOWN WORKAROUNDS

### aapt2 ARM Issue
Add to `gradle.properties`:
```properties
android.aapt2FromMavenOverride=/data/data/com.termux/files/home/android-sdk/build-tools/34.0.0/aapt2
```

### Java Version
Use Java 17 for building, target Java 11 in build.gradle.

---

**When user returns:** Read this file first, then ask what they want to test/fix.
