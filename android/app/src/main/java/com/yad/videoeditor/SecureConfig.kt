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

    private fun decodeGithubToken(): String {
        return try {
            BuildConfig.G_P1 + BuildConfig.G_P2 + BuildConfig.G_P3 +
            BuildConfig.G_P4 + BuildConfig.G_P5 + BuildConfig.G_P6
        } catch (_: Exception) { "" }
    }

    fun getGithubToken(): String = decodeGithubToken()
    fun hasGithubToken(): Boolean = getGithubToken().isNotEmpty()

    // Method ini WAJIB ada supaya AdminActivity & SettingsActivity tidak error
    fun setGithubToken(t: String) {
        p()?.edit()?.putString("gh_token_manual", t)?.apply()
    }
    fun clearGithubToken() {
        p()?.edit()?.remove("gh_token_manual")?.apply()
    }

    fun getHfApiKey(): String = ""
    fun hasHfApiKey(): Boolean = false

    private fun decodeUser(): String {
        return try { BuildConfig.U_P1 + BuildConfig.U_P2 }
        catch (_: Exception) { "" }
    }

    private fun decodeRepo(): String {
        return try { BuildConfig.R_P1 + BuildConfig.R_P2 }
        catch (_: Exception) { "" }
    }

    fun getCredit(): String = try { BuildConfig.CREDIT }
        catch (_: Exception) { "Created by KARYADI, Coding by KARYADI" }

    fun getGithubUser(): String = decodeUser().ifEmpty { "byadiganteng-blip" }
    fun getGithubRepo(): String = decodeRepo().ifEmpty { "yad-video-editor" }

    fun getString(key: String, def: String = ""): String = p()?.getString(key, def) ?: def
    fun setString(key: String, v: String) { p()?.edit()?.putString(key, v)?.apply() }

    private const val KEY_ADMIN_EMAIL = "admin_email"
    private const val KEY_IS_ADMIN    = "is_admin"

    fun getAdminEmail(): String = p()?.getString(KEY_ADMIN_EMAIL, "") ?: ""
    fun setAdminEmail(email: String) { p()?.edit()?.putString(KEY_ADMIN_EMAIL, email)?.apply() }
    fun isAdmin(): Boolean = p()?.getBoolean(KEY_IS_ADMIN, false) ?: false
    fun setIsAdmin(v: Boolean) { p()?.edit()?.putBoolean(KEY_IS_ADMIN, v)?.apply() }
    fun clearAdmin() {
        p()?.edit()?.remove(KEY_ADMIN_EMAIL)?.remove(KEY_IS_ADMIN)?.apply()
    }
}
