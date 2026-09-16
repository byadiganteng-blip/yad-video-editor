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

class YadFcmService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "YadFcmService"
        private const val CHANNEL_ID = "yad_admin_notif"
        private const val CHANNEL_NAME = "Notifikasi Admin"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val prefix = if (token.length > 20) token.substring(0, 20) + "..." else token
        Tracker.fcmEvent("token_received", mapOf("token_prefix" to prefix))

        try {
            val uid = FirebaseManager.getCurrentUserUid()
            if (uid != null) {
                GlobalScope.launch {
                    try {
                        val ok = FirebaseManager.addFcmToken(uid, token)
                        Tracker.fcmEvent("token_saved", mapOf(
                            "uid" to uid, "success" to ok
                        ))
                    } catch (e: Exception) {
                        Tracker.error(TAG, "save_token_failed", "", e)
                    }
                }
            } else {
                Tracker.warn(TAG, "no_uid_for_token", "user not logged in")
            }
        } catch (e: Exception) {
            Tracker.error(TAG, "on_new_token_failed", "", e)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        try {
            val title = message.notification?.title
                ?: message.data["title"]
                ?: "Pesan dari Admin"
            val body = message.notification?.body
                ?: message.data["body"]
                ?: ""

            Tracker.fcmEvent("message_received", mapOf(
                "title" to title,
                "body_preview" to (if (body.length > 50) body.substring(0, 50) + "..." else body),
                "from" to (message.from ?: "unknown"),
                "has_notification" to (message.notification != null),
                "data_keys" to message.data.keys.joinToString(",")
            ))

            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            ensureChannel()

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notifId = (System.currentTimeMillis() % 100000).toInt()
            nm.notify(notifId, notification)

            Tracker.fcmEvent("notification_shown", mapOf(
                "notif_id" to notifId,
                "title" to title
            ))
        } catch (e: Exception) {
            Tracker.error(TAG, "on_message_failed", "", e)
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME,
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
