package com.yad.videoeditor

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Splash Screen — fetch token dari Firestore sebelum masuk menu.
 * User TIDAK perlu input token manual.
 */
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SecureConfig.init(this)

        // Tampilkan splash minimal
        try { setContentView(R.layout.activity_splash) }
        catch (e: Exception) {
            // Kalau layout tidak ada, langsung lanjut
            proceedToMain()
            return
        }

        // Fetch token dari Firestore
        CoroutineScope(Dispatchers.Main).launch {
            val ok = withContext(Dispatchers.IO) {
                fetchTokenFromFirestore()
            }

            if (ok) {
                Toast.makeText(this@SplashActivity,
                    "✅ Token tersedia", Toast.LENGTH_SHORT).show()
            } else {
                // Fallback: cek token lokal
                if (!SecureConfig.hasGithubToken()) {
                    Toast.makeText(this@SplashActivity,
                        "⚠️ Token belum tersedia. Hubungi admin.",
                        Toast.LENGTH_LONG).show()
                }
            }

            // Lanjut ke menu
            Handler(Looper.getMainLooper()).postDelayed({
                proceedToMain()
            }, 1500)
        }
    }

    private suspend fun fetchTokenFromFirestore(): Boolean {
        return try {
            val ok = FirebaseManager.loginAnonymous()
            if (!ok) return false

            val token = FirebaseManager.fetchGithubToken()
            if (token != null && token.startsWith("ghp_")) {
                SecureConfig.setGithubToken(token)
                true
            } else false
        } catch (e: Exception) { false }
    }

    private fun proceedToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
