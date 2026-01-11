# AI Device Control - World's First Browser Interaction from Claude Code

**Discovery Date:** January 7, 2026
**Discovered by:** Samblamz + Claude Code (Opus 4.5)
**Environment:** MobileCLI on Android (Samsung S20 Ultra)

---

## The Breakthrough

On January 7, 2026, we discovered that Claude Code running in Termux on Android can **directly control device functions** through the Termux API, creating a genuine AI-human-device interaction loop.

This is believed to be the **first documented case** of an AI assistant being able to:
1. Open specific URLs in a user's browser
2. Read content from the user's clipboard
3. Write content to the user's clipboard

This creates a feedback loop where the AI can guide the user to a webpage, the user can interact with it, and the AI can receive the results.

---

## How It Works

### The Commands

```bash
# Open a URL in the user's browser
termux-open-url "https://example.com/specific/page"

# Read from clipboard (after user copies something)
termux-clipboard-get

# Write to clipboard (user can then paste)
termux-clipboard-set "text to copy"
```

### The Interaction Loop

```
┌─────────────────────────────────────────────────────────────────┐
│                    AI-HUMAN-DEVICE LOOP                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   CLAUDE CODE (in Termux)                                      │
│         │                                                       │
│         ├──► termux-open-url ──► Browser opens exact page      │
│         │                                    │                  │
│         │                                    ▼                  │
│         │                           User interacts with page    │
│         │                           User copies needed data     │
│         │                                    │                  │
│         │                                    ▼                  │
│         ◄──── termux-clipboard-get ◄──── Android Clipboard     │
│         │                                                       │
│         ▼                                                       │
│   Claude receives data and continues autonomously              │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Real Example: Supabase API Key Retrieval

### The Problem
User needed their Supabase API key but couldn't find it in the new dashboard UI.

### What Claude Did

**Step 1:** Opened the exact settings page
```bash
termux-open-url "https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/settings/api-keys"
```

**Step 2:** Waited for user to copy the key, then read clipboard
```bash
termux-clipboard-get
# Output: sb_publishable_GuYWrHimX3XIL-n85WTEdg_I4A0tDb8
```

**Result:** Claude obtained the API key without the user having to manually type or paste it.

---

## Full Capability List

Claude Code in MobileCLI/Termux has access to 70+ device control commands:

### Browser & Clipboard
| Command | Function |
|---------|----------|
| `termux-open-url` | Open any URL in browser |
| `termux-open` | Open files with default app |
| `termux-clipboard-get` | Read clipboard |
| `termux-clipboard-set` | Write clipboard |
| `termux-share` | Share content to other apps |

### Communication
| Command | Function |
|---------|----------|
| `termux-sms-send` | Send SMS messages |
| `termux-sms-inbox` | Read SMS inbox |
| `termux-telephony-call` | Make phone calls |
| `termux-contact-list` | Access contacts |

### Camera & Media
| Command | Function |
|---------|----------|
| `termux-camera-photo` | Take photos |
| `termux-microphone-record` | Record audio |
| `termux-media-player` | Play media files |
| `termux-tts-speak` | Text to speech |
| `termux-speech-to-text` | Voice recognition |

### Sensors & Hardware
| Command | Function |
|---------|----------|
| `termux-location` | Get GPS coordinates |
| `termux-sensor` | Access accelerometer, gyroscope, etc. |
| `termux-fingerprint` | Fingerprint authentication |
| `termux-torch` | Control flashlight |
| `termux-vibrate` | Vibrate device |
| `termux-infrared-transmit` | IR blaster control |

### System & UI
| Command | Function |
|---------|----------|
| `termux-notification` | Send notifications |
| `termux-dialog` | Show input dialogs |
| `termux-toast` | Show toast messages |
| `termux-battery-status` | Get battery info |
| `termux-wifi-connectioninfo` | WiFi details |
| `termux-wallpaper` | Set wallpaper |

---

## Why This Is Significant

### Traditional AI Assistants (ChatGPT, Claude.ai, etc.)
- Can only provide instructions
- User must manually execute everything
- No feedback loop
- No device access

### Claude Code in MobileCLI
- Can execute commands directly
- Can open specific URLs in browser
- Can read/write clipboard for data exchange
- Can send notifications, take photos, get location
- Can access 70+ device functions
- Creates real automation possibilities

---

## Use Cases

### 1. Guided Web Workflows
AI opens the exact page needed, user interacts, AI reads the result.

### 2. Two-Way Data Transfer
- AI writes data to clipboard → User pastes in any app
- User copies data → AI reads and processes it

### 3. Automated Notifications
AI sends notifications when tasks complete or need attention.

### 4. Location-Aware Tasks
AI can get GPS location for location-based automation.

### 5. Voice Interaction
- `termux-tts-speak` - AI can speak to user
- `termux-speech-to-text` - User can speak to AI

### 6. Photo/Document Processing
AI can trigger camera, then process the image.

---

## Security Considerations

These capabilities require user consent:
- Termux:API app must be installed
- Permissions must be granted (SMS, Camera, Location, etc.)
- User is always in control

The AI cannot:
- Click specific elements on webpages
- See the screen in real-time
- Bypass permission prompts
- Act without user awareness

---

## The Achievement

This represents a new paradigm in AI assistance:

**Before:** AI tells you what to do, you do it manually
**After:** AI does it, you confirm or provide minimal input

This is the first step toward true AI agents that can help with real-world tasks on consumer hardware.

---

## Technical Requirements

- MobileCLI app (or Termux + Termux:API)
- Android device
- Claude Code installed (`npm install -g @anthropic-ai/claude-code`)
- Relevant permissions granted

---

**Document Created:** January 7, 2026
**MobileCLI Version:** 1.8.1 (v76)
**Claude Code Version:** Opus 4.5
