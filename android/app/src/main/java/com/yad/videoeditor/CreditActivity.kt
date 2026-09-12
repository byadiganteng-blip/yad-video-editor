package com.yad.videoeditor

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class CreditActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { setContentView(R.layout.activity_credit) } catch (e: Exception) { finish() }
    }
}
