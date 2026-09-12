package com.yad.videoeditor

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class AdminBackupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_backup)

        val tvInfo = findViewById<TextView>(R.id.tvBackupInfo)
        val btnBackup = findViewById<Button>(R.id.btnBackup)

        btnBackup?.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val sb = StringBuilder()
                    sb.append("=== BACKUP ${SimpleDateFormat("yyyy-MM-dd HH:mm",
                        Locale.getDefault()).format(Date())} ===\n\n")

                    // Users
                    val users = FirebaseManager.usersFlow().first()
                    sb.append("USERS (${users.size}):\n")
                    for ((id, u) in users) {
                        sb.append("  $id | ${u.deviceModel} | premium=${u.isPremium} | " +
                                  "daily=${u.dailyCount}\n")
                    }

                    // Stats
                    val stats = FirebaseManager.statsFlow().first()
                    sb.append("\nSTATS:\n")
                    stats?.forEach { (k, v) -> sb.append("  $k: $v\n") }

                    tvInfo?.text = sb.toString()
                    Toast.makeText(this@AdminBackupActivity,
                        "✅ Backup siap (copy manual)", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(this@AdminBackupActivity,
                        "❌ ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
