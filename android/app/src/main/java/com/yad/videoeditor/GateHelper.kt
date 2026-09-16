package com.yad.videoeditor

import android.content.Context
import android.content.SharedPreferences

/**
 * GateHelper — kontrol akses fitur berbasis rewarded video.
 *
 * Behavior:
 *  - Setiap fitur punya slot akses 30 menit
 *  - Setelah nonton rewarded selesai → grantAccess(feature)
 *  - Akses valid selama 30 menit sejak grant
 *  - Setelah 30 menit → harus nonton lagi
 *
 * Kalau iklan GAGAL LOAD → caller tetap boleh grant akses (graceful).
 * Kalau user SENGAJA keluar / skip → onReward tidak terpanggil →
 *   akses tidak diberikan.
 */
object GateHelper {

    private const val PREFS = "yad_gate"
    private const val ACCESS_DURATION_MS = 30L * 60L * 1000L  // 30 menit

    // Feature IDs — konsisten dengan pemanggil
    const val FEATURE_VIDEO_EDITOR   = "video_editor"
    const val FEATURE_AI_TEXT_VIDEO  = "ai_text_video"
    const val FEATURE_VIDEO_LIST     = "video_list"

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    /**
     * Cek apakah fitur masih bisa diakses (belum kadaluarsa).
     */
    fun hasAccess(context: Context, feature: String): Boolean {
        val until = prefs(context).getLong("${feature}_until", 0L)
        return System.currentTimeMillis() < until
    }

    /**
     * Beri akses 30 menit ke fitur.
     */
    fun grantAccess(context: Context, feature: String) {
        val until = System.currentTimeMillis() + ACCESS_DURATION_MS
        prefs(context).edit()
            .putLong("${feature}_until", until)
            .apply()
    }

    /**
     * Sisa waktu akses dalam detik (0 kalau tidak ada).
     */
    fun remainingSeconds(context: Context, feature: String): Long {
        val until = prefs(context).getLong("${feature}_until", 0L)
        val remaining = until - System.currentTimeMillis()
        return if (remaining > 0) remaining / 1000 else 0
    }

    /**
     * Reset akses (untuk testing).
     */
    fun clearAccess(context: Context, feature: String) {
        prefs(context).edit()
            .remove("${feature}_until")
            .apply()
    }

    fun clearAll(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
