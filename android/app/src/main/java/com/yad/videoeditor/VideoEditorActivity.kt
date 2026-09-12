package com.yad.videoeditor

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File

/**
 * Video Editor - Media3 Transformer
 * Created by KARYADI, Coding by KARYADI
 *
 * Note: Media3 support trim, crop, rotate, scale.
 * Speed change & extract audio akan ditambah nanti.
 */
class VideoEditorActivity : AppCompatActivity() {

    companion object {
        private const val REQ_PICK_VIDEO = 1001
    }

    private lateinit var tvSelected: TextView
    private lateinit var tvStatus: TextView
    private var selectedVideo: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_video_editor)
        } catch (e: Exception) { finish(); return }

        tvSelected = findViewById(R.id.tvSelected)
        tvStatus = findViewById(R.id.tvStatus)

        findViewById<Button>(R.id.btnPickVideo)?.setOnClickListener { pickVideo() }
        findViewById<Button>(R.id.btnTrim)?.setOnClickListener {
            toast("Trim: coming soon (via Media3 Transformer)")
        }
        findViewById<Button>(R.id.btnRotate)?.setOnClickListener {
            toast("Rotate: coming soon (via Media3 Transformer)")
        }
        findViewById<Button>(R.id.btnSpeed)?.setOnClickListener {
            toast("Speed: coming soon")
        }
        findViewById<Button>(R.id.btnExtractAudio)?.setOnClickListener {
            toast("Extract Audio: coming soon")
        }
    }

    private fun pickVideo() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQ_PICK_VIDEO)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_PICK_VIDEO && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            selectedVideo = copyToCache(uri)
            tvSelected.text = "Selected: ${selectedVideo?.name ?: "unknown"}"
            tvStatus.text = "Ready to edit"
        }
    }

    private fun copyToCache(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val outFile = File(cacheDir, "input_video_${System.currentTimeMillis()}.mp4")
            outFile.outputStream().use { out -> inputStream.copyTo(out) }
            inputStream.close()
            outFile
        } catch (e: Exception) {
            toast("Gagal copy: ${e.message}")
            null
        }
    }

    private fun toast(m: String) =
        Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
}
