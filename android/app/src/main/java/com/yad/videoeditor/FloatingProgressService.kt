package com.yad.videoeditor

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.app.NotificationCompat

/**
 * FloatingProgressService — floating progress bar.
 *
 * FIX: Panggil startForeground() di onStartCommand() untuk Android 8+.
 * Tanpa ini, Android akan crash: "did not then call Service.startForeground()"
 */
class FloatingProgressService : Service() {

    companion object {
        private const val TAG = "FloatingProgress"
        private const val CHANNEL_ID = "floating_progress_channel"
        private const val NOTIFICATION_ID = 9999

        const val ACTION_SHOW = "com.yad.videoeditor.SHOW_PROGRESS"
        const val ACTION_UPDATE = "com.yad.videoeditor.UPDATE_PROGRESS"
        const val ACTION_HIDE = "com.yad.videoeditor.HIDE_PROGRESS"

        const val EXTRA_PERCENT = "percent"
        const val EXTRA_MESSAGE = "message"

        @Volatile
        var isRunning = false
            private set

        fun show(context: Context, percent: Int = 0, message: String = "Memproses...") {
            try {
                val intent = Intent(context, FloatingProgressService::class.java).apply {
                    action = ACTION_SHOW
                    putExtra(EXTRA_PERCENT, percent)
                    putExtra(EXTRA_MESSAGE, message)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                AutoLogSaver.logError(TAG, "show helper failed", e)
            }
        }

        fun update(context: Context, percent: Int, message: String = "") {
            try {
                val intent = Intent(context, FloatingProgressService::class.java).apply {
                    action = ACTION_UPDATE
                    putExtra(EXTRA_PERCENT, percent)
                    putExtra(EXTRA_MESSAGE, message)
                }
                context.startService(intent)
            } catch (e: Exception) {
                AutoLogSaver.logError(TAG, "update helper failed", e)
            }
        }

        fun hide(context: Context) {
            try {
                val intent = Intent(context, FloatingProgressService::class.java).apply {
                    action = ACTION_HIDE
                }
                context.startService(intent)
            } catch (e: Exception) {
                AutoLogSaver.logError(TAG, "hide helper failed", e)
            }
        }
    }

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var progressBar: ProgressBar? = null
    private var tvPercent: TextView? = null
    private var tvMessage: TextView? = null

    private var rotateAnimator: ObjectAnimator? = null
    private var floatAnimator: ObjectAnimator? = null

    private var foregroundStarted = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        AutoLogSaver.log(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // === WAJIB: Panggil startForeground() untuk Android 8+ ===
        // Kalau tidak, Android akan crash service dalam 5 detik.
        try {
            ensureNotificationChannel()
            val notification = buildNotification("Memproses video...", 0)
            startForeground(NOTIFICATION_ID, notification)
            foregroundStarted = true
            AutoLogSaver.log(TAG, "startForeground called OK")
        } catch (e: Exception) {
            AutoLogSaver.logError(TAG, "startForeground failed", e)
        }

        when (intent?.action) {
            ACTION_SHOW -> {
                val percent = intent.getIntExtra(EXTRA_PERCENT, 0)
                val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "Memproses..."
                updateForegroundNotification(message, percent)
                showFloating(percent, message)
            }
            ACTION_UPDATE -> {
                val percent = intent.getIntExtra(EXTRA_PERCENT, 0)
                val message = intent.getStringExtra(EXTRA_MESSAGE) ?: ""
                updateForegroundNotification(message, percent)
                updateProgress(percent, message)
            }
            ACTION_HIDE -> {
                hideFloating()
                stopForeground(true)
                foregroundStarted = false
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    // ============================================================
    //  NOTIFICATION
    // ============================================================
    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Video Generation",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Progress generate video"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(message: String, percent: Int): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("YAD Video Editor")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, percent, false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        return builder.build()
    }

    private fun updateForegroundNotification(message: String, percent: Int) {
        if (!foregroundStarted) return
        try {
            val notification = buildNotification(message, percent)
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            AutoLogSaver.logError(TAG, "updateForegroundNotification failed", e)
        }
    }

    // ============================================================
    //  FLOATING VIEW
    // ============================================================
    private fun showFloating(percent: Int, message: String) {
        if (floatingView != null) {
            updateProgress(percent, message)
            return
        }

        try {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

            val inflater = LayoutInflater.from(this)
            floatingView = inflater.inflate(R.layout.floating_progress, null)

            progressBar = floatingView?.findViewById(R.id.fpProgressBar)
            tvPercent = floatingView?.findViewById(R.id.fpPercent)
            tvMessage = floatingView?.findViewById(R.id.fpMessage)

            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.TOP or Gravity.START
            params.x = 100
            params.y = 200

            windowManager?.addView(floatingView, params)
            setupDrag(floatingView!!, params)
            updateProgress(percent, message)
            startAnimations()

            AutoLogSaver.log(TAG, "Floating progress shown")
        } catch (e: Exception) {
            AutoLogSaver.logError(TAG, "showFloating failed", e)
        }
    }

    private fun updateProgress(percent: Int, message: String) {
        try {
            progressBar?.progress = percent
            tvPercent?.text = "$percent%"
            if (message.isNotEmpty()) {
                tvMessage?.text = message
            }
        } catch (e: Exception) {
            AutoLogSaver.logError(TAG, "updateProgress failed", e)
        }
    }

    private fun setupDrag(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    try {
                        windowManager?.updateViewLayout(view, params)
                    } catch (_: Exception) {}
                    true
                }
                else -> false
            }
        }
    }

    private fun startAnimations() {
        val spinner = floatingView?.findViewById<View>(R.id.fpSpinner)

        if (spinner != null) {
            rotateAnimator = ObjectAnimator.ofFloat(spinner, "rotation", 0f, 360f).apply {
                duration = 1500
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                start()
            }
        }

        floatingView?.let { view ->
            floatAnimator = ObjectAnimator.ofFloat(view, "translationY", 0f, -15f, 0f, 15f, 0f).apply {
                duration = 3000
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                start()
            }
        }
    }

    private fun hideFloating() {
        try {
            rotateAnimator?.cancel()
            floatAnimator?.cancel()

            floatingView?.let { view ->
                try {
                    windowManager?.removeView(view)
                } catch (_: Exception) {}
            }
            floatingView = null
            AutoLogSaver.log(TAG, "Floating progress hidden")
        } catch (e: Exception) {
            AutoLogSaver.logError(TAG, "hideFloating failed", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        hideFloating()
        if (foregroundStarted) {
            try { stopForeground(true) } catch (_: Exception) {}
            foregroundStarted = false
        }
        isRunning = false
        AutoLogSaver.log(TAG, "Service destroyed")
    }
}
