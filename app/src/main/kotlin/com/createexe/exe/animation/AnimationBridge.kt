package com.createexe.exe.animation
import kotlin.math.sin
object AnimationBridge {
    private var speakPhase = 0.0
    private var idlePhase = 0.0
    private var blinkTimer = 0.0
    private var blinkCooldown = 3000.0
    fun tickJaw(deltaMs: Float, isSpeaking: Boolean): Float {
        if (!isSpeaking) return 0f
        speakPhase += deltaMs * 0.003
        return ((sin(speakPhase) + 1.0) / 2.0).toFloat().coerceIn(0f, 1f)
    }
    fun tickIdleBreath(deltaMs: Float): Float {
        idlePhase += deltaMs * 0.0008
        return (sin(idlePhase) * 0.004f).toFloat()
    }
    fun tickBlink(deltaMs: Float): Float {
        blinkTimer += deltaMs
        if (blinkTimer >= blinkCooldown) {
            blinkTimer = 0.0
            blinkCooldown = 2000.0 + (Math.random() * 4000.0)
            return 1.0f
        }
        return 0f
    }
}
