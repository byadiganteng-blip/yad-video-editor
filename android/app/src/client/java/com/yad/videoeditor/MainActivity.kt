package com.yad.videoeditor

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

/**
 * MainActivity Client — versi MINIMAL (pasti compile).
 *
 * Tidak butuh class eksternal (GenerationMode, AdMobHelper, dll).
 * Fitur yang belum ada di-stub pakai Toast.
 *
 * Setelah build hijau, tinggal tambahkan:
 *   - AdMob banner loader
 *   - Intent ke VideoListActivity, VideoEditorActivity, dll
 *   - GenerationMode enum
 *   - Generator class
 */
class MainActivity : AppCompatActivity() {

    private val modes = listOf(
        "Direct Video (Non-AI)",
        "Google Image (Non-AI)",
        "AI: Text to Video",
        "AI: Image to Video",
        "AI: Style Transfer",
        "AI: Motion",
        "AI: Upscale",
        "AI: Background Remove",
        "AI: Auto Subtitle",
        "AI: Voice Over"
    )

    private val descriptions = listOf(
        "Buat video langsung dari teks tanpa AI",
        "Ambil gambar dari Google lalu jadikan video",
        "Generate video dari prompt teks",
        "Animasikan gambar jadi video",
        "Ubah gaya visual video",
        "Tambah gerakan pada gambar statis",
        "Naikkan resolusi video",
        "Hapus background otomatis",
        "Auto-generate subtitle",
        "Buat voice over otomatis"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupSpinner()
        setupCards()
    }

    private fun setupSpinner() {
        val spinner = findViewById<Spinner>(R.id.spinnerGenerationMode)
        val tvDesc = findViewById<TextView>(R.id.tvModeDescription)

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            modes
        )
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                tvDesc.text = descriptions[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupCards() {
        findViewById<CardView>(R.id.cardVideoSaya)?.setOnClickListener {
            Toast.makeText(this, "Video Saya (belum tersedia)", Toast.LENGTH_SHORT).show()
        }
        findViewById<CardView>(R.id.cardVideoEditor)?.setOnClickListener {
            Toast.makeText(this, "Video Editor (belum tersedia)", Toast.LENGTH_SHORT).show()
        }
        findViewById<CardView>(R.id.cardAITextToVideo)?.setOnClickListener {
            Toast.makeText(this, "AI Text to Video (belum tersedia)", Toast.LENGTH_SHORT).show()
        }
        findViewById<CardView>(R.id.cardActions)?.setOnClickListener {
            Toast.makeText(this, "Actions (belum tersedia)", Toast.LENGTH_SHORT).show()
        }
    }
}
