package com.createexe.exe.service
import android.app.*
import android.content.*
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.*
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.createexe.exe.core.*
import com.createexe.exe.tts.TtsEngine

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private var overlay: View? = null
    private var tts: TtsEngine? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        tts = TtsEngine(applicationContext)
        startForeground(1, NotificationCompat.Builder(this, "exe").setContentTitle("EXE Active").setSmallIcon(android.R.drawable.star_on).build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        val tv = TextView(this).apply { text = "EXE Node 1"; setTextColor(0xFF00FFFF.toInt()) }
        wm.addView(tv, params)
        overlay = tv
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        overlay?.let { wm.removeView(it) }
        tts?.destroy()
        super.onDestroy()
    }
}
