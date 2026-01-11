# MobileCLI Progress Tracking

> This file persists progress across Claude sessions. Read this FIRST when starting a new session.

---

## Current Version: 1.6.3-welcome-fix (v95)

**Last Updated:** 2026-01-10 16:15
**Last Session:** IP protection analysis - Claude Code vs MobileCLI

---

## Recently Completed

### 2026-01-10: IP Protection Analysis
- [x] Extracted and analyzed Claude Code APK (`com.anthropic.claude`)
- [x] Compared protection methods between native app vs terminal-based
- [x] Documented findings in `progress/ip-protection-analysis.md`
- [x] Identified BUG-010: AI installation commands visible in terminal
- [x] Proposed 4 fix options (overlay, silent, native, background service)
- [x] Recommended implementation path

**Key Finding:** Claude Code uses native Kotlin compiled to DEX bytecode with no terminal exposure. MobileCLI needs overlay during `installAI()` to hide proprietary commands.

### 2026-01-10: Welcome Screen Fix (v1.6.3)
- [x] Identified terminal flash bug at 100% completion
- [x] Added `welcome_overlay` to activity_main.xml (lines 723-800)
- [x] Added `welcomeOverlay` variable to MainActivity.kt (line 203)
- [x] Modified `hideSetupOverlay()` to show welcome overlay first
- [x] Added `hideWelcomeOverlay()` with fade animation
- [x] Built APK: `/sdcard/Download/MobileCLI-v1.6.3-welcome-fix.apk`

### 2026-01-10: Comprehensive Security Audit
- [x] Scanned for personal data leaks (CRITICAL issues found)
- [x] Scanned for hardcoded secrets (CRITICAL issues found)
- [x] Scanned for security vulnerabilities (15 issues found)
- [x] Reviewed legal compliance (Privacy Policy MISSING)
- [x] Created audit report (see `progress/security-audit.md`)

---

## In Progress

### Security Remediation (URGENT)
- [ ] Revoke GitHub token: `[REDACTED_TOKEN]`
- [ ] Rotate Supabase API key in LicenseManager.kt
- [ ] Move signing credentials out of build.gradle.kts
- [ ] Create Privacy Policy
- [ ] Create Terms of Service

---

## Backlog

### IP Protection (Recommended)
- [ ] Add installation overlay to `installAI()` function in MainActivity.kt
- [ ] Redirect command output to log file during installation
- [ ] Show progress UI instead of terminal during AI installation
- [ ] Test all installation paths (Claude, Gemini, Codex, All)

### Security Fixes (High Priority)
- [ ] Disable WebView file access (allowFileAccess = false)
- [ ] Sanitize command inputs in MobileAppsRoomActivity
- [ ] Remove/restrict JavaScript injection via INJECT_JS
- [ ] Review all 10+ exported components in manifest

### Legal Requirements
- [ ] Privacy Policy document
- [ ] Terms of Service document
- [ ] In-app legal screen
- [ ] GDPR/CCPA compliance documentation
- [ ] Google Play Data Safety form preparation

### Features
- [ ] Test welcome screen fix on fresh install
- [ ] Verify timing works for slow devices

---

## Key Files

| File | Purpose |
|------|---------|
| `MainActivity.kt` | Main activity, overlay logic, welcome screen |
| `BootstrapInstaller.kt` | Bootstrap download, API scripts |
| `LicenseManager.kt` | Contains Supabase credentials (NEEDS FIX) |
| `build.gradle.kts` | Contains signing passwords (NEEDS FIX) |
| `activity_main.xml` | Layout with welcome_overlay |
| `HANDOFF.md` | Contains exposed GitHub token (NEEDS FIX) |
| `progress/ip-protection-analysis.md` | Claude Code vs MobileCLI comparison |

---

## Session Handoff Notes

When starting a new session, Claude should:
1. Read this file first
2. Read `progress/bugs-and-issues.md` for known issues
3. Read `progress/security-audit.md` for security context
4. Check `.mobilecli/memory/` for persistent knowledge
