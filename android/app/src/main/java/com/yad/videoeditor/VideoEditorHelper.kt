package com.yad.videoeditor

import android.content.Context
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.FFmpegSessionCompleteCallback
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File

/**
 * VideoEditorHelper — semua operasi video editing pakai ffmpeg-kit (LOCAL).
 * Tidak butuh internet, tidak butuh upload ke server.
 */
object VideoEditorHelper {

    private const val TAG = "VideoEditorHelper"

    // ============================================================
    //  TRIM — potong durasi video
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
        val cmd = "-y -i \"$inputPath\" -ss $startSec -t $durationSec -c copy \"$output\""
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
        val cmd = "-y -i \"$inputPath\" -vf \"$filter\" -c:a copy \"$output\""
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
        val cmd = "-y -i \"$inputPath\" -filter_complex \"[0:v]setpts=$speed*PTS[v];[0:a]atempo=$speed[a]\" -map \"[v]\" -map \"[a]\" \"$output\""
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
        val cmd = "-y -i \"$inputPath\" -af \"volume=$volume\" -c:v copy \"$output\""
        runFfmpeg(cmd, output, onSuccess, onError)
    }

    // ============================================================
    //  COMPRESS
    // ============================================================
    fun compress(
        context: Context,
        inputPath: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val output = newOutputPath(context, "compress")
        val cmd = "-y -i \"$inputPath\" -vcodec libx264 -crf 28 -preset fast -c:a aac -b:a 128k \"$output\""
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
        val cmd = "-y -i \"$inputPath\" -vf \"crop=$width:$height:$pos\" \"$output\""
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
        val cmd = "-y -i \"$inputPath\" -vf reverse -af areverse \"$output\""
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
        val cmd = "-y -i \"$inputPath\" -vf \"$filter\" \"$output\""
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
        val cmd = "-y -i \"$inputPath\" -vf \"scale=$width:$height\" \"$output\""
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
        val cmd = "-y -i \"$inputPath\" -an -c:v copy \"$output\""
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
        val cmd = "-y -i \"$inputPath\" -vf \"fps=$fps,scale=$width:-1:flags=lanczos\" \"$output\""
        runFfmpeg(cmd, output, onSuccess, onError)
    }

    // ============================================================
    //  SCREENSHOT
    // ============================================================
    fun screenshot(
        context: Context,
        inputPath: String,
        timestamp: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val output = newOutputPath(context, "frame", ".png")
        val cmd = "-y -i \"$inputPath\" -ss $timestamp -vframes 1 \"$output\""
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
        val cmd = "-y -i \"$inputPath\" -vf \"drawtext=text='$safeText':fontcolor=white:fontsize=48:box=1:boxcolor=black@0.5:boxborderw=10:$pos\" \"$output\""
        runFfmpeg(cmd, output, onSuccess, onError)
    }

    // ============================================================
    //  CORE — run ffmpeg command
    // ============================================================
    private fun runFfmpeg(
        cmd: String,
        outputPath: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            AutoLogSaver.log(TAG, "Running: $cmd")
            FFmpegKit.executeAsync(cmd, FFmpegSessionCompleteCallback { session ->
                val returnCode = session.returnCode
                if (ReturnCode.isSuccess(returnCode)) {
                    AutoLogSaver.log(TAG, "FFmpeg SUCCESS: $outputPath")
                    onSuccess(outputPath)
                } else if (ReturnCode.isCancel(returnCode)) {
                    AutoLogSaver.log(TAG, "FFmpeg CANCELLED")
                    onError("Dibatalkan")
                } else {
                    val log = session.allLogsAsString
                    AutoLogSaver.logError(TAG, "FFmpeg FAILED: $log", null)
                    onError("FFmpeg gagal: ${returnCode?.value ?: "unknown"}")
                }
            })
        } catch (e: Exception) {
            AutoLogSaver.logError(TAG, "runFfmpeg exception", e)
            onError(e.message ?: "Unknown error")
        }
    }

    private fun newOutputPath(context: Context, prefix: String, ext: String = ".mp4"): String {
        val dir = File(context.getExternalFilesDir(null), "edited")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "${prefix}_${System.currentTimeMillis()}$ext").absolutePath
    }
}
