package com.yad.videoeditor

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class AdminPanelActivity : AppCompatActivity() {

    private lateinit var tvInfo: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { setContentView(R.layout.activity_admin_panel) }
        catch (e: Exception) { finish(); return }

        if (!SecureConfig.isAdmin()) { finish(); return }

        tvInfo = findViewById(R.id.tvAdminInfo)
        tvInfo.text = "👑 ADMIN PANEL\n${SecureConfig.getAdminEmail()}"

        findViewById<Button>(R.id.btnAdminLogout)?.setOnClickListener {
            SecureConfig.clearAdmin()
            finish()
        }

        findViewById<Button>(R.id.btnAdminForceUpdate)?.setOnClickListener {
            lifecycleScope.launch {
                val ok = FirebaseManager.updateConfig("force_update", true)
                Toast.makeText(this@AdminPanelActivity,
                    if (ok) "✅ Force update ON" else "❌ Gagal",
                    Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnAdminMaintenance)?.setOnClickListener {
            lifecycleScope.launch {
                val ok = FirebaseManager.updateConfig("maintenance_mode", true)
                Toast.makeText(this@AdminPanelActivity,
                    if (ok) "✅ Maintenance ON" else "❌ Gagal",
                    Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnAdminToggleAds)?.setOnClickListener {
            lifecycleScope.launch {
                val ok = FirebaseManager.updateConfig("show_ads", false)
                Toast.makeText(this@AdminPanelActivity,
                    if (ok) "✅ Iklan OFF" else "❌ Gagal",
                    Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnAdminResetAll)?.setOnClickListener {
            lifecycleScope.launch {
                val ok = FirebaseManager.updateConfig("free_limit_per_day", 5)
                Toast.makeText(this@AdminPanelActivity,
                    if (ok) "✅ Reset semua limit" else "❌ Gagal",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }
}
