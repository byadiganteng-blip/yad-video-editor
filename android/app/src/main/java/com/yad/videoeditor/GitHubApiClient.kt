package com.yad.videoeditor

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * GitHubApiClient — trigger workflow & download artifact.
 *
 * Semua generate video dilakukan di GitHub Actions.
 * APK hanya:
 *   1. Trigger workflow
 *   2. Poll status
 *   3. Download hasil
 */
object GitHubApiClient {

    private const val TAG = "GitHubApiClient"
    private const val OWNER = "byadiganteng-blip"
    private const val REPO = "yad-video-editor"
    private const val WORKFLOW_FILE = "generate-video.yml"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Trigger workflow generate video.
     */
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

                if (code == 204) {
                    AutoLogSaver.log(TAG, "Workflow triggered OK")
                    // Ambil run ID terbaru
                    val runId = getLatestRunId(token)
                    withContext(Dispatchers.Main) {
                        onSuccess(runId)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onError("HTTP $code: Gagal trigger workflow")
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

    /**
     * Ambil run ID workflow terbaru.
     */
    private suspend fun getLatestRunId(token: String): Long {
        return try {
            val url = "https://api.github.com/repos/$OWNER/$REPO/actions/workflows/$WORKFLOW_FILE/runs?per_page=1"
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
            if (runs.length() > 0) {
                runs.getJSONObject(0).getLong("id")
            } else {
                0L
            }
        } catch (e: Exception) {
            AutoLogSaver.logError(TAG, "getLatestRunId failed", e)
            0L
        }
    }

    /**
     * Cek status workflow.
     */
    fun checkWorkflowStatus(
        context: Context,
        token: String,
        runId: Long,
        onStatus: (String, Int) -> Unit,
        onComplete: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
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

                when (status) {
                    "queued" -> withContext(Dispatchers.Main) {
                        onStatus("Menunggu antrian", 10)
                    }
                    "in_progress" -> withContext(Dispatchers.Main) {
                        onStatus("Sedang membuat video", 50)
                    }
                    "completed" -> {
                        if (conclusion == "success") {
                            // Ambil artifact URL
                            val artifactUrl = getArtifactUrl(token, runId)
                            withContext(Dispatchers.Main) {
                                onStatus("Video selesai", 100)
                                onComplete(artifactUrl)
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                onError("Workflow gagal: $conclusion")
                            }
                        }
                    }
                    else -> withContext(Dispatchers.Main) {
                        onStatus("Status: $status", 30)
                    }
                }
            } catch (e: Exception) {
                AutoLogSaver.logError(TAG, "checkWorkflowStatus failed", e)
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Unknown error")
                }
            }
        }
    }

    /**
     * Ambil artifact URL dari run.
     */
    private suspend fun getArtifactUrl(token: String, runId: Long): String {
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
                artifact.getString("archive_download_url")
            } else {
                ""
            }
        } catch (e: Exception) {
            AutoLogSaver.logError(TAG, "getArtifactUrl failed", e)
            ""
        }
    }

    /**
     * Download artifact — simpan ke folder Downloads.
     */
    fun downloadArtifact(
        context: Context,
        token: String,
        artifactUrl: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Request.Builder()
                    .url(artifactUrl)
                    .header("Authorization", "Bearer $token")
                    .header("Accept", "application/vnd.github+json")
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        onError("Download gagal: ${response.code}")
                    }
                    return@launch
                }

                // Simpan ke folder Downloads
                val downloadsDir = File(
                    android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS
                    ),
                    "YADVideoEditor"
                )
                if (!downloadsDir.exists()) downloadsDir.mkdirs()

                val zipFile = File(downloadsDir, "video_${System.currentTimeMillis()}.zip")
                response.body?.byteStream()?.use { input ->
                    zipFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                response.close()

                // Unzip
                val videoFile = unzipArtifact(zipFile, downloadsDir)
                zipFile.delete()

                if (videoFile != null) {
                    withContext(Dispatchers.Main) {
                        onSuccess(videoFile.absolutePath)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onError("Tidak ada file video di artifact")
                    }
                }
            } catch (e: Exception) {
                AutoLogSaver.logError(TAG, "downloadArtifact failed", e)
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Unknown error")
                }
            }
        }
    }

    /**
     * Unzip artifact — cari file .mp4.
     */
    private fun unzipArtifact(zipFile: File, destDir: File): File? {
        return try {
            java.util.zip.ZipInputStream(zipFile.inputStream()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name.endsWith(".mp4")) {
                        val outFile = File(destDir, entry.name)
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
}
