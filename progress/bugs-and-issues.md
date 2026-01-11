# MobileCLI Bugs and Issues

> Persistent bug tracking across Claude sessions

---

## CRITICAL - Security Issues

### BUG-001: GitHub Token Exposed in Source
**Status:** OPEN - CRITICAL
**File:** `HANDOFF.md` line 97
**Token:** `[REDACTED_TOKEN]`
**Action Required:** Revoke at github.com/settings/tokens IMMEDIATELY
**Impact:** Anyone with source code can access owner's GitHub repos

### BUG-002: Supabase API Key Hardcoded
**Status:** OPEN - CRITICAL
**File:** `LicenseManager.kt` lines 32-33
**Details:** Full Supabase anon key exposed in source code
**Action Required:** Rotate key in Supabase dashboard, move to secure storage
**Impact:** Database access compromised

### BUG-003: Signing Credentials in Source
**Status:** OPEN - CRITICAL
**File:** `build.gradle.kts` lines 27-32
**Details:** Keystore passwords hardcoded: `mobilecli2026`
**Action Required:** Generate new keystore, use environment variables
**Impact:** APK signing identity can be stolen

---

## HIGH - Security Vulnerabilities

### BUG-004: Command Injection Risk
**Status:** OPEN - HIGH
**File:** `MobileAppsRoomActivity.kt` line 480
**Details:** User input passed directly to `Runtime.exec()` without sanitization
**Fix:** Validate/sanitize all command inputs before execution

### BUG-005: Unsafe WebView Configuration
**Status:** OPEN - HIGH
**File:** `MainActivity.kt` lines 1690-1699
**Details:** `allowFileAccess=true` + `allowContentAccess=true` + JavaScript enabled
**Fix:** Set `allowFileAccess=false`, restrict JavaScript bridge

### BUG-006: Arbitrary JavaScript Injection
**Status:** OPEN - HIGH
**File:** `MainActivity.kt` lines 2010-2020
**Details:** `executeInjectJs()` runs arbitrary JS without validation
**Fix:** Remove or heavily restrict this feature

---

## MEDIUM - Functional Issues

### BUG-010: Proprietary Commands Visible During AI Installation
**Status:** OPEN - MEDIUM
**File:** `MainActivity.kt` lines 2662-2710
**Details:** When user taps "Install AI Tools" from settings menu, the full installation commands are visible in terminal:
- `pkg update -y && pkg upgrade -y && pkg install nodejs-lts -y && npm install -g @anthropic-ai/claude-code`
- All package URLs and download progress visible
- Exposes complete installation methodology
**Impact:** Users can see and copy exact installation process
**Fix Options:**
1. Show overlay during installation (like initial bootstrap)
2. Redirect output to /dev/null with progress indicator
3. Run installation in background service
4. Compile installation logic into native Kotlin code
**Analysis Date:** 2026-01-10

### BUG-007: Terminal Flash at 100%
**Status:** FIXED in v1.6.3
**File:** `MainActivity.kt`
**Details:** Users briefly saw proprietary terminal output after bootstrap
**Fix:** Added welcome_overlay that covers terminal during transition
**Verified:** Fix triggers at correct point (after all downloads complete)

---

## LOW - Quality Issues

### BUG-008: Personal Name in Documentation
**Status:** OPEN - LOW
**Files:** Multiple .md files
**Details:** Owner name "Samblamz" appears in 15+ files
**Fix:** Decide if this should be removed for privacy

### BUG-009: Device Info in Docs
**Status:** OPEN - LOW
**Files:** CLAUDE.md, various .md files
**Details:** "Samsung SM-G988U (S20 Ultra)" mentioned
**Fix:** Remove if privacy concern

---

## Fixed Bugs

| Bug ID | Description | Fixed In | Date |
|--------|-------------|----------|------|
| BUG-007 | Terminal flash at 100% | v1.6.3 | 2026-01-10 |

---

## Bug Template

```
### BUG-XXX: Title
**Status:** OPEN/FIXED - CRITICAL/HIGH/MEDIUM/LOW
**File:** filename.kt line XX
**Details:** Description of the issue
**Fix:** How to fix it
**Verified:** How it was tested
```
