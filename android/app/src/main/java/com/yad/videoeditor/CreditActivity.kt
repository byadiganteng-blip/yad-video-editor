package com.yad.videoeditor

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CreditActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { setContentView(R.layout.activity_credit) }
        catch (e: Exception) { finish(); return }

        val tvInfo = findViewById<TextView>(R.id.tvCredit)
        tvInfo.text = """
🎬 AI TXT TO VIDEO + EDITOR

✨ KARYA ASLI

👨‍💻 DEVELOPER & CREATOR
Nama   : KARYADI
Peran  : Founder, Developer, Designer
Motto  : Coding by KARYADI, Kualitas oleh KARYADI

🎨 TENTANG APLIKASI
Mengubah cerita tulisan menjadi video AI
berkualitas tinggi — otomatis dan cepat.

🚀 TEKNOLOGI
• Stable Diffusion AI (10 Model)
• Edge TTS Neural Voice
• GitHub Actions Cloud
• MoviePy Video Engine
• Kotlin Native Android

💝 SPECIAL THANKS
• Keluarga tercinta
• Pengguna setia
• Komunitas AI Indonesia
• Allah SWT yang Maha Kuasa

📜 COPYRIGHT
© 2026 KARYADI. All Rights Reserved.
Dilarang memperjualbelikan aplikasi ini
tanpa izin tertulis dari pembuat.

📞 KONTAK
Email  : ynuraini686@gmail.com
GitHub : byadiganteng-blip

"Setiap cerita layak jadi video."
       — KARYADI
        """.trimIndent()
    }
}
