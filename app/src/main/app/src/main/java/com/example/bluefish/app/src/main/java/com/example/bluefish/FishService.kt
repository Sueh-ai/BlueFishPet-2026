package com.example.bluefish

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.EditText
import kotlin.math.hypot
import kotlin.random.Random

class FishService : Service(), FishView.Host {

    private lateinit var wm: WindowManager
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var fishView: FishView
    private val dialogue = Dialogue()

    private var idleRunnable: Runnable? = null
    private var snapAnimator: ValueAnimator? = null

    private var cruising = false
    private var cruiseRunnable: Runnable? = null
    private var cruiseAnimator: ValueAnimator? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        fishView = FishView(this, this)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        val density = resources.displayMetrics.density
        params = WindowManager.LayoutParams(
            (280 * density).toInt(),
            (320 * density).toInt(),
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 60
            y = 400
        }

        wm.addView(fishView, params)
        fishView.postDelayed({ fishView.showBubble(dialogue.greeting(), 5000) }, 500)
        startIdleLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundCompat()
        return START_STICKY
    }

    override fun onDestroy() {
        idleRunnable?.let { fishView.removeCallbacks(it) }
        stopCruise(showTip = false)
        snapAnimator?.cancel()
        try { if (fishView.isAttachedToWindow) wm.removeView(fishView) } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundCompat() {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel("fish_channel", "大肥鱼", NotificationManager.IMPORTANCE_MIN)
            ch.setShowBadge(false)
            nm.createNotificationChannel(ch)
        }
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(this, "fish_channel")
        else @Suppress("DEPRECATION") Notification.Builder(this)

        builder.setContentTitle("蓝色大肥鱼")
            .setContentText("正在陪你摸鱼～")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)

        val n = builder.build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, n)
        }
    }

    private fun startIdleLoop() {
        val r = object : Runnable {
            override fun run() {
                if (Random.nextInt(100) < 70) fishView.showBubble(dialogue.idle(), 5000)
                fishView.postDelayed(this, 20000L + Random.nextLong(20000L))
            }
        }
        idleRunnable = r
        fishView.postDelayed(r, 25000L)
    }

    override fun moveWindowBy(dx: Float, dy: Float) {
        snapAnimator?.cancel()
        cruiseAnimator?.cancel()
        params.x += dx.toInt()
        params.y += dy.toInt()
        try { wm.updateViewLayout(fishView, params) } catch (_: Exception) {}
    }

    override fun onDragEnd() {
        val real = Point()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealSize(real)
        val screenW = real.x
        val screenH = real.y
        val snap = (40 * resources.displayMetrics.density).toInt()
        val winW = params.width
        val winH = params.height

        var tx = params.x
        var ty = params.y

        if (params.x < snap) tx = 0
        else if (params.x + winW > screenW - snap) tx = screenW - winW

        if (params.y < snap) ty = 0
        else if (params.y + winH > screenH - snap) ty = screenH - winH

        tx = tx.coerceIn(0, (screenW - winW).coerceAtLeast(0))
        ty = ty.coerceIn(0, (screenH - winH).coerceAtLeast(0))

        if (tx != params.x || t
