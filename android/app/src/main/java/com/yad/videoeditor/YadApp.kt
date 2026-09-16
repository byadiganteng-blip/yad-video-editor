package com.yad.videoeditor

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
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

            // Subscribe topik FCM "all_users" untuk terima notif dari admin
            try {
                FirebaseMessaging.getInstance().subscribeToTopic("all_users")
                    .addOnSuccessListener {
                        AutoLogSaver.log("YadApp", "Subscribed to topic all_users")
                    }
                    .addOnFailureListener { e ->
                        AutoLogSaver.logError("YadApp", "Subscribe topic failed", e)
                    }
            } catch (e: Exception) {
                AutoLogSaver.logError("YadApp", "FCM subscribe exception", e)
            }
        } catch (e: Exception) {
            Log.e("YadApp", "Init failed", e)
            AutoLogSaver.logError("YadApp", "Init failed", e)
        }
    }
}
