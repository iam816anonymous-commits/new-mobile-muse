package com.agent.android.speech

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID

interface TextToSpeechListener {
    fun onStart(utteranceId: String)
    fun onDone(utteranceId: String)
    fun onError(utteranceId: String, errorCode: Int? = null)
}

class TextToSpeechEngine(private val context: Context) {

    private var tts: TextToSpeech? = null
    @Volatile
    private var isInitialized = false
    @Volatile
    private var initError = false

    fun initialize(onResult: ((Boolean) -> Unit)? = null) {
        if (isInitialized && tts != null) {
            onResult?.invoke(true)
            return
        }

        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                initError = false
                val langResult = tts?.setLanguage(Locale.US)
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // Language not fully supported, but initialization succeeded
                }
                setupUtteranceListener()
                onResult?.invoke(true)
            } else {
                isInitialized = false
                initError = true
                onResult?.invoke(false)
            }
        }
    }

    private var activeListener: TextToSpeechListener? = null

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {
                activeListener?.onStart(utteranceId)
            }

            override fun onDone(utteranceId: String) {
                activeListener?.onDone(utteranceId)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) {
                activeListener?.onError(utteranceId, null)
            }

            override fun onError(utteranceId: String, errorCode: Int) {
                activeListener?.onError(utteranceId, errorCode)
            }
        })
    }

    fun isAvailable(): Boolean {
        return isInitialized && !initError && tts != null
    }

    fun isLanguageAvailable(locale: Locale = Locale.US): Boolean {
        if (!isAvailable()) return false
        val result = tts?.isLanguageAvailable(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
        return result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
    }

    fun speak(text: String, listener: TextToSpeechListener? = null): Boolean {
        if (!isAvailable()) {
            return false
        }
        activeListener = listener
        val utteranceId = UUID.randomUUID().toString()

        val params = Bundle()
        val result = tts?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            params,
            utteranceId
        )
        return result == TextToSpeech.SUCCESS
    }

    fun stop() {
        if (isAvailable()) {
            try {
                tts?.stop()
            } catch (ignored: Exception) {}
        }
    }

    fun shutdown() {
        if (tts != null) {
            try {
                tts?.stop()
                tts?.shutdown()
            } catch (ignored: Exception) {}
            tts = null
            isInitialized = false
        }
    }
}
