package com.yad.videoeditor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch

/**
 * DirectVideoGenerator — generate video LANGSUNG dari teks.
 *
 * FIX v2: 
 *   - Coba beberapa COLOR_Format sampai ada yang support
 *   - Verifikasi codec support sebelum encode
 *   - Cek buffer size YUV sesuai
 */
object DirectVideoGenerator {

    private const val TAG = "DirectVideoGenerator"
    private const val WIDTH = 1280
    private const val HEIGHT = 720
    private const val FPS = 30
    private const val BIT_RATE = 4_000_000
    private const val TIMEOUT_US = 10_000L

    fun generateFromText(
        context: Context,
        text: String,
        durationSec: Int,
        outputPath: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                AutoLogSaver.log(TAG, "Generate start: text_len=${text.length}, dur=$durationSec")

                val cacheDir = context.cacheDir
                val videoOnly = File(cacheDir, "direct_video_only.mp4")
                val ttsFile = File(cacheDir, "direct_tts.mp3")

                // 1. Generate video frame
                encodeTextVideo(text, durationSec, videoOnly.absolutePath)
                
                if (!videoOnly.exists() || videoOnly.length() < 1000) {
                    AutoLogSaver.logError(TAG, "Video 0 byte atau tidak valid", null)
                    onError("Gagal encode video — codec tidak support format")
                    return@Thread
                }
                
                AutoLogSaver.log(TAG, "Video only OK: ${videoOnly.length()} bytes")

                // 2. Generate TTS
                val latch = CountDownLatch(1)
                var ttsOk = false
                try {
                    TtsHelper.synthesizeToFile(context, text, ttsFile.absolutePath) { ok ->
                        ttsOk = ok
                        latch.countDown()
                    }
                    latch.await()
                    AutoLogSaver.log(TAG, "TTS OK: $ttsOk, size=${ttsFile.length()}")
                } catch (e: Exception) {
                    AutoLogSaver.logError(TAG, "TTS error", e)
                    ttsOk = false
                }

                // 3. Mux audio ke video
                if (ttsOk && ttsFile.exists() && ttsFile.length() > 0) {
                    try {
                        VideoAudioMuxer.muxAudioToVideo(
                            context,
                            videoOnly.absolutePath,
                            ttsFile.absolutePath,
                            outputPath,
                            onSuccess = { path ->
                                try { videoOnly.delete() } catch (_: Exception) {}
                                try { ttsFile.delete() } catch (_: Exception) {}
                                AutoLogSaver.log(TAG, "Mux OK: $path")
                                onSuccess(path)
                            },
                            onError = { err ->
                                AutoLogSaver.warn(TAG, "Mux gagal: $err")
                                videoOnly.renameTo(File(outputPath))
                                onSuccess(outputPath)
                            }
                        )
                    } catch (e: Exception) {
                        AutoLogSaver.logError(TAG, "Mux exception", e)
                        videoOnly.renameTo(File(outputPath))
                        onSuccess(outputPath)
                    }
                } else {
                    videoOnly.renameTo(File(outputPath))
                    AutoLogSaver.log(TAG, "No audio, pakai video saja")
                    onSuccess(outputPath)
                }
            } catch (e: Exception) {
                AutoLogSaver.logError(TAG, "Generate failed", e)
                onError("Error: ${e.javaClass.simpleName}: ${e.message}")
            }
        }.start()
    }

    /**
     * Cari COLOR_Format yang support untuk encoder AVC.
     * Coba beberapa format, dari yang paling kompatibel.
     */
    private fun findSupportedColorFormat(): Int {
        try {
            // Ambil codec AVC encoder
            val codecList = android.media.MediaCodecList(android.media.MediaCodecList.REGULAR_CODECS)
            for (info in codecList.codecInfos) {
                if (!info.isEncoder) continue
                for (type in info.supportedTypes) {
                    if (type.equals("video/avc", ignoreCase = true)) {
                        val caps = info.getCapabilitiesForType("video/avc")
                        val colorFormats = caps.colorFormats
                        AutoLogSaver.log(TAG, "Codec AVC support colors: ${colorFormats.joinToString()}")
                        
                        // Coba format satu per satu
                        val preferred = intArrayOf(
                            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar,  // NV12/NV21 (paling umum)
                            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar,      // I420
                            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible,    // fleksibel
                        )
                        for (pref in preferred) {
                            if (colorFormats.contains(pref)) {
                                AutoLogSaver.log(TAG, "Pakai color format: $pref")
                                return pref
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            AutoLogSaver.logError(TAG, "findSupportedColorFormat error", e)
        }
        // Fallback ke SemiPlanar (paling umum)
        return MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
    }

    /**
     * Encode text ke video.
     */
    private fun encodeTextVideo(text: String, durationSec: Int, outputPath: String) {
        val colorFormat = findSupportedColorFormat()
        
        val format = MediaFormat.createVideoFormat(
            MediaFormat.MIMETYPE_VIDEO_AVC, WIDTH, HEIGHT
        )
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat)
        format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FPS)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)

        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()
        
        // Log actual format yang dipakai
        try {
            val actualFormat = codec.inputFormat
            AutoLogSaver.log(TAG, "Actual input format: $actualFormat")
        } catch (_: Exception) {}

        val muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var trackIndex = -1
        var muxerStarted = false

        val bufferInfo = MediaCodec.BufferInfo()
        val totalFrames = durationSec * FPS
        val frameDurationUs = 1_000_000L / FPS

        // Render Bitmap
        val bmp = renderTextBitmap(text)

        // Konversi Bitmap → YUV sesuai colorFormat
        val yuvBytes = if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) {
            bitmapToNV21(bmp)
        } else {
            bitmapToI420(bmp)
        }
        
        AutoLogSaver.log(TAG, "YUV bytes size: ${yuvBytes.size}, expected: ${WIDTH * HEIGHT * 3 / 2}")

        try {
            for (frameIdx in 0 until totalFrames) {
                val ptsUs = frameIdx * frameDurationUs

                // INPUT
                val inIdx = codec.dequeueInputBuffer(TIMEOUT_US)
                if (inIdx >= 0) {
                    val inputBuffer = codec.getInputBuffer(inIdx)
                    if (inputBuffer != null) {
                        inputBuffer.clear()
                        if (inputBuffer.remaining() >= yuvBytes.size) {
                            inputBuffer.put(yuvBytes)
                            codec.queueInputBuffer(inIdx, 0, yuvBytes.size, ptsUs, 0)
                        } else {
                            AutoLogSaver.warn(TAG, "Input buffer terlalu kecil: ${inputBuffer.remaining()} < ${yuvBytes.size}")
                            inputBuffer.put(yuvBytes, 0, inputBuffer.remaining())
                            codec.queueInputBuffer(inIdx, 0, inputBuffer.remaining(), ptsUs, 0)
                        }
                    }
                }

                // DRAIN
                var outIdx = codec.dequeueOutputBuffer(bufferInfo, 0)
                while (outIdx >= 0) {
                    if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (!muxerStarted) {
                            trackIndex = muxer.addTrack(codec.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                    } else {
                        val encodedData = codec.getOutputBuffer(outIdx)
                        if (encodedData != null && bufferInfo.size > 0 && muxerStarted) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                        }
                        codec.releaseOutputBuffer(outIdx, false)
                    }
                    outIdx = codec.dequeueOutputBuffer(bufferInfo, 0)
                }
            }

            // EOS
            val inIdx = codec.dequeueInputBuffer(TIMEOUT_US)
            if (inIdx >= 0) {
                codec.queueInputBuffer(inIdx, 0, 0,
                    totalFrames * frameDurationUs,
                    MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            }

            // Drain sisa
            var eos = false
            var drainCount = 0
            while (!eos && drainCount < 1000) {
                drainCount++
                val outIdx = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                when {
                    outIdx == MediaCodec.INFO_TRY_AGAIN_LATER -> continue
                    outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (!muxerStarted) {
                            trackIndex = muxer.addTrack(codec.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                    }
                    outIdx >= 0 -> {
                        val encodedData = codec.getOutputBuffer(outIdx)
                        if (encodedData != null && bufferInfo.size > 0 && muxerStarted) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                        }
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            eos = true
                        }
                        codec.releaseOutputBuffer(outIdx, false)
                    }
                }
            }
            
            AutoLogSaver.log(TAG, "Drain selesai: muxerStarted=$muxerStarted, trackIndex=$trackIndex")
        } finally {
            try { codec.stop() } catch (_: Exception) {}
            try { codec.release() } catch (_: Exception) {}
            try { if (muxerStarted) muxer.stop() } catch (_: Exception) {}
            try { muxer.release() } catch (_: Exception) {}
            bmp.recycle()
        }
    }

    private fun renderTextBitmap(text: String): Bitmap {
        val bmp = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.parseColor("#1E3A8A"))

        val paint = Paint().apply {
            color = Color.WHITE
            textSize = 48f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            setShadowLayer(4f, 2f, 2f, Color.BLACK)
        }

        val maxWidth = WIDTH - 100f
        val lines = wrapText(text, paint, maxWidth)
        val lineHeight = paint.fontSpacing
        val totalHeight = lines.size * lineHeight
        var y = (HEIGHT / 2f) - (totalHeight / 2f) + paint.textSize

        for (line in lines) {
            canvas.drawText(line, WIDTH / 2f, y, paint)
            y += lineHeight
        }

        return bmp
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            val test = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(test) <= maxWidth) {
                currentLine = StringBuilder(test)
            } else {
                if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
        return if (lines.size > 20) lines.take(20) else lines
    }

    /**
     * Konversi Bitmap → NV21 (SemiPlanar: Y + interleaved VU)
     */
    private fun bitmapToNV21(bmp: Bitmap): ByteArray {
        val width = bmp.width
        val height = bmp.height
        val argb = IntArray(width * height)
        bmp.getPixels(argb, 0, width, 0, 0, width, height)

        val yuv = ByteArray(width * height * 3 / 2)
        val frameSize = width * height

        var yIndex = 0
        var uvIndex = frameSize

        for (j in 0 until height) {
            for (i in 0 until width) {
                val argbPixel = argb[j * width + i]
                val r = (argbPixel shr 16) and 0xff
                val g = (argbPixel shr 8) and 0xff
                val b = argbPixel and 0xff

                val y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                yuv[yIndex++] = y.coerceIn(0, 255).toByte()

                if (j % 2 == 0 && i % 2 == 0) {
                    val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                    val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                    // NV21: V dulu, lalu U
                    yuv[uvIndex++] = v.coerceIn(0, 255).toByte()
                    yuv[uvIndex++] = u.coerceIn(0, 255).toByte()
                }
            }
        }
        return yuv
    }

    /**
     * Konversi Bitmap → I420 (Planar: Y + U + V)
     */
    private fun bitmapToI420(bmp: Bitmap): ByteArray {
        val width = bmp.width
        val height = bmp.height
        val argb = IntArray(width * height)
        bmp.getPixels(argb, 0, width, 0, 0, width, height)

        val yuv = ByteArray(width * height * 3 / 2)
        val frameSize = width * height

        var yIndex = 0
        var uvIndex = frameSize

        for (j in 0 until height) {
            for (i in 0 until width) {
                val argbPixel = argb[j * width + i]
                val r = (argbPixel shr 16) and 0xff
                val g = (argbPixel shr 8) and 0xff
                val b = argbPixel and 0xff

                val y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                yuv[yIndex++] = y.coerceIn(0, 255).toByte()

                if (j % 2 == 0 && i % 2 == 0) {
                    val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                    val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                    // I420: U dulu, lalu V
                    yuv[uvIndex++] = u.coerceIn(0, 255).toByte()
                    yuv[uvIndex++] = v.coerceIn(0, 255).toByte()
                }
            }
        }
        return yuv
    }
}
