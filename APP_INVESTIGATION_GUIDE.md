# App Investigation Guide - Claude Code Capability

## Overview

Claude Code running in Termux can **investigate any app installed on your Android phone**. This is a powerful reverse-engineering and analysis capability.

## What We Can Analyze

For ANY installed app, we can extract:
- Package name, version, SDK targets
- All declared permissions
- Activities, Services, Receivers, Providers
- Intent filters
- Application metadata
- Resource files
- DEX code (with additional tools)

## Commands

### List All Installed Apps
```bash
pm list packages
```

### Get APK Path
```bash
pm path com.example.app
```

### Analyze APK Metadata
```bash
aapt dump badging /path/to/base.apk
```

### Extract Permissions
```bash
aapt dump permissions /path/to/base.apk
```

### Full Manifest XML Tree
```bash
aapt dump xmltree /path/to/base.apk AndroidManifest.xml
```

### Extract Resources
```bash
aapt dump resources /path/to/base.apk
```

## Example: Investigating Real Termux

```bash
# Get APK path
pm path com.termux
# Output: package:/data/app/.../com.termux-.../base.apk

# Analyze
aapt dump badging /data/app/.../base.apk

# Key findings:
# - Package: com.termux
# - targetSdkVersion: 28
# - Has SYSTEM_ALERT_WINDOW permission
# - Uses custom TermuxAm for activity manager
```

## Example: Investigating Samsung Internet

```bash
pm path com.sec.android.app.sbrowser
aapt dump permissions /data/app/.../base.apk

# Findings:
# - Has many custom Samsung permissions
# - Uses SYSTEM_ALERT_WINDOW
# - Has custom intent filters for URL handling
```

## Use Cases

1. **Learn how apps work** - See what permissions they need, what components they have
2. **Security auditing** - Check what permissions apps are requesting
3. **Reverse engineering** - Understand app architecture
4. **Compatibility testing** - Ensure your app matches others' patterns
5. **Debugging** - Find why intents aren't working between apps

## Limitations

- Cannot read app's private data (needs root)
- Cannot execute app's code directly
- Binary DEX code requires additional decompilers
- Some system apps may have restricted access

## Apps Analyzed on This Device

| App | Package | Permissions | Notes |
|-----|---------|-------------|-------|
| Termux | com.termux | SYSTEM_ALERT_WINDOW, INTERNET, WAKE_LOCK | Uses custom TermuxAm |
| Samsung Internet | com.sec.android.app.sbrowser | 50+ permissions | Custom URL handling |
| zandroidapk1 | com.example.zandroidapk1 | Storage, Internet | User's custom app |
| GDevelop IDE | io.gdevelop.ide | Various | Split APKs |

---

*This capability was discovered on January 6, 2026 while investigating URL opening issues.*
