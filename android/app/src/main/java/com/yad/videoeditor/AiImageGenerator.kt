package com.yad.videoeditor

import android.content.Context
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

object AiImageGenerator {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .build()

    suspend fun generateImage(context: Context, prompt: String, apiKey: String): File? =
        withContext(Dispatchers.IO) {
            try {
                val url = "https://api-inference.huggingface.co/models/hakurei/waifu-diffusion"
                val body = JSONObject().apply {
                    put("inputs", prompt)
                    put("options", JSONObject().apply { put("wait_for_model", true) })
                }.toString()

                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $apiKey")
                    .header("Content-Type", "application/json")
                    .post(body.toRequestBody("application/json".toMediaTypeOrNull()))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val bytes = response.body?.bytes() ?: return@withContext null
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        ?: return@withContext null
                    val dir = File(context.cacheDir, "ai_images")
                    if (!dir.exists()) dir.mkdirs()
                    val file = File(dir, "img_${System.currentTimeMillis()}.png")
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
