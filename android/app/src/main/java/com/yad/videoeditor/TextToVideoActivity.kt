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
    private lateinit var spVideoSize: Spinner
    private lateinit var spQuality: Spinner
    private lateinit var tvStatus: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { setContentView(R.layout.activity_text_to_video) }
        catch (e: Exception) { finish(); return }

        etStory = findViewById(R.id.etStory)
        spVideoSize = findViewById(R.id.spVideoSize)
        spQuality = findViewById(R.id.spQuality)
        tvStatus = findViewById(R.id.tvStatus)
        progressBar = findViewById(R.id.progressBar)

        spVideoSize.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            VideoSizePreset.ALL.map { "${it.displayName} (${it.aspectRatio})" })
        spQuality.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            QualityPreset.ALL.map { it.displayName })
        spQuality.setSelection(3)

        if (!SecureConfig.hasGithubToken()) {
            tvStatus.text = "⚠️ Token GitHub tidak tersedia"
        } else {
            tvStatus.text = "✅ Siap — generate via GitHub Actions"
        }

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
            Toast.makeText(this, "Token GitHub tidak tersedia", Toast.LENGTH_LONG).show()
            return
        }

        val videoSize = VideoSizePreset.ALL[spVideoSize.selectedItemPosition]
        val quality = QualityPreset.ALL[spQuality.selectedItemPosition]

        progressBar.visibility = View.VISIBLE
        tvStatus.text = "Mengirim ke GitHub Actions..."

        lifecycleScope.launch {
            try {
                val scenes = AiImageGenerator.splitIntoScenes(story, 8)
                var ok = 0
                scenes.forEachIndexed { i, scene ->
                    tvStatus.text = "Gambar ${i+1}/${scenes.size} (via GitHub)..."
                    if (AiImageGenerator.generateImage(this@TextToVideoActivity, scene) != null) ok++
                }
                progressBar.visibility = View.GONE
                tvStatus.text = "Selesai: $ok/${scenes.size} gambar\n" +
                        "Ukuran: ${videoSize.displayName}\nKualitas: ${quality.displayName}"
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                tvStatus.text = "Error: ${e.message}"
            }
        }
    }
}
