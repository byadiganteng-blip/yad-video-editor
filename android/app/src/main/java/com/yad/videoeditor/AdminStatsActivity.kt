package com.yad.videoeditor

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AdminStatsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { setContentView(R.layout.activity_admin_stats) }
        catch (e: Exception) { finish(); return }

        val tv = findViewById<TextView>(R.id.tvStats)

        lifecycleScope.launch {
            FirebaseManager.statsFlow().collectLatest { stats ->
                if (stats == null) return@collectLatest
                tv.text = """
                    📊 STATISTIK

                    Total User    : ${stats["total_users"] ?: 0}
                    Total Video   : ${stats["total_videos"] ?: 0}
                    Total Premium : ${stats["total_premium"] ?: 0}
                """.trimIndent()
            }
        }
    }
}
