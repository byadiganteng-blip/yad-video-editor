package com.yad.videoeditor

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class TokenSetupActivity : AppCompatActivity() {
    companion object { private const val REQ_PICK_TXT = 2001 }

    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_token_setup)

        SecureConfig.init(this)
        tvStatus = findViewById(R.id.tvTokenStatus)

        updateStatus()

        findViewById<Button>(R.id.btnUploadTokenTxt).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "text/plain"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(Intent.createChooser(intent, "Pilih token.txt"), REQ_PICK_TXT)
        }

        findViewById<Button>(R.id.btnClearToken).setOnClickListener {
            SecureConfig.clearGithubToken()
            updateStatus()
            Toast.makeText(this, "Token upload dihapus (fallback ke embed)", Toast.LENGTH_LONG).show()
        }

        findViewById<Button>(R.id.btnBackFromToken).setOnClickListener { finish() }
    }

    private fun updateStatus() {
        val token = SecureConfig.getGithubToken()
        val source = SecureConfig.getTokenSource()
        val sourceText = when (source) {
            "upload_txt" -> "📁 Dari upload txt"
            "embedded" -> "🔒 Dari embed (BuildConfig)"
            else -> "❌ Tidak ada"
        }

        tvStatus.text = if (SecureConfig.hasGithubToken()) {
            "✅ Token aktif\n" +
            "Sumber: $sourceText\n" +
            "Token: ${token.take(12)}...${token.takeLast(4)}"
        } else {
            "❌ Belum ada token\n\nUpload file .txt berisi token GitHub Anda"
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_PICK_TXT && resultCode == Activity.RESULT_OK) {
            val uri: Uri = data?.data ?: return
            val token = SecureConfig.readTokenFromUri(this, uri)
            if (token != null) {
                Toast.makeText(this, "Token dari txt berhasil dibaca ✅", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "File tidak berisi token valid ❌", Toast.LENGTH_LONG).show()
            }
            updateStatus()
        }
    }
}
