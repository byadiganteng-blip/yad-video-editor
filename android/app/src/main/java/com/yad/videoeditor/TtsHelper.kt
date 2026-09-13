package com.yad.videoeditor

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.io.File
import java.util.Locale
import java.util.UUID

/**
 * TtsHelper — Text-to-Speech ke file audio
 * Dipakai untuk generate suara kalau video tidak punya audio
 */
object TtsHelper {

    fun synthesizeToFile(
        context: Context,
        text: String,
        outputPath: String,
        onComplete: (Boolean) -> Unit
    ) {
        var tts: TextToSpeech? = null
        val utteranceId = UUID.randomUUID().toString()

        tts = TextToSpeech(context) { status ->
            if (status != TextToSpeech.SUCCESS) {
                onComplete(false)
                return@TextToSpeech
            }

            tts?.language = Locale("id", "ID") // Bahasa Indonesia
            if (tts?.isLanguageAvailable(Locale("id", "ID")) != TextToSpeech.LANG_AVAILABLE) {
                tts?.language = Locale.US // fallback English
            }
            tts?.setSpeechRate(1.0f)
            tts?.setPitch(1.0f)

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}

                override fun onDone(utteranceId: String?) {
                    tts?.shutdown()
                    onComplete(File(outputPath).exists())
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    tts?.shutdown()
                    onComplete(false)
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    tts?.shutdown()
                    onComplete(false)
                }
            })

            val params = android.os.Bundle()
            tts?.synthesizeToFile(text, params, File(outputPath), utteranceId)
        }
    }
}
