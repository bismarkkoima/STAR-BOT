package com.personalai.assistant

import ai.picovoice.porcupine.Porcupine
import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineManagerCallback
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Always-on foreground service: listens for the wake word ("Hey Assistant" by
 * default — you record/choose the exact phrase in the Picovoice console, see
 * docs/02_voice_wake_word.md), then hands off to VoicePipeline for
 * speech-to-text -> LLM -> action -> text-to-speech.
 *
 * Trade-off you're accepting by using this service: continuous mic access,
 * a persistent notification (Android requirement, can't be hidden), and some
 * battery/CPU draw. Porcupine's wake-word model itself is tiny and efficient —
 * the ongoing cost is mostly "a service exists," not heavy computation.
 */
class WakeWordService : Service() {

    private var porcupineManager: PorcupineManager? = null
    private lateinit var voicePipeline: VoicePipeline

    companion object {
        const val CHANNEL_ID = "wake_word_channel"
        const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        voicePipeline = VoicePipeline(applicationContext)
        startForeground(NOTIFICATION_ID, buildNotification("Listening for wake word..."))
        startWakeWordDetection()
    }

    private fun startWakeWordDetection() {
        val callback = PorcupineManagerCallback { _ ->
            // Wake word heard — pause wake-word listening, run the full voice turn,
            // then resume once done.
            porcupineManager?.stop()
            voicePipeline.runVoiceTurn(
                onFinished = { porcupineManager?.start() }
            )
        }

        // NOTE: "Hey Assistant" is not a Porcupine built-in keyword — built-ins are
        // things like "Hey Google", "Alexa", "Computer", "Jarvis", "Picovoice".
        // For a custom phrase, train a .ppn keyword file for free at
        // console.picovoice.ai and drop it in app/src/main/assets/.
        // Using a built-in keyword ("Picovoice") below as a placeholder so this
        // compiles out of the box — swap for your custom keyword when ready.
        porcupineManager = PorcupineManager.Builder()
            .setAccessKey(BuildConfig.PICOVOICE_ACCESS_KEY)
            .setKeyword(Porcupine.BuiltInKeyword.PICOVOICE)
            .setSensitivity(0.6f)
            .build(applicationContext, callback)

        porcupineManager?.start()
    }

    private fun buildNotification(text: String): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Assistant listening", NotificationManager.IMPORTANCE_LOW)
            )
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Personal Assistant")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        porcupineManager?.stop()
        porcupineManager?.delete()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
