package com.yad.videoeditor

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Halaman Petunjuk — instruksi lengkap penggunaan
 */
class InstructionsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { setContentView(R.layout.activity_instructions) }
        catch (e: Exception) { finish(); return }

        val tv = findViewById<TextView>(R.id.tvInstructions)
        tv.text = """
📖 PANDUAN LENGKAP YAD VIDEO EDITOR

═══════════════════════════════════════
🎬 CARA MEMBUAT VIDEO DARI CERITA
═══════════════════════════════════════

1. Buka menu "AI Text to Video"
2. Tulis cerita Anda di kotak (atau upload .txt)
3. Pilih model AI yang diinginkan
4. Atur pengaturan:
   • Ukuran video (YouTube/TikTok/Square)
   • Kualitas (480p/720p/1080p)
   • Suara narasi (pria/wanita/anak/robot)
   • Subtitle (tampil/tidak, gaya teks)
   • Watermark (opsional)
5. Klik "MEMBUAT VIDEO"
6. Tunggu proses (10-30 menit)
7. Video otomatis terdownload saat selesai

═══════════════════════════════════════
🎨 PILIHAN MODEL AI
═══════════════════════════════════════

🌸 WAIFU DIFFUSION (Klasik)
   Model lama tapi sangat bagus untuk anime.
   Stabil, ringan, hasil konsisten.
   Cocok untuk: cerita anime, karakter

🎨 STABLE DIFFUSION 1.5
   Model serbaguna. Cocok untuk realistis
   & anime. Paling stabil & ringan.
   Cocok untuk: semua jenis cerita

✨ ANYTHING v4.0
   Anime berkualitas tinggi. Detail bagus,
   warna cerah.
   Cocok untuk: storyboard, komik

💫 DREAMSHAPER
   Realistis bagus untuk karakter manusia.
   Populer di komunitas AI.
   Cocok untuk: cerita manusia

🏆 SDXL BASE 1.0
   Kualitas tertinggi. Resolusi 1024x1024.
   Cocok untuk: cinematic epic, film pendek
   (Lebih lambat, butuh waktu lebih)

🎭 OPENJOURNEY
   Gaya artistik & surreal.
   Cocok untuk: cerita fantasi & abstrak

🌙 DREAMLIIKE
   Surreal & dreamy.
   Cocok untuk: cerita mimpi & fantasi

🎌 TRINART v2
   Anime alternatif. Karakter fokus.
   Cocok untuk: portrait, karakter tunggal

📷 REALISTIC VISION 5.1
   Realistis natural.
   Cocok untuk: dokumenter & tutorial

🌺 MAJICMIX REALISTIC
   Realistis kualitas tinggi. Detail tajam.
   Cocok untuk: video profesional

═══════════════════════════════════════
🎙️ PILIHAN SUARA NARASI
═══════════════════════════════════════

Male ID   → Pria Indonesia (Ardi)
Female ID → Wanita Indonesia (Gadis)
Child ID  → Anak Indonesia (Ardi)
Male EN   → Pria Inggris (Guy)
Female EN → Wanita Inggris (Jenny)
Robot     → Robot (Davis)

═══════════════════════════════════════
💡 TIPS & TRIK
═══════════════════════════════════════

✅ Cerita yang baik:
   • 3-10 paragraf
   • Ada tokoh, latar, konflik
   • Gunakan deskripsi visual
   • Contoh: "Di sebuah desa kecil..."

✅ Untuk hasil terbaik:
   • Pilih 4-8 scene
   • Gunakan model sesuai tema
   • Tambah watermark untuk branding
   • Aktifkan subtitle untuk aksesibilitas

✅ Hemat waktu:
   • Gunakan model SD 1.5 untuk testing
   • Baru pakai SDXL untuk final

═══════════════════════════════════════
📥 CARA DOWNLOAD VIDEO
═══════════════════════════════════════

1. Setelah proses selesai, akan muncul
   notifikasi "Video Selesai"
2. Tap notifikasi → masuk ke Preview
3. Klik tombol "Download"
4. Video tersimpan di:
   /Movies/YAD Video Editor/

Atau:
1. Buka menu "Video Saya"
2. Pilih video
3. Tap download
4. Video masuk ke galeri

═══════════════════════════════════════
📂 FITUR LAIN
═══════════════════════════════════════

🎬 Video Editor
   • Trim (potong video)
   • Rotate (putar video)
   • Speed (ubah kecepatan)
   • Extract audio

📁 Files
   • Kelola file di perangkat
   • Refresh, buka folder

📊 Statistik
   • Data penggunaan aplikasi

⚙️ Pengaturan
   • Konfigurasi token
   • Reset data

═══════════════════════════════════════
❓ MASALAH UMUM
═══════════════════════════════════════

❌ "Token tidak tersedia"
   → Buka Pengaturan → isi token GitHub

❌ "Gagal mengirim permintaan"
   → Cek koneksi internet
   → Pastikan token masih valid

❌ "Timeout"
   → Server sedang sibuk
   → Coba lagi nanti

❌ "Video tidak muncul"
   → Tunggu notifikasi selesai
   → Buka menu Video Saya
   → Refresh

═══════════════════════════════════════
📞 KONTAK & DUKUNGAN
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
