package com.yad.videoeditor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private var headerTapCount = 0
    private var lastTapTime = 0L
    private val REQUIRED_TAPS = 5
    private val TAP_INTERVAL = 500L
    private val PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SecureConfig.init(this)
        try {
            setContentView(R.layout.activity_main)
        } catch (e: Exception) {
            finish()
            return
        }

        try {
            findViewById<TextView>(R.id.tvCredit)?.text =
                "Created by KARYADI, Coding by KARYADI"
        } catch (_: Exception) {}

        // Menu utama
        clickCard(R.id.cardVideoList) { start(VideoListActivity::class.java) }
        clickCard(R.id.cardEditor) { start(VideoEditorActivity::class.java) }
        clickCard(R.id.cardAiVideo) { start(TextToVideoActivity::class.java) }
        clickCard(R.id.cardFiles) { start(FilesActivity::class.java) }
        clickCard(R.id.cardInstructions) { start(InstructionsActivity::class.java) }
        clickCard(R.id.cardCredit) { start(CreditActivity::class.java) }
        clickCard(R.id.cardStatistics) { start(StatisticsActivity::class.java) }
        // Tombol Pengaturan DIHAPUS

        // Tap 5x header → Admin Panel (login)
        setupAdminTap()

        requestAllPermissions()
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

    private fun start(cls: Class<*>) {
        startActivity(Intent(this, cls))
    }

    private fun setupAdminTap() {
        val header = findViewById<View>(R.id.headerLayout) ?: return
        header.isClickable = true
        header.setOnClickListener {
            val count = SecureConfig.incrementTapCount()
            val remaining = 5 - count
            if (remaining in 1..4) {
                Toast.makeText(this, "Tap $remaining kali lagi...",
                    Toast.LENGTH_SHORT).show()
            } else if (count == 5) {
                SecureConfig.resetTapCount()
                startActivity(Intent(this, AdminPanelActivity::class.java))
            } else if (count > 5) {
                SecureConfig.resetTapCount()
            }
        }
    }

    private fun requestAllPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) !=
                PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this, notGranted.toTypedArray(), PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
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
        } catch (_: Exception) { super.onBackPressed() }
    }
}
