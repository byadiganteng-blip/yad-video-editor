package com.yad.videoeditor

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
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
            FirebaseManager.updateConfig("force_update", true)
            Toast.makeText(this, "✅ Force update ON", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnAdminMaintenance)?.setOnClickListener {
            FirebaseManager.updateConfig("maintenance_mode", true)
            Toast.makeText(this, "✅ Maintenance ON", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnAdminToggleAds)?.setOnClickListener {
            FirebaseManager.updateConfig("show_ads", false)
            Toast.makeText(this, "✅ Iklan OFF", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnAdminResetAll)?.setOnClickListener {
            FirebaseManager.updateConfig("free_limit_per_day", 5)
            Toast.makeText(this, "✅ Reset semua limit", Toast.LENGTH_SHORT).show()
        }
    }
}
