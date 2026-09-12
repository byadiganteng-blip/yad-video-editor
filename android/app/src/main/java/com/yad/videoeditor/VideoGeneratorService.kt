package com.yad.videoeditor

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class VideoGeneratorService : Service() {

    companion object {
        const val CHANNEL_ID = "yad_video_gen"
        const val NOTIF_ID = 1001
        const val EXTRA_PROMPT = "prompt"
        const val EXTRA_VOICE = "voice"
        const val EXTRA_WATERMARK = "watermark"
        const val EXTRA_SHOW_SUBTITLE = "show_subtitle"
        const val EXTRA_SUBTITLE_STYLE = "subtitle_style"

        var isRunning = false
            private set

        const val ACTION_PROGRESS = "com.yad.videoeditor.PROGRESS"
        const val ACTION_DONE = "com.yad.videoeditor.DONE"
        const val ACTION_FAILED = "com.yad.videoeditor.FAILED"
        const val EXTRA_PERCENT = "percent"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_FILE_PATH = "file_path"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var currentJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        val prompt = intent.getStringExtra(EXTRA_PROMPT) ?: return START_NOT_STICKY
        val voice = intent.getStringExtra(EXTRA_VOICE) ?: "none"
        val watermark = intent.getStringExtra(EXTRA_WATERMARK) ?: ""
        val showSubtitle = intent.getBooleanExtra(EXTRA_SHOW_SUBTITLE, true)
        val subtitleStyle = intent.getStringExtra(EXTRA_SUBTITLE_STYLE) ?: "neon"

        isRunning = true
        startForeground(NOTIF_ID, buildNotification(0, "Memulai proses..."))

        currentJob?.cancel()
        currentJob = scope.launch {
            try {
                val file = AiImageGenerator.generateVideo(
                    this@VideoGeneratorService,
                    prompt, voice, watermark, showSubtitle, subtitleStyle,
                    AiImageGenerator.ProgressCallback { _, _, stage, pct, msg ->
                        updateNotification(pct, msg)
                        sendBroadcast(ACTION_PROGRESS, pct, msg, null)
                    }
                )

                if (file != null) {
                    updateNotification(100, "✅ Selesai!")
                    sendBroadcast(ACTION_DONE, 100, "Video selesai", file.absolutePath)
                    showDoneNotification(file.absolutePath)
                } else {
                    updateNotification(0, "❌ Gagal membuat video")
                    sendBroadcast(ACTION_FAILED, 0, "Gagal membuat video", null)
                }
            } catch (e: Exception) {
                updateNotification(0, "❌ Error: ${e.message}")
                sendBroadcast(ACTION_FAILED, 0, e.message ?: "Error", null)
            } finally {
                isRunning = false
                delay(3000)
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "YAD Video Generator",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Proses pembuatan video AI"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(progress: Int, message: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            this, 0, intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🎬 YAD Video Editor")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pi)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (progress > 0 && progress < 100) {
            builder.setProgress(100, progress, false)
        } else if (progress >= 100) {
            builder.setProgress(0, 0, false)
        } else {
            builder.setProgress(0, 0, true)
        }

        return builder.build()
    }

    private fun updateNotification(progress: Int, message: String) {
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIF_ID, buildNotification(progress, message))
        } catch (_: Exception) {}
    }

    private fun showDoneNotification(filePath: String) {
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val intent = Intent(this, VideoPreviewActivity::class.java).apply {
                putExtra("video_path", filePath)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val pi = PendingIntent.getActivity(
                this, 2, intent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                else PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notif = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("✅ Video Selesai!")
                .setContentText("Tap untuk melihat preview")
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            nm.notify(NOTIF_ID + 1, notif)
        } catch (_: Exception) {}
    }

    private fun sendBroadcast(action: String, pct: Int, msg: String, filePath: String?) {
        val i = Intent(action).apply {
            setPackage(packageName)
            putExtra(EXTRA_PERCENT, pct)
            putExtra(EXTRA_MESSAGE, msg)
            if (filePath != null) putExtra(EXTRA_FILE_PATH, filePath)
        }
        sendBroadcast(i)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        currentJob?.cancel()
        scope.cancel()
    }
}
