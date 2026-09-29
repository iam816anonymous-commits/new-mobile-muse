package com.agent.android

import com.agent.android.execution.ExecutionController
import com.agent.android.safety.Phase1SafetyTestHarness
import com.agent.android.storage.Logger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase1TestHarnessUnitTest {

    @Test
    fun testPhase1SafetyTestHarnessIndividualTests() {
        val harness = Phase1SafetyTestHarness()

        val resOwnership = harness.testExecutionOwnership()
        assertTrue("Execution ownership test failed: ${resOwnership.errorMessage}", resOwnership.passed)

        val resConcurrent = harness.testConcurrentExecutionRejection()
        assertTrue("Concurrent rejection test failed: ${resConcurrent.errorMessage}", resConcurrent.passed)

        val resCancellation = harness.testManualCancellation()
        assertTrue("Manual cancellation test failed: ${resCancellation.errorMessage}", resCancellation.passed)

        val resWatchdog = harness.testWatchdogTimeout()
        assertTrue("Watchdog timeout test failed: ${resWatchdog.errorMessage}", resWatchdog.passed)

        val resPanic = harness.testPanicLogicSimulation()
        assertTrue("Panic simulation test failed: ${resPanic.errorMessage}", resPanic.passed)

        val resReset = harness.testStateReset()
        assertTrue("State reset test failed: ${resReset.errorMessage}", resReset.passed)
    }

    @Test
    fun testPhase1SafetyTestHarnessRunAllSuite() {
        val harness = Phase1SafetyTestHarness()
        val summary = harness.runAllTests()

        assertEquals(6, summary.totalCount)
        assertEquals(6, summary.passedCount)
        assertEquals(0, summary.failedCount)
        assertTrue(summary.overallPassed)
    }
}
