package com.yad.videoeditor

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

/**
 * SplashActivity — entry point yang menentukan:
 *  - Kalau sudah login → MainActivity
 *  - Kalau belum login → LoginActivity
 *
 * User TIDAK perlu login ulang setiap kali buka aplikasi.
 * Firebase Auth otomatis simpan sesi.
 */
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SecureConfig.init(this)

        try { setContentView(R.layout.activity_splash) }
        catch (e: Exception) {
            // Kalau layout tidak ada, langsung routing
        }

        // Delay minimal untuk branding
        Handler(Looper.getMainLooper()).postDelayed({
            route()
        }, 800)
    }

    private fun route() {
        // Cek login status (sinkron, tidak perlu coroutine)
        val loggedIn = try {
            FirebaseManager.isLoggedIn()
        } catch (e: Exception) {
            false
        }

        if (loggedIn) {
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
        }
        finish()
    }
}
