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
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

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

            callback?.onProgress(1, 1, "SENDING", 5, "Mengirim permintaan...")
            val ok = GitHubAiClient.dispatchVideo(
                prompt, jobId, voice, watermark, showSubtitle, subtitleStyle, modelId
            )
            if (!ok) {
                callback?.onProgress(1, 1, "FAILED", 0, "Gagal mengirim permintaan")
                return@withContext null
            }
            callback?.onProgress(1, 1, "SENDING", 10, "Permintaan terkirim")

            var url: String? = null
            for (i in 0 until 180) {
                delay(5_000)
                val pct = 10 + (i * 75 / 180)
                url = GitHubAiClient.checkResult(jobId)
                if (url != null) break
                callback?.onProgress(1, 1, "WAITING", pct,
                    "Memproses AI... ${i * 5}s")
            }
            if (url == null) {
                callback?.onProgress(1, 1, "FAILED", 0, "Timeout — coba lagi")
                return@withContext null
            }

            callback?.onProgress(1, 1, "DOWNLOADING", 90, "Mengunduh hasil...")
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    callback?.onProgress(1, 1, "FAILED", 0, "Download gagal")
                    return@withContext null
                }
                val bytes = response.body?.bytes() ?: return@withContext null
                val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, "ai_videos")
                if (!dir.exists()) dir.mkdirs()

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
}
