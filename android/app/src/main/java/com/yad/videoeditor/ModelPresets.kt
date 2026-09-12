package com.yad.videoeditor

/**
 * Daftar model AI yang bisa dipilih user.
 * Setiap model punya karakteristik berbeda.
 */
data class AiModel(
    val id: String,           // ID yang dikirim ke GitHub (harus sama dgn python)
    val displayName: String,  // Nama tampil di APK
    val description: String,  // Deskripsi singkat
    val style: String,        // anime / realistic / cinematic / artistic
    val hfPath: String        // Path HuggingFace
)

object ModelPresets {
    val ALL = listOf(
        AiModel(
            id = "waifu",
            displayName = "🌸 Waifu Diffusion (Klasik)",
            description = "Model lama tapi sangat bagus untuk anime. Stabil, ringan, hasil konsisten.",
            style = "anime",
            hfPath = "hakurei/waifu-diffusion"
        ),
        AiModel(
            id = "sd15",
            displayName = "🎨 Stable Diffusion 1.5",
            description = "Model serbaguna. Cocok untuk realistis & anime. Paling stabil & ringan.",
            style = "realistic",
            hfPath = "runwayml/stable-diffusion-v1-5"
        ),
        AiModel(
            id = "anything",
            displayName = "✨ Anything v4.0 (Anime)",
            description = "Anime berkualitas tinggi. Detail bagus, warna cerah, cocok untuk storyboard.",
            style = "anime",
            hfPath = "andite/anything-v4.0"
        ),
        AiModel(
            id = "dreamshaper",
            displayName = "💫 DreamShaper",
            description = "Realistis bagus untuk karakter manusia. Populer di komunitas AI.",
            style = "realistic",
            hfPath = "Lykon/DreamShaper"
        ),
        AiModel(
            id = "sdxl",
            displayName = "🏆 SDXL Base 1.0",
            description = "Kualitas tertinggi. Resolusi 1024x1024. Cocok untuk cinematic epic. Lebih lambat.",
            style = "cinematic",
            hfPath = "stabilityai/stable-diffusion-xl-base-1.0"
        ),
        AiModel(
            id = "openjourney",
            displayName = "🎭 OpenJourney (Artistik)",
            description = "Gaya artistik & surreal. Cocok untuk cerita fantasi & abstrak.",
            style = "artistic",
            hfPath = "prompthero/openjourney"
        ),
        AiModel(
            id = "dreamlike",
            displayName = "🌙 Dreamlike Diffusion",
            description = "Surreal & dreamy. Cocok untuk cerita mimpi & fantasi.",
            style = "artistic",
            hfPath = "dreamlike-art/dreamlike-diffusion-1.0"
        ),
        AiModel(
            id = "trinart",
            displayName = "🎌 Trinart v2",
            description = "Anime alternatif. Karakter fokus, cocok untuk portrait.",
            style = "anime",
            hfPath = "naclbit/trinart_stable_diffusion_v2"
        ),
        AiModel(
            id = "realistic",
            displayName = "📷 Realistic Vision 5.1",
            description = "Realistis natural. Cocok untuk video dokumenter & tutorial.",
            style = "realistic",
            hfPath = "SG161222/Realistic_Vision_V5.1_noVAE"
        ),
        AiModel(
            id = "majicmix",
            displayName = "🌺 MajicMix Realistic",
            description = "Realistis kualitas tinggi. Detail tajam & natural.",
            style = "realistic",
            hfPath = "digiplay/majicMIX_realistic_v7"
        )
    )

    fun getById(id: String): AiModel? = ALL.find { it.id == id }
    fun default(): AiModel = ALL[0]
}
