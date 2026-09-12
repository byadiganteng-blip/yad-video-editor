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
import java.text.SimpleDateFormat
import java.util.*

/**
 * ADMIN MENU — hanya untuk APK admin (com.yad.videoeditor.admin)
 * Fokus: control panel, lihat user, broadcast, setting.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvStats: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SecureConfig.init(this)
        try { setContentView(R.layout.activity_main) }
        catch (e: Exception) { finish(); return }

        // Login admin
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
        tvStats = findViewById(R.id.tvBanner)

        // Sembunyikan card client yang tidak perlu
        hideCard(R.id.cardVideoList)
        hideCard(R.id.cardEditor)
        hideCard(R.id.cardAiVideo)
        hideCard(R.id.cardFiles)
        hideCard(R.id.cardCredit)

        // Menu admin
        clickCard(R.id.cardInstructions) { start(AdminUserListActivity::class.java) }
        clickCard(R.id.cardStatistics) { start(AdminStatsActivity::class.java) }

        // Set judul
        try {
            findViewById<TextView>(R.id.tvAppTitle)?.text = "YAD Admin"
            findViewById<TextView>(R.id.tvCredit)?.text =
                "Login: ${SecureConfig.getAdminEmail()}"
        } catch (_: Exception) {}

        // Listen stats
        lifecycleScope.launch {
            FirebaseManager.statsFlow().collectLatest { stats ->
                if (stats != null) {
                    tvStats?.text = "📊 Users: ${stats["total_users"] ?: 0} | " +
                                    "Videos: ${stats["total_videos"] ?: 0}"
                    tvStats?.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun hideCard(id: Int) {
        try { findViewById<View>(id)?.visibility = View.GONE } catch (_: Exception) {}
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

    override fun onResume() {
        super.onResume()
        // Refresh stats
    }
}
