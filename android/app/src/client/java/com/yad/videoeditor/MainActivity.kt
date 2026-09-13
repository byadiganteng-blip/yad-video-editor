package com.yad.videoeditor

import android.content.Intent
import android.os.Bundle
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
 * MainActivity Client — versi bersih (rewrite).
 * Fitur:
 *   - Spinner 10 mode generate (2 non-AI + 8 AI)
 *   - Card ke Video Saya, Video Editor, AI Text to Video, dll
 *   - Banner Ad di bawah (AdMob)
 */
class MainActivity : AppCompatActivity() {

    // Mode yang dipilih user di spinner
    private var currentMode: GenerationMode = GenerationMode.DIRECT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ==== SPINNER MODE ====
        setupGenerationModeSpinner()

        // ==== BANNER AD ====
        try {
            val adView = findViewById<AdView>(R.id.bannerAd)
            adView?.let { AdMobHelper.loadBanner(this, it) }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Banner error: ${e.message}")
        }

        // ==== CARDS ====
        // Video Saya
        try {
            val cardVideo = findViewById<CardView>(R.id.cardVideoSaya)
            cardVideo?.setOnClickListener {
                startActivity(Intent(this, VideoListActivity::class.java))
            }
        } catch (_: Exception) {}

        // Video Editor
        try {
            val cardEditor = findViewById<CardView>(R.id.cardVideoEditor)
            cardEditor?.setOnClickListener {
                startActivity(Intent(this, VideoEditorActivity::class.java))
            }
        } catch (_: Exception) {}

        // AI Text to Video
        try {
            val cardAI = findViewById<CardView>(R.id.cardAITextToVideo)
            cardAI?.setOnClickListener {
                startActivity(Intent(this, TextToVideoActivity::class.java))
            }
        } catch (_: Exception) {}

        // Actions
        try {
            val cardActions = findViewById<CardView>(R.id.cardActions)
            cardActions?.setOnClickListener {
                startActivity(Intent(this, ActionsActivity::class.java))
            }
        } catch (_: Exception) {}
    }

    // ============================================================
    //  SPINNER MODE GENERATE — 10 opsi
    // ============================================================
    private fun setupGenerationModeSpinner() {
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
    //  GENERATE VIDEO — dipanggil dari tombol
    // ============================================================
    private fun generateVideoByMode(prompt: String, outputPath: String) {
        when (currentMode.type) {
            ModeType.DIRECT -> {
                DirectVideoGenerator.generateFromText(
                    this, prompt, 5, outputPath,
                    onSuccess = { path ->
                        runOnUiThread {
                            Toast.makeText(
                                this,
                                "Video dibuat: $path",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    onError = { err ->
                        runOnUiThread {
                            Toast.makeText(
                                this,
                                "Error: $err",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                )
            }

            ModeType.GOOGLE_IMAGE -> {
                GoogleImageGenerator.generateFromText(
                    this, prompt, 5, outputPath,
                    onSuccess = { path ->
                        runOnUiThread {
                            Toast.makeText(
                                this,
                                "Video dari Google: $path",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    onError = { err ->
                        runOnUiThread {
                            Toast.makeText(
                                this,
                                "Error: $err",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                )
            }

            ModeType.AI_MODEL -> {
                Toast.makeText(
                    this,
                    "Pakai model AI: ${currentMode.label}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
