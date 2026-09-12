package com.yad.videoeditor

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Admin Panel — hanya bisa diakses setelah login
 * Email : ynuraini686@gmail.com
 * Pass  : YADIGANTENG
 */
class AdminPanelActivity : AppCompatActivity() {

    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { setContentView(R.layout.activity_admin_panel) }
        catch (e: Exception) { finish(); return }

        // Cek login dulu
        if (!SecureConfig.isAdmin()) {
            showLoginDialog()
        } else {
            showAdminPanel()
        }
    }

    private fun showLoginDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 30)
        }
        val etEmail = EditText(this).apply {
            hint = "Email admin"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        val etPass = EditText(this).apply {
            hint = "Kata sandi"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        container.addView(etEmail)
        container.addView(etPass)

        AlertDialog.Builder(this)
            .setTitle("🔐 Admin Login")
            .setMessage("Masukkan kredensial admin")
            .setView(container)
            .setCancelable(false)
            .setPositiveButton("Login") { _, _ ->
                val email = etEmail.text.toString().trim()
                val pass = etPass.text.toString()
                if (SecureConfig.verifyAdminCredentials(email, pass)) {
                    Toast.makeText(this, "✅ Login berhasil", Toast.LENGTH_SHORT).show()
                    showAdminPanel()
                } else {
                    Toast.makeText(this, "❌ Email atau sandi salah", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            .setNegativeButton("Batal") { _, _ -> finish() }
            .show()
    }

    private fun showAdminPanel() {
        val tvInfo = findViewById<TextView>(R.id.tvAdminInfo)
        val tvWelcome = findViewById<TextView>(R.id.tvAdminWelcome)
        val btnLogout = findViewById<Button>(R.id.btnAdminLogout)

        tvWelcome.text = "👑 ADMIN PANEL\nKARYADI Control Center"
        tvInfo.text = "Status: LOGGED IN\nEmail: ${SecureConfig.getAdminEmail()}\n\nMemuat data..."

        btnLogout.setOnClickListener {
            SecureConfig.clearAdmin()
            Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Load workflow status
        scope.launch {
            try {
                val (ok, runs) = AdminApi.listRuns(10)
                if (ok && runs.isNotEmpty()) {
                    val sb = StringBuilder()
                    sb.append("Status: LOGGED IN\n")
                    sb.append("Email: ${SecureConfig.getAdminEmail()}\n\n")
                    sb.append("📊 WORKFLOW TERBARU:\n\n")
                    for (r in runs.take(5)) {
                        sb.append("• ${r.name}\n")
                        sb.append("  Status: ${r.status}/${r.conclusion}\n")
                        sb.append("  Run #${r.runNumber}\n\n")
                    }
                    tvInfo.text = sb.toString()
                } else {
                    tvInfo.text = "Status: LOGGED IN\n\nTidak ada workflow run."
                }
            } catch (e: Exception) {
                tvInfo.text = "Status: LOGGED IN\n\nError: ${e.message}"
            }
        }
    }
}
