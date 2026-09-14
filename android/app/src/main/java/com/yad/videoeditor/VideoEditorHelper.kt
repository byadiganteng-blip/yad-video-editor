package com.yad.videoeditor

import android.content.Context
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSessionCompleteCallback
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File

/**
 * VideoEditorHelper — semua operasi video editing pakai ffmpeg-kit (LOCAL).
 * Tidak butuh internet, tidak butuh upload ke server.
 *
 * CATATAN FFmpeg build ini:
 *   - libx264 TIDAK tersedia (lisensi GPL)
 *   - Pakai mpeg4 sebagai encoder video
 *   - Pakai aac sebagai encoder audio
 */
object VideoEditorHelper {

    private const val TAG = "VideoEditorHelper"

    // Encoder yang tersedia di AAR
    private const val VIDEO_CODEC = "mpeg4"
    private const val AUDIO_CODEC = "aac"

    // Escape path untuk FFmpeg command
    private fun esc(path: String): String {
        return "\"" + path.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
    }

    // ============================================================
    //  TRIM — potong durasi video (re-encode untuk keandalan)
    // ============================================================
    fun trim(
        context: Context,
        inputPath: String,
        startSec: String,
        durationSec: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val output = newOutputPath(context, "trim")
        // Re-encode agar video stream pasti ada
        val cmd = "-y -i ${esc(inputPath)} -ss $startSec -t $durationSec " +
                  "-c:v $VIDEO_CODEC -q:v 3 -c:a $AUDIO_CODEC -b:a 128k ${esc(output)}"
        runFfmpeg(cmd, output, onSuccess, onError)
    }

    // ============================================================
    //  ROTATE
    // ============================================================
    fun rotate(
        context: Context,
        inputPath: String,
        degrees: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val output = newOutputPath(context, "rotate")
        val filter = when (degrees) {
            "90" -> "transpose=1"
            "180" -> "transpose=2,transpose=2"
            "270" -> "transpose=2"
            else -> "transpose=1"
        }
        val cmd = "-y -i ${esc(inputPath)} -vf \"$filter\" " +
                  "-c:v $VIDEO_CODEC -q:v 3 -c:a copy ${esc(output)}"
        runFfmpeg(cmd, output, onSuccess, onError)
    }

    // ============================================================
    //  SPEED
    // ============================================================
    fun speed(
        context: Context,
        inputPath: String,
        speed: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val output = newOutputPath(context, "speed")
        // atempo max 2.0, jadi pakai chain kalau > 2
        val atempo = speed
        val cmd = "-y -i ${esc(inputPath)} " +
                  "-filter_complex \"[0:v]setpts=$speed*PTS[v];[0:a]atempo=$atempo[a]\" " +
                  "-map \"[v]\" -map \"[a]\" " +
                  "-c:v $VIDEO_CODEC -q:v 3 -c:a $AUDIO_CODEC ${esc(output)}"
        runFfmpeg(cmd, output, onSuccess, onError)
    }

    // ============================================================
    //  VOLUME
    // ============================================================
    fun volume(
        context: Context,
        inputPath: String,
        volume: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val output = newOutputPath(context, "volume")
        val cmd = "-y -i ${esc(inputPath)} -af \"volume=$volume\" " +
                  "-c:v copy -c:a $AUDIO_CODEC ${esc(output)}"
        runFfmpeg(cmd, output, onSuccess, onError)
    }

    // ============================================================
    //  COMPRESS — pakai mpeg4 karena libx264 tidak tersedia
    // ============================================================
    fun compress(
        context: Context,
        inputPath: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val output = newOutputPath(context, "compress")
        // q:v 5 = kualitas menengah
        val cmd = "-y -i ${esc(inputPath)} -c:v $VIDEO_CODEC -q:v 5 " +
                  "-c:a $AUDIO_CODEC -b:a 96k ${esc(output)}"
        runFfmpeg(cmd, output, onSuccess, onError)
    }

    // ============================================================
    //  CROP
    // ============================================================
    fun crop(
        context: Context,
        inputPath: String,
        width: String,
        height: String,
        pos: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val output = newOutputPath(context, "crop")
        val cmd = "-y -i ${esc(inputPath)} -vf \"crop=$width:$height:$pos\" " +
                  "-c:v $VIDEO_CODEC -q:v 3 -c:a copy ${esc(output)}"
        runFfmpeg(cmd, output, onSuccess, onError)
    }

    // ============================================================
    //  REVERSE
    // ============================================================
    fun reverse(
        context: Context,
        inputPath: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val output = newOutputPath(context, "reverse")
        val cmd = "-y -i ${esc(inputPath)} -vf reverse -af areverse " +
                  "-c:v $VIDEO_CODEC -q:v 3 -c:a $AUDIO_CODEC ${esc(output)}"
        runFfmpeg(cmd, output, onSuccess, onError)
    }

    // ============================================================
    //  FILTER
    // ============================================================
    fun filter(
        context: Context,
        inputPath: String,
        filterName: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val output = newOutputPath(context, "filter")
        val filter = when (filterName.lowercase()) {
            "grayscale" -> "hue=s=0"
            "vintage" -> "curves=vintage"
            "warm" -> "colorbalance=rs=0.3:gs=0.1:bs=-0.2"
            "cool" -> "colorbalance=rs=-0.2:gs=0.1:bs=0.3"
            "blur" -> "boxblur=5:1"
            "sharp" -> "unsharp=5:5:1.0"
            "sepia" -> "colorchannelmixer=.393:.769:.189:0:.349:.686:.168:0:.272:.534:.131"
            "dramatic" -> "eq=contrast=1.5:brightness=-0.1:saturation=1.2"
            "cartoon" -> "edgedetect=low=0.1:high=0.3"
            else -> "null"
        }
        val cmd = "-y -i ${esc(inputPath)} -vf \"$filter\" " +
                  "-c:v $VIDEO_CODEC -q:v 3 -c:a copy ${esc(output)}"
        runFfmpeg(cmd, output, onSuccess, onError)
    }

    // ============================================================
    //  RESIZE
    // ============================================================
    fun resize(
        context: Context,
        inputPath: String,
        width: String,
        height: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val output = newOutputPath(context, "resize")
        val cmd = "-y -i ${esc(inputPath)} -vf \"scale=$width:$height\" " +
                  "-c:v $VIDEO_CODEC -q:v 3 -c:a copy ${esc(output)}"
        runFfmpeg(cmd, output, onSuccess, onError)
    }

    // ============================================================
    //  MUTE
    // ============================================================
    fun mute(
        context: Context,
        inputPath: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val output = newOutputPath(context, "mute")
        // Re-encode video (bukan copy) supaya stream pasti ada
        val cmd = "-y -i ${esc(inputPath)} -an -c:v $VIDEO_CODEC -q:v 3 ${esc(output)}"
        runFfmpeg(cmd, output, onSuccess, onError)
    }

    // ============================================================
    //  GIF
    // ============================================================
    fun toGif(
        context: Context,
        inputPath: String,
        fps: String,
        width: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val output = newOutputPath(context, "gif", ".gif")
        // Pastikan ada video stream: cek dulu dengan scale filter
        val cmd = "-y -i ${esc(inputPath)} -vf \"fps=$fps,scale=$width:-1:flags=lanczos\" " +
                  "-an ${esc(output)}"
        runFfmpeg(cmd, output, onSuccess, onError)
    }

    // ============================================================
    //  SCREENSHOT — fix format timestamp
    // ============================================================
    fun screenshot(
        context: Context,
        inputPath: String,
        timestamp: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val output = newOutputPath(context, "frame", ".png")
        // Timestamp format: pakai titik bukan koma
        val cleanTs = timestamp.replace(",", ".")
        val cmd = "-y -ss $cleanTs -i ${esc(inputPath)} -vframes 1 -q:v 2 ${esc(output)}"
        runFfmpeg(cmd, output, onSuccess, onError)
    }

    // ============================================================
    //  TEXT OVERLAY
    // ============================================================
    fun addText(
        context: Context,
        inputPath: String,
        text: String,
        position: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val output = newOutputPath(context, "text")
        val pos = when (position.lowercase()) {
            "top" -> "x=(w-text_w)/2:y=50"
            "bottom" -> "x=(w-text_w)/2:y=h-th-50"
            else -> "x=(w-text_w)/2:y=(h-text_h)/2"
        }
        val safeText = text.replace(":", "\\:").replace("'", "")
        val cmd = "-y -i ${esc(inputPath)} " +
                  "-vf \"drawtext=text='$safeText':fontcolor=white:fontsize=48:" +
                  "box=1:boxcolor=black@0.5:boxborderw=10:$pos\" " +
                  "-c:v $VIDEO_CODEC -q:v 3 -c:a copy ${esc(output)}"
        runFfmpeg(cmd, output, onSuccess, onError)
    }

    // ============================================================
    //  CORE — run ffmpeg command (defensive)
    // ============================================================
    private fun runFfmpeg(
        cmd: String,
        outputPath: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            if (cmd.isBlank()) {
                safeLogError("runFfmpeg: cmd kosong", null)
                safeCallback { onError("Command kosong") }
                return
            }

            safeLog("Running: $cmd")

            FFmpegKit.executeAsync(cmd, FFmpegSessionCompleteCallback { session ->
                try {
                    if (session == null) {
                        safeLogError("FFmpeg session null", null)
                        safeCallback { onError("FFmpeg gagal start") }
                        return@FFmpegSessionCompleteCallback
                    }

                    val returnCode = session.returnCode
                    if (ReturnCode.isSuccess(returnCode)) {
                        safeLog("FFmpeg SUCCESS: $outputPath")
                        safeCallback { onSuccess(outputPath) }
                    } else if (ReturnCode.isCancel(returnCode)) {
                        safeLog("FFmpeg CANCELLED")
                        safeCallback { onError("Dibatalkan") }
                    } else {
                        val log = try { session.allLogsAsString } catch (_: Exception) { "" }
                        // Ambil baris error terakhir untuk pesan yang jelas
                        val lastErr = log.split("\n")
                            .lastOrNull { it.contains("Error", ignoreCase = true) 
                                       || it.contains("Invalid", ignoreCase = true)
                                       || it.contains("Unrecognized", ignoreCase = true) }
                            ?: "FFmpeg gagal"
                        safeLogError("FFmpeg FAILED: $lastErr", null)
                        safeCallback { onError(lastErr) }
                    }
                } catch (e: Exception) {
                    safeLogError("FFmpeg callback exception", e)
                    safeCallback { onError(e.message ?: "Unknown error") }
                }
            })
        } catch (e: Exception) {
            safeLogError("runFfmpeg exception", e)
            safeCallback { onError(e.message ?: "Unknown error") }
        }
    }

    private fun safeLog(msg: String) {
        try { AutoLogSaver.log(TAG, msg) } catch (_: Exception) {}
    }

    private fun safeLogError(msg: String, e: Throwable?) {
        try { AutoLogSaver.logError(TAG, msg, e) } catch (_: Exception) {}
    }

    private inline fun safeCallback(block: () -> Unit) {
        try { block() } catch (e: Exception) {
            try { AutoLogSaver.logError(TAG, "callback exception", e) } catch (_: Exception) {}
        }
    }

    private fun newOutputPath(context: Context, prefix: String, ext: String = ".mp4"): String {
        val dir = File(context.getExternalFilesDir(null), "edited")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "${prefix}_${System.currentTimeMillis()}$ext").absolutePath
    }
}
