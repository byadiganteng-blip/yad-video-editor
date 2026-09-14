package com.yad.videoeditor

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.io.File
import kotlin.concurrent.thread

/**
 * VideoEditorActivity — KineMaster-style editor.
 * Preview pakai ExoPlayer, timeline pakai RangeTimelineView.
 */
class VideoEditorActivity : AppCompatActivity() {

    companion object {
        private const val REQ_PICK_VIDEO = 2001
        private const val THUMB_COUNT = 10
    }

    private lateinit var playerView: PlayerView
    private lateinit var btnBigPlay: ImageView
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnSkipBack: ImageButton
    private lateinit var btnSkipForward: ImageButton
    private lateinit var emptyState: LinearLayout
    private lateinit var timelineView: RangeTimelineView
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvDuration: TextView
    private lateinit var tvStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressContainer: LinearLayout
    private lateinit var btnExport: Button

    // Tools
    private lateinit var toolTrim: LinearLayout
    private lateinit var toolSplit: LinearLayout
    private lateinit var toolRotate: LinearLayout
    private lateinit var toolSpeed: LinearLayout
    private lateinit var toolVolume: LinearLayout
    private lateinit var toolFilter: LinearLayout
    private lateinit var toolText: LinearLayout
    private lateinit var toolCrop: LinearLayout
    private lateinit var toolReverse: LinearLayout
    private lateinit var toolMute: LinearLayout
    private lateinit var toolCompress: LinearLayout
    private lateinit var toolGif: LinearLayout
    private lateinit var toolFrame: LinearLayout

    private var exoPlayer: ExoPlayer? = null
    private var currentVideoPath: String? = null
    private var videoDurationMs: Long = 0L
    private val handler = Handler(Looper.getMainLooper())
    private var isPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_editor)

        bindViews()
        setupPlayer()
        setupTimeline()
        setupTools()
        startPlayheadUpdater()
    }

    private fun bindViews() {
        playerView = findViewById(R.id.playerView)
        btnBigPlay = findViewById(R.id.btnBigPlay)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        btnSkipBack = findViewById(R.id.btnSkipBack)
        btnSkipForward = findViewById(R.id.btnSkipForward)
        emptyState = findViewById(R.id.emptyState)
        timelineView = findViewById(R.id.timelineView)
        tvCurrentTime = findViewById(R.id.tvCurrentTime)
        tvDuration = findViewById(R.id.tvDuration)
        tvStatus = findViewById(R.id.tvStatus)
        progressBar = findViewById(R.id.progressBar)
        progressContainer = findViewById(R.id.progressContainer)
        btnExport = findViewById(R.id.btnExport)

        toolTrim = findViewById(R.id.toolTrim)
        toolSplit = findViewById(R.id.toolSplit)
        toolRotate = findViewById(R.id.toolRotate)
        toolSpeed = findViewById(R.id.toolSpeed)
        toolVolume = findViewById(R.id.toolVolume)
        toolFilter = findViewById(R.id.toolFilter)
        toolText = findViewById(R.id.toolText)
        toolCrop = findViewById(R.id.toolCrop)
        toolReverse = findViewById(R.id.toolReverse)
        toolMute = findViewById(R.id.toolMute)
        toolCompress = findViewById(R.id.toolCompress)
        toolGif = findViewById(R.id.toolGif)
        toolFrame = findViewById(R.id.toolFrame)
    }

    // ============================================================
    //  PLAYER
    // ============================================================
    private fun setupPlayer() {
        // ExoPlayer
        exoPlayer = ExoPlayer.Builder(this).build().also { player ->
            playerView.player = player
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        isPlaying = false
                        btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
                        btnBigPlay.visibility = View.VISIBLE
                    }
                }
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
            })
        }

        btnBigPlay.setOnClickListener { playPause() }
        btnPlayPause.setOnClickListener { playPause() }
        btnSkipBack.setOnClickListener { seekRelative(-5000) }
        btnSkipForward.setOnClickListener { seekRelative(5000) }

        findViewById<Button>(R.id.btnPickVideo).setOnClickListener { pickVideo() }
        btnExport.setOnClickListener {
            Toast.makeText(this, "Fitur export akan segera hadir", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playPause() {
        val p = exoPlayer ?: return
        if (p.isPlaying) {
            p.pause()
            btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
            btnBigPlay.visibility = View.VISIBLE
        } else {
            p.play()
            btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
            btnBigPlay.visibility = View.GONE
        }
    }

    private fun seekRelative(deltaMs: Long) {
        val p = exoPlayer ?: return
        val target = (p.currentPosition + deltaMs).coerceIn(0L, videoDurationMs)
        p.seekTo(target)
    }

    private fun pickVideo() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "video/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(Intent.createChooser(intent, "Pilih Video"), REQ_PICK_VIDEO)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_PICK_VIDEO && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            loadVideoFromUri(uri)
        }
    }

    private fun loadVideoFromUri(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val cacheFile = File(cacheDir, "input_${System.currentTimeMillis()}.mp4")
            cacheFile.outputStream().use { output -> inputStream?.copyTo(output) }
            inputStream?.close()
            loadVideo(cacheFile.absolutePath)
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadVideo(path: String) {
        currentVideoPath = path
        val file = File(path)

        // Set ke ExoPlayer
        exoPlayer?.let { p ->
            p.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            p.prepare()
            p.playWhenReady = true
        }

        // Empty state hilang
        emptyState.visibility = View.GONE
        btnBigPlay.visibility = View.GONE
        btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)

        // Metadata
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(path)
            videoDurationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            tvDuration.text = formatTime(videoDurationMs)
        } catch (_: Exception) {}
        retriever.release()

        // Timeline
        timelineView.setVideoDuration(videoDurationMs)
        timelineView.setRange(0, videoDurationMs)
        timelineView.setPlayhead(0)

        // Thumbnails (background)
        extractThumbnails(path)

        val sizeMB = file.length() / (1024.0 * 1024.0)
        tvStatus.text = "✅ ${file.name} (${String.format("%.1f", sizeMB)} MB)"
    }

    private fun extractThumbnails(path: String) {
        thread {
            val thumbs = mutableListOf<Bitmap>()
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(path)
                val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
                for (i in 0 until THUMB_COUNT) {
                    val timeUs = (durationMs * i / THUMB_COUNT) * 1000
                    val bmp = retriever.getFrameAtTime(timeUs,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    if (bmp != null) thumbs.add(bmp)
                }
            } catch (_: Exception) {}
            retriever.release()

            runOnUiThread {
                timelineView.setThumbnails(thumbs)
            }
        }
    }

    // ============================================================
    //  TIMELINE
    // ============================================================
    private fun setupTimeline() {
        timelineView.onPlayheadMoved = { ms ->
            exoPlayer?.seekTo(ms)
            tvCurrentTime.text = formatTime(ms)
        }
        timelineView.onRangeChanged = { start, end ->
            // Update status (info trim range)
            val dur = end - start
            tvStatus.text = "Range: ${formatTime(start)} – ${formatTime(end)} (${formatTime(dur)})"
        }
    }

    private fun startPlayheadUpdater() {
        handler.post(object : Runnable {
            override fun run() {
                exoPlayer?.let { p ->
                    val pos = p.currentPosition
                    tvCurrentTime.text = formatTime(pos)
                    timelineView.setPlayhead(pos)
                }
                handler.postDelayed(this, 200)
            }
        })
    }

    private fun formatTime(millis: Long): String {
        val totalSec = millis / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format("%02d:%02d", min, sec)
    }

    // ============================================================
    //  TOOLS
    // ============================================================
    private fun setupTools() {
        toolTrim.setOnClickListener { doTrim() }
        toolSplit.setOnClickListener { doSplit() }
        toolRotate.setOnClickListener { doRotate() }
        toolSpeed.setOnClickListener { doSpeed() }
        toolVolume.setOnClickListener { doVolume() }
        toolFilter.setOnClickListener { doFilter() }
        toolText.setOnClickListener { doText() }
        toolCrop.setOnClickListener { doCrop() }
        toolReverse.setOnClickListener { doReverse() }
        toolMute.setOnClickListener { doMute() }
        toolCompress.setOnClickListener { doCompress() }
        toolGif.setOnClickListener { doGif() }
        toolFrame.setOnClickListener { doFrame() }
    }

    private fun requireVideo(): String? {
        val p = currentVideoPath
        if (p == null) {
            Toast.makeText(this, "Pilih video dulu", Toast.LENGTH_SHORT).show()
        }
        return p
    }

    private fun doTrim() {
        val path = requireVideo() ?: return
        val startMs = timelineView.rangeStartMs
        val endMs = timelineView.rangeEndMs
        val startSec = (startMs / 1000.0).toString()
        val durationSec = ((endMs - startMs) / 1000.0).toString()
        runEdit("trim") { ok, err ->
            VideoEditorHelper.trim(this, path, startSec, durationSec, ok, err)
        }
    }

    private fun doSplit() {
        val path = requireVideo() ?: return
        val posMs = exoPlayer?.currentPosition ?: 0L
        if (posMs <= 500 || posMs >= videoDurationMs - 500) {
            Toast.makeText(this, "Posisikan playhead di tengah video", Toast.LENGTH_SHORT).show()
            return
        }
        val posSec = (posMs / 1000.0).toString()
        Toast.makeText(this, "Split di ${formatTime(posMs)}", Toast.LENGTH_SHORT).show()
        runEdit("split-part1") { ok, err ->
            VideoEditorHelper.trim(this, path, "0", posSec, ok, err)
        }
    }

    private fun doRotate() {
        val path = requireVideo() ?: return
        showChoiceDialog("🔄 Rotate", arrayOf("90°", "180°", "270°")) { choice ->
            val deg = when (choice) { "90°" -> "90"; "180°" -> "180"; else -> "270" }
            runEdit("rotate") { ok, err ->
                VideoEditorHelper.rotate(this, path, deg, ok, err)
            }
        }
    }

    private fun doSpeed() {
        val path = requireVideo() ?: return
        showChoiceDialog("⚡ Speed", arrayOf("0.5x", "1x", "1.5x", "2x")) { choice ->
            val sp = when {
                choice.startsWith("0.5") -> "0.5"
                choice.startsWith("1.5") -> "1.5"
                choice.startsWith("2") -> "2.0"
                else -> "1.0"
            }
            runEdit("speed") { ok, err ->
                VideoEditorHelper.speed(this, path, sp, ok, err)
            }
        }
    }

    private fun doVolume() {
        val path = requireVideo() ?: return
        showInputDialog("🎵 Volume", "Volume (0-10):", "") { p1, _ ->
            val vol = if (p1.isEmpty()) "1" else p1
            runEdit("volume") { ok, err ->
                VideoEditorHelper.volume(this, path, vol, ok, err)
            }
        }
    }

    private fun doFilter() {
        val path = requireVideo() ?: return
        val filters = arrayOf("Grayscale", "Vintage", "Warm", "Cool", "Blur",
            "Sharp", "Sepia", "Dramatic", "Cartoon")
        showChoiceDialog("🎨 Filter", filters) { choice ->
            runEdit("filter") { ok, err ->
                VideoEditorHelper.filter(this, path, choice.lowercase(), ok, err)
            }
        }
    }

    private fun doText() {
        val path = requireVideo() ?: return
        showInputDialog("📝 Text", "Teks:", "Posisi (top/center/bottom):") { p1, p2 ->
            val pos = if (p2.isEmpty()) "center" else p2
            runEdit("text") { ok, err ->
                VideoEditorHelper.addText(this, path, p1, pos, ok, err)
            }
        }
    }

    private fun doCrop() {
        val path = requireVideo() ?: return
        showInputDialog("📐 Crop", "Lebar x Tinggi (720x1280):", "Posisi (0:0):") { p1, p2 ->
            val dims = p1.split("x")
            if (dims.size == 2) {
                val pos = if (p2.isEmpty()) "0:0" else p2
                runEdit("crop") { ok, err ->
                    VideoEditorHelper.crop(this, path, dims[0], dims[1], pos, ok, err)
                }
            } else {
                Toast.makeText(this, "Format: 720x1280", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun doReverse() {
        val path = requireVideo() ?: return
        runEdit("reverse") { ok, err ->
            VideoEditorHelper.reverse(this, path, ok, err)
        }
    }

    private fun doMute() {
        val path = requireVideo() ?: return
        runEdit("mute") { ok, err ->
            VideoEditorHelper.mute(this, path, ok, err)
        }
    }

    private fun doCompress() {
        val path = requireVideo() ?: return
        runEdit("compress") { ok, err ->
            VideoEditorHelper.compress(this, path, ok, err)
        }
    }

    private fun doGif() {
        val path = requireVideo() ?: return
        runEdit("gif") { ok, err ->
            VideoEditorHelper.toGif(this, path, "10", "480", ok, err)
        }
    }

    private fun doFrame() {
        val path = requireVideo() ?: return
        val posMs = exoPlayer?.currentPosition ?: 0L
        val sec = posMs / 1000.0
        val ts = String.format(java.util.Locale.US, "%.2f", sec)
        runEdit("screenshot") { ok, err ->
            VideoEditorHelper.screenshot(this, path, ts, ok, err)
        }
    }

    // ============================================================
    //  RUN EDIT
    // ============================================================
    private inline fun runEdit(name: String,
        crossinline block: (ok: (String) -> Unit, err: (String) -> Unit) -> Unit) {
        val path = currentVideoPath
        if (path == null) {
            Toast.makeText(this, "Pilih video dulu", Toast.LENGTH_SHORT).show()
            return
        }
        progressContainer.visibility = View.VISIBLE
        progressBar.isIndeterminate = true
        tvStatus.text = "⏳ Memproses $name..."
        block(
            { output ->
                runOnUiThread {
                    progressBar.isIndeterminate = false
                    progressBar.progress = 100
                    tvStatus.text = "✅ Selesai: ${File(output).name}"
                    progressContainer.visibility = View.GONE
                    Toast.makeText(this, "Berhasil: ${File(output).name}",
                        Toast.LENGTH_LONG).show()
                    // Load hasil edit ke preview
                    loadVideo(output)
                }
            },
            { err ->
                runOnUiThread {
                    progressContainer.visibility = View.GONE
                    tvStatus.text = "❌ $err"
                    Toast.makeText(this, "Gagal: $err", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    // ============================================================
    //  DIALOG HELPERS
    // ============================================================
    private fun showInputDialog(title: String, hint1: String, hint2: String,
        onOk: (String, String) -> Unit) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }
        val et1 = EditText(this).apply { hint = hint1 }
        layout.addView(et1)
        val et2 = if (hint2.isNotEmpty()) {
            EditText(this).apply { hint = hint2 }.also { layout.addView(it) }
        } else null

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(layout)
            .setPositiveButton("OK") { _, _ ->
                onOk(et1.text.toString(), et2?.text?.toString() ?: "")
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showChoiceDialog(title: String, choices: Array<String>,
        onPick: (String) -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(choices) { _, which -> onPick(choices[which]) }
            .setNegativeButton("Batal", null)
            .show()
    }

    // ============================================================
    //  LIFECYCLE
    // ============================================================
    override fun onPause() {
        super.onPause()
        exoPlayer?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        exoPlayer?.release()
        exoPlayer = null
    }
}
