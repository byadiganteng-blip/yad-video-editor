package com.yad.videoeditor

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class InstructionsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { setContentView(R.layout.activity_instructions) }
        catch (e: Exception) { finish(); return }

        // Layout activity_instructions.xml punya ID "tvInstructions"
        val tv = findViewById<TextView>(R.id.tvInstructions)
        tv.text = """
📖 PANDUAN LENGKAP AI TXT TO VIDEO + EDITOR

═══════════════════════════════════════
🎬 CARA MEMBUAT VIDEO
═══════════════════════════════════════

1. Buka menu "AI Text to Video"
2. Tulis cerita Anda (atau upload .txt)
3. Pilih model AI yang diinginkan
4. Atur ukuran video, suara, subtitle
5. Klik "MEMBUAT VIDEO"
6. Tunggu proses selesai
7. Video otomatis siap di-download

═══════════════════════════════════════
🎨 PILIHAN MODEL AI
═══════════════════════════════════════

🌸 Waifu Diffusion (Klasik)
   Model lama tapi sangat bagus untuk anime.

🎨 Stable Diffusion 1.5
   Model serbaguna. Cocok realistis & anime.

✨ Anything v4.0
   Anime berkualitas tinggi.

💫 DreamShaper
   Realistis bagus untuk karakter manusia.

🏆 SDXL Base 1.0
   Kualitas tertinggi (lebih lambat).

🎭 OpenJourney
   Gaya artistik & surreal.

🌙 Dreamlike
   Surreal & dreamy.

🎌 Trinart v2
   Anime alternatif.

📷 Realistic Vision
   Realistis natural.

🌺 MajicMix
   Realistis detail tajam.

═══════════════════════════════════════
🎙️ PILIHAN SUARA NARASI
═══════════════════════════════════════

Male ID   → Pria Indonesia
Female ID → Wanita Indonesia
Child ID  → Anak Indonesia
Male EN   → Pria Inggris
Female EN → Wanita Inggris
Robot     → Robot

═══════════════════════════════════════
💡 TIPS & TRIK
═══════════════════════════════════════

✅ Cerita yang baik:
   • 3-10 paragraf
   • Ada tokoh, latar, konflik
   • Gunakan deskripsi visual

✅ Untuk hasil terbaik:
   • Pilih 4-8 scene
   • Gunakan model sesuai tema
   • Aktifkan subtitle

═══════════════════════════════════════
📥 CARA DOWNLOAD VIDEO
═══════════════════════════════════════

1. Setelah selesai, notifikasi muncul
2. Tap notifikasi → Preview
3. Klik tombol "Download"
4. Video tersimpan di:
   /Movies/YAD Video Editor/

═══════════════════════════════════════
❓ MASALAH UMUM
═══════════════════════════════════════

❌ "Token tidak tersedia"
   → Hubungi admin

❌ "Timeout"
   → Coba lagi nanti

❌ "Video tidak muncul"
   → Cek menu Video Saya

═══════════════════════════════════════
📞 KONTAK
═══════════════════════════════════════

Email  : ynuraini686@gmail.com
GitHub : byadiganteng-blip/yad-video-editor

═══════════════════════════════════════

"Dari cerita jadi karya."
              — KARYADI

═══════════════════════════════════════
        """.trimIndent()
    }
}
