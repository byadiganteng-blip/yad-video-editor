package com.yad.videoeditor

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AdminUserListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { setContentView(R.layout.activity_admin_user_list) }
        catch (e: Exception) { finish(); return }

        val tv = findViewById<TextView>(R.id.tvUserList)
        val sdf = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())

        lifecycleScope.launch {
            FirebaseManager.usersFlow().collectLatest { list ->
                if (list == null) return@collectLatest
                val sb = StringBuilder()
                sb.append("👥 TOTAL: ${list.size} user\n\n")
                list.forEachIndexed { i, (id, u) ->
                    sb.append("${i+1}. ${u.deviceModel}\n")
                    sb.append("   ID: ${id.take(8)}...\n")
                    sb.append("   Premium: ${if (u.isPremium) "✅" else "❌"}\n")
                    sb.append("   Daily: ${u.dailyCount}\n")
                    sb.append("   Last: ${sdf.format(Date(u.lastUsed))}\n\n")
                }
                tv.text = sb.toString()
            }
        }
    }
}
