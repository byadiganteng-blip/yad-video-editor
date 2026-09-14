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
                    updateProgress(
                        intent.getIntExtra(VideoGeneratorService.EXTRA_PERCENT, 0),
                        intent.getStringExtra(VideoGeneratorService.EXTRA_MESSAGE) ?: ""
                    )
                }
                VideoGeneratorService.ACTION_DONE -> {
                    val path = intent.getStringExtra(VideoGeneratorService.EXTRA_FILE_PATH)
                    lastVideoPath = path
                    progressBar.progress = 100
                    tvPercent.text = "100%"
                    tvStatus.text = "✅ Video selesai: " + (path ?: "")
                    btnDownloadNow.visibility = View.VISIBLE
                }
                VideoGeneratorService.ACTION_FAILED -> {
                    tvStatus.text = "❌ " + (intent.getStringExtra(VideoGeneratorService.EXTRA_MESSAGE) ?: "Gagal")
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

        spModel.adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_dropdown_item,
            GenerationMode.allLabels())
        spVideoSize.adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_dropdown_item,
            VideoSizePreset.ALL.map { it.displayName + " (" + it.aspectRatio + ")" })
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
        restoreState()
    }

    private fun restoreState() {
        if (GeneratorState.isRunning(this) || VideoGeneratorService.isRunning) {
            val pct = GeneratorState.getProgress(this)
            val msg = GeneratorState.getMessage(this)
            progressContainer.visibility = View.VISIBLE
            progressBar.progress = pct
            tvPercent.text = "$pct%"
            tvStatus.text = if (msg.isNotEmpty()) "⏳ $msg" else "⏳ Proses di background..."
        }
    }

    override fun onResume() {
        super.onResume()
        val f = IntentFilter().apply {
            addAction(VideoGeneratorService.ACTION_PROGRESS)
            addAction(VideoGeneratorService.ACTION_DONE)
            addAction(VideoGeneratorService.ACTION_FAILED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(progressReceiver, f, Context.RECEIVER_NOT_EXPORTED)
        } else registerReceiver(progressReceiver, f)
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
        if (FloatingProgressService.isRunning) FloatingProgressService.update(this, pct, msg)
    }

    private fun pickTxtFile() {
        startActivityForResult(Intent.createChooser(Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "text/plain"; addCategory(Intent.CATEGORY_OPENABLE)
        }, "Pilih .txt"), REQ_PICK_TXT)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_PICK_TXT && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                try {
                    val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    if (!text.isNullOrBlank()) etStory.setText(text)
                } catch (_: Exception) {}
            }
        }
    }

    private fun generate() {
        val story = etStory.text.toString().trim()
        if (story.isEmpty()) {
            Toast.makeText(this, "Masukkan cerita", Toast.LENGTH_SHORT).show(); return
        }
        if (!SecureConfig.hasGithubToken()) {
            Toast.makeText(this, "Layanan belum siap", Toast.LENGTH_LONG).show(); return
        }

        val mode = GenerationMode.values()[spModel.selectedItemPosition]
        val watermark = if (swShowWatermark.isChecked) etWatermark.text.toString().trim() else ""
        val voice = VoicePreset.ALL[spVoice.selectedItemPosition].id
        val showSubtitle = swShowSubtitle.isChecked
        val subtitleStyle = SubtitleStyle.ALL[spSubtitleStyle.selectedItemPosition].id

        progressContainer.visibility = View.VISIBLE
        progressBar.progress = 0
        tvPercent.text = "0%"
        tvStatus.text = "Memulai dengan ${mode.label}..."
        btnDownloadNow.visibility = View.GONE
        lastVideoPath = null

        GeneratorState.saveRunning(this, true)
        GeneratorState.saveProgress(this, 0, "Memulai...")
        FloatingProgressService.show(this, 0, "Memulai...")

        // Analisa cerita dulu (log)
        val detail = StoryAnalyzer.analyze(story)
        AutoLogSaver.log("TextToVideo", "Story: mood=${detail.mood}, time=${detail.timeOfDay}, loc=${detail.location}")

        val modeStr = when (mode.type) {
            ModeType.DIRECT -> "direct"
            ModeType.GOOGLE_IMAGE -> "google_image"
            ModeType.AI_MODEL -> "ai_model"
        }

        triggerGithubWorkflow(
            mode = modeStr,
            styleSuffix = mode.styleSuffix,   // ← Kirim style
            modelId = mode.id,
            prompt = story,
            voice = voice,
            watermark = watermark,
            showSubtitle = showSubtitle,
            subtitleStyle = subtitleStyle
        )
    }

    private fun triggerGithubWorkflow(
        mode: String,
        styleSuffix: String,
        modelId: String,
        prompt: String,
        voice: String,
        watermark: String,
        showSubtitle: Boolean,
        subtitleStyle: String
    ) {
        try {
            AutoLogSaver.log("TextToVideo", "Trigger: mode=$mode, style=$styleSuffix, modelId=$modelId")
            tvStatus.text = "⏳ Mengirim ke server..."

            val token = SecureConfig.getGithubToken()
            if (token.isNullOrEmpty()) {
                tvStatus.text = "❌ Layanan belum siap"
                FloatingProgressService.hide(this); return
            }

            GitHubApiClient.triggerVideoWorkflow(
                context = this, token = token,
                mode = mode, modelId = modelId,
                styleSuffix = styleSuffix,   // ← TAMBAH
                prompt = prompt, voice = voice,
                watermark = watermark,
                showSubtitle = showSubtitle,
                subtitleStyle = subtitleStyle,
                onSuccess = { runId ->
                    runOnUiThread {
                        AutoLogSaver.log("TextToVideo", "Triggered: runId=$runId")
                        tvStatus.text = "⏳ Video sedang dibuat..."
                        val i = Intent(this@TextToVideoActivity, VideoGeneratorService::class.java).apply {
                            action = VideoGeneratorService.ACTION_POLL_WORKFLOW
                            putExtra(VideoGeneratorService.EXTRA_RUN_ID, runId)
                            putExtra(VideoGeneratorService.EXTRA_TOKEN, token)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i) else startService(i)
                    }
                },
                onError = { err ->
                    runOnUiThread {
                        tvStatus.text = "❌ $err"
                        progressContainer.visibility = View.GONE
                        FloatingProgressService.hide(this)
                        GeneratorState.saveRunning(this, false)
                    }
                }
            )
        } catch (e: Exception) {
            AutoLogSaver.logError("TextToVideo", "trigger failed", e)
            tvStatus.text = "❌ ${e.message}"
            FloatingProgressService.hide(this)
        }
    }
}
