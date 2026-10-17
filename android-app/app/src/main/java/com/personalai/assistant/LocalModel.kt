package com.personalai.assistant

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * On-device LLM (Gemma via Google's MediaPipe LLM Inference API).
 * Runs fully offline: no network, no API key, nothing leaves the phone.
 *
 * The model file is NOT bundled in the app or in git (it is 500MB+ and has its own
 * license). Copy it onto the phone once — see docs/01_offline_model.md.
 */
class LocalModel(private val context: Context) {

    private val mutex = Mutex()
    private var engine: LlmInference? = null

    companion object {
        const val MODEL_FILE_NAME = "model.task"
    }

    /** Where the model file must be placed on the phone. */
    fun modelFile(): File =
        File(context.getExternalFilesDir(null), "models/$MODEL_FILE_NAME")

    fun isModelInstalled(): Boolean = modelFile().let { it.exists() && it.length() > 0 }

    /** Loads the model on first use (takes a few seconds), then reuses it. */
    private fun engine(): LlmInference {
        engine?.let { return it }
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelFile().absolutePath)
            .setMaxTokens(768)
            .setTopK(40)
            .setTemperature(0.3f)
            .build()
        return LlmInference.createFromOptions(context, options).also { engine = it }
    }

    /** Blocking generation — call from a background dispatcher. */
    suspend fun generate(prompt: String): String = mutex.withLock {
        engine().generateResponse(prompt)
    }

    fun close() {
        engine?.close()
        engine = null
    }
}
