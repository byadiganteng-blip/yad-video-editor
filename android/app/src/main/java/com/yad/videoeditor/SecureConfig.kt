package com.yad.videoeditor

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader

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
                    // ═══ FITUR LAMA: auto-embed token saat pertama init ═══
                    try {
                        val emb = decodeToken()
                        if (emb.isNotEmpty() &&
                            (prefs?.getString("gh_token", "") ?: "").isEmpty()) {
                            prefs?.edit()?.putString("gh_token", emb)?.apply()
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    private fun p(): SharedPreferences? = prefs

    // ═══════════════════════════════════════════════════════════
    // FITUR LAMA: Gabungkan token dari 6 bagian BuildConfig
    // ═══════════════════════════════════════════════════════════
    private fun decodeToken(): String {
        return try {
            BuildConfig.G_P1 + BuildConfig.G_P2 + BuildConfig.G_P3 +
            BuildConfig.G_P4 + BuildConfig.G_P5 + BuildConfig.G_P6
        } catch (_: Exception) { "" }
    }

    private fun decodeUser(): String {
        return try { BuildConfig.U_P1 + BuildConfig.U_P2 }
        catch (_: Exception) { "" }
    }

    private fun decodeRepo(): String {
        return try { BuildConfig.R_P1 + BuildConfig.R_P2 }
        catch (_: Exception) { "" }
    }

    // ═══════════════════════════════════════════════════════════
    // FITUR BARU: Baca token dari file .txt yang di-upload user
    // ═══════════════════════════════════════════════════════════
    fun readTokenFromUri(context: Context, uri: Uri): String? {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            val reader = BufferedReader(InputStreamReader(input))
            val content = reader.readText().trim()
            reader.close()
            val token = extractToken(content)
            if (token != null) setGithubToken(token)
            token
        } catch (e: Exception) {
            null
        }
    }

    private fun extractToken(raw: String): String? {
        val cleaned = raw.trim()
        if (cleaned.startsWith("ghp_") || cleaned.startsWith("github_pat_")) {
            return cleaned.lines().firstOrNull()?.trim()
        }
        val regex = Regex("""(ghp_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{30,})""")
        return regex.find(cleaned)?.value
    }

    // ═══════════════════════════════════════════════════════════
    // GETTER / SETTER TOKEN (prioritas: SharedPreferences > BuildConfig)
    // ═══════════════════════════════════════════════════════════
    fun getGithubToken(): String {
        // Prioritas 1: dari SharedPreferences (hasil upload txt)
        val fromPrefs = p()?.getString("gh_token", "") ?: ""
        if (fromPrefs.isNotEmpty()) return fromPrefs
        // Prioritas 2: fallback ke token embed di BuildConfig
        return decodeToken()
    }

    fun setGithubToken(t: String) { p()?.edit()?.putString("gh_token", t)?.apply() }
    fun clearGithubToken() { p()?.edit()?.remove("gh_token")?.apply() }
    fun hasGithubToken(): Boolean = getGithubToken().isNotEmpty()

    fun getHfApiKey(): String = p()?.getString("hf_api_key", "") ?: ""
    fun setHfApiKey(k: String) { p()?.edit()?.putString("hf_api_key", k)?.apply() }

    fun isEmbeddedToken(): Boolean = decodeToken().isNotEmpty()

    fun getCredit(): String = try { BuildConfig.CREDIT }
        catch (_: Exception) { "Created by KARYADI, Coding by KARYADI" }

    fun getGithubUser(): String = decodeUser().ifEmpty { "byadiganteng-blip" }
    fun getGithubRepo(): String = decodeRepo().ifEmpty { "yad-video-editor" }

    fun getString(key: String, def: String = ""): String = p()?.getString(key, def) ?: def
    fun setString(key: String, v: String) { p()?.edit()?.putString(key, v)?.apply() }

    // ═══ Fungsi tambahan untuk info sumber token ═══
    fun getTokenSource(): String {
        val fromPrefs = p()?.getString("gh_token", "") ?: ""
        return if (fromPrefs.isNotEmpty()) "upload_txt" else "embedded"
    }
}
