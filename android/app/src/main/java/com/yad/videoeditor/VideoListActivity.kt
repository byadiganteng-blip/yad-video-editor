package com.yad.videoeditor

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Video List - List video hasil edit dari GitHub
 * Created by KARYADI, Coding by KARYADI
 */
class VideoListActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var tvStatus: TextView
    private lateinit var progressBar: ProgressBar

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_video_list)
        } catch (e: Exception) { finish(); return }

        recycler = findViewById(R.id.recyclerVideos)
        tvStatus = findViewById(R.id.tvStatus)
        progressBar = findViewById(R.id.progressBar)

        recycler.layoutManager = LinearLayoutManager(this)

        findViewById<Button>(R.id.btnRefreshList)?.setOnClickListener { loadVideos() }
        loadVideos()
    }

    private fun loadVideos() {
        tvStatus.text = "Loading..."
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val videos = withContext(Dispatchers.IO) { fetchVideos() }
            progressBar.visibility = View.GONE

            if (videos.isEmpty()) {
                tvStatus.text = "Belum ada video. Rekam/edit dulu di menu Video Editor."
                recycler.adapter = null
            } else {
                tvStatus.text = "${videos.size} video ditemukan"
                recycler.adapter = VideoAdapter(videos) { v -> downloadVideo(v) }
            }
        }
    }

    private fun fetchVideos(): List<VideoItem> {
        val result = mutableListOf<VideoItem>()
        try {
            val token = SecureConfig.getGithubToken()
            if (token.isEmpty()) return result

            // Ambil dari GitHub Releases
            val url = "https://api.github.com/repos/byadiganteng-blip/youtube-auto-pipeline/releases"
            val req = Request.Builder()
                .url(url)
                .header("Authorization", "token $token")
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return result
                val arr = org.json.JSONArray(resp.body?.string() ?: "[]")
                for (i in 0 until arr.length()) {
                    val rel = arr.getJSONObject(i)
                    val tag = rel.optString("tag_name", "")
                    val assets = rel.optJSONArray("assets") ?: continue
                    for (j in 0 until assets.length()) {
                        val a = assets.getJSONObject(j)
                        val name = a.optString("name", "")
                        // Filter video/audio saja
                        if (name.endsWith(".mp4") || name.endsWith(".mp3") ||
                            name.endsWith(".mov") || name.endsWith(".mkv")) {
                            result.add(VideoItem(
                                name = name,
                                url = a.optString("browser_download_url", ""),
                                size = a.optLong("size", 0),
                                tag = tag,
                                date = a.optString("created_at", "")
                            ))
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return result
    }

    private fun downloadVideo(v: VideoItem) {
        try {
            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val req = DownloadManager.Request(Uri.parse(v.url))
                .setTitle(v.name)
                .setDescription("Yad Video Editor")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_MOVIES, "YadEditor/${v.name}")
                .setAllowedOverMetered(true)

            dm.enqueue(req)
            Toast.makeText(this, "Download mulai: ${v.name}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun openVideo(v: VideoItem) {
        try {
            val file = File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES), "YadEditor/${v.name}")
            if (!file.exists()) {
                Toast.makeText(this, "File belum di-download", Toast.LENGTH_SHORT).show()
                return
            }
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/mp4")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal buka: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    data class VideoItem(
        val name: String, val url: String, val size: Long,
        val tag: String, val date: String
    )

    inner class VideoAdapter(
        val items: List<VideoItem>,
        val onDownload: (VideoItem) -> Unit
    ) : RecyclerView.Adapter<VideoAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvName: TextView = v.findViewById(R.id.tvVideoName)
            val tvInfo: TextView = v.findViewById(R.id.tvVideoInfo)
            val btnDl: Button = v.findViewById(R.id.btnVideoDownload)
            val btnOpen: Button = v.findViewById(R.id.btnVideoOpen)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_video, parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val v = items[position]
            holder.tvName.text = v.name
            val sizeMb = v.size / 1024 / 1024
            holder.tvInfo.text = "${v.tag} • ${sizeMb}MB"
            holder.btnDl.setOnClickListener { onDownload(v) }
            holder.btnOpen.setOnClickListener { openVideo(v) }
        }

        override fun getItemCount() = items.size
    }
}
