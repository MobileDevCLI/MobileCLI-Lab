# AI Integration Expansion Plan

**Owner:** Samblamz
**Date:** January 7, 2026
**Status:** PLAN FOR REVIEW
**Foundation:** MobileCLI v89 - 150+ hours, working codebase

---

## Current State

MobileCLI already has:
- Claude Code CLI working (API-based)
- Full terminal environment (Termux)
- DITTO WebView for custom UI
- 84+ device APIs
- GitHub, Vercel control
- APK building capability

**Goal:** Expand to connect ANY AI - local models AND external APIs.

---

## Phase 1: Shell Scripts for AI Connections

Add shell commands that work immediately in terminal. No app rebuild needed.

### 1.1 Hugging Face CLI

```bash
# Install
pip install huggingface_hub

# Shell script: ~/.local/bin/hf
#!/data/data/com.termux/files/usr/bin/bash
# Hugging Face wrapper

case "$1" in
    login)
        huggingface-cli login
        ;;
    download)
        huggingface-cli download "$2" "$3"
        ;;
    run)
        # Run inference via API
        curl -X POST "https://api-inference.huggingface.co/models/$2" \
            -H "Authorization: Bearer $(cat ~/.huggingface/token)" \
            -H "Content-Type: application/json" \
            -d "{\"inputs\": \"$3\"}"
        ;;
    list)
        huggingface-cli repo list
        ;;
    *)
        echo "Usage: hf [login|download|run|list]"
        ;;
esac
```

### 1.2 LM Studio Connection

```bash
# Shell script: ~/.local/bin/lmstudio
#!/data/data/com.termux/files/usr/bin/bash
# Connect to LM Studio running on local network

LMSTUDIO_HOST="${LMSTUDIO_HOST:-192.168.1.100}"
LMSTUDIO_PORT="${LMSTUDIO_PORT:-1234}"

case "$1" in
    chat)
        shift
        curl -s "http://$LMSTUDIO_HOST:$LMSTUDIO_PORT/v1/chat/completions" \
            -H "Content-Type: application/json" \
            -d "{
                \"messages\": [{\"role\": \"user\", \"content\": \"$*\"}],
                \"temperature\": 0.7
            }" | jq -r '.choices[0].message.content'
        ;;
    models)
        curl -s "http://$LMSTUDIO_HOST:$LMSTUDIO_PORT/v1/models" | jq
        ;;
    set-host)
        echo "export LMSTUDIO_HOST=$2" >> ~/.bashrc
        echo "Host set to $2"
        ;;
    *)
        echo "Usage: lmstudio [chat|models|set-host]"
        echo "  chat <message>  - Send chat message"
        echo "  models          - List available models"
        echo "  set-host <ip>   - Set LM Studio host IP"
        ;;
esac
```

### 1.3 OpenAI API

```bash
# Shell script: ~/.local/bin/openai
#!/data/data/com.termux/files/usr/bin/bash
# OpenAI API wrapper

OPENAI_KEY="${OPENAI_API_KEY:-$(cat ~/.openai/key 2>/dev/null)}"

case "$1" in
    chat)
        shift
        curl -s "https://api.openai.com/v1/chat/completions" \
            -H "Authorization: Bearer $OPENAI_KEY" \
            -H "Content-Type: application/json" \
            -d "{
                \"model\": \"gpt-4\",
                \"messages\": [{\"role\": \"user\", \"content\": \"$*\"}]
            }" | jq -r '.choices[0].message.content'
        ;;
    set-key)
        mkdir -p ~/.openai
        echo "$2" > ~/.openai/key
        chmod 600 ~/.openai/key
        echo "Key saved"
        ;;
    *)
        echo "Usage: openai [chat|set-key]"
        ;;
esac
```

### 1.4 Ollama (Local Models)

```bash
# Shell script: ~/.local/bin/ollama-remote
#!/data/data/com.termux/files/usr/bin/bash
# Connect to Ollama running on local network or localhost

OLLAMA_HOST="${OLLAMA_HOST:-localhost}"
OLLAMA_PORT="${OLLAMA_PORT:-11434}"

case "$1" in
    run)
        shift
        MODEL="$1"
        shift
        curl -s "http://$OLLAMA_HOST:$OLLAMA_PORT/api/generate" \
            -d "{\"model\": \"$MODEL\", \"prompt\": \"$*\", \"stream\": false}" \
            | jq -r '.response'
        ;;
    list)
        curl -s "http://$OLLAMA_HOST:$OLLAMA_PORT/api/tags" | jq '.models[].name'
        ;;
    pull)
        curl -s "http://$OLLAMA_HOST:$OLLAMA_PORT/api/pull" \
            -d "{\"name\": \"$2\"}"
        ;;
    *)
        echo "Usage: ollama-remote [run|list|pull]"
        ;;
esac
```

---

## Phase 2: Local Model Support

Run models directly in Termux/MobileCLI.

### 2.1 llama.cpp Installation

```bash
# Install script: install-llama
#!/data/data/com.termux/files/usr/bin/bash

pkg install -y git cmake clang

cd ~
git clone https://github.com/ggerganov/llama.cpp
cd llama.cpp
make -j4

# Create wrapper
cat > ~/.local/bin/llama << 'EOF'
#!/data/data/com.termux/files/usr/bin/bash
~/llama.cpp/main "$@"
EOF
chmod +x ~/.local/bin/llama

echo "llama.cpp installed. Download a model:"
echo "  wget https://huggingface.co/TheBloke/Llama-2-7B-GGUF/resolve/main/llama-2-7b.Q4_K_M.gguf"
echo "Then run:"
echo "  llama -m llama-2-7b.Q4_K_M.gguf -p 'Hello'"
```

### 2.2 Whisper.cpp (Speech Recognition)

```bash
# Install script: install-whisper
#!/data/data/com.termux/files/usr/bin/bash

cd ~
git clone https://github.com/ggerganov/whisper.cpp
cd whisper.cpp
make -j4

# Download small model
./models/download-ggml-model.sh small

# Create wrapper
cat > ~/.local/bin/whisper << 'EOF'
#!/data/data/com.termux/files/usr/bin/bash
~/whisper.cpp/main -m ~/whisper.cpp/models/ggml-small.bin "$@"
EOF
chmod +x ~/.local/bin/whisper

echo "whisper.cpp installed."
echo "Usage: whisper -f audio.wav"
```

### 2.3 Stable Diffusion (Image Generation)

```bash
# Install script: install-sd
#!/data/data/com.termux/files/usr/bin/bash

pip install diffusers transformers accelerate

# Wrapper script
cat > ~/.local/bin/sd << 'EOF'
#!/data/data/com.termux/files/usr/bin/python
import sys
from diffusers import StableDiffusionPipeline
import torch

prompt = " ".join(sys.argv[1:])
pipe = StableDiffusionPipeline.from_pretrained(
    "runwayml/stable-diffusion-v1-5",
    torch_dtype=torch.float16
)
pipe = pipe.to("cpu")  # Mobile doesn't have CUDA
image = pipe(prompt).images[0]
image.save("output.png")
print("Saved to output.png")
EOF
chmod +x ~/.local/bin/sd

echo "Stable Diffusion installed (CPU mode - slow but works)"
```

---

## Phase 3: Unified AI Command

One command to access all AI services.

### 3.1 Master AI Script

```bash
# ~/.local/bin/ai
#!/data/data/com.termux/files/usr/bin/bash
# Unified AI interface for MobileCLI

CONFIG_DIR="$HOME/.mobilecli/ai"
mkdir -p "$CONFIG_DIR"

show_help() {
    cat << 'EOF'
MobileCLI AI Hub - Access any AI from terminal

USAGE:
    ai <service> <command> [args...]

SERVICES:
    claude      - Claude Code CLI (default)
    hf          - Hugging Face API
    openai      - OpenAI API (GPT-4, etc.)
    lmstudio    - LM Studio (local network)
    ollama      - Ollama (local network)
    llama       - llama.cpp (local)
    whisper     - whisper.cpp (local speech-to-text)
    sd          - Stable Diffusion (local image gen)

EXAMPLES:
    ai claude               # Start Claude Code
    ai hf run gpt2 "Hello"  # Run Hugging Face model
    ai openai chat "Hi"     # Chat with GPT-4
    ai lmstudio chat "Hi"   # Chat with LM Studio
    ai ollama run llama2 "Hi"  # Run Ollama model
    ai llama -m model.gguf -p "Hello"  # Run local llama
    ai whisper -f audio.wav # Transcribe audio
    ai sd "a cat in space"  # Generate image

CONFIG:
    ai config               # Show current configuration
    ai config set <key> <value>  # Set config value

EOF
}

case "$1" in
    claude|"")
        shift
        claude "$@"
        ;;
    hf)
        shift
        hf "$@"
        ;;
    openai)
        shift
        openai "$@"
        ;;
    lmstudio)
        shift
        lmstudio "$@"
        ;;
    ollama)
        shift
        ollama-remote "$@"
        ;;
    llama)
        shift
        llama "$@"
        ;;
    whisper)
        shift
        whisper "$@"
        ;;
    sd)
        shift
        sd "$@"
        ;;
    config)
        shift
        case "$1" in
            set)
                echo "$2=$3" >> "$CONFIG_DIR/config"
                echo "Set $2=$3"
                ;;
            *)
                cat "$CONFIG_DIR/config" 2>/dev/null || echo "No config set"
                ;;
        esac
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        echo "Unknown service: $1"
        show_help
        exit 1
        ;;
esac
```

---

## Phase 4: BootstrapInstaller Integration

Add these scripts to BootstrapInstaller.kt so they're installed automatically.

### 4.1 New Install Function

```kotlin
private fun installAiTools() {
    // Create ~/.local/bin if not exists
    val localBin = File(homeDir, ".local/bin")
    localBin.mkdirs()

    // Add to PATH in .bashrc if not present
    val bashrc = File(homeDir, ".bashrc")
    if (!bashrc.readText().contains(".local/bin")) {
        bashrc.appendText("\nexport PATH=\"\$HOME/.local/bin:\$PATH\"\n")
    }

    // Write ai script
    writeScript(localBin, "ai", AI_SCRIPT)
    writeScript(localBin, "hf", HF_SCRIPT)
    writeScript(localBin, "openai", OPENAI_SCRIPT)
    writeScript(localBin, "lmstudio", LMSTUDIO_SCRIPT)
    writeScript(localBin, "ollama-remote", OLLAMA_SCRIPT)
    writeScript(localBin, "install-llama", INSTALL_LLAMA_SCRIPT)
    writeScript(localBin, "install-whisper", INSTALL_WHISPER_SCRIPT)
}

private fun writeScript(dir: File, name: String, content: String) {
    val script = File(dir, name)
    script.writeText(content)
    script.setExecutable(true)
}
```

---

## Phase 5: DITTO AI Dashboard (Optional)

WebView UI showing all connected AI services.

```html
<!-- ~/.mobilecli/ui/ai-hub.html -->
<!DOCTYPE html>
<html>
<head>
    <style>
        body {
            background: #1a1a2e;
            color: white;
            font-family: system-ui;
            padding: 20px;
        }
        .service {
            background: #16213e;
            border-radius: 10px;
            padding: 15px;
            margin: 10px 0;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        .service.connected { border-left: 4px solid #4ade80; }
        .service.disconnected { border-left: 4px solid #f87171; }
        .btn {
            background: #e94560;
            border: none;
            padding: 8px 16px;
            border-radius: 5px;
            color: white;
            cursor: pointer;
        }
        h1 { color: #e94560; }
    </style>
</head>
<body>
    <h1>AI Hub</h1>

    <div class="service connected">
        <div>
            <strong>Claude Code</strong>
            <div style="font-size: 12px; color: #888;">API Connected</div>
        </div>
        <button class="btn" onclick="MobileCLI.runCmd('claude')">Launch</button>
    </div>

    <div class="service disconnected" id="hf-service">
        <div>
            <strong>Hugging Face</strong>
            <div style="font-size: 12px; color: #888;">Not configured</div>
        </div>
        <button class="btn" onclick="MobileCLI.runCmd('hf login')">Setup</button>
    </div>

    <div class="service disconnected" id="lmstudio-service">
        <div>
            <strong>LM Studio</strong>
            <div style="font-size: 12px; color: #888;">Network</div>
        </div>
        <button class="btn" onclick="configureLMStudio()">Configure</button>
    </div>

    <div class="service disconnected" id="llama-service">
        <div>
            <strong>llama.cpp</strong>
            <div style="font-size: 12px; color: #888;">Local</div>
        </div>
        <button class="btn" onclick="MobileCLI.runCmd('install-llama')">Install</button>
    </div>

    <script>
        function configureLMStudio() {
            const ip = prompt("LM Studio IP address:", "192.168.1.100");
            if (ip) {
                MobileCLI.runCmd(`lmstudio set-host ${ip}`);
                MobileCLI.toast("LM Studio configured");
            }
        }

        // Check service status on load
        // (would need JS bridge methods to check)
    </script>
</body>
</html>
```

---

## Implementation Order

| Step | What | Requires App Rebuild? |
|------|------|----------------------|
| 1 | Add shell scripts manually to test | NO |
| 2 | Test each AI connection | NO |
| 3 | Add to BootstrapInstaller | YES (v90) |
| 4 | Add AI Hub DITTO page | YES (v90) |
| 5 | Test full integration | NO |

---

## What This Enables

After implementation:

```bash
# All of these work from MobileCLI terminal:

ai claude                    # Claude Code (existing)
ai hf run bert-base "Hello"  # Hugging Face
ai openai chat "Explain X"   # GPT-4
ai lmstudio chat "Hello"     # Local network LM Studio
ai ollama run llama2 "Hi"    # Local network Ollama
ai llama -m model.gguf -p "X" # Local llama.cpp
ai whisper -f audio.wav      # Local speech-to-text

# Plus DITTO dashboard to manage all services
```

**One environment. All AI. No limits.**

---

## Files to Modify

| File | Changes |
|------|---------|
| BootstrapInstaller.kt | Add `installAiTools()` function, script constants |
| MainActivity.kt | Add JS bridge for AI Hub status checks (optional) |
| BOOTSTRAP_VERSION | Increment to trigger reinstall |

---

## Review Questions

Before implementing:

1. **Which AI services do you want first?** (Can prioritize)
2. **Want the DITTO AI Hub dashboard?** (Optional visual)
3. **Any specific models you want pre-configured?**
4. **Should local model installers run automatically or on-demand?**

---

**Ready for your review before any code changes.**
