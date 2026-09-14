package com.yad.videoeditor

/**
 * GenerationMode — gaya visual untuk video.
 *
 * Ganti model AI dengan GAYA VISUAL yang lebih intuitif.
 * Setiap gaya punya prompt suffix untuk Pollinations.ai.
 */
enum class GenerationMode(
    val id: String,
    val label: String,
    val description: String,
    val type: ModeType,
    val styleSuffix: String = ""
) {
    // ============================================================
    //  MODE KHUSUS
    // ============================================================
    DIRECT(
        "direct",
        "🎬 Video Polos (Tanpa Gambar)",
        "Video dari teks saja, background warna solid",
        ModeType.DIRECT,
        ""
    ),
    RANDOM_PHOTO(
        "random",
        "🖼️ Foto Random (Picsum)",
        "Foto asli random dari Picsum — cepat, tanpa AI",
        ModeType.GOOGLE_IMAGE,
        ""
    ),

    // ============================================================
    //  10 GAYA VISUAL (via Pollinations.ai)
    // ============================================================
    ANIME(
        "anime",
        "🌸 Anime",
        "Gaya anime khas Jepang — warna cerah, karakter lucu",
        ModeType.AI_MODEL,
        "anime style, japanese animation, vibrant colors, detailed, studio ghibli inspired"
    ),
    REALISTIC(
        "realistic",
        "📷 Realistis",
        "Foto-like, natural, seperti foto asli",
        ModeType.AI_MODEL,
        "photorealistic, sharp focus, natural lighting, DSLR photo, 8k, hyper detailed"
    ),
    CINEMATIC(
        "cinematic",
        "🎬 Cinematic",
        "Seperti film Hollywood — dramatic, epic",
        ModeType.AI_MODEL,
        "cinematic shot, dramatic lighting, movie still, anamorphic lens, epic composition, 4k"
    ),
    HORROR(
        "horror",
        "👻 Horror",
        "Gelap, seram, menakutkan",
        ModeType.AI_MODEL,
        "horror atmosphere, dark, creepy, ominous, shadows, fog, unsettling, night"
    ),
    FANTASY(
        "fantasy",
        "🧙 Fantasy",
        "Dunia fantasi — magic, epic, colorful",
        ModeType.AI_MODEL,
        "fantasy art, magical, ethereal, epic, glowing, mystical creatures, digital painting"
    ),
    WATERCOLOR(
        "watercolor",
        "🎨 Watercolor",
        "Gaya lukisan cat air — lembut, artistik",
        ModeType.AI_MODEL,
        "watercolor painting, soft colors, artistic, hand painted, pastel, paper texture"
    ),
    CYBERPUNK(
        "cyberpunk",
        "🤖 Cyberpunk",
        "Futuristik — neon, kota malam",
        ModeType.AI_MODEL,
        "cyberpunk, neon lights, futuristic city, blade runner style, dystopian, rain, night"
    ),
    CARTOON(
        "cartoon",
        "😄 Kartun",
        "Gaya kartun — 2D, colorful, ceria",
        ModeType.AI_MODEL,
        "cartoon style, 2D animation, bold outlines, bright colors, disney pixar style"
    ),
    PAINTING(
        "painting",
        "🖼️ Lukisan Klasik",
        "Gaya lukisan cat minyak — klasik, elegan",
        ModeType.AI_MODEL,
        "oil painting, classical art, rembrandt style, renaissance, museum quality, textured"
    ),
    DREAMY(
        "dreamy",
        "✨ Dreamy",
        "Mimpi — soft, ethereal, surreal",
        ModeType.AI_MODEL,
        "dreamlike, surreal, ethereal, soft glow, pastel colors, fantasy, magical realism"
    );

    companion object {
        fun fromId(id: String): GenerationMode =
            values().firstOrNull { it.id == id } ?: CINEMATIC

        fun fromLabel(label: String): GenerationMode =
            values().firstOrNull { it.label == label } ?: CINEMATIC

        fun allLabels(): List<String> = values().map { it.label }
    }
}

enum class ModeType {
    DIRECT,        // tanpa gambar
    GOOGLE_IMAGE,  // foto random
    AI_MODEL       // AI generate dengan gaya
}
