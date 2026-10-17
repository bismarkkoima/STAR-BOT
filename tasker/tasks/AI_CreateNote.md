# Tasker Task: AI_CreateNote

**Purpose**: proves the whole pipeline works. Build this one first.

## Steps
1. Tasker → Tasks tab → **+** → name it exactly `AI_CreateNote`
2. Add action: **AutoTools → Json Read**
   - Json = `%par1`
   - Variable Array = `%jsonkeys` (default)
   - After running, the note text will be in `%text` if your JSON key is `text`
   - (Simpler alt: skip AutoTools and use Tasker's built-in **Variable Set**
     with `%par1` directly if you only ever pass one plain string instead of JSON —
     up to you; JSON is more future-proof once tasks take multiple args.)
3. Add action: **File → Write File**
   - File: `/storage/emulated/0/Documents/ai_notes.txt`
   - Text: `%text`
   - Append: ON
4. Add action: **Alert → Notify** (optional, so you get confirmation)
   - Title: "Note saved"
   - Text: `%text`

## Test it manually
In Tasker, long-press the task → Run, with a test value, to confirm the note
gets written before wiring it to the app.

## Test it from the app
Once `android-app` is running, type: *"note that I need to renew my passport"*
— the app should call Claude, get back `{"tool":"create_note", "args":{"text":"..."}}`,
broadcast it, and Tasker should write the file.
