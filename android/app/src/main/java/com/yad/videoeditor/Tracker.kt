package com.yad.videoeditor

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tracker — logging terpusat untuk semua aktivitas app.
 *
 * Format:
 *  - Text readable di Logcat & file
 *  - JSON line untuk parsing otomatis
 *
 * Cara pakai:
 *   Tracker.init(context)
 *   Tracker.event("MainActivity", "user_click", mapOf("button" to "credit"))
 *   Tracker.error("AdHelper", "banner_fail", "no fill", exception)
 *   Tracker.lifecycle("MainActivity", "onCreate")
 *   Tracker.adEvent("StartApp", "interstitial", "shown", mapOf("duration_ms" to "5000"))
 *   Tracker.gateEvent("video_editor", "access_granted")
 *   Tracker.authEvent("google", "success", mapOf("uid" to "abc123"))
 *   Tracker.fcmEvent("token_received", mapOf("token_prefix" to "abc..."))
 *
 * Semua event otomatis ke file log via AutoLogSaver.
 */
object Tracker {

    private const val TAG = "Tracker"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val jsonDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)

    @Volatile
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        jsonDateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
        AutoLogSaver.log(TAG, "Tracker initialized")
    }

    // ============================================================
    //  CORE — semua log lewat sini
    // ============================================================
    @PublishedApi
    internal fun log(
        category: String,
        source: String,
        action: String,
        detail: String = "",
        data: Map<String, Any?> = emptyMap(),
        exception: Throwable? = null
    ) {
        try {
            val now = Date()
            val timeText = dateFormat.format(now)
            val timeJson = jsonDateFormat.format(now)

            // Text line
            val textLine = buildString {
                append("[$timeText] [$category] [$source] $action")
                if (detail.isNotEmpty()) append(" — $detail")
                if (data.isNotEmpty()) {
                    append(" {")
                    append(data.entries.joinToString(", ") { "${it.key}=${it.value}" })
                    append("}")
                }
            }

            // Log ke Logcat
            when (category) {
                "ERROR", "CRASH" -> Log.e(source, "$action — $detail", exception)
                "WARN" -> Log.w(source, "$action — $detail")
                else -> Log.d(source, "$action — $detail")
            }

            // Simpan ke file (text + json)
            AutoLogSaver.logRaw(textLine)

            // JSON line
            val jsonObj = JSONObject().apply {
                put("ts", timeJson)
                put("category", category)
                put("source", source)
                put("action", action)
                if (detail.isNotEmpty()) put("detail", detail)
                if (data.isNotEmpty()) {
                    val dataObj = JSONObject()
                    data.forEach { (k, v) ->
                        if (v == null) dataObj.put(k, JSONObject.NULL)
                        else dataObj.put(k, v)
                    }
                    put("data", dataObj)
                }
                if (exception != null) {
                    put("exception_type", exception.javaClass.simpleName)
                    put("exception_message", exception.message ?: "")
                    put("stack_trace", exception.stackTraceToString())
                }
            }
            AutoLogSaver.logRaw("__JSON__" + jsonObj.toString())
        } catch (e: Exception) {
            // Jangan sampai tracking bikin crash
            Log.e(TAG, "log failed: ${e.message}")
        }
    }

    // ============================================================
    //  PUBLIC API — kategori
    // ============================================================

    /** Lifecycle Activity: onCreate, onResume, onPause, onDestroy */
    fun lifecycle(source: String, event: String, data: Map<String, Any?> = emptyMap()) {
        log("LIFECYCLE", source, event, "", data)
    }

    /** User action: klik tombol, buka menu, navigasi */
    fun userAction(source: String, action: String, data: Map<String, Any?> = emptyMap()) {
        log("USER", source, action, "", data)
    }

    /** Ad event: banner, interstitial, rewarded */
    fun adEvent(network: String, adType: String, event: String, data: Map<String, Any?> = emptyMap()) {
        log("AD", network, "$adType/$event", "", data)
    }

    /** Gate event: hasAccess, grantAccess, expired, fallback */
    fun gateEvent(feature: String, event: String, data: Map<String, Any?> = emptyMap()) {
        log("GATE", "Gate", "$feature/$event", "", data)
    }

    /** Auth event: login, logout, token refresh */
    fun authEvent(provider: String, event: String, data: Map<String, Any?> = emptyMap()) {
        log("AUTH", provider, event, "", data)
    }

    /** FCM event: token, message, notification */
    fun fcmEvent(event: String, data: Map<String, Any?> = emptyMap()) {
        log("FCM", "FCM", event, "", data)
    }

    /** Network: request start/success/fail */
    fun networkEvent(endpoint: String, event: String, data: Map<String, Any?> = emptyMap()) {
        log("NETWORK", "Network", "$endpoint/$event", "", data)
    }

    /** State change: shared prefs write, flag set */
    fun stateChange(source: String, key: String, value: Any?, data: Map<String, Any?> = emptyMap()) {
        log("STATE", source, key, "value=$value", data)
    }

    /** Info umum */
    fun info(source: String, action: String, detail: String = "", data: Map<String, Any?> = emptyMap()) {
        log("INFO", source, action, detail, data)
    }

    /** Warning */
    fun warn(source: String, action: String, detail: String = "", data: Map<String, Any?> = emptyMap()) {
        log("WARN", source, action, detail, data)
    }

    /** Error + optional exception */
    fun error(source: String, action: String, detail: String = "", e: Throwable? = null, data: Map<String, Any?> = emptyMap()) {
        log("ERROR", source, action, detail, data, e)
    }

    /** Performance: ukur durasi eksekusi */
    inline fun <T> measure(source: String, action: String, block: () -> T): T {
        val start = System.currentTimeMillis()
        try {
            val result = block()
            val duration = System.currentTimeMillis() - start
            log("PERF", source, action, "duration_ms=$duration", mapOf("duration_ms" to duration))
            return result
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - start
            log("PERF", source, action, "failed_after_ms=$duration", mapOf("duration_ms" to duration), e)
            throw e
        }
    }
}
