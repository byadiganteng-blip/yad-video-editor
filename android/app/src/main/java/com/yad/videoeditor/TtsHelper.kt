package com.yad.videoeditor

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.io.File
import java.util.Locale
import java.util.UUID

/**
 * TtsHelper — Text-to-Speech ke file audio (pakai Android TTS native)
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
        val outFile = File(outputPath)

        tts = TextToSpeech(context) { status ->
            if (status != TextToSpeech.SUCCESS) {
                onComplete(false)
                return@TextToSpeech
            }

            val idLocale = Locale("id", "ID")
            val langResult = tts?.setLanguage(idLocale)
            if (langResult == TextToSpeech.LANG_MISSING_DATA ||
                langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.language = Locale.US
            }
            tts?.setSpeechRate(1.0f)
            tts?.setPitch(1.0f)

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}

                override fun onDone(utteranceId: String?) {
                    tts?.shutdown()
                    onComplete(outFile.exists() && outFile.length() > 0)
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

            val params = Bundle()
            tts?.synthesizeToFile(text, params, outFile, utteranceId)
        }
    }
}
