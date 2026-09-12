package com.yad.videoeditor

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

/**
 * Video Editor lengkap — trim, rotate, speed, compress, dll.
 */
class VideoEditorActivity : AppCompatActivity() {

    companion object {
        private const val REQ_PICK_VIDEO = 2001
    }

    private var videoUri: Uri? = null
    private lateinit var tvStatus: TextView
    private lateinit var tvSelected: TextView
    private lateinit var seekBar: SeekBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { setContentView(R.layout.activity_video_editor) }
        catch (e: Exception) { finish(); return }

        tvStatus = findViewById(R.id.tvStatus)
        tvSelected = findViewById(R.id.tvSelected)
        seekBar = findViewById(R.id.seekBarTrim)

        findViewById<Button>(R.id.btnPickVideo)?.setOnClickListener {
            pickVideo()
        }

        findViewById<Button>(R.id.btnTrim)?.setOnClickListener { doAction("Trim") }
        findViewById<Button>(R.id.btnRotate)?.setOnClickListener { doAction("Rotate") }
        findViewById<Button>(R.id.btnSpeed)?.setOnClickListener { doAction("Speed") }
        findViewById<Button>(R.id.btnExtractAudio)?.setOnClickListener { doAction("Extract Audio") }
        findViewById<Button>(R.id.btnCompress)?.setOnClickListener { doAction("Compress") }
        findViewById<Button>(R.id.btnMerge)?.setOnClickListener { doAction("Merge") }
        findViewById<Button>(R.id.btnAddMusic)?.setOnClickListener { doAction("Add Music") }
        findViewById<Button>(R.id.btnAddText)?.setOnClickListener { doAction("Add Text") }
        findViewById<Button>(R.id.btnCrop)?.setOnClickListener { doAction("Crop") }
        findViewById<Button>(R.id.btnReverse)?.setOnClickListener { doAction("Reverse") }
    }

    private fun pickVideo() {
        val i = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "video/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(Intent.createChooser(i, "Pilih Video"), REQ_PICK_VIDEO)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_PICK_VIDEO && resultCode == Activity.RESULT_OK) {
            videoUri = data?.data
            tvSelected?.text = "📁 ${videoUri?.lastPathSegment ?: "Video"}"
            tvStatus?.text = "✅ Video dipilih"
        }
    }

    private fun doAction(action: String) {
        if (videoUri == null) {
            Toast.makeText(this, "Pilih video dulu", Toast.LENGTH_SHORT).show()
            return
        }
        // Placeholder — proses di server/FFmpeg
        tvStatus?.text = "⏳ $action sedang diproses..."
        Toast.makeText(this, "🔄 $action — coming soon", Toast.LENGTH_LONG).show()
    }
}
