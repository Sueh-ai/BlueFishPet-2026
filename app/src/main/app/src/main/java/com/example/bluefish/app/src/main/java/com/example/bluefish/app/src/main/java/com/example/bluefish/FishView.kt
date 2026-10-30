package com.example.bluefish

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

class FishView(context: Context, private val host: Host) : View(context) {

    enum class Region { HEAD, BODY, TAIL, BELLY }

    interface Host {
        fun moveWindowBy(dx: Float, dy: Float)
        fun onFishTapInRegion(region: Region)
        fun onFishLongPress()
        fun onDragEnd()
    }

    private val d = resources.displayMetrics.density
    private fun dp(v: Float) = v * d

    private val fillBlue = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#4AA3FF") }
    private val strokeDark = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1E6FD9"); style = Paint.Style.STROKE; strokeWidth = dp(2.5f)
    }
    private val strokeLight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8CCBFF"); style = Paint.Style.STROKE; strokeWidth = dp(2f)
    }
    private val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val pupilPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#112233") }
    private val blushPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFB3C7") }
    private val bubbleBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val bubbleStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4AA3FF"); style = Paint.Style.STROKE; strokeWidth = dp(1.5f)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#112233")
        textSize = dp(13f)
    }

    private var bubbleText: String? = null
    private var bubbleStart = 0L
    private var bubbleDuration = 4500L

    private var nextBlinkAt = 0L
    private var blinkEndAt = 0L

    private var lastX = 0f
    private var lastY = 0f
    private var downX = 0f
    private var downY = 0f
    private var dragging = false
    private var longFired = false

    private val longPressRunnable = Runnable {
        if (!dragging) {
            longFired = true
            host.onFishLongPress()
        }
    }

    init {
        nextBlinkAt = System.currentTimeMillis() + 2500
    }

    fun showBubble(text: String, durationMs: Long = 4500L) {
        bubbleText = text
        bubbleStart = System.currentTimeMillis()
        bubbleDuration = durationMs
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = System.currentTimeMillis()
        val t = SystemClock.uptimeMillis() / 1000f
        val bob = sin(t * 1.5f) * dp(4f)

        val cx = dp(140f)
        val cy = dp(235f) + bob

        drawFish(canvas, cx, cy, now)
        drawBubble(canvas, now)

        postInvalidateDelayed(33)
    }

    private fun drawFish(canvas: Canvas, cx: Float, cy: Float, now: Long) {
        val tail = Path().apply {
            moveTo(cx + dp(58f), cy - dp(20f))
            lineTo(cx + dp(105f), cy - dp(48f))
            lineTo(cx + dp(105f), cy + dp(48f))
            lineTo(cx + dp(58f), cy + dp(20f))
            close()
        }
        canvas.drawPath(tail, fillBlue)
        canvas.drawPath(tail, strokeDark)

        val dorsal = Path().apply {
            moveTo(cx - dp(32f), cy - dp(45f))
            lineTo(cx - dp(8f), cy - dp(82f))
            lineTo(cx + dp(22f), cy - dp(46f))
            close()
        }
        canvas.drawPath(dorsal, fillBlue)
        canvas.drawPath(dorsal, strokeDark)

        val ventral = Path().apply {
            moveTo(cx - dp(22f), cy + dp(44f))
            lineTo(cx + dp(4f), cy + dp(80f))
            lineTo(cx + dp(28f), cy + dp(44f))
            close()
        }
        canvas.drawPath(ventral, fillBlue)
        canvas.drawPath(ventral, strokeDark)

        val body = RectF(cx - dp(70f), cy - dp(50f), cx + dp(70f), cy + dp(50f))
        canvas.drawOval(body, fillBlue)
        canvas.drawOval(body, strokeDark)

        val belly = RectF(cx - dp(45f), cy - dp(5f), cx + dp(48f), cy + dp(48f))
        canvas.drawArc(belly, 200f, 140f, false, strokeLight)

        val blinkScale = if (now < blinkEndAt) 0.12f else 1f
        val eyeR = dp(13f)
        val eyeRy = eyeR * blinkScale
        val leftEye = PointF(cx - dp(44f), cy - dp(14f))
        val rightEye = PointF(cx - dp(8f), cy - dp(14f))
        for (eye in listOf(leftEye, rightEye)) {
            val r = RectF(eye.x - eyeR, eye.y - eyeRy, eye.x + eyeR, eye.y + eyeRy)
            canvas.drawOval(r, white)
            canvas.drawOval(r, strokeDark)
            if (blinkScale > 0.5f) {
                canvas.drawCircle(eye.x + dp(1.5f), eye.y + dp(2f), dp(6f), pupilPaint)
       
