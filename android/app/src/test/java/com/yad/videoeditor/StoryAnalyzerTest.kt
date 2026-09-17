package com.yad.videoeditor

import org.junit.Test
import org.junit.Assert.*

class StoryAnalyzerTest {

    @Test
    fun detectHorrorMood() {
        val story = "Di malam yang gelap, Sinta melihat hantu di cermin."
        val detail = StoryAnalyzer.analyze(story)
        assertEquals("horror", detail.mood)
        assertTrue(detail.extraEffects.contains("ghost"))
        assertTrue(detail.extraEffects.contains("mirror"))
    }

    @Test
    fun detectHappyMood() {
        val story = "Ani tertawa riang melihat kucing lucu."
        val detail = StoryAnalyzer.analyze(story)
        assertEquals("happy", detail.mood)
    }

    @Test
    fun detectNightTime() {
        val story = "Pada tengah malam, bulan bersinar terang."
        val detail = StoryAnalyzer.analyze(story)
        assertEquals("night", detail.timeOfDay)
    }

    @Test
    fun detectForestLocation() {
        val story = "Ia tersesat di hutan rimba yang lebat."
        val detail = StoryAnalyzer.analyze(story)
        assertEquals("forest", detail.location)
    }

    @Test
    fun buildPromptNotEmpty() {
        val detail = StoryAnalyzer.analyze("Sinta di hutan")
        val prompt = StoryAnalyzer.buildPrompt(detail, "cinematic")
        assertTrue(prompt.isNotEmpty())
        assertTrue(prompt.contains("cinematic"))
    }
}
