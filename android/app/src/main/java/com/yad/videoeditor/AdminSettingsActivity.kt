package com.yad.videoeditor

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

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
        val btnSync = findViewById<Button>(R.id.btnSyncFromFirebase)

        etToken?.setText(SecureConfig.getGithubToken())
        etLimit?.setText("3")

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
            lifecycleScope.launch {
                val ok = FirebaseManager.updateConfig("free_limit_per_day", limit)
                Toast.makeText(this@AdminSettingsActivity,
                    if (ok) "✅ Config tersimpan" else "❌ Gagal",
                    Toast.LENGTH_SHORT).show()
            }
        }

        btnSync?.setOnClickListener {
            SecureConfig.fetchTokenFromFirestore { ok ->
                if (ok) {
                    etToken?.setText(SecureConfig.getGithubToken())
                    Toast.makeText(this, "✅ Token di-sync",
                        Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "❌ Gagal sync",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
