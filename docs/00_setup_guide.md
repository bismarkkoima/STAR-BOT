# 00 — Setup Guide

## Prerequisites
- Android Studio (Koala+ recommended)
- A physical Android device (voice/mic features don't work well in emulators) with USB debugging enabled, Android 8.0 (API 26)+
- (Optional) A Claude API key from https://console.anthropic.com — only needed if you set `OFFLINE_ONLY=false`
- The offline model file — see `docs/01_offline_model.md`
- (For the wake-word feature) a free Picovoice AccessKey — see `docs/02_voice_wake_word.md`
- Tasker installed on your device (from Play Store) — see `tasker/tasks/README.md`

## 1. Clone and open
```bash
git clone <your-repo-url>
cd PersonalAI-Assistant/android-app
```
Open the `android-app/` folder (not the repo root) in Android Studio — that's the actual Gradle project.

## 2. Add your secrets
```bash
cp local.properties.example local.properties
```
Edit `local.properties` and fill in:
- `sdk.dir` — path to your Android SDK (Android Studio usually sets this automatically on first sync, but double check)
- `OFFLINE_ONLY` (leave `true` for fully offline)
- `CLAUDE_API_KEY` (optional)
- `PICOVOICE_ACCESS_KEY` (only needed if you're using the wake-word feature)

This file is gitignored — it will never be committed.

## 3. Sync and run
Let Gradle sync (Android Studio will prompt), then run on your physical device.
The app requests `RECORD_AUDIO` and notification permissions at first launch —
accept these or voice features won't work.

## 4. Set up Tasker
Follow `tasker/tasks/README.md`, starting with `AI_CreateNote.md` — build that
one task, test it manually inside Tasker, then test it end-to-end by typing
"note that ..." in the app.

## 5. (Optional) Enable always-listening wake word
Follow `docs/02_voice_wake_word.md`.

## Editing the assistant's abilities
- `config/tools.json` — add/remove/modify what the assistant can do. This is
  bundled directly into the app as an asset (see the `sourceSets` block in
  `android-app/app/build.gradle.kts`), so editing this file and rebuilding is
  all you need — no need to touch Kotlin code for new simple tools that map to
  an existing dispatch type.
- `prompts/system_prompt.md` — how the LLM is instructed to use those tools.
- For a genuinely new *kind* of action (not just a new Tasker task, but a new
  dispatch mechanism), add a case in `ActionDispatcher.kt`.

## Troubleshooting
- **Tasker task not firing**: confirm Tasker → Preferences → Misc → "Allow
  External Access" is enabled, and that your task name matches
  `tasker_task_name` in `tools.json` exactly (case-sensitive).
- **Wake word service gets killed in the background**: check your device's
  battery optimization settings and exclude this app (varies by manufacturer —
  Xiaomi/Huawei/OnePlus are especially aggressive about this).
- **Gradle can't find `local.properties`**: make sure you copied the example
  file inside `android-app/`, not the repo root.
