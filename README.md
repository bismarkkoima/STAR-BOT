# STAR-BOT — Offline Personal AI Assistant for Android

**Author: Bismark** · Kabarak University, Kenya · [LICENSE](LICENSE) · [Third-party notices](NOTICE.md)

A personal, always-on Android assistant that:
- Understands natural language (typed or voice)
- Decides which **tool/action** to run (reminder, open app, search, etc.)
- Executes that action via **Tasker** (device control) or native Android APIs
- Runs its brain **fully offline** on an on-device Gemma model (MediaPipe) — no internet or API key needed
- Can optionally use a cloud model when online (`OFFLINE_ONLY=false`)

This is built for personal use on your own device — not for Play Store distribution —
so it intentionally uses Tasker + Accessibility-style automation rather than a
from-scratch automation engine.

## How it fits together

```
 You (voice/text)
        │
        ▼
 [Android App]  ── holds chat UI, decides offline vs online, calls LLM
        │
        ▼
 [LLM call] ── cloud (Claude API) when online, local (Gemma via MediaPipe) when offline
        │            returns structured JSON: { "tool": "...", "args": {...} }
        ▼
 [Action Dispatcher] ── maps tool name -> Android Intent OR Tasker task trigger
        │
        ├── Native Android Intent (calendar, SMS, alarms, sharing) — preferred, no Tasker needed
        │
        └── Tasker Task (via `net.dinglisch.android.tasker.ACTION_TASK` broadcast
            or Tasker's HTTP Server plugin) — for anything needing UI automation
            or apps with no public API
        │
        ▼
 Result fed back into the conversation (success/failure, any returned data)
```

## Repo structure

```
PersonalAI-Assistant/
├── android-app/              ← open THIS folder in Android Studio (Gradle root)
│   ├── app/
│   │   ├── build.gradle.kts
│   │   └── src/main/
│   │       ├── AndroidManifest.xml
│   │       └── java/com/personalai/assistant/
│   │           ├── MainActivity.kt        (chat UI)
│   │           ├── LlmBridge.kt           (offline-first routing, parses tool call)
│   │           ├── LocalModel.kt          (on-device Gemma via MediaPipe)
│   │           ├── ActionDispatcher.kt    (tool call -> Intent or Tasker)
│   │           ├── ToolSchema.kt          (loads tools.json/system_prompt.md)
│   │           ├── WakeWordService.kt     (always-listening "Hey Assistant")
│   │           └── VoicePipeline.kt       (STT -> LlmBridge -> TTS)
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   ├── gradle.properties
│   └── local.properties.example  ← copy to local.properties, add your keys (gitignored)
├── config/tools.json          ← what the assistant can do (bundled as an asset, single source of truth)
├── prompts/system_prompt.md   ← how the LLM is told to use those tools
├── tasker/tasks/               ← step-by-step specs for each Tasker task to build
├── docs/                       ← setup guide, offline model, voice setup
├── LICENSE                     ← MIT, (c) Bismark
├── NOTICE.md                   ← third-party components and their licenses
└── .gitignore
```

## Folder guide

| Folder | Purpose |
|---|---|
| `android-app/` | Kotlin/Jetpack Compose app — chat UI, LLM bridge, action dispatcher, voice layer |
| `config/tools.json` | The single source of truth: every action the assistant is allowed to take, as a function-calling schema |
| `prompts/system_prompt.md` | System prompt that teaches the LLM your tools and how to respond |
| `tasker/tasks/` | Human-readable spec for each Tasker Profile/Task you need to build (Tasker configs aren't plain-text friendly, so these are step-by-step build docs, not XML you import) |
| `docs/` | Setup guide, voice/wake-word setup, offline model notes, permissions checklist |

## Build order (recommended)

1. Read `docs/00_setup_guide.md`
2. Build 3–5 Tasker tasks from `tasker/tasks/` manually in the Tasker app
3. Open `android-app/` in Android Studio, fill in your API key, run it on your phone
4. Test the loop: type a command → app calls Claude → Tasker task fires
5. (Optional) Set up the always-listening wake word — `docs/02_voice_wake_word.md`
6. Install the offline model — `docs/01_offline_model.md` (do this before step 3 if you want it fully offline)
7. Expand `config/tools.json` as you want more actions

## Getting this onto GitHub

This is a **personal** project with API keys involved — make the repo
**private** on GitHub. `local.properties` (your actual keys) is gitignored and
will never be committed; only `local.properties.example` (placeholders) goes
up.

```bash
cd PersonalAI-Assistant
git init
git add .
git commit -m "Initial commit: STAR-BOT offline AI assistant"
git branch -M main
git remote add origin <your-private-repo-url>
git push -u origin main
```

Double-check before your first push:
```bash
git status   # local.properties should NOT appear here — if it does, check .gitignore
```
