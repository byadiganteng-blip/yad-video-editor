package com.yad.videoeditor

import org.junit.Test
import org.junit.Assert.*

class GenerationModeTest {

    @Test
    fun allModesHaveUniqueId() {
        val ids = GenerationMode.values().map { it.id }
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun fromIdReturnsCorrectMode() {
        assertEquals(GenerationMode.ANIME, GenerationMode.fromId("anime"))
        assertEquals(GenerationMode.HORROR, GenerationMode.fromId("horror"))
    }

    @Test
    fun fromIdFallback() {
        assertEquals(GenerationMode.CINEMATIC, GenerationMode.fromId("unknown"))
    }

    @Test
    fun allLabelsUnique() {
        val labels = GenerationMode.allLabels()
        assertEquals(labels.size, labels.distinct().size)
    }
}
