package com.yad.videoeditor

data class VideoSizePreset(val name: String, val displayName: String, val width: Int, val height: Int) {
    val aspectRatio: String get() = "$width:$height"
    companion object {
        val ALL = listOf(
            VideoSizePreset("yt_shorts", "YouTube Shorts", 1080, 1920),
            VideoSizePreset("tiktok", "TikTok", 1080, 1920),
            VideoSizePreset("reels", "Instagram Reels", 1080, 1920),
            VideoSizePreset("ig_feed", "Instagram Feed", 1080, 1080),
            VideoSizePreset("youtube", "YouTube 16:9", 1920, 1080),
            VideoSizePreset("youtube_4k", "YouTube 4K", 3840, 2160),
            VideoSizePreset("facebook", "Facebook Video", 1920, 1080),
        )
        fun byName(name: String): VideoSizePreset = ALL.firstOrNull { it.name == name } ?: ALL[0]
    }
}

data class QualityPreset(val name: String, val displayName: String, val bitrate: Int, val fps: Int) {
    companion object {
        val ALL = listOf(
            QualityPreset("240p", "240p", 500_000, 24),
            QualityPreset("360p", "360p", 1_000_000, 30),
            QualityPreset("480p", "480p", 2_000_000, 30),
            QualityPreset("720p", "720p HD", 4_000_000, 30),
            QualityPreset("1080p", "1080p FHD", 8_000_000, 30),
            QualityPreset("1440p", "1440p 2K", 16_000_000, 30),
            QualityPreset("2160p", "2160p 4K", 30_000_000, 30),
        )
        fun byName(name: String): QualityPreset = ALL.firstOrNull { it.name == name } ?: ALL[2]
    }
}

// ═══ VOICE PRESET ═══
data class VoicePreset(val id: String, val displayName: String) {
    companion object {
        val ALL = listOf(
            VoicePreset("none",     "🔇 Tanpa Suara"),
            VoicePreset("male_id",  "👨 Pria Indonesia"),
            VoicePreset("female_id","👩 Wanita Indonesia"),
            VoicePreset("child_id", "🧒 Anak-anak"),
            VoicePreset("robot",    "🤖 Robot"),
            VoicePreset("male_en",  "🇬🇧 Male English"),
            VoicePreset("female_en","🇬🇧 Female English"),
        )
        fun byId(id: String): VoicePreset = ALL.firstOrNull { it.id == id } ?: ALL[0]
    }
}

// ═══ SUBTITLE STYLE ═══
data class SubtitleStyle(val id: String, val displayName: String) {
    companion object {
        val ALL = listOf(
            SubtitleStyle("neon",     "✨ Neon Glow"),
            SubtitleStyle("gradient", "🌈 Gradient"),
            SubtitleStyle("outline",  "🖤 Outline Tebal"),
            SubtitleStyle("shadow",   "🌑 Shadow Dalam"),
            SubtitleStyle("classic",  "📺 Classic Putih"),
            SubtitleStyle("karaoke",  "🎤 Karaoke"),
            SubtitleStyle("typewriter","⌨️ Typewriter"),
        )
        fun byId(id: String): SubtitleStyle = ALL.firstOrNull { it.id == id } ?: ALL[0]
    }
}
