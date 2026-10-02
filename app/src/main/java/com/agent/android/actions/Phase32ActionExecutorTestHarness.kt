package com.agent.android.actions

import android.accessibilityservice.AccessibilityService
import android.content.Context
import com.agent.android.observation.ScreenObserver
import com.agent.android.test.FoundationTestRegistry
import com.agent.android.test.model.TestStatus
import org.json.JSONObject
import java.io.File

class Phase32ActionExecutorTestHarness(
    private val context: Context?,
    private val executor: Phase32ActionExecutor = Phase32ActionExecutor(),
    private val testRegistry: FoundationTestRegistry = FoundationTestRegistry()
) {

    fun executePhase32TestCase(
        testId: String,
        service: AccessibilityService?,
        currentForegroundPackage: String?,
        node: android.view.accessibility.AccessibilityNodeInfo?,
        screenObserver: ScreenObserver? = null
    ): Phase32ActionResult {
        val startMs = System.currentTimeMillis()

        val request = when (testId) {
            "P3.2-ACT-001", "P3.2-ACT-008" -> ActionRequest("act_001", ActionType.CLICK, expectedPackage = currentForegroundPackage)
            "P3.2-ACT-002" -> ActionRequest("act_002", ActionType.LONG_CLICK, expectedPackage = currentForegroundPackage)
            "P3.2-ACT-003", "P3.2-ACT-009" -> ActionRequest("act_003", ActionType.TEXT_INPUT, inputText = "TestInput123", expectedPackage = currentForegroundPackage)
            "P3.2-ACT-004", "P3.2-ACT-010" -> ActionRequest("act_004", ActionType.SCROLL, scrollDirection = "FORWARD", expectedPackage = currentForegroundPackage)
            "P3.2-ACT-005" -> ActionRequest("act_005", ActionType.BACK)
            "P3.2-ACT-006", "P3.2-ACT-007" -> ActionRequest("act_006", ActionType.CLICK, expectedPackage = currentForegroundPackage)
            "P3.2-ACT-011" -> ActionRequest("act_011", ActionType.CLICK)
            "P3.2-ACT-012", "P3.2-NEG-009" -> ActionRequest("act_012", ActionType.CLICK)
            "P3.2-ACT-013", "P3.2-NEG-010" -> ActionRequest("act_013", ActionType.CLICK)

            "P3.2-NEG-001" -> ActionRequest("neg_001", ActionType.CLICK, targetIdentity = "nonexistent_target_123")
            "P3.2-NEG-002" -> ActionRequest("neg_002", ActionType.CLICK, expectedPackage = "com.nonexistent.wrong.package")
            "P3.2-NEG-003" -> ActionRequest("neg_003", ActionType.CLICK, expectedPackage = "com.different.app")
            "P3.2-NEG-004" -> ActionRequest("neg_004", ActionType.CLICK)
            "P3.2-NEG-005" -> ActionRequest("neg_005", ActionType.CLICK)
            "P3.2-NEG-006" -> ActionRequest("neg_006", ActionType.TEXT_INPUT, inputText = "SampleText")
            "P3.2-NEG-007" -> ActionRequest("neg_007", ActionType.CLICK)
            "P3.2-NEG-008" -> ActionRequest("neg_008", ActionType.CLICK)
            "P3.2-NEG-011" -> ActionRequest("neg_011", ActionType.CLICK, expectedPackage = "com.android.chrome")
            "P3.2-NEG-012" -> ActionRequest("neg_012", ActionType.TEXT_INPUT, inputText = "VerificationFailureTestStr")

            else -> ActionRequest("generic_act", ActionType.CLICK)
        }

        val result = if (testId == "P3.2-NEG-008") {
            executor.executeActionRequest(request, null, currentForegroundPackage, node, screenObserver)
        } else if (testId == "P3.2-ACT-012" || testId == "P3.2-NEG-009") {
            executor.executeActionRequest(request, service, currentForegroundPackage, node, screenObserver, isCancelled = true)
        } else if (testId == "P3.2-ACT-013" || testId == "P3.2-NEG-010") {
            executor.executeActionRequest(request, service, currentForegroundPackage, node, screenObserver, isTimedOut = true)
        } else if (testId == "P3.2-NEG-001") {
            executor.executeActionRequest(request, service, currentForegroundPackage, null, screenObserver)
        } else if (testId == "P3.2-NEG-011" && currentForegroundPackage == "com.agent.android") {
            executor.executeActionRequest(request, service, "com.agent.android", node, screenObserver)
        } else {
            executor.executeActionRequest(request, service, currentForegroundPackage, node, screenObserver)
        }

        val duration = System.currentTimeMillis() - startMs

        val expectedStatusMatch = when {
            testId.startsWith("P3.2-ACT-") -> result.status == ActionStatus.SUCCESS
            testId == "P3.2-NEG-001" -> result.status == ActionStatus.TARGET_NOT_FOUND
            testId == "P3.2-NEG-002" -> result.status == ActionStatus.WRONG_PACKAGE || result.status == ActionStatus.WRONG_FOREGROUND_APP
            testId == "P3.2-NEG-003" -> result.status == ActionStatus.WRONG_FOREGROUND_APP
            testId == "P3.2-NEG-004" -> result.status == ActionStatus.TARGET_NOT_ACTIONABLE || result.status == ActionStatus.TARGET_NOT_FOUND
            testId == "P3.2-NEG-005" -> result.status == ActionStatus.ACTION_UNSUPPORTED || result.status == ActionStatus.TARGET_NOT_ACTIONABLE || result.status == ActionStatus.TARGET_NOT_FOUND
            testId == "P3.2-NEG-006" -> result.status == ActionStatus.ACTION_UNSUPPORTED || result.status == ActionStatus.TARGET_NOT_ACTIONABLE || result.status == ActionStatus.TARGET_NOT_FOUND
            testId == "P3.2-NEG-007" -> result.status == ActionStatus.TARGET_STALE || result.status == ActionStatus.TARGET_NOT_FOUND
            testId == "P3.2-NEG-008" -> result.status == ActionStatus.ACCESSIBILITY_UNAVAILABLE
            testId == "P3.2-NEG-009" -> result.status == ActionStatus.CANCELLED
            testId == "P3.2-NEG-010" -> result.status == ActionStatus.TIMEOUT
            testId == "P3.2-NEG-011" -> result.status == ActionStatus.WRONG_FOREGROUND_APP
            testId == "P3.2-NEG-012" -> result.status == ActionStatus.VERIFICATION_FAILED || result.status == ActionStatus.TARGET_NOT_FOUND
            else -> result.status == ActionStatus.SUCCESS
        }

        val finalTestStatus = if (expectedStatusMatch) TestStatus.PASSED else TestStatus.FAILED

        testRegistry.updateTestCase(
            id = testId,
            status = finalTestStatus,
            observedResult = result.message,
            error = if (!expectedStatusMatch) "Expected status match failed: actual status ${result.status}" else null,
            duration = duration
        )

        saveEvidenceReport(testId, result, finalTestStatus)

        return result
    }

    private fun saveEvidenceReport(testId: String, result: Phase32ActionResult, testStatus: TestStatus) {
        if (context == null) return
        try {
            val dir = File(context.filesDir, "evidence/phase3.2")
            if (!dir.exists()) dir.mkdirs()

            val json = JSONObject()
            json.put("testId", testId)
            json.put("timestampMs", System.currentTimeMillis())
            json.put("testStatus", testStatus.name)
            json.put("actionResult", result.toJson())

            val file = File(dir, "$testId.json")
            file.writeText(json.toString(2))
        } catch (_: Exception) {}
    }
}
