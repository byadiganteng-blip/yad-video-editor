package com.yad.videoeditor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Wrapper untuk GitHub API — trigger generate_image.yml
 * Created by KARYADI, Coding by KARYADI
 * Fixed by Auto-Fix Script
 */
object GitHubAiClient {

    private const val OWNER = "byadiganteng-blip"
    private const val REPO = "yad-video-editor"
    private const val WORKFLOW = "generate_image.yml"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    /**
     * Kirim permintaan generate video ke GitHub Actions.
     * Return: true kalau berhasil trigger.
     */
    suspend fun dispatchVideo(
        prompt: String,
        jobId: String,
        voice: String = "male_id",
        watermark: String = "",
        showSubtitle: Boolean = true,
        subtitleStyle: String = "neon"
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.github.com/repos/$OWNER/$REPO/" +
                      "actions/workflows/$WORKFLOW/dispatches"

            val json = """
                {
                  "ref": "main",
                  "inputs": {
                    "prompt": ${JSONObject.quote(prompt)},
                    "voice": "$voice",
                    "watermark": "${watermark.replace("\"", "\\\"")}",
                    "show_subtitle": "$showSubtitle",
                    "subtitle_style": "$subtitleStyle",
                    "scenes_count": "8",
                    "image_style": "cinematic"
                  }
                }
            """.trimIndent()

            val body = okhttp3.RequestBody.Companion.run {
                json.toRequestBody("application/json".toMediaTypeOrNull())
            }

            val req = Request.Builder()
                .url(url)
                .header("Authorization", "token ${SecureConfig.getGithubToken()}")
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "YadApp")
                .post(body)
                .build()

            val resp = client.newCall(req).execute()
            println("dispatchVideo: HTTP ${resp.code}")
            resp.code == 204
        } catch (e: Exception) {
            println("dispatchVideo error: ${e.message}")
            false
        }
    }

    /**
     * Cek hasil generate video via GitHub Release.
     * Return: URL download video kalau sudah siap, null kalau belum.
     */
    suspend fun checkResult(jobId: String): String? = withContext(Dispatchers.IO) {
        try {
            // Cek releases terbaru
            val url = "https://api.github.com/repos/$OWNER/$REPO/releases?per_page=5"
            val req = Request.Builder()
                .url(url)
                .header("Authorization", "token ${SecureConfig.getGithubToken()}")
                .header("Accept", "application/vnd.github+json")
                .build()

            val resp = client.newCall(req).execute()
            val body = resp.body?.string() ?: "[]"
            val arr = org.json.JSONArray(body)

            for (i in 0 until arr.length()) {
                val release = arr.getJSONObject(i)
                val tag = release.optString("tag_name")
                val assets = release.optJSONArray("assets") ?: continue

                for (j in 0 until assets.length()) {
                    val asset = assets.getJSONObject(j)
                    val name = asset.optString("name")
                    if (name.endsWith(".mp4")) {
                        return@withContext asset.optString("browser_download_url")
                    }
                }
            }
            null
        } catch (e: Exception) {
            println("checkResult error: ${e.message}")
            null
        }
    }
}
