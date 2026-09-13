package com.yad.videoeditor

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.gms.ads.AdView

/**
 * MainActivity Client — halaman utama.
 *
 * Fitur:
 *   - Banner AdMob
 *   - Card: Video Saya, Video Editor, AI Text to Video, Aksi Cepat
 *
 * Tidak ada "Mode Generate" di halaman ini.
 * Mode generate (12 mode) ada di TextToVideoActivity.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Init AdMob
        AdMobHelper.init(this)
        AdMobHelper.loadInterstitial(this)
        AdMobHelper.loadRewarded(this)

        // Setup UI
        setupBannerAd()
        setupCards()
    }

    // ============================================================
    //  BANNER AD
    // ============================================================
    private fun setupBannerAd() {
        try {
            val adView = findViewById<AdView>(R.id.bannerAd)
            if (adView != null) {
                AdMobHelper.loadBanner(this, adView)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Banner error: " + e.message)
        }
    }

    // ============================================================
    //  CARDS — navigate ke activity asli
    // ============================================================
    private fun setupCards() {
        // Video Saya → VideoListActivity
        findViewById<CardView>(R.id.cardVideoSaya)?.setOnClickListener {
            try {
                startActivity(Intent(this, VideoListActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(this, "Video Saya: " + e.message, Toast.LENGTH_SHORT).show()
            }
        }

        // Video Editor → VideoEditorActivity
        findViewById<CardView>(R.id.cardVideoEditor)?.setOnClickListener {
            try {
                startActivity(Intent(this, VideoEditorActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(this, "Video Editor: " + e.message, Toast.LENGTH_SHORT).show()
            }
        }

        // AI Text to Video → TextToVideoActivity (di sini ada 12 mode)
        findViewById<CardView>(R.id.cardAITextToVideo)?.setOnClickListener {
            try {
                startActivity(Intent(this, TextToVideoActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(this, "AI Text to Video: " + e.message, Toast.LENGTH_SHORT).show()
            }
        }

        // Actions → ActionsActivity
        findViewById<CardView>(R.id.cardActions)?.setOnClickListener {
            try {
                startActivity(Intent(this, ActionsActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(this, "Actions: " + e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
