package com.yad.videoeditor

/**
 * GenerationMode — semua opsi yang bisa dipilih di spinner.
 * Urutan: 2 non-AI dulu, lalu 10 model AI.
 */
enum class GenerationMode(
    val id: String,
    val label: String,
    val description: String,
    val type: ModeType
) {
    // ============================================================
    //  2 MODE NON-AI (Generate Langsung & Google Image)
    // ============================================================
    DIRECT(
        "direct",
        "\uD83C\uDFAC Generate Langsung (Tanpa Gambar)",
        "Buat video dari teks langsung — tanpa AI, tanpa gambar",
        ModeType.DIRECT
    ),
    GOOGLE_IMAGE(
        "google_image",
        "\uD83D\uDD0D Cari Gambar Google + Video",
        "Cari gambar dari Google lalu jadikan video",
        ModeType.GOOGLE_IMAGE
    ),

    // ============================================================
    //  10 MODEL AI
    // ============================================================
    WAIFU_DIFFUSION(
        "waifu",
        "\uD83C\uDF38 Waifu Diffusion (Klasik)",
        "Model anime klasik — bagus untuk karakter anime",
        ModeType.AI_MODEL
    ),
    SD_15(
        "sd15",
        "\uD83C\uDFA8 Stable Diffusion 1.5",
        "Model dasar SD 1.5 — serba bisa",
        ModeType.AI_MODEL
    ),
    ANYTHING_V4(
        "anything",
        "\u2728 Anything v4.0",
        "Model anime modern — kualitas tinggi",
        ModeType.AI_MODEL
    ),
    DREAMSHAPER(
        "dreamshaper",
        "\uD83C\uDF08 DreamShaper",
        "Model artistik — warna cerah",
        ModeType.AI_MODEL
    ),
    SDXL_BASE(
        "sdxl",
        "\uD83C\uDFC6 SDXL Base 1.0",
        "Model terbaru — resolusi tinggi, lambat",
        ModeType.AI_MODEL
    ),
    OPENJOURNEY(
        "openjourney",
        "\uD83E\uDDD1 OpenJourney",
        "Model bergaya Midjourney",
        ModeType.AI_MODEL
    ),
    DREAMLIKE(
        "dreamlike",
        "\uD83C\uDF19 Dreamlike",
        "Model cinematic — untuk cerita horor/dramatis",
        ModeType.AI_MODEL
    ),
    TRINART_V2(
        "trinart",
        "\uD83C\uDFAF Trinart v2",
        "Model semi-realistic",
        ModeType.AI_MODEL
    ),
    REALISTIC_VISION(
        "realistic",
        "\uD83D\uDCF7 Realistic Vision",
        "Realistis natural — foto-like",
        ModeType.AI_MODEL
    ),
    MAJICMIX(
        "majicmix",
        "\uD83C\uDF3A MajicMix",
        "Realistis detail tajam — kualitas tinggi",
        ModeType.AI_MODEL
    );

    companion object {
        fun fromId(id: String): GenerationMode =
            values().firstOrNull { it.id == id } ?: DIRECT

        fun fromLabel(label: String): GenerationMode =
            values().firstOrNull { it.label == label } ?: DIRECT

        /** Ambil hanya label untuk spinner adapter. */
        fun allLabels(): List<String> = values().map { it.label }
    }
}

/**
 * ModeType — kategori mode
 */
enum class ModeType {
    DIRECT,        // generate langsung tanpa AI
    GOOGLE_IMAGE,  // cari gambar dari Google
    AI_MODEL       // pakai AI model
}
