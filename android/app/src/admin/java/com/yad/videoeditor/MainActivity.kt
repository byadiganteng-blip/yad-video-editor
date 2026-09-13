package com.yad.videoeditor

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

/**
 * MainActivity Admin — panel kontrol YAD Admin
 * Versi minimal & bersih (rewrite).
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Card: Daftar User
        val cardUserList = findViewById<CardView>(R.id.cardAdminUserList)
        cardUserList?.setOnClickListener {
            Toast.makeText(this, "Buka Daftar User", Toast.LENGTH_SHORT).show()
        }

        // Card: Statistik
        val cardStats = findViewById<CardView>(R.id.cardAdminStats)
        cardStats?.setOnClickListener {
            Toast.makeText(this, "Buka Statistik", Toast.LENGTH_SHORT).show()
        }

        // Card: Broadcast
        val cardBroadcast = findViewById<CardView>(R.id.cardAdminBroadcast)
        cardBroadcast?.setOnClickListener {
            Toast.makeText(this, "Buka Broadcast", Toast.LENGTH_SHORT).show()
        }

        // Card: Push Notif
        val cardPushNotif = findViewById<CardView>(R.id.cardAdminPushNotif)
        cardPushNotif?.setOnClickListener {
            Toast.makeText(this, "Buka Push Notif", Toast.LENGTH_SHORT).show()
        }

        // Card: Force Update
        val cardForceUpdate = findViewById<CardView>(R.id.cardAdminForceUpdate)
        cardForceUpdate?.setOnClickListener {
            Toast.makeText(this, "Buka Force Update", Toast.LENGTH_SHORT).show()
        }

        // Card: Maintenance
        val cardMaintenance = findViewById<CardView>(R.id.cardAdminMaintenance)
        cardMaintenance?.setOnClickListener {
            Toast.makeText(this, "Buka Maintenance", Toast.LENGTH_SHORT).show()
        }

        // Card: Set Token
        val cardToken = findViewById<CardView>(R.id.cardAdminToken)
        cardToken?.setOnClickListener {
            Toast.makeText(this, "Buka Set Token", Toast.LENGTH_SHORT).show()
        }

        // Card: Manage Models
        val cardModels = findViewById<CardView>(R.id.cardAdminModels)
        cardModels?.setOnClickListener {
            Toast.makeText(this, "Buka Manage Models", Toast.LENGTH_SHORT).show()
        }

        // Card: Set Config
        val cardConfig = findViewById<CardView>(R.id.cardAdminConfig)
        cardConfig?.setOnClickListener {
            Toast.makeText(this, "Buka Set Config", Toast.LENGTH_SHORT).show()
        }

        // Card: Logs
        val cardLogs = findViewById<CardView>(R.id.cardAdminLogs)
        cardLogs?.setOnClickListener {
            Toast.makeText(this, "Buka Logs", Toast.LENGTH_SHORT).show()
        }

        // Card: Backup
        val cardBackup = findViewById<CardView>(R.id.cardAdminBackup)
        cardBackup?.setOnClickListener {
            Toast.makeText(this, "Buka Backup", Toast.LENGTH_SHORT).show()
        }

        // Card: Instructions
        val cardInstructions = findViewById<CardView>(R.id.cardInstructions)
        cardInstructions?.setOnClickListener {
            Toast.makeText(this, "Buka Panduan", Toast.LENGTH_SHORT).show()
        }

        // Card: Logout
        val cardLogout = findViewById<CardView>(R.id.cardAdminLogout)
        cardLogout?.setOnClickListener {
            Toast.makeText(this, "Logout", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
