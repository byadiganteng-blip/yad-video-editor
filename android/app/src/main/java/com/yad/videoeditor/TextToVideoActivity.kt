package com.yad.videoeditor

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class TextToVideoActivity : AppCompatActivity() {
    companion object { private const val REQ_PICK_TXT = 1001 }

    private lateinit var etStory: EditText
    private lateinit var etWatermark: EditText
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { setContentView(R.layout.activity_text_to_video) }
        catch (e: Exception) { finish(); return }

        etStory = findViewById(R.id.etStory)
        etWatermark = findViewById(R.id.etWatermark)
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

        // Setup spinners
        spVideoSize.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            VideoSizePreset.ALL.map { "${it.displayName} (${it.aspectRatio})" })
        spQuality.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            QualityPreset.ALL.map { it.displayName })
        spQuality.setSelection(3)
        spVoice.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            VoicePreset.ALL.map { it.displayName })
        spSubtitleStyle.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            SubtitleStyle.ALL.map { it.displayName })

        tvStatus.text = if (SecureConfig.hasGithubToken())
            "✅ Siap membuat video"
        else
            "⚠️ Token tidak tersedia"

        findViewById<Button>(R.id.btnGenerate).setOnClickListener { generate() }
        findViewById<Button>(R.id.btnUploadTxt).setOnClickListener { pickTxtFile() }
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
                val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!text.isNullOrBlank()) {
                    etStory.setText(text)
                    Toast.makeText(this, "File dimuat", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Gagal: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun generate() {
        val story = etStory.text.toString().trim()
        if (story.isEmpty()) {
            Toast.makeText(this, "Masukkan cerita", Toast.LENGTH_SHORT).show()
            return
        }
        if (!SecureConfig.hasGithubToken()) {
            Toast.makeText(this, "Token tidak tersedia", Toast.LENGTH_LONG).show()
            return
        }

        val watermark = if (swShowWatermark.isChecked) etWatermark.text.toString().trim() else ""
        val voice = VoicePreset.ALL[spVoice.selectedItemPosition].id
        val showSubtitle = swShowSubtitle.isChecked
        val subtitleStyle = SubtitleStyle.ALL[spSubtitleStyle.selectedItemPosition].id

        progressContainer.visibility = View.VISIBLE
        progressBar.progress = 0
        tvPercent.text = "0%"
        tvStatus.text = "Memulai..."

        lifecycleScope.launch {
            try {
                AiImageGenerator.generateVideo(
                    this@TextToVideoActivity,
                    story,
                    voice,
                    watermark,
                    showSubtitle,
                    subtitleStyle,
                    AiImageGenerator.ProgressCallback { scene, total, stage, pct, msg ->
                        runOnUiThread {
                            progressBar.progress = pct
                            tvPercent.text = "$pct%"
                            tvStatus.text = msg
                        }
                    }
                )
                progressBar.progress = 100
                tvPercent.text = "100%"
                tvStatus.text = "✅ Video selesai dibuat!"
                Toast.makeText(this@TextToVideoActivity,
                    "Video selesai! Cek di folder Video Saya", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                tvStatus.text = "❌ Error: ${e.message}"
            }
        }
    }
}
