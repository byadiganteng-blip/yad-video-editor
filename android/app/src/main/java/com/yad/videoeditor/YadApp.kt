package com.yad.videoeditor

import android.app.Application
import com.google.firebase.FirebaseApp
import android.util.Log

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
            // Login anonymous DIHAPUS — user login via Google di LoginActivity
        } catch (e: Exception) {
            Log.e("YadApp", "Init failed", e)
            AutoLogSaver.logError("YadApp", "Init failed", e)
        }
    }
}
