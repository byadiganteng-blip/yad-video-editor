package com.yad.videoeditor

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer

/**
 * VideoAudioMuxer — gabungkan audio ke video TANPA FFmpeg
 * Pakai MediaMuxer + MediaExtractor bawaan Android (API 18+)
 *
 * Cara kerja:
 *   1. Buka video source → ekstrak track video
 *   2. Buka audio source → ekstrak track audio
 *   3. Mux ke output baru
 */
object VideoAudioMuxer {

    private const val TIMEOUT_US = 10000L

    /**
     * Gabungkan video + audio jadi 1 file output
     */
    fun muxAudioToVideo(
        context: Context,
        videoPath: String,
        audioPath: String,
        outputPath: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                val videoFile = File(videoPath)
                val audioFile = File(audioPath)
                if (!videoFile.exists()) {
                    onError("Video tidak ditemukan: $videoPath")
                    return@Thread
                }
                if (!audioFile.exists()) {
                    onError("Audio tidak ditemukan: $audioPath")
                    return@Thread
                }

                val muxer = MediaMuxer(outputPath,
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

                val videoExtractor = MediaExtractor()
                videoExtractor.setDataSource(videoPath)

                val audioExtractor = MediaExtractor()
                audioExtractor.setDataSource(audioPath)

                // === TRACK VIDEO ===
                var videoTrackIndex = -1
                var videoFormat: MediaFormat? = null
                for (i in 0 until videoExtractor.trackCount) {
                    val fmt = videoExtractor.getTrackFormat(i)
                    val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
                    if (mime.startsWith("video/")) {
                        videoExtractor.selectTrack(i)
                        videoFormat = fmt
                        videoTrackIndex = muxer.addTrack(fmt)
                        break
                    }
                }

                // === TRACK AUDIO ===
                var audioTrackIndex = -1
                var audioFormat: MediaFormat? = null
                for (i in 0 until audioExtractor.trackCount) {
                    val fmt = audioExtractor.getTrackFormat(i)
                    val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
                    if (mime.startsWith("audio/")) {
                        audioExtractor.selectTrack(i)
                        audioFormat = fmt
                        audioTrackIndex = muxer.addTrack(fmt)
                        break
                    }
                }

                if (videoTrackIndex < 0) {
                    muxer.release()
                    videoExtractor.release()
                    audioExtractor.release()
                    onError("Video tidak punya track video")
                    return@Thread
                }
                if (audioTrackIndex < 0) {
                    muxer.release()
                    videoExtractor.release()
                    audioExtractor.release()
                    onError("Audio tidak punya track audio")
                    return@Thread
                }

                muxer.start()

                // === COPY VIDEO SAMPLES ===
                val videoBufSize = videoFormat!!.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                val videoBuf = ByteBuffer.allocate(if (videoBufSize > 0) videoBufSize else 1024 * 1024)
                val videoInfo = MediaCodec.BufferInfo()
                while (true) {
                    videoInfo.offset = 0
                    videoInfo.size = videoExtractor.readSampleData(videoBuf, 0)
                    if (videoInfo.size < 0) break
                    videoInfo.presentationTimeUs = videoExtractor.sampleTime
                    videoInfo.flags = videoExtractor.sampleFlags
                    muxer.writeSampleData(videoTrackIndex, videoBuf, videoInfo)
                    videoExtractor.advance()
                }

                // === COPY AUDIO SAMPLES ===
                val audioBufSize = audioFormat!!.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                val audioBuf = ByteBuffer.allocate(if (audioBufSize > 0) audioBufSize else 1024 * 1024)
                val audioInfo = MediaCodec.BufferInfo()
                while (true) {
                    audioInfo.offset = 0
                    audioInfo.size = audioExtractor.readSampleData(audioBuf, 0)
                    if (audioInfo.size < 0) break
                    audioInfo.presentationTimeUs = audioExtractor.sampleTime
                    audioInfo.flags = audioExtractor.sampleFlags
                    muxer.writeSampleData(audioTrackIndex, audioBuf, audioInfo)
                    audioExtractor.advance()
                }

                muxer.stop()
                muxer.release()
                videoExtractor.release()
                audioExtractor.release()

                onSuccess(outputPath)
            } catch (e: Exception) {
                e.printStackTrace()
                onError("Mux error: ${e.message}")
            }
        }.start()
    }

    /**
     * Cek apakah video punya audio stream
     */
    fun hasAudioStream(videoPath: String): Boolean {
        return try {
            val ex = MediaExtractor()
            ex.setDataSource(videoPath)
            var has = false
            for (i in 0 until ex.trackCount) {
                val mime = ex.getTrackFormat(i).getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) { has = true; break }
            }
            ex.release()
            has
        } catch (e: Exception) {
            false
        }
    }
}
