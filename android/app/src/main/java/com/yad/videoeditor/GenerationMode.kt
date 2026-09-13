package com.yad.videoeditor

/**
 * GenerationMode — mode generate video
 */
enum class GenerationMode(val id: String, val label: String, val description: String) {
    DIRECT(
        "direct",
        "Generate Langsung",
        "Buat video langsung dari teks tanpa model/gambar"
    ),
    GOOGLE_IMAGE(
        "google_image",
        "Cari Gambar dari Google",
        "Cari gambar di Google lalu jadikan video"
    ),
    AI_MODEL(
        "ai_model",
        "Model AI",
        "Pakai model AI (Stable Diffusion, dll)"
    );

    companion object {
        fun fromId(id: String): GenerationMode =
            values().firstOrNull { it.id == id } ?: DIRECT
    }
}
