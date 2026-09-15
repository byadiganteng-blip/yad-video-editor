package com.yad.videoeditor

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

object GitHubApiClient {

    private const val TAG = "GitHubApiClient"
    private const val OWNER = "byadiganteng-blip"
    private const val REPO = "yad-video-editor"
    private const val WORKFLOW_FILE = "generate-video.yml"

    // Progress tracking
    private val startTimes = mutableMapOf<Long, Long>()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val noRedirectClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    fun triggerVideoWorkflow(
        context: Context,
        token: String,
        mode: String,
        modelId: String,
        styleSuffix: String,
        prompt: String,
        voice: String,
        watermark: String,
        showSubtitle: Boolean,
        subtitleStyle: String,
        onSuccess: (Long) -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "https://api.github.com/repos/$OWNER/$REPO/actions/workflows/$WORKFLOW_FILE/dispatches"

                val body = JSONObject().apply {
                    put("ref", "main")
                    put("inputs", JSONObject().apply {
                        put("mode", mode)
                        put("model_id", modelId)
                        put("style_suffix", styleSuffix)
                        put("prompt", prompt)
                        put("voice", voice)
                        put("watermark", watermark)
                        put("show_subtitle", showSubtitle.toString())
                        put("subtitle_style", subtitleStyle)
                    })
                }

                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $token")
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val code = response.code
                response.close()

                if (code != 204) {
                    withContext(Dispatchers.Main) { onError("HTTP $code: Gagal trigger") }
                    return@launch
                }

                AutoLogSaver.log(TAG, "Workflow triggered OK (model=$modelId)")
                delay(3000)

                val runId = getLatestRunId(token)
                if (runId == 0L) {
                    withContext(Dispatchers.Main) { onError("Gagal dapat run ID") }
                } else {
                    // Catat waktu mulai untuk progress
                    startTimes[runId] = System.currentTimeMillis()
                    withContext(Dispatchers.Main) { onSuccess(runId) }
                }
            } catch (e: Exception) {
                AutoLogSaver.logError(TAG, "triggerVideoWorkflow failed", e)
                withContext(Dispatchers.Main) { onError(e.message ?: "Unknown") }
            }
        }
    }

    private suspend fun getLatestRunId(token: String, retries: Int = 30): Long {
        for (attempt in 1..retries) {
            try {
                val url = "https://api.github.com/repos/$OWNER/$REPO/actions/workflows/$WORKFLOW_FILE/runs?per_page=5"
                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $token")
                    .header("Accept", "application/vnd.github+json")
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""
                response.close()

                val json = JSONObject(body)
                val runs = json.getJSONArray("workflow_runs")
                val now = System.currentTimeMillis()

                for (i in 0 until runs.length()) {
                    val run = runs.getJSONObject(i)
                    val createdAt = run.getString("created_at")
                    val createdMs = try {
                        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                        fmt.timeZone = TimeZone.getTimeZone("UTC")
                        fmt.parse(createdAt)?.time ?: 0L
                    } catch (_: Exception) { 0L }

                    if (now - createdMs < 120_000) return run.getLong("id")
                }
                delay(3000)
            } catch (e: Exception) {
                AutoLogSaver.logError(TAG, "getLatestRunId attempt $attempt failed", e)
                delay(3000)
            }
        }
        return 0L
    }

    /**
     * Cek status + hitung progress smooth.
     */
    fun checkStatusOnce(
        token: String,
        runId: Long,
        onStatus: (String, Int) -> Unit
    ): Triple<Boolean, String, String>? {
        return try {
            val url = "https://api.github.com/repos/$OWNER/$REPO/actions/runs/$runId"
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github+json")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            response.close()

            val json = JSONObject(body)
            val status = json.getString("status")
            val conclusion = json.optString("conclusion", "")

            AutoLogSaver.log(TAG, "Status: $status ($conclusion)")

            when (status) {
                "queued" -> {
                    onStatus("Menunggu antrian...", 5)
                    null
                }
                "in_progress" -> {
                    // Progress smooth: naik perlahan dari 10 → 95
                    val progress = calculateSmoothProgress(runId)
                    onStatus("Sedang membuat video...", progress)
                    null
                }
                "completed" -> {
                    if (conclusion == "success") {
                        val artifactUrl = getArtifactUrlSync(token, runId)
                        if (artifactUrl.isEmpty()) Triple(false, "", "Artifact tidak ditemukan")
                        else {
                            // Cleanup tracking
                            startTimes.remove(runId)
                            Triple(true, artifactUrl, "")
                        }
                    } else {
                        startTimes.remove(runId)
                        Triple(false, "", "Workflow gagal: $conclusion")
                    }
                }
                else -> {
                    onStatus("Status: $status", 30)
                    null
                }
            }
        } catch (e: Exception) {
            AutoLogSaver.logError(TAG, "checkStatusOnce failed", e)
            null
        }
    }

    /**
     * Hitung progress smooth berdasarkan waktu.
     * Estimasi total 3 menit untuk generate video.
     */
    private fun calculateSmoothProgress(runId: Long): Int {
        val startTime = startTimes.getOrPut(runId) { System.currentTimeMillis() }
        val elapsed = System.currentTimeMillis() - startTime

        // Estimasi total: 10 menit (bisa disesuaikan)
        val estimatedTotal = 600_000L

        // Progress 10% → 95% (smooth based on time)
        val ratio = elapsed.toFloat() / estimatedTotal
        val progress = 10 + (ratio * 85).toInt()

        return progress.coerceIn(10, 95)
    }

    private fun getArtifactUrlSync(token: String, runId: Long): String {
        return try {
            val url = "https://api.github.com/repos/$OWNER/$REPO/actions/runs/$runId/artifacts"
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github+json")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            response.close()

            val json = JSONObject(body)
            val artifacts = json.getJSONArray("artifacts")
            if (artifacts.length() > 0) artifacts.getJSONObject(0).getString("archive_download_url")
            else ""
        } catch (e: Exception) {
            AutoLogSaver.logError(TAG, "getArtifactUrlSync failed", e)
            ""
        }
    }

    fun downloadArtifactSync(context: Context, token: String, artifactUrl: String): String? {
        return try {
            val request = Request.Builder()
                .url(artifactUrl)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github+json")
                .build()

            val response = noRedirectClient.newCall(request).execute()
            val code = response.code
            var downloadUrl: String? = null

            if (code == 302) {
                downloadUrl = response.header("Location")
                response.close()
            } else if (response.isSuccessful) {
                downloadUrl = artifactUrl
                response.close()
            } else {
                response.close()
                return null
            }

            if (downloadUrl.isNullOrEmpty()) return null

            val s3Request = Request.Builder().url(downloadUrl).build()
            val s3Response = client.newCall(s3Request).execute()
            if (!s3Response.isSuccessful) { s3Response.close(); return null }

            val downloadsDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "YADVideoEditor"
            )
            downloadsDir.mkdirs()

            val zipFile = File(downloadsDir, "video_${System.currentTimeMillis()}.zip")
            s3Response.body?.byteStream()?.use { input ->
                zipFile.outputStream().use { input.copyTo(it) }
            }
            s3Response.close()

            val videoFile = unzipArtifact(zipFile, downloadsDir)
            zipFile.delete()
            videoFile?.absolutePath
        } catch (e: Exception) {
            AutoLogSaver.logError(TAG, "downloadArtifactSync failed", e)
            null
        }
    }

    private fun unzipArtifact(zipFile: File, destDir: File): File? {
        return try {
            ZipInputStream(zipFile.inputStream()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name.endsWith(".mp4", ignoreCase = true)) {
                        val outFile = File(destDir, entry.name.substringAfterLast("/"))
                        outFile.outputStream().use { zis.copyTo(it) }
                        return outFile
                    }
                    entry = zis.nextEntry
                }
            }
            null
        } catch (e: Exception) {
            AutoLogSaver.logError(TAG, "unzipArtifact failed", e)
            null
        }
    }

    fun cleanupArtifact(token: String, runId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "https://api.github.com/repos/$OWNER/$REPO/actions/runs/$runId/artifacts"
                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $token")
                    .header("Accept", "application/vnd.github+json")
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""
                response.close()

                val json = JSONObject(body)
                val artifacts = json.getJSONArray("artifacts")
                for (i in 0 until artifacts.length()) {
                    val artifactId = artifacts.getJSONObject(i).getLong("id")
                    val delUrl = "https://api.github.com/repos/$OWNER/$REPO/actions/artifacts/$artifactId"
                    val delReq = Request.Builder()
                        .url(delUrl)
                        .header("Authorization", "Bearer $token")
                        .header("Accept", "application/vnd.github+json")
                        .delete()
                        .build()
                    client.newCall(delReq).execute().close()
                }
            } catch (e: Exception) {
                AutoLogSaver.logError(TAG, "cleanupArtifact failed", e)
            }
        }
    }
}
