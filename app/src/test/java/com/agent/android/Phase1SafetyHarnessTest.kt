package com.agent.android

import android.view.KeyEvent
import com.agent.android.execution.ExecutionController
import com.agent.android.execution.ExecutionState
import com.agent.android.safety.CancellationReason
import com.agent.android.safety.CentralCancellationManager
import com.agent.android.safety.ExecutionTimeoutPolicy
import com.agent.android.safety.MasterWatchdog
import com.agent.android.safety.SafetyStatus
import com.agent.android.service.LocalAgentAccessibilityService
import com.agent.android.storage.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase1SafetyHarnessTest {

    @Test
    fun testSingleExecutionOwnershipAcquisitionAndRelease() {
        val controller = ExecutionController()
        assertFalse(controller.isExecuting())

        val acquired = controller.acquireExecution()
        assertTrue(acquired)
        assertTrue(controller.isExecuting())

        // Second acquisition attempt must be rejected
        val concurrentAcquired = controller.acquireExecution()
        assertFalse(concurrentAcquired)

        controller.releaseExecution()
        assertFalse(controller.isExecuting())
        assertEquals(ExecutionState.IDLE, controller.stateMachine.currentState)
    }

    @Test
    fun testExecutionOwnershipReleaseOnException() {
        val controller = ExecutionController()
        controller.acquireExecution()
        assertTrue(controller.isExecuting())

        try {
            throw RuntimeException("Simulated execution crash")
        } catch (e: Exception) {
            controller.releaseExecution(CancellationReason.INTERNAL_FAILURE)
        }

        assertFalse(controller.isExecuting())
        assertEquals(SafetyStatus.FAILED, controller.safetyState.status)
        assertEquals(ExecutionState.IDLE, controller.stateMachine.currentState)
    }

    @Test
    fun testCentralizedCancellationManager() {
        val manager = CentralCancellationManager()
        assertFalse(manager.isCancellationRequested())
        assertEquals(CancellationReason.NONE, manager.getCancellationReason())

        manager.requestCancellation(CancellationReason.USER_PANIC)
        assertTrue(manager.isCancellationRequested())
        assertEquals(CancellationReason.USER_PANIC, manager.getCancellationReason())

        var thrown = false
        try {
            manager.throwIfCancellationRequested()
        } catch (e: CentralCancellationManager.CancellationException) {
            thrown = true
        }
        assertTrue(thrown)

        manager.reset()
        assertFalse(manager.isCancellationRequested())
        assertEquals(CancellationReason.NONE, manager.getCancellationReason())
    }

    @Test
    fun testMasterWatchdogTimeoutFiresAndCancelsExecution() = runBlocking {
        val controller = ExecutionController()
        val job = Job()
        controller.acquireExecution(job)

        val logger = Logger()
        val policy = ExecutionTimeoutPolicy(masterTimeoutMs = 100L)
        val watchdog = MasterWatchdog(controller, policy, logger, this)

        watchdog.startMonitoring("action-test", 100L)
        kotlinx.coroutines.delay(200L)

        assertTrue(controller.cancellationManager.isCancellationRequested())
        assertEquals(CancellationReason.WATCHDOG_TIMEOUT, controller.cancellationManager.getCancellationReason())
        assertFalse(controller.isExecuting())
        assertEquals(SafetyStatus.WATCHDOG_TIMEOUT, controller.safetyState.status)
    }

    @Test
    fun testMasterWatchdogCancellationWhenJobCompletesNormally() = runBlocking {
        val controller = ExecutionController()
        val job = Job()
        controller.acquireExecution(job)

        val logger = Logger()
        val policy = ExecutionTimeoutPolicy(masterTimeoutMs = 500L)
        val watchdog = MasterWatchdog(controller, policy, logger, this)

        watchdog.startMonitoring("action-fast", 500L)
        // Complete execution early
        watchdog.stopMonitoring()
        controller.releaseExecution()

        kotlinx.coroutines.delay(600L)

        assertFalse(controller.cancellationManager.isCancellationRequested())
        assertEquals(SafetyStatus.SAFE_IDLE, controller.safetyState.status)
    }

    @Test
    fun testDoubleVolumeUpPanicGestureDetection() {
        val service = LocalAgentAccessibilityService()
        val controller = ExecutionController()
        val job = Job()
        controller.acquireExecution(job)
        service.executionController = controller

        val consumed1 = service.handleKeyEventInternal(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.ACTION_DOWN, 100L)
        assertFalse("First press should not be consumed", consumed1)
        assertTrue("Execution should still be active after 1st press", controller.isExecuting())

        val consumed2 = service.handleKeyEventInternal(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.ACTION_DOWN, 200L)
        assertTrue("Second press within 500ms should be consumed", consumed2)

        assertTrue(controller.cancellationManager.isCancellationRequested())
        assertEquals(CancellationReason.USER_PANIC, controller.cancellationManager.getCancellationReason())
        assertFalse(controller.isExecuting())
        assertEquals(SafetyStatus.PANIC, controller.safetyState.status)
    }

    @Test
    fun testVolumeUpOutsideThresholdDoesNotTriggerPanic() {
        val service = LocalAgentAccessibilityService()
        val controller = ExecutionController()
        controller.acquireExecution()
        service.executionController = controller

        service.handleKeyEventInternal(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.ACTION_DOWN, 100L)
        assertTrue(controller.isExecuting())

        val consumed2 = service.handleKeyEventInternal(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.ACTION_DOWN, 800L)
        assertFalse(consumed2)
        assertTrue(controller.isExecuting())
    }

    @Test
    fun testUnrelatedKeysDoNotTriggerPanic() {
        val service = LocalAgentAccessibilityService()
        val controller = ExecutionController()
        controller.acquireExecution()
        service.executionController = controller

        val consumed = service.handleKeyEventInternal(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_DOWN, 100L)
        assertFalse(consumed)
        assertTrue(controller.isExecuting())
        assertFalse(controller.cancellationManager.isCancellationRequested())
    }
}
