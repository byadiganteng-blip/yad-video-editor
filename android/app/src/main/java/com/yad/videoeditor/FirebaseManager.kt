package com.yad.videoeditor

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

object FirebaseManager {
    private const val TAG = "FirebaseManager"
    private const val DB_URL = "https://yad-video-editor-default-rtdb.asia-southeast1.firebasedatabase.app"

    private val db: FirebaseDatabase by lazy {
        FirebaseDatabase.getInstance(DB_URL)
    }

    fun getDeviceId(context: Context): String {
        return try {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: "unknown"
        } catch (e: Exception) { "unknown" }
    }

    fun getDeviceModel(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL}"
    }

    // ============================================================
    //  CONFIG
    // ============================================================
    data class AppConfig(
        val freeLimitPerDay: Int = 3,
        val premiumLimitPerDay: Int = -1,
        val enabledModels: List<String> = listOf(),
        val defaultModel: String = "waifu",
        val showAds: Boolean = true,
        val forceUpdate: Boolean = false,
        val minVersionCode: Int = 13,
        val updateUrl: String = "",
        val messageBanner: String = "",
        val maintenanceMode: Boolean = false
    )

    fun configFlow(): Flow<AppConfig> = callbackFlow {
        val ref = db.getReference("config")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val cfg = AppConfig(
                        freeLimitPerDay = snapshot.child("free_limit_per_day")
                            .getValue(Int::class.java) ?: 3,
                        premiumLimitPerDay = snapshot.child("premium_limit_per_day")
                            .getValue(Int::class.java) ?: -1,
                        enabledModels = snapshot.child("enabled_models")
                            .children.mapNotNull { it.getValue(String::class.java) },
                        defaultModel = snapshot.child("default_model")
                            .getValue(String::class.java) ?: "waifu",
                        showAds = snapshot.child("show_ads")
                            .getValue(Boolean::class.java) ?: true,
                        forceUpdate = snapshot.child("force_update")
                            .getValue(Boolean::class.java) ?: false,
                        minVersionCode = snapshot.child("min_version_code")
                            .getValue(Int::class.java) ?: 13,
                        updateUrl = snapshot.child("update_url")
                            .getValue(String::class.java) ?: "",
                        messageBanner = snapshot.child("message_banner")
                            .getValue(String::class.java) ?: "",
                        maintenanceMode = snapshot.child("maintenance_mode")
                            .getValue(Boolean::class.java) ?: false
                    )
                    trySend(cfg)
                } catch (e: Exception) {
                    Log.e(TAG, "Parse config error: ${e.message}")
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun updateConfig(key: String, value: Any) {
        db.getReference("config").child(key).setValue(value)
    }

    // ============================================================
    //  USER
    // ============================================================
    data class UserData(
        val isPremium: Boolean = false,
        val dailyCount: Int = 0,
        val lastUsed: Long = 0,
        val deviceModel: String = "",
        val installedAt: Long = 0,
        val appVersion: Int = 13,
        val isAdmin: Boolean = false
    )

    fun registerUser(context: Context) {
        val deviceId = getDeviceId(context)
        val ref = db.getReference("users").child(deviceId)
        ref.child("device_model").setValue(getDeviceModel())
        ref.child("installed_at").setValue(System.currentTimeMillis())
        ref.child("app_version").setValue(13)
        ref.child("last_used").setValue(System.currentTimeMillis())
    }

    fun updateLastUsed(context: Context) {
        val deviceId = getDeviceId(context)
        db.getReference("users").child(deviceId)
            .child("last_used").setValue(System.currentTimeMillis())
    }

    fun getUserFlow(context: Context): Flow<UserData?> = callbackFlow {
        val deviceId = getDeviceId(context)
        val ref = db.getReference("users").child(deviceId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(UserData::class.java))
            }
            override fun onCancelled(error: DatabaseError) { trySend(null) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun resetUserLimit(context: Context) {
        val deviceId = getDeviceId(context)
        db.getReference("users").child(deviceId).child("daily_count").setValue(0)
    }

    fun setPremium(context: Context, premium: Boolean) {
        val deviceId = getDeviceId(context)
        db.getReference("users").child(deviceId)
            .child("is_premium").setValue(premium)
    }

    // ============================================================
    //  BROADCAST
    // ============================================================
    data class BroadcastMsg(
        val message: String = "",
        val active: Boolean = false,
        val createdAt: Long = 0
    )

    fun sendBroadcast(message: String) {
        val data = mapOf(
            "message" to message,
            "active" to true,
            "created_at" to System.currentTimeMillis()
        )
        db.getReference("broadcast").setValue(data)
    }

    fun broadcastFlow(): Flow<BroadcastMsg?> = callbackFlow {
        val ref = db.getReference("broadcast")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(BroadcastMsg::class.java))
            }
            override fun onCancelled(error: DatabaseError) { trySend(null) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // ============================================================
    //  STATS
    // ============================================================
    fun incrementCounter(key: String) {
        val ref = db.getReference("stats").child(key)
        ref.runTransaction(object : Transaction.Handler {
            override fun doTransaction(current: MutableData): Transaction.Result {
                val count = current.getValue(Int::class.java) ?: 0
                current.value = count + 1
                return Transaction.success(current)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean,
                                    snapshot: DataSnapshot?) {}
        })
    }

    fun statsFlow(): Flow<Map<String, Int>?> = callbackFlow {
        val ref = db.getReference("stats")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val map = mutableMapOf<String, Int>()
                for (child in snapshot.children) {
                    map[child.key ?: ""] = child.getValue(Int::class.java) ?: 0
                }
                trySend(map)
            }
            override fun onCancelled(error: DatabaseError) { trySend(null) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun usersFlow(): Flow<List<Pair<String, UserData>>> = callbackFlow {
        val ref = db.getReference("users")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Pair<String, UserData>>()
                for (child in snapshot.children) {
                    val data = child.getValue(UserData::class.java)
                    if (data != null) list.add(child.key ?: "" to data)
                }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) { trySend(null) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}
