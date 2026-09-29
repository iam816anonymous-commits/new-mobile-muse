package com.agent.android

import com.agent.android.actions.Action
import com.agent.android.actions.ActionController
import com.agent.android.execution.ExecutionState
import com.agent.android.execution.ExecutionStateMachine
import com.agent.android.learning.LearningRecord
import com.agent.android.safety.SafetyState
import com.agent.android.safety.SafetyStatus
import com.agent.android.storage.Logger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase0UnitTest {

    @Test
    fun testExecutionStateMachineValidTransitions() {
        val sm = ExecutionStateMachine()
        assertEquals(ExecutionState.IDLE, sm.currentState)

        assertTrue(sm.transitionTo(ExecutionState.RECEIVING))
        assertEquals(ExecutionState.RECEIVING, sm.currentState)

        assertTrue(sm.transitionTo(ExecutionState.PLANNING))
        assertEquals(ExecutionState.PLANNING, sm.currentState)

        assertTrue(sm.transitionTo(ExecutionState.EXECUTING))
        assertEquals(ExecutionState.EXECUTING, sm.currentState)

        assertTrue(sm.transitionTo(ExecutionState.VERIFYING))
        assertEquals(ExecutionState.VERIFYING, sm.currentState)

        assertTrue(sm.transitionTo(ExecutionState.IDLE))
        assertEquals(ExecutionState.IDLE, sm.currentState)
    }

    @Test
    fun testExecutionStateMachineInvalidTransitions() {
        val sm = ExecutionStateMachine()
        assertEquals(ExecutionState.IDLE, sm.currentState)

        // Invalid direct transition IDLE -> EXECUTING
        assertFalse(sm.transitionTo(ExecutionState.EXECUTING))
        assertEquals(ExecutionState.IDLE, sm.currentState)

        // Invalid direct transition IDLE -> VERIFYING
        assertFalse(sm.transitionTo(ExecutionState.VERIFYING))
        assertEquals(ExecutionState.IDLE, sm.currentState)
    }

    @Test
    fun testActionControllerOneActionAtATime() {
        val controller = ActionController()
        val action1 = object : Action {
            override val id = "act-1"
            override val actionType = "TEST"
            override val description = "Test Action 1"
        }

        val result = controller.executeAction(action1) {
            assertTrue(controller.isExecuting())
            assertEquals("act-1", controller.getCurrentActionId())
            com.agent.android.actions.ActionResult("act-1", true, "Executed")
        }

        assertTrue(result.success)
        assertFalse(controller.isExecuting())
    }

    @Test
    fun testSafetyStateContract() {
        val state = SafetyState(status = SafetyStatus.SAFE_IDLE)
        assertEquals(SafetyStatus.SAFE_IDLE, state.status)
        assertFalse(state.isEmergencyStopActive)
    }

    @Test
    fun testLearningSchemaSerializationAndValidation() {
        val record = LearningRecord(
            schemaVersion = 1,
            recordId = "rec-101",
            source = "test",
            confidence = 0.95
        )

        val jsonString = record.toJson()
        val parsedRecord = LearningRecord.jsonToRecord(jsonString)

        assertEquals(1, parsedRecord.schemaVersion)
        assertEquals("rec-101", parsedRecord.recordId)
        assertEquals("test", parsedRecord.source)
        assertEquals(0.95, parsedRecord.confidence, 0.001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testLearningSchemaFutureVersionRejection() {
        val futureJson = """
            {
                "schemaVersion": 99,
                "recordId": "rec-future",
                "timestamp": 12345678,
                "source": "test",
                "confidence": 0.9
            }
        """.trimIndent()

        LearningRecord.jsonToRecord(futureJson)
    }

    @Test
    fun testBoundedLogger() {
        val logger = Logger(maxCapacity = 3)
        logger.i("CAT", "Message 1")
        logger.i("CAT", "Message 2")
        logger.i("CAT", "Message 3")

        assertEquals(3, logger.size())

        logger.i("CAT", "Message 4")
        assertEquals(3, logger.size())

        val logs = logger.getLogs()
        assertEquals("Message 2", logs[0].message)
        assertEquals("Message 4", logs[2].message)
    }
}
