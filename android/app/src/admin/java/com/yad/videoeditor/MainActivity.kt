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
    private var isForceUpdateOn = false
    private var isMaintenanceOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SecureConfig.init(this)
        try { setContentView(R.layout.activity_main)
        // Admin panel tidak perlu iklan — hindari risiko suspend AdMob
        try {
            val bannerAd = findViewById<android.view.View>(R.id.bannerAd)
            bannerAd?.visibility = android.view.View.GONE
        } catch (e: Exception) {
            // Banner tidak ada di admin layout — aman
        }
        }
        catch (e: Exception) { finish(); return }

        if (!SecureConfig.isAdmin()) showLoginDialog()
        else showAdminMenu()
    }

    private fun showLoginDialog() {
        val c = android.widget.LinearLayout(this).apply {
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
        c.addView(etEmail); c.addView(etPass)

        AlertDialog.Builder(this)
            .setTitle("🔐 Admin Login")
            .setView(c)
            .setCancelable(false)
            .setPositiveButton("Login") { _, _ ->
                if (SecureConfig.verifyAdminCredentials(
                        etEmail.text.toString(), etPass.text.toString())) {
                    Toast.makeText(this, "✅ Login berhasil", Toast.LENGTH_SHORT).show()
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
        clickCard(R.id.cardAdminPushNotif) { start(AdminPushNotifActivity::class.java) }

        // KONTROL APK
        clickCard(R.id.cardAdminForceUpdate) { toggleForceUpdate() }
        clickCard(R.id.cardAdminMaintenance) { toggleMaintenance() }

        // KONFIGURASI
        clickCard(R.id.cardAdminToken) { start(AdminSettingsActivity::class.java) }
        clickCard(R.id.cardAdminModels) { start(AdminModelsActivity::class.java) }
        clickCard(R.id.cardAdminConfig) { start(AdminSettingsActivity::class.java) }

        // DATA & LOG
        clickCard(R.id.cardAdminLogs) { start(AdminLogsActivity::class.java) }
        clickCard(R.id.cardAdminBackup) { start(AdminBackupActivity::class.java) }

        // LAINNYA
        clickCard(R.id.cardInstructions) { start(InstructionsActivity::class.java) }
        clickCard(R.id.cardAdminLogout) { logout() }

        // Listen config
        lifecycleScope.launch {
            FirebaseManager.configFlow().collectLatest { cfg ->
                isForceUpdateOn = cfg.forceUpdate
                isMaintenanceOn = cfg.maintenanceMode
                tvStatus?.text = "🔄 Force: ${if (cfg.forceUpdate) "ON" else "OFF"} | " +
                                 "🛠️ Maint: ${if (cfg.maintenanceMode) "ON" else "OFF"} | " +
                                 "📢 Ads: ${if (cfg.showAds) "ON" else "OFF"}"
            }
        }
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
    }

    private fun toggleForceUpdate() {
        val newVal = !isForceUpdateOn
        lifecycleScope.launch {
            val ok = FirebaseManager.updateConfig("force_update", newVal)
            if (ok) {
                isForceUpdateOn = newVal
                Toast.makeText(this@MainActivity,
                    "✅ Force Update ${if (newVal) "ON" else "OFF"}",
                    Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "❌ Gagal", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleMaintenance() {
        val newVal = !isMaintenanceOn
        AlertDialog.Builder(this)
            .setTitle("Maintenance Mode")
            .setMessage("${if (newVal) "Aktifkan" else "Matikan"} maintenance?")
            .setPositiveButton("Ya") { _, _ ->
                lifecycleScope.launch {
                    val ok = FirebaseManager.updateConfig("maintenance_mode", newVal)
                    if (ok) {
                        isMaintenanceOn = newVal
                        Toast.makeText(this@MainActivity,
                            "✅ Maintenance ${if (newVal) "ON" else "OFF"}",
                            Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity,
                            "❌ Gagal", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun logout() {
        SecureConfig.clearAdmin()
        Toast.makeText(this, "Logout", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun clickCard(id: Int, action: () -> Unit) {
        try {
            findViewById<View>(id)?.setOnClickListener {
                try { action() }
                catch (e: Exception) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (_: Exception) {}
    }

    private fun start(cls: Class<*>) { startActivity(Intent(this, cls)) }
}
