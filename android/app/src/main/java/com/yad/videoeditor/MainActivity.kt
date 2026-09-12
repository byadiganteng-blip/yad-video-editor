package com.yad.videoeditor

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/**
 * Main Activity
 * Created by KARYADI, Coding by KARYADI
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)
        } catch (e: Exception) {
            Toast.makeText(this, "Layout error: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Credit
        findViewById<TextView>(R.id.tvCredit)?.text = "Created by KARYADI, Coding by KARYADI"

        // Menu buttons
        setupBtn(R.id.btnFiles) { start(FilesActivity::class.java) }
        setupBtn(R.id.btnActions) { start(ActionsActivity::class.java) }
        setupBtn(R.id.btnAdmin) { start(AdminActivity::class.java) }
        setupBtn(R.id.btnEditor) { start(VideoEditorActivity::class.java) }
        setupBtn(R.id.btnInstructions) { start(InstructionsActivity::class.java) }
        setupBtn(R.id.btnCredit) { start(CreditActivity::class.java) }
        setupBtn(R.id.btnStatistics) { start(StatisticsActivity::class.java) }
        setupBtn(R.id.btnSettings) { start(SettingsActivity::class.java) }

        // Login dialog
        if (!SecureConfig.isAdminLoggedIn()) {
            showLoginDialog()
        }
    }

    private fun setupBtn(id: Int, action: () -> Unit) {
        try {
            findViewById<Button>(id)?.setOnClickListener { action() }
        } catch (_: Exception) {}
    }

    private fun start(cls: Class<*>) {
        try {
            startActivity(Intent(this, cls))
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoginDialog() {
        try {
            val view = layoutInflater.inflate(R.layout.dialog_login, null)
            val etEmail = view.findViewById<EditText>(R.id.etEmail)
            val etToken = view.findViewById<EditText>(R.id.etToken)

            AlertDialog.Builder(this)
                .setTitle("Login Admin")
                .setView(view)
                .setCancelable(false)
                .setPositiveButton("Login") { _, _ ->
                    val email = etEmail.text.toString().trim()
                    val token = etToken.text.toString().trim()
                    if (email.isNotEmpty() && token.isNotEmpty()) {
                        SecureConfig.setAdminEmail(email)
                        SecureConfig.setGithubToken(token)
                        Toast.makeText(this, "Login berhasil", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Email & token wajib", Toast.LENGTH_SHORT).show()
                        showLoginDialog()
                    }
                }
                .setNegativeButton("Skip") { _, _ -> }
                .show()
        } catch (_: Exception) {}
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        try {
            AlertDialog.Builder(this)
                .setTitle("Keluar?")
                .setMessage("Tutup aplikasi?")
                .setPositiveButton("Ya") { _, _ -> finish() }
                .setNegativeButton("Batal", null)
                .show()
        } catch (_: Exception) {
            super.onBackPressed()
        }
    }
}
