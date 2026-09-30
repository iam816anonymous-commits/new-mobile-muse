# LocalAgent STT & TTS Platform Abstractions

## Design Principles
1. **Zero External Cloud Dependencies**: Uses built-in Android platform APIs (`SpeechRecognizer` and `TextToSpeech`). No Gemini, OpenAI, or Google Cloud STT/TTS SDKs.
2. **On-Device Execution & Fallback**: Handles missing engines or denied permissions gracefully with explicit status returns.
3. **Bounded & Lifecycle Safe**: Engines are initialized on-demand and destroyed cleanly when parent Activity is destroyed. No background continuous listening or speaking loops.

## SpeechToTextEngine (`com.agent.android.speech.SpeechToTextEngine`)
- **API**: `android.speech.SpeechRecognizer`
- **Methods**:
  - `isAvailable(): Boolean`
  - `hasRecordAudioPermission(): Boolean`
  - `startListening(timeoutMs: Long, listener: SpeechToTextListener)`
  - `stopListening()`
  - `cancel()`
  - `destroy()`
- **Error Codes Handled**: Permission denied, Speech timeout, No match, Recognizer busy, Network error.

## TextToSpeechEngine (`com.agent.android.speech.TextToSpeechEngine`)
- **API**: `android.speech.tts.TextToSpeech`
- **Methods**:
  - `initialize(onResult: ((Boolean) -> Unit)?)`
  - `isAvailable(): Boolean`
  - `isLanguageAvailable(locale: Locale): Boolean`
  - `speak(text: String, listener: TextToSpeechListener?): Boolean`
  - `stop()`
  - `shutdown()`
- **Utterance Tracking**: Unique UUID per utterance with start, done, and error callbacks.
