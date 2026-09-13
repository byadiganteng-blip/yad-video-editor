package com.yad.videoeditor

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.File

/**
 * VideoEditorActivity — editor video lengkap dengan preview player.
 *
 * Semua operasi edit pakai VideoEditorHelper (ffmpeg lokal).
 */
class VideoEditorActivity : AppCompatActivity() {

    companion object { private const val REQ_PICK_VIDEO = 2001 }

    private lateinit var videoView: VideoView
    private lateinit var btnPlayPause: Button
    private lateinit var seekBar: SeekBar
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvDuration: TextView
    private lateinit var tvVideoInfo: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvStatusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressContainer: LinearLayout

    private var currentVideoPath: String? = null
    private var isVideoPlaying = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_editor)

        bindViews()
        setupPlayer()
        setupEditorButtons()
    }

    private fun bindViews() {
        videoView = findViewById(R.id.videoView)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        seekBar = findViewById(R.id.seekBar)
        tvCurrentTime = findViewById(R.id.tvCurrentTime)
        tvDuration = findViewById(R.id.tvDuration)
        tvVideoInfo = findViewById(R.id.tvVideoInfo)
        tvStatus = findViewById(R.id.tvStatus)
        tvStatusText = findViewById(R.id.tvStatusText)
        progressBar = findViewById(R.id.progressBar)
        progressContainer = findViewById(R.id.progressContainer)
    }

    // ============================================================
    //  PLAYER SETUP
    // ============================================================
    private fun setupPlayer() {
        findViewById<Button>(R.id.btnPickVideo).setOnClickListener { pickVideo() }
        btnPlayPause.setOnClickListener { togglePlayPause() }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    videoView.seekTo(progress)
                    tvCurrentTime.text = formatTime(progress)
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {
                if (isVideoPlaying) videoView.pause()
            }
            override fun onStopTrackingTouch(sb: SeekBar?) {
                if (isVideoPlaying) videoView.start()
            }
        })

        videoView.setOnPreparedListener { mp ->
            mp.isLooping = true
            seekBar.max = videoView.duration
            tvDuration.text = formatTime(videoView.duration)
        }

        videoView.setOnCompletionListener {
            isVideoPlaying = false
            btnPlayPause.text = "▶️ PLAY"
            seekBar.progress = 0
        }

        // Update seekbar tiap 500ms
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (videoView.isPlaying) {
                    val pos = videoView.currentPosition
                    seekBar.progress = pos
                    tvCurrentTime.text = formatTime(pos)
                }
                handler.postDelayed(this, 500)
            }
        }, 500)
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
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val cacheFile = File(cacheDir, "input_${System.currentTimeMillis()}.mp4")
                cacheFile.outputStream().use { output -> inputStream?.copyTo(output) }
                inputStream?.close()
                currentVideoPath = cacheFile.absolutePath
                loadVideo(cacheFile.absolutePath)
            } catch (e: Exception) {
                Toast.makeText(this, "Gagal: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadVideo(path: String) {
        videoView.setVideoPath(path)
        videoView.start()
        isVideoPlaying = true
        btnPlayPause.text = "⏸️ PAUSE"

        val file = File(path)
        val sizeMB = file.length() / (1024.0 * 1024.0)
        tvVideoInfo.text = "📹 ${file.name} (${String.format("%.1f", sizeMB)} MB)"
        tvStatusText.text = "✅ Video siap. Pilih fitur editor di bawah."
    }

    private fun togglePlayPause() {
        if (videoView.isPlaying) {
            videoView.pause()
            isVideoPlaying = false
            btnPlayPause.text = "▶️ PLAY"
        } else {
            videoView.start()
            isVideoPlaying = true
            btnPlayPause.text = "⏸️ PAUSE"
        }
    }

    private fun formatTime(millis: Int): String {
        val totalSec = millis / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format("%02d:%02d", min, sec)
    }

    // ============================================================
    //  EDITOR BUTTONS
    // ============================================================
    private fun setupEditorButtons() {
        findViewById<Button>(R.id.btnTrim).setOnClickListener {
            showInputDialog("✂️ Trim Video", "Mulai (detik):", "Durasi (detik):") { p1, p2 ->
                runEdit("trim") { ok, err ->
                    VideoEditorHelper.trim(this, currentVideoPath!!, p1, p2, ok, err)
                }
            }
        }

        findViewById<Button>(R.id.btnRotate).setOnClickListener {
            showChoiceDialog("🔄 Rotate", arrayOf("90°", "180°", "270°")) { choice ->
                val deg = when (choice) { "90°" -> "90"; "180°" -> "180"; else -> "270" }
                runEdit("rotate") { ok, err ->
                    VideoEditorHelper.rotate(this, currentVideoPath!!, deg, ok, err)
                }
            }
        }

        findViewById<Button>(R.id.btnSpeed).setOnClickListener {
            showChoiceDialog("⚡ Speed", arrayOf("0.5x (Slow)", "1x (Normal)", "1.5x", "2x (Fast)")) { choice ->
                val sp = when {
                    choice.startsWith("0.5") -> "0.5"
                    choice.startsWith("1.5") -> "1.5"
                    choice.startsWith("2") -> "2.0"
                    else -> "1.0"
                }
                runEdit("speed") { ok, err ->
                    VideoEditorHelper.speed(this, currentVideoPath!!, sp, ok, err)
                }
            }
        }

        findViewById<Button>(R.id.btnAudio).setOnClickListener {
            showInputDialog("🎵 Volume Audio", "Volume (0-10, 1=normal):", "") { p1, _ ->
                val vol = if (p1.isEmpty()) "1" else p1
                runEdit("volume") { ok, err ->
                    VideoEditorHelper.volume(this, currentVideoPath!!, vol, ok, err)
                }
            }
        }

        findViewById<Button>(R.id.btnCompress).setOnClickListener {
            runEdit("compress") { ok, err ->
                VideoEditorHelper.compress(this, currentVideoPath!!, ok, err)
            }
        }

        findViewById<Button>(R.id.btnMute).setOnClickListener {
            runEdit("mute") { ok, err ->
                VideoEditorHelper.mute(this, currentVideoPath!!, ok, err)
            }
        }

        findViewById<Button>(R.id.btnFilter).setOnClickListener {
            val filters = arrayOf("Grayscale", "Vintage", "Warm", "Cool", "Blur", "Sharp", "Sepia", "Dramatic", "Cartoon")
            showChoiceDialog("🎨 Filter", filters) { choice ->
                runEdit("filter") { ok, err ->
                    VideoEditorHelper.filter(this, currentVideoPath!!, choice.lowercase(), ok, err)
                }
            }
        }

        findViewById<Button>(R.id.btnResize).setOnClickListener {
            showChoiceDialog("📐 Resize", arrayOf("480p", "720p", "1080p", "1440p")) { choice ->
                val (w, h) = when (choice) {
                    "480p" -> "854" to "480"
                    "720p" -> "1280" to "720"
                    "1080p" -> "1920" to "1080"
                    else -> "2560" to "1440"
                }
                runEdit("resize") { ok, err ->
                    VideoEditorHelper.resize(this, currentVideoPath!!, w, h, ok, err)
                }
            }
        }

        findViewById<Button>(R.id.btnCrop).setOnClickListener {
            showInputDialog("✂️ Crop", "Lebar x Tinggi (720x1280):", "Posisi (0:0):") { p1, p2 ->
                val dims = p1.split("x")
                if (dims.size == 2) {
                    val pos = if (p2.isEmpty()) "0:0" else p2
                    runEdit("crop") { ok, err ->
                        VideoEditorHelper.crop(this, currentVideoPath!!, dims[0], dims[1], pos, ok, err)
                    }
                } else {
                    Toast.makeText(this, "Format: 720x1280", Toast.LENGTH_SHORT).show()
                }
            }
        }

        findViewById<Button>(R.id.btnReverse).setOnClickListener {
            runEdit("reverse") { ok, err ->
                VideoEditorHelper.reverse(this, currentVideoPath!!, ok, err)
            }
        }

        findViewById<Button>(R.id.btnGif).setOnClickListener {
            runEdit("gif") { ok, err ->
                VideoEditorHelper.toGif(this, currentVideoPath!!, "10", "480", ok, err)
            }
        }

        findViewById<Button>(R.id.btnScreenshot).setOnClickListener {
            showInputDialog("📸 Screenshot", "Timestamp (00:00:05):", "") { p1, _ ->
                val ts = if (p1.isEmpty()) "00:00:01" else p1
                runEdit("screenshot") { ok, err ->
                    VideoEditorHelper.screenshot(this, currentVideoPath!!, ts, ok, err)
                }
            }
        }

        findViewById<Button>(R.id.btnText).setOnClickListener {
            showInputDialog("📝 Text Overlay", "Teks:", "Posisi (top/center/bottom):") { p1, p2 ->
                val pos = if (p2.isEmpty()) "center" else p2
                runEdit("text") { ok, err ->
                    VideoEditorHelper.addText(this, currentVideoPath!!, p1, pos, ok, err)
                }
            }
        }

        findViewById<Button>(R.id.btnMerge).setOnClickListener {
            Toast.makeText(this, "Fitur merge segera hadir", Toast.LENGTH_SHORT).show()
        }
    }

    // ============================================================
    //  HELPER — run edit dengan progress
    // ============================================================
    private inline fun runEdit(name: String, crossinline block: (ok: (String) -> Unit, err: (String) -> Unit) -> Unit) {
        val path = currentVideoPath
        if (path == null) {
            Toast.makeText(this, "Pilih video dulu", Toast.LENGTH_SHORT).show()
            return
        }

        progressContainer.visibility = View.VISIBLE
        progressBar.isIndeterminate = true
        tvStatus.text = "⏳ Memproses $name..."
        tvStatusText.text = "Proses: $name"

        block(
            { output ->
                runOnUiThread {
                    progressBar.isIndeterminate = false
                    progressBar.progress = 100
                    tvStatus.text = "✅ Selesai: $output"
                    tvStatusText.text = "✅ Hasil edit: $output"
                    Toast.makeText(this, "Berhasil disimpan ke $output", Toast.LENGTH_LONG).show()
                    // Load hasil
                    currentVideoPath = output
                    loadVideo(output)
                }
            },
            { err ->
                runOnUiThread {
                    progressContainer.visibility = View.GONE
                    tvStatusText.text = "❌ $err"
                    Toast.makeText(this, "Gagal: $err", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun showInputDialog(
        title: String,
        hint1: String,
        hint2: String,
        onOk: (String, String) -> Unit
    ) {
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

    private fun showChoiceDialog(title: String, choices: Array<String>, onPick: (String) -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(choices) { _, which -> onPick(choices[which]) }
            .setNegativeButton("Batal", null)
            .show()
    }
}
