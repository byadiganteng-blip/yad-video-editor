package com.yad.videoeditor

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

object AiImageGenerator {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .build()

    // 45 menit = 540 iterasi × 5 detik
    private const val MAX_POLL_ITERATIONS = 540

    fun interface ProgressCallback {
        fun onProgress(sceneIndex: Int, totalScenes: Int,
                       stage: String, progressPercent: Int, message: String)
    }

    suspend fun generateVideo(
        context: Context,
        prompt: String,
        voice: String,
        watermark: String,
        showSubtitle: Boolean,
        subtitleStyle: String,
        modelId: String = "waifu",
        callback: ProgressCallback?
    ): File? = withContext(Dispatchers.IO) {
        try {
            val jobId = UUID.randomUUID().toString().replace("-", "")

            // 1. Trigger workflow
            callback?.onProgress(1, 1, "SENDING", 5, "Mengirim permintaan...")
            val ok = GitHubAiClient.dispatchVideo(
                prompt, jobId, voice, watermark, showSubtitle, subtitleStyle, modelId
            )
            if (!ok) {
                callback?.onProgress(1, 1, "FAILED", 0, "Gagal mengirim permintaan")
                return@withContext null
            }
            callback?.onProgress(1, 1, "SENDING", 10, "Permintaan terkirim")

            // 2. Poll status — max 45 menit
            var url: String? = null
            for (i in 0 until MAX_POLL_ITERATIONS) {
                delay(5_000)

                // Progress: 10% → 88% selama 45 menit
                val pct = 10 + (i * 78 / MAX_POLL_ITERATIONS)
                val elapsedMin = (i * 5) / 60
                val elapsedSec = (i * 5) % 60

                url = GitHubAiClient.checkResult(jobId)
                if (url != null) break

                callback?.onProgress(
                    1, 1, "WAITING", pct,
                    "Memproses AI... (%02d:%02d)".format(elapsedMin, elapsedSec)
                )
            }

            // Cek: kalau timeout tapi workflow mungkin masih jalan
            if (url == null) {
                callback?.onProgress(
                    1, 1, "TIMEOUT", 90,
                    "Masih diproses. Cek notifikasi nanti."
                )
                return@withContext null
            }

            // 3. Download video
            callback?.onProgress(1, 1, "DOWNLOADING", 90, "Mengunduh video...")
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    callback?.onProgress(1, 1, "FAILED", 0, "Download gagal")
                    return@withContext null
                }
                val bytes = response.body?.bytes() ?: return@withContext null
                val dir = File(
                    context.getExternalFilesDir(null) ?: context.filesDir,
                    "ai_videos"
                )
                if (!dir.exists()) dir.mkdirs()

                val file = File(dir, "video_$jobId.mp4")
                FileOutputStream(file).use { out -> out.write(bytes) }

                callback?.onProgress(1, 1, "DONE", 100, "Selesai!")
                file
            }
        } catch (e: Exception) {
            callback?.onProgress(1, 1, "FAILED", 0, "Error: ${e.message}")
            null
        }
    }
}
