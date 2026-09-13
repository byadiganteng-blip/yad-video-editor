package com.yad.videoeditor

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * InstructionsActivity — panduan pakai aplikasi.
 *
 * Semua bahasa user-friendly, tanpa sebut "GitHub" / "Firebase".
 */
class InstructionsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_instructions)

        // Isi text panduan
        val tvContent = findViewById<TextView>(R.id.tvInstructionsContent)
        tvContent.text = getInstructionsText()
    }

    private fun getInstructionsText(): String {
        return """
        🎬 SELAMAT DATANG DI YAD VIDEO EDITOR 🎬

        Aplikasi ini dibuat untuk memudahkan kamu membuat
        video keren dari teks, gambar, atau AI.

        ═══════════════════════════════════════
        📱 CARA PAKAI
        ═══════════════════════════════════════

        1️⃣  BUAT VIDEO DARI TEKS
           • Buka menu "AI Text to Video"
           • Tulis cerita atau upload file .txt
           • Pilih mode yang diinginkan:
             - Generate Langsung (tanpa AI)
             - Cari Gambar + Video
             - Atau pilih model AI favoritmu
           • Pilih ukuran video, kualitas, suara
           • Tap "MEMBUAT VIDEO"
           • Tunggu sampai selesai ✨

        2️⃣  LIHAT VIDEO SAYA
           • Buka menu "Video Saya"
           • Semua video yang sudah dibuat muncul di sini
           • Tap untuk preview
           • Tekan lama untuk hapus / share

        3️⃣  EDIT VIDEO
           • Buka menu "Video Editor"
           • Pilih video yang mau diedit
           • Trim, gabung, atau tambah filter

        4️⃣  AKSI CEPAT
           • Buka menu "Aksi Cepat"
           • Shortcut ke fitur favoritmu

        ═══════════════════════════════════════
        ✨ TIPS & TRIK
        ═══════════════════════════════════════

        💡 Tulis cerita yang menarik dan detail
           supaya hasil video lebih bagus.

        💡 Pilih ukuran video:
           • YouTube Shorts (1080:1920) - vertikal
           • YouTube (1920:1080) - horizontal
           • Instagram (1080:1080) - kotak

        💡 Atur kualitas video:
           • 240p - hemat kuota
           • 720p - seimbang
           • 1080p - kualitas terbaik

        💡 Pilih suara narasi yang cocok:
           • Pria Indonesia
           • Wanita Indonesia
           • Atau suara lain sesuai selera

        💡 Tambahkan watermark:
           • Contoh: @username_kamu
           • Muncul di pojok video

        💡 Style teks video:
           • Gradient - warna lembut
           • Neon Glow - efek cahaya
           • Shadow - bayangan tegas
           • Outline - garis tepi

        ═══════════════════════════════════════
        ❓ PERTANYAAN UMUM
        ═══════════════════════════════════════

        ❔ Kenapa video saya gagal dibuat?
        → Pastikan:
          • Izin penyimpanan sudah diberikan
          • Koneksi internet stabil
          • Teks cerita tidak terlalu panjang

        ❔ Berapa lama proses pembuatan video?
        → Tergantung panjang cerita dan mode:
          • Generate Langsung: ~30 detik
          • AI Model: 1-3 menit
          • Video panjang: lebih lama

        ❔ Video saya disimpan di mana?
        → Buka menu "Video Saya" untuk lihat semua video.
          Atau cek folder:
          /Android/data/com.yad.videoeditor/files/

        ❔ Bisa share video ke media sosial?
        → Ya! Buka video → tap tombol Share
          → pilih aplikasi tujuan

        ═══════════════════════════════════════
        🎨 TENTANG APLIKASI
        ═══════════════════════════════════════

        Nama: YAD Video Editor
        Versi: 14.0.0
        Dibuat oleh: KARYADI CODING KARYADI

        Terima kasih sudah menggunakan aplikasi ini.
        Semoga bermanfaat! 🙏

        ═══════════════════════════════════════
        © 2026 KARYADI CODING KARYADI
        ═══════════════════════════════════════
        """.trimIndent()
    }
}
