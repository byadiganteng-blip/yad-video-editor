package com.yad.videoeditor

import android.content.Context
import android.os.Environment
import android.util.Log
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

    // Client biasa — follow redirect
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    // Client khusus — TIDAK follow redirect (untuk artifact download)
    private val noRedirectClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    // ============================================================
    //  TRIGGER WORKFLOW
    // ============================================================
    fun triggerVideoWorkflow(
        context: Context,
        token: String,
        mode: String,
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
                    withContext(Dispatchers.Main) {
                        onError("HTTP $code: Gagal trigger workflow")
                    }
                    return@launch
                }

                AutoLogSaver.log(TAG, "Workflow triggered OK, waiting for run registration...")

                // Tunggu 3 detik — GitHub butuh waktu register run
                delay(3000)

                val runId = getLatestRunId(token)
                if (runId == 0L) {
                    withContext(Dispatchers.Main) {
                        onError("Gagal dapat run ID. Coba lagi.")
                    }
                } else {
                    AutoLogSaver.log(TAG, "Got runId: $runId")
                    withContext(Dispatchers.Main) {
                        onSuccess(runId)
                    }
                }
            } catch (e: Exception) {
                AutoLogSaver.logError(TAG, "triggerVideoWorkflow failed", e)
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Unknown error")
                }
            }
        }
    }

    // ============================================================
    //  GET LATEST RUN ID — retry + filter created_at
    // ============================================================
    private suspend fun getLatestRunId(token: String, retries: Int = 10): Long {
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

                    if (now - createdMs < 120_000) {  // 2 menit
                        val id = run.getLong("id")
                        AutoLogSaver.log(TAG, "Found new run: id=$id, age=${(now - createdMs)/1000}s")
                        return id
                    }
                }

                AutoLogSaver.log(TAG, "Run belum register, retry $attempt/$retries...")
                delay(3000)
            } catch (e: Exception) {
                AutoLogSaver.logError(TAG, "getLatestRunId attempt $attempt failed", e)
                delay(3000)
            }
        }
        AutoLogSaver.logError(TAG, "getLatestRunId GAGAL setelah $retries retry", null)
        return 0L
    }

    // ============================================================
    //  CHECK STATUS ONCE — untuk polling loop di Service
    //  Return:
    //    null                     → masih running (lanjut poll)
    //    Triple(true, url, "")    → selesai, download dari url
    //    Triple(false, "", msg)   → gagal
    // ============================================================
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

            AutoLogSaver.log(TAG, "checkStatusOnce: status=$status, conclusion=$conclusion")

            when (status) {
                "queued" -> {
                    onStatus("Menunggu antrian...", 10)
                    null
                }
                "in_progress" -> {
                    onStatus("Sedang membuat video...", 50)
                    null
                }
                "completed" -> {
                    if (conclusion == "success") {
                        val artifactUrl = getArtifactUrlSync(token, runId)
                        if (artifactUrl.isEmpty()) {
                            Triple(false, "", "Artifact tidak ditemukan")
                        } else {
                            Triple(true, artifactUrl, "")
                        }
                    } else {
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

    // ============================================================
    //  GET ARTIFACT URL — sync
    // ============================================================
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
            if (artifacts.length() > 0) {
                val artifact = artifacts.getJSONObject(0)
                val url = artifact.getString("archive_download_url")
                AutoLogSaver.log(TAG, "Artifact URL: $url")
                url
            } else {
                AutoLogSaver.log(TAG, "No artifacts found")
                ""
            }
        } catch (e: Exception) {
            AutoLogSaver.logError(TAG, "getArtifactUrlSync failed", e)
            ""
        }
    }

    // ============================================================
    //  DOWNLOAD ARTIFACT — handle redirect 302 ke S3
    // ============================================================
    fun downloadArtifactSync(context: Context, token: String, artifactUrl: String): String? {
        return try {
            AutoLogSaver.log(TAG, "Downloading artifact...")

            // STEP 1: Request dengan auth → dapat 302
            val request = Request.Builder()
                .url(artifactUrl)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github+json")
                .build()

            val response = noRedirectClient.newCall(request).execute()
            val code = response.code
            AutoLogSaver.log(TAG, "Artifact response: HTTP $code")

            var downloadUrl: String? = null

            if (code == 302) {
                downloadUrl = response.header("Location")
                response.close()
                AutoLogSaver.log(TAG, "Redirect to S3, following...")
            } else if (response.isSuccessful) {
                // Langsung download
                downloadUrl = artifactUrl
                response.close()
            } else {
                response.close()
                AutoLogSaver.logError(TAG, "Artifact HTTP $code", null)
                return null
            }

            if (downloadUrl.isNullOrEmpty()) {
                AutoLogSaver.logError(TAG, "Download URL kosong", null)
                return null
            }

            // STEP 2: Download dari S3 TANPA auth
            val s3Request = Request.Builder().url(downloadUrl).build()
            val s3Response = client.newCall(s3Request).execute()

            if (!s3Response.isSuccessful) {
                s3Response.close()
                AutoLogSaver.logError(TAG, "S3 HTTP ${s3Response.code}", null)
                return null
            }

            val downloadsDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "YADVideoEditor"
            )
            if (!downloadsDir.exists()) downloadsDir.mkdirs()

            val zipFile = File(downloadsDir, "video_${System.currentTimeMillis()}.zip")
            s3Response.body?.byteStream()?.use { input ->
                zipFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            s3Response.close()

            AutoLogSaver.log(TAG, "ZIP downloaded: ${zipFile.length()} bytes")

            // Unzip
            val videoFile = unzipArtifact(zipFile, downloadsDir)
            zipFile.delete()

            if (videoFile != null) {
                AutoLogSaver.log(TAG, "Video extracted: ${videoFile.absolutePath}")
                videoFile.absolutePath
            } else {
                AutoLogSaver.logError(TAG, "No video in ZIP", null)
                null
            }
        } catch (e: Exception) {
            AutoLogSaver.logError(TAG, "downloadArtifactSync failed", e)
            null
        }
    }

    // ============================================================
    //  UNZIP — cari .mp4
    // ============================================================
    private fun unzipArtifact(zipFile: File, destDir: File): File? {
        return try {
            ZipInputStream(zipFile.inputStream()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name.endsWith(".mp4", ignoreCase = true)) {
                        val outFile = File(destDir, entry.name.substringAfterLast("/"))
                        outFile.outputStream().use { output ->
                            zis.copyTo(output)
                        }
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

    // ============================================================
    //  CLEANUP — hapus artifact + run setelah selesai
    // ============================================================
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
                    val artifact = artifacts.getJSONObject(i)
                    val artifactId = artifact.getLong("id")

                    val delUrl = "https://api.github.com/repos/$OWNER/$REPO/actions/artifacts/$artifactId"
                    val delReq = Request.Builder()
                        .url(delUrl)
                        .header("Authorization", "Bearer $token")
                        .header("Accept", "application/vnd.github+json")
                        .delete()
                        .build()

                    val delResp = client.newCall(delReq).execute()
                    val dc = delResp.code
                    delResp.close()
                    AutoLogSaver.log(TAG, "Artifact deleted: $artifactId (HTTP $dc)")
                }

                // Hapus run
                val runDelUrl = "https://api.github.com/repos/$OWNER/$REPO/actions/runs/$runId"
                val runDelReq = Request.Builder()
                    .url(runDelUrl)
                    .header("Authorization", "Bearer $token")
                    .header("Accept", "application/vnd.github+json")
                    .delete()
                    .build()

                val runDelResp = client.newCall(runDelReq).execute()
                val rc = runDelResp.code
                runDelResp.close()
                AutoLogSaver.log(TAG, "Workflow run deleted: $runId (HTTP $rc)")
            } catch (e: Exception) {
                AutoLogSaver.logError(TAG, "cleanupArtifact failed", e)
            }
        }
    }
}
