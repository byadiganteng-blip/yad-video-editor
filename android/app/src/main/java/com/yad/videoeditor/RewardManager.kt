package com.yad.videoeditor

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.launch

object RewardManager {

    private const val PREFS_NAME = "yad_rewards"
    private const val KEY_PREMIUM = "premium_unlocked"
    private const val KEY_WATERMARK = "watermark_removed"
    private const val KEY_CREDITS = "user_credits"
    private const val KEY_UNLOCKED_FILTERS = "unlocked_filters"

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // ============================================================
    //  PREMIUM
    // ============================================================
    fun isPremium(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_PREMIUM, false)
    }

    fun unlockPremium(context: Context) {
        prefs(context).edit()
            .putBoolean(KEY_PREMIUM, true)
            .putLong("premium_unlocked_at", System.currentTimeMillis())
            .apply()
    }

    // ============================================================
    //  WATERMARK
    // ============================================================
    fun isWatermarkRemoved(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_WATERMARK, false)
    }

    fun removeWatermark(context: Context) {
        prefs(context).edit()
            .putBoolean(KEY_WATERMARK, true)
            .putLong("watermark_removed_at", System.currentTimeMillis())
            .apply()
    }

    // ============================================================
    //  CREDITS
    // ============================================================
    fun getCredits(context: Context): Int {
        return prefs(context).getInt(KEY_CREDITS, 0)
    }

    fun addCredits(context: Context, amount: Int) {
        val current = getCredits(context)
        prefs(context).edit()
            .putInt(KEY_CREDITS, current + amount)
            .apply()
    }

    fun useCredits(context: Context, amount: Int): Boolean {
        val current = getCredits(context)
        if (current < amount) return false
        prefs(context).edit()
            .putInt(KEY_CREDITS, current - amount)
            .apply()
        return true
    }

    // ============================================================
    //  FILTERS
    // ============================================================
    fun getUnlockedFilters(context: Context): Set<String> {
        return prefs(context).getStringSet(KEY_UNLOCKED_FILTERS, emptySet()) ?: emptySet()
    }

    fun unlockFilter(context: Context, filterId: String) {
        val current = getUnlockedFilters(context).toMutableSet()
        current.add(filterId)
        prefs(context).edit()
            .putStringSet(KEY_UNLOCKED_FILTERS, current)
            .apply()
    }

    fun isFilterUnlocked(context: Context, filterId: String): Boolean {
        return getUnlockedFilters(context).contains(filterId)
    }

    // ============================================================
    //  RESET (untuk testing)
    // ============================================================
    fun resetAll(context: Context) {
        prefs(context).edit().clear().apply()
    }

    // ============================================================
    //  FIRESTORE SYNC
    // ============================================================
    fun syncToFirestore(context: Context) {
        val uid = FirebaseManager.getCurrentUserUid() ?: return
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val profile = FirebaseManager.loadUserFromFirestore(uid)
                    ?: UserProfile(uid = uid)
                
                // Merge data lokal → cloud
                val updated = profile.copy(
                    isPremium = isPremium(context),
                    credits = getCredits(context).toLong(),
                    watermarkRemoved = isWatermarkRemoved(context),
                    unlockedFilters = getUnlockedFilters(context).toList()
                )
                FirebaseManager.saveUserToFirestore(updated)
            } catch (e: Exception) {
                // silent fail
            }
        }
    }

    fun loadFromFirestore(context: Context) {
        val uid = FirebaseManager.getCurrentUserUid() ?: return
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val profile = FirebaseManager.loadUserFromFirestore(uid) ?: return@launch
                // Sync cloud → lokal
                if (profile.isPremium) unlockPremium(context)
                if (profile.watermarkRemoved) removeWatermark(context)
                prefs(context).edit()
                    .putInt(KEY_CREDITS, profile.credits.toInt())
                    .putStringSet(KEY_UNLOCKED_FILTERS, profile.unlockedFilters.toSet())
                    .apply()
            } catch (e: Exception) {
                // silent fail
            }
        }
    }
}

