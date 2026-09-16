package com.yad.videoeditor

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import android.util.Log

class YadApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Init AutoLogSaver + Tracker PALING AWAL
        try {
            AutoLogSaver.init(this)
            Tracker.init(this)
            Tracker.lifecycle("YadApp", "onCreate")
            Tracker.info("YadApp", "app_started", data = mapOf(
                "version_code" to BuildConfig.VERSION_CODE,
                "version_name" to BuildConfig.VERSION_NAME
            ))
        } catch (e: Exception) {
            Log.e("YadApp", "AutoLogSaver/Tracker init failed", e)
        }

        try {
            SecureConfig.init(this)
            FirebaseApp.initializeApp(this)
            Tracker.info("YadApp", "firebase_initialized")

            // Cek apakah user sudah login
            try {
                val isLoggedIn = FirebaseManager.isLoggedIn()
                Tracker.authEvent("firebase", if (isLoggedIn) "already_logged_in" else "not_logged_in")

                if (isLoggedIn) {
                    val uid = FirebaseManager.getCurrentUserUid()
                    Tracker.authEvent("firebase", "current_uid", mapOf("uid" to uid))
                }
            } catch (e: Exception) {
                Tracker.error("YadApp", "check_login_failed", "", e)
            }

            // Subscribe topik FCM "all_users"
            try {
                FirebaseMessaging.getInstance().subscribeToTopic("all_users")
                    .addOnSuccessListener {
                        Tracker.fcmEvent("subscribe_success", mapOf("topic" to "all_users"))
                    }
                    .addOnFailureListener { e ->
                        Tracker.fcmEvent("subscribe_failed", mapOf(
                            "topic" to "all_users",
                            "error" to (e.message ?: "unknown")
                        ))
                    }
            } catch (e: Exception) {
                Tracker.error("YadApp", "fcm_subscribe_exception", "", e)
            }

            // Ambil FCM token sekarang
            try {
                FirebaseMessaging.getInstance().token
                    .addOnSuccessListener { token ->
                        val prefix = if (token.length > 20) token.substring(0, 20) + "..." else token
                        Tracker.fcmEvent("token_available", mapOf("token_prefix" to prefix))
                    }
                    .addOnFailureListener { e ->
                        Tracker.error("YadApp", "get_token_failed", "", e)
                    }
            } catch (e: Exception) {
                Tracker.error("YadApp", "get_token_exception", "", e)
            }
        } catch (e: Exception) {
            Log.e("YadApp", "Init failed", e)
            Tracker.error("YadApp", "init_failed", "", e)
        }
    }
}
