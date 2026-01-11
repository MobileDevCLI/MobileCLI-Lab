# MobileCLI Multi-Agent System

> **v67 Feature:** Multiple Claude Code instances can now communicate with each other.

---

## Overview

The Multi-Agent System enables multiple Claude Code sessions running in different terminal tabs to:

1. **Discover** each other's sessions
2. **Read** each other's conversations
3. **Send messages** to each other
4. **Execute commands** in other terminals
5. **Monitor** all sessions from a supervisor hub

This leverages Claude Code's built-in conversation logging at `~/.claude/projects/`.

---

## Quick Start

```bash
# Discover all Claude sessions
agent discover

# See what another agent is working on
agent read 2c783855

# Watch another agent in real-time
agent tail 2c783855

# Send a message to another agent
agent send 2c783855 "Check the build output"

# Execute a command in another terminal
agent exec 1 "git status"

# Launch supervisor mode
agent hub
```

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    MobileCLI Terminal                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │   pts/0     │  │   pts/1     │  │   pts/2     │   ...       │
│  │  Claude #1  │  │  Claude #2  │  │  Claude #3  │             │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘             │
│         │                │                │                     │
│         ▼                ▼                ▼                     │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              ~/.claude/projects/                         │   │
│  │  ┌─────────────────────────────────────────────────┐    │   │
│  │  │  {session-id}.jsonl (46MB+ per session)          │    │   │
│  │  │  - Full conversation history                     │    │   │
│  │  │  - All tool uses and results                     │    │   │
│  │  │  - Timestamps for real-time monitoring           │    │   │
│  │  └─────────────────────────────────────────────────┘    │   │
│  └─────────────────────────────────────────────────────────┘   │
│                           │                                     │
│                           ▼                                     │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              ~/.mobilecli-agents/                        │   │
│  │  ├── hub/           # Agent CLI tools                   │   │
│  │  ├── sessions/      # Session registry                  │   │
│  │  ├── messages/      # Inter-agent messaging             │   │
│  │  │   ├── inbox/     # Incoming messages per agent       │   │
│  │  │   └── outbox/    # Sent messages log                 │   │
│  │  ├── exec/          # Cross-terminal command queue      │   │
│  │  └── logs/          # Activity logs                     │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## Commands

### `agent` - Main CLI

| Command | Description |
|---------|-------------|
| `agent discover` | Find all active Claude sessions |
| `agent list` | List known agents/sessions |
| `agent read <id>` | Read another agent's conversation |
| `agent tail <id>` | Watch conversation in real-time |
| `agent send <id> <msg>` | Send message to another agent |
| `agent inbox` | Check your inbox for messages |
| `agent broadcast <msg>` | Send message to all agents |
| `agent exec <pty> <cmd>` | Execute command in another terminal |
| `agent status` | Show hub status |
| `agent register` | Register this session |
| `agent hub` | Launch supervisor mode |

### `agent-exec` - Cross-Terminal Execution

| Command | Description |
|---------|-------------|
| `agent-exec <pty> <cmd>` | Queue command for pts/<pty> |
| `agent-exec --listen` | Listen for incoming commands |
| `agent-exec --status` | Show pending commands |

---

## Use Cases

### 1. Parallel Development

Run multiple Claude instances on different parts of a project:

**Terminal 1 (pts/0):** Working on frontend
```bash
claude
# "Help me build the React components"
```

**Terminal 2 (pts/1):** Working on backend
```bash
claude
# "Help me implement the API endpoints"
```

**Terminal 3 (pts/2):** Supervisor
```bash
agent hub
# Monitors both agents in real-time
```

### 2. Code Review Workflow

**Terminal 1:** Developer agent making changes
**Terminal 2:** Reviewer agent watching changes

```bash
# In Terminal 2
agent tail <session-id-of-terminal-1>
```

### 3. Build Coordination

One agent triggers another to run tests:

```bash
# Agent 1 finishes coding, tells Agent 2 to test
agent send <agent2-session> "I've finished the feature. Run the tests."

# Or execute directly
agent exec 2 "npm test"
```

### 4. Distributed Tasks

Split a large task across multiple agents:

```bash
# Terminal 1: Agent working on files A-M
# Terminal 2: Agent working on files N-Z
# Terminal 3: Supervisor coordinating

agent broadcast "All agents report status"
```

---

## JSONL Format

Claude Code stores conversations in JSONL format at:
```
~/.claude/projects/{project-path}/{session-id}.jsonl
```

Each line is a JSON object:

```json
{"type":"user","message":{"role":"user","content":"Hello"},"sessionId":"...","timestamp":"..."}
{"type":"assistant","message":{"role":"assistant","content":[{"type":"text","text":"Hi!"}]},"sessionId":"..."}
```

### Message Types

| Type | Description |
|------|-------------|
| `user` | User input or tool results |
| `assistant` | Claude's response (text, tool_use, thinking) |

### Content Types (Assistant)

| Type | Description |
|------|-------------|
| `text` | Plain text response |
| `tool_use` | Tool invocation (Bash, Edit, Read, etc.) |
| `thinking` | Claude's internal reasoning |

---

## Setup

The multi-agent system is automatically set up during bootstrap. To manually install:

```bash
# Create directories
mkdir -p ~/.mobilecli-agents/{hub,sessions,messages/inbox,messages/outbox,exec,logs}

# Link to PATH
ln -sf ~/.mobilecli-agents/hub/agent $PREFIX/bin/agent
ln -sf ~/.mobilecli-agents/hub/agent-exec $PREFIX/bin/agent-exec
```

---

## Supervisor Mode

Launch with `agent hub` to monitor all Claude sessions:

```
======================================
  MobileCLI Multi-Agent Supervisor
======================================

Monitoring all Claude sessions...
Press Ctrl+C to exit

[17:30:01 2c783855] USER: Help me fix this bug
[17:30:05 2c783855] AI responding...
[17:30:15 a1b2c3d4] USER: What's the build status?
[17:30:18 a1b2c3d4] AI responding...
```

---

## Security Notes

- All communication is local (file-based)
- No network traffic between agents
- Message files are user-readable only (chmod 600)
- Session IDs are UUIDs (not guessable)

---

## Files

| File | Location | Purpose |
|------|----------|---------|
| `agent` | `~/.mobilecli-agents/hub/agent` | Main CLI |
| `agent-exec` | `~/.mobilecli-agents/hub/agent-exec` | Cross-terminal execution |
| `registry.txt` | `~/.mobilecli-agents/sessions/` | Known sessions |
| `*.msg` | `~/.mobilecli-agents/messages/inbox/` | Message queues |
| `*.queue` | `~/.mobilecli-agents/exec/` | Command queues |

---

## Version History

| Version | Feature |
|---------|---------|
| v67 | Multi-Agent System introduced |
| v66 | Save point (stable baseline) |

---

## Future Enhancements

1. **Agent Names** - Human-readable names instead of session IDs
2. **Persistent Tasks** - Task queue that survives session restart
3. **Agent Roles** - Specialized agents (tester, reviewer, builder)
4. **Web Dashboard** - Browser-based agent monitoring
5. **Voice Commands** - Control agents via termux-tts

---

## Owner

**Samblamz / MobileDevCLI**

---

**Copyright 2026 MobileDevCLI. All Rights Reserved.**
