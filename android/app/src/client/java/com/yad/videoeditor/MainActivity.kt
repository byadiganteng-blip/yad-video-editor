package com.yad.videoeditor

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide

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

        // Init StartApp saja (AdMob dinonaktifkan)
        StartAppHelper.init(this)

        // Banner StartApp
        setupBannerAd()

        // Setup UI
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
            val granted = grantResults.isNotEmpty() && grantResults.all {
                it == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
            if (!granted) {
                Toast.makeText(this, "Izin diperlukan untuk menyimpan video",
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupBannerAd() {
        try {
            val container = findViewById<android.widget.FrameLayout>(R.id.startAppBannerContainer)
            if (container != null) {
                StartAppHelper.loadBanner(this, container)
                AutoLogSaver.log("MainActivity", "StartApp banner loading")
            }
        } catch (e: Exception) {
            AutoLogSaver.logError("MainActivity", "Banner error", e)
        }
    }

    private fun setupCards() {
        // Video Saya — dengan iklan + gate
        findViewById<CardView>(R.id.cardVideoSaya)?.setOnClickListener {
            openWithAdAndGate(GateHelper.FEATURE_VIDEO_LIST, "Video Saya") {
                try { startActivity(Intent(this, VideoListActivity::class.java)) }
                catch (e: Exception) { Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show() }
            }
        }

        // Video Editor — dengan iklan + gate
        findViewById<CardView>(R.id.cardVideoEditor)?.setOnClickListener {
            openWithAdAndGate(GateHelper.FEATURE_VIDEO_EDITOR, "Video Editor") {
                try { startActivity(Intent(this, VideoEditorActivity::class.java)) }
                catch (e: Exception) { Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show() }
            }
        }

        // AI Text to Video — dengan iklan + gate
        findViewById<CardView>(R.id.cardAITextToVideo)?.setOnClickListener {
            openWithAdAndGate(GateHelper.FEATURE_AI_TEXT_VIDEO, "AI Text to Video") {
                try { startActivity(Intent(this, TextToVideoActivity::class.java)) }
                catch (e: Exception) { Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show() }
            }
        }

        // Bebas akses (info saja)
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
     * Buka fitur dengan:
     *  1. Cek gate rewarded (30 menit)
     *  2. Kalau belum ada akses → dialog
     *  3. Kalau sudah → tampil interstitial StartApp → buka fitur
     */
    private fun openWithAdAndGate(feature: String, label: String, action: () -> Unit) {
        // Sudah ada akses? tampil interstitial lalu buka
        if (GateHelper.hasAccess(this, feature)) {
            showInterstitialThen { action() }
            return
        }

        // Tampilkan dialog gate
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
            StartAppRewardedHelper.loadRewarded(
                this,
                onLoaded = {
                    runOnUiThread {
                        StartAppRewardedHelper.showRewarded(
                            this,
                            onReward = {
                                runOnUiThread {
                                    GateHelper.grantAccess(this, feature)
                                    Toast.makeText(this, "✅ Akses diberikan 30 menit",
                                        Toast.LENGTH_SHORT).show()
                                    showInterstitialThen { action() }
                                }
                            },
                            onFailed = { msg ->
                                runOnUiThread {
                                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                                }
                            }
                        )
                    }
                },
                onFailed = { msg ->
                    runOnUiThread {
                        AutoLogSaver.log("MainActivity", "Rewarded fail: $msg — fallback")
                        Toast.makeText(this, "Iklan tidak tersedia, akses tetap diberikan",
                            Toast.LENGTH_SHORT).show()
                        GateHelper.grantAccess(this, feature)
                        showInterstitialThen { action() }
                    }
                }
            )
        }

        dialogView.findViewById<Button>(R.id.btnGateCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * Tampil interstitial StartApp dulu, lalu jalankan action.
     * Kalau cooldown atau gagal → langsung action.
     */
    private fun showInterstitialThen(action: () -> Unit) {
        try {
            StartAppHelper.showInterstitial(this) {
                action()
            }
        } catch (e: Exception) {
            action()
        }
    }

    /**
     * Setup user info di hero header.
     */
    private fun setupUserInfo() {
        try {
            val tvName = findViewById<TextView>(R.id.tvUserName)
            val tvEmail = findViewById<TextView>(R.id.tvUserEmail)
            val ivPhoto = findViewById<ImageView>(R.id.ivUserPhoto)
            val btnLogout = findViewById<TextView>(R.id.btnLogout)

            val name = FirebaseManager.getCurrentUserName() ?: "User"
            val email = FirebaseManager.getCurrentUserEmail() ?: ""
            val photo = FirebaseManager.getCurrentUserPhoto()

            tvName?.text = name
            tvEmail?.text = email

            if (!photo.isNullOrEmpty() && ivPhoto != null) {
                try {
                    Glide.with(this)
                        .load(photo)
                        .circleCrop()
                        .placeholder(android.R.drawable.sym_def_app_icon)
                        .into(ivPhoto)
                } catch (e: Exception) {
                    ivPhoto.setImageResource(android.R.drawable.sym_def_app_icon)
                }
            } else {
                ivPhoto?.setImageResource(android.R.drawable.sym_def_app_icon)
            }

            btnLogout?.setOnClickListener {
                confirmLogout()
            }
        } catch (e: Exception) {
            AutoLogSaver.logError("MainActivity", "setupUserInfo failed", e)
        }
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Yakin mau keluar dari akun?")
            .setPositiveButton("Ya") { _, _ ->
                FirebaseManager.logoutUser()
                Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
