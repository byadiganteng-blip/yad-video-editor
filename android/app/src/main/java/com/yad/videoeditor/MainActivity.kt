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

/**
 * Main Activity - dengan mode admin tersembunyi + auto permission
 * Created by KARYADI, Coding by KARYADI
 */
class MainActivity : AppCompatActivity() {

    private var headerTapCount = 0
    private var lastTapTime = 0L
    private val REQUIRED_TAPS = 5
    private val TAP_INTERVAL = 500L
    private val PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)
        } catch (e: Exception) {
            Toast.makeText(this, "Layout error: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        findViewById<TextView>(R.id.tvCredit)?.text = "Created by KARYADI, Coding by KARYADI"

        // FIX: Setup click listener di CardView (bukan TextView panah)
        clickCard(R.id.cardVideoList) { start(VideoListActivity::class.java) }
        clickCard(R.id.cardEditor) { start(VideoEditorActivity::class.java) }
        clickCard(R.id.cardFiles) { start(FilesActivity::class.java) }
        clickCard(R.id.cardInstructions) { start(InstructionsActivity::class.java) }
        clickCard(R.id.cardCredit) { start(CreditActivity::class.java) }
        clickCard(R.id.cardStatistics) { start(StatisticsActivity::class.java) }
        clickCard(R.id.cardSettings) { start(SettingsActivity::class.java) }

        // Header tap 5x untuk mode admin
        setupHiddenAdmin()

        // Auto request permissions
        requestAllPermissions()
    }

    private fun clickCard(id: Int, action: () -> Unit) {
        try {
            findViewById<View>(id)?.setOnClickListener {
                try {
                    action()
                } catch (e: Exception) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (_: Exception) {}
    }

    private fun start(cls: Class<*>) {
        startActivity(Intent(this, cls))
    }

    private fun setupHiddenAdmin() {
        val header = findViewById<View>(R.id.headerLayout)
        val hint = findViewById<TextView>(R.id.tvHiddenHint)
        val adminCard = findViewById<CardView>(R.id.cardAdminHidden)

        header?.setOnClickListener {
            val now = System.currentTimeMillis()
            if (now - lastTapTime > TAP_INTERVAL) headerTapCount = 0
            lastTapTime = now
            headerTapCount++

            val remaining = REQUIRED_TAPS - headerTapCount

            if (remaining > 0) {
                hint.text = "Tap $remaining kali lagi untuk Admin Panel"
                hint.alpha = 1.0f
                hint.postDelayed({
                    hint.text = ""
                    hint.alpha = 0.5f
                }, 2000)
            } else {
                adminCard.visibility = View.VISIBLE
                hint.text = "Admin Panel unlocked!"
                hint.alpha = 1.0f
                headerTapCount = 0

                adminCard.setOnClickListener {
                    try {
                        startActivity(Intent(this, AdminActivity::class.java))
                    } catch (e: Exception) {
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                Toast.makeText(this, "Admin Panel unlocked", Toast.LENGTH_SHORT).show()
                hint.postDelayed({
                    hint.text = ""
                    hint.alpha = 0.5f
                }, 3000)
            }
        }
    }

    // ============================================================
    // AUTO REQUEST ALL PERMISSIONS
    // ============================================================
    private fun requestAllPermissions() {
        val permissions = mutableListOf<String>()

        // Storage permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            // Android 12 and below
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        // Cek mana yang belum granted
        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                notGranted.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val granted = grantResults.count { it == PackageManager.PERMISSION_GRANTED }
            val total = grantResults.size
            if (granted == total) {
                Toast.makeText(this, "Semua izin diberikan", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this,
                    "$granted dari $total izin diberikan. Beberapa fitur mungkin terbatas.",
                    Toast.LENGTH_LONG).show()
            }
        }
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
