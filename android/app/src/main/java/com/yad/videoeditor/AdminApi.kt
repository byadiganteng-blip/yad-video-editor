package com.yad.videoeditor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * GitHub API client
 * Created by KARYADI, Coding by KARYADI
 * Fixed by Auto-Fix Script
 */
object AdminApi {

    private const val OWNER = "byadiganteng-blip"
    private const val REPO = "yad-video-editor"   // ✅ FIXED
    private const val BRANCH = "main"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun req(url: String, method: String = "GET", body: String? = null): Request {
        val b = Request.Builder()
            .url(url)
            .header("Authorization", "token ${SecureConfig.getGithubToken()}")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "YadApp")

        when (method.uppercase()) {
            "POST", "PATCH", "PUT" -> b.method(
                method.uppercase(),
                (body ?: "{}").toRequestBody("application/json".toMediaTypeOrNull())
            )
            "DELETE" -> b.delete()
            else -> b.get()
        }
        return b.build()
    }

    // ============================================================
    //  TRIGGER BUILD APK
    // ============================================================
    suspend fun triggerBuild(workflow: String = "build-apk.yml", ref: String = BRANCH): Pair<Boolean, String> =
        withContext(Dispatchers.IO) {
            try {
                val url = "https://api.github.com/repos/$OWNER/$REPO/actions/workflows/$workflow/dispatches"
                val resp = client.newCall(req(url, "POST", """{"ref":"$ref"}""")).execute()
                if (resp.isSuccessful) true to "Build triggered"
                else false to "HTTP ${resp.code}"
            } catch (e: Exception) {
                false to "Error: ${e.message}"
            }
        }

    // ============================================================
    //  TRIGGER GENERATE VIDEO (generate_image.yml)
    // ============================================================
    suspend fun triggerVideoGenerate(
        prompt: String,
        voice: String = "male_id",
        watermark: String = "",
        showSubtitle: Boolean = true,
        subtitleStyle: String = "neon",
        scenesCount: String = "8",
        imageStyle: String = "cinematic"
    ): Pair<Boolean, Long?> = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.github.com/repos/$OWNER/$REPO/" +
                      "actions/workflows/generate_image.yml/dispatches"

            val json = """
                {
                  "ref": "$BRANCH",
                  "inputs": {
                    "prompt": ${JSONObject.quote(prompt)},
                    "voice": "$voice",
                    "watermark": "${watermark.replace("\"", "\\\"")}",
                    "show_subtitle": "$showSubtitle",
                    "subtitle_style": "$subtitleStyle",
                    "scenes_count": "$scenesCount",
                    "image_style": "$imageStyle"
                  }
                }
            """.trimIndent()

            println("Trigger URL: $url")
            println("Body: $json")

            val resp = client.newCall(req(url, "POST", json)).execute()
            val body = resp.body?.string() ?: ""
            println("Response: ${resp.code} / $body")

            if (resp.code == 204) {
                delay(3000)
                val runId = getLatestVideoRunId()
                true to runId
            } else {
                false to null
            }
        } catch (e: Exception) {
            println("Error: ${e.message}")
            false to null
        }
    }

    private suspend fun getLatestVideoRunId(): Long? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.github.com/repos/$OWNER/$REPO/" +
                      "actions/workflows/generate_image.yml/runs?per_page=1"
            val resp = client.newCall(req(url)).execute()
            val arr = JSONObject(resp.body?.string() ?: "{}")
                .optJSONArray("workflow_runs") ?: return@withContext null
            if (arr.length() == 0) return@withContext null
            arr.getJSONObject(0).getLong("id")
        } catch (e: Exception) {
            null
        }
    }

    // ============================================================
    //  STATUS WORKFLOW
    // ============================================================
    suspend fun getRunStatus(runId: Long): WorkflowStatus = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.github.com/repos/$OWNER/$REPO/actions/runs/$runId"
            val resp = client.newCall(req(url)).execute()
            val json = JSONObject(resp.body?.string() ?: "{}")
            WorkflowStatus(
                status = json.optString("status", "unknown"),
                conclusion = json.optString("conclusion", ""),
                htmlUrl = json.optString("html_url", "")
            )
        } catch (e: Exception) {
            WorkflowStatus("unknown", "", "")
        }
    }

    data class WorkflowStatus(
        val status: String,
        val conclusion: String,
        val htmlUrl: String
    )

    // ============================================================
    //  LIST RUNS (untuk Admin)
    // ============================================================
    suspend fun listRuns(limit: Int = 30): Pair<Boolean, List<WorkflowRun>> =
        withContext(Dispatchers.IO) {
            try {
                val url = "https://api.github.com/repos/$OWNER/$REPO/actions/runs?per_page=$limit"
                val resp = client.newCall(req(url)).execute()
                if (!resp.isSuccessful) return@withContext false to emptyList()
                val arr = JSONObject(resp.body?.string() ?: "{}").optJSONArray("workflow_runs")
                val runs = mutableListOf<WorkflowRun>()
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val r = arr.getJSONObject(i)
                        runs.add(WorkflowRun(
                            r.getLong("id"),
                            r.optString("name"),
                            r.optString("status"),
                            r.optString("conclusion"),
                            r.optString("html_url"),
                            r.optInt("run_number"),
                            r.optString("created_at")
                        ))
                    }
                }
                true to runs
            } catch (e: Exception) {
                false to emptyList()
            }
        }

    data class WorkflowRun(
        val id: Long, val name: String, val status: String,
        val conclusion: String, val htmlUrl: String,
        val runNumber: Int, val createdAt: String
    )
}
