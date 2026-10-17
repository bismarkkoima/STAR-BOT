# Tasker Tasks

Tasker configs are stored as XML but aren't really hand-editable, so instead of
shipping broken XML, each file here is a precise spec for a task to build
inside the Tasker app. Budget ~10-15 min per task the first time.

## Prerequisites (do this once)
1. Install **Tasker** from Play Store.
2. Install **AutoTools** and **AutoInput** (Tasker plugins by same developer, João Dias).
3. In Tasker: **☰ menu → Preferences → Misc → enable "Allow External Access."**
   This lets your app fire tasks via broadcast intent.
4. Every task below should be triggered by **Profile → Event → Plugin → "AutoTools" (or Task) → Task Broadcast**
   with the task name matching exactly what's in `config/tools.json` under `tasker_task_name`.
   Simplest setup: create the Task first, then add an Event profile:
   `Plugin > AutoTools > Task Broadcast Receiver` and just point it at the task —
   OR simpler still, just name your Task exactly `AI_CreateNote` etc. and rely on
   Tasker's built-in behavior of accepting broadcasts addressed to a task by name
   (Tasker does this natively via `net.dinglisch.android.tasker.ACTION_TASK` —
   no profile needed, just the task existing with that exact name).

## Reading the incoming args
Your Android app sends the tool's `args` as a single JSON string in `%par1`.
In each Tasker task, start with:
- **Action: AutoTools → Json Read**, Variable Name = `%par1`, and pull out each
  field you need (e.g. `text`, `contact`, `message`) into Tasker local variables.

## Tasks to build

- `AI_CreateNote.md` — save a note
- `AI_SearchInApp.md` — open an app and search inside it
- `AI_SendMessage.md` — send SMS or WhatsApp message
- `AI_ReadNotifications.md` — read back recent notifications

Start with `AI_CreateNote` — it's the simplest and proves the whole pipeline
(app → broadcast → Tasker → result) works before you tackle UI automation ones.
