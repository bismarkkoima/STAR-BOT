package com.personalai.assistant

import android.content.Context
import org.json.JSONObject

/**
 * Loads config/tools.json and prompts/system_prompt.md, which should be copied
 * into app/src/main/assets/ (see docs/00_setup_guide.md) so they're bundled
 * with the app and easy to edit without touching Kotlin code.
 */
object ToolSchema {
    fun load(context: Context): JSONObject {
        val text = context.assets.open("tools.json").bufferedReader().use { it.readText() }
        return JSONObject(text)
    }

    fun loadSystemPrompt(context: Context): String {
        return context.assets.open("system_prompt.md").bufferedReader().use { it.readText() }
    }
}
