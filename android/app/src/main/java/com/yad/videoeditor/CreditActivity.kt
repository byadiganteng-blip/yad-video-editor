package com.yad.videoeditor

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Halaman Credit — dibuat dengan bangga
 */
class CreditActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { setContentView(R.layout.activity_credit) }
        catch (e: Exception) { finish(); return }

        val tvCredit = findViewById<TextView>(R.id.tvCreditText)
        tvCredit.text = """
            ╔══════════════════════════════════════╗
            ║   🎬 YAD VIDEO EDITOR v11.0         ║
            ║   AI Story-to-Video Generator        ║
            ╚══════════════════════════════════════╝
            
            ✨ KARYA ASLI
            ─────────────────────────────────────
            
            👨‍💻 DEVELOPER & CREATOR
            ═══════════════════════════════════════
            
            Nama      : KARYADI
            Peran     : Founder, Developer, Designer
            Motto     : "Coding by KARYADI, 
                        Kualitas oleh KARYADI"
            
            ─────────────────────────────────────
            
            🎨 TENTANG APLIKASI
            ═══════════════════════════════════════
            
            YAD Video Editor adalah aplikasi
            revolusioner yang mengubah cerita
            tulisan menjadi video AI berkualitas
            tinggi — otomatis, cepat, dan gratis.
            
            ─────────────────────────────────────
            
            🚀 TEKNOLOGI
            ═══════════════════════════════════════
            
            • Stable Diffusion AI
            • Edge TTS Neural Voice
            • GitHub Actions Cloud
            • MoviePy Video Engine
            • Kotlin Native Android
            
            ─────────────────────────────────────
            
            💝 SPECIAL THANKS
            ═══════════════════════════════════════
            
            • Keluarga tercinta
            • Pengguna setia YAD
            • Komunitas AI Indonesia
            • Allah SWT yang Maha Kuasa
            
            ─────────────────────────────────────
            
            📜 COPYRIGHT
            ═══════════════════════════════════════
            
            © 2026 KARYADI. All Rights Reserved.
            
            Dilarang memperjualbelikan aplikasi
            ini tanpa izin tertulis dari pembuat.
            
            ─────────────────────────────────────
            
            📞 KONTAK
            ═══════════════════════════════════════
            
            Email  : ynuraini686@gmail.com
            GitHub : byadiganteng-blip
            
            ─────────────────────────────────────
            
            "Setiap cerita layak jadi video."
                     — KARYADI
            
            ═══════════════════════════════════════
            Dibuat dengan ❤️ di Indonesia
            ═══════════════════════════════════════
        """.trimIndent()
    }
}
