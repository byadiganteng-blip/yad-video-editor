package com.yad.videoeditor

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.gms.ads.AdView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        PermissionHelper.requestAllPermissions(this)

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try {
                if (!PermissionHelper.hasFloatingPermission(this)) {
                    PermissionHelper.requestFloatingPermission(this)
                }
            } catch (_: Exception) {}
        }, 1500)

        AdMobHelper.init(this)
        AdMobHelper.loadInterstitial(this)
        AdMobHelper.loadRewarded(this)

        StartAppHelper.init(this)

        setupBannerAd()
        setupCards()
        setupUserInfo()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PermissionHelper.REQ_CODE_STORAGE) {
            val granted = grantResults.isNotEmpty() && grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }
            if (!granted) {
                Toast.makeText(this, "Izin diperlukan untuk menyimpan video",
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupBannerAd() {
        try {
            val startAppContainer = findViewById<android.widget.FrameLayout>(R.id.startAppBannerContainer)
            if (startAppContainer != null) {
                StartAppHelper.loadBanner(this, startAppContainer)
                AutoLogSaver.log("MainActivity", "StartApp banner loading (primary)")
            }

            val adView = findViewById<com.google.android.gms.ads.AdView>(R.id.bannerAd)
            if (adView != null) {
                adView.visibility = android.view.View.GONE
                AdMobHelper.loadBanner(this, adView)
                AutoLogSaver.log("MainActivity", "AdMob banner loading (fallback)")
            }
        } catch (e: Exception) {
            AutoLogSaver.logError("MainActivity", "Banner error", e)
        }
    }

    private fun setupCards() {
        // Gate: Video Saya
        findViewById<CardView>(R.id.cardVideoSaya)?.setOnClickListener {
            openWithGate(GateHelper.FEATURE_VIDEO_LIST, "Video Saya") {
                try { startActivity(Intent(this, VideoListActivity::class.java)) }
                catch (e: Exception) { Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show() }
            }
        }

        // Gate: Video Editor
        findViewById<CardView>(R.id.cardVideoEditor)?.setOnClickListener {
            openWithGate(GateHelper.FEATURE_VIDEO_EDITOR, "Video Editor") {
                try { startActivity(Intent(this, VideoEditorActivity::class.java)) }
                catch (e: Exception) { Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show() }
            }
        }

        // Gate: AI Text to Video
        findViewById<CardView>(R.id.cardAITextToVideo)?.setOnClickListener {
            openWithGate(GateHelper.FEATURE_AI_TEXT_VIDEO, "AI Text to Video") {
                try { startActivity(Intent(this, TextToVideoActivity::class.java)) }
                catch (e: Exception) { Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show() }
            }
        }

        // Bebas akses (tidak di-gate)
        findViewById<CardView>(R.id.cardInstructions)?.setOnClickListener {
            try { startActivity(Intent(this, InstructionsActivity::class.java)) }
            catch (e: Exception) { Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show() }
        }

        findViewById<CardView>(R.id.cardCredit)?.setOnClickListener {
            try { startActivity(Intent(this, CreditActivity::class.java)) }
            catch (e: Exception) { Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show() }
        }

        findViewById<CardView>(R.id.cardSaweria)?.setOnClickListener {
            try { startActivity(Intent(this, SaweriaActivity::class.java)) }
            catch (e: Exception) { Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show() }
        }

        findViewById<CardView>(R.id.cardReward)?.setOnClickListener {
            try { startActivity(Intent(this, RewardActivity::class.java)) }
            catch (e: Exception) { Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show() }
        }
    }

    /**
     * Buka fitur dengan gate. Kalau sudah ada akses → langsung buka.
     * Kalau belum → tampil dialog "Tonton video dulu".
     * Kalau user pilih tonton → load rewarded → kalau selesai → grant + buka.
     * Kalau iklan gagal load → tetap buka (graceful).
     * Kalau user cancel / skip → tetap di MainActivity.
     */
    private fun openWithGate(feature: String, label: String, action: () -> Unit) {
        // Sudah ada akses? langsung buka
        if (GateHelper.hasAccess(this, feature)) {
            action()
            return
        }

        // Tampilkan dialog
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_gate, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val tvMessage = dialogView.findViewById<TextView>(R.id.tvGateMessage)
        tvMessage.text = "Tonton video singkat untuk membuka \"$label\" selama 30 menit."

        dialogView.findViewById<Button>(R.id.btnGateWatch).setOnClickListener {
            dialog.dismiss()
            // Load rewarded dulu, baru show
            StartAppRewardedHelper.loadRewarded(
                this,
                onLoaded = {
                    runOnUiThread {
                        StartAppRewardedHelper.showRewarded(
                            this,
                            onReward = {
                                // User selesai nonton → grant akses
                                runOnUiThread {
                                    GateHelper.grantAccess(this, feature)
                                    Toast.makeText(
                                        this,
                                        "✅ Akses diberikan selama 30 menit",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    action()
                                }
                            },
                            onFailed = { msg ->
                                // Iklan gagal tampil (skip / no fill) → BUKAN grant akses
                                runOnUiThread {
                                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                                }
                            }
                        )
                    }
                },
                onFailed = { msg ->
                    // Iklan gagal LOAD (network, no fill) → tetap kasih akses
                    runOnUiThread {
                        AutoLogSaver.log("MainActivity", "Rewarded load failed: $msg — granting fallback")
                        Toast.makeText(
                            this,
                            "Iklan tidak tersedia, akses tetap diberikan",
                            Toast.LENGTH_SHORT
                        ).show()
                        GateHelper.grantAccess(this, feature)
                        action()
                    }
                }
            )
        }

        dialogView.findViewById<Button>(R.id.btnGateCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setupUserInfo() {
        try {
            val tvName = findViewById<android.widget.TextView>(R.id.tvUserName)
            val tvEmail = findViewById<android.widget.TextView>(R.id.tvUserEmail)
            val ivPhoto = findViewById<android.widget.ImageView>(R.id.ivUserPhoto)

            val name = FirebaseManager.getCurrentUserName() ?: "User"
            val email = FirebaseManager.getCurrentUserEmail() ?: ""
            val photo = FirebaseManager.getCurrentUserPhoto()

            tvName?.text = name
            tvEmail?.text = email

            if (!photo.isNullOrEmpty() && ivPhoto != null) {
                // Load image async dengan library sederhana
                // Untuk sekarang pakai placeholder emoji
                ivPhoto.setImageResource(android.R.drawable.sym_def_app_icon)
            }
        } catch (e: Exception) {
            AutoLogSaver.logError("MainActivity", "setupUserInfo failed", e)
        }
    }
}
