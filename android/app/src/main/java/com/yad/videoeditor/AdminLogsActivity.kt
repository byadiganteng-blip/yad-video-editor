package com.yad.videoeditor

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AdminLogsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_logs)

        val tvLogs = findViewById<TextView>(R.id.tvLogs)
        val sdf = SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault())

        lifecycleScope.launch {
            FirebaseManager.logsFlow().collectLatest { logs ->
                if (logs.isEmpty()) {
                    tvLogs?.text = "Tidak ada log"
                    return@collectLatest
                }
                val sb = StringBuilder()
                for (log in logs) {
                    val action = log["action"] ?: ""
                    val detail = log["detail"] ?: ""
                    val ts = (log["timestamp"] as? Long) ?: 0L
                    sb.append("• ${sdf.format(Date(ts))}\n")
                    sb.append("  $action: $detail\n\n")
                }
                tvLogs?.text = sb.toString()
            }
        }
    }
}
