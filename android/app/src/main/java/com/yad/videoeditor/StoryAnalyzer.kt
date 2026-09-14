package com.yad.videoeditor

/**
 * StoryAnalyzer — deteksi otomatis detail dari cerita.
 *
 * Analisa keyword untuk tentukan:
 *  - Mood/effect (horror, happy, sad, action)
 *  - Waktu (malam, siang, senja)
 *  - Lokasi (rumah, hutan, kota)
 *  - Karakter utama (Sinta, cermin, dll)
 */
object StoryAnalyzer {

    private const val TAG = "StoryAnalyzer"

    data class StoryDetail(
        val mood: String,
        val timeOfDay: String,
        val location: String,
        val characters: List<String>,
        val mainSubject: String,
        val extraEffects: List<String>
    )

    // ============================================================
    //  KEYWORD MAPS
    // ============================================================
    private val MOOD_KEYWORDS = mapOf(
        "horror"    to listOf("seram", "takut", "hantu", "kengerian", "gelap",
                              "teror", "mengerikan", "horor", "mayat", "darah",
                              "kematian", "berhantu", "menakutkan", "panik"),
        "happy"     to listOf("senang", "bahagia", "gembira", "tawa", "tertawa",
                              "ceria", "sukacita", "riang", "girang", "bahagia"),
        "sad"       to listOf("sedih", "menangis", "tangis", "duka", "kehilangan",
                              "pilu", "nestapa", "air mata", "pilu", "merana"),
        "action"    to listOf("kejar", "lari", "tembak", "ledakan", "perang",
                              "bertarung", "adu", "pukul", "tinju", "kejar-kejaran",
                              "letusan", "benturan"),
        "romantic"  to listOf("cinta", "kasih", "peluk", "cium", "rindu",
                              "kekasih", "sayang", "asmara", "pacaran"),
        "mystery"   to listOf("misteri", "rahasia", "teka-teki", "misterius",
                              "aneh", "gangguan", "misteri", "puzzle"),
        "adventure" to listOf("petualangan", "perjalanan", "ekspedisi", "misi",
                              "jelajah", "menjelajah", "penjelajahan"),
        "magic"     to listOf("sihir", "ajaib", "mantra", "sulap", "gaib",
                              "magis", "kuasa", "sakti"),
    )

    private val TIME_KEYWORDS = mapOf(
        "night"   to listOf("malam", "gelap", "tengah malam", "subuh", "petang",
                            "senja", "maghrib", "isya", "bintang", "bulan"),
        "day"     to listOf("siang", "pagi", "terang", "matahari", "panas",
                            "cerah", "terik", "pukul 12", "tengah hari"),
        "sunset"  to listOf("senja", "maghrib", "matahari terbenam", "jingga",
                            "kemerahan", "oranye", "golden hour"),
        "dawn"    to listOf("subuh", "fajar", "matahari terbit", "pagi buta"),
    )

    private val LOCATION_KEYWORDS = mapOf(
        "house"     to listOf("rumah", "kamar", "dapur", "kamar mandi", "ruang",
                              "halaman", "teras", "pintu", "jendela"),
        "forest"    to listOf("hutan", "pohon", "rimba", "belantara", "pepohonan"),
        "city"      to listOf("kota", "jalan", "gedung", "mall", "sekolah",
                              "kantor", "pasar", "bandara", "stasiun"),
        "beach"     to listOf("pantai", "laut", "pesisir", "pulau", "ombak"),
        "mountain"  to listOf("gunung", "bukit", "pegunungan", "lembah"),
        "hospital"  to listOf("rumah sakit", "klinik", "dokter", "perawat"),
        "cemetery"  to listOf("kuburan", "makam", "kubur", "pemakaman"),
        "school"    to listOf("sekolah", "kelas", "kampus", "universitas"),
    )

    private val EXTRA_EFFECTS = mapOf(
        "rain"      to listOf("hujan", "gerimis", "basah", "air"),
        "fog"       to listOf("kabut", "berkabut", "asap", "samar"),
        "fire"      to listOf("api", "kebakaran", "membara", "panas", "terbakar"),
        "wind"      to listOf("angin", "ribut", "badai", "kencang", "puting beliung"),
        "lightning" to listOf("petir", "kilat", "geledek", "halilintar"),
        "snow"      to listOf("salju", "dingin", "beku", "es"),
        "ghost"     to listOf("hantu", "arwah", "setan", "jin", "iblis", "makhluk"),
        "mirror"    to listOf("cermin", "kaca", "pantulan", "refleksi"),
        "shadow"    to listOf("bayangan", "siluet", "gelap", "samar"),
    )

    // ============================================================
    //  ANALISA CERITA
    // ============================================================
    fun analyze(text: String): StoryDetail {
        val lower = text.lowercase()

        // Detect mood
        var mood = "neutral"
        var maxHits = 0
        MOOD_KEYWORDS.forEach { (m, keywords) ->
            val hits = keywords.count { lower.contains(it) }
            if (hits > maxHits) { maxHits = hits; mood = m }
        }

        // Detect time
        var timeOfDay = "day"
        maxHits = 0
        TIME_KEYWORDS.forEach { (t, keywords) ->
            val hits = keywords.count { lower.contains(it) }
            if (hits > maxHits) { maxHits = hits; timeOfDay = t }
        }

        // Detect location
        var location = ""
        maxHits = 0
        LOCATION_KEYWORDS.forEach { (l, keywords) ->
            val hits = keywords.count { lower.contains(it) }
            if (hits > maxHits) { maxHits = hits; location = l }
        }

        // Detect effects
        val effects = mutableListOf<String>()
        EXTRA_EFFECTS.forEach { (effect, keywords) ->
            if (keywords.any { lower.contains(it) }) {
                effects.add(effect)
            }
        }

        // Extract karakter (kata dengan huruf kapital di awal, kecuali stop words)
        val stopWords = setOf("Yang", "Dan", "Di", "Ke", "Dari", "Ini", "Itu",
                              "Dengan", "Untuk", "Pada", "Adalah", "The", "A")
        val characters = text.split(" ", ".", ",", "!", "?")
            .map { it.trim() }
            .filter { it.length > 2 && it[0].isUpperCase() && !stopWords.contains(it) }
            .distinct()
            .take(3)

        // Main subject = karakter pertama
        val mainSubject = characters.firstOrNull() ?: "mysterious person"

        AutoLogSaver.log(TAG, "Mood=$mood, time=$timeOfDay, location=$location")
        AutoLogSaver.log(TAG, "Effects=$effects, characters=$characters")

        return StoryDetail(
            mood = mood,
            timeOfDay = timeOfDay,
            location = location,
            characters = characters,
            mainSubject = mainSubject,
            extraEffects = effects
        )
    }

    /**
     * Build prompt dari detail + style suffix.
     * Contoh: "Sinta, creepy mirror, horror, night, dark atmosphere,
     *          cinematic lighting, studio ghibli inspired"
     */
    fun buildPrompt(detail: StoryDetail, styleSuffix: String): String {
        val parts = mutableListOf<String>()

        // 1. Karakter utama
        parts.add(detail.mainSubject)

        // 2. Mood
        val moodMap = mapOf(
            "horror"    to "creepy, scary, dark atmosphere",
            "happy"     to "joyful, bright, cheerful",
            "sad"       to "melancholic, somber, emotional",
            "action"    to "dynamic action, motion blur, intense",
            "romantic"  to "romantic, soft lighting, intimate",
            "mystery"   to "mysterious, enigmatic, intrigue",
            "adventure" to "epic adventure, expansive",
            "magic"     to "magical, glowing, enchanted",
            "neutral"   to "atmospheric",
        )
        parts.add(moodMap[detail.mood] ?: "atmospheric")

        // 3. Waktu
        val timeMap = mapOf(
            "night"  to "night time, moonlight, stars",
            "day"    to "daytime, bright sunlight",
            "sunset" to "golden hour, sunset, warm light",
            "dawn"   to "dawn, sunrise, soft morning light",
        )
        parts.add(timeMap[detail.timeOfDay] ?: "daytime")

        // 4. Lokasi
        if (detail.location.isNotEmpty()) {
            val locationMap = mapOf(
                "house"     to "inside a house, cozy room",
                "forest"    to "dense forest, tall trees",
                "city"      to "urban city, buildings",
                "beach"     to "beach, ocean waves",
                "mountain"  to "mountains, high altitude",
                "hospital"  to "hospital, clinical setting",
                "cemetery"  to "graveyard, tombstones",
                "school"    to "school, classroom",
            )
            parts.add(locationMap[detail.location] ?: detail.location)
        }

        // 5. Efek tambahan
        val effectMap = mapOf(
            "rain"      to "rainy, wet, raindrops",
            "fog"       to "foggy, misty, hazy",
            "fire"      to "fire, flames, burning",
            "wind"      to "windy, windy atmosphere",
            "lightning" to "lightning, thunderstorm",
            "snow"      to "snowy, cold, winter",
            "ghost"     to "ghostly, supernatural, spectral",
            "mirror"    to "mirror reflection, glass",
            "shadow"    to "shadows, silhouettes",
        )
        detail.extraEffects.forEach { effect ->
            effectMap[effect]?.let { parts.add(it) }
        }

        // 6. Style suffix
        if (styleSuffix.isNotEmpty()) {
            parts.add(styleSuffix)
        }

        val prompt = parts.joinToString(", ")
        AutoLogSaver.log(TAG, "Final prompt: $prompt")
        return prompt
    }
}
