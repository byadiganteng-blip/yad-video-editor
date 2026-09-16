package com.yad.videoeditor

import android.content.Context
import android.content.SharedPreferences

object GateHelper {

    private const val PREFS = "yad_gate"
    private const val ACCESS_DURATION_MS = 30L * 60L * 1000L

    const val FEATURE_VIDEO_EDITOR   = "video_editor"
    const val FEATURE_AI_TEXT_VIDEO  = "ai_text_video"
    const val FEATURE_VIDEO_LIST     = "video_list"

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    fun hasAccess(context: Context, feature: String): Boolean {
        val until = prefs(context).getLong("${feature}_until", 0L)
        val now = System.currentTimeMillis()
        val has = now < until
        val remaining = if (has) (until - now) / 1000 else 0
        Tracker.gateEvent(feature, "check", mapOf(
            "has_access" to has,
            "remaining_sec" to remaining,
            "until" to until
        ))
        return has
    }

    fun grantAccess(context: Context, feature: String) {
        val until = System.currentTimeMillis() + ACCESS_DURATION_MS
        prefs(context).edit().putLong("${feature}_until", until).apply()
        Tracker.gateEvent(feature, "grant", mapOf(
            "until" to until,
            "duration_ms" to ACCESS_DURATION_MS
        ))
    }

    fun remainingSeconds(context: Context, feature: String): Long {
        val until = prefs(context).getLong("${feature}_until", 0L)
        val remaining = until - System.currentTimeMillis()
        return if (remaining > 0) remaining / 1000 else 0
    }

    fun clearAccess(context: Context, feature: String) {
        prefs(context).edit().remove("${feature}_until").apply()
        Tracker.gateEvent(feature, "clear")
    }

    fun clearAll(context: Context) {
        prefs(context).edit().clear().apply()
        Tracker.gateEvent("ALL", "clear_all")
    }
}
