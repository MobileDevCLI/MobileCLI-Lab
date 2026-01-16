# MOBILECLI COMPLETE INDEX
## Last Updated: January 16, 2026
## For Claude Code: START HERE

---

> **NEW CLAUDE SESSION?** Read files in this order:
> 1. This file (INDEX)
> 2. `~/MOBILECLI-MASTER.md` (canonical source of truth)
> 3. Relevant docs below based on task

---

## QUICK LINKS

### Gold APKs (DO NOT LOSE)
| APK | Location | MD5 |
|-----|----------|-----|
| **v1.6.1-fix (stable)** | `~/downloads/MobileCLI-v1.6.1-fix.apk` | `d473f8f5c3f06a42fb8fd4d5aa79bd91` |
| **Lab-SELFMOD** | `~/downloads/MobileCLI-Lab-SELFMOD.apk` | `a9a7e18e04280ce8a41d03980ecb0b20` |

### Development Repos
| Repo | Location | Purpose |
|------|----------|---------|
| **Production** | `~/MobileCLI-Production/` | Main development |
| **Lab** | `~/MobileCLI-Lab/` | Self-modification experiments |
| **Baseline** | `~/MobileCLI-Baseline/` | Reference documentation |
| **Website** | `~/website/` | mobilecli.com |

---

## DOCUMENTATION MAP

### In ~/MobileCLI-Baseline/docs/

| File | Purpose | Read When |
|------|---------|-----------|
| `MASTER_REFERENCE.md` | Complete project reference | Starting new work |
| `ARCHITECTURE.md` | App architecture | Understanding structure |
| `intellectual-property.md` | IP and DITTO architecture | Legal/IP questions |
| `V161_COMPLETE_FEATURES.md` | v1.6.1 feature list | Feature reference |
| `STRIPE_INTEGRATION.md` | Payment setup | Payment work |
| `PRIVACY_POLICY.md` | Privacy policy | Legal |
| `TERMS_OF_SERVICE.md` | Terms of service | Legal |
| `security-audit.md` | Security findings | Security work |
| `bugs-and-issues.md` | Known bugs | Bug fixing |
| `progress.md` | Current progress | Status check |
| `REBUILD_PLAN.md` | Rebuild instructions | Rebuilding |

### In ~/

| File | Purpose |
|------|---------|
| `MOBILECLI-MASTER.md` | **CANONICAL SOURCE - Read this** |
| `MOBILECLI-INDEX.md` | This file (navigation) |
| `DEVELOPMENT_HISTORY.md` | v1-v32 journey |
| `APP_BUILD_HISTORY.md` | All bugs and fixes |
| `FEATURE_PLANS_v33+.md` | Future roadmap |
| `TERMUX_COMPARISON.md` | MobileCLI vs Termux |
| `LEGAL_AUDIT.md` | Legal considerations |

### In ~/.mobilecli/

| File | Purpose |
|------|---------|
| `SYSTEM_PROMPT.md` | AI environment knowledge |
| `memory/` | Persistent AI memory |

### In ~/v161-decompiled/

| Path | Purpose |
|------|---------|
| `sources/com/termux/MainActivity.java` | Decompiled main activity |
| `sources/com/termux/BootstrapInstaller.java` | Decompiled bootstrap |

---

## DITTO ARCHITECTURE (AI UI Control)

**What is DITTO?**
DITTO allows the AI to control the app's UI at runtime via terminal commands.

**Key Components:**
- `DittoJsBridge` - JavaScript bridge for terminal commands
- WebView overlay - Morphable UI layer
- Profile system - Shareable UI configurations

**Docs:** `~/MobileCLI-Baseline/docs/intellectual-property.md` (Section 2)

**Repo:** `https://github.com/MobileDevCLI/ditto-store`

---

## INFRASTRUCTURE

| Service | Details |
|---------|---------|
| **Website** | https://mobilecli.com |
| **Hosting** | Vercel (`~/website/vercel.json`) |
| **Backend** | Supabase (`mwxlguqukyfberyhtkmg.supabase.co`) |
| **Payments** | Stripe (see `~/website/supabase/functions/`) |
| **GitHub** | https://github.com/MobileDevCLI |

---

## BUILD COMMANDS

### Production Build
```bash
cd ~/MobileCLI-Production
./gradlew assembleDebug
cp app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/MobileCLI-vX.X.X.apk
```

### Website Deploy
```bash
cd ~/website
vercel --prod
```

---

## DO NOT TOUCH (Side Quests)

| Project | Reason |
|---------|--------|
| MobileCLI-Games | Abandoned |
| v80-v99 builds | Experimental, possibly broken |
| Ditto profiles standalone | Incomplete |
| Game developing mode | Abandoned |

**Focus on:** MobileCLI-Production and MobileCLI-Lab only.

---

## FOR FUTURE CLAUDE SESSIONS

When starting fresh, say:
```
Read ~/MOBILECLI-INDEX.md and ~/MOBILECLI-MASTER.md first,
then help me develop MobileCLI.
```

Or from GitHub:
```
Read MOBILECLI-MASTER.md from MobileCLI-Production repo,
then help me with MobileCLI development.
```

---

## KEY FACTS TO REMEMBER

1. **Package name MUST be `com.termux`** (hardcoded paths)
2. **targetSdkVersion MUST be 28** (binary execution)
3. **HOME = `/data/data/com.termux/files/home`** (not /files)
4. **am.apk MUST be chmod 0400** (Android 14+ security)
5. **v1.6.1-fix (6,877,419 bytes) is GOLD** - all same-size copies are identical

---

## CONTACT

**Owner:** Samblamz / MobileDevCLI
**Website:** https://mobilecli.com
**GitHub:** https://github.com/MobileDevCLI
