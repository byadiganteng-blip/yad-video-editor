package com.yad.videoeditor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * ImageSearcher — cari gambar TANPA API KEY.
 *
 * Strategi:
 *   1. Pollinations.ai (AI generate + keyword search)
 *   2. Fallback Picsum Photos (foto random)
 *   3. Auto-crop watermark kalau terdeteksi
 */
object ImageSearcher {

    private const val TAG = "ImageSearcher"

    private val client = OkHttpClient.Builder()
        .connectTimeout(90, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    // ============================================================
    //  MAIN METHOD — dipanggil dari DirectVideoGenerator
    // ============================================================
    suspend fun searchAndDownload(
        keyword: String,
        styleSuffix: String,
        cacheDir: File,
        sceneIndex: Int = 0
    ): File? = withContext(Dispatchers.IO) {

        AutoLogSaver.log(TAG, "Scene $sceneIndex: keyword='$keyword', style='$styleSuffix'")

        // 1. Coba Pollinations.ai
        val pollinations = tryPollinations(keyword, styleSuffix, cacheDir, sceneIndex)
        if (pollinations != null) {
            // Cek + auto-crop watermark
            return@withContext processImage(pollinations, sceneIndex)
        }

        // 2. Fallback Picsum
        AutoLogSaver.log(TAG, "Pollinations gagal, fallback ke Picsum")
        val picsum = tryPicsum(cacheDir, sceneIndex)
        if (picsum != null) {
            return@withContext picsum  // Picsum tidak watermark
        }

        AutoLogSaver.log(TAG, "Semua sumber gagal")
        null
    }

    // ============================================================
    //  1. POLLINATIONS.AI
    // ============================================================
    private fun tryPollinations(
        keyword: String,
        styleSuffix: String,
        cacheDir: File,
        idx: Int
    ): File? {
        // Build prompt
        val promptParts = mutableListOf<String>()
        if (keyword.isNotBlank()) promptParts.add(keyword)
        if (styleSuffix.isNotBlank()) promptParts.add(styleSuffix)
        promptParts.add("high quality, detailed")

        val prompt = promptParts.joinToString(", ")
        AutoLogSaver.log(TAG, "Pollinations prompt: $prompt")

        // Coba beberapa konfigurasi untuk menghindari watermark
        val paramVariants = listOf(
            "nologo=true&model=flux",
            "nologo=true&model=turbo",
            "nologo=true",
            "logo=false&nologo=true",
        )

        for ((i, params) in paramVariants.withIndex()) {
            try {
                val encoded = URLEncoder.encode(prompt, "UTF-8")
                val seed = (System.currentTimeMillis() % 100000) + idx * 100 + i
                val url = "https://image.pollinations.ai/prompt/$encoded?width=1280&height=720&$params&seed=$seed"

                AutoLogSaver.log(TAG, "Attempt ${i+1}: $url")

                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "YAD-Video-Editor/1.0")
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    AutoLogSaver.logError(TAG, "HTTP ${response.code}", null)
                    response.close()
                    continue
                }

                val file = File(cacheDir, "pollinations_${idx}_${System.currentTimeMillis()}.jpg")
                response.body?.byteStream()?.use { input ->
                    file.outputStream().use { input.copyTo(it) }
                }
                response.close()

                if (file.length() < 1000) {
                    file.delete()
                    continue
                }

                // Verifikasi gambar valid
                val bmp = BitmapFactory.decodeFile(file.absolutePath)
                if (bmp == null) {
                    file.delete()
                    continue
                }
                bmp.recycle()

                AutoLogSaver.log(TAG, "Pollinations OK: ${file.length()} bytes")
                return file
            } catch (e: Exception) {
                AutoLogSaver.logError(TAG, "Attempt ${i+1} failed", e)
            }
        }

        return null
    }

    // ============================================================
    //  2. PICSUM (fallback)
    // ============================================================
    private fun tryPicsum(cacheDir: File, idx: Int): File? {
        return try {
            val seed = (System.currentTimeMillis() % 10000) + idx
            val url = "https://picsum.photos/seed/$seed/1280/720"

            AutoLogSaver.log(TAG, "Picsum URL: $url")

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "YAD-Video-Editor/1.0")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                response.close()
                return null
            }

            val file = File(cacheDir, "picsum_${idx}_${System.currentTimeMillis()}.jpg")
            response.body?.byteStream()?.use { input ->
                file.outputStream().use { input.copyTo(it) }
            }
            response.close()

            if (file.length() < 1000) {
                file.delete()
                return null
            }

            AutoLogSaver.log(TAG, "Picsum OK: ${file.length()} bytes")
            file
        } catch (e: Exception) {
            AutoLogSaver.logError(TAG, "Picsum failed", e)
            null
        }
    }

    // ============================================================
    //  3. AUTO-CROP WATERMARK
    // ============================================================
    /**
     * Proses gambar:
     *   1. Cek apakah ada watermark di pojok kanan bawah
     *   2. Kalau ada, crop bagian bawah (buang watermark)
     *   3. Resize ke 1280x720
     */
    private fun processImage(inputFile: File, sceneIndex: Int): File? {
        return try {
            val original = BitmapFactory.decodeFile(inputFile.absolutePath) ?: return null

            AutoLogSaver.log(TAG, "Original image: ${original.width}x${original.height}")

            // Deteksi watermark (heuristik):
            // Watermark Pollinations biasanya di pojok kanan bawah,
            // ukuran ~5% dari tinggi gambar, dengan warna terang.
            val hasWatermark = detectWatermark(original)
            AutoLogSaver.log(TAG, "Watermark detected: $hasWatermark")

            val processed = if (hasWatermark) {
                cropWatermark(original)
            } else {
                original
            }

            // Resize ke 1280x720
            val finalBmp = Bitmap.createScaledBitmap(processed, 1280, 720, true)

            if (processed !== original) processed.recycle()
            if (finalBmp !== original && finalBmp !== processed) original.recycle()

            // Simpan
            val outputFile = File(inputFile.parentFile, "processed_${sceneIndex}_${System.currentTimeMillis()}.jpg")
            outputFile.outputStream().use { out ->
                finalBmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            finalBmp.recycle()

            // Hapus file asli
            inputFile.delete()

            AutoLogSaver.log(TAG, "Processed saved: ${outputFile.length()} bytes")
            outputFile
        } catch (e: Exception) {
            AutoLogSaver.logError(TAG, "processImage failed", e)
            inputFile
        }
    }

    /**
     * Deteksi watermark dengan heuristik sederhana:
     * cek apakah ada konten kompleks di area pojok kanan bawah.
     */
    private fun detectWatermark(bmp: Bitmap): Boolean {
        try {
            // Area watermark biasanya: 100x50 pixel di pojok kanan bawah
            val wmWidth = 150
            val wmHeight = 60
            val startX = bmp.width - wmWidth
            val startY = bmp.height - wmHeight

            if (startX < 0 || startY < 0) return false

            // Sample beberapa pixel di area watermark
            var brightCount = 0
            var totalSamples = 0

            for (x in startX until bmp.width step 10) {
                for (y in startY until bmp.height step 5) {
                    val pixel = bmp.getPixel(x, y)
                    val r = (pixel shr 16) and 0xFF
                    val g = (pixel shr 8) and 0xFF
                    val b = pixel and 0xFF
                    val brightness = (r + g + b) / 3

                    // Watermark biasanya terang (> 200) atau punya kontras tinggi
                    if (brightness > 200) brightCount++
                    totalSamples++
                }
            }

            if (totalSamples == 0) return false

            val brightRatio = brightCount.toFloat() / totalSamples
            AutoLogSaver.log(TAG, "Bright ratio in corner: $brightRatio")

            // Kalau > 15% pixel terang → kemungkinan watermark
            return brightRatio > 0.15
        } catch (e: Exception) {
            AutoLogSaver.logError(TAG, "detectWatermark failed", e)
            return false
        }
    }

    /**
     * Crop bagian bawah gambar untuk membuang watermark.
     * Lalu resize agar tetap 16:9.
     */
    private fun cropWatermark(bmp: Bitmap): Bitmap {
        try {
            // Buang 8% bagian bawah
            val cropHeight = (bmp.height * 0.92).toInt()
            val cropped = Bitmap.createBitmap(bmp, 0, 0, bmp.width, cropHeight)

            AutoLogSaver.log(TAG, "Cropped: ${bmp.width}x${bmp.height} → ${cropped.width}x${cropped.height}")
            return cropped
        } catch (e: Exception) {
            AutoLogSaver.logError(TAG, "cropWatermark failed", e)
            return bmp
        }
    }
}
