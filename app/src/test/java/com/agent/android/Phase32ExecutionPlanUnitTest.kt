package com.agent.android

import com.agent.android.execution.ExecutionPlan
import com.agent.android.execution.ExecutionPlanStatus
import com.agent.android.execution.ExecutionStep
import com.agent.android.execution.FailurePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase32ExecutionPlanUnitTest {

    @Test
    fun testExecutionStepCreationAndWaitPrimitive() {
        val step1 = ExecutionStep("step-1", "flashlight.on")
        assertEquals("step-1", step1.stepId)
        assertEquals("flashlight.on", step1.commandId)
        assertEquals(false, step1.isWaitPrimitive)

        val waitStep = ExecutionStep.createWaitStep("step-2", 60000L)
        assertEquals("step-2", waitStep.stepId)
        assertEquals("wait", waitStep.commandId)
        assertEquals(true, waitStep.isWaitPrimitive)
        assertEquals(60000L, waitStep.waitDurationMs)
        assertEquals("60000", waitStep.arguments["durationMs"])
    }

    @Test
    fun testExecutionPlanModelRepresentation() {
        val step1 = ExecutionStep("s1", "flashlight.on")
        val step2 = ExecutionStep.createWaitStep("s2", 60000L)
        val step3 = ExecutionStep("s3", "flashlight.off")

        val plan = ExecutionPlan(
            planId = "plan-flash-60s",
            goalDescription = "Turn on flashlight for 1 minute",
            steps = listOf(step1, step2, step3),
            failurePolicy = FailurePolicy.STOP_ON_FAILURE
        )

        assertEquals("plan-flash-60s", plan.planId)
        assertEquals(3, plan.steps.size)
        assertEquals("flashlight.on", plan.steps[0].commandId)
        assertEquals(true, plan.steps[1].isWaitPrimitive)
        assertEquals("flashlight.off", plan.steps[2].commandId)
        assertEquals(FailurePolicy.STOP_ON_FAILURE, plan.failurePolicy)
        assertEquals(ExecutionPlanStatus.PLAN_CREATED, plan.status)
    }

    @Test
    fun testExecutionPlanJsonSerializationAndDeserialization() {
        val step1 = ExecutionStep("s1", "volume.music.set", mapOf("percentage" to "50"))
        val step2 = ExecutionStep("s2", "flashlight.on")

        val plan = ExecutionPlan(
            planId = "plan-vol-flash",
            goalDescription = "Set volume to 50% and turn on flashlight",
            steps = listOf(step1, step2),
            failurePolicy = FailurePolicy.CONTINUE_ON_FAILURE
        )

        val jsonStr = plan.toJsonString()
        assertNotNull(jsonStr)

        val restored = ExecutionPlan.fromJsonString(jsonStr)
        assertEquals("plan-vol-flash", restored.planId)
        assertEquals(2, restored.steps.size)
        assertEquals("volume.music.set", restored.steps[0].commandId)
        assertEquals("50", restored.steps[0].arguments["percentage"])
        assertEquals("flashlight.on", restored.steps[1].commandId)
        assertEquals(FailurePolicy.CONTINUE_ON_FAILURE, restored.failurePolicy)
    }

    @Test
    fun testFailurePolicyEnumValues() {
        val policies = FailurePolicy.values()
        assertTrue(policies.contains(FailurePolicy.STOP_ON_FAILURE))
        assertTrue(policies.contains(FailurePolicy.CONTINUE_ON_FAILURE))
        assertTrue(policies.contains(FailurePolicy.RETRY))
        assertTrue(policies.contains(FailurePolicy.ABORT))
    }
}
