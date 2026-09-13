package com.yad.videoeditor

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class TextToVideoActivity : AppCompatActivity() {
    companion object { private const val REQ_PICK_TXT = 1001 }

    private lateinit var etStory: EditText
    private lateinit var etWatermark: EditText
    private lateinit var spModel: Spinner
    private lateinit var spVideoSize: Spinner
    private lateinit var spQuality: Spinner
    private lateinit var spVoice: Spinner
    private lateinit var spSubtitleStyle: Spinner
    private lateinit var swShowSubtitle: Switch
    private lateinit var swShowWatermark: Switch
    private lateinit var tvStatus: TextView
    private lateinit var tvPercent: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressContainer: LinearLayout
    private lateinit var btnDownloadNow: Button
    private var lastVideoPath: String? = null

    private val progressReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                VideoGeneratorService.ACTION_PROGRESS -> {
                    val pct = intent.getIntExtra(VideoGeneratorService.EXTRA_PERCENT, 0)
                    val msg = intent.getStringExtra(VideoGeneratorService.EXTRA_MESSAGE) ?: ""
                    updateProgress(pct, msg)
                }
                VideoGeneratorService.ACTION_DONE -> {
                    val path = intent.getStringExtra(VideoGeneratorService.EXTRA_FILE_PATH)
                    lastVideoPath = path
                    progressBar.progress = 100
                    tvPercent.text = "100%"
                    tvStatus.text = "✅ Video selesai!"
                    btnDownloadNow.visibility = View.VISIBLE
                }
                VideoGeneratorService.ACTION_FAILED -> {
                    val msg = intent.getStringExtra(VideoGeneratorService.EXTRA_MESSAGE) ?: "Gagal"
                    tvStatus.text = "❌ $msg"
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { setContentView(R.layout.activity_text_to_video) }
        catch (e: Exception) { finish(); return }

        etStory = findViewById(R.id.etStory)
        etWatermark = findViewById(R.id.etWatermark)
        spModel = findViewById(R.id.spModel)
        spVideoSize = findViewById(R.id.spVideoSize)
        spQuality = findViewById(R.id.spQuality)
        spVoice = findViewById(R.id.spVoice)
        spSubtitleStyle = findViewById(R.id.spSubtitleStyle)
        swShowSubtitle = findViewById(R.id.swShowSubtitle)
        swShowWatermark = findViewById(R.id.swShowWatermark)
        tvStatus = findViewById(R.id.tvStatus)
        tvPercent = findViewById(R.id.tvPercent)
        progressBar = findViewById(R.id.progressBar)
        progressContainer = findViewById(R.id.progressContainer)
        btnDownloadNow = findViewById(R.id.btnDownloadNow)

        // Spinner model pakai GenerationMode (12 mode: 2 non-AI + 10 AI)
        spModel.adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_dropdown_item,
            GenerationMode.allLabels())
        spVideoSize.adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_dropdown_item,
            VideoSizePreset.ALL.map { "${it.displayName} (${it.aspectRatio})" })
        spQuality.adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_dropdown_item,
            QualityPreset.ALL.map { it.displayName })
        spQuality.setSelection(3)
        spVoice.adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_dropdown_item,
            VoicePreset.ALL.map { it.displayName })
        spSubtitleStyle.adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_dropdown_item,
            SubtitleStyle.ALL.map { it.displayName })

        tvStatus.text = if (SecureConfig.hasGithubToken())
            "✨ Siap membuat video" else "⚙️ Layanan sedang disiapkan..."

        findViewById<Button>(R.id.btnGenerate).setOnClickListener { generate() }
        findViewById<Button>(R.id.btnUploadTxt).setOnClickListener { pickTxtFile() }

        btnDownloadNow.setOnClickListener {
            lastVideoPath?.let { path ->
                val intent = Intent(this, VideoPreviewActivity::class.java)
                intent.putExtra("video_path", path)
                startActivity(intent)
            } ?: Toast.makeText(this, "Video belum siap", Toast.LENGTH_SHORT).show()
        }

        // ============================================================
        //  RESTORE STATE — kalau service masih running
        // ============================================================
        restoreState()
    }

    private fun restoreState() {
        if (GeneratorState.isRunning(this) || VideoGeneratorService.isRunning) {
            val pct = GeneratorState.getProgress(this)
            val msg = GeneratorState.getMessage(this)
            progressContainer.visibility = View.VISIBLE
            progressBar.progress = pct
            tvPercent.text = "$pct%"
            tvStatus.text = if (msg.isNotEmpty())
                "⏳ $msg" else "⏳ Proses berjalan di background..."
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction(VideoGeneratorService.ACTION_PROGRESS)
            addAction(VideoGeneratorService.ACTION_DONE)
            addAction(VideoGeneratorService.ACTION_FAILED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(progressReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(progressReceiver, filter)
        }
        // Restore setiap kali activity kembali ke foreground
        restoreState()
    }

    override fun onPause() {
        super.onPause()
        try { unregisterReceiver(progressReceiver) } catch (_: Exception) {}
    }

    private fun updateProgress(pct: Int, msg: String) {
        progressContainer.visibility = View.VISIBLE
        progressBar.progress = pct
        tvPercent.text = "$pct%"
        tvStatus.text = msg
        // Update floating progress
        if (FloatingProgressService.isRunning) {
            FloatingProgressService.update(this, pct, msg)
        }
    }

    private fun pickTxtFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "text/plain"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(Intent.createChooser(intent, "Pilih .txt"), REQ_PICK_TXT)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_PICK_TXT && resultCode == Activity.RESULT_OK) {
            val uri: Uri = data?.data ?: return
            try {
                val text = contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.use { it.readText() }
                if (!text.isNullOrBlank()) etStory.setText(text)
            } catch (_: Exception) {}
        }
    }

    private fun generate() {
        val story = etStory.text.toString().trim()
        if (story.isEmpty()) {
            Toast.makeText(this, "Masukkan cerita", Toast.LENGTH_SHORT).show()
            return
        }
        if (!SecureConfig.hasGithubToken()) {
            Toast.makeText(this, "Layanan belum siap, coba lagi sebentar", Toast.LENGTH_LONG).show()
            return
        }

        val mode = GenerationMode.values()[spModel.selectedItemPosition]
        val modelName = mode.label
        val watermark = if (swShowWatermark.isChecked) etWatermark.text.toString().trim() else ""
        val voice = VoicePreset.ALL[spVoice.selectedItemPosition].id
        val showSubtitle = swShowSubtitle.isChecked
        val subtitleStyle = SubtitleStyle.ALL[spSubtitleStyle.selectedItemPosition].id

        progressContainer.visibility = View.VISIBLE
        progressBar.progress = 0
        tvPercent.text = "0%"
        tvStatus.text = "Memulai dengan $modelName..."
        btnDownloadNow.visibility = View.GONE
        lastVideoPath = null

        // Simpan state
        GeneratorState.saveRunning(this, true)
        GeneratorState.saveProgress(this, 0, "Memulai...")
        
        // Tampilkan floating progress
        FloatingProgressService.show(this, 0, "Memulai...")

        // ============================================================
        //  HANDLE NON-AI: DIRECT & GOOGLE_IMAGE
        // ============================================================
        if (mode.type == ModeType.DIRECT) {
            // Mode DIRECT: trigger GitHub workflow (bukan generate lokal)
            AutoLogSaver.log("TextToVideo", "DIRECT mode — trigger GitHub workflow")
            triggerGithubWorkflow("direct", story, voice, watermark, showSubtitle, subtitleStyle)
            return
        }

        if (mode.type == ModeType.GOOGLE_IMAGE) {
            // Mode GOOGLE_IMAGE: trigger GitHub workflow
            AutoLogSaver.log("TextToVideo", "GOOGLE_IMAGE mode — trigger GitHub workflow")
            triggerGithubWorkflow("google_image", story, voice, watermark, showSubtitle, subtitleStyle)
            return
        }

        // ============================================================
        //  MODE AI: panggil VideoGeneratorService
        // ============================================================
        val si = Intent(this, VideoGeneratorService::class.java).apply {
            putExtra(VideoGeneratorService.EXTRA_PROMPT, story)
            putExtra(VideoGeneratorService.EXTRA_VOICE, voice)
            putExtra(VideoGeneratorService.EXTRA_WATERMARK, watermark)
            putExtra(VideoGeneratorService.EXTRA_SHOW_SUBTITLE, showSubtitle)
            putExtra(VideoGeneratorService.EXTRA_SUBTITLE_STYLE, subtitleStyle)
            putExtra("model_id", mode.id)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, si)
        } else {
            startService(si)
        }
    }
}
