package com.onisong.exe.core

// Every AI must reference this interface so they can "talk"
interface SystemBridge {
    fun sendSubjective(input: String) // Hemisphere A (Actor)
    fun receiveObjective(output: String) // Hemisphere B (Auditor)
    fun updateUI(state: UIState) // The Visuals
}
