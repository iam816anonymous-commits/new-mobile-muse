package com.agent.android.speech

import java.util.Locale

enum class AgentLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val locale: Locale,
    val sttTag: String
) {
    ENGLISH("en", "English", "English", Locale.US, "en-US"),
    TELUGU("te", "Telugu", "తెలుగు", Locale("te", "IN"), "te-IN"),
    HINDI("hi", "Hindi", "हिन्दी", Locale("hi", "IN"), "hi-IN"),
    KANNADA("kn", "Kannada", "ಕನ್ನಡ", Locale("kn", "IN"), "kn-IN");

    companion object {
        fun fromCode(code: String): AgentLanguage {
            return values().find { it.code.equals(code, true) || it.name.equals(code, true) } ?: ENGLISH
        }
    }
}
