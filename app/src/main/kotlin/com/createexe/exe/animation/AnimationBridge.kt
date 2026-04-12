package com.createexe.exe.animation
import kotlin.math.sin

object AnimationBridge {
    private var speakPhase = 0.0
    private var idlePhase = 0.0

    fun tickJaw(deltaMs: Float, isSpeaking: Boolean): Float {
        if (!isSpeaking) return 0f
        speakPhase += deltaMs * 0.003
        return ((sin(speakPhase) + 1.0) / 2.0).toFloat().coerceIn(0f, 1f)
    }

    fun tickIdleBreath(deltaMs: Float): Float {
        idlePhase += deltaMs * 0.0008
        return (sin(idlePhase) * 0.004).toFloat()
    }
}
