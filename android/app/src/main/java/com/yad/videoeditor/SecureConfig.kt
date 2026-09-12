package com.yad.videoeditor

import android.content.Context
import android.content.SharedPreferences

/**
 * Secure config (nullable, thread-safe)
 * Created by KARYADI, Coding by KARYADI
 */
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

    fun getGithubToken(): String = p()?.getString("gh_token", "") ?: ""
    fun setGithubToken(t: String) { p()?.edit()?.putString("gh_token", t)?.apply() }
    fun clearGithubToken() { p()?.edit()?.remove("gh_token")?.apply() }
    fun hasGithubToken(): Boolean = getGithubToken().isNotEmpty()

    fun getAdminEmail(): String = p()?.getString("admin_email", "") ?: ""
    fun setAdminEmail(e: String) { p()?.edit()?.putString("admin_email", e)?.apply() }
    fun clearAdmin() { p()?.edit()?.remove("admin_email")?.apply() }
    fun isAdminLoggedIn(): Boolean = getAdminEmail().isNotEmpty()

    fun getString(key: String, def: String = ""): String =
        p()?.getString(key, def) ?: def
    fun setString(key: String, v: String) { p()?.edit()?.putString(key, v)?.apply() }
}
