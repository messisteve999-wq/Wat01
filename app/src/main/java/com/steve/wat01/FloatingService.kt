package com.steve.wat01

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.concurrent.thread

class FloatingService : Service() {

    companion object {
        const val ACTION_REFRESH = "refresh"
    }

    private lateinit var wm: WindowManager
    private var bubble: View? = null
    private var panel: View? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        startForeground(1, buildNotif())
        showBubble()
    }

    override fun onStartCommand(i: Intent?, flags: Int, startId: Int): Int {
        if (i?.action == ACTION_REFRESH) refreshPanel()
        return START_STICKY
    }

    override fun onDestroy() {
        bubble?.let { wm.removeView(it) }
        panel?.let { wm.removeView(it) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun params(w: Int, h: Int) = WindowManager.LayoutParams(
        w, h,
        if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        android.graphics.PixelFormat.TRANSLUCENT
    )

    // Bulle : fond noir, deux yeux rouges
    private fun bubbleView() = object : View(this) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(c: Canvas) {
            val r = width / 2f
            p.color = Color.BLACK
            c.drawCircle(r, r, r, p)
            p.color = Color.RED
            c.drawCircle(r * 0.7f, r * 0.8f, r * 0.14f, p)
            c.drawCircle(r * 1.3f, r * 0.8f, r * 0.14f, p)
        }
    }

    private fun showBubble() {
        val v = bubbleView()
        val lp = params(150, 150).apply { gravity = Gravity.TOP or Gravity.START; x = 0; y = 300 }
        var ix = 0; var iy = 0; var tx = 0f; var ty = 0f; var moved = false
        v.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { ix = lp.x; iy = lp.y; tx = e.rawX; ty = e.rawY; moved = false }
                MotionEvent.ACTION_MOVE -> {
                    lp.x = ix + (e.rawX - tx).toInt()
                    lp.y = iy + (e.rawY - ty).toInt()
                    wm.updateViewLayout(v, lp)
                    if (Math.abs(e.rawX - tx) + Math.abs(e.rawY - ty) > 10) moved = true
                }
                MotionEvent.ACTION_UP -> if (!moved) togglePanel()
            }
            true
        }
        wm.addView(v, lp)
        bubble = v
    }

    private fun togglePanel() {
        panel?.let { wm.removeView(it); panel = null; return }
        val lp = params(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        lp.gravity = Gravity.BOTTOM
        panel = buildPanel()
        wm.addView(panel, lp)
    }

    private fun refreshPanel() {
        if (panel != null) { wm.removeView(panel); panel = null; togglePanel() }
    }

    private fun btn(label: String, onClick: () -> Unit) =
        Button(this).apply { text = label; setOnClickListener { onClick() } }

    private fun buildPanel(): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F2111111"))
            setPadding(32, 32, 32, 32)
        }
        val msg = Inbox.last()
        box.addView(TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 15f
            text = msg?.let { "${it.title}\n${it.text}" } ?: "Aucun message synchronisé"
        })
        val out = TextView(this).apply { setTextColor(Color.parseColor("#FF5252")); textSize = 14f }
        box.addView(out)
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(btn("Suggestions") {
            if (msg == null) return@btn
            out.text = "Réflexion..."
            thread {
                val s = try {
                    GeminiClient.suggestions(this, msg.text).joinToString("\n• ", "• ")
                } catch (e: Exception) {
                    "Erreur serveur"
                }
                Handler(Looper.getMainLooper()).post { out.text = s }
            }
        })
        row.addView(btn(if (Prefs.autoReply(this)) "Auto: ON" else "Auto: OFF") {
            Prefs.sp(this).edit().putBoolean("auto", !Prefs.autoReply(this)).apply()
            refreshPanel()
        })
        row.addView(btn("Fermer") { togglePanel() })
        box.addView(row)
        return box
    }

    private fun buildNotif(): Notification {
        val ch = "wat01"
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(NotificationChannel(ch, "Wat01", NotificationManager.IMPORTANCE_MIN))
        }
        return Notification.Builder(this, ch)
            .setContentTitle("Wat01 actif")
            .setSmallIcon(R.drawable.ic_dragon)
            .build()
    }
}
