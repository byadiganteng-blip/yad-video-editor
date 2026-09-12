package com.yad.videoeditor

import android.content.ContentValues
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileInputStream

class VideoPreviewActivity : AppCompatActivity() {

    private var videoPath: String? = null
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var videoView: VideoView
    private lateinit var tvInfo: TextView
    private lateinit var btnPlay: Button
    private lateinit var btnDownload: Button
    private lateinit var btnShare: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { setContentView(R.layout.activity_video_preview) }
        catch (e: Exception) { finish(); return }

        videoPath = intent.getStringExtra("video_path")
        videoView = findViewById(R.id.videoView)
        tvInfo = findViewById(R.id.tvPreviewInfo)
        btnPlay = findViewById(R.id.btnPreviewPlay)
        btnDownload = findViewById(R.id.btnPreviewDownload)
        btnShare = findViewById(R.id.btnPreviewShare)
        progressBar = findViewById(R.id.previewProgress)

        if (videoPath.isNullOrEmpty()) {
            tvInfo.text = "❌ Tidak ada video"
            btnPlay.isEnabled = false
            btnDownload.isEnabled = false
            btnShare.isEnabled = false
            return
        }

        val file = File(videoPath!!)
        if (!file.exists()) {
            tvInfo.text = "❌ File tidak ditemukan"
            return
        }

        val sizeMb = file.length() / (1024.0 * 1024.0)
        tvInfo.text = "📁 ${file.name}\n📊 ${String.format("%.2f", sizeMb)} MB"

        // Setup VideoView
        videoView.setVideoPath(videoPath)
        videoView.setOnPreparedListener { mp ->
            mediaPlayer = mp
            mp.isLooping = true
            progressBar.visibility = View.GONE
            videoView.start()
        }
        videoView.setOnErrorListener { _, _, _ ->
            Toast.makeText(this, "❌ Gagal memutar video", Toast.LENGTH_LONG).show()
            progressBar.visibility = View.GONE
            true
        }
        progressBar.visibility = View.VISIBLE
        videoView.setOnCompletionListener {
            btnPlay.text = "▶️ Putar"
        }

        btnPlay.setOnClickListener {
            if (videoView.isPlaying) {
                videoView.pause()
                btnPlay.text = "▶️ Putar"
            } else {
                videoView.start()
                btnPlay.text = "⏸️ Jeda"
            }
        }

        btnDownload.setOnClickListener { downloadVideo(file) }
        btnShare.setOnClickListener { shareVideo(file) }
    }

    /**
     * ✅ Download video ke gallery — support Android 6 ke bawah
     */
    private fun downloadVideo(file: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ — MediaStore
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/YAD Video Editor")
                }
                val uri = contentResolver.insert(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values
                ) ?: throw Exception("Gagal buat MediaStore entry")

                contentResolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(file).use { it.copyTo(out) }
                }
                Toast.makeText(this, "✅ Tersimpan di Movies/YAD Video Editor",
                    Toast.LENGTH_LONG).show()
            } else {
                // Android 9 ke bawah — direct ke folder Movies
                val destDir = File(
                    Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_MOVIES
                    ), "YAD Video Editor"
                )
                if (!destDir.exists()) destDir.mkdirs()

                val destFile = File(destDir, file.name)
                FileInputStream(file).use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                // Trigger media scanner biar muncul di Gallery
                val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                intent.data = Uri.fromFile(destFile)
                sendBroadcast(intent)

                Toast.makeText(this, "✅ Tersimpan di ${destFile.absolutePath}",
                    Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Download gagal: ${e.message}",
                Toast.LENGTH_LONG).show()
        }
    }

    private fun shareVideo(file: File) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this, "$packageName.fileprovider", file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Bagikan video"))
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Gagal share: ${e.message}",
                Toast.LENGTH_LONG).show()
        }
    }

    override fun onPause() {
        super.onPause()
        videoView.pause()
        btnPlay.text = "▶️ Putar"
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
