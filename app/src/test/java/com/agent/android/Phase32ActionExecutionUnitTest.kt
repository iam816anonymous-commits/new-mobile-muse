package com.agent.android

import com.agent.android.actions.ActionExecutionState
import com.agent.android.actions.ActionRequest
import com.agent.android.actions.ActionStatus
import com.agent.android.actions.ActionType
import com.agent.android.actions.ActionValidator
import com.agent.android.actions.Phase32ActionExecutor
import com.agent.android.actions.Phase32ActionExecutorTestHarness
import com.agent.android.actions.Phase32ActionResult
import com.agent.android.test.FoundationTestRegistry
import com.agent.android.test.model.TestStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase32ActionExecutionUnitTest {

    @Test
    fun testActionModelsAndJsonSerialization() {
        val req = ActionRequest(
            actionId = "act_test_1",
            actionType = ActionType.TEXT_INPUT,
            targetIdentity = "edit_text_1",
            expectedPackage = "com.android.calculator2",
            inputText = "12345"
        )
        val jsonReq = req.toJson()
        val parsedReq = ActionRequest.fromJson(jsonReq)

        assertEquals("act_test_1", parsedReq.actionId)
        assertEquals(ActionType.TEXT_INPUT, parsedReq.actionType)
        assertEquals("edit_text_1", parsedReq.targetIdentity)
        assertEquals("com.android.calculator2", parsedReq.expectedPackage)
        assertEquals("12345", parsedReq.inputText)

        val res = Phase32ActionResult(
            actionId = "act_test_1",
            actionType = ActionType.TEXT_INPUT.name,
            status = ActionStatus.SUCCESS,
            message = "Action executed successfully",
            beforeSnapshotId = "snap_before_1",
            afterSnapshotId = "snap_after_1",
            verificationSuccess = true
        )
        val jsonRes = res.toJson()
        val parsedRes = Phase32ActionResult.fromJson(jsonRes)

        assertEquals("act_test_1", parsedRes.actionId)
        assertEquals(ActionStatus.SUCCESS, parsedRes.status)
        assertEquals("snap_before_1", parsedRes.beforeSnapshotId)
        assertEquals("snap_after_1", parsedRes.afterSnapshotId)
        assertEquals(true, parsedRes.verificationSuccess)
    }

    @Test
    fun testTargetValidationRules() {
        val validator = ActionValidator()
        val req = ActionRequest("val_1", ActionType.CLICK, expectedPackage = "com.android.settings")

        // 1. Service Null -> ACCESSIBILITY_UNAVAILABLE
        val valNullService = validator.validateAction(req, null, "com.android.settings", null)
        assertFalse(valNullService.isValid)
        assertEquals(ActionStatus.ACCESSIBILITY_UNAVAILABLE, valNullService.status)

        // 2. Cancelled -> CANCELLED
        val dummyService = object : android.accessibilityservice.AccessibilityService() {
            override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {}
            override fun onInterrupt() {}
        }
        val valCancelled = validator.validateAction(req, dummyService, "com.android.settings", null, isCancelled = true)
        assertFalse(valCancelled.isValid)
        assertEquals(ActionStatus.CANCELLED, valCancelled.status)

        // 3. Timed Out -> TIMEOUT
        val valTimeout = validator.validateAction(req, dummyService, "com.android.settings", null, isTimedOut = true)
        assertFalse(valTimeout.isValid)
        assertEquals(ActionStatus.TIMEOUT, valTimeout.status)

        // 4. Wrong Foreground App -> WRONG_FOREGROUND_APP
        val valWrongFg = validator.validateAction(req, dummyService, "com.android.calculator2", null)
        assertFalse(valWrongFg.isValid)
        assertEquals(ActionStatus.WRONG_FOREGROUND_APP, valWrongFg.status)

        // 5. Null Target Node -> TARGET_NOT_FOUND
        val valNullNode = validator.validateAction(req, dummyService, "com.android.settings", null)
        assertFalse(valNullNode.isValid)
        assertEquals(ActionStatus.TARGET_NOT_FOUND, valNullNode.status)

        // 6. BACK Action -> SUCCESS (No target node required)
        val reqBack = ActionRequest("val_back", ActionType.BACK)
        val valBack = validator.validateAction(reqBack, dummyService, "com.android.settings", null)
        assertTrue(valBack.isValid)
        assertEquals(ActionStatus.SUCCESS, valBack.status)
    }

    @Test
    fun testActionExecutorValidationFailure() {
        val executor = Phase32ActionExecutor()
        val req = ActionRequest("fail_req", ActionType.CLICK, expectedPackage = "com.android.settings")

        // Execute against null service
        val res = executor.executeActionRequest(req, null, "com.android.settings", null)

        assertFalse(res.isSuccess())
        assertEquals(ActionStatus.ACCESSIBILITY_UNAVAILABLE, res.status)
        assertEquals(ActionExecutionState.TERMINATED_WITH_ERROR, executor.currentState)
    }

    @Test
    fun testActionExecutorHarnessExecution() {
        val testRegistry = FoundationTestRegistry()
        val harness = Phase32ActionExecutorTestHarness(null, Phase32ActionExecutor(), testRegistry)

        // Test P3.2-NEG-001 (Target Not Found)
        val resNeg001 = harness.executePhase32TestCase("P3.2-NEG-001", null, "com.agent.android", null)
        assertEquals(ActionStatus.ACCESSIBILITY_UNAVAILABLE, resNeg001.status)

        // Test P3.2-NEG-008 (Accessibility Service Unavailable)
        val resNeg008 = harness.executePhase32TestCase("P3.2-NEG-008", null, "com.agent.android", null)
        assertEquals(ActionStatus.ACCESSIBILITY_UNAVAILABLE, resNeg008.status)

        // Test P3.2-NEG-009 (Cancelled)
        val dummyService = object : android.accessibilityservice.AccessibilityService() {
            override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {}
            override fun onInterrupt() {}
        }
        val resNeg009 = harness.executePhase32TestCase("P3.2-NEG-009", dummyService, "com.agent.android", null)
        assertEquals(ActionStatus.CANCELLED, resNeg009.status)

        // Test P3.2-NEG-010 (Timeout)
        val resNeg010 = harness.executePhase32TestCase("P3.2-NEG-010", dummyService, "com.agent.android", null)
        assertEquals(ActionStatus.TIMEOUT, resNeg010.status)

        val tc008 = testRegistry.getTestCaseById("P3.2-NEG-008")
        assertNotNull(tc008)
        assertEquals(TestStatus.PASSED, tc008?.status)
    }
}
