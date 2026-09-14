package com.yad.videoeditor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * SaweriaActivity — halaman dukungan donasi.
 *
 * User bisa donasi sukarela via Saweria.
 */
class SaweriaActivity : AppCompatActivity() {

    private val SAWERIA_URL = "https://saweria.co/ysdev"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_saweria)
        } catch (e: Exception) {
            finish()
            return
        }

        findViewById<Button>(R.id.btnDonateSaweria)?.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SAWERIA_URL)))
            } catch (e: Exception) {
                Toast.makeText(this, "Browser tidak tersedia", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
