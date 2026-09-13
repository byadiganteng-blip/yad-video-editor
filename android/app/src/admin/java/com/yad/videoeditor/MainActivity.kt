package com.yad.videoeditor

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

/**
 * MainActivity Admin — panel kontrol YAD Admin.
 *
 * Navigate ke activity admin yang sudah ada di src/main/.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ============================================================
        //  CHECK ADMIN STATUS
        // ============================================================
        if (!SecureConfig.isAdmin()) {
            // Belum login sebagai admin → buka AdminActivity untuk login
            try {
                startActivity(Intent(this, AdminActivity::class.java))
                finish()
                return
            } catch (e: Exception) {
                Toast.makeText(this, "Bukan admin: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        // ============================================================
        //  CARDS — navigate ke activity asli
        // ============================================================
        bindCard(R.id.cardAdminUserList, AdminUserListActivity::class.java)
        bindCard(R.id.cardAdminStats, AdminStatsActivity::class.java)
        bindCard(R.id.cardAdminBroadcast, AdminBroadcastActivity::class.java)
        bindCard(R.id.cardAdminPushNotif, AdminPushNotifActivity::class.java)
        bindCard(R.id.cardAdminModels, AdminModelsActivity::class.java)
        bindCard(R.id.cardAdminLogs, AdminLogsActivity::class.java)
        bindCard(R.id.cardAdminBackup, AdminBackupActivity::class.java)
        bindCard(R.id.cardInstructions, InstructionsActivity::class.java)

        // Card khusus: buka AdminPanelActivity (panel utama)
        findViewById<CardView>(R.id.cardAdminConfig)?.setOnClickListener {
            try {
                startActivity(Intent(this, AdminPanelActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(this, "Panel: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Card khusus: Set Token — buka SettingsActivity
        findViewById<CardView>(R.id.cardAdminToken)?.setOnClickListener {
            try {
                startActivity(Intent(this, SettingsActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(this, "Settings: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Card khusus: Force Update — buka AdminSettingsActivity
        findViewById<CardView>(R.id.cardAdminForceUpdate)?.setOnClickListener {
            try {
                startActivity(Intent(this, AdminSettingsActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(this, "Settings: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Card khusus: Maintenance — buka AdminSettingsActivity juga
        findViewById<CardView>(R.id.cardAdminMaintenance)?.setOnClickListener {
            try {
                startActivity(Intent(this, AdminSettingsActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(this, "Settings: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Card: Logout
        findViewById<CardView>(R.id.cardAdminLogout)?.setOnClickListener {
            SecureConfig.clearAdmin()
            Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    // ============================================================
    //  HELPER: Bind card ke activity
    // ============================================================
    private fun bindCard(cardId: Int, targetClass: Class<*>) {
        try {
            findViewById<CardView>(cardId)?.setOnClickListener {
                try {
                    startActivity(Intent(this, targetClass))
                } catch (e: Exception) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            // Card tidak ada — skip
        }
    }
}
