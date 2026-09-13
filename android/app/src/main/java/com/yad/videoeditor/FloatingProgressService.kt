package com.yad.videoeditor

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
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
 * FloatingProgressService — floating progress bar yang:
 *   - Berputar (rotating spinner)
 *   - Bergerak pelan (auto animation)
 *   - Bisa digeser (drag)
 *   - Tampil di atas semua app
 */
class FloatingProgressService : Service() {

    companion object {
        private const val TAG = "FloatingProgress"
        const val ACTION_SHOW = "com.yad.videoeditor.SHOW_PROGRESS"
        const val ACTION_UPDATE = "com.yad.videoeditor.UPDATE_PROGRESS"
        const val ACTION_HIDE = "com.yad.videoeditor.HIDE_PROGRESS"

        const val EXTRA_PERCENT = "percent"
        const val EXTRA_MESSAGE = "message"

        @Volatile
        var isRunning = false
            private set
    }

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var progressBar: ProgressBar? = null
    private var tvPercent: TextView? = null
    private var tvMessage: TextView? = null

    private var rotateAnimator: ObjectAnimator? = null
    private var floatAnimator: ObjectAnimator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        AutoLogSaver.log(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> {
                val percent = intent.getIntExtra(EXTRA_PERCENT, 0)
                val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "Memproses..."
                showFloating(percent, message)
            }
            ACTION_UPDATE -> {
                val percent = intent.getIntExtra(EXTRA_PERCENT, 0)
                val message = intent.getStringExtra(EXTRA_MESSAGE) ?: ""
                updateProgress(percent, message)
            }
            ACTION_HIDE -> {
                hideFloating()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun showFloating(percent: Int, message: String) {
        if (floatingView != null) {
            updateProgress(percent, message)
            return
        }

        try {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

            // Inflate layout
            val inflater = LayoutInflater.from(this)
            floatingView = inflater.inflate(R.layout.floating_progress, null)

            progressBar = floatingView?.findViewById(R.id.fpProgressBar)
            tvPercent = floatingView?.findViewById(R.id.fpPercent)
            tvMessage = floatingView?.findViewById(R.id.fpMessage)

            // Setup window params
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

            // Add view
            windowManager?.addView(floatingView, params)

            // Drag support
            setupDrag(floatingView!!, params)

            // Update initial progress
            updateProgress(percent, message)

            // Start animations
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

    /**
     * Setup drag — floating view bisa digeser.
     */
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

    /**
     * Start animasi — rotate + float.
     */
    private fun startAnimations() {
        val spinner = floatingView?.findViewById<View>(R.id.fpSpinner)

        // Rotate animation (berputar)
        if (spinner != null) {
            rotateAnimator = ObjectAnimator.ofFloat(spinner, "rotation", 0f, 360f).apply {
                duration = 1500
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                start()
            }
        }

        // Float animation (bergerak pelan naik turun)
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
        isRunning = false
        AutoLogSaver.log(TAG, "Service destroyed")
    }

    // ============================================================
    //  STATIC HELPERS — cara pakai dari Activity
    // ============================================================
    companion object Helper {
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
}
