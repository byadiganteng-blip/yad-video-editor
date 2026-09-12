package com.yad.videoeditor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Wrapper GitHub API — trigger generate_image.yml
 */
object GitHubAiClient {

    private const val OWNER = "byadiganteng-blip"
    private const val REPO = "yad-video-editor"
    private const val WORKFLOW = "generate_image.yml"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    suspend fun dispatchVideo(
        prompt: String,
        jobId: String,
        voice: String = "male_id",
        watermark: String = "",
        showSubtitle: Boolean = true,
        subtitleStyle: String = "neon",
        modelId: String = "waifu"
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.github.com/repos/$OWNER/$REPO/actions/workflows/$WORKFLOW/dispatches"
            val wm = watermark.replace("\"", "\\\"")
            val json = """
                {
                  "ref": "main",
                  "inputs": {
                    "prompt": ${JSONObject.quote(prompt)},
                    "voice": "$voice",
                    "watermark": "$wm",
                    "show_subtitle": "$showSubtitle",
                    "subtitle_style": "$subtitleStyle",
                    "scenes_count": "8",
                    "image_style": "cinematic",
                    "model_id": "$modelId"
                  }
                }
            """.trimIndent()

            val body = json.toRequestBody("application/json".toMediaTypeOrNull())
            val req = Request.Builder()
                .url(url)
                .header("Authorization", "token ${SecureConfig.getGithubToken()}")
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "YadApp")
                .post(body)
                .build()

            val resp = client.newCall(req).execute()
            resp.code == 204
        } catch (e: Exception) { false }
    }

    suspend fun checkResult(jobId: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.github.com/repos/$OWNER/$REPO/releases?per_page=10"
            val req = Request.Builder()
                .url(url)
                .header("Authorization", "token ${SecureConfig.getGithubToken()}")
                .header("Accept", "application/vnd.github+json")
                .build()
            val resp = client.newCall(req).execute()
            val arr = JSONArray(resp.body?.string() ?: "[]")
            for (i in 0 until arr.length()) {
                val release = arr.getJSONObject(i)
                val assets = release.optJSONArray("assets") ?: continue
                for (j in 0 until assets.length()) {
                    val asset = assets.getJSONObject(j)
                    if (asset.optString("name").endsWith(".mp4")) {
                        return@withContext asset.optString("browser_download_url")
                    }
                }
            }
            null
        } catch (e: Exception) { null }
    }
}
