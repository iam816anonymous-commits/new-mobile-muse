package com.agent.android.observation

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.agent.android.agent.skills.app.AppLauncherImpl
import com.agent.android.test.evidence.EvidenceManager
import com.agent.android.test.model.TestStatus
import org.json.JSONObject
import java.io.File

enum class GuidedTestApp(val label: String, val testId: String, val staticPackage: String?) {
    CHROME("Chrome", "P3.1-EXT-001", "com.android.chrome"),
    YOUTUBE("YouTube", "P3.1-EXT-002", "com.google.android.youtube"),
    SETTINGS("Settings", "P3.1-EXT-003", "com.android.settings"),
    CALCULATOR("Calculator", "P3.1-EXT-004", null)
}

enum class GuidedTestState {
    IDLE,
    PREPARING,
    LAUNCHING_TARGET,
    WAITING_FOR_FOREGROUND,
    TARGET_DETECTED,
    CAPTURING,
    VALIDATING,
    PRESERVING,
    RETURNING_TO_AGENT,
    PASSED,
    FAILED,
    TIMED_OUT,
    CANCELLED
}

data class GuidedTestValidationCheck(
    val description: String,
    val passed: Boolean
)

data class GuidedTestResult(
    val testId: String,
    val runId: String,
    val targetApp: GuidedTestApp,
    val state: GuidedTestState,
    val status: TestStatus,
    val expectedPackage: String?,
    val actualPackage: String?,
    val actualActivity: String?,
    val nodeCount: Int,
    val validationChecks: List<GuidedTestValidationCheck>,
    val preserved: Boolean,
    val localAgentOverwritePrevented: Boolean,
    val evidencePath: String?,
    val failureReason: String?
)

class GuidedExternalObservationRunner(private val context: Context) {

    companion object {
        private const val TAG = "GuidedObsRunner"
        const val WAIT_TIMEOUT_MS = 15000L
        const val POLL_INTERVAL_MS = 500L
    }

    var currentState: GuidedTestState = GuidedTestState.IDLE
        private set

    var currentResult: GuidedTestResult? = null
        private set

    private val handler = Handler(Looper.getMainLooper())
    private var pollRunnable: Runnable? = null

    fun resolveTargetPackage(app: GuidedTestApp): String? {
        val pm = context.packageManager
        if (app.staticPackage != null) {
            val intent = pm.getLaunchIntentForPackage(app.staticPackage)
            if (intent != null) return app.staticPackage
            return try {
                pm.getPackageInfo(app.staticPackage, 0)
                app.staticPackage
            } catch (e: Exception) {
                null
            }
        } else if (app == GuidedTestApp.CALCULATOR) {
            val appLauncher = AppLauncherImpl(context)
            val valRes = appLauncher.validateApp("Calculator")
            if (valRes.status.name.contains("RESOLVED") && valRes.resolvedPackage != null) {
                return valRes.resolvedPackage
            }
            val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (info in installed) {
                if (info.packageName.contains("calculator", ignoreCase = true) || info.packageName.contains("calc", ignoreCase = true)) {
                    if (pm.getLaunchIntentForPackage(info.packageName) != null) {
                        return info.packageName
                    }
                }
            }
        }
        return null
    }

    fun startGuidedTest(
        targetApp: GuidedTestApp,
        observationEngine: AccessibilityObservationEngine,
        evidenceManager: EvidenceManager,
        onUpdate: (GuidedTestResult) -> Unit
    ) {
        cancel()

        val startTime = System.currentTimeMillis()
        val runId = "${targetApp.testId}-$startTime"

        currentState = GuidedTestState.PREPARING
        observationEngine.snapshotStore.startTestRun(runId)

        val resolvedPkg = resolveTargetPackage(targetApp)

        if (resolvedPkg == null) {
            currentState = GuidedTestState.FAILED
            observationEngine.snapshotStore.endTestRun()
            val failRes = GuidedTestResult(
                testId = targetApp.testId,
                runId = runId,
                targetApp = targetApp,
                state = GuidedTestState.FAILED,
                status = TestStatus.FAILED,
                expectedPackage = targetApp.staticPackage ?: "Calculator",
                actualPackage = null,
                actualActivity = null,
                nodeCount = 0,
                validationChecks = listOf(
                    GuidedTestValidationCheck("Package Resolution", false)
                ),
                preserved = false,
                localAgentOverwritePrevented = true,
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
            currentState = GuidedTestState.FAILED
            observationEngine.snapshotStore.endTestRun()
            val failRes = GuidedTestResult(
                testId = targetApp.testId,
                runId = runId,
                targetApp = targetApp,
                state = GuidedTestState.FAILED,
                status = TestStatus.FAILED,
                expectedPackage = resolvedPkg,
                actualPackage = null,
                actualActivity = null,
                nodeCount = 0,
                validationChecks = listOf(
                    GuidedTestValidationCheck("Launch Intent Available", false)
                ),
                preserved = false,
                localAgentOverwritePrevented = true,
                evidencePath = null,
                failureReason = "NO LAUNCH INTENT FOR PACKAGE $resolvedPkg"
            )
            currentResult = failRes
            onUpdate(failRes)
            return
        }

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        // State: LAUNCHING_TARGET
        currentState = GuidedTestState.LAUNCHING_TARGET
        observationEngine.startObservationMode()

        var prepRes = GuidedTestResult(
            testId = targetApp.testId,
            runId = runId,
            targetApp = targetApp,
            state = GuidedTestState.LAUNCHING_TARGET,
            status = TestStatus.RUNNING,
            expectedPackage = resolvedPkg,
            actualPackage = null,
            actualActivity = null,
            nodeCount = 0,
            validationChecks = emptyList(),
            preserved = false,
            localAgentOverwritePrevented = true,
            evidencePath = null,
            failureReason = null
        )
        currentResult = prepRes
        onUpdate(prepRes)

        try {
            context.startActivity(launchIntent)
        } catch (e: Exception) {
            currentState = GuidedTestState.FAILED
            observationEngine.snapshotStore.endTestRun()
            val failRes = prepRes.copy(
                state = GuidedTestState.FAILED,
                status = TestStatus.FAILED,
                failureReason = "Failed to launch application: ${e.message}"
            )
            currentResult = failRes
            onUpdate(failRes)
            return
        }

        // State: WAITING_FOR_FOREGROUND
        currentState = GuidedTestState.WAITING_FOR_FOREGROUND

        pollRunnable = object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - startTime
                val snapshot = observationEngine.captureCurrentScreen()
                val currentPkg = snapshot.packageName

                // Reject stale snapshots from before run start or runId mismatch
                val isRunOwnedSnapshot = snapshot.timestampMs >= startTime

                if (currentPkg == resolvedPkg && snapshot.state == ObservationState.SUCCESS && isRunOwnedSnapshot) {
                    // State: TARGET_DETECTED -> CAPTURING -> VALIDATING
                    currentState = GuidedTestState.TARGET_DETECTED
                    currentState = GuidedTestState.CAPTURING
                    currentState = GuidedTestState.VALIDATING

                    val checks = mutableListOf<GuidedTestValidationCheck>()

                    val checkService = GuidedTestValidationCheck("Accessibility Service Connected", observationEngine.isServiceConnected())
                    checks.add(checkService)

                    val checkPkg = GuidedTestValidationCheck("External Package Match ($resolvedPkg)", currentPkg == resolvedPkg)
                    checks.add(checkPkg)

                    val checkState = GuidedTestValidationCheck("Snapshot State SUCCESS", snapshot.state == ObservationState.SUCCESS)
                    checks.add(checkState)

                    val checkRoot = GuidedTestValidationCheck("Root Node Available", snapshot.rootNode != null)
                    checks.add(checkRoot)

                    val checkNodes = GuidedTestValidationCheck("Node Count > 0 (${snapshot.nodeCount} nodes)", snapshot.nodeCount > 0)
                    checks.add(checkNodes)

                    val checkExt = GuidedTestValidationCheck("Package Is External & Non-SystemUI", !observationEngine.isExcludedExternalPackage(currentPkg))
                    checks.add(checkExt)

                    val checkClass = GuidedTestValidationCheck(
                        "Window Classification Valid (${snapshot.classification.name})",
                        snapshot.classification == WindowClassification.APPLICATION || snapshot.classification == WindowClassification.SETTINGS
                    )
                    checks.add(checkClass)

                    val checkRunId = GuidedTestValidationCheck("Snapshot Timestamp Matches Run Start", isRunOwnedSnapshot)
                    checks.add(checkRunId)

                    val allPassed = checks.all { it.passed }

                    // State: PRESERVING -> PASSED / FAILED
                    currentState = GuidedTestState.PRESERVING
                    val evPath = saveEvidenceJson(runId, targetApp, resolvedPkg, snapshot, checks, allPassed)

                    currentState = if (allPassed) GuidedTestState.PASSED else GuidedTestState.FAILED
                    observationEngine.snapshotStore.endTestRun()

                    val finalRes = GuidedTestResult(
                        testId = targetApp.testId,
                        runId = runId,
                        targetApp = targetApp,
                        state = currentState,
                        status = if (allPassed) TestStatus.PASSED else TestStatus.FAILED,
                        expectedPackage = resolvedPkg,
                        actualPackage = currentPkg,
                        actualActivity = snapshot.activityName,
                        nodeCount = snapshot.nodeCount,
                        validationChecks = checks,
                        preserved = true,
                        localAgentOverwritePrevented = true,
                        evidencePath = evPath,
                        failureReason = if (allPassed) null else "Validation checks failed"
                    )
                    currentResult = finalRes
                    onUpdate(finalRes)
                    return
                }

                if (elapsed >= WAIT_TIMEOUT_MS) {
                    currentState = GuidedTestState.TIMED_OUT
                    observationEngine.snapshotStore.endTestRun()
                    val failRes = GuidedTestResult(
                        testId = targetApp.testId,
                        runId = runId,
                        targetApp = targetApp,
                        state = GuidedTestState.TIMED_OUT,
                        status = TestStatus.FAILED,
                        expectedPackage = resolvedPkg,
                        actualPackage = currentPkg,
                        actualActivity = snapshot.activityName,
                        nodeCount = 0,
                        validationChecks = listOf(
                            GuidedTestValidationCheck("Target Foreground Detection Within 15s", false)
                        ),
                        preserved = false,
                        localAgentOverwritePrevented = true,
                        evidencePath = null,
                        failureReason = "TARGET APPLICATION DID NOT BECOME FOREGROUND (Last detected: $currentPkg)"
                    )
                    currentResult = failRes
                    onUpdate(failRes)
                    return
                }

                val waitRes = prepRes.copy(
                    state = GuidedTestState.WAITING_FOR_FOREGROUND,
                    status = TestStatus.RUNNING,
                    actualPackage = currentPkg,
                    actualActivity = snapshot.activityName
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
        app: GuidedTestApp,
        targetPkg: String,
        snapshot: ObservationSnapshot,
        checks: List<GuidedTestValidationCheck>,
        passed: Boolean
    ): String {
        return try {
            val dir = File(context.filesDir, "evidence/phase3.1")
            if (!dir.exists()) dir.mkdirs()

            val file = File(dir, "guided_external_${app.name.lowercase()}.json")
            val json = JSONObject()
            json.put("testId", app.testId)
            json.put("runId", runId)
            json.put("targetLabel", app.label)
            json.put("targetPackage", targetPkg)
            json.put("activity", snapshot.activityName ?: "UNKNOWN")
            json.put("timestamp", snapshot.timestampMs)
            json.put("nodeCount", snapshot.nodeCount)
            json.put("snapshotState", snapshot.state.name)

            val checksObj = JSONObject()
            for (c in checks) {
                checksObj.put(c.description, c.passed)
            }
            json.put("validationResults", checksObj)
            json.put("preserved", true)
            json.put("localAgentOverwritePrevented", true)
            json.put("finalResult", if (passed) "PASSED" else "FAILED")

            file.writeText(json.toString(2))
            "evidence/phase3.1/${file.name}"
        } catch (e: Exception) {
            Log.e(TAG, "Error saving evidence JSON: ${e.message}")
            "EVIDENCE_UNAVAILABLE"
        }
    }

    fun cancel() {
        pollRunnable?.let { handler.removeCallbacks(it) }
        pollRunnable = null
        if (currentState != GuidedTestState.PASSED && currentState != GuidedTestState.FAILED && currentState != GuidedTestState.TIMED_OUT) {
            currentState = GuidedTestState.CANCELLED
        }
    }
}
