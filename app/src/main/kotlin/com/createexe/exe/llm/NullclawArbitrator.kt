package com.createexe.exe.llm

import android.util.Log

enum class Hemisphere { LEFT, RIGHT }

/**
 * NullclawArbitrator manages the routing of prompts to either the Left (Factual)
 * or Right (Persona) hemisphere based on keyword weighting and context intent.
 */
class NullclawArbitrator(
    private val leftEngine: LlamaEngine,
    private val rightEngine: LlamaEngine
) {
    private val TAG = "NullclawArbitrator"
    
    // Factual triggers map to Left Hemisphere
    private val factualKeywords = setOf("what", "how", "define", "calculate", "time", "date", "weather")
    
    // Emotional/Persona triggers map to Right Hemisphere
    private val personaKeywords = setOf("feel", "think", "love", "hate", "you", "avatar", "opinion")

    fun processQuery(prompt: String): String {
        val hemisphere = determineHemisphere(prompt)
        Log.i(TAG, "Routing query to $hemisphere hemisphere.")

        return when (hemisphere) {
            Hemisphere.LEFT -> {
                val sysPrompt = "[System: You are the Logic Auditor. Answer factually.]\nUser: $prompt\nAI:"
                leftEngine.generate(sysPrompt)
            }
            Hemisphere.RIGHT -> {
                val sysPrompt = "[System: You are the Creative Core Fait. Respond with personality.]\nUser: $prompt\nAI:"
                rightEngine.generate(sysPrompt)
            }
        }
    }

    private fun determineHemisphere(prompt: String): Hemisphere {
        val words = prompt.lowercase().split("\\s+".toRegex())
        
        var leftScore = 0
        var rightScore = 0

        for (word in words) {
            if (factualKeywords.contains(word)) leftScore++
            if (personaKeywords.contains(word)) rightScore++
        }

        // Bias towards persona if scores are tied, to maintain the AI's identity
        return if (leftScore > rightScore) Hemisphere.LEFT else Hemisphere.RIGHT
    }
}
