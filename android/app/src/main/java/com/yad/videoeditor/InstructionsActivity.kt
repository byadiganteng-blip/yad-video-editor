package com.yad.videoeditor

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class InstructionsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { setContentView(R.layout.activity_instructions) }
        catch (e: Exception) { finish(); return }

        val tv = findViewById<TextView>(R.id.tvInstructions)
        tv.text = """
📖 PANDUAN LENGKAP

🎬 CARA MEMBUAT VIDEO
1. Buka menu AI Text to Video
2. Tulis cerita atau upload .txt
3. Pilih model AI
4. Atur video, suara, subtitle
5. Klik MEMBUAT VIDEO
6. Tunggu proses selesai
7. Video otomatis siap download

🎨 PILIHAN MODEL AI
🌸 Waifu Diffusion - Anime klasik
🎨 Stable Diffusion 1.5 - Serbaguna
✨ Anything v4.0 - Anime HD
💫 DreamShaper - Realistis
🏆 SDXL Base - Kualitas tertinggi
🎭 OpenJourney - Artistik
🌙 Dreamlike - Surreal
🎌 Trinart v2 - Anime alternatif
📷 Realistic Vision - Natural
🌺 MajicMix - Detail tajam

🎙️ SUARA NARASI
Male ID, Female ID, Child ID,
Male EN, Female EN, Robot

📥 CARA DOWNLOAD
Setelah selesai, notifikasi muncul.
Tap notifikasi → Preview → Download.
Video tersimpan di Movies/YAD Video Editor.

❓ MASALAH UMUM
"Token tidak tersedia" → Isi di Pengaturan
"Timeout" → Coba lagi nanti

📞 KONTAK
Email  : ynuraini686@gmail.com
GitHub : byadiganteng-blip

"Dari cerita jadi karya."
       — KARYADI
        """.trimIndent()
    }
}
