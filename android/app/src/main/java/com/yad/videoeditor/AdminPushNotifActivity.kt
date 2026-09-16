package com.yad.videoeditor

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * AdminPushNotifActivity — kirim notifikasi ke semua user via FCM.
 *
 * Mekanisme:
 *  1. Admin isi judul + pesan
 *  2. Klik "Kirim"
 *  3. App trigger GitHub Actions workflow `send-fcm.yml`
 *  4. Workflow pakai FCM_SERVICE_ACCOUNT untuk kirim ke FCM HTTP v1
 *  5. FCM broadcast ke topik "all_users"
 *  6. Semua user terima notif
 */
class AdminPushNotifActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_push_notif)

        val etTitle = findViewById<EditText>(R.id.etPushTitle)
        val etMsg = findViewById<EditText>(R.id.etPushMsg)
        val btnSend = findViewById<Button>(R.id.btnSendPush)

        btnSend?.setOnClickListener {
            val title = etTitle?.text?.toString()?.trim() ?: ""
            val msg = etMsg?.text?.toString()?.trim() ?: ""

            if (title.isEmpty()) {
                Toast.makeText(this, "Isi judul notifikasi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (msg.isEmpty()) {
                Toast.makeText(this, "Isi pesan notifikasi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Disable tombol sementara
            btnSend.isEnabled = false
            btnSend.text = "⏳ Mengirim..."

            lifecycleScope.launch {
                try {
                    val (ok, resultMsg) = AdminApi.triggerSendFcm(title, msg)
                    btnSend.isEnabled = true
                    btnSend.text = "📤 Kirim Notifikasi"

                    if (ok) {
                        Toast.makeText(this@AdminPushNotifActivity,
                            "✅ $resultMsg", Toast.LENGTH_LONG).show()
                        etTitle.text.clear()
                        etMsg.text.clear()
                    } else {
                        Toast.makeText(this@AdminPushNotifActivity,
                            "❌ Gagal: $resultMsg", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    btnSend.isEnabled = true
                    btnSend.text = "📤 Kirim Notifikasi"
                    Toast.makeText(this@AdminPushNotifActivity,
                        "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
