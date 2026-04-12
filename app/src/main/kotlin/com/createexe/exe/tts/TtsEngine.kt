package com.createexe.exe.tts
import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.UUID

class TtsEngine(context: Context) {
    private var tts: TextToSpeech? = null
    private var isReady = false
    var onUtteranceDone: ((id: String) -> Unit)? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.setLanguage(Locale.US)
                isReady = true
                tts?.setSpeechRate(0.95f)
                tts?.setPitch(1.05f)
            }
        }
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) { id?.let { onUtteranceDone?.invoke(it) } }
            override fun onError(id: String?) { Log.e("TTS", "Error: $id") }
        })
    }

    fun speak(text: String): String? {
        if (!isReady) return null
        val id = UUID.randomUUID().toString()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
        return id
    }

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
    }
}
