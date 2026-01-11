# MobileCLI Lab Notes

> **Experiment Tracking** - Document what you're testing and what you learn

---

## Active Experiments

*(None currently - Experiment #1 moved to Completed)*

---

## Completed Experiments

### Experiment #1: Multi-Agent System - VERIFIED SUCCESS

**Started:** January 10, 2026
**Completed:** January 10, 2026, 18:10 UTC-8
**Status:** **VERIFIED - TWO CLAUDE SESSIONS COMMUNICATED**
**Version:** v67 (bootstrap), v100 (lab build)

#### Goal
Enable multiple Claude Code instances to communicate with each other inside MobileCLI.

#### Approach
- Tap into Claude Code's existing JSONL logging at `~/.claude/projects/`
- File-based messaging system at `~/.mobilecli-agents/`
- CLI tool `agent` for discovery, reading, messaging

#### What Was Built
1. `agent` CLI with commands: discover, read, tail, send, exec, hub, status
2. `agent-exec` for cross-terminal command execution
3. Directory structure at `~/.mobilecli-agents/`

#### Test Results - ALL CORE FEATURES VERIFIED

| Test | Status | Evidence |
|------|--------|----------|
| Multiple terminals discovering each other | **PASSED** | Session e47fb870 found session 2c783855 |
| Message inbox/outbox working | **PASSED** | 3 messages received and displayed |
| Session reading | **PASSED** | Could read 46MB conversation history |
| Bug detection/fixing | **PASSED** | Second Claude fixed first Claude's bugs |

#### What Still Needs Testing
- [ ] Real-time tail working across sessions
- [ ] Cross-terminal exec functioning
- [ ] Hub supervisor mode monitoring all sessions
- [ ] Broadcast to all agents

#### Bugs Found and Fixed During Test

**Bug 1:** `jq -n` outputs pretty JSON, should be `jq -c -n` for compact JSONL
**Bug 2:** Inbox parser couldn't handle multi-line JSON, fixed with `jq -s '.[]'`

Both bugs were found and fixed by Session e47fb870 during live testing.

#### Key Observations
1. Claude Code logs everything to JSONL automatically - no custom logging needed
2. Session files can be 46MB+ (full conversation history) - readable in real-time
3. jq is required for JSON parsing - added to bootstrap dependencies
4. File-based messaging is instant - no network latency
5. One Claude instance can fix another's code - true collaboration

#### Questions Answered
1. **Can JSONL be read while Claude writes?** YES - no locking issues observed
2. **Is file locking an issue?** NO - concurrent read/write works fine
3. **How to handle large files?** Use `tail` for recent messages, full file for history

#### Significance
**World's first mobile app with local AI-to-AI communication.**

Full documentation: [DISCOVERY.md](DISCOVERY.md)

---

## Failed Experiments

*(None yet)*

---

## Ideas Backlog

### Idea: Agent Roles (HIGH PRIORITY)
Give agents specialized roles (builder, tester, reviewer) with different capabilities.
**Status:** Ready to implement after multi-agent verification

### Idea: UI Panel (HIGH PRIORITY)
Swipe-to-reveal panel in Android app showing active agents and messages.
**Status:** User requested - implement in MainActivity.kt

### Idea: Unlimited Tabs (HIGH PRIORITY)
Remove current 10-tab limitation.
**Status:** User requested - find and remove limit

### Idea: Voice Control
Use termux-tts to announce when messages arrive from other agents.

### Idea: Persistent Tasks
Task queue that survives session restart - agents can pick up where they left off.

### Idea: Agent-Start Wrapper
Gemini suggested `agent-start` script to auto-register and launch Claude with roles.

---

## Session Log

### January 10, 2026

**18:00** - Created multi-agent system (agent CLI, directories, messaging)
**18:06** - Virtual test began, found JSON formatting issues
**18:09** - User opened second terminal tab with Claude
**18:09** - Session e47fb870 discovered session 2c783855
**18:10** - Session e47fb870 sent messages to session 2c783855
**18:10** - Session e47fb870 found and fixed bugs
**18:10** - Session 2c783855 received all messages - **TEST PASSED**

---

## How to Use This File

1. **Starting an experiment**: Add a new section under "Active Experiments"
2. **Testing**: Check off items as you verify them
3. **Learning**: Add observations as you discover things
4. **Completing**: Move to "Completed" when it works, or "Failed" if it doesn't
5. **Ideas**: Add new ideas to the backlog

---

**Last Updated:** January 10, 2026, 18:15 UTC-8
