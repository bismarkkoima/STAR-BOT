# 02 — Voice / Wake Word Setup ("Hey Assistant")

This makes the app listen continuously and respond to a wake phrase, like Siri
or "Hey Google" — fully on-device, no audio sent anywhere until *after* the
wake word triggers and Claude is called.

## 1. Get a free Picovoice AccessKey
1. Sign up at https://console.picovoice.ai (free personal-use tier).
2. Copy your AccessKey.
3. Paste it into `WakeWordService.kt` → `PICOVOICE_ACCESS_KEY`.
   (Better: move it into `local.properties` like the Claude key — see
   `docs/00_setup_guide.md`.)

## 2. Choose your wake phrase
Porcupine ships a few **built-in** keywords for free with zero setup:
`Alexa, Americano, Blueberry, Bumblebee, Computer, Grapefruit, Grasshopper,
Hey Google, Hey Siri, Jarvis, OK Google, Picovoice, Porcupine, Terminator`

- Fastest path: pick one of these (e.g. `Jarvis` or `Computer`) and set it in
  `WakeWordService.kt` via `Porcupine.BuiltInKeyword.JARVIS`. Works immediately.
- For a fully custom phrase like *"Hey Assistant"*: in the Picovoice console,
  go to **Porcupine → Create custom keyword**, type your phrase, choose
  Android as the platform, and download the generated `.ppn` file. Drop it in
  `app/src/main/assets/`, then in `WakeWordService.kt` use
  `.setKeywordPath("hey_assistant_android.ppn")` instead of `.setKeyword(...)`.
  Free tier covers personal use fine.

## 3. Start the service
The service needs to be started once (e.g. from `MainActivity.onCreate`, or a
"Enable always-listening" toggle in your UI) with:

```kotlin
startForegroundService(Intent(this, WakeWordService::class.java))
```

Also request `RECORD_AUDIO` and `POST_NOTIFICATIONS` permissions at runtime
before starting it — Android won't let the service start without them granted.

## 4. What happens on wake
1. Porcupine detects the phrase → pauses itself.
2. `VoicePipeline` opens Android's `SpeechRecognizer` to transcribe what you say next.
3. Transcribed text goes to `LlmBridge` (same code path as typed chat).
4. Reply is spoken via `TextToSpeech`.
5. If the LLM picked a tool, `ActionDispatcher` fires it (Intent or Tasker).
6. Porcupine resumes listening for the wake word.

## Known trade-offs (read before shipping to your own phone)
- **Persistent notification**: Android requires a visible notification while a
  foreground mic service runs. You can't hide this — it's a privacy safeguard.
- **Battery**: wake-word models are lightweight, but a 24/7 service still costs
  some battery. Consider only enabling it during certain hours, or adding a
  manual on/off toggle rather than starting it automatically on boot.
- **Reliability across Android versions**: some OEMs (Xiaomi, Huawei, aggressive
  battery-saver modes) kill background services. You may need to disable
  battery optimization for this app specifically in Settings.
- **Not a true system-wide Assist replacement**: this triggers only while the
  service is alive, not via a hardware button like Siri's. If you want the
  hold-button invocation too, that's the `VoiceInteractionService` path
  mentioned earlier — can add later, doesn't conflict with this.
