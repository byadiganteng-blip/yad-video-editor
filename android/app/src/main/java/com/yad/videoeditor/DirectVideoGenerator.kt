package com.yad.videoeditor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.concurrent.CountDownLatch

object DirectVideoGenerator {

    private const val TAG = "DirectVideoGenerator"
    private const val WIDTH = 1280
    private const val HEIGHT = 720
    private const val FPS = 30
    private const val BIT_RATE = 4_000_000
    private const val TIMEOUT_US = 10_000L

    /**
     * Generate video.
     *
     * @param styleSuffix  prompt suffix gaya visual (dari GenerationMode.styleSuffix)
     * @param showSubtitle  true = tampilkan teks
     */
    fun generateFromText(
        context: Context,
        text: String,
        durationSec: Int,
        outputPath: String,
        styleSuffix: String = "",
        showSubtitle: Boolean = false,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                AutoLogSaver.log(TAG, "Generate: len=${text.length}, style=$styleSuffix, subtitle=$showSubtitle")

                val cacheDir = context.cacheDir
                val videoOnly = File(cacheDir, "direct_video_only.mp4")
                val ttsFile = File(cacheDir, "direct_tts.mp3")

                // Cari gambar dengan style + deteksi cerita
                val imageFile = runBlocking {
                    ImageSearcher.searchAndDownload(text, styleSuffix, cacheDir, 0)
                }

                if (imageFile != null) {
                    AutoLogSaver.log(TAG, "Image OK: ${imageFile.absolutePath}")
                }

                // Encode video
                encodeVideo(text, durationSec, videoOnly.absolutePath, showSubtitle, imageFile)

                if (!videoOnly.exists() || videoOnly.length() < 1000) {
                    onError("Gagal encode video")
                    return@Thread
                }

                // TTS
                val latch = CountDownLatch(1)
                var ttsOk = false
                try {
                    TtsHelper.synthesizeToFile(context, text, ttsFile.absolutePath) { ok ->
                        ttsOk = ok
                        latch.countDown()
                    }
                    latch.await()
                } catch (e: Exception) {
                    AutoLogSaver.logError(TAG, "TTS error", e)
                }

                // Mux
                if (ttsOk && ttsFile.exists() && ttsFile.length() > 0) {
                    try {
                        VideoAudioMuxer.muxAudioToVideo(
                            context, videoOnly.absolutePath, ttsFile.absolutePath, outputPath,
                            onSuccess = { path ->
                                try { videoOnly.delete() } catch (_: Exception) {}
                                try { ttsFile.delete() } catch (_: Exception) {}
                                try { imageFile?.delete() } catch (_: Exception) {}
                                onSuccess(path)
                            },
                            onError = {
                                videoOnly.renameTo(File(outputPath))
                                onSuccess(outputPath)
                            }
                        )
                    } catch (e: Exception) {
                        videoOnly.renameTo(File(outputPath))
                        onSuccess(outputPath)
                    }
                } else {
                    videoOnly.renameTo(File(outputPath))
                    onSuccess(outputPath)
                }
            } catch (e: Exception) {
                AutoLogSaver.logError(TAG, "Generate failed", e)
                onError("Error: ${e.message}")
            }
        }.start()
    }

    private fun encodeVideo(
        text: String, durationSec: Int, outputPath: String,
        showText: Boolean, imageFile: File?
    ) {
        val colorFormat = findSupportedColorFormat()
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, WIDTH, HEIGHT)
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat)
        format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FPS)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)

        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()

        val muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var trackIndex = -1
        var muxerStarted = false
        val bufferInfo = MediaCodec.BufferInfo()
        val totalFrames = durationSec * FPS
        val frameDurationUs = 1_000_000L / FPS

        val bmp = renderBitmap(text, showText, imageFile)
        val yuvBytes = if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) {
            bitmapToNV21(bmp)
        } else {
            bitmapToI420(bmp)
        }

        try {
            for (frameIdx in 0 until totalFrames) {
                val ptsUs = frameIdx * frameDurationUs
                val inIdx = codec.dequeueInputBuffer(TIMEOUT_US)
                if (inIdx >= 0) {
                    val inputBuffer = codec.getInputBuffer(inIdx)
                    if (inputBuffer != null) {
                        inputBuffer.clear()
                        if (inputBuffer.remaining() >= yuvBytes.size) {
                            inputBuffer.put(yuvBytes)
                            codec.queueInputBuffer(inIdx, 0, yuvBytes.size, ptsUs, 0)
                        } else {
                            inputBuffer.put(yuvBytes, 0, inputBuffer.remaining())
                            codec.queueInputBuffer(inIdx, 0, inputBuffer.remaining(), ptsUs, 0)
                        }
                    }
                }
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

            val inIdx = codec.dequeueInputBuffer(TIMEOUT_US)
            if (inIdx >= 0) {
                codec.queueInputBuffer(inIdx, 0, 0,
                    totalFrames * frameDurationUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            }

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
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) eos = true
                        codec.releaseOutputBuffer(outIdx, false)
                    }
                }
            }
        } finally {
            try { codec.stop() } catch (_: Exception) {}
            try { codec.release() } catch (_: Exception) {}
            try { if (muxerStarted) muxer.stop() } catch (_: Exception) {}
            try { muxer.release() } catch (_: Exception) {}
            bmp.recycle()
        }
    }

    private fun renderBitmap(text: String, showText: Boolean, imageFile: File?): Bitmap {
        val bmp = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        if (imageFile != null && imageFile.exists()) {
            try {
                val img = BitmapFactory.decodeFile(imageFile.absolutePath)
                if (img != null) {
                    canvas.drawBitmap(img, Rect(0, 0, img.width, img.height),
                        Rect(0, 0, WIDTH, HEIGHT), null)
                    img.recycle()
                }
            } catch (e: Exception) {
                canvas.drawColor(Color.parseColor("#1E3A8A"))
            }
        } else {
            canvas.drawColor(Color.parseColor("#1E3A8A"))
        }

        if (showText) {
            val paint = Paint().apply {
                color = Color.WHITE
                textSize = 48f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
                setShadowLayer(4f, 2f, 2f, Color.BLACK)
            }
            val lines = wrapText(text, paint, WIDTH - 100f)
            val lh = paint.fontSpacing
            var y = (HEIGHT / 2f) - (lines.size * lh / 2f) + paint.textSize
            for (line in lines) {
                canvas.drawText(line, WIDTH / 2f, y, paint)
                y += lh
            }
        }
        return bmp
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var cur = StringBuilder()
        for (w in words) {
            val test = if (cur.isEmpty()) w else "$cur $w"
            if (paint.measureText(test) <= maxWidth) cur = StringBuilder(test)
            else { if (cur.isNotEmpty()) lines.add(cur.toString()); cur = StringBuilder(w) }
        }
        if (cur.isNotEmpty()) lines.add(cur.toString())
        return if (lines.size > 20) lines.take(20) else lines
    }

    private fun findSupportedColorFormat(): Int {
        try {
            val list = android.media.MediaCodecList(android.media.MediaCodecList.REGULAR_CODECS)
            for (info in list.codecInfos) {
                if (!info.isEncoder) continue
                for (t in info.supportedTypes) {
                    if (t.equals("video/avc", true)) {
                        val caps = info.getCapabilitiesForType("video/avc")
                        for (p in intArrayOf(
                            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
                            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar)) {
                            if (caps.colorFormats.contains(p)) return p
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
    }

    private fun bitmapToNV21(bmp: Bitmap): ByteArray {
        val w = bmp.width; val h = bmp.height
        val argb = IntArray(w * h); bmp.getPixels(argb, 0, w, 0, 0, w, h)
        val yuv = ByteArray(w * h * 3 / 2); val fs = w * h
        var yi = 0; var uvi = fs
        for (j in 0 until h) for (i in 0 until w) {
            val p = argb[j * w + i]
            val r = (p shr 16) and 0xff; val g = (p shr 8) and 0xff; val b = p and 0xff
            yuv[yi++] = ((((66*r + 129*g + 25*b + 128) shr 8) + 16).coerceIn(0, 255)).toByte()
            if (j % 2 == 0 && i % 2 == 0) {
                yuv[uvi++] = ((((112*r - 94*g - 18*b + 128) shr 8) + 128).coerceIn(0, 255)).toByte()
                yuv[uvi++] = ((((-38*r - 74*g + 112*b + 128) shr 8) + 128).coerceIn(0, 255)).toByte()
            }
        }
        return yuv
    }

    private fun bitmapToI420(bmp: Bitmap): ByteArray {
        val w = bmp.width; val h = bmp.height
        val argb = IntArray(w * h); bmp.getPixels(argb, 0, w, 0, 0, w, h)
        val yuv = ByteArray(w * h * 3 / 2); val fs = w * h
        var yi = 0; var uvi = fs
        for (j in 0 until h) for (i in 0 until w) {
            val p = argb[j * w + i]
            val r = (p shr 16) and 0xff; val g = (p shr 8) and 0xff; val b = p and 0xff
            yuv[yi++] = ((((66*r + 129*g + 25*b + 128) shr 8) + 16).coerceIn(0, 255)).toByte()
            if (j % 2 == 0 && i % 2 == 0) {
                yuv[uvi++] = ((((-38*r - 74*g + 112*b + 128) shr 8) + 128).coerceIn(0, 255)).toByte()
                yuv[uvi++] = ((((112*r - 94*g - 18*b + 128) shr 8) + 128).coerceIn(0, 255)).toByte()
            }
        }
        return yuv
    }
}
