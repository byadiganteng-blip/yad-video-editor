package com.yad.videoeditor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * ImageSearcher — generate gambar dari Pollinations.ai.
 *
 * Pakai StoryAnalyzer untuk deteksi detail cerita + style.
 */
object ImageSearcher {

    private const val TAG = "ImageSearcher"

    private val client = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    /**
     * Generate gambar berdasarkan scene text + style suffix.
     */
    suspend fun searchAndDownload(
        sceneText: String,
        styleSuffix: String,
        cacheDir: File,
        sceneIndex: Int = 0
    ): File? = withContext(Dispatchers.IO) {

        // Analisa cerita
        val detail = StoryAnalyzer.analyze(sceneText)
        val prompt = StoryAnalyzer.buildPrompt(detail, styleSuffix)

        AutoLogSaver.log(TAG, "Scene $sceneIndex prompt: $prompt")

        // Coba Pollinations.ai
        val pollinations = tryPollinations(prompt, cacheDir, sceneIndex)
        if (pollinations != null) return@withContext pollinations

        // Fallback: Picsum
        AutoLogSaver.log(TAG, "Pollinations gagal, fallback ke Picsum")
        tryPicsum(cacheDir)
    }

    // ============================================================
    //  POLLINATIONS.AI
    // ============================================================
    private fun tryPollinations(prompt: String, cacheDir: File, idx: Int): File? {
        return try {
            val encoded = URLEncoder.encode(prompt, "UTF-8")
            val seed = (System.currentTimeMillis() % 100000) + idx
            val url = "https://image.pollinations.ai/prompt/$encoded?width=1280&height=720&nologo=true&seed=$seed"

            AutoLogSaver.log(TAG, "Pollinations URL: $url")

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "YAD-Video-Editor/1.0")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                AutoLogSaver.logError(TAG, "Pollinations HTTP ${response.code}", null)
                response.close()
                return null
            }

            val file = File(cacheDir, "ai_${idx}_${System.currentTimeMillis()}.jpg")
            response.body?.byteStream()?.use { input ->
                file.outputStream().use { input.copyTo(it) }
            }
            response.close()

            if (file.length() < 1000) {
                AutoLogSaver.logError(TAG, "Pollinations file kecil", null)
                file.delete()
                return null
            }

            AutoLogSaver.log(TAG, "Pollinations OK: ${file.length()} bytes")
            file
        } catch (e: Exception) {
            AutoLogSaver.logError(TAG, "tryPollinations failed", e)
            null
        }
    }

    // ============================================================
    //  PICSUM (fallback)
    // ============================================================
    private fun tryPicsum(cacheDir: File): File? {
        return try {
            val seed = System.currentTimeMillis() % 10000
            val url = "https://picsum.photos/seed/$seed/1280/720"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "YAD-Video-Editor/1.0")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) { response.close(); return null }

            val file = File(cacheDir, "picsum_${System.currentTimeMillis()}.jpg")
            response.body?.byteStream()?.use { input ->
                file.outputStream().use { input.copyTo(it) }
            }
            response.close()

            if (file.length() < 1000) { file.delete(); return null }
            AutoLogSaver.log(TAG, "Picsum OK: ${file.length()} bytes")
            file
        } catch (e: Exception) {
            AutoLogSaver.logError(TAG, "tryPicsum failed", e)
            null
        }
    }
}
