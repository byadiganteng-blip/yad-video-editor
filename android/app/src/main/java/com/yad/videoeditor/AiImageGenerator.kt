package com.yad.videoeditor

import android.content.Context
import android.graphics.BitmapFactory
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
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    /**
     * Callback progress real-time.
     * @param sceneIndex 1-based index
     * @param totalScenes total scene
     * @param stage "SENDING" / "WAITING" / "DOWNLOADING" / "DONE" / "FAILED"
     * @param progressPercent 0-100
     * @param message pesan detail
     */
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
        callback: ProgressCallback?
    ): File? = withContext(Dispatchers.IO) {
        try {
            val jobId = UUID.randomUUID().toString().replace("-", "")

            // Stage 1: Sending (0-10%)
            callback?.onProgress(1, 1, "SENDING", 5, "Mengirim permintaan...")
            val ok = GitHubAiClient.dispatchVideo(
                prompt, jobId, voice, watermark, showSubtitle, subtitleStyle
            )
            if (!ok) {
                callback?.onProgress(1, 1, "FAILED", 0, "Gagal mengirim permintaan")
                return@withContext null
            }
            callback?.onProgress(1, 1, "SENDING", 10, "Permintaan terkirim")

            // Stage 2: Waiting (10-85%)
            var url: String? = null
            for (i in 0 until 60) {
                delay(5_000)
                val pct = 10 + (i * 75 / 60) // 10 → 85
                url = GitHubAiClient.checkResult(jobId)
                if (url != null) break
                callback?.onProgress(1, 1, "WAITING", pct,
                    "Memproses AI... ${i * 5}s")
            }
            if (url == null) {
                callback?.onProgress(1, 1, "FAILED", 0, "Timeout — coba lagi")
                return@withContext null
            }

            // Stage 3: Downloading (85-100%)
            callback?.onProgress(1, 1, "DOWNLOADING", 90, "Mengunduh hasil...")
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    callback?.onProgress(1, 1, "FAILED", 0, "Download gagal")
                    return@withContext null
                }
                val bytes = response.body?.bytes() ?: return@withContext null
                val dir = File(context.cacheDir, "ai_videos")
                if (!dir.exists()) dir.mkdirs()

                // Simpan sebagai file video (mp4) atau gambar (png)
                val isVideo = url.endsWith(".mp4", ignoreCase = true)
                val ext = if (isVideo) "mp4" else "png"
                val file = File(dir, "video_$jobId.$ext")
                FileOutputStream(file).use { out -> out.write(bytes) }

                callback?.onProgress(1, 1, "DONE", 100, "Selesai ✓")
                file
            }
        } catch (e: Exception) {
            callback?.onProgress(1, 1, "FAILED", 0, "Error: ${e.message}")
            null
        }
    }

    fun splitIntoScenes(story: String, maxScenes: Int = 8): List<String> {
        val paragraphs = story.split("\n\n", "\n", ". ")
            .map { it.trim() }.filter { it.isNotEmpty() }
        return if (paragraphs.size <= maxScenes) paragraphs
        else {
            val chunkSize = (paragraphs.size + maxScenes - 1) / maxScenes
            paragraphs.chunked(chunkSize).map { it.joinToString(". ") + "." }
        }
    }
}
