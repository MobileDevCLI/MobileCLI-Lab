# MOBILECLI MASTER DOCUMENT
## Last Updated: January 16, 2026
## Status: CANONICAL SOURCE OF TRUTH

---

> **CLAUDE CODE: READ THIS FIRST**
>
> This document is the ONLY source of truth for MobileCLI development.
> If you're a new Claude session, READ THIS ENTIRE FILE before doing anything.
> Do NOT look at other repos or files until you understand this document.

---

## WHAT IS MOBILECLI?

MobileCLI is a **proprietary Android terminal app** that runs AI assistants (Claude Code, Gemini, Codex) on phones.

**Key Achievement:** Built entirely on an Android phone using Claude Code. The app can run the AI that built it. Self-referential development loop.

**Owner:** Samblamz / MobileDevCLI
**Website:** https://mobilecli.com
**GitHub:** https://github.com/MobileDevCLI

---

## THE TWO GOLD APKs (January 16, 2026 - VERIFIED)

### 1. WORKING STABLE: v1.8.1-dev
| Property | Value |
|----------|-------|
| File | `MobileCLI-v1.8.1-dev-WORKING.apk` |
| Internal Version | **1.8.1-dev (versionCode 76)** |
| MD5 | `d473f8f5c3f06a42fb8fd4d5aa79bd91` |
| Size | 6,877,419 bytes |
| Bootstrap | mobilecli-v66 |
| Location | `/sdcard/Download/MobileCLI-v1.8.1-dev-WORKING.apk` |
| Status | **CURRENT STABLE - Use for development** |
| Verified | Jan 16, 2026 - Fresh install tested and working |

### 2. LEGACY PROOF: AI Self-Modified (January 9, 2026)
| Property | Value |
|----------|-------|
| File | `LEGACY-AI-SELF-MODIFIED-Jan9-2026.apk` |
| External Labels | v1.5.1-welcome → v1.6.0-secure → v1.6.1-fix |
| Internal Version | **1.8.1-dev (versionCode 76)** |
| MD5 | `81663d84f041a8e8eb35be6e1163a4f6` |
| Size | 6,877,419 bytes |
| Location | `/sdcard/Download/LEGACY-AI-SELF-MODIFIED-Jan9-2026.apk` |
| Status | **HISTORICAL PROOF - AI internally changed version** |

### The Discovery Story (January 16, 2026)

After 4 days of confusion with hundreds of APKs named similarly, we discovered:

1. **The naming was misleading:** Files named "v1.6.1-fix" actually contained **v1.8.1-dev** internally
2. **AI self-modification proof:** On January 9, the AI modified the app, changing the internal version from the external label (1.5.1/1.6.x) to 1.8.1-dev
3. **Two distinct builds exist:** Same internal version (1.8.1-dev) but different MD5s - the Jan 9 original and the current stable
4. **Verification method:** Use `aapt dump badging <apk>` to see true internal version, not filename

### WARNING: Filename vs Internal Version

Many APKs have **MISLEADING NAMES**. Always verify with:
```bash
aapt dump badging <apk> | grep versionName
```

| Filename | Actual Internal Version |
|----------|------------------------|
| MobileCLI-v1.5.1-welcome.apk | 1.8.1-dev |
| MobileCLI-v1.6.0-secure.apk | 1.8.1-dev |
| MobileCLI-v1.6.1-fix.apk | 1.8.1-dev |
| MobileCLI-CURRENTLY-RUNNING.apk | 1.8.1-dev |

**Rule:** Trust the MD5 hash, not the filename.

---

## INFRASTRUCTURE (Website, Backend, Payments)

### Website
| Property | Value |
|----------|-------|
| URL | https://mobilecli.com |
| Local Path | `~/website/` |
| GitHub Repo | `MobileDevCLI/website` |
| Hosting | **Vercel** |
| Config | `~/website/vercel.json` |

### Supabase (Backend)
| Property | Value |
|----------|-------|
| Project URL | `mwxlguqukyfberyhtkmg.supabase.co` |
| SQL Setup | `~/website/supabase-setup.sql` |
| Functions | `~/website/supabase/functions/` |

### Stripe (Payments)
| Property | Value |
|----------|-------|
| Config | `~/website/js/stripe-config.js` |
| Functions | `create-checkout`, `customer-portal`, `stripe-webhook` |
| Dashboard | `~/website/dashboard.html` |

### Deploy Website
```bash
cd ~/website
vercel --prod
```

---

## HOW THE APP WORKS (IP ARCHITECTURE)

### Legal Foundation
- **Package name:** `com.termux` (required for binary compatibility)
- **Our code:** ~1,280 lines of Kotlin (100% proprietary)
- **External libs:** terminal-view, terminal-emulator (Apache 2.0 - OK for commercial)
- **Bootstrap:** Downloaded at runtime from GitHub (GPL compliant)

### Bootstrap Flow (User Experience)
```
1. App opens → Setup Overlay HIDES terminal
2. Progress bar: 0% → 100%
3. AI Selection: Claude | Gemini | Codex | None
4. Welcome animation (5 seconds)
5. Clean terminal: user@mobilecli:~$
```

### IP Protection Points
| What | How |
|------|-----|
| Setup overlay | Hides all technical output during install |
| motd file | Shows "Welcome to MobileCLI" not Termux stuff |
| PS1 prompt | Shows `@mobilecli` not paths |
| Dev mode | Hidden until 7-tap on version |

### Key Proprietary Features
- Claude Power Mode (`--dangerously-skip-permissions` by default)
- Auto-create CLAUDE.md when entering git repos
- Persistent AI memory system (`~/.mobilecli/memory/`)
- 42 API scripts built-in
- Self-modification capability

---

## WHICH FILES TO USE

### Source Code
| Repo | Purpose | Use? |
|------|---------|------|
| `~/MobileCLI-Production` | Main development | **YES** |
| `~/MobileCLI-Lab` | Self-modification experiments | **YES (carefully)** |
| `~/MobileCLI-Baseline` | Reference documentation | Reference only |

### Critical Files
| File | Location | Purpose |
|------|----------|---------|
| Signing Key | `~/mobilecli-release.keystore` | **PROTECT THIS** |
| Main CLAUDE.md | `~/CLAUDE.md` | 56KB project instructions |

### Gold Documentation
| File | Location | Contains |
|------|----------|----------|
| `SYSTEM_PROMPT.md` | `~/.mobilecli/` | AI environment knowledge |
| `DEVELOPMENT_HISTORY.md` | `~/` | v1-v32 journey |
| `APP_BUILD_HISTORY.md` | `~/` | All bugs and fixes |
| `INTELLECTUAL_PROPERTY_REGISTER.md` | `~/MobileCLI-Lab/` | 15 documented inventions |
| `21_INVENTIONS.md` | `~/MobileCLI-Lab/` | 21 invention ideas |

### Decompiled Reference
| Path | Contains |
|------|----------|
| `~/v161-decompiled/sources/com/termux/` | Decompiled source |

---

## DO NOT TOUCH (SIDE QUESTS)

These were experiments that went wrong. **DO NOT develop these:**

| Project | Why Not |
|---------|---------|
| MobileCLI-Games | Side quest, incomplete |
| Game developing mode | Abandoned experiment |
| Ditto profiles standalone | Experimental, broken |
| Any "v80-v99" numbered builds | Experimental iterations |

**If user asks about these, redirect to main MobileCLI development.**

---

## CURRENT DEVELOPMENT STATE (v1.8.1-dev)

### What Works
- Bootstrap download and extraction
- AI selection (Claude/Gemini/Codex)
- Terminal with full Linux environment
- 42 API scripts
- Multi-session (10 tabs)
- Wake lock for background
- URL opening (OAuth works)
- Developer mode (7-tap)
- Fresh install tested and verified (Jan 16, 2026)

### What Needs Work
- Multi-agent communication (Lab feature)
- Self-modification wizard (Lab feature)
- Home screen widgets (planned)
- Boot scripts (planned)

---

## HOW TO START DEVELOPING

### Step 1: Verify Gold APKs Exist
```bash
# Check working stable
ls -la /sdcard/Download/MobileCLI-v1.8.1-dev-WORKING.apk
md5sum /sdcard/Download/MobileCLI-v1.8.1-dev-WORKING.apk
# Should be: d473f8f5c3f06a42fb8fd4d5aa79bd91

# Check legacy proof
ls -la /sdcard/Download/LEGACY-AI-SELF-MODIFIED-Jan9-2026.apk
md5sum /sdcard/Download/LEGACY-AI-SELF-MODIFIED-Jan9-2026.apk
# Should be: 81663d84f041a8e8eb35be6e1163a4f6
```

### Step 2: Build
```bash
cd ~/MobileCLI-Production
./gradlew assembleDebug
cp app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/MobileCLI-NEW.apk
```

### Step 3: Test
Install on phone, verify:
1. Bootstrap completes
2. AI selection shows
3. Terminal works
4. `claude` command works

---

## KEY TECHNICAL REQUIREMENTS

| Requirement | Value | Why |
|-------------|-------|-----|
| Package name | `com.termux` | Hardcoded RUNPATH in Termux binaries |
| targetSdkVersion | 28 | Android 10+ blocks exec() otherwise |
| HOME path | `/data/data/com.termux/files/home` | npm/node require exact path |
| am.apk permissions | 0400 (read-only) | Android 14+ security |

---

## 3 BREAKTHROUGHS (NEVER FORGET)

1. **v10 - HOME Directory**
   ```kotlin
   // WRONG: val homeDir = filesDir
   // RIGHT: val homeDir = File(filesDir, "home")
   ```

2. **v19 - Screen Size (Reflection)**
   ```kotlin
   val rendererField = terminalView.javaClass.getDeclaredField("mRenderer")
   // Get actual font metrics, not calculated
   ```

3. **v54 - URL Opening (am.apk)**
   ```kotlin
   amApkDest.setReadOnly() // chmod 0400 for Android 14+
   ```

---

## FOR NEW CLAUDE SESSIONS

If you're a new Claude Code session helping with MobileCLI:

1. **Read this entire file first**
2. **Use `~/MobileCLI-Production` for development**
3. **The working APK is v1.8.1-dev (NOT v1.6.1 despite old filenames)**
4. **Verify APK versions with `aapt dump badging`, not filenames**
5. **DO NOT create new repos or side projects**
6. **Build and test on actual device**
7. **Save APKs to `/sdcard/Download/` with version numbers**

---

## DOCUMENT HISTORY

| Date | Change |
|------|--------|
| Jan 16, 2026 | **MAJOR UPDATE:** Discovered true version is 1.8.1-dev, not v1.6.1. Updated gold APKs. Added legacy proof APK. |
| Jan 16, 2026 | Verified working APK via fresh install test |
| Jan 10, 2026 | Original gold APK identified (mislabeled as v1.6.1-fix) |
| Jan 9, 2026 | AI self-modification created internal v1.8.1-dev |
| Jan 7, 2026 | Original documentation created |
| Jan 5, 2026 | v10 breakthrough (HOME directory fix) |

---

**This document is the canonical source. When in doubt, follow this.**

**Copyright 2026 Samblamz / MobileDevCLI. All Rights Reserved.**
