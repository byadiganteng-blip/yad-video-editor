package com.yad.videoeditor

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvBanner: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SecureConfig.init(this)
        try { setContentView(R.layout.activity_main) }
        catch (e: Exception) { finish(); return }

        if (!SecureConfig.isAdmin()) {
            showLoginDialog()
        } else {
            showAdminMenu()
        }
    }

    private fun showLoginDialog() {
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 30, 50, 30)
        }
        val etEmail = android.widget.EditText(this).apply {
            hint = "Email admin"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        val etPass = android.widget.EditText(this).apply {
            hint = "Kata sandi"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        container.addView(etEmail)
        container.addView(etPass)

        AlertDialog.Builder(this)
            .setTitle("🔐 Admin Login")
            .setView(container)
            .setCancelable(false)
            .setPositiveButton("Login") { _, _ ->
                if (SecureConfig.verifyAdminCredentials(
                        etEmail.text.toString(),
                        etPass.text.toString())) {
                    Toast.makeText(this, "✅ Login berhasil",
                        Toast.LENGTH_SHORT).show()
                    showAdminMenu()
                } else {
                    Toast.makeText(this, "❌ Salah", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            .setNegativeButton("Batal") { _, _ -> finish() }
            .show()
    }

    private fun showAdminMenu() {
        tvStatus = findViewById(R.id.tvStatus)
        tvBanner = findViewById(R.id.tvBanner)

        findViewById<TextView>(R.id.tvCredit)?.text =
            "Login: ${SecureConfig.getAdminEmail()}"

        // MANAJEMEN USER
        clickCard(R.id.cardAdminUserList) { start(AdminUserListActivity::class.java) }
        clickCard(R.id.cardAdminStats) { start(AdminStatsActivity::class.java) }

        // KOMUNIKASI
        clickCard(R.id.cardAdminBroadcast) { start(AdminBroadcastActivity::class.java) }
        clickCard(R.id.cardAdminPushNotif) {
            Toast.makeText(this, "Push Notif — coming soon", Toast.LENGTH_SHORT).show()
        }

        // KONTROL APK
        clickCard(R.id.cardAdminForceUpdate) { toggleForceUpdate() }
        clickCard(R.id.cardAdminMaintenance) { toggleMaintenance() }

        // KONFIGURASI
        clickCard(R.id.cardAdminToken) { start(AdminSettingsActivity::class.java) }
        clickCard(R.id.cardAdminModels) {
            Toast.makeText(this, "Manage Model — coming soon", Toast.LENGTH_SHORT).show()
        }
        clickCard(R.id.cardAdminConfig) { start(AdminSettingsActivity::class.java) }

        // DATA & LOG
        clickCard(R.id.cardAdminLogs) {
            Toast.makeText(this, "Log — coming soon", Toast.LENGTH_SHORT).show()
        }
        clickCard(R.id.cardAdminBackup) {
            Toast.makeText(this, "Backup — coming soon", Toast.LENGTH_SHORT).show()
        }

        // LAINNYA
        clickCard(R.id.cardInstructions) { start(InstructionsActivity::class.java) }
        clickCard(R.id.cardAdminLogout) {
            SecureConfig.clearAdmin()
            Toast.makeText(this, "Logout", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Listen stats
        lifecycleScope.launch {
            FirebaseManager.statsFlow().collectLatest { stats ->
                if (stats != null) {
                    tvBanner?.text = "📊 Users: ${stats["total_users"] ?: 0} | " +
                                     "Videos: ${stats["total_videos"] ?: 0} | " +
                                     "Premium: ${stats["total_premium"] ?: 0}"
                    tvBanner?.visibility = View.VISIBLE
                }
            }
        }
        lifecycleScope.launch {
            FirebaseManager.configFlow().collectLatest { cfg ->
                tvStatus?.text = "🔄 Force: ${cfg.forceUpdate} | " +
                                 "🛠️ Maintenance: ${cfg.maintenanceMode} | " +
                                 "📢 Ads: ${cfg.showAds}"
            }
        }
    }

    private fun toggleForceUpdate() {
        lifecycleScope.launch {
            try {
                FirebaseManager.updateConfig("force_update", true) { ok ->
                    Toast.makeText(this@MainActivity,
                        if (ok) "✅ Force Update ON" else "❌ Gagal",
                        Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleMaintenance() {
        AlertDialog.Builder(this)
            .setTitle("Maintenance Mode")
            .setMessage("Aktifkan mode maintenance? Client tidak bisa akses.")
            .setPositiveButton("Aktifkan") { _, _ ->
                FirebaseManager.updateConfig("maintenance_mode", true) { ok ->
                    Toast.makeText(this@MainActivity,
                        if (ok) "✅ Maintenance ON" else "❌ Gagal",
                        Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun clickCard(id: Int, action: () -> Unit) {
        try {
            findViewById<View>(id)?.setOnClickListener {
                try { action() }
                catch (e: Exception) {
                    Toast.makeText(this, "Error: ${e.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
        } catch (_: Exception) {}
    }

    private fun start(cls: Class<*>) { startActivity(Intent(this, cls)) }
}
