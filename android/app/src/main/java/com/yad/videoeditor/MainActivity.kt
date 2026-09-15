package com.yad.videoeditor

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Video Saya
        findViewById<CardView>(R.id.cardVideoSaya)?.setOnClickListener {
            try {
                startActivity(Intent(this, VideoListActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(this, "Fitur Video Saya", Toast.LENGTH_SHORT).show()
            }
        }

        // Video Editor
        findViewById<CardView>(R.id.cardVideoEditor)?.setOnClickListener {
            try {
                startActivity(Intent(this, VideoEditorActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(this, "Fitur Video Editor", Toast.LENGTH_SHORT).show()
            }
        }

        // AI Text to Video
        findViewById<CardView>(R.id.cardAiTextToVideo)?.setOnClickListener {
            try {
                startActivity(Intent(this, TextToVideoActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(this, "Fitur AI Text to Video", Toast.LENGTH_SHORT).show()
            }
        }

        // Credit
        findViewById<CardView>(R.id.cardCredit)?.setOnClickListener {
            try {
                startActivity(Intent(this, CreditActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(this, "Fitur Credit", Toast.LENGTH_SHORT).show()
            }
        }

        // Support
        findViewById<CardView>(R.id.cardSupport)?.setOnClickListener {
            try {
                startActivity(Intent(this, SaweriaActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(this, "Fitur Dukung Developer", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
