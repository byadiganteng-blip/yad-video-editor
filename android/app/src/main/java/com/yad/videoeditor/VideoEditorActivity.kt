package com.yad.videoeditor

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Video Editor - Trim, Rotate, Speed, Extract Audio
 * Created by KARYADI, Coding by KARYADI
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
        findViewById<Button>(R.id.btnTrim)?.setOnClickListener { trimVideo() }
        findViewById<Button>(R.id.btnRotate)?.setOnClickListener { rotateVideo() }
        findViewById<Button>(R.id.btnSpeed)?.setOnClickListener { speedVideo() }
        findViewById<Button>(R.id.btnExtractAudio)?.setOnClickListener { extractAudio() }
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
            tvSelected.text = "Selected: ${selectedVideo?.name}"
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
            Toast.makeText(this, "Gagal copy: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun outputFile(name: String): File {
        val dir = File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_MOVIES), "YadEditor")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, name)
    }

    private fun ts(): String =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

    private fun runFFmpeg(cmd: Array<String>, output: File) {
        val input = selectedVideo ?: run {
            Toast.makeText(this, "Pilih video dulu", Toast.LENGTH_SHORT).show()
            return
        }
        tvStatus.text = "Processing..."
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val session = FFmpegKit.execute(cmd.joinToString(" ")
                        .split(" ").toTypedArray())
                    val success = ReturnCode.isSuccess(session.returnCode)
                    success to (session.allLogsAsString ?: "")
                } catch (e: Exception) {
                    false to "Error: ${e.message}"
                }
            }
            tvStatus.text = if (result.first) "Done: ${output.name}"
                else "Failed: ${result.second.takeLast(200)}"
        }
    }

    private fun trimVideo() {
        val input = selectedVideo ?: return
        val output = outputFile("trim_${ts()}.mp4")
        runFFmpeg(arrayOf(
            "-y", "-i", input.absolutePath,
            "-ss", "5", "-t", "10",
            "-c", "copy", output.absolutePath
        ), output)
    }

    private fun rotateVideo() {
        val input = selectedVideo ?: return
        val output = outputFile("rotate_${ts()}.mp4")
        runFFmpeg(arrayOf(
            "-y", "-i", input.absolutePath,
            "-vf", "transpose=1",
            "-c:a", "copy", output.absolutePath
        ), output)
    }

    private fun speedVideo() {
        val input = selectedVideo ?: return
        val output = outputFile("speed_${ts()}.mp4")
        runFFmpeg(arrayOf(
            "-y", "-i", input.absolutePath,
            "-filter_complex", "[0:v]setpts=0.5*PTS[v];[0:a]atempo=2.0[a]",
            "-map", "[v]", "-map", "[a]",
            output.absolutePath
        ), output)
    }

    private fun extractAudio() {
        val input = selectedVideo ?: return
        val output = outputFile("audio_${ts()}.mp3")
        runFFmpeg(arrayOf(
            "-y", "-i", input.absolutePath,
            "-vn", "-acodec", "copy", output.absolutePath
        ), output)
    }
}
