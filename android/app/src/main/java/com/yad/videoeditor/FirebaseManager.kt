package com.yad.videoeditor

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object FirebaseManager {
    private const val TAG = "FirebaseManager"

    private val db: FirebaseFirestore by lazy { Firebase.firestore }

    // ============================================================
    //  AUTH ANONYMOUS
    // ============================================================
    suspend fun loginAnonymous(): Boolean {
        return try {
            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser == null) {
                auth.signInAnonymously().await()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Anonymous login failed: ${e.message}")
            false
        }
    }

    // ============================================================
    //  SECRETS — Token GitHub (collection: secrets, doc: github)
    // ============================================================
    suspend fun fetchGithubToken(): String? {
        return try {
            val doc = db.collection("secrets").document("github").get().await()
            doc.getString("token")
        } catch (e: Exception) {
            Log.e(TAG, "Fetch token failed: ${e.message}")
            null
        }
    }

    fun saveGithubToken(token: String, callback: (Boolean) -> Unit = {}) {
        val data = mapOf("token" to token)
        db.collection("secrets").document("github").set(data)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    fun tokenFlow(): Flow<String?> = callbackFlow {
        val ref = db.collection("secrets").document("github")
        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) { trySend(null); return@addSnapshotListener }
            trySend(snapshot?.getString("token"))
        }
        awaitClose { listener.remove() }
    }

    // ============================================================
    //  DEVICE
    // ============================================================
    fun getDeviceId(context: Context): String {
        return try {
            Settings.Secure.getString(context.contentResolver,
                Settings.Secure.ANDROID_ID) ?: "unknown"
        } catch (e: Exception) { "unknown" }
    }

    fun getDeviceModel(): String = "${Build.MANUFACTURER} ${Build.MODEL}"

    // ============================================================
    //  CONFIG — collection: config, doc: main
    // ============================================================
    data class AppConfig(
        val freeLimitPerDay: Int = 3,
        val premiumLimitPerDay: Int = -1,
        val defaultModel: String = "waifu",
        val showAds: Boolean = true,
        val forceUpdate: Boolean = false,
        val minVersionCode: Int = 13,
        val updateUrl: String = "",
        val messageBanner: String = "",
        val maintenanceMode: Boolean = false
    )

    fun configFlow(): Flow<AppConfig> = callbackFlow {
        val ref = db.collection("config").document("main")
        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            val cfg = AppConfig(
                freeLimitPerDay = snapshot.getLong("free_limit_per_day")?.toInt() ?: 3,
                premiumLimitPerDay = snapshot.getLong("premium_limit_per_day")?.toInt() ?: -1,
                defaultModel = snapshot.getString("default_model") ?: "waifu",
                showAds = snapshot.getBoolean("show_ads") ?: true,
                forceUpdate = snapshot.getBoolean("force_update") ?: false,
                minVersionCode = snapshot.getLong("min_version_code")?.toInt() ?: 13,
                updateUrl = snapshot.getString("update_url") ?: "",
                messageBanner = snapshot.getString("message_banner") ?: "",
                maintenanceMode = snapshot.getBoolean("maintenance_mode") ?: false
            )
            trySend(cfg)
        }
        awaitClose { listener.remove() }
    }

    fun updateConfig(key: String, value: Any, callback: (Boolean) -> Unit = {}) {
        db.collection("config").document("main")
            .set(mapOf(key to value), SetOptions.merge())
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    // ============================================================
    //  USER — collection: users, doc: {deviceId}
    // ============================================================
    data class UserData(
        val isPremium: Boolean = false,
        val dailyCount: Long = 0,
        val lastUsed: Long = 0,
        val deviceModel: String = "",
        val installedAt: Long = 0,
        val appVersion: Long = 13,
        val isAdmin: Boolean = false
    )

    fun registerUser(context: Context) {
        val deviceId = getDeviceId(context)
        val data = mapOf(
            "device_model" to getDeviceModel(),
            "installed_at" to System.currentTimeMillis(),
            "app_version" to 13,
            "last_used" to System.currentTimeMillis()
        )
        db.collection("users").document(deviceId)
            .set(data, SetOptions.merge())
    }

    fun updateLastUsed(context: Context) {
        val deviceId = getDeviceId(context)
        db.collection("users").document(deviceId)
            .set(mapOf("last_used" to System.currentTimeMillis()),
                 SetOptions.merge())
    }

    fun getUserFlow(context: Context): Flow<UserData?> = callbackFlow {
        val deviceId = getDeviceId(context)
        val ref = db.collection("users").document(deviceId)
        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) { trySend(null); return@addSnapshotListener }
            val data = snapshot?.toObject(UserData::class.java)
            trySend(data)
        }
        awaitClose { listener.remove() }
    }

    fun resetUserLimit(context: Context) {
        val deviceId = getDeviceId(context)
        db.collection("users").document(deviceId)
            .set(mapOf("daily_count" to 0), SetOptions.merge())
    }

    fun setPremium(context: Context, premium: Boolean) {
        val deviceId = getDeviceId(context)
        db.collection("users").document(deviceId)
            .set(mapOf("is_premium" to premium), SetOptions.merge())
    }

    // ============================================================
    //  BROADCAST — collection: broadcast, doc: main
    // ============================================================
    data class BroadcastMsg(
        val message: String = "",
        val active: Boolean = false,
        val createdAt: Long = 0
    )

    fun sendBroadcast(message: String, callback: (Boolean) -> Unit = {}) {
        val data = mapOf(
            "message" to message,
            "active" to true,
            "created_at" to System.currentTimeMillis()
        )
        db.collection("broadcast").document("main").set(data)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    fun broadcastFlow(): Flow<BroadcastMsg?> = callbackFlow {
        val ref = db.collection("broadcast").document("main")
        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) { trySend(null); return@addSnapshotListener }
            trySend(snapshot?.toObject(BroadcastMsg::class.java))
        }
        awaitClose { listener.remove() }
    }

    // ============================================================
    //  STATS — collection: stats, doc: main
    // ============================================================
    fun incrementCounter(key: String) {
        val ref = db.collection("stats").document("main")
        db.runTransaction { transaction ->
            val snapshot = transaction.get(ref)
            val current = snapshot.getLong(key) ?: 0
            transaction.set(ref, mapOf(key to current + 1), SetOptions.merge())
        }
    }

    fun statsFlow(): Flow<Map<String, Long>?> = callbackFlow {
        val ref = db.collection("stats").document("main")
        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) { trySend(null); return@addSnapshotListener }
            val map = snapshot?.data?.mapValues { (it.value as? Long) ?: 0L }
            trySend(map)
        }
        awaitClose { listener.remove() }
    }

    // ============================================================
    //  USERS LIST — untuk admin
    // ============================================================
    fun usersFlow(): Flow<List<Pair<String, UserData>>> = callbackFlow {
        val ref = db.collection("users")
        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) { trySend(emptyList()); return@addSnapshotListener }
            val list = snapshot?.documents?.mapNotNull { doc ->
                val data = doc.toObject(UserData::class.java)
                if (data != null) doc.id to data else null
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }
}
