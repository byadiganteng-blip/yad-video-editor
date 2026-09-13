package com.yad.videoeditor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.ads.AdView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    // Mode generate video yang dipilih user
    private var currentMode: GenerationMode = GenerationMode.DIRECT


    private val PERMISSION_REQUEST_CODE = 1001
    private lateinit var tvBanner: TextView
    private lateinit var tvStatus: TextView
    private var maintenanceShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SecureConfig.init(this)
        try { setContentView(R.layout.activity_main)

        // === SETUP SPINNER MODE GENERATE ===
        setupGenerationModeSpinner()
        
        // Load Banner Ad
        try {
            val adView = findViewById<AdView>(R.id.bannerAd)
            if (adView != null) {
                AdMobHelper.loadBanner(this, adView)
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Banner load error: ${e.message}")
        }
 }
        catch (e: Exception) { finish(); return }

        FirebaseManager.registerUser(this)
        FirebaseManager.updateLastUsed(this)

        tvBanner = findViewById(R.id.tvBanner)
        tvStatus = findViewById(R.id.tvStatus)

        try {
            findViewById<TextView>(R.id.tvCredit)?.text =
                "Created by KARYADI, Coding by KARYADI"
        } catch (_: Exception) {}

        updateStatus()

        // Menu CLIENT
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
                // Force update
                if (cfg.forceUpdate && BuildConfig.VERSION_CODE < cfg.minVersionCode) {
                    showForceUpdateDialog(cfg.updateUrl)
                }
                // Maintenance
                if (cfg.maintenanceMode && !maintenanceShown) {
                    maintenanceShown = true
                    showMaintenanceDialog()
                }
                // Banner
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
            .setTitle("⚠️ Update Tersedia")
            .setMessage("Versi baru tersedia. Silakan update untuk lanjut.")
            .setCancelable(false)
            .setPositiveButton("Update Sekarang") { _, _ ->
                if (url.isNotEmpty()) {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW,
                            android.net.Uri.parse(url)))
                    } catch (e: Exception) {
                        Toast.makeText(this, "Gagal buka link", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "URL update belum di-set admin",
                        Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Nanti") { _, _ -> }
            .show()
    }

    private fun showMaintenanceDialog() {
        AlertDialog.Builder(this)
            .setTitle("🛠️ Maintenance")
            .setMessage("Aplikasi sedang dalam perbaikan.\nCoba lagi nanti.")
            .setCancelable(false)
            .setPositiveButton("Keluar") { _, _ -> finish() }
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

    // ============================================================
    //  SPINNER MODE GENERATE VIDEO
    // ============================================================
    private fun setupGenerationModeSpinner() {
        val spinner = findViewById<Spinner>(R.id.spinnerGenerationMode)
        val tvDesc = findViewById<TextView>(R.id.tvModeDescription)

        val modes = GenerationMode.values()
        val labels = modes.map { it.label }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            labels
        )
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                currentMode = modes[position]
                tvDesc?.text = currentMode.description
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                currentMode = GenerationMode.DIRECT
            }
        }
    }

    // ============================================================
    //  GENERATE VIDEO SESUAI MODE
    // ============================================================
    private fun generateVideoByMode(prompt: String, outputPath: String) {
        when (currentMode) {
            GenerationMode.DIRECT -> {
                DirectVideoGenerator.generateFromText(
                    this, prompt, 5, outputPath,
                    onSuccess = { path ->
                        runOnUiThread {
                            Toast.makeText(this, "✅ Video dibuat: $path",
                                Toast.LENGTH_LONG).show()
                        }
                    },
                    onError = { err ->
                        runOnUiThread {
                            Toast.makeText(this, "❌ $err",
                                Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }
            GenerationMode.GOOGLE_IMAGE -> {
                GoogleImageGenerator.generateFromText(
                    this, prompt, 5, outputPath,
                    onSuccess = { path ->
                        runOnUiThread {
                            Toast.makeText(this, "✅ Video dari Google: $path",
                                Toast.LENGTH_LONG).show()
                        }
                    },
                    onError = { err ->
                        runOnUiThread {
                            Toast.makeText(this, "❌ $err",
                                Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }
            GenerationMode.AI_MODEL -> {
                // Panggil API model AI existing
                Toast.makeText(this, "🎨 Pakai model AI...",
                    Toast.LENGTH_SHORT).show()
                // TODO: panggil ApiClient.generateVideo(...)
            }
        }
    }
}
