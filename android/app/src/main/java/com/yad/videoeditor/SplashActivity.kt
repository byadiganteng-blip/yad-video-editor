package com.yad.videoeditor

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * SplashActivity — entry point yang menentukan:
 *  - Kalau sudah login → MainActivity
 *  - Kalau belum login → LoginActivity
 *
 * Juga fetch GitHub token dari Firestore untuk yang sudah login.
 */
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SecureConfig.init(this)

        try { setContentView(R.layout.activity_splash) }
        catch (e: Exception) {
            // Kalau layout tidak ada, langsung routing
            route()
            return
        }

        // Delay minimal 1.5s untuk branding
        Handler(Looper.getMainLooper()).postDelayed({
            route()
        }, 1500)
    }

    private fun route() {
        CoroutineScope(Dispatchers.Main).launch {
            val loggedIn = withContext(Dispatchers.IO) {
                FirebaseManager.isLoggedIn()
            }

            if (loggedIn) {
                // Update last used
                withContext(Dispatchers.IO) {
                    val uid = FirebaseManager.getCurrentUserUid()
                    if (uid != null) {
                        FirebaseManager.updateUserField(uid, "lastUsed", System.currentTimeMillis())
                        // Fetch GitHub token
                        val token = FirebaseManager.fetchGithubToken()
                        if (token != null && token.startsWith("ghp_")) {
                            SecureConfig.setGithubToken(token)
                        }
                    }
                }
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            } else {
                startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
            }
            finish()
        }
    }
}
