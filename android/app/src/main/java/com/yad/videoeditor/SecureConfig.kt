package com.yad.videoeditor

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object SecureConfig {
    private const val PREF = "yad_secure"

    @Volatile
    private var prefs: SharedPreferences? = null

    @Volatile
    private var cachedToken: String = ""

    fun init(context: Context) {
        if (prefs == null) {
            synchronized(this) {
                if (prefs == null) {
                    prefs = context.applicationContext
                        .getSharedPreferences(PREF, Context.MODE_PRIVATE)
                    cachedToken = prefs?.getString("gh_token", "") ?: ""
                }
            }
        }
    }

    private fun p(): SharedPreferences? = prefs

    fun getGithubToken(): String {
        if (cachedToken.isNotEmpty()) return cachedToken
        return try { p()?.getString("gh_token", "") ?: "" }
        catch (_: Exception) { "" }
    }

    fun fetchTokenFromFirestore(onDone: (Boolean) -> Unit = {}) {
        GlobalScope.launch {
            try {
                val ok = FirebaseManager.loginAnonymous()
                if (!ok) { onDone(false); return@launch }
                val token = FirebaseManager.fetchGithubToken()
                if (token != null && token.startsWith("ghp_")) {
                    setGithubToken(token)
                    onDone(true)
                } else onDone(false)
            } catch (e: Exception) { onDone(false) }
        }
    }

    fun hasGithubToken(): Boolean = getGithubToken().isNotEmpty()

    fun setGithubToken(t: String) {
        val trimmed = t.trim()
        cachedToken = trimmed
        p()?.edit()?.putString("gh_token", trimmed)?.apply()
    }

    fun clearGithubToken() {
        cachedToken = ""
        p()?.edit()?.remove("gh_token")?.apply()
    }

    fun getCredit(): String = "Created by KARYADI, Coding by KARYADI"
    fun getGithubUser(): String = "byadiganteng-blip"
    fun getGithubRepo(): String = "yad-video-editor"

    // ADMIN
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
        val ok = e == ADMIN_EMAIL.lowercase() && password == ADMIN_PASS
        if (ok) { setAdminEmail(e); setIsAdmin(true) }
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
