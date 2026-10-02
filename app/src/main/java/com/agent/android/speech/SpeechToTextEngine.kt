package com.agent.android.speech

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import java.util.Locale

interface SpeechToTextListener {
    fun onReadyForSpeech()
    fun onBeginningOfSpeech()
    fun onRmsChanged(rmsdB: Float)
    fun onResults(text: String)
    fun onError(errorCode: Int, errorMessage: String)
    fun onPartialResults(partialText: String)
}

class SpeechToTextEngine(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null

    fun isAvailable(): Boolean {
        return try {
            if (context.packageName.isNullOrBlank()) return false
            SpeechRecognizer.isRecognitionAvailable(context)
        } catch (e: Throwable) {
            false
        }
    }

    fun hasRecordAudioPermission(): Boolean {
        return try {
            if (context.packageName.isNullOrBlank()) return false
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        } catch (e: Throwable) {
            false
        }
    }

    fun startListening(
        timeoutMs: Long,
        listener: SpeechToTextListener
    ) {
        startListening(AgentLanguage.ENGLISH, timeoutMs, listener)
    }

    fun startListening(
        language: AgentLanguage = AgentLanguage.ENGLISH,
        timeoutMs: Long = 10000L,
        listener: SpeechToTextListener
    ) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { startListening(language, timeoutMs, listener) }
            return
        }

        if (!isAvailable()) {
            listener.onError(
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                "Speech recognizer is unavailable on this device."
            )
            return
        }

        if (!hasRecordAudioPermission()) {
            listener.onError(
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS,
                "RECORD_AUDIO permission is required for speech recognition."
            )
            return
        }

        stopListeningInternal()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            if (speechRecognizer == null) {
                listener.onError(
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                    "Failed to create SpeechRecognizer instance."
                )
                return
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    listener.onReadyForSpeech()
                }

                override fun onBeginningOfSpeech() {
                    listener.onBeginningOfSpeech()
                }

                override fun onRmsChanged(rmsdB: Float) {
                    listener.onRmsChanged(rmsdB)
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    cancelTimeout()
                }

                override fun onError(error: Int) {
                    isListening = false
                    cancelTimeout()
                    val msg = getErrorMessage(error)
                    listener.onError(error, msg)
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    cancelTimeout()
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = if (!matches.isNullOrEmpty()) matches[0] else ""
                    listener.onResults(text)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        listener.onPartialResults(matches[0])
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.sttTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language.sttTag)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }

            isListening = true
            speechRecognizer?.startListening(intent)

            timeoutRunnable = Runnable {
                if (isListening) {
                    cancel()
                    listener.onError(
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                        "Speech recognition timed out."
                    )
                }
            }
            mainHandler.postDelayed(timeoutRunnable!!, timeoutMs)

        } catch (e: Throwable) {
            isListening = false
            cancelTimeout()
            listener.onError(
                SpeechRecognizer.ERROR_CLIENT,
                "Failed to start speech recognition: ${e.message}"
            )
        }
    }

    fun stopListening() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { stopListening() }
            return
        }
        stopListeningInternal()
    }

    private fun stopListeningInternal() {
        cancelTimeout()
        if (isListening) {
            try {
                speechRecognizer?.stopListening()
            } catch (ignored: Throwable) {}
            isListening = false
        }
    }

    fun cancel() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { cancel() }
            return
        }
        cancelTimeout()
        try {
            speechRecognizer?.cancel()
        } catch (ignored: Throwable) {}
        isListening = false
    }

    fun isLanguageAvailable(language: AgentLanguage): Boolean {
        return isAvailable()
    }

    fun detectAvailableLanguages(): List<AgentLanguage> {
        return if (isAvailable()) AgentLanguage.values().toList() else emptyList()
    }

    fun destroy() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { destroy() }
            return
        }
        cancelTimeout()
        try {
            speechRecognizer?.destroy()
        } catch (ignored: Throwable) {}
        speechRecognizer = null
        isListening = false
    }

    private fun cancelTimeout() {
        timeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        timeoutRunnable = null
    }

    private fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
            SpeechRecognizer.ERROR_CLIENT -> "Client side error."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions."
            SpeechRecognizer.ERROR_NETWORK -> "Network error."
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout."
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech match found."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy."
            SpeechRecognizer.ERROR_SERVER -> "Server error."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input received."
            else -> "Unknown speech recognition error ($errorCode)."
        }
    }
}
