package com.yad.videoeditor

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class AdminSettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { setContentView(R.layout.activity_admin_settings) }
        catch (e: Exception) { finish(); return }

        val etLimit = findViewById<EditText>(R.id.etFreeLimit)
        findViewById<Button>(R.id.btnSaveConfig)?.setOnClickListener {
            val limit = etLimit.text.toString().toIntOrNull() ?: 3
            FirebaseManager.updateConfig("free_limit_per_day", limit)
            Toast.makeText(this, "✅ Tersimpan", Toast.LENGTH_SHORT).show()
        }
    }
}
