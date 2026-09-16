package com.yad.videoeditor

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CreditActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { setContentView(R.layout.activity_credit) }
        catch (e: Exception) { finish(); return }

        // Tampil interstitial
        try { StartAppHelper.showInterstitial(this) {} } catch (_: Exception) {}

        // Layout activity_credit.xml punya ID "tvCredit"
        val tvInfo = findViewById<TextView>(R.id.tvCredit)
        tvInfo.text = """
            ╔════════════════════════════════════════╗
            ║   🎬 AI TXT TO VIDEO + EDITOR         ║
            ║   AI Story-to-Video Generator         ║
            ╚════════════════════════════════════════╝

            ✨ KARYA ASLI

            👨‍💻 DEVELOPER & CREATOR
            ────────────────────────────────────────
            Nama  : KARYADI
            Peran : Founder, Developer, Designer
            Motto : Coding by KARYADI,
                    Kualitas oleh KARYADI

            🎨 TENTANG APLIKASI
            ────────────────────────────────────────
            AI TXT to Video + Editor mengubah
            cerita tulisan menjadi video AI
            berkualitas tinggi — otomatis & cepat.

            🚀 TEKNOLOGI
            ────────────────────────────────────────
            • Stable Diffusion AI (10 Model)
            • Edge TTS Neural Voice
            • GitHub Actions Cloud
            • MoviePy Video Engine
            • Firebase + Kotlin Android

            💝 SPECIAL THANKS
            ────────────────────────────────────────
            • Keluarga tercinta
            • Pengguna setia
            • Komunitas AI Indonesia
            • Allah SWT yang Maha Kuasa

            📜 COPYRIGHT
            ────────────────────────────────────────
            © 2026 KARYADI. All Rights Reserved.

            📞 KONTAK
            ────────────────────────────────────────
            Email  : ynuraini686@gmail.com
            GitHub : byadiganteng-blip

            "Setiap cerita layak jadi video."
                     — KARYADI

            Dibuat dengan ❤️ di Indonesia
        """.trimIndent()
    }
}
