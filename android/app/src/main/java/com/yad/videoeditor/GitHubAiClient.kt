package com.yad.videoeditor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GitHubAiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun dispatchGeneration(prompt: String, jobId: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val token = SecureConfig.getGithubToken()
                val user  = SecureConfig.getGithubUser()
                val repo  = SecureConfig.getGithubRepo()
                if (token.isEmpty()) return@withContext false

                val url = "https://api.github.com/repos/$user/$repo/dispatches"
                val body = JSONObject().apply {
                    put("event_type", "generate_image")
                    put("client_payload", JSONObject().apply {
                        put("prompt", prompt)
                        put("job_id", jobId)
                    })
                }.toString()

                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $token")
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .post(body.toRequestBody("application/json".toMediaTypeOrNull()))
                    .build()

                client.newCall(request).execute().use { it.isSuccessful }
            } catch (e: Exception) { false }
        }

    suspend fun checkResult(jobId: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val token = SecureConfig.getGithubToken()
                val user  = SecureConfig.getGithubUser()
                val repo  = SecureConfig.getGithubRepo()

                val url = "https://api.github.com/repos/$user/$repo/releases/tags/ai-results"
                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $token")
                    .header("Accept", "application/vnd.github+json")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val body = response.body?.string() ?: return@withContext null
                    val json = JSONObject(body)
                    val assets = json.optJSONArray("assets") ?: return@withContext null
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name")
                        if (name.startsWith("$jobId.")) {
                            return@withContext asset.optString("browser_download_url")
                        }
                    }
                    null
                }
            } catch (e: Exception) { null }
        }
}
