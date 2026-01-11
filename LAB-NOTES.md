# MobileCLI Lab Notes

> **Experiment Tracking** - Document what you're testing and what you learn

---

## Active Experiments

### Experiment #1: Multi-Agent System

**Started:** January 10, 2026
**Status:** Testing
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

#### What Needs Testing
- [ ] Multiple terminals actually discovering each other
- [ ] Real-time tail working across sessions
- [ ] Message inbox/outbox working
- [ ] Cross-terminal exec functioning
- [ ] Hub supervisor mode monitoring all sessions

#### Observations
- Claude Code logs everything to JSONL automatically
- Session files can be 40MB+ (full conversation history)
- jq is required for JSON parsing (needs to be in bootstrap)

#### Questions
1. Can the JSONL be read while Claude is actively writing to it?
2. Is file locking an issue?
3. How do we handle very large session files?

---

## Completed Experiments

*(None yet - this is the first)*

---

## Failed Experiments

*(Document what didn't work and why)*

---

## Ideas Backlog

### Idea: Agent Roles
Give agents specialized roles (builder, tester, reviewer) with different capabilities.

### Idea: UI Panel
Swipe-to-reveal panel in Android app showing active agents and messages.

### Idea: Voice Control
Use termux-tts to announce when messages arrive from other agents.

### Idea: Persistent Tasks
Task queue that survives session restart - agents can pick up where they left off.

---

## How to Use This File

1. **Starting an experiment**: Add a new section under "Active Experiments"
2. **Testing**: Check off items as you verify them
3. **Learning**: Add observations as you discover things
4. **Completing**: Move to "Completed" when it works, or "Failed" if it doesn't
5. **Ideas**: Add new ideas to the backlog

---

## Template for New Experiments

```markdown
### Experiment #N: [Name]

**Started:** [Date]
**Status:** Planning / Testing / Complete / Failed
**Version:** [version info]

#### Goal
[What are you trying to achieve?]

#### Approach
[How are you going to do it?]

#### What Was Built
[List of files/features created]

#### What Needs Testing
- [ ] Test item 1
- [ ] Test item 2

#### Observations
[What did you learn?]

#### Questions
[What's still unclear?]
```

---

**Last Updated:** January 10, 2026
