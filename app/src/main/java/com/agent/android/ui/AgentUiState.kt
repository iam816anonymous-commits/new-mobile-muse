package com.agent.android.ui

import com.agent.android.speech.AgentLanguage

enum class AgentUiState {
    IDLE,
    LISTENING,
    TRANSCRIBING,
    PROCESSING,
    CONFIRMATION_REQUIRED,
    EXECUTING,
    OBSERVING,
    VERIFYING,
    SPEAKING,
    SUCCESS,
    ERROR
}

data class UiModel(
    val state: AgentUiState = AgentUiState.IDLE,
    val selectedLanguage: AgentLanguage = AgentLanguage.ENGLISH,
    val lastTranscript: String? = null,
    val resolvedCommand: String? = null,
    val executionMessage: String? = null,
    val isMicActive: Boolean = false,
    val isSpeaking: Boolean = false,
    val isConfirmationPending: Boolean = false,
    val errorMessage: String? = null
)
