# MobileCLI Lab Edition

> **EXPERIMENTAL SANDBOX** - For innovation and invention testing

---

## Purpose

This is the **Lab Edition** of MobileCLI - an isolated sandbox for testing new ideas without risking the stable codebase.

| Quest | Repo | Description |
|-------|------|-------------|
| **Main Quest** | MobileCLI-Store | The sellable product |
| **Personal Dev** | MobileCLI-Developer | Your Swiss Army knife |
| **Side Quests** | MobileCLI-Lab | Inventions and experiments |

---

## The Superpower

**MobileCLI is the world's first Android app where AI can modify its own container.**

An AI running inside this app rebuilt it from v1.6.1 to v1.8.1-dev - making hard changes to compiled code. No other app can do this.

**Read:** [SUPERPOWER.md](SUPERPOWER.md) for full documentation.

---

## Current Experiments

### Multi-Agent System (Active)

Multiple Claude Code instances communicating with each other.

```bash
agent discover        # Find all Claude sessions
agent read <id>       # Read another agent's conversation
agent tail <id>       # Watch in real-time
agent send <id> <msg> # Send message to another agent
agent hub             # Launch supervisor mode
```

**Status:** In testing - needs verification
**Documentation:** [MULTI-AGENT.md](MULTI-AGENT.md)
**Lab Notes:** [LAB-NOTES.md](LAB-NOTES.md)

---

## Lab Rules

1. **Experiments stay here** - Don't merge back to Store/Developer until proven
2. **Document everything** - Use LAB-NOTES.md to track what you're testing
3. **Break things freely** - This is the sandbox, not production
4. **Version jumps allowed** - Lab uses v100+ to distinguish from main builds

---

## Version Info

| Property | Value |
|----------|-------|
| versionCode | 100 |
| versionName | 2.0.0-lab |
| Bootstrap | mobilecli-v67 |
| Build Flavor | lab |

---

## Build Commands

### Lab Debug Build
```bash
cd ~/MobileCLI-Lab
export JAVA_HOME=/data/data/com.termux/files/usr/lib/jvm/java-17-openjdk
./gradlew assembleLabDebug
cp app/build/outputs/apk/lab/debug/app-lab-debug.apk /sdcard/Download/MobileCLI-Lab.apk
```

---

## Reference Build

The 99% perfect working version is preserved:

```
reference/
├── WORKING-REFERENCE-v66-DO-NOT-MODIFY.apk
└── REFERENCE.md
```

If experiments break everything, install this to restore functionality.

---

## Graduation Criteria

An experiment moves from Lab to Developer/Store when:

1. Works reliably (tested multiple times)
2. Doesn't break existing functionality
3. Documentation is complete
4. Adds clear value to users

---

## All Documentation Files

### Core
- `README.md` - This file
- `CLAUDE.md` - AI instructions
- `LAB-NOTES.md` - Experiment tracking
- `SAVEPOINT.md` - Recovery checkpoint

### Features
- `MULTI-AGENT.md` - Multi-agent system docs
- `SUPERPOWER.md` - Self-modification capability

### Inherited Reference
- `MASTER_REFERENCE.md` - Full architecture
- `DEVELOPMENT_HISTORY.md` - Version timeline
- `INTELLECTUAL_PROPERTY_REGISTER.md` - IP documentation

---

## Related Repos

| Repo | Purpose |
|------|---------|
| [MobileCLI-Store](https://github.com/MobileDevCLI/MobileCLI-Store) | Production version |
| [MobileCLI-Developer](https://github.com/MobileDevCLI/MobileCLI-Developer) | Personal dev build |
| [MobileCLI-Lab](https://github.com/MobileDevCLI/MobileCLI-Lab) | This repo |

---

## Owner

**Samblamz / MobileDevCLI**

**Experimental - Not for Distribution**

---

**Copyright 2026 MobileDevCLI. All Rights Reserved.**
