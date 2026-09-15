package com.yad.videoeditor

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.yad.videoeditor.databinding.ActivityAdminBinding
import kotlinx.coroutines.launch

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Header info
        binding.tvAdminEmail.text = "📧 " + (SecureConfig.getAdminEmail() ?: "admin@yad.com")
        binding.tvAdminLogin.text = "🕐 Login: " + formatNow()

        // === LOAD STATISTIK REAL-TIME ===
        loadStatistics()

        // === TOMBOL KONTROL UTAMA ===
        binding.cardBroadcast.setOnClickListener {
            startActivity(Intent(this, AdminBroadcastActivity::class.java))
        }
        binding.cardPushUpdate.setOnClickListener {
            Toast.makeText(this, "Fitur Push Update", Toast.LENGTH_SHORT).show()
        }
        binding.cardForceUpdate.setOnClickListener {
            FirebaseManager.updateConfigAsync("force_update", true) { ok ->
                Toast.makeText(this,
                    if (ok) "✅ Force update aktif" else "❌ Gagal",
                    Toast.LENGTH_SHORT).show()
            }
        }

        // === DATA & MONITORING ===
        binding.cardUserList.setOnClickListener {
            startActivity(Intent(this, AdminUserListActivity::class.java))
        }
        binding.cardDeviceList.setOnClickListener {
            startActivity(Intent(this, AdminUserListActivity::class.java))
        }
        binding.cardLogs.setOnClickListener {
            startActivity(Intent(this, AdminLogsActivity::class.java))
        }
        binding.cardStats.setOnClickListener {
            startActivity(Intent(this, AdminStatsActivity::class.java))
        }

        // === KONFIGURASI ===
        binding.cardTokens.setOnClickListener {
            startActivity(Intent(this, AdminSettingsActivity::class.java))
        }
        binding.cardBackup.setOnClickListener {
            startActivity(Intent(this, AdminBackupActivity::class.java))
        }
        binding.cardSettings.setOnClickListener {
            startActivity(Intent(this, AdminSettingsActivity::class.java))
        }

        // === LOGOUT ===
        binding.btnLogout.setOnClickListener {
            FirebaseManager.logout()
            startActivity(Intent(this, SplashActivity::class.java))
            finish()
        }
    }

    private fun loadStatistics() {
        lifecycleScope.launch {
            FirebaseManager.getTotalUserCount { count ->
                runOnUiThread { binding.tvStatUsers.text = count.toString() }
            }
            FirebaseManager.getActiveDeviceCount { count ->
                runOnUiThread { binding.tvStatActive.text = count.toString() }
            }
            FirebaseManager.getPremiumUserCount { count ->
                runOnUiThread { binding.tvStatPremium.text = count.toString() }
            }
            FirebaseManager.getGenerationTodayCount { count ->
                runOnUiThread { binding.tvStatToday.text = count.toString() }
            }
        }
    }

    private fun formatNow(): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }
}
