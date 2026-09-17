package com.yad.videoeditor

import org.junit.Test
import org.junit.Assert.*

class UserProfileTest {

    @Test
    fun toMapAndBack() {
        val profile = UserProfile(
            uid = "test123",
            email = "test@example.com",
            displayName = "Test User",
            isPremium = true,
            credits = 100
        )
        val map = profile.toMap()
        val restored = UserProfile.fromMap("test123", map)

        assertEquals(profile.uid, restored.uid)
        assertEquals(profile.email, restored.email)
        assertEquals(profile.isPremium, restored.isPremium)
        assertEquals(profile.credits, restored.credits)
    }

    @Test
    fun fromMapHandlesNulls() {
        val map = mapOf<String, Any?>(
            "email" to null,
            "isPremium" to null
        )
        val profile = UserProfile.fromMap("uid", map)
        assertEquals("", profile.email)
        assertFalse(profile.isPremium)
    }
}
