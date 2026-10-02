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

    private val targetController = Phase3TestTargetController(context)
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

        val resolvedPkg = targetController.resolveTargetSpec(testId).expectedPackage ?: targetController.resolveCalculatorPackage()

        if (resolvedPkg == null) {
            currentState = GuidedActionTestState.FAILED
            observationEngine.snapshotStore.endTestRun()
            val failRes = GuidedActionTestResult(
                testId = testId,
                runId = runId,
                targetApp = targetApp,
                state = GuidedActionTestState.FAILED,
                status = TestStatus.FAILED,
                expectedPackage = targetApp.staticPackage ?: "Calculator",
                actualPackage = null,
                actualActivity = null,
                actionType = actionType,
                targetQuery = targetQueryText,
                beforeNodeCount = 0,
                afterNodeCount = 0,
                validationChecks = listOf(GuidedTestValidationCheck("Package Resolution", false)),
                evidencePath = null,
                failureReason = "TARGET APPLICATION NOT INSTALLED / NOT RESOLVABLE"
            )
            currentResult = failRes
            onUpdate(failRes)
            return
        }

        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(resolvedPkg)
        if (launchIntent == null) {
            currentState = GuidedActionTestState.FAILED
            observationEngine.snapshotStore.endTestRun()
            val failRes = GuidedActionTestResult(
                testId = testId,
                runId = runId,
                targetApp = targetApp,
                state = GuidedActionTestState.FAILED,
                status = TestStatus.FAILED,
                expectedPackage = resolvedPkg,
                actualPackage = null,
                actualActivity = null,
                actionType = actionType,
                targetQuery = targetQueryText,
                beforeNodeCount = 0,
                afterNodeCount = 0,
                validationChecks = listOf(GuidedTestValidationCheck("Launch Intent Available", false)),
                evidencePath = null,
                failureReason = "NO LAUNCH INTENT FOR PACKAGE $resolvedPkg"
            )
            currentResult = failRes
            onUpdate(failRes)
            return
        }

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        currentState = GuidedActionTestState.LAUNCHING_TARGET
        observationEngine.startObservationMode()

        var prepRes = GuidedActionTestResult(
            testId = testId,
            runId = runId,
            targetApp = targetApp,
            state = GuidedActionTestState.LAUNCHING_TARGET,
            status = TestStatus.RUNNING,
            expectedPackage = resolvedPkg,
            actualPackage = null,
            actualActivity = null,
            actionType = actionType,
            targetQuery = targetQueryText,
            beforeNodeCount = 0,
            afterNodeCount = 0,
            validationChecks = emptyList(),
            evidencePath = null,
            failureReason = null
        )
        currentResult = prepRes
        onUpdate(prepRes)

        try {
            context.startActivity(launchIntent)
        } catch (e: Exception) {
            currentState = GuidedActionTestState.FAILED
            observationEngine.snapshotStore.endTestRun()
            val failRes = prepRes.copy(
                state = GuidedActionTestState.FAILED,
                status = TestStatus.FAILED,
                failureReason = "Failed to launch target app: ${e.message}"
            )
            currentResult = failRes
            onUpdate(failRes)
            return
        }

        currentState = GuidedActionTestState.WAITING_FOR_FOREGROUND

        pollRunnable = object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - startTime
                val beforeSnap = observationEngine.captureCurrentScreen()
                val currentPkg = beforeSnap.packageName

                val isRunOwnedSnapshot = beforeSnap.timestampMs >= startTime

                if (currentPkg == resolvedPkg && beforeSnap.state == ObservationState.SUCCESS && isRunOwnedSnapshot) {
                    currentState = GuidedActionTestState.TARGET_DETECTED
                    currentState = GuidedActionTestState.OBSERVING

                    val checks = mutableListOf<GuidedTestValidationCheck>()
                    checks.add(GuidedTestValidationCheck("Accessibility Service Connected", observationEngine.isServiceConnected()))
                    checks.add(GuidedTestValidationCheck("Target Package Match ($resolvedPkg)", currentPkg == resolvedPkg))
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
                        val failRes = prepRes.copy(
                            state = GuidedActionTestState.FAILED,
                            status = TestStatus.FAILED,
                            actualPackage = currentPkg,
                            actualActivity = beforeSnap.activityName,
                            beforeNodeCount = beforeSnap.nodeCount,
                            validationChecks = checks,
                            failureReason = "TARGET RESOLUTION FAILED: ${targetRes.explanation}"
                        )
                        currentResult = failRes
                        onUpdate(failRes)
                        return
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
                        actualPackage = currentPkg,
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
                    return
                }

                if (elapsed >= WAIT_TIMEOUT_MS) {
                    currentState = GuidedActionTestState.TIMED_OUT
                    observationEngine.snapshotStore.endTestRun()
                    val failRes = GuidedActionTestResult(
                        testId = testId,
                        runId = runId,
                        targetApp = targetApp,
                        state = GuidedActionTestState.TIMED_OUT,
                        status = TestStatus.FAILED,
                        expectedPackage = resolvedPkg,
                        actualPackage = currentPkg,
                        actualActivity = beforeSnap.activityName,
                        actionType = actionType,
                        targetQuery = targetQueryText,
                        beforeNodeCount = 0,
                        afterNodeCount = 0,
                        validationChecks = listOf(GuidedTestValidationCheck("Target Foreground Detection Within 15s", false)),
                        evidencePath = null,
                        failureReason = "TARGET APPLICATION DID NOT BECOME FOREGROUND (Last detected: $currentPkg)"
                    )
                    currentResult = failRes
                    onUpdate(failRes)
                    return
                }

                val waitRes = prepRes.copy(
                    state = GuidedActionTestState.WAITING_FOR_FOREGROUND,
                    status = TestStatus.RUNNING,
                    actualPackage = currentPkg,
                    actualActivity = beforeSnap.activityName
                )
                currentResult = waitRes
                onUpdate(waitRes)

                handler.postDelayed(this, POLL_INTERVAL_MS)
            }
        }

        handler.postDelayed(pollRunnable!!, POLL_INTERVAL_MS)
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
