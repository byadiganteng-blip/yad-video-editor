package com.yad.videoeditor

import android.content.Context
import android.content.SharedPreferences

object SecureConfig {
    private const val PREF = "yad_secure"

    @Volatile
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            synchronized(this) {
                if (prefs == null) {
                    prefs = context.applicationContext
                        .getSharedPreferences(PREF, Context.MODE_PRIVATE)
                }
            }
        }
    }

    private fun p(): SharedPreferences? = prefs

    // ============================================================
    //  GITHUB TOKEN — dari SharedPreferences
    // ============================================================
    fun getGithubToken(): String {
        return try {
            val fromPrefs = p()?.getString("gh_token", "") ?: ""
            if (fromPrefs.isNotEmpty()) return fromPrefs

            // Fallback ke BuildConfig (kosong kalau tidak di-embed)
            try {
                val fromBc = BuildConfig.GH_TOKEN
                if (fromBc.isNotEmpty()) return fromBc
            } catch (_: Exception) {}

            ""
        } catch (_: Exception) { "" }
    }

    fun hasGithubToken(): Boolean = getGithubToken().isNotEmpty()

    fun setGithubToken(t: String) {
        p()?.edit()?.putString("gh_token", t.trim())?.apply()
    }

    fun clearGithubToken() {
        p()?.edit()?.remove("gh_token")?.apply()
    }

    fun getCredit(): String = "Created by KARYADI, Coding by KARYADI"
    fun getGithubUser(): String = "byadiganteng-blip"
    fun getGithubRepo(): String = "yad-video-editor"

    // ============================================================
    //  ADMIN
    // ============================================================
    private const val KEY_ADMIN_EMAIL = "admin_email"
    private const val KEY_IS_ADMIN = "is_admin"
    private const val KEY_TAP_COUNT = "admin_tap_count"

    private const val ADMIN_EMAIL = "ynuraini686@gmail.com"
    private const val ADMIN_PASS = "YADIGANTENG"

    fun getAdminEmail(): String = p()?.getString(KEY_ADMIN_EMAIL, "") ?: ""
    fun setAdminEmail(email: String) {
        p()?.edit()?.putString(KEY_ADMIN_EMAIL, email)?.apply()
    }
    fun isAdmin(): Boolean = p()?.getBoolean(KEY_IS_ADMIN, false) ?: false
    fun setIsAdmin(v: Boolean) {
        p()?.edit()?.putBoolean(KEY_IS_ADMIN, v)?.apply()
    }
    fun clearAdmin() {
        p()?.edit()?.remove(KEY_ADMIN_EMAIL)?.remove(KEY_IS_ADMIN)?.apply()
    }

    fun verifyAdminCredentials(email: String, password: String): Boolean {
        val e = email.trim().lowercase()
        val adminEmail = try {
            BuildConfig.ADMIN_EMAIL.ifEmpty { ADMIN_EMAIL }
        } catch (_: Exception) { ADMIN_EMAIL }
        val adminPass = try {
            BuildConfig.ADMIN_PASS.ifEmpty { ADMIN_PASS }
        } catch (_: Exception) { ADMIN_PASS }

        val ok = e == adminEmail.lowercase() && password == adminPass
        if (ok) {
            setAdminEmail(e)
            setIsAdmin(true)
        }
        return ok
    }

    fun incrementTapCount(): Int {
        val current = p()?.getInt(KEY_TAP_COUNT, 0) ?: 0
        val next = current + 1
        p()?.edit()?.putInt(KEY_TAP_COUNT, next)?.apply()
        return next
    }

    fun resetTapCount() { p()?.edit()?.putInt(KEY_TAP_COUNT, 0)?.apply() }
    fun getTapCount(): Int = p()?.getInt(KEY_TAP_COUNT, 0) ?: 0
    fun getAdminEmailConst(): String = ADMIN_EMAIL
    fun getAdminPassConst(): String = ADMIN_PASS
}
