package com.createexe.exe.tts
import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import com.createexe.exe.core.*
import kotlinx.coroutines.*
import java.util.Locale

class TtsEngine(context: Context) {
    private var tts: TextToSpeech? = null
    private var isReady = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.setLanguage(Locale.US)
                isReady = true
            }
        }
        NodeEngine.subscribe(scope, { it.type == EventType.TTS_SPEAK }) { event ->
            val text = event.payload["text"] as? String ?: return@subscribe
            if (isReady) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }
    fun destroy() { tts?.shutdown() }
}
