package com.yad.videoeditor

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class StatisticsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { setContentView(R.layout.activity_statistics) }
        catch (e: Exception) { finish(); return }

        val tv = findViewById<TextView>(R.id.tvStatistics)

        lifecycleScope.launch {
            FirebaseManager.statsFlow().collectLatest { stats ->
                if (stats == null) {
                    tv.text = "Memuat statistik..."
                    return@collectLatest
                }
                tv.text = """
                    STATISTIK APLIKASI

                    Total User     : ${stats["total_users"] ?: 0}
                    Total Video    : ${stats["total_videos"] ?: 0}
                    Total Premium  : ${stats["total_premium"] ?: 0}
                    Hari Ini       : ${stats["total_today"] ?: 0}
                    Total Credits  : ${stats["total_credits"] ?: 0}
                """.trimIndent()
            }
        }
    }
}
