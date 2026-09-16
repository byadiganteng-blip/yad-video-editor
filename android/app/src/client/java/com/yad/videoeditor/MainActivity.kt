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

        Tracker.lifecycle("MainActivity", "onCreate")

        try {
            PermissionHelper.requestAllPermissions(this)
        } catch (e: Exception) {
            Tracker.error("MainActivity", "permission_request_failed", "", e)
        }

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try {
                if (!PermissionHelper.hasFloatingPermission(this)) {
                    PermissionHelper.requestFloatingPermission(this)
                }
            } catch (_: Exception) {}
        }, 1500)

        // Init StartApp saja (AdMob dihapus)
        StartAppHelper.init(this)

        setupBannerAd()
        setupCards()
        setupUserInfo()
    }

    override fun onResume() {
        super.onResume()
        Tracker.lifecycle("MainActivity", "onResume")
    }

    override fun onPause() {
        super.onPause()
        Tracker.lifecycle("MainActivity", "onPause")
    }

    override fun onDestroy() {
        super.onDestroy()
        Tracker.lifecycle("MainActivity", "onDestroy")
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
            Tracker.info("MainActivity", "permission_result", data = mapOf(
                "request_code" to requestCode,
                "granted" to granted
            ))
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
            } else {
                Tracker.warn("MainActivity", "banner_container_missing")
            }
        } catch (e: Exception) {
            Tracker.error("MainActivity", "banner_setup_failed", "", e)
        }
    }

    private fun setupCards() {
        findViewById<CardView>(R.id.cardVideoSaya)?.setOnClickListener {
            Tracker.userAction("MainActivity", "click", mapOf("card" to "video_saya"))
            openWithAdAndGate(GateHelper.FEATURE_VIDEO_LIST, "Video Saya") {
                try { startActivity(Intent(this, VideoListActivity::class.java)) }
                catch (e: Exception) { Tracker.error("MainActivity", "open_videolist_failed", "", e) }
            }
        }

        findViewById<CardView>(R.id.cardVideoEditor)?.setOnClickListener {
            Tracker.userAction("MainActivity", "click", mapOf("card" to "video_editor"))
            openWithAdAndGate(GateHelper.FEATURE_VIDEO_EDITOR, "Video Editor") {
                try { startActivity(Intent(this, VideoEditorActivity::class.java)) }
                catch (e: Exception) { Tracker.error("MainActivity", "open_editor_failed", "", e) }
            }
        }

        findViewById<CardView>(R.id.cardAITextToVideo)?.setOnClickListener {
            Tracker.userAction("MainActivity", "click", mapOf("card" to "ai_text_video"))
            openWithAdAndGate(GateHelper.FEATURE_AI_TEXT_VIDEO, "AI Text to Video") {
                try { startActivity(Intent(this, TextToVideoActivity::class.java)) }
                catch (e: Exception) { Tracker.error("MainActivity", "open_ai_failed", "", e) }
            }
        }

        findViewById<CardView>(R.id.cardInstructions)?.setOnClickListener {
            Tracker.userAction("MainActivity", "click", mapOf("card" to "instructions"))
            showInterstitialThen("instructions") {
                try { startActivity(Intent(this, InstructionsActivity::class.java)) }
                catch (e: Exception) { Tracker.error("MainActivity", "open_instructions_failed", "", e) }
            }
        }

        findViewById<CardView>(R.id.cardCredit)?.setOnClickListener {
            Tracker.userAction("MainActivity", "click", mapOf("card" to "credit"))
            showInterstitialThen("credit") {
                try { startActivity(Intent(this, CreditActivity::class.java)) }
                catch (e: Exception) { Tracker.error("MainActivity", "open_credit_failed", "", e) }
            }
        }

        findViewById<CardView>(R.id.cardSaweria)?.setOnClickListener {
            Tracker.userAction("MainActivity", "click", mapOf("card" to "saweria"))
            // Saweria: TIDAK pakai interstitial (donasi)
            try { startActivity(Intent(this, SaweriaActivity::class.java)) }
            catch (e: Exception) { Tracker.error("MainActivity", "open_saweria_failed", "", e) }
        }

        findViewById<CardView>(R.id.cardReward)?.setOnClickListener {
            Tracker.userAction("MainActivity", "click", mapOf("card" to "reward"))
            showInterstitialThen("reward") {
                try { startActivity(Intent(this, RewardActivity::class.java)) }
                catch (e: Exception) { Tracker.error("MainActivity", "open_reward_failed", "", e) }
            }
        }
    }

    private fun openWithAdAndGate(feature: String, label: String, action: () -> Unit) {
        Tracker.gateEvent(feature, "open_requested", mapOf("label" to label))

        if (GateHelper.hasAccess(this, feature)) {
            Tracker.gateEvent(feature, "access_granted_direct")
            showInterstitialThen("feature_$feature") { action() }
            return
        }

        Tracker.gateEvent(feature, "show_dialog")

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_gate, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val tvMessage = dialogView.findViewById<TextView>(R.id.tvGateMessage)
        tvMessage.text = "Tonton video singkat untuk membuka \"$label\" selama 30 menit."

        dialogView.findViewById<Button>(R.id.btnGateWatch).setOnClickListener {
            Tracker.userAction("Gate", "dialog_watch_clicked", mapOf("feature" to feature))
            dialog.dismiss()

            StartAppRewardedHelper.loadRewarded(
                this,
                onLoaded = {
                    runOnUiThread {
                        Tracker.adEvent("StartApp", "rewarded", "ready_to_show", mapOf(
                            "feature" to feature
                        ))
                        StartAppRewardedHelper.showRewarded(
                            this,
                            onReward = {
                                runOnUiThread {
                                    Tracker.gateEvent(feature, "reward_received")
                                    GateHelper.grantAccess(this, feature)
                                    Toast.makeText(this, "✅ Akses diberikan 30 menit",
                                        Toast.LENGTH_SHORT).show()
                                    showInterstitialThen("feature_$feature") { action() }
                                }
                            },
                            onFailed = { msg ->
                                runOnUiThread {
                                    Tracker.gateEvent(feature, "reward_failed", mapOf("msg" to msg))
                                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                                }
                            }
                        )
                    }
                },
                onFailed = { msg ->
                    runOnUiThread {
                        Tracker.gateEvent(feature, "load_failed_fallback", mapOf("msg" to msg))
                        Toast.makeText(this, "Iklan tidak tersedia, akses tetap diberikan",
                            Toast.LENGTH_SHORT).show()
                        GateHelper.grantAccess(this, feature)
                        showInterstitialThen("feature_$feature") { action() }
                    }
                }
            )
        }

        dialogView.findViewById<Button>(R.id.btnGateCancel).setOnClickListener {
            Tracker.userAction("Gate", "dialog_cancel_clicked", mapOf("feature" to feature))
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showInterstitialThen(source: String, action: () -> Unit) {
        Tracker.info("MainActivity", "show_interstitial_then", source)
        try {
            StartAppHelper.showInterstitial(this) {
                Tracker.info("MainActivity", "interstitial_dismissed", source)
                action()
            }
        } catch (e: Exception) {
            Tracker.error("MainActivity", "interstitial_error", source, e)
            action()
        }
    }

    private fun setupUserInfo() {
        try {
            val tvName = findViewById<TextView>(R.id.tvUserName)
            val tvEmail = findViewById<TextView>(R.id.tvUserEmail)
            val ivPhoto = findViewById<ImageView>(R.id.ivUserPhoto)
            val btnLogout = findViewById<TextView>(R.id.btnLogout)

            val name = FirebaseManager.getCurrentUserName() ?: "User"
            val email = FirebaseManager.getCurrentUserEmail() ?: ""
            val photo = FirebaseManager.getCurrentUserPhoto()

            Tracker.info("MainActivity", "user_info_setup", data = mapOf(
                "name" to name,
                "has_photo" to (!photo.isNullOrEmpty())
            ))

            tvName?.text = name
            tvEmail?.text = email

            if (!photo.isNullOrEmpty() && ivPhoto != null) {
                try {
                    Glide.with(this).load(photo).circleCrop()
                        .placeholder(android.R.drawable.sym_def_app_icon).into(ivPhoto)
                } catch (e: Exception) {
                    Tracker.error("MainActivity", "load_photo_failed", "", e)
                    ivPhoto.setImageResource(android.R.drawable.sym_def_app_icon)
                }
            } else {
                ivPhoto?.setImageResource(android.R.drawable.sym_def_app_icon)
            }

            btnLogout?.setOnClickListener {
                Tracker.userAction("MainActivity", "click_logout")
                confirmLogout()
            }
        } catch (e: Exception) {
            Tracker.error("MainActivity", "setup_user_info_failed", "", e)
        }
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Yakin mau keluar dari akun?")
            .setPositiveButton("Ya") { _, _ ->
                Tracker.authEvent("firebase", "logout_started")
                FirebaseManager.logoutUser()
                Tracker.authEvent("firebase", "logout_success")
                Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
