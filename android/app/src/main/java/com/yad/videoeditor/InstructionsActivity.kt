package com.yad.videoeditor

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class InstructionsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_instructions)
        } catch (e: Exception) {
            finish()
            return
        }

        val tv = try {
            findViewById<TextView>(R.id.tvInstructions)
        } catch (e: Exception) {
            null
        } ?: try {
            findViewById<TextView>(R.id.tvInfo)
        } catch (e: Exception) {
            null
        } ?: try {
            findViewById<TextView>(R.id.tvContent)
        } catch (e: Exception) {
            null
        }

        tv?.text = """
📖 PANDUAN LENGKAP AI TXT TO VIDEO + EDITOR

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
🎨 SD 1.5 - Serbaguna
✨ Anything v4 - Anime HD
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
"Video tidak muncul" → Cek menu Video Saya

📞 KONTAK
Email  : ynuraini686@gmail.com
GitHub : byadiganteng-blip

"Dari cerita jadi karya."
       — KARYADI
        """.trimIndent()
    }
}
