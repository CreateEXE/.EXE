package com.createexe.exe.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.WindowManager
import com.createexe.exe.utils.TtsEngine
import com.createexe.exe.utils.AnimationBridge
import com.google.ar.sceneform.SceneView

class OverlayService : Service() {
    private lateinit var sceneView: SceneView
    private lateinit var ttsEngine: TtsEngine
    private lateinit var animationBridge: AnimationBridge

    companion object {
        const val ACTION_START = "com.createexe.exe.ACTION_START"
        const val ACTION_STOP = "com.createexe.exe.ACTION_STOP"
        const val EXTRA_VRM_URI = "vrm_uri"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // Initialize SceneView, TtsEngine, AnimationBridge, etc.
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                handleStartAction(intent)
            }
            ACTION_STOP -> {
                handleStopAction()
            }
        }
        return START_STICKY
    }

    private fun handleStartAction(intent: Intent) {
        val vrmUri = intent.getStringExtra(EXTRA_VRM_URI)
        if (vrmUri != null) {
            loadVrmModel(vrmUri)
            // Start foreground service with a notification
        }
    }

    private fun loadVrmModel(vrmUri: String) {
        // Logic to load VRM/GLB models into SceneView
    }

    private fun handleStopAction() {
        stopForeground(true)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "OverlayServiceChannel",
                "Overlay Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}

