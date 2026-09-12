package com.yad.videoeditor

import android.app.Application
import android.util.Log

class YadApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            SecureConfig.init(this)
            FirebaseManager.registerUser(this)
            Log.d("YadApp", "Initialized")
        } catch (e: Exception) {
            Log.e("YadApp", "Init failed", e)
        }
    }
}
