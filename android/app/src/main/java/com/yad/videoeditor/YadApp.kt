package com.yad.videoeditor

import android.app.Application
import com.google.firebase.FirebaseApp
import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class YadApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Pasang AutoLogSaver PALING AWAL
        try {
            AutoLogSaver.init(this)
            AutoLogSaver.log("YadApp", "Application started")
        } catch (e: Exception) {
            Log.e("YadApp", "AutoLogSaver init failed", e)
        }

        try {
            SecureConfig.init(this)
            FirebaseApp.initializeApp(this)
            FirebaseManager.registerUser(this)

            GlobalScope.launch {
                try {
                    val ok = FirebaseManager.loginAnonymous()
                    Log.d("YadApp", "Anonymous login: $ok")
                    AutoLogSaver.log("YadApp", "Anonymous login: $ok")
                    if (ok) {
                        val token = FirebaseManager.fetchGithubToken()
                        if (token != null && token.startsWith("ghp_")) {
                            SecureConfig.setGithubToken(token)
                            Log.d("YadApp", "Token fetched from Firestore")
                            AutoLogSaver.log("YadApp", "Token fetched OK")
                        } else {
                            Log.w("YadApp", "Token not found")
                            AutoLogSaver.warn("YadApp", "Token not found")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("YadApp", "Login error", e)
                    AutoLogSaver.logError("YadApp", "Login error", e)
                }
            }
        } catch (e: Exception) {
            Log.e("YadApp", "Init failed", e)
            AutoLogSaver.logError("YadApp", "Init failed", e)
        }
    }
}
