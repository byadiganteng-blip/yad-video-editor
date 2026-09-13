package com.yad.videoeditor

import android.content.Context
import android.util.Log
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * GoogleImageGenerator — cari gambar dari internet lalu jadikan video.
 *
 * Karena Google Custom Search API butuh API key + CX, kita pakai
 * sumber GRATIS tanpa key:
 *   - Unsplash Source (untuk foto)
 *   - Picsum (fallback)
 *
 * Fitur:
 *   - Timeout 30 detik
 *   - Retry 3x
 *   - User-Agent browser-like
 *   - Fallback ke DirectVideoGenerator kalau gambar gagal
 */
object GoogleImageGenerator {

    private const val TAG = "GoogleImageGenerator"

    // Sumber gambar gratis (tanpa API key)
    private const val UNSPLASH_URL = "https://source.unsplash.com/1280x720/?"
    private const val PICSUM_URL = "https://picsum.photos/1280/720"

    private const val TIMEOUT_MS = 30_000
    private const val MAX_RETRY = 3

    fun generateFromText(
        context: Context,
        query: String,
        durationSec: Int,
        outputPath: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                val cacheDir = context.cacheDir
                val imageFile = File(cacheDir, "google_img.jpg")

                // 1. Coba download gambar dari beberapa sumber
                val downloaded = tryDownloadImage(query, imageFile)

                if (downloaded) {
                    Log.d(TAG, "Gambar berhasil di-download: ${imageFile.length()} bytes")
                } else {
                    Log.w(TAG, "Gagal download gambar, fallback ke DirectVideoGenerator tanpa background")
                }

                // 2. Generate video (dengan atau tanpa background)
                DirectVideoGenerator.generateFromText(
                    context, query, durationSec, outputPath,
                    onSuccess = { path ->
                        try { imageFile.delete() } catch (_: Exception) {}
                        onSuccess(path)
                    },
                    onError = { err ->
                        Log.e(TAG, "DirectVideoGenerator error: $err")
                        onError(err)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Fatal error", e)
                onError("Error: ${e.message}")
            }
        }.start()
    }

    /**
     * Coba download gambar dari beberapa sumber (Unsplash, Picsum).
     * Return true kalau berhasil.
     */
    private fun tryDownloadImage(query: String, dest: File): Boolean {
        val urls = listOf(
            UNSPLASH_URL + URLEncoder.encode(query, "UTF-8"),
            PICSUM_URL
        )

        for (url in urls) {
            for (attempt in 1..MAX_RETRY) {
                try {
                    Log.d(TAG, "Download attempt $attempt: $url")
                    val ok = downloadFile(url, dest)
                    if (ok && dest.exists() && dest.length() > 1000) {
                        return true
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Attempt $attempt failed: ${e.message}")
                    if (attempt < MAX_RETRY) {
                        try { Thread.sleep(1000L * attempt) } catch (_: Exception) {}
                    }
                }
            }
        }
        return false
    }

    /**
     * Download file dengan HttpURLConnection + User-Agent browser.
     */
    private fun downloadFile(urlStr: String, dest: File): Boolean {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            conn.setRequestProperty("Accept", "image/*,*/*;q=0.8")
            conn.connect()

            val code = conn.responseCode
            Log.d(TAG, "HTTP $code dari $urlStr")

            if (code !in 200..299) {
                return false
            }

            conn.inputStream.use { input ->
                dest.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "downloadFile error: ${e.message}")
            false
        } finally {
            try { conn?.disconnect() } catch (_: Exception) {}
        }
    }
}
