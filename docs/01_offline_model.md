# 01 — Offline Model Setup

STAR-BOT's brain runs **on your phone** using Google's MediaPipe LLM Inference API
and a Gemma model. No internet, no API key, nothing leaves the device.

By default the app is built with `OFFLINE_ONLY=true`, so it never makes a network
request at all. (Set `OFFLINE_ONLY=false` plus a `CLAUDE_API_KEY` in
`local.properties` if you later want cloud help when you're online.)

## 1. Get a model file
Use a small model converted to MediaPipe's `.task` format. A good starting point:

- **Gemma 3 1B instruction-tuned, int4** — about 500 MB, runs on most phones with 6 GB+ RAM.
  Published by the `litert-community` organisation on Hugging Face (search "Gemma3-1B-IT litert-community"
  and pick the `.task` file for int4).
- You must log in to Hugging Face and **accept the Gemma license** before you can download it.
- Phones with 8 GB+ RAM can try a larger Gemma (2B/4B) for better answers — same steps.

## 2. Put it on the phone
Install and open the app once (so Android creates its folder), then from your PC with
USB debugging on:

```bash
adb shell mkdir -p /sdcard/Android/data/com.personalai.assistant/files/models
adb push gemma3-1b-it-int4.task /sdcard/Android/data/com.personalai.assistant/files/models/model.task
```

The file **must be named `model.task`** (the name is set in `LocalModel.kt`). If you
see "The offline model isn't installed yet" in the app, the path above is wrong.

## 3. Use it
Type or speak a command. The first request is slow (a few seconds while the model
loads into memory); after that it stays loaded.

## Tips
- Small models sometimes answer in plain text instead of JSON. `LlmBridge` handles
  this: the text is shown/spoken as a normal reply, so it still works as a chatbot.
- For more reliable tool-calling, use a bigger model, or shorten `config/tools.json`
  to only the tools you actually use.
- Model files are large and have their own licenses — they are gitignored; never commit them.
