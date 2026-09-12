package com.yad.videoeditor

data class AiModel(
    val id: String,
    val displayName: String,
    val description: String,
    val style: String,
    val hfPath: String
)

object ModelPresets {
    val ALL = listOf(
        AiModel("waifu", "🌸 Waifu Diffusion (Klasik)",
            "Model lama tapi sangat bagus untuk anime.", "anime",
            "hakurei/waifu-diffusion"),
        AiModel("sd15", "🎨 Stable Diffusion 1.5",
            "Model serbaguna. Cocok realistis & anime.", "realistic",
            "runwayml/stable-diffusion-v1-5"),
        AiModel("anything", "✨ Anything v4.0",
            "Anime berkualitas tinggi.", "anime",
            "andite/anything-v4.0"),
        AiModel("dreamshaper", "💫 DreamShaper",
            "Realistis bagus untuk karakter manusia.", "realistic",
            "Lykon/DreamShaper"),
        AiModel("sdxl", "🏆 SDXL Base 1.0",
            "Kualitas tertinggi. Lebih lambat.", "cinematic",
            "stabilityai/stable-diffusion-xl-base-1.0"),
        AiModel("openjourney", "🎭 OpenJourney",
            "Gaya artistik & surreal.", "artistic",
            "prompthero/openjourney"),
        AiModel("dreamlike", "🌙 Dreamlike",
            "Surreal & dreamy.", "artistic",
            "dreamlike-art/dreamlike-diffusion-1.0"),
        AiModel("trinart", "🎌 Trinart v2",
            "Anime alternatif.", "anime",
            "naclbit/trinart_stable_diffusion_v2"),
        AiModel("realistic", "📷 Realistic Vision",
            "Realistis natural.", "realistic",
            "SG161222/Realistic_Vision_V5.1_noVAE"),
        AiModel("majicmix", "🌺 MajicMix",
            "Realistis detail tajam.", "realistic",
            "digiplay/majicMIX_realistic_v7"),
    )
    fun getById(id: String): AiModel? = ALL.find { it.id == id }
    fun default(): AiModel = ALL[0]
}
