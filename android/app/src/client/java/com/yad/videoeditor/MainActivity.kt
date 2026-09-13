package com.yad.videoeditor

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.gms.ads.AdView

/**
 * MainActivity Client — FULL FITUR.
 *
 * Pakai semua class asli dari src/main/:
 *   - GenerationMode (enum, 10 mode)
 *   - AdMobHelper (banner, interstitial, rewarded)
 *   - VideoListActivity, VideoEditorActivity, TextToVideoActivity, ActionsActivity
 *   - DirectVideoGenerator, GoogleImageGenerator
 */
class MainActivity : AppCompatActivity() {

    // ============================================================
    //  STATE
    // ============================================================

    // ============================================================
    //  LIFECYCLE
    // ============================================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Init AdMob (sekali saja)
        AdMobHelper.init(this)
        AdMobHelper.loadInterstitial(this)
        AdMobHelper.loadRewarded(this)

        // Setup UI
        setupBannerAd()
        setupCards()
    }

    // ============================================================
    //  SPINNER MODE — pakai GenerationMode enum
    // ============================================================
        val spinner = findViewById<Spinner>(R.id.spinnerGenerationMode)
        val tvDesc = findViewById<TextView>(R.id.tvModeDescription)

        val labels = GenerationMode.allLabels()
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
                currentMode = GenerationMode.values()[position]
                tvDesc?.text = currentMode.description
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                currentMode = GenerationMode.DIRECT
            }
        }
    }

    // ============================================================
    //  BANNER AD — pakai AdMobHelper
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

        // AI Text to Video → TextToVideoActivity
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

    // ============================================================
    //  GENERATE VIDEO — per mode
    // ============================================================
    @Suppress("unused")
}
