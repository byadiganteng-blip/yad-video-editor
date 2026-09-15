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
        const val EXTRA_MODEL = "model_id"
        const val EXTRA_RUN_ID = "runId"
        const val EXTRA_TOKEN = "token"

        const val ACTION_POLL_WORKFLOW = "com.yad.videoeditor.POLL_WORKFLOW"

        const val ACTION_PROGRESS = "com.yad.videoeditor.PROGRESS"
        const val ACTION_DONE = "com.yad.videoeditor.DONE"
        const val ACTION_FAILED = "com.yad.videoeditor.FAILED"
        const val EXTRA_PERCENT = "percent"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_FILE_PATH = "file_path"

        @Volatile
        var isRunning = false
            private set
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var currentJob: Job? = null
    private var pollingJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            return START_NOT_STICKY
        }

        // ============================================================
        //  MODE 1: POLL WORKFLOW (dari TextToVideoActivity)
        // ============================================================
        if (intent.action == ACTION_POLL_WORKFLOW) {
            val runId = intent.getLongExtra(EXTRA_RUN_ID, 0L)
            val token = intent.getStringExtra(EXTRA_TOKEN) ?: ""
            if (runId == 0L || token.isEmpty()) {
                AutoLogSaver.logError("VideoGeneratorService", "POLL: invalid runId/token", null)
                return START_NOT_STICKY
            }

            isRunning = true
            startForeground(NOTIF_ID, buildNotification(0, "Menunggu server..."))
            startPolling(runId, token)
            return START_NOT_STICKY
        }

        // ============================================================
        //  MODE 2: GENERATE LOKAL (via AI)
        // ============================================================
        val prompt = intent.getStringExtra(EXTRA_PROMPT)
        if (prompt != null) {
            val voice = intent.getStringExtra(EXTRA_VOICE) ?: "male_id"
            val watermark = intent.getStringExtra(EXTRA_WATERMARK) ?: ""
            val showSubtitle = intent.getBooleanExtra(EXTRA_SHOW_SUBTITLE, true)
            val subtitleStyle = intent.getStringExtra(EXTRA_SUBTITLE_STYLE) ?: "neon"
            val modelId = intent.getStringExtra(EXTRA_MODEL) ?: "waifu"

            isRunning = true
            GeneratorState.saveRunning(this, true)
            GeneratorState.saveJobInfo(this, "", 0L, prompt, voice, modelId)

            startForeground(NOTIF_ID, buildNotification(0, "Memulai..."))

            currentJob?.cancel()
            currentJob = scope.launch {
                try {
                    val file = AiImageGenerator.generateVideo(
                        this@VideoGeneratorService,
                        prompt, voice, watermark, showSubtitle, subtitleStyle, modelId,
                        AiImageGenerator.ProgressCallback { _, _, _, pct, msg ->
                            GeneratorState.saveProgress(this@VideoGeneratorService, pct, msg)
                            updateNotification(pct, msg)
                            sendBroadcast(ACTION_PROGRESS, pct, msg, null)
                        }
                    )

                    if (file != null) {
                        updateNotification(100, "Selesai!")
                        GeneratorState.saveRunning(this@VideoGeneratorService, false)
                        GeneratorState.saveProgress(this@VideoGeneratorService, 100, "Selesai")
                        sendBroadcast(ACTION_DONE, 100, "Video selesai", file.absolutePath)
                        showDoneNotification(file.absolutePath)
                    } else {
                        updateNotification(0, "Gagal")
                        GeneratorState.saveRunning(this@VideoGeneratorService, false)
                        sendBroadcast(ACTION_FAILED, 0, "Gagal membuat video", null)
                    }
                } catch (e: Exception) {
                    GeneratorState.saveRunning(this@VideoGeneratorService, false)
                    sendBroadcast(ACTION_FAILED, 0, e.message ?: "Error", null)
                } finally {
                    isRunning = false
                    delay(3000)
                    stopForeground(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                        STOP_FOREGROUND_DETACH else 0)
                    stopSelf()
                }
            }
        }

        return START_NOT_STICKY
    }

    // ============================================================
    //  POLLING WORKFLOW
    // ============================================================
    private fun startPolling(runId: Long, token: String) {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            val maxAttempts = Int.MAX_VALUE   // unlimited — tunggu sampai selesai
            var attempt = 0

            AutoLogSaver.log("VideoGeneratorService", "Polling start: runId=$runId")

            while (attempt < maxAttempts) {
                attempt++
                delay(5000)

                val result = GitHubApiClient.checkStatusOnce(token, runId) { status, pct ->
                    updateNotification(pct, status)
                    sendBroadcast(ACTION_PROGRESS, pct, status, null)
                    FloatingProgressService.update(this@VideoGeneratorService, pct, status)
                }

                if (result != null) {
                    if (result.first) {
                        // SUKSES → download
                        val artifactUrl = result.second
                        AutoLogSaver.log("VideoGeneratorService", "Run completed, downloading...")
                        updateNotification(90, "Mengunduh video...")

                        val path = GitHubApiClient.downloadArtifactSync(
                            this@VideoGeneratorService, token, artifactUrl
                        )

                        if (path != null) {
                            AutoLogSaver.log("VideoGeneratorService", "Download OK: $path")
                            GeneratorState.saveRunning(this@VideoGeneratorService, false)
                            GeneratorState.saveProgress(this@VideoGeneratorService, 100, "Selesai")
                            updateNotification(100, "Selesai!")
                            sendBroadcast(ACTION_DONE, 100, "Video selesai", path)
                            showDoneNotification(path)
                            FloatingProgressService.hide(this@VideoGeneratorService)
                            GitHubApiClient.cleanupArtifact(token, runId)
                        } else {
                            AutoLogSaver.logError("VideoGeneratorService", "Download failed", null)
                            updateNotification(0, "Download gagal")
                            sendBroadcast(ACTION_FAILED, 0, "Download gagal", null)
                            FloatingProgressService.hide(this@VideoGeneratorService)
                        }
                    } else {
                        // GAGAL
                        val errMsg = result.third
                        AutoLogSaver.logError("VideoGeneratorService", "Workflow error: $errMsg", null)
                        updateNotification(0, errMsg)
                        sendBroadcast(ACTION_FAILED, 0, errMsg, null)
                        FloatingProgressService.hide(this@VideoGeneratorService)
                    }
                    break
                }
                // null = masih running, lanjut poll
            }

            if (attempt >= maxAttempts) {
                AutoLogSaver.logError("VideoGeneratorService", "Polling timeout", null)
                updateNotification(0, "Polling timeout - cek GitHub Actions")
                sendBroadcast(ACTION_FAILED, 0, "Polling timeout - cek GitHub Actions", null)
                FloatingProgressService.hide(this@VideoGeneratorService)
            }

            isRunning = false
            delay(2000)
            stopForeground(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                STOP_FOREGROUND_DETACH else 0)
            stopSelf()
        }
    }

    // ============================================================
    //  NOTIFICATION
    // ============================================================
    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "YAD Video Generator",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(progress: Int, message: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🎬 YAD Video Editor")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pi)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
        if (progress in 1..99) builder.setProgress(100, progress, false)
        else if (progress >= 100) builder.setProgress(0, 0, false)
        else builder.setProgress(0, 0, true)
        return builder.build()
    }

    private fun updateNotification(progress: Int, message: String) {
        try {
            getSystemService(NotificationManager::class.java)
                .notify(NOTIF_ID, buildNotification(progress, message))
        } catch (_: Exception) {}
    }

    private fun showDoneNotification(filePath: String) {
        try {
            val intent = Intent(this, VideoPreviewActivity::class.java).apply {
                putExtra("video_path", filePath)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val pi = PendingIntent.getActivity(this, 2, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val notif = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("✅ Video Selesai!")
                .setContentText("Tap untuk preview")
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
            getSystemService(NotificationManager::class.java)
                .notify(NOTIF_ID + 1, notif)
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
        AutoLogSaver.log("VideoGeneratorService", "Service destroyed by user/system")
        GeneratorState.saveRunning(this, false)
        isRunning = false
        currentJob?.cancel()
        pollingJob?.cancel()
        scope.cancel()
    }
}
