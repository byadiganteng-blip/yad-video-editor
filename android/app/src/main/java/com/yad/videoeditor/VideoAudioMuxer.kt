package com.yad.videoeditor

import android.content.Context
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File

/**
 * VideoAudioMuxer — gabungkan audio ke video pakai FFmpeg
 * Fix: video hasil generate tidak ada suara
 */
object VideoAudioMuxer {

    /**
     * Gabungkan video (tanpa audio) + audio file → output video dengan suara
     */
    fun muxAudioToVideo(
        context: Context,
        videoPath: String,
        audioPath: String,
        outputPath: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val videoFile = File(videoPath)
        val audioFile = File(audioPath)

        if (!videoFile.exists()) {
            onError("Video file tidak ditemukan: $videoPath")
            return
        }
        if (!audioFile.exists()) {
            onError("Audio file tidak ditemukan: $audioPath")
            return
        }

        // FFmpeg command: gabung video + audio, video di-copy, audio di-encode AAC
        val cmd = arrayOf(
            "-y",
            "-i", videoPath,
            "-i", audioPath,
            "-c:v", "copy",           // video tidak di-reencode (cepat)
            "-c:a", "aac",            // audio encode ke AAC
            "-b:a", "192k",           // bitrate audio
            "-shortest",              // potong sesuai durasi terpendek
            "-map", "0:v:0",          // ambil video dari input 0
            "-map", "1:a:0",          // ambil audio dari input 1
            outputPath
        )

        FFmpegKit.executeAsync(cmd.joinToString(" ")) { session ->
            val returnCode = session.returnCode
            if (ReturnCode.isSuccess(returnCode)) {
                onSuccess(outputPath)
            } else {
                onError("FFmpeg gagal: ${session.failStackTrace}")
            }
        }
    }

    /**
     * Tambahkan audio TTS (text-to-speech) ke video
     * Kalau video tidak punya audio sama sekali, generate dari teks
     */
    fun addTtsAudioToVideo(
        context: Context,
        videoPath: String,
        text: String,
        outputPath: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val ttsFile = File(context.cacheDir, "tts_temp.mp3")

        // Generate TTS dulu
        TtsHelper.synthesizeToFile(context, text, ttsFile.absolutePath) { success ->
            if (!success) {
                onError("Gagal generate TTS")
                return@synthesizeToFile
            }
            // Lalu mux ke video
            muxAudioToVideo(context, videoPath, ttsFile.absolutePath, outputPath,
                onSuccess, onError)
        }
    }

    /**
     * Cek apakah video punya audio stream
     */
    fun hasAudioStream(videoPath: String, callback: (Boolean) -> Unit) {
        val cmd = "-i $videoPath -hide_banner"
        FFmpegKit.executeAsync(cmd) { session ->
            val output = session.allLogsAsString ?: ""
            // FFmpeg print stream info ke stderr, cek ada "Audio:"
            callback(output.contains("Audio:", ignoreCase = true))
        }
    }
}
