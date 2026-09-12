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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * CLIENT MENU — hanya untuk APK client (com.yad.videoeditor)
 * TIDAK ada tombol admin panel di sini.
 */
class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 1001
    private lateinit var tvBanner: TextView
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SecureConfig.init(this)
        try { setContentView(R.layout.activity_main) }
        catch (e: Exception) { finish(); return }

        FirebaseManager.registerUser(this)
        FirebaseManager.updateLastUsed(this)

        tvBanner = findViewById(R.id.tvBanner)
        tvStatus = findViewById(R.id.tvStatus)

        try {
            findViewById<TextView>(R.id.tvCredit)?.text =
                "Created by KARYADI, Coding by KARYADI"
        } catch (_: Exception) {}

        // Status token
        updateStatus()

        // Menu CLIENT (tanpa admin)
        clickCard(R.id.cardVideoList) { start(VideoListActivity::class.java) }
        clickCard(R.id.cardEditor) { start(VideoEditorActivity::class.java) }
        clickCard(R.id.cardAiVideo) { start(TextToVideoActivity::class.java) }
        clickCard(R.id.cardFiles) { start(FilesActivity::class.java) }
        clickCard(R.id.cardInstructions) { start(InstructionsActivity::class.java) }
        clickCard(R.id.cardCredit) { start(CreditActivity::class.java) }
        clickCard(R.id.cardStatistics) { start(StatisticsActivity::class.java) }

        requestAllPermissions()
        listenConfig()
    }

    private fun updateStatus() {
        tvStatus?.text = if (SecureConfig.hasGithubToken())
            "✅ Siap" else "⚠️ Token belum tersedia"
    }

    private fun listenConfig() {
        lifecycleScope.launch {
            FirebaseManager.configFlow().collectLatest { cfg ->
                if (cfg.forceUpdate && BuildConfig.VERSION_CODE < cfg.minVersionCode) {
                    showForceUpdateDialog(cfg.updateUrl)
                }
                if (cfg.maintenanceMode) showMaintenanceDialog()
                if (cfg.messageBanner.isNotEmpty()) {
                    tvBanner?.text = cfg.messageBanner
                    tvBanner?.visibility = View.VISIBLE
                } else {
                    tvBanner?.visibility = View.GONE
                }
            }
        }
        lifecycleScope.launch {
            FirebaseManager.broadcastFlow().collectLatest { b ->
                if (b?.active == true && b.message.isNotEmpty()) {
                    tvBanner?.text = "📢 " + b.message
                    tvBanner?.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun showForceUpdateDialog(url: String) {
        AlertDialog.Builder(this)
            .setTitle("Update Tersedia")
            .setMessage("Versi baru tersedia. Silakan update.")
            .setCancelable(false)
            .setPositiveButton("Update") { _, _ ->
                if (url.isNotEmpty())
                    startActivity(Intent(Intent.ACTION_VIEW,
                        android.net.Uri.parse(url)))
            }
            .show()
    }

    private fun showMaintenanceDialog() {
        AlertDialog.Builder(this)
            .setTitle("Maintenance")
            .setMessage("Aplikasi sedang maintenance.")
            .setCancelable(false)
            .setPositiveButton("Tutup") { _, _ -> finish() }
            .show()
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
                this, notGranted.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setTitle("Keluar?")
            .setPositiveButton("Ya") { _, _ -> finish() }
            .setNegativeButton("Batal", null)
            .show()
    }
}
