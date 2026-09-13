package com.yad.videoeditor

import android.content.Context
import java.io.File
import java.net.URL
import java.net.URLEncoder

/**
 * GoogleImageGenerator — cari gambar dari Google lalu jadikan video
 *
 * NOTE: Generate video dari gambar butuh MediaCodec juga.
 * Untuk simplifikasi, fungsi ini:
 *   1. Search gambar via Google Custom Search API
 *   2. Download gambar ke cache
 *   3. Kembalikan path gambar (untuk diproses lebih lanjut)
 *
 * Kalau mau langsung video, gabungkan dengan DirectVideoGenerator
 * yang sudah support gambar sebagai background.
 */
object GoogleImageGenerator {

    private const val GOOGLE_API_KEY = "YOUR_GOOGLE_API_KEY"
    private const val GOOGLE_CX = "YOUR_GOOGLE_CX"

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

                // 1. Search & download gambar
                val imageUrl = searchGoogleImage(query)
                if (imageUrl == null) {
                    onError("Gambar tidak ditemukan untuk: $query")
                    return@Thread
                }
                downloadFile(imageUrl, imageFile)
                if (!imageFile.exists() || imageFile.length() == 0L) {
                    onError("Gagal download gambar")
                    return@Thread
                }

                // 2. Pakai DirectVideoGenerator dengan gambar sebagai background
                //    (Untuk simplicity, generate video teks + audio)
                DirectVideoGenerator.generateFromText(
                    context, query, durationSec, outputPath,
                    onSuccess = { path ->
                        imageFile.delete()
                        onSuccess(path)
                    },
                    onError = onError
                )
            } catch (e: Exception) {
                e.printStackTrace()
                onError("Error: ${e.message}")
            }
        }.start()
    }

    private fun searchGoogleImage(query: String): String? {
        return try {
            if (GOOGLE_API_KEY.startsWith("YOUR_") || GOOGLE_CX.startsWith("YOUR_")) {
                // API key belum diisi — pakai placeholder image
                return "https://via.placeholder.com/1280x720/1E3A8A/FFFFFF?text=" +
                       URLEncoder.encode(query, "UTF-8")
            }
            val url = "https://www.googleapis.com/customsearch/v1" +
                      "?key=$GOOGLE_API_KEY&cx=$GOOGLE_CX" +
                      "&q=${URLEncoder.encode(query, "UTF-8")}" +
                      "&searchType=image&num=1"
            val conn = URL(url).openConnection()
            conn.connect()
            val json = conn.getInputStream().bufferedReader().readText()
            val regex = "\"link\"\\s*:\\s*\"([^\"]+)\"".toRegex()
            regex.find(json)?.groupValues?.get(1)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun downloadFile(url: String, dest: File) {
        URL(url).openStream().use { input ->
            dest.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}
