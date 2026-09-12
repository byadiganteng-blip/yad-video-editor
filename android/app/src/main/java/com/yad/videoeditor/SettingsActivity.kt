package com.yad.videoeditor

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { setContentView(R.layout.activity_settings) }
        catch (e: Exception) { finish(); return }

        val etToken = findViewById<EditText>(R.id.etGithubToken)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnClear = findViewById<Button>(R.id.btnClear)
        val tvInfo = findViewById<TextView>(R.id.tvInfo)

        etToken?.setText(SecureConfig.getGithubToken())
        tvInfo?.text = "App version: ${BuildConfig.VERSION_NAME}\n" +
                "Created by KARYADI"

        btnSave?.setOnClickListener {
            val t = etToken?.text?.toString()?.trim() ?: ""
            if (t.isNotEmpty()) {
                SecureConfig.setGithubToken(t)
                Toast.makeText(this, "✅ Tersimpan", Toast.LENGTH_SHORT).show()
            }
        }

        btnClear?.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Hapus token?")
                .setPositiveButton("Hapus") { _, _ ->
                    SecureConfig.clearGithubToken()
                    etToken?.setText("")
                    Toast.makeText(this, "Token dihapus", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }
}
