package com.yad.videoeditor

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class AdminPushNotifActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_push_notif)

        val etDevice = findViewById<EditText>(R.id.etPushDevice)
        val etMsg = findViewById<EditText>(R.id.etPushMsg)
        val btnSend = findViewById<Button>(R.id.btnSendPush)

        btnSend?.setOnClickListener {
            val device = etDevice?.text?.toString()?.trim() ?: ""
            val msg = etMsg?.text?.toString()?.trim() ?: ""
            if (device.isEmpty() || msg.isEmpty()) {
                Toast.makeText(this, "Isi device ID & pesan", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            FirebaseManager.sendBroadcast("[$device] $msg") { ok ->
                Toast.makeText(this,
                    if (ok) "✅ Notif terkirim" else "❌ Gagal",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }
}
