package com.yad.videoeditor

/**
 * Data user yang disimpan di Firestore.
 * Dipakai untuk sync premium, credits, filters, dll.
 */
data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val isPremium: Boolean = false,
    val credits: Long = 0,
    val watermarkRemoved: Boolean = false,
    val unlockedFilters: List<String> = emptyList(),
    val deviceModel: String = "",
    val appVersion: Long = 15,
    val installedAt: Long = 0,
    val lastUsed: Long = 0,
    val fcmTokens: List<String> = emptyList()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "uid" to uid,
        "email" to email,
        "displayName" to displayName,
        "photoUrl" to photoUrl,
        "isPremium" to isPremium,
        "credits" to credits,
        "watermarkRemoved" to watermarkRemoved,
        "unlockedFilters" to unlockedFilters,
        "deviceModel" to deviceModel,
        "appVersion" to appVersion,
        "installedAt" to installedAt,
        "lastUsed" to lastUsed,
        "fcmTokens" to fcmTokens
    )

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(uid: String, map: Map<String, Any?>): UserProfile {
            return UserProfile(
                uid = uid,
                email = map["email"] as? String ?: "",
                displayName = map["displayName"] as? String ?: "",
                photoUrl = map["photoUrl"] as? String ?: "",
                isPremium = map["isPremium"] as? Boolean ?: false,
                credits = (map["credits"] as? Number)?.toLong() ?: 0L,
                watermarkRemoved = map["watermarkRemoved"] as? Boolean ?: false,
                unlockedFilters = (map["unlockedFilters"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                deviceModel = map["deviceModel"] as? String ?: "",
                appVersion = (map["appVersion"] as? Number)?.toLong() ?: 15L,
                installedAt = (map["installedAt"] as? Number)?.toLong() ?: 0L,
                lastUsed = (map["lastUsed"] as? Number)?.toLong() ?: 0L,
                fcmTokens = (map["fcmTokens"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            )
        }
    }
}
