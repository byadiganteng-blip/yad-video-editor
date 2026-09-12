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

    /**
     * Ambil token GitHub.
     * Prioritas:
     *   1. Token yang di-embed di BuildConfig (GH_TOKEN)
     *   2. Token manual dari SharedPreferences (jika user set)
     */
    fun getGithubToken(): String {
        // Coba BuildConfig dulu
        try {
            val embedded = BuildConfig.GH_TOKEN
            if (embedded.isNotEmpty()) return embedded
        } catch (_: Exception) {}

        // Fallback ke SharedPreferences
        return try {
            p()?.getString("gh_token_manual", "") ?: ""
        } catch (_: Exception) { "" }
    }

    fun hasGithubToken(): Boolean = getGithubToken().isNotEmpty()

    fun setGithubToken(t: String) {
        p()?.edit()?.putString("gh_token_manual", t)?.apply()
    }

    fun clearGithubToken() {
        p()?.edit()?.remove("gh_token_manual")?.apply()
    }

    fun getCredit(): String = "Created by KARYADI, Coding by KARYADI"
    fun getGithubUser(): String = "byadiganteng-blip"
    fun getGithubRepo(): String = "yad-video-editor"

    fun getString(key: String, def: String = ""): String =
        p()?.getString(key, def) ?: def

    fun setString(key: String, v: String) {
        p()?.edit()?.putString(key, v)?.apply()
    }

    // ADMIN
    private const val KEY_ADMIN_EMAIL = "admin_email"
    private const val KEY_IS_ADMIN    = "is_admin"
    private const val KEY_TAP_COUNT   = "admin_tap_count"

    private const val ADMIN_EMAIL = "ynuraini686@gmail.com"
    private const val ADMIN_PASS  = "YADIGANTENG"

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
        val ok = email.trim().equals(ADMIN_EMAIL, ignoreCase = true) &&
                 password == ADMIN_PASS
        if (ok) {
            setAdminEmail(email.trim())
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
