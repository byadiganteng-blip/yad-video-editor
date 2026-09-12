package com.yad.videoeditor

import android.app.Application
import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class YadApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            SecureConfig.init(this)
            FirebaseManager.registerUser(this)

            GlobalScope.launch {
                val ok = FirebaseManager.loginAnonymous()
                Log.d("YadApp", "Anonymous login: $ok")
                if (ok) {
                    val token = FirebaseManager.fetchGithubToken()
                    if (token != null && token.startsWith("ghp_")) {
                        SecureConfig.setGithubToken(token)
                        Log.d("YadApp", "Token fetched from Firestore")
                    } else {
                        Log.w("YadApp", "Token not found")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("YadApp", "Init failed", e)
        }
    }
}
