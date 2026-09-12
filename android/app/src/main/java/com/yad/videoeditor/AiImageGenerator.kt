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

    suspend fun generateImage(context: Context, prompt: String): File? =
        withContext(Dispatchers.IO) {
            try {
                val jobId = UUID.randomUUID().toString().replace("-", "")
                val ok = GitHubAiClient.dispatchGeneration(prompt, jobId)
                if (!ok) return@withContext null

                var url: String? = null
                for (i in 0 until 60) {
                    delay(5_000)
                    url = GitHubAiClient.checkResult(jobId)
                    if (url != null) break
                }
                if (url == null) return@withContext null

                val request = Request.Builder().url(url).get().build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val bytes = response.body?.bytes() ?: return@withContext null
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        ?: return@withContext null
                    val dir = File(context.cacheDir, "ai_images")
                    if (!dir.exists()) dir.mkdirs()
                    val file = File(dir, "img_$jobId.png")
                    FileOutputStream(file).use { out ->
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                    }
                    file
                }
            } catch (e: Exception) { null }
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
