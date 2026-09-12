package com.yad.videoeditor

import android.app.Application
import android.util.Log

/**
 * Application class
 * Created by KARYADI, Coding by KARYADI
 */
class YadApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            SecureConfig.init(this)
            Log.d("YadApp", "Initialized")
        } catch (e: Exception) {
            Log.e("YadApp", "Init failed", e)
        }
    }
}
