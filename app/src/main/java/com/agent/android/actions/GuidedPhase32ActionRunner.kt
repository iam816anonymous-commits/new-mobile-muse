package com.agent.android.actions

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.agent.android.observation.AccessibilityObservationEngine
import com.agent.android.observation.GuidedTestApp
import com.agent.android.observation.GuidedTestValidationCheck
import com.agent.android.observation.ObservationSnapshot
import com.agent.android.observation.ObservationState
import com.agent.android.observation.Phase3TestTargetController
import com.agent.android.target.TargetQuery
import com.agent.android.target.TargetResolver
import com.agent.android.target.TargetResolutionStatus
import com.agent.android.test.model.TestStatus
import org.json.JSONObject
import java.io.File

enum class GuidedActionTestState {
    IDLE,
    PREPARING,
    LAUNCHING_TARGET,
    WAITING_FOR_FOREGROUND,
    TARGET_DETECTED,
    OBSERVING,
    TARGET_RESOLVED,
    VALIDATING,
    EXECUTING,
    POST_ACTION_OBSERVATION,
    VERIFYING,
    PASSED,
    FAILED,
    TIMED_OUT,
    CANCELLED
}

data class GuidedActionTestResult(
    val testId: String,
    val runId: String,
    val targetApp: GuidedTestApp,
    val state: GuidedActionTestState,
    val status: TestStatus,
    val expectedPackage: String?,
    val actualPackage: String?,
    val actualActivity: String?,
    val actionType: UiActionType,
    val targetQuery: String?,
    val beforeNodeCount: Int,
    val afterNodeCount: Int,
    val validationChecks: List<GuidedTestValidationCheck>,
    val evidencePath: String?,
    val failureReason: String?
)

class GuidedPhase32ActionRunner(private val context: Context) {

    companion object {
        private const val TAG = "GuidedActionRunner"
        const val WAIT_TIMEOUT_MS = 15000L
        const val POLL_INTERVAL_MS = 500L
    }

    var currentState: GuidedActionTestState = GuidedActionTestState.IDLE
        private set

    var currentResult: GuidedActionTestResult? = null
        private set

    private val targetLauncher = ControlledTestAppLauncher(context)
    private val targetResolver = TargetResolver()
    private val actionExecutor = UiActionExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private var pollRunnable: Runnable? = null

    fun startGuidedActionTest(
        testId: String,
        targetApp: GuidedTestApp,
        actionType: UiActionType,
        targetQueryText: String,
        textInputVal: String? = null,
        observationEngine: AccessibilityObservationEngine,
        onUpdate: (GuidedActionTestResult) -> Unit
    ) {
        cancel()

        val startTime = System.currentTimeMillis()
        val runId = "$testId-$startTime"

        currentState = GuidedActionTestState.PREPARING
        observationEngine.snapshotStore.startTestRun(runId)

        currentState = GuidedActionTestState.LAUNCHING_TARGET
        observationEngine.startObservationMode()

        targetLauncher.launchAndWaitForForeground(targetApp, observationEngine) { launchRes ->
            if (launchRes.status != LaunchStatus.SUCCESS) {
                currentState = if (launchRes.status == LaunchStatus.FOREGROUND_TIMEOUT) GuidedActionTestState.TIMED_OUT else GuidedActionTestState.FAILED
                observationEngine.snapshotStore.endTestRun()
                val failRes = GuidedActionTestResult(
                    testId = testId,
                    runId = runId,
                    targetApp = targetApp,
                    state = currentState,
                    status = TestStatus.FAILED,
                    expectedPackage = launchRes.expectedPackage,
                    actualPackage = launchRes.actualPackage,
                    actualActivity = launchRes.actualActivity,
                    actionType = actionType,
                    targetQuery = targetQueryText,
                    beforeNodeCount = 0,
                    afterNodeCount = 0,
                    validationChecks = listOf(GuidedTestValidationCheck("Foreground Confirmation", false)),
                    evidencePath = null,
                    failureReason = launchRes.message
                )
                currentResult = failRes
                onUpdate(failRes)
                return@launchAndWaitForForeground
            }

            val resolvedPkg = launchRes.actualPackage ?: launchRes.expectedPackage ?: ""
            val beforeSnap = observationEngine.captureCurrentScreen()

            currentState = GuidedActionTestState.TARGET_DETECTED
            currentState = GuidedActionTestState.OBSERVING

            val checks = mutableListOf<GuidedTestValidationCheck>()
            checks.add(GuidedTestValidationCheck("Accessibility Service Connected", observationEngine.isServiceConnected()))
            checks.add(GuidedTestValidationCheck("Target Package Match ($resolvedPkg)", beforeSnap.packageName == resolvedPkg))
            checks.add(GuidedTestValidationCheck("Pre-Action Snapshot SUCCESS", beforeSnap.state == ObservationState.SUCCESS))

            // Resolve target node
            currentState = GuidedActionTestState.TARGET_RESOLVED
            val targetRes = targetResolver.resolve(beforeSnap, TargetQuery(text = targetQueryText))
            val resolvedTarget = targetRes.resolvedTarget

            val checkTargetResolved = GuidedTestValidationCheck("Target Resolution ($targetQueryText)", targetRes.status == TargetResolutionStatus.RESOLVED && resolvedTarget != null)
            checks.add(checkTargetResolved)

            if (resolvedTarget == null) {
                currentState = GuidedActionTestState.FAILED
                observationEngine.snapshotStore.endTestRun()
                val failRes = GuidedActionTestResult(
                    testId = testId,
                    runId = runId,
                    targetApp = targetApp,
                    state = GuidedActionTestState.FAILED,
                    status = TestStatus.FAILED,
                    expectedPackage = resolvedPkg,
                    actualPackage = beforeSnap.packageName,
                    actualActivity = beforeSnap.activityName,
                    actionType = actionType,
                    targetQuery = targetQueryText,
                    beforeNodeCount = beforeSnap.nodeCount,
                    afterNodeCount = 0,
                    validationChecks = checks,
                    evidencePath = null,
                    failureReason = "TARGET RESOLUTION FAILED: ${targetRes.explanation}"
                )
                currentResult = failRes
                onUpdate(failRes)
                return@launchAndWaitForForeground
            }

            // Validate action preconditions
            currentState = GuidedActionTestState.VALIDATING
            val actionReq = UiActionRequest(
                actionType = actionType,
                targetQueryText = targetQueryText,
                resolvedTarget = resolvedTarget,
                expectedPackage = resolvedPkg,
                textInput = textInputVal,
                sourceSnapshotId = beforeSnap.snapshotId
            )

            val executorInstance = UiActionExecutor(observationEngine = observationEngine)

            currentState = GuidedActionTestState.EXECUTING
            val actionRes = executorInstance.executeAction(actionReq, isServiceConnectedOverride = true)

            currentState = GuidedActionTestState.POST_ACTION_OBSERVATION
            val afterSnap = observationEngine.captureCurrentScreen()

            currentState = GuidedActionTestState.VERIFYING
            checks.add(GuidedTestValidationCheck("Action Execution SUCCESS (${actionRes.status})", actionRes.status == ActionExecutionStatus.SUCCESS))
            checks.add(GuidedTestValidationCheck("Post-Action Snapshot Captured", afterSnap.state == ObservationState.SUCCESS))

            val allPassed = checks.all { it.passed }
            val evPath = saveEvidenceJson(runId, testId, targetApp, resolvedPkg, beforeSnap, afterSnap, actionRes, checks, allPassed)

            currentState = if (allPassed) GuidedActionTestState.PASSED else GuidedActionTestState.FAILED
            observationEngine.snapshotStore.endTestRun()

            val finalRes = GuidedActionTestResult(
                testId = testId,
                runId = runId,
                targetApp = targetApp,
                state = currentState,
                status = if (allPassed) TestStatus.PASSED else TestStatus.FAILED,
                expectedPackage = resolvedPkg,
                actualPackage = beforeSnap.packageName,
                actualActivity = afterSnap.activityName ?: beforeSnap.activityName,
                actionType = actionType,
                targetQuery = targetQueryText,
                beforeNodeCount = beforeSnap.nodeCount,
                afterNodeCount = afterSnap.nodeCount,
                validationChecks = checks,
                evidencePath = evPath,
                failureReason = if (allPassed) null else actionRes.explanation
            )
            currentResult = finalRes
            onUpdate(finalRes)
        }
    }

    private fun saveEvidenceJson(
        runId: String,
        testId: String,
        app: GuidedTestApp,
        targetPkg: String,
        beforeSnap: ObservationSnapshot,
        afterSnap: ObservationSnapshot,
        actionRes: UiActionResult,
        checks: List<GuidedTestValidationCheck>,
        passed: Boolean
    ): String {
        return try {
            val dir = File(context.filesDir, "evidence/phase3.2")
            if (!dir.exists()) dir.mkdirs()

            val file = File(dir, "guided_action_${app.name.lowercase()}.json")
            val json = JSONObject()
            json.put("testId", testId)
            json.put("runId", runId)
            json.put("targetLabel", app.label)
            json.put("targetPackage", targetPkg)
            json.put("actionType", actionRes.actionType.name)
            json.put("actionStatus", actionRes.status.name)
            json.put("beforeNodeCount", beforeSnap.nodeCount)
            json.put("afterNodeCount", afterSnap.nodeCount)
            json.put("explanation", actionRes.explanation)

            val checksObj = JSONObject()
            for (c in checks) {
                checksObj.put(c.description, c.passed)
            }
            json.put("validationResults", checksObj)
            json.put("finalResult", if (passed) "PASSED" else "FAILED")

            file.writeText(json.toString(2))
            "evidence/phase3.2/${file.name}"
        } catch (e: Exception) {
            Log.e(TAG, "Error saving Phase 3.2 evidence JSON: ${e.message}")
            "EVIDENCE_UNAVAILABLE"
        }
    }

    fun cancel() {
        pollRunnable?.let { handler.removeCallbacks(it) }
        pollRunnable = null
        if (currentState != GuidedActionTestState.PASSED && currentState != GuidedActionTestState.FAILED && currentState != GuidedActionTestState.TIMED_OUT) {
            currentState = GuidedActionTestState.CANCELLED
        }
    }
}
