package com.yad.videoeditor

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * YadFcmService — terima FCM message dari admin.
 *
 * Behavior:
 *  - onNewToken: simpan token ke Firestore (untuk keperluan tracking)
 *  - onMessageReceived: tampilkan notifikasi di system tray
 *
 * Catatan: FCM juga kirim "notification" payload otomatis
 * kalau app di background — tapi kita tangani sendiri di sini
 * supaya bisa custom (judul, pesan, ikon, deep link).
 */
class YadFcmService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "YadFcmService"
        private const val CHANNEL_ID = "yad_admin_notif"
        private const val CHANNEL_NAME = "Notifikasi Admin"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        AutoLogSaver.log(TAG, "FCM token received")

        // Simpan token ke Firestore
        try {
            val uid = FirebaseManager.getCurrentUserUid()
            if (uid != null) {
                kotlinx.coroutines.GlobalScope.launch {
                    try {
                        FirebaseManager.addFcmToken(uid, token)
                    } catch (e: Exception) {
                        AutoLogSaver.logError(TAG, "save token failed", e)
                    }
                }
            }
        } catch (e: Exception) {
            AutoLogSaver.logError(TAG, "onNewToken failed", e)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        AutoLogSaver.log(TAG, "FCM message received")

        try {
            // Ambil judul + pesan
            val title = message.notification?.title
                ?: message.data["title"]
                ?: "Pesan dari Admin"
            val body = message.notification?.body
                ?: message.data["body"]
                ?: ""

            // Klik notif → buka MainActivity
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Buat channel (Android 8+)
            ensureChannel()

            // Bangun notifikasi
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            // Tampilkan
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notifId = (System.currentTimeMillis() % 100000).toInt()
            nm.notify(notifId, notification)

            AutoLogSaver.log(TAG, "Notification shown: $title")
        } catch (e: Exception) {
            AutoLogSaver.logError(TAG, "onMessageReceived failed", e)
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi dari admin"
                enableVibration(true)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}
