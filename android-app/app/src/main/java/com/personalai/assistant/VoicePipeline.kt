package com.personalai.assistant

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * One full voice turn: listen (STT) -> LlmBridge -> speak reply (TTS) -> dispatch action.
 * Reuses the exact same LlmBridge/ActionDispatcher as the typed-chat path in
 * MainActivity, so the LLM/tool logic never has to know whether the command
 * came from typing or speaking.
 */
class VoicePipeline(private val context: Context) {

    private val bridge = LlmBridge(context)
    private val dispatcher = ActionDispatcher(context)
    private val scope = CoroutineScope(Dispatchers.Main)

    private var tts: TextToSpeech? = TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
        }
    }

    fun runVoiceTurn(onFinished: () -> Unit) {
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle) {
                val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val heard = matches?.firstOrNull()
                recognizer.destroy()
                if (heard.isNullOrBlank()) {
                    speak("Sorry, I didn't catch that.") { onFinished() }
                    return
                }
                scope.launch {
                    val result = bridge.handleUserMessage(heard)
                    speak(result.say) {
                        if (result.tool != "answer_only") {
                            dispatcher.dispatch(result.tool, result.args)
                        }
                        onFinished()
                    }
                }
            }

            override fun onError(error: Int) {
                recognizer.destroy()
                onFinished()
            }

            // Unused callbacks
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        recognizer.startListening(intent)
    }

    private fun speak(text: String, onDone: () -> Unit) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utterance_id")
        // Simple approach: TTS doesn't block, so give it a moment. For production,
        // use UtteranceProgressListener.onDone() instead of a fixed delay.
        android.os.Handler(context.mainLooper).postDelayed({ onDone() }, 2500)
    }
}
