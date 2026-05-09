package com.paster.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import java.util.concurrent.Executors

class FloatingBallService : Service() {

    private var ballView: FloatingBallView? = null
    private var isBallShowing = false
    private val windowManager: WindowManager by lazy {
        getSystemService(WINDOW_SERVICE) as WindowManager
    }
    private val ioExecutor = Executors.newSingleThreadExecutor()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> showBall()
            ACTION_HIDE -> hideBall()
            ACTION_TOGGLE -> if (isBallShowing) hideBall() else showBall()
            else -> showBall()
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(createPendingIntent())
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        hideBall()
        ioExecutor.shutdown()
    }

    private fun showBall() {
        if (isBallShowing) return
        val view = FloatingBallView(this).apply {
            onClickAction = { copyLatestImage() }
            onLongPressAction = { openSettings() }
        }
        try {
            windowManager.addView(view, view.createLayoutParams())
            ballView = view
            isBallShowing = true
        } catch (_: Exception) {}
    }

    private fun hideBall() {
        val view = ballView ?: return
        try {
            windowManager.removeView(view)
        } catch (_: Exception) {}
        ballView = null
        isBallShowing = false
    }

    private fun copyLatestImage() {
        ioExecutor.execute {
            val bitmap = GalleryReader.getLatestImage(this@FloatingBallService)
            Handler(Looper.getMainLooper()).post {
                if (bitmap != null) {
                    ClipboardHelper.copyBitmap(this@FloatingBallService, bitmap)
                } else {
                    Toast.makeText(this@FloatingBallService,
                        "未找到相册图片，请先截图", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openSettings() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(this, 0, intent, flags)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "floating_ball_service"
        const val NOTIFICATION_ID = 1001
        const val ACTION_SHOW = "com.paster.app.SHOW_BALL"
        const val ACTION_HIDE = "com.paster.app.HIDE_BALL"
        const val ACTION_TOGGLE = "com.paster.app.TOGGLE_BALL"

        fun start(context: android.content.Context) {
            val intent = Intent(context, FloatingBallService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: android.content.Context) {
            context.stopService(Intent(context, FloatingBallService::class.java))
        }

        fun showBall(context: android.content.Context) {
            val intent = Intent(context, FloatingBallService::class.java).apply {
                action = ACTION_SHOW
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun hideBall(context: android.content.Context) {
            val intent = Intent(context, FloatingBallService::class.java).apply {
                action = ACTION_HIDE
            }
            context.startService(intent)
        }
    }
}
