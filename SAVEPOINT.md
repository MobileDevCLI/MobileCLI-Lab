# SAVE POINT - January 10, 2026 (Evening)

> **STABLE CHECKPOINT** - Three-Edition Structure Established

---

## Save Point Summary

| Item | Status |
|------|--------|
| **Date** | January 10, 2026 |
| **Time** | ~18:00 UTC |
| **State** | STABLE - Three repos established |
| **Version** | v67 (Store/Developer), v100 (Lab) |

---

## The Three Editions

| Edition | Repo | Purpose | versionCode |
|---------|------|---------|-------------|
| **Store** | MobileCLI-Store | Production/sellable | 97 |
| **Developer** | MobileCLI-Developer | Personal dev environment | 97 |
| **Lab** | MobileCLI-Lab | Experimental sandbox | 100 |

---

## What's New Since Last Save Point

1. **Multi-Agent System (v67)**
   - `agent` CLI for Claude instances to communicate
   - Reads `~/.claude/projects/` JSONL files
   - Commands: discover, read, tail, send, exec, hub

2. **Three-Edition Structure**
   - Store: Main quest (sellable product)
   - Developer: Your Swiss Army knife
   - Lab: Side quests (experiments)

3. **New Documentation**
   - `MULTI-AGENT.md` - Multi-agent system docs
   - `LAB-NOTES.md` - Experiment tracking (Lab only)

---

## Reference Build (Gold Standard)

```
reference/WORKING-REFERENCE-v66-DO-NOT-MODIFY.apk
```
- Actual version: 1.8.1-dev (versionCode 76)
- Bootstrap: mobilecli-v66
- 99% perfect functionality

If anything breaks, install this.

---

## APKs on Phone (/sdcard/Download/)

```
MobileCLI-STORE-v67.apk         # Store edition
MobileCLI-DEVELOPER-v67.apk     # Developer edition
MobileCLI-Lab.apk               # Lab edition (when built)
MobileCLI-REFERENCE-v66-GOLD.apk # Recovery backup
```

---

## GitHub Repos

| Repo | URL |
|------|-----|
| Store | https://github.com/MobileDevCLI/MobileCLI-Store |
| Developer | https://github.com/MobileDevCLI/MobileCLI-Developer |
| Lab | https://github.com/MobileDevCLI/MobileCLI-Lab |

---

## How to Revert

### Option 1: Install Reference APK
```bash
cp ~/MobileCLI-Store/reference/WORKING-REFERENCE-v66-DO-NOT-MODIFY.apk /sdcard/Download/
# Install via file manager
```

### Option 2: Git Reset
```bash
# Check git log for specific commit
cd ~/MobileCLI-Store
git log --oneline -10
git reset --hard <commit-hash>
```

---

## Key Files by Edition

### Store (Production)
- Clean UX, IP protected
- `./gradlew assembleUserDebug`

### Developer (Personal)
- All features visible
- `./gradlew assembleDevDebug`

### Lab (Experiments)
- Sandbox for inventions
- `./gradlew assembleLabDebug`

---

## Owner

**Samblamz / MobileDevCLI**

**Save Point Created: January 10, 2026 Evening**
