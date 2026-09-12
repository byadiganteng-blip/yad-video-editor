package com.yad.videoeditor

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class AdminPanelActivity : AppCompatActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var btnLogout: Button
    private lateinit var layoutAdminContent: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { setContentView(R.layout.activity_admin_panel) }
        catch (e: Exception) { finish(); return }

        tvWelcome = findViewById(R.id.tvAdminWelcome)
        btnLogout = findViewById(R.id.btnAdminLogout)
        layoutAdminContent = findViewById(R.id.layoutAdminContent)

        if (!SecureConfig.isAdmin()) {
            showLoginDialog()
        } else {
            showAdminContent()
        }

        btnLogout.setOnClickListener {
            SecureConfig.clearAdmin()
            Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun showLoginDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 10)
        }

        val etEmail = EditText(this).apply {
            hint = "Email admin"
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        val etPass = EditText(this).apply {
            hint = "Kata sandi"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        layout.addView(etEmail)
        layout.addView(etPass)

        val dialog = AlertDialog.Builder(this)
            .setTitle("🔐 Admin Panel")
            .setMessage("Masukkan email & kata sandi admin")
            .setView(layout)
            .setCancelable(false)
            .setPositiveButton("MASUK", null)
            .setNegativeButton("BATAL") { _, _ -> finish() }
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val email = etEmail.text.toString().trim()
                val pass = etPass.text.toString()
                if (SecureConfig.verifyAdminCredentials(email, pass)) {
                    Toast.makeText(this, "✅ Selamat datang, Admin!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    showAdminContent()
                } else {
                    Toast.makeText(this, "❌ Email atau kata sandi salah", Toast.LENGTH_LONG).show()
                    etPass.text.clear()
                }
            }
        }
        dialog.show()
    }

    private fun showAdminContent() {
        layoutAdminContent.visibility = View.VISIBLE
        tvWelcome.text = "👑 Admin: ${SecureConfig.getAdminEmail()}"

        findViewById<TextView>(R.id.tvAdminInfo).text = buildString {
            append("═══════════════════════\n")
            append("📱 App Info\n")
            append("═══════════════════════\n")
            append("Version : ${BuildConfig.VERSION_NAME}\n")
            append("Code    : ${BuildConfig.VERSION_CODE}\n")
            append("Credit  : ${SecureConfig.getCredit()}\n\n")
            append("═══════════════════════\n")
            append("🔑 GitHub Config\n")
            append("═══════════════════════\n")
            append("User    : ${SecureConfig.getGithubUser()}\n")
            append("Repo    : ${SecureConfig.getGithubRepo()}\n")
            append("Token   : ${if (SecureConfig.hasGithubToken()) "✅ Aktif" else "❌ Kosong"}\n")
        }
    }
}
