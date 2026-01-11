# MobileCLI Security Audit Report

> Comprehensive security audit performed 2026-01-10
> This file persists findings for future Claude sessions

---

## Executive Summary

| Category | Issues Found | Critical | High | Medium | Low |
|----------|--------------|----------|------|--------|-----|
| Personal Data Leaks | 6 | 1 | 2 | 2 | 1 |
| Hardcoded Secrets | 4 | 2 | 1 | 1 | 0 |
| Security Vulnerabilities | 15 | 5 | 4 | 4 | 2 |
| Legal Compliance | 8 missing | - | - | - | - |
| **TOTAL** | **33** | **8** | **7** | **7** | **3** |

---

## 1. Personal Data Leaks

### CRITICAL: GitHub Token Exposed
- **File:** `HANDOFF.md:97`
- **Value:** `[REDACTED_TOKEN]`
- **Status:** ACTIVE - MUST REVOKE

### HIGH: Email Addresses
- `ai@mobilecli.com` in BootstrapInstaller.kt:2114
- `mobiledevcli@gmail.com` in ThirdPartyLicenses.kt:150

### MEDIUM: Owner Identifier
- "Samblamz" found in 15+ documentation files
- Associated with IP ownership claims

### MEDIUM: Device Information
- Samsung SM-G988U (S20 Ultra) mentioned in docs

### LOW: Private Network IP
- `192.168.1.100` in AI_INTEGRATION_EXPANSION_PLAN.md

---

## 2. Hardcoded Secrets

### CRITICAL: Supabase JWT Token
```kotlin
// LicenseManager.kt:32-33
private const val API_BASE = "https://mwxlguqukyfberyhtkmg.supabase.co/rest/v1"
private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIs..."
```
**Risk:** Full database access for anyone with source code
**Fix:** Move to secure storage or environment variables

### CRITICAL: Signing Keystore Credentials
```kotlin
// build.gradle.kts:27-32
storePassword = "mobilecli2026"
keyAlias = "mobilecli"
keyPassword = "mobilecli2026"
```
**Risk:** APK identity theft, malware signing
**Fix:** Use environment variables, never commit credentials

### HIGH: Keystore File in Repo
- File: `mobilecli-release.keystore`
- Combined with exposed passwords = complete compromise

---

## 3. Security Vulnerabilities

### CRITICAL (5)

1. **Command Injection** - MobileAppsRoomActivity.kt:480
   - `Runtime.exec()` with unsanitized user input

2. **Unsafe WebView** - MainActivity.kt:1690-1699
   - `allowFileAccess=true` + JavaScript enabled

3. **JS Injection** - MainActivity.kt:2010-2020
   - `evaluateJavascript()` with arbitrary input

4. **World-Readable Files** - BootstrapInstaller.kt:331
   - `setReadable(true, false)` makes files world-readable

5. **Hardcoded Signing** - build.gradle.kts:28-31
   - Credentials in source control

### HIGH (4)

1. **10+ Exported Components** - AndroidManifest.xml
   - Many activities/services exported without protection

2. **JavaScript Bridge** - MainActivity.kt:1721
   - `DittoJsBridge` allows JS to send terminal commands

3. **Unsafe Gradle Execution** - MobileAppsRoomActivity.kt:770
   - Build commands from user input

4. **File WebView Loading** - MainActivity.kt:1945
   - `file://` URLs with file access enabled

### MEDIUM (4)

1. **chmod 755 Permissions** - Multiple locations
2. **Local File Disclosure** - WebView file:// loading
3. **Missing Input Validation** - executeUiCommand()
4. **Dynamic JS Interface** - DittoJsBridge exposure

### LOW (2)

1. **Sensitive Data Logging** - Potential token logging
2. **Reflection Usage** - Private field access

---

## 4. Legal Compliance

### MISSING (Required for App Store)

| Document | Status | Priority |
|----------|--------|----------|
| Privacy Policy | MISSING | CRITICAL |
| Terms of Service | MISSING | CRITICAL |
| EULA | MISSING | HIGH |
| In-App Legal Screen | MISSING | HIGH |
| GDPR Compliance | MISSING | HIGH |
| CCPA Compliance | MISSING | HIGH |
| Data Safety Labels | MISSING | MEDIUM |
| Accessibility Statement | MISSING | MEDIUM |

### PRESENT (Good)

- Third-party license attribution (ThirdPartyLicenses.kt)
- GPL source code links for termux-am
- Intellectual property register
- Permissions documented in manifest

---

## 5. Remediation Priority

### TODAY (Immediate)
1. Revoke GitHub token
2. Rotate Supabase key
3. Generate new signing keystore
4. Remove secrets from source

### THIS WEEK
5. Create Privacy Policy
6. Create Terms of Service
7. Disable WebView file access
8. Sanitize command inputs

### BEFORE RELEASE
9. Add in-app legal screen
10. Complete Google Play Data Safety
11. Review exported components
12. Security audit of JS bridge

---

## 6. Comparison to Claude Code App

| Security Feature | Claude Code | MobileCLI |
|------------------|-------------|-----------|
| Secrets in env vars | Yes | No (hardcoded) |
| Privacy Policy | Yes | Missing |
| Terms of Service | Yes | Missing |
| WebView restrictions | Strict | Permissive |
| Command sanitization | Yes | Missing |
| GDPR compliance | Yes | Missing |
| Signed with protected key | Yes | Exposed |

---

## Audit Metadata

- **Auditor:** Claude Code (Opus 4.5)
- **Date:** 2026-01-10
- **Duration:** ~30 minutes
- **Files Scanned:** All .kt, .java, .xml, .gradle, .md, .json
- **Methods:** Static analysis, pattern matching, manual review
