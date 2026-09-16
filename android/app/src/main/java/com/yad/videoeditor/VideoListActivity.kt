package com.yad.videoeditor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Daftar video hasil generate AI.
 * Scan folder yang BENAR: /Android/data/com.yad.videoeditor/files/ai_videos/
 * BUKAN "parts-v3" atau folder asing lainnya.
 */
class VideoListActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout
    private lateinit var tvCount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { setContentView(R.layout.activity_video_list) }
        catch (e: Exception) { finish(); return }

        // Tampil interstitial saat buka fitur
        try { StartAppHelper.showInterstitial(this) {} } catch (_: Exception) {}

        container = findViewById(R.id.videoListContainer)
        tvCount = findViewById(R.id.tvVideoCount)

        findViewById<Button>(R.id.btnRefreshList)?.setOnClickListener {
            loadVideos()
        }
    }

    override fun onResume() {
        super.onResume()
        loadVideos()
    }

    /**
     * Folder yang discan — hanya folder internal app kita!
     */
    private fun getVideoDirs(): List<File> {
        val dirs = mutableListOf<File>()

        // 1. Folder utama: /Android/data/com.yad.videoeditor/files/ai_videos
        val extFiles = getExternalFilesDir(null)
        if (extFiles != null) {
            dirs.add(File(extFiles, "ai_videos"))
        }

        // 2. Folder internal: /data/data/com.yad.videoeditor/files/ai_videos
        dirs.add(File(filesDir, "ai_videos"))

        // 3. Download folder (kalau user pernah download manual dari app ini)
        val downloads = File(
            getExternalFilesDir(null)?.parentFile?.parentFile,
            "Download/YAD Video Editor"
        )
        if (downloads.exists()) dirs.add(downloads)

        return dirs
    }

    private fun loadVideos() {
        container.removeAllViews()

        val allVideos = mutableListOf<File>()
        for (dir in getVideoDirs()) {
            if (dir.exists() && dir.isDirectory) {
                val files = dir.listFiles { f ->
                    f.isFile && f.name.endsWith(".mp4", ignoreCase = true)
                } ?: emptyArray()
                allVideos.addAll(files)
            }
        }

        // Sort by last modified (terbaru dulu)
        allVideos.sortByDescending { it.lastModified() }

        tvCount?.text = "${allVideos.size} video ditemukan"

        if (allVideos.isEmpty()) {
            val empty = TextView(this).apply {
                text = "Belum ada video.\n\n" +
                       "Generate video lewat menu:\n" +
                       "AI Text to Video"
                textSize = 14f
                setPadding(32, 64, 32, 64)
                gravity = android.view.Gravity.CENTER
            }
            container.addView(empty)
            return
        }

        val sdf = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
        for (file in allVideos) {
            container.addView(createVideoRow(file, sdf))
        }
    }

    private fun createVideoRow(file: File, sdf: SimpleDateFormat): View {
        val ctx = this
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(0xFFFFFFFF.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(16, 8, 16, 8) }
        }

        // Nama file
        val name = TextView(ctx).apply {
            text = file.name
            textSize = 15f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(0xFF1A1A1A.toInt())
        }

        // Info
        val sizeMb = file.length() / (1024.0 * 1024.0)
        val info = TextView(ctx).apply {
            text = "${String.format("%.1f", sizeMb)} MB • " +
                   sdf.format(Date(file.lastModified()))
            textSize = 12f
            setTextColor(0xFF666666.toInt())
            setPadding(0, 4, 0, 12)
        }

        // Tombol row
        val btnRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        // Tombol Download
        val btnDownload = Button(ctx).apply {
            text = "⬇ DOWNLOAD"
            setBackgroundColor(0xFF3498DB.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            ).apply { setMargins(0, 0, 8, 0) }
            setOnClickListener { downloadVideo(file) }
        }

        // Tombol Buka
        val btnOpen = Button(ctx).apply {
            text = "▶ BUKA"
            setBackgroundColor(0xFF27AE60.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
            setOnClickListener {
                val intent = Intent(ctx, VideoPreviewActivity::class.java)
                intent.putExtra("video_path", file.absolutePath)
                startActivity(intent)
            }
        }

        btnRow.addView(btnDownload)
        btnRow.addView(btnOpen)

        card.addView(name)
        card.addView(info)
        card.addView(btnRow)

        return card
    }

    private fun downloadVideo(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                this, "$packageName.fileprovider", file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/mp4")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
            Toast.makeText(this, "Buka dengan pemutar video",
                Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal: ${e.message}",
                Toast.LENGTH_SHORT).show()
        }
    }
}
