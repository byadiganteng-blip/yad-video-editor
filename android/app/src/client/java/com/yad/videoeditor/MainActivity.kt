package com.yad.videoeditor

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PermissionHelper.REQ_CODE_STORAGE) {
            val granted = grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }
            if (!granted) {
                Toast.makeText(this, "Izin diperlukan untuk menyimpan video",
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupBannerAd() {
        try {
            val adView = findViewById<AdView>(R.id.bannerAd)
            if (adView != null) AdMobHelper.loadBanner(this, adView)
        } catch (e: Exception) {
            Log.e("MainActivity", "Banner error: " + e.message)
        }
    }

    private fun setupCards() {
        // Video Saya → VideoListActivity
        findViewById<CardView>(R.id.cardVideoSaya)?.setOnClickListener {
            try { startActivity(Intent(this, VideoListActivity::class.java)) }
            catch (e: Exception) { Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show() }
        }

        // Video Editor → VideoEditorActivity
        findViewById<CardView>(R.id.cardVideoEditor)?.setOnClickListener {
            try { startActivity(Intent(this, VideoEditorActivity::class.java)) }
            catch (e: Exception) { Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show() }
        }

        // AI Text to Video → TextToVideoActivity
        findViewById<CardView>(R.id.cardAITextToVideo)?.setOnClickListener {
            try { startActivity(Intent(this, TextToVideoActivity::class.java)) }
            catch (e: Exception) { Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show() }
        }

        // Instructions / Panduan → InstructionsActivity
        findViewById<CardView>(R.id.cardInstructions)?.setOnClickListener {
            try { startActivity(Intent(this, InstructionsActivity::class.java)) }
            catch (e: Exception) { Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show() }
        }

        // Credit → CreditActivity
        findViewById<CardView>(R.id.cardCredit)?.setOnClickListener {
            try { startActivity(Intent(this, CreditActivity::class.java)) }
            catch (e: Exception) { Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show() }
        }

        // Saweria → SaweriaActivity
        findViewById<CardView>(R.id.cardSaweria)?.setOnClickListener {
            try { startActivity(Intent(this, SaweriaActivity::class.java)) }
            catch (e: Exception) { Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show() }
        }
    }
}
