package com.yad.videoeditor

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class AdminSettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { setContentView(R.layout.activity_admin_settings) }
        catch (e: Exception) { finish(); return }

        if (!SecureConfig.isAdmin()) { finish(); return }

        val etToken = findViewById<EditText>(R.id.etAdminToken)
        val etLimit = findViewById<EditText>(R.id.etFreeLimit)
        val btnSaveToken = findViewById<Button>(R.id.btnSaveToken)
        val btnSaveConfig = findViewById<Button>(R.id.btnSaveConfig)

        etToken?.setText(SecureConfig.getGithubToken())

        btnSaveToken?.setOnClickListener {
            val t = etToken?.text?.toString()?.trim() ?: ""
            if (t.startsWith("ghp_") && t.length > 20) {
                FirebaseManager.saveGithubToken(t) { ok ->
                    if (ok) {
                        SecureConfig.setGithubToken(t)
                        Toast.makeText(this, "✅ Token disimpan",
                            Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "❌ Gagal simpan",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "❌ Token tidak valid",
                    Toast.LENGTH_SHORT).show()
            }
        }

        btnSaveConfig?.setOnClickListener {
            val limit = etLimit?.text?.toString()?.toIntOrNull() ?: 3
            FirebaseManager.updateConfig("free_limit_per_day", limit) { ok ->
                if (ok) Toast.makeText(this, "✅ Config tersimpan",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }
}
