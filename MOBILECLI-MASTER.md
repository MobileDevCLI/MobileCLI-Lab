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

### Slack MCP Integration
| Property | Value |
|----------|-------|
| URL | `https://slack.mcp.anthropic.com/mcp` |

### Key Website Pages
| Page | Purpose |
|------|---------|
| `index.html` | Homepage |
| `pricing.html` | Subscription plans |
| `download.html` | APK downloads |
| `app-story.html` | The development story |
| `proof.html` | Evidence of achievement |
| `dashboard.html` | User dashboard |
| `studio.html` | MobileCLI Studio |

### Deploy Website
```bash
cd ~/website
vercel --prod
```

---

## THE TWO GOLD APKs (DO NOT LOSE THESE)

### 1. Stable Base: v1.6.1-fix
| Property | Value |
|----------|-------|
| File | `MobileCLI-v1.6.1-fix.apk` |
| MD5 | `d473f8f5c3f06a42fb8fd4d5aa79bd91` |
| Size | 6,877,419 bytes |
| Bootstrap | mobilecli-v66 |
| Location | `~/downloads/MobileCLI-v1.6.1-fix.apk` |
| Status | **99% PERFECT - Use as baseline** |

### 2. Self-Modification: Lab-SELFMOD
| Property | Value |
|----------|-------|
| File | `MobileCLI-Lab-SELFMOD.apk` |
| MD5 | `a9a7e18e04280ce8a41d03980ecb0b20` |
| Size | 7,327,663 bytes |
| Bootstrap | mobilecli-v67 |
| Location | `~/downloads/MobileCLI-Lab-SELFMOD.apk` |
| Status | **Has self-modification features** |

### WARNING: Multiple v1.6.x Versions Exist

| File | Size | Status |
|------|------|--------|
| `MobileCLI-v1.6.1-fix.apk` | 6,877,419 | **GOLD - Use this** |
| `MobileCLI-v1.6.0-secure.apk` | 6,877,419 | Same as above (identical) |
| `MobileCLI-v1.6.2-overlay-fix.apk` | 5,911,229 | **DIFFERENT - May be broken** |
| `MobileCLI-v1.6.3-welcome-fix.apk` | 7,149,956 | **DIFFERENT - May be broken** |

**Rule:** If size is 6,877,419 bytes, it's the gold v1.6.1-fix. Other sizes are experimental attempts.

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
- Self-modification capability (Lab version)

---

## WHICH FILES TO USE

### Source Code
| Repo | Purpose | Use? |
|------|---------|------|
| `~/MobileCLI-Production` | Main development | **YES** |
| `~/MobileCLI-Lab` | Self-modification experiments | **YES (carefully)** |
| `~/MobileCLI-Store` | Store-ready version | Reference only |
| `~/MobileCLI-Developer` | Dev edition | Reference only |

### Gold Documentation (DO NOT MODIFY)
| File | Location | Contains |
|------|----------|----------|
| `SYSTEM_PROMPT.md` | `~/.mobilecli/` | AI environment knowledge |
| `DEVELOPMENT_HISTORY.md` | `~/` | v1-v32 journey |
| `APP_BUILD_HISTORY.md` | `~/` | All bugs and fixes |
| `FEATURE_PLANS_v33+.md` | `~/` | Future roadmap |

### Decompiled Reference
| Path | Contains |
|------|----------|
| `~/v161-decompiled/sources/com/termux/` | Decompiled v1.6.1 source |

---

## DO NOT TOUCH (SIDE QUESTS)

These were experiments that went wrong. **DO NOT develop these:**

| Project | Why Not |
|---------|---------|
| MobileCLI-Games | Side quest, incomplete |
| Game developing mode | Abandoned experiment |
| Ditto profiles | Experimental, broken |
| Any "v80-v99" numbered builds | Experimental iterations |

**If user asks about these, redirect to main MobileCLI development.**

---

## CURRENT DEVELOPMENT STATE

### What Works (v1.6.1-fix)
- Bootstrap download and extraction
- AI selection (Claude/Gemini/Codex)
- Terminal with full Linux environment
- 42 API scripts
- Multi-session (10 tabs)
- Wake lock for background
- URL opening (OAuth works)
- Developer mode (7-tap)

### What Needs Work
- Multi-agent communication (Lab feature)
- Self-modification wizard (Lab feature)
- Home screen widgets (planned v33)
- Boot scripts (planned v33)

---

## HOW TO START DEVELOPING

### Step 1: Verify Environment
```bash
# Check gold APKs exist
ls -la ~/downloads/MobileCLI-v1.6.1-fix.apk
ls -la ~/downloads/MobileCLI-Lab-SELFMOD.apk

# Check source exists
ls ~/MobileCLI-Production/app/src/main/java/com/termux/
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
3. **Reference `~/v161-decompiled/` to understand existing code**
4. **DO NOT create new repos or side projects**
5. **Build and test on actual device**
6. **Save APKs to `/sdcard/Download/` with version numbers**

---

## DOCUMENT HISTORY

| Date | Change |
|------|--------|
| Jan 16, 2026 | Created master document to restore development capability |
| Jan 10, 2026 | v1.6.1-fix identified as gold standard |
| Jan 7, 2026 | Original documentation created |
| Jan 5, 2026 | v10 breakthrough (HOME directory fix) |

---

**This document is the canonical source. When in doubt, follow this.**
