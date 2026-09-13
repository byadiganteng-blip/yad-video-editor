package com.yad.videoeditor

import android.content.Context
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File

/**
 * DirectVideoGenerator — generate video LANGSUNG dari teks
 * Tanpa model AI, tanpa gambar. Bikin video dari:
 *   - Background warna/gradient
 *   - Teks overlay
 *   - Audio TTS
 */
object DirectVideoGenerator {

    fun generateFromText(
        context: Context,
        text: String,
        durationSec: Int,
        outputPath: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val cacheDir = context.cacheDir
        val ttsFile = File(cacheDir, "direct_tts.mp3")
        val tempVideo = File(cacheDir, "direct_temp.mp4")

        // 1. Generate TTS dulu
        TtsHelper.synthesizeToFile(context, text, ttsFile.absolutePath) { ttsOk ->
            if (!ttsOk) {
                onError("Gagal generate TTS")
                return@synthesizeToFile
            }

            // 2. Buat video dari warna solid + teks pakai FFmpeg drawtext
            //    Background gradient biru-ungu, teks di tengah
            val escapedText = text.replace("'", "\\'").replace(":", "\\:")
            val cmd = arrayOf(
                "-y",
                "-f", "lavfi",
                "-i", "color=c=0x1E3A8A:s=1280x720:d=$durationSec",
                "-i", ttsFile.absolutePath,
                "-vf", "drawtext=text='$escapedText':fontcolor=white:fontsize=48:" +
                       "x=(w-text_w)/2:y=(h-text_h)/2:box=1:boxcolor=black@0.5:boxborderw=20",
                "-c:v", "libx264",
                "-preset", "ultrafast",
                "-tune", "stillimage",
                "-c:a", "aac",
                "-b:a", "192k",
                "-pix_fmt", "yuv420p",
                "-shortest",
                outputPath
            )

            FFmpegKit.executeAsync(cmd.joinToString(" ")) { session ->
                if (ReturnCode.isSuccess(session.returnCode)) {
                    // Bersihkan temp
                    ttsFile.delete()
                    tempVideo.delete()
                    onSuccess(outputPath)
                } else {
                    onError("FFmpeg gagal: ${session.failStackTrace}")
                }
            }
        }
    }
}
