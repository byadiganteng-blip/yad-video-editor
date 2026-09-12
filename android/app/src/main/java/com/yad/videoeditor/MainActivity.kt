package com.yad.videoeditor

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

/**
 * Main Activity - dengan mode admin tersembunyi
 * Created by KARYADI, Coding by KARYADI
 */
class MainActivity : AppCompatActivity() {

    private var headerTapCount = 0
    private var lastTapTime = 0L
    private val REQUIRED_TAPS = 5
    private val TAP_INTERVAL = 500L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)
        } catch (e: Exception) { finish(); return }

        findViewById<TextView>(R.id.tvCredit)?.text = "Created by KARYADI, Coding by KARYADI"

        // Setup tombol menu
        clickCard(R.id.btnVideoList) { start(VideoListActivity::class.java) }
        clickCard(R.id.btnEditor) { start(VideoEditorActivity::class.java) }
        clickCard(R.id.btnFiles) { start(FilesActivity::class.java) }
        clickCard(R.id.btnInstructions) { start(InstructionsActivity::class.java) }
        clickCard(R.id.btnCredit) { start(CreditActivity::class.java) }
        clickCard(R.id.btnStatistics) { start(StatisticsActivity::class.java) }
        clickCard(R.id.btnSettings) { start(SettingsActivity::class.java) }

        // Header tap detection — 5x tap untuk mode admin
        setupHiddenAdmin()
    }

    private fun clickCard(id: Int, action: () -> Unit) {
        try {
            findViewById<View>(id)?.setOnClickListener { action() }
        } catch (_: Exception) {}
    }

    private fun start(cls: Class<*>) {
        try {
            startActivity(Intent(this, cls))
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupHiddenAdmin() {
        val header = findViewById<View>(R.id.headerLayout)
        val hint = findViewById<TextView>(R.id.tvHiddenHint)
        val adminCard = findViewById<CardView>(R.id.cardAdminHidden)

        header?.setOnClickListener {
            val now = System.currentTimeMillis()
            if (now - lastTapTime > TAP_INTERVAL) {
                headerTapCount = 0
            }
            lastTapTime = now
            headerTapCount++

            val remaining = REQUIRED_TAPS - headerTapCount

            if (remaining > 0) {
                hint.text = "Tap $remaining kali lagi untuk Admin Panel"
                hint.alpha = 1.0f
                // Auto-hide hint setelah 2 detik
                hint.postDelayed({
                    hint.alpha = 0.5f
                    hint.text = ""
                }, 2000)
            } else {
                // Unlock admin panel
                adminCard.visibility = View.VISIBLE
                hint.text = "✅ Admin Panel unlocked!"
                hint.alpha = 1.0f
                headerTapCount = 0

                // Click listener untuk admin card
                findViewById<View>(R.id.btnAdmin)?.setOnClickListener {
                    try {
                        startActivity(Intent(this, AdminActivity::class.java))
                    } catch (e: Exception) {
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                Toast.makeText(this, "🔓 Admin Panel unlocked", Toast.LENGTH_SHORT).show()

                hint.postDelayed({
                    hint.text = ""
                    hint.alpha = 0.5f
                }, 3000)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        try {
            android.app.AlertDialog.Builder(this)
                .setTitle("Keluar?")
                .setMessage("Tutup aplikasi?")
                .setPositiveButton("Ya") { _, _ -> finish() }
                .setNegativeButton("Batal", null)
                .show()
        } catch (_: Exception) { super.onBackPressed() }
    }
}
