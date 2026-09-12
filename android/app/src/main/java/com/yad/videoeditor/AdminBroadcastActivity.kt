package com.yad.videoeditor

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class AdminBroadcastActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { setContentView(R.layout.activity_admin_broadcast) }
        catch (e: Exception) { finish(); return }

        val etMsg = findViewById<EditText>(R.id.etBroadcastMsg)
        findViewById<Button>(R.id.btnSendBroadcast)?.setOnClickListener {
            val msg = etMsg.text.toString().trim()
            if (msg.isNotEmpty()) {
                FirebaseManager.sendBroadcast(msg)
                Toast.makeText(this, "📢 Terkirim", Toast.LENGTH_SHORT).show()
                etMsg.text.clear()
            }
        }
    }
}
