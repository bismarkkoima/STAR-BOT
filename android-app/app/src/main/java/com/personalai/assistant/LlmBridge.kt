package com.personalai.assistant

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.OffsetDateTime

data class LlmResult(val tool: String, val args: Map<String, String>, val say: String)

/**
 * Turns a user message into a structured tool call.
 *
 * Offline-first: the on-device model (LocalModel) is the default brain. The cloud
 * model is only used when BuildConfig.OFFLINE_ONLY is false AND the phone is online.
 * With OFFLINE_ONLY = true (the default) no network request is ever made.
 */
class LlmBridge(private val context: Context) {

    private val client = OkHttpClient()
    private val localModel = LocalModel(context)

    // Pulled from local.properties (gitignored) via BuildConfig — see local.properties.example.
    private val apiKey = BuildConfig.CLAUDE_API_KEY

    private val toolsJson: JSONObject by lazy { ToolSchema.load(context) }
    private val systemPrompt: String by lazy { ToolSchema.loadSystemPrompt(context) }

    suspend fun handleUserMessage(userText: String): LlmResult = withContext(Dispatchers.IO) {
        val useCloud = !BuildConfig.OFFLINE_ONLY && apiKey.isNotBlank() && isOnline()
        try {
            if (useCloud) callCloudModel(userText) else callLocalModel(userText)
        } catch (e: Exception) {
            // Cloud failed (timeout, bad key...)? Fall back to the on-device model.
            if (useCloud) callLocalModel(userText)
            else fallbackResult("Something went wrong running the on-device model: ${e.message}")
        }
    }

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // ---------------------------------------------------------------- offline

    private suspend fun callLocalModel(userText: String): LlmResult {
        if (!localModel.isModelInstalled()) {
            return fallbackResult(
                "The offline model isn't installed yet. Copy it to " +
                    "${localModel.modelFile().absolutePath} — see docs/01_offline_model.md."
            )
        }
        val raw = localModel.generate(buildLocalPrompt(userText))
        return parseToolResponse(raw, plainTextFallback = true)
    }

    /**
     * Small on-device models do better with a SHORT prompt than with the full JSON
     * schema, so the tool list is flattened to one line per tool. Uses Gemma's
     * chat template.
     */
    private fun buildLocalPrompt(userText: String): String {
        val tools = toolsJson.getJSONArray("tools")
        val toolLines = (0 until tools.length()).joinToString("\n") { i ->
            val t = tools.getJSONObject(i)
            val params = t.optJSONObject("parameters")?.keys()?.asSequence()?.joinToString(", ") ?: ""
            "- ${t.getString("name")}($params): ${t.getString("description")}"
        }
        val instructions = """
            You are a personal assistant on the user's phone. Pick ONE tool for the request.
            Reply with ONLY a JSON object, nothing else:
            {"tool": "<tool name>", "args": {<arguments>}, "say": "<one short sentence for the user>"}
            If no device action is needed, use tool "answer_only" with args {"answer": "<your reply>"}.
            Times must be ISO 8601. Current time: ${OffsetDateTime.now()}

            Tools:
            $toolLines
        """.trimIndent()

        return "<start_of_turn>user\n$instructions\n\nRequest: $userText<end_of_turn>\n<start_of_turn>model\n"
    }

    // ----------------------------------------------------------------- online

    private fun callCloudModel(userText: String): LlmResult {
        val fullSystemPrompt = systemPrompt + "\n\nTOOLS:\n" + toolsJson.toString(2) +
            "\n\ncurrent_datetime: " + OffsetDateTime.now().toString()

        val body = JSONObject().apply {
            put("model", "claude-sonnet-4-6")
            put("max_tokens", 512)
            put("system", fullSystemPrompt)
            put("messages", JSONArray().put(
                JSONObject().put("role", "user").put("content", userText)
            ))
        }

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("Cloud API error ${response.code}")
            val respBody = response.body?.string() ?: return fallbackResult()
            val content = JSONObject(respBody).optJSONArray("content") ?: return fallbackResult()
            val text = (0 until content.length())
                .map { content.getJSONObject(it) }
                .firstOrNull { it.optString("type") == "text" }
                ?.optString("text") ?: return fallbackResult()
            return parseToolResponse(text, plainTextFallback = false)
        }
    }

    // ---------------------------------------------------------------- parsing

    /**
     * Models (especially small ones) often wrap JSON in ```json fences or add chatter,
     * so grab the outermost {...}. If the local model just answered in plain text,
     * show that text as the reply instead of an error — it then works as a plain chatbot.
     */
    private fun parseToolResponse(rawText: String, plainTextFallback: Boolean): LlmResult {
        val start = rawText.indexOf('{')
        val end = rawText.lastIndexOf('}')
        try {
            if (start < 0 || end <= start) throw IllegalArgumentException("no JSON")
            val json = JSONObject(rawText.substring(start, end + 1))
            val tool = json.getString("tool")
            val argsJson = json.optJSONObject("args") ?: JSONObject()
            val args = mutableMapOf<String, String>()
            argsJson.keys().forEach { key -> args[key] = argsJson.get(key).toString() }
            val say = json.optString("say", args["answer"] ?: "Done.")
            return LlmResult(tool, args, say)
        } catch (e: Exception) {
            val text = rawText.trim()
            return if (plainTextFallback && text.isNotEmpty()) {
                LlmResult("answer_only", mapOf("answer" to text), text)
            } else {
                fallbackResult("I had trouble understanding that — could you rephrase?")
            }
        }
    }

    private fun fallbackResult(message: String = "Something went wrong.") =
        LlmResult("answer_only", mapOf("answer" to message), message)
}
