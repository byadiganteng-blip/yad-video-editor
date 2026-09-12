package com.yad.videoeditor

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Admin Panel
 * Created by KARYADI, Coding by KARYADI
 */
class AdminActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_admin)
        } catch (e: Exception) {
            finish(); return
        }

        findViewById<TextView>(R.id.tvAdminEmail)?.text = "Email: ${SecureConfig.getAdminEmail()}"
        findViewById<TextView>(R.id.tvAdminLogin)?.text =
            "Login: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}"

        setupBtn(R.id.btnPushUpdate) {
            lifecycleScope.launch {
                val (ok, msg) = AdminApi.triggerBuild("build-apk.yml", "main")
                Toast.makeText(this@AdminActivity, if (ok) "OK: $msg" else "FAIL: $msg",
                    Toast.LENGTH_LONG).show()
            }
        }

        setupBtn(R.id.btnForceUpdate) {
            AlertDialog.Builder(this)
                .setTitle("Force Update")
                .setMessage("Trigger build ulang?")
                .setPositiveButton("Ya") { _, _ ->
                    lifecycleScope.launch {
                        val (ok, _) = AdminApi.triggerBuild("build-apk.yml", "main")
                        Toast.makeText(this@AdminActivity,
                            if (ok) "Build dipicu" else "Gagal", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        setupBtn(R.id.btnManageTokens) { showTokenDialog() }

        setupBtn(R.id.btnViewLogs) {
            lifecycleScope.launch {
                val logs = AdminApi.getLatestLogs()
                AlertDialog.Builder(this)
                    .setTitle("Latest Logs")
                    .setMessage(logs)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }

        setupBtn(R.id.btnViewDevices) {
            val info = "Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n" +
                       "Android: ${android.os.Build.VERSION.RELEASE}"
            AlertDialog.Builder(this)
                .setTitle("Device Info")
                .setMessage(info)
                .setPositiveButton("OK", null)
                .show()
        }

        setupBtn(R.id.btnBroadcast) {
            val input = EditText(this)
            input.hint = "Pesan broadcast"
            AlertDialog.Builder(this)
                .setTitle("Broadcast")
                .setView(input)
                .setPositiveButton("Kirim") { _, _ ->
                    Toast.makeText(this, "Broadcast terkirim", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        setupBtn(R.id.btnLogout) {
            SecureConfig.clearAdmin()
            finish()
        }
    }

    private fun setupBtn(id: Int, action: () -> Unit) {
        try {
            findViewById<Button>(id)?.setOnClickListener { action() }
        } catch (_: Exception) {}
    }

    private fun showTokenDialog() {
        val input = EditText(this)
        input.hint = "ghp_..."
        input.setText(SecureConfig.getGithubToken())
        AlertDialog.Builder(this)
            .setTitle("Manage Token")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val t = input.text.toString().trim()
                if (t.isNotEmpty()) SecureConfig.setGithubToken(t)
                Toast.makeText(this, "Tersimpan", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
