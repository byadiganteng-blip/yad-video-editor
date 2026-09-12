package com.yad.videoeditor

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
    //  AUTH
    // ============================================================
    suspend fun loginAnonymous(): Boolean {
        return try {
            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser == null) auth.signInAnonymously().await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Login failed: ${e.message}"); false
        }
    }

    // ============================================================
    //  SECRETS
    // ============================================================
    suspend fun fetchGithubToken(): String? {
        return try {
            db.collection("secrets").document("github").get().await()
                .getString("token")
        } catch (e: Exception) { null }
    }

    fun saveGithubToken(token: String, cb: (Boolean) -> Unit = {}) {
        db.collection("secrets").document("github")
            .set(mapOf("token" to token, "updated_at" to System.currentTimeMillis()))
            .addOnSuccessListener { cb(true) }
            .addOnFailureListener { cb(false) }
    }

    // ============================================================
    //  CONFIG
    // ============================================================
    data class AppConfig(
        val freeLimitPerDay: Int = 3,
        val premiumLimitPerDay: Int = -1,
        val defaultModel: String = "waifu",
        val enabledModels: List<String> = listOf(),
        val showAds: Boolean = true,
        val forceUpdate: Boolean = false,
        val minVersionCode: Int = 13,
        val updateUrl: String = "",
        val messageBanner: String = "",
        val maintenanceMode: Boolean = false
    )

    fun configFlow(): Flow<AppConfig> = callbackFlow {
        val ref = db.collection("config").document("main")
        val listener = ref.addSnapshotListener { snap, err ->
            if (err != null || snap == null) return@addSnapshotListener
            try {
                val cfg = AppConfig(
                    freeLimitPerDay = snap.getLong("free_limit_per_day")?.toInt() ?: 3,
                    premiumLimitPerDay = snap.getLong("premium_limit_per_day")?.toInt() ?: -1,
                    defaultModel = snap.getString("default_model") ?: "waifu",
                    enabledModels = (snap.get("enabled_models") as? List<*>)?.mapNotNull { it as? String } ?: listOf(),
                    showAds = snap.getBoolean("show_ads") ?: true,
                    forceUpdate = snap.getBoolean("force_update") ?: false,
                    minVersionCode = snap.getLong("min_version_code")?.toInt() ?: 13,
                    updateUrl = snap.getString("update_url") ?: "",
                    messageBanner = snap.getString("message_banner") ?: "",
                    maintenanceMode = snap.getBoolean("maintenance_mode") ?: false
                )
                trySend(cfg)
            } catch (e: Exception) { Log.e(TAG, "Parse: ${e.message}") }
        }
        awaitClose { listener.remove() }
    }

    suspend fun updateConfig(key: String, value: Any): Boolean {
        return try {
            db.collection("config").document("main")
                .set(mapOf(key to value), SetOptions.merge()).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "updateConfig: ${e.message}"); false
        }
    }

    fun updateConfigAsync(key: String, value: Any, cb: (Boolean) -> Unit) {
        db.collection("config").document("main")
            .set(mapOf(key to value), SetOptions.merge())
            .addOnSuccessListener { cb(true) }
            .addOnFailureListener { cb(false) }
    }

    // ============================================================
    //  USER
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

    fun getDeviceId(context: Context): String {
        return try {
            Settings.Secure.getString(context.contentResolver,
                Settings.Secure.ANDROID_ID) ?: "unknown"
        } catch (e: Exception) { "unknown" }
    }

    fun registerUser(context: Context) {
        val id = getDeviceId(context)
        db.collection("users").document(id).set(mapOf(
            "device_model" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "installed_at" to System.currentTimeMillis(),
            "app_version" to 13,
            "last_used" to System.currentTimeMillis()
        ), SetOptions.merge())
    }

    fun updateLastUsed(context: Context) {
        val id = getDeviceId(context)
        db.collection("users").document(id)
            .set(mapOf("last_used" to System.currentTimeMillis()), SetOptions.merge())
    }

    fun getUserFlow(context: Context): Flow<UserData?> = callbackFlow {
        val id = getDeviceId(context)
        val ref = db.collection("users").document(id)
        val listener = ref.addSnapshotListener { snap, err ->
            if (err != null) { trySend(null); return@addSnapshotListener }
            trySend(snap?.toObject(UserData::class.java))
        }
        awaitClose { listener.remove() }
    }

    fun resetUserLimit(context: Context, cb: (Boolean) -> Unit = {}) {
        val id = getDeviceId(context)
        db.collection("users").document(id)
            .set(mapOf("daily_count" to 0), SetOptions.merge())
            .addOnSuccessListener { cb(true) }
            .addOnFailureListener { cb(false) }
    }

    fun resetAllLimits(cb: (Boolean) -> Unit = {}) {
        db.collection("users").get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    doc.reference.set(mapOf("daily_count" to 0), SetOptions.merge())
                }
                cb(true)
            }
            .addOnFailureListener { cb(false) }
    }

    fun setPremium(deviceId: String, premium: Boolean, cb: (Boolean) -> Unit = {}) {
        db.collection("users").document(deviceId)
            .set(mapOf("is_premium" to premium), SetOptions.merge())
            .addOnSuccessListener { cb(true) }
            .addOnFailureListener { cb(false) }
    }

    fun usersFlow(): Flow<List<Pair<String, UserData>>> = callbackFlow {
        val ref = db.collection("users")
        val listener = ref.addSnapshotListener { snap, err ->
            if (err != null) { trySend(emptyList()); return@addSnapshotListener }
            val list = snap?.documents?.mapNotNull { doc ->
                val data = doc.toObject(UserData::class.java)
                if (data != null) doc.id to data else null
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    // ============================================================
    //  BROADCAST
    // ============================================================
    data class BroadcastMsg(
        val message: String = "",
        val active: Boolean = false,
        val createdAt: Long = 0
    )

    fun sendBroadcast(message: String, cb: (Boolean) -> Unit = {}) {
        val data = mapOf(
            "message" to message,
            "active" to true,
            "created_at" to System.currentTimeMillis()
        )
        db.collection("broadcast").document("main").set(data)
            .addOnSuccessListener { cb(true) }
            .addOnFailureListener { cb(false) }
    }

    fun broadcastFlow(): Flow<BroadcastMsg?> = callbackFlow {
        val ref = db.collection("broadcast").document("main")
        val listener = ref.addSnapshotListener { snap, err ->
            if (err != null) { trySend(null); return@addSnapshotListener }
            trySend(snap?.toObject(BroadcastMsg::class.java))
        }
        awaitClose { listener.remove() }
    }

    // ============================================================
    //  STATS
    // ============================================================
    fun incrementCounter(key: String) {
        val ref = db.collection("stats").document("main")
        db.runTransaction { tx ->
            val snap = tx.get(ref)
            val current = snap.getLong(key) ?: 0
            tx.set(ref, mapOf(key to current + 1), SetOptions.merge())
        }
    }

    fun statsFlow(): Flow<Map<String, Long>?> = callbackFlow {
        val ref = db.collection("stats").document("main")
        val listener = ref.addSnapshotListener { snap, err ->
            if (err != null) { trySend(null); return@addSnapshotListener }
            trySend(snap?.data?.mapValues { (it.value as? Long) ?: 0L })
        }
        awaitClose { listener.remove() }
    }

    // ============================================================
    //  LOGS
    // ============================================================
    fun logAction(action: String, detail: String = "") {
        val data = mapOf(
            "action" to action,
            "detail" to detail,
            "timestamp" to System.currentTimeMillis()
        )
        db.collection("logs").add(data)
    }

    fun logsFlow(): Flow<List<Map<String, Any>>> = callbackFlow {
        val ref = db.collection("logs")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(50)
        val listener = ref.addSnapshotListener { snap, err ->
            if (err != null) { trySend(emptyList()); return@addSnapshotListener }
            val list = snap?.documents?.mapNotNull { it.data } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    // ============================================================
    //  MODELS
    // ============================================================
    fun modelsFlow(): Flow<List<String>> = callbackFlow {
        val ref = db.collection("config").document("main")
        val listener = ref.addSnapshotListener { snap, err ->
            if (err != null) { trySend(emptyList()); return@addSnapshotListener }
            val list = (snap?.get("enabled_models") as? List<*>)?.mapNotNull { it as? String } ?: listOf()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    fun setEnabledModels(models: List<String>, cb: (Boolean) -> Unit = {}) {
        db.collection("config").document("main")
            .set(mapOf("enabled_models" to models), SetOptions.merge())
            .addOnSuccessListener { cb(true) }
            .addOnFailureListener { cb(false) }
    }
}
