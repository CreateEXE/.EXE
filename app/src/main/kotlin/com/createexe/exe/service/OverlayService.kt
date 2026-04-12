package com.createexe.exe.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.createexe.exe.MainActivity
import com.createexe.exe.system.SystemClockModule
import com.createexe.exe.tts.TtsEngine

class OverlayService : Service() {
    private val CHANNEL_ID = "exe_agent_channel"
    private lateinit var tts: TtsEngine

    override fun onCreate() {
        super.onCreate()
        tts = TtsEngine(this)
        showForegroundNotification()
        
        // Initial system check
        val time = SystemClockModule.nowHuman()
        tts.speak("System initialized at $time. EXE is online.")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showForegroundNotification() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(
                CHANNEL_ID, "EXE Agent", NotificationManager.IMPORTANCE_LOW
            ))
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("× ./.EXE Active")
            .setContentText("Edge Agent logic and 3D environment running.")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        tts.destroy()
        super.onDestroy()
    }
}
