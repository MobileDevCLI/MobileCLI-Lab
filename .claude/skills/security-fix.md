---
name: security-fix
description: Fix security vulnerabilities in MobileCLI codebase
---

# Security Fix Skill

Use this skill when addressing security issues in MobileCLI.

## Critical Issues to Fix

### 1. Remove Hardcoded Supabase Key
**File:** `LicenseManager.kt` lines 32-33

**Current (INSECURE):**
```kotlin
private const val SUPABASE_ANON_KEY = "eyJhbG..."
```

**Fix:** Move to BuildConfig or secure storage
```kotlin
private val SUPABASE_ANON_KEY: String
    get() = BuildConfig.SUPABASE_KEY
```

### 2. Remove Signing Credentials
**File:** `build.gradle.kts` lines 27-32

**Current (INSECURE):**
```kotlin
storePassword = "mobilecli2026"
keyPassword = "mobilecli2026"
```

**Fix:** Use environment variables
```kotlin
storePassword = System.getenv("MOBILECLI_STORE_PASSWORD") ?: ""
keyPassword = System.getenv("MOBILECLI_KEY_PASSWORD") ?: ""
```

### 3. Disable WebView File Access
**File:** `MainActivity.kt` line 1692-1693

**Current (INSECURE):**
```kotlin
settings.allowFileAccess = true
settings.allowContentAccess = true
```

**Fix:**
```kotlin
settings.allowFileAccess = false
settings.allowContentAccess = false
```

### 4. Sanitize Command Input
**File:** `MobileAppsRoomActivity.kt` line 480

**Add validation before exec:**
```kotlin
private fun sanitizeCommand(cmd: String): String {
    // Remove dangerous characters
    return cmd.replace(Regex("[;&|`\$]"), "")
}
```

### 5. Remove JavaScript Injection
**File:** `MainActivity.kt` lines 2010-2020

**Either remove entirely or add strict validation:**
```kotlin
private fun executeInjectJs(args: String): String {
    // DISABLED for security
    return "ERROR: JavaScript injection disabled for security"
}
```

## Files to Remove/Redact

- `HANDOFF.md` - Contains GitHub token (line 97)
- Remove or regenerate `mobilecli-release.keystore`

## Verification

After fixes, run security scan:
```bash
grep -r "ghp_" ~/MobileCLI-v2-fix/
grep -r "eyJhbG" ~/MobileCLI-v2-fix/
grep -r "mobilecli2026" ~/MobileCLI-v2-fix/
```

All should return empty.
