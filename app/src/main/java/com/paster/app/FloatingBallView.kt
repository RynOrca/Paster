package com.paster.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.OvershootInterpolator

class FloatingBallView(context: Context) : View(context) {

    private val ballRadius = 24f * resources.displayMetrics.density
    private val edgeMargin = 8f * resources.displayMetrics.density
    private val touchSlop = android.view.ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xCC4CAF50.toInt()
        style = Paint.Style.FILL
        setShadowLayer(8f, 0f, 2f, 0x40000000.toInt())
    }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF4CAF50.toInt()
        style = Paint.Style.FILL
    }

    private var isDragging = false
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var viewStartX = 0
    private var viewStartY = 0
    private var downX = 0f
    private var downY = 0f
    private var totalDrag = 0f

    var onClickAction: (() -> Unit)? = null
    var onLongPressAction: (() -> Unit)? = null

    private fun wmParams() = layoutParams as WindowManager.LayoutParams

    fun createLayoutParams(): WindowManager.LayoutParams {
        val size = (ballRadius * 2 + 16).toInt()
        val screenW = wm.currentWindowMetrics.bounds.width()
        val screenH = wm.currentWindowMetrics.bounds.height()
        return WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenW - size - edgeMargin.toInt()
            y = screenH / 3
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        canvas.drawCircle(cx, cy, ballRadius, bgPaint)

        // Clipboard icon in white
        val pad = ballRadius * 0.28f
        val bodyL = cx - ballRadius * 0.42f
        val bodyR = cx + ballRadius * 0.42f
        val bodyT = cy - ballRadius * 0.12f
        val bodyB = cy + ballRadius * 0.55f
        val cornerR = pad * 0.4f

        // Clipboard body (white rounded rect)
        canvas.drawRoundRect(bodyL, bodyT, bodyR, bodyB, cornerR, cornerR, iconPaint)

        // Top spring clip
        canvas.drawRoundRect(bodyL - pad * 0.25f, cy - ballRadius * 0.38f,
            bodyR + pad * 0.25f, cy - ballRadius * 0.08f, cornerR, cornerR, iconPaint)

        // Text lines (green, inside the body)
        val l = bodyL + pad
        val r = bodyR - pad
        val th = pad * 0.22f
        canvas.drawRoundRect(l, bodyT + pad, r, bodyT + pad + th, th * 0.3f, th * 0.3f, linePaint)
        canvas.drawRoundRect(l, bodyT + pad * 1.9f, l + (r - l) * 0.6f, bodyT + pad * 1.9f + th, th * 0.3f, th * 0.3f, linePaint)
        canvas.drawRoundRect(l, bodyT + pad * 2.8f, l + (r - l) * 0.8f, bodyT + pad * 2.8f + th, th * 0.3f, th * 0.3f, linePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.rawX
        val y = event.rawY

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = x
                downY = y
                dragStartX = x
                dragStartY = y
                val lp = wmParams()
                viewStartX = lp.x
                viewStartY = lp.y
                isDragging = false
                totalDrag = 0f
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = x - downX
                val dy = y - downY
                totalDrag = Math.abs(dx) + Math.abs(dy)

                if (totalDrag > touchSlop) {
                    isDragging = true
                }

                if (isDragging) {
                    val lp = wmParams()
                    val screenH = wm.currentWindowMetrics.bounds.height()
                    lp.x = (viewStartX + (x - dragStartX)).toInt()
                    lp.y = (viewStartY + (y - dragStartY)).toInt()
                        .coerceIn(0, screenH - lp.height)
                    wm.updateViewLayout(this, lp)
                }
            }

            MotionEvent.ACTION_UP -> {
                if (isDragging) {
                    snapToEdge()
                } else {
                    onClickAction?.invoke()
                }
                isDragging = false
            }
        }
        return true
    }

    private fun snapToEdge() {
        val lp = wmParams()
        val screenW = wm.currentWindowMetrics.bounds.width()
        val centerX = lp.x + lp.width / 2
        val targetX = if (centerX < screenW / 2) {
            edgeMargin.toInt()
        } else {
            screenW - lp.width - edgeMargin.toInt()
        }

        ValueAnimator.ofInt(lp.x, targetX).apply {
            duration = 250
            interpolator = OvershootInterpolator(0.5f)
            addUpdateListener { anim ->
                lp.x = anim.animatedValue as Int
                wm.updateViewLayout(this@FloatingBallView, lp)
            }
            start()
        }
    }
}
