package com.yad.videoeditor

import android.content.Context
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File
import java.net.URL

/**
 * GoogleImageGenerator — cari gambar dari Google lalu jadikan video
 *
 * Cara kerja:
 *   1. Search gambar via Google Custom Search API (atau scrape)
 *   2. Download gambar
 *   3. Buat video slideshow dari gambar + audio TTS
 */
object GoogleImageGenerator {

    private const val GOOGLE_API_KEY = "YOUR_GOOGLE_API_KEY"       // isi nanti
    private const val GOOGLE_CX = "YOUR_GOOGLE_CX"                 // isi nanti

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
                val ttsFile = File(cacheDir, "google_tts.mp3")

                // 1. Cari & download gambar dari Google
                val imageUrl = searchGoogleImage(query)
                if (imageUrl == null) {
                    onError("Gambar tidak ditemukan untuk: $query")
                    return@Thread
                }
                downloadFile(imageUrl, imageFile)

                // 2. Generate TTS
                val ttsLatch = java.util.concurrent.CountDownLatch(1)
                var ttsOk = false
                TtsHelper.synthesizeToFile(context, query, ttsFile.absolutePath) { ok ->
                    ttsOk = ok
                    ttsLatch.countDown()
                }
                ttsLatch.await()

                if (!ttsOk) {
                    onError("Gagal generate TTS")
                    return@Thread
                }

                // 3. Buat video dari gambar + audio
                val cmd = arrayOf(
                    "-y",
                    "-loop", "1",
                    "-i", imageFile.absolutePath,
                    "-i", ttsFile.absolutePath,
                    "-c:v", "libx264",
                    "-t", durationSec.toString(),
                    "-pix_fmt", "yuv420p",
                    "-vf", "scale=1280:720:force_original_aspect_ratio=decrease,pad=1280:720:(ow-iw)/2:(oh-ih)/2",
                    "-c:a", "aac",
                    "-b:a", "192k",
                    "-shortest",
                    outputPath
                )

                FFmpegKit.executeAsync(cmd.joinToString(" ")) { session ->
                    if (ReturnCode.isSuccess(session.returnCode)) {
                        imageFile.delete()
                        ttsFile.delete()
                        onSuccess(outputPath)
                    } else {
                        onError("FFmpeg gagal: ${session.failStackTrace}")
                    }
                }
            } catch (e: Exception) {
                onError("Error: ${e.message}")
            }
        }.start()
    }

    /**
     * Search gambar via Google Custom Search API
     * Daftar dulu di: https://developers.google.com/custom-search/v1/overview
     */
    private fun searchGoogleImage(query: String): String? {
        return try {
            val url = "https://www.googleapis.com/customsearch/v1" +
                      "?key=$GOOGLE_API_KEY&cx=$GOOGLE_CX" +
                      "&q=${java.net.URLEncoder.encode(query, "UTF-8")}" +
                      "&searchType=image&num=1"
            val conn = URL(url).openConnection()
            conn.connect()
            val json = conn.getInputStream().bufferedReader().readText()
            // Parse JSON ambil link pertama
            val regex = """"link"\s*:\s*"([^"]+)"""".toRegex()
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
