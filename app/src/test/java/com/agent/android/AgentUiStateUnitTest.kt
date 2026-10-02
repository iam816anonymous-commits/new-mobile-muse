package com.agent.android

import com.agent.android.speech.AgentLanguage
import com.agent.android.ui.AgentUiState
import com.agent.android.ui.UiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentUiStateUnitTest {

    @Test
    fun testDefaultUiModel() {
        val model = UiModel()
        assertEquals(AgentUiState.IDLE, model.state)
        assertEquals(AgentLanguage.ENGLISH, model.selectedLanguage)
        assertFalse(model.isMicActive)
        assertFalse(model.isSpeaking)
        assertFalse(model.isConfirmationPending)
    }

    @Test
    fun testUiModelTransitions() {
        var model = UiModel()

        // 1. Transition to LISTENING
        model = model.copy(state = AgentUiState.LISTENING, isMicActive = true, lastTranscript = "Listening...")
        assertEquals(AgentUiState.LISTENING, model.state)
        assertTrue(model.isMicActive)

        // 2. Transition to PROCESSING
        model = model.copy(state = AgentUiState.PROCESSING, isMicActive = false, resolvedCommand = "flashlight.on")
        assertEquals(AgentUiState.PROCESSING, model.state)
        assertFalse(model.isMicActive)
        assertEquals("flashlight.on", model.resolvedCommand)

        // 3. Transition to EXECUTING
        model = model.copy(state = AgentUiState.EXECUTING)
        assertEquals(AgentUiState.EXECUTING, model.state)

        // 4. Transition to SPEAKING
        model = model.copy(state = AgentUiState.SPEAKING, isSpeaking = true)
        assertEquals(AgentUiState.SPEAKING, model.state)
        assertTrue(model.isSpeaking)

        // 5. Transition to SUCCESS
        model = model.copy(state = AgentUiState.SUCCESS, isSpeaking = false, executionMessage = "Flashlight turned on")
        assertEquals(AgentUiState.SUCCESS, model.state)
        assertEquals("Flashlight turned on", model.executionMessage)
    }

    @Test
    fun testConfirmationRequiredState() {
        val model = UiModel(
            state = AgentUiState.CONFIRMATION_REQUIRED,
            resolvedCommand = "notes.clear",
            isConfirmationPending = true
        )

        assertEquals(AgentUiState.CONFIRMATION_REQUIRED, model.state)
        assertTrue(model.isConfirmationPending)
        assertEquals("notes.clear", model.resolvedCommand)
    }
}
