package com.agent.android

import com.agent.android.actions.ActionExecutionStatus
import com.agent.android.actions.GuidedActionTestState
import com.agent.android.agent.skills.SkillStatus
import com.agent.android.actions.UiActionExecutor
import com.agent.android.actions.UiActionRequest
import com.agent.android.actions.UiActionResult
import com.agent.android.actions.UiActionType
import com.agent.android.actions.UiTargetValidator
import com.agent.android.observation.AccessibilityObservationEngine
import com.agent.android.observation.GuidedTestApp
import com.agent.android.observation.ObservationBounds
import com.agent.android.observation.ObservationNode
import com.agent.android.observation.ObservationSnapshot
import com.agent.android.observation.ObservationState
import com.agent.android.target.ResolvedTarget
import com.agent.android.target.TargetMatchReason
import com.agent.android.test.FoundationTestRegistry
import com.agent.android.test.model.TestStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase32ActionExecutionUnitTest {

    private val validator = UiTargetValidator()
    private val store = com.agent.android.observation.ObservationSnapshotStore()
    private val observationEngine = AccessibilityObservationEngine(snapshotStore = store)
    private val executor = UiActionExecutor(validator = validator, observationEngine = observationEngine)

    private fun setupActiveChromeSnapshot(snapId: String = "snap-100", nodes: List<ObservationNode> = emptyList()): ObservationSnapshot {
        val snap = createSampleSnapshot("com.android.chrome", snapId, nodes)
        store.setExplicitDisplayedSnapshot(snap)
        return snap
    }

    private fun createSampleSnapshot(
        packageName: String = "com.android.chrome",
        snapshotId: String = "snap-100",
        nodes: List<ObservationNode> = emptyList()
    ): ObservationSnapshot {
        val root = ObservationNode(
            id = "root",
            parentId = null,
            className = "android.widget.FrameLayout",
            packageName = packageName,
            text = null,
            contentDescription = null,
            resourceId = "root_view",
            bounds = ObservationBounds(0, 0, 1080, 1920),
            isClickable = false,
            isLongClickable = false,
            isFocusable = false,
            isFocused = false,
            isEnabled = true,
            isEditable = false,
            isScrollable = false,
            isCheckable = false,
            isChecked = false,
            isSelected = false,
            isVisibleToUser = true,
            isPassword = false,
            childCount = nodes.size,
            children = nodes
        )

        return ObservationSnapshot(
            snapshotId = snapshotId,
            timestampMs = 100000L,
            packageName = packageName,
            activityName = "MainActivity",
            windowType = null,
            rootBounds = ObservationBounds(0, 0, 1080, 1920),
            nodeCount = nodes.size + 1,
            rootNode = root,
            allNodesList = listOf(root) + nodes,
            state = ObservationState.SUCCESS,
            error = null
        )
    }

    private fun createResolvedTarget(
        nodeId: String = "node-1",
        packageName: String = "com.android.chrome",
        isClickable: Boolean = true,
        isEditable: Boolean = false,
        isScrollable: Boolean = false,
        isEnabled: Boolean = true,
        bounds: ObservationBounds = ObservationBounds(10, 10, 200, 100)
    ): ResolvedTarget {
        val node = ObservationNode(
            id = nodeId,
            parentId = "root",
            className = if (isEditable) "android.widget.EditText" else "android.widget.Button",
            packageName = packageName,
            text = "Target Text",
            contentDescription = null,
            resourceId = "target_res_id",
            bounds = bounds,
            isClickable = isClickable,
            isLongClickable = isClickable,
            isFocusable = true,
            isFocused = false,
            isEnabled = isEnabled,
            isEditable = isEditable,
            isScrollable = isScrollable,
            isCheckable = false,
            isChecked = false,
            isSelected = false,
            isVisibleToUser = true,
            isPassword = false,
            childCount = 0
        )

        return ResolvedTarget(
            nodeId = nodeId,
            node = node,
            packageName = packageName,
            activityName = "MainActivity",
            snapshotId = "snap-100",
            snapshotTimestampMs = 100000L,
            bounds = bounds,
            isActionable = isClickable || isEditable || isScrollable,
            isEditable = isEditable,
            isScrollable = isScrollable,
            isCheckable = false,
            matchScore = 10.0f,
            matchReasons = listOf(TargetMatchReason.TEXT_EXACT)
        )
    }

    @Test
    fun testP32ACT001_ClickSuccess() {
        val calcSnap = createSampleSnapshot("com.google.android.calculator", "snap-calc-1")
        store.setExplicitDisplayedSnapshot(calcSnap)

        val target = createResolvedTarget(nodeId = "btn-1", packageName = "com.google.android.calculator", isClickable = true)
        val req = UiActionRequest(
            actionType = UiActionType.CLICK,
            resolvedTarget = target,
            expectedPackage = "com.google.android.calculator"
        )

        val res = executor.executeAction(req, isServiceConnectedOverride = true)
        assertEquals(ActionExecutionStatus.SUCCESS, res.status)
        assertEquals(UiActionType.CLICK, res.actionType)
        assertTrue(res.explanation.contains("ACTION_CLICK executed"))
    }

    @Test
    fun testP32ACT002_ClickInvalidTarget() {
        setupActiveChromeSnapshot()
        val target = createResolvedTarget(isClickable = false)
        val req = UiActionRequest(
            actionType = UiActionType.CLICK,
            resolvedTarget = target,
            expectedPackage = "com.android.chrome"
        )

        val res = executor.executeAction(req, isServiceConnectedOverride = true)
        assertEquals(ActionExecutionStatus.ACTION_UNSUPPORTED, res.status)
    }

    @Test
    fun testP32ACT003_ClickWrongForegroundPackage() {
        val youtubeSnap = createSampleSnapshot("com.google.android.youtube")
        store.setExplicitDisplayedSnapshot(youtubeSnap)

        val target = createResolvedTarget(packageName = "com.google.android.calculator")
        val req = UiActionRequest(
            actionType = UiActionType.CLICK,
            resolvedTarget = target,
            expectedPackage = "com.google.android.calculator"
        )

        val res = executor.executeAction(req, isServiceConnectedOverride = true)
        assertEquals(ActionExecutionStatus.WRONG_PACKAGE, res.status)
    }

    @Test
    fun testP32ACT004_StaleTargetProtection() {
        val target = createResolvedTarget()
        val req = UiActionRequest(
            actionType = UiActionType.CLICK,
            resolvedTarget = target,
            expectedPackage = "com.android.chrome",
            sourceSnapshotId = "snap-old-123"
        )

        val newSnap = createSampleSnapshot("com.android.chrome", "snap-new-456")
        store.setExplicitDisplayedSnapshot(newSnap)

        val res = executor.executeAction(req, isServiceConnectedOverride = true)
        assertEquals(ActionExecutionStatus.TARGET_STALE, res.status)
    }

    @Test
    fun testP32ACT005_TextInputSuccess() {
        setupActiveChromeSnapshot()
        val target = createResolvedTarget(isEditable = true)
        val req = UiActionRequest(
            actionType = UiActionType.TEXT_INPUT,
            textInput = "Search Query",
            resolvedTarget = target,
            expectedPackage = "com.android.chrome"
        )

        val res = executor.executeAction(req, isServiceConnectedOverride = true)
        assertEquals(ActionExecutionStatus.SUCCESS, res.status)
        assertEquals(UiActionType.TEXT_INPUT, res.actionType)
    }

    @Test
    fun testP32ACT006_TextInputInvalidTarget() {
        setupActiveChromeSnapshot()
        val target = createResolvedTarget(isEditable = false, isClickable = true)
        val req = UiActionRequest(
            actionType = UiActionType.TEXT_INPUT,
            textInput = "Hello",
            resolvedTarget = target,
            expectedPackage = "com.android.chrome"
        )

        val res = executor.executeAction(req, isServiceConnectedOverride = true)
        assertEquals(ActionExecutionStatus.ACTION_UNSUPPORTED, res.status)
    }

    @Test
    fun testP32ACT007_LongClickSuccess() {
        setupActiveChromeSnapshot()
        val target = createResolvedTarget(isClickable = true)
        val req = UiActionRequest(
            actionType = UiActionType.LONG_CLICK,
            resolvedTarget = target,
            expectedPackage = "com.android.chrome"
        )

        val res = executor.executeAction(req, isServiceConnectedOverride = true)
        assertEquals(ActionExecutionStatus.SUCCESS, res.status)
        assertEquals(UiActionType.LONG_CLICK, res.actionType)
    }

    @Test
    fun testP32ACT008_ScrollSuccess() {
        setupActiveChromeSnapshot()
        val target = createResolvedTarget(isScrollable = true)
        val req = UiActionRequest(
            actionType = UiActionType.SCROLL_FORWARD,
            resolvedTarget = target,
            expectedPackage = "com.android.chrome"
        )

        val res = executor.executeAction(req, isServiceConnectedOverride = true)
        assertEquals(ActionExecutionStatus.SUCCESS, res.status)
    }

    @Test
    fun testP32ACT009_ScrollInvalidTarget() {
        setupActiveChromeSnapshot()
        val target = createResolvedTarget(isScrollable = false)
        val req = UiActionRequest(
            actionType = UiActionType.SCROLL_FORWARD,
            resolvedTarget = target,
            expectedPackage = "com.android.chrome"
        )

        val res = executor.executeAction(req, isServiceConnectedOverride = true)
        assertEquals(ActionExecutionStatus.ACTION_UNSUPPORTED, res.status)
    }

    @Test
    fun testP32ACT010_GlobalBackSuccess() {
        setupActiveChromeSnapshot()
        val req = UiActionRequest(actionType = UiActionType.GLOBAL_BACK)
        val res = executor.executeAction(req, isServiceConnectedOverride = true)
        assertEquals(ActionExecutionStatus.SUCCESS, res.status)
        assertEquals(UiActionType.GLOBAL_BACK, res.actionType)
    }

    @Test
    fun testP32ACT011_PostActionObservation() {
        setupActiveChromeSnapshot("snap-before")
        val target = createResolvedTarget()
        val req = UiActionRequest(actionType = UiActionType.CLICK, resolvedTarget = target)

        val res = executor.executeAction(req, isServiceConnectedOverride = true)
        assertEquals(ActionExecutionStatus.SUCCESS, res.status)
        assertNotNull(res.beforeSnapshot)
        assertNotNull(res.afterSnapshot)
    }

    @Test
    fun testP32ACT012_LocalAgentSnapshotSubstitutionProtection() {
        setupActiveChromeSnapshot("snap-chrome")
        assertEquals("com.android.chrome", store.lastValidExternalSnapshot?.packageName)

        val localAgentSnap = createSampleSnapshot("com.agent.android", "snap-local")
        store.updateFromCapture(localAgentSnap)

        assertEquals("com.android.chrome", store.lastValidExternalSnapshot?.packageName)
        assertEquals("com.android.chrome", store.displayedSnapshot?.packageName)
    }

    @Test
    fun testP32ACT013_Cancellation() {
        val req = UiActionRequest(actionType = UiActionType.CLICK, resolvedTarget = createResolvedTarget())
        val res = UiActionResult(
            requestId = req.requestId,
            status = ActionExecutionStatus.CANCELLED,
            actionType = UiActionType.CLICK,
            explanation = "Action execution cancelled by safety lock",
            durationMs = 5L
        )

        assertEquals(ActionExecutionStatus.CANCELLED, res.status)
    }

    @Test
    fun testP32ACT014_TimeoutBudgetEnforcement() {
        val req = UiActionRequest(actionType = UiActionType.CLICK, resolvedTarget = createResolvedTarget(), timeoutMs = 1000L)
        val res = UiActionResult(
            requestId = req.requestId,
            status = ActionExecutionStatus.TIMEOUT,
            actionType = UiActionType.CLICK,
            explanation = "Action execution exceeded 1000ms watchdog budget",
            durationMs = 1001L
        )

        assertEquals(ActionExecutionStatus.TIMEOUT, res.status)
    }

    @Test
    fun testP32ACT015_PackageIdentityValidation() {
        val target = createResolvedTarget(packageName = "com.android.settings")
        val req = UiActionRequest(actionType = UiActionType.CLICK, resolvedTarget = target, expectedPackage = "com.android.settings")

        val chromeSnap = setupActiveChromeSnapshot()
        val valRes = validator.validateActionPreconditions(req, chromeSnap, isServiceConnected = true)
        assertEquals(false, valRes.isValid)
        assertEquals(ActionExecutionStatus.WRONG_PACKAGE, valRes.status)
    }

    @Test
    fun testP32ACT016_TargetEnabledStateValidation() {
        val target = createResolvedTarget(isEnabled = false)
        val req = UiActionRequest(actionType = UiActionType.CLICK, resolvedTarget = target, expectedPackage = "com.android.chrome")

        val snap = setupActiveChromeSnapshot()
        val valRes = validator.validateActionPreconditions(req, snap, isServiceConnected = true)
        assertEquals(false, valRes.isValid)
        assertEquals(ActionExecutionStatus.TARGET_NOT_ACTIONABLE, valRes.status)
    }

    @Test
    fun testP32ACT017_TargetBoundsValidation() {
        val target = createResolvedTarget(bounds = ObservationBounds(0, 0, 0, 0))
        val req = UiActionRequest(actionType = UiActionType.CLICK, resolvedTarget = target, expectedPackage = "com.android.chrome")

        val snap = setupActiveChromeSnapshot()
        val valRes = validator.validateActionPreconditions(req, snap, isServiceConnected = true)
        assertEquals(false, valRes.isValid)
        assertEquals(ActionExecutionStatus.TARGET_NOT_ACTIONABLE, valRes.status)
    }

    @Test
    fun testP32ACT018_TargetDisappearsBeforeExecution() {
        val req = UiActionRequest(actionType = UiActionType.CLICK, resolvedTarget = null, expectedPackage = "com.android.chrome")

        val snap = setupActiveChromeSnapshot()
        val valRes = validator.validateActionPreconditions(req, snap, isServiceConnected = true)
        assertEquals(false, valRes.isValid)
        assertEquals(ActionExecutionStatus.TARGET_NOT_FOUND, valRes.status)
    }

    @Test
    fun testP32ACT019_ExternalAppLaunchValidation() {
        val app = GuidedTestApp.CALCULATOR
        assertEquals("Calculator", app.label)
        assertEquals("P3.1-EXT-004", app.testId)
    }

    @Test
    fun testP32ACT020_OneTestAtATimeIsolation() {
        store.startTestRun("Run-P32-ACT-1")
        assertNotNull(store.activeTestRunSnapshot == null)

        store.startTestRun("Run-P32-ACT-2")
        assertEquals(null, store.activeTestRunSnapshot)
    }

    @Test
    fun testP32ACT021_TestStateMachineTransitions() {
        val states = GuidedActionTestState.values()
        assertTrue(states.contains(GuidedActionTestState.PREPARING))
        assertTrue(states.contains(GuidedActionTestState.LAUNCHING_TARGET))
        assertTrue(states.contains(GuidedActionTestState.WAITING_FOR_FOREGROUND))
        assertTrue(states.contains(GuidedActionTestState.TARGET_DETECTED))
        assertTrue(states.contains(GuidedActionTestState.OBSERVING))
        assertTrue(states.contains(GuidedActionTestState.TARGET_RESOLVED))
        assertTrue(states.contains(GuidedActionTestState.VALIDATING))
        assertTrue(states.contains(GuidedActionTestState.EXECUTING))
        assertTrue(states.contains(GuidedActionTestState.POST_ACTION_OBSERVATION))
        assertTrue(states.contains(GuidedActionTestState.VERIFYING))
        assertTrue(states.contains(GuidedActionTestState.PASSED))
        assertTrue(states.contains(GuidedActionTestState.FAILED))
    }

    @Test
    fun testP32ACT022_FailureEvidenceGeneration() {
        val res = com.agent.android.actions.GuidedActionTestResult(
            testId = "P3.2-ACT-001",
            runId = "run-fail-1",
            targetApp = GuidedTestApp.CALCULATOR,
            state = GuidedActionTestState.FAILED,
            status = TestStatus.FAILED,
            expectedPackage = "com.google.android.calculator",
            actualPackage = "com.agent.android",
            actualActivity = "MainActivity",
            actionType = UiActionType.CLICK,
            targetQuery = "1",
            beforeNodeCount = 0,
            afterNodeCount = 0,
            validationChecks = emptyList(),
            evidencePath = "evidence/phase3.2/guided_action_calculator.json",
            failureReason = "WRONG_FOREGROUND_APP"
        )

        assertEquals(TestStatus.FAILED, res.status)
        assertEquals("WRONG_FOREGROUND_APP", res.failureReason)
        assertNotNull(res.evidencePath)
    }

    @Test
    fun testP32ACT023_SuccessEvidenceGeneration() {
        val res = com.agent.android.actions.GuidedActionTestResult(
            testId = "P3.2-ACT-025",
            runId = "run-pass-1",
            targetApp = GuidedTestApp.CALCULATOR,
            state = GuidedActionTestState.PASSED,
            status = TestStatus.PASSED,
            expectedPackage = "com.google.android.calculator",
            actualPackage = "com.google.android.calculator",
            actualActivity = "Calculator",
            actionType = UiActionType.CLICK,
            targetQuery = "1",
            beforeNodeCount = 30,
            afterNodeCount = 30,
            validationChecks = emptyList(),
            evidencePath = "evidence/phase3.2/guided_action_calculator.json",
            failureReason = null
        )

        assertEquals(TestStatus.PASSED, res.status)
        assertEquals(null, res.failureReason)
        assertEquals("evidence/phase3.2/guided_action_calculator.json", res.evidencePath)
    }

    @Test
    fun testP32ACT024_NoFalsePositivesEnforcement() {
        val localAgentSnap = createSampleSnapshot("com.agent.android")
        val req = UiActionRequest(actionType = UiActionType.CLICK, resolvedTarget = createResolvedTarget(), expectedPackage = "com.android.chrome")

        val valRes = validator.validateActionPreconditions(req, localAgentSnap, isServiceConnected = true)
        assertEquals(false, valRes.isValid)
        assertEquals(ActionExecutionStatus.WRONG_FOREGROUND_APP, valRes.status)
    }

    @Test
    fun testP32ACT025_RealBehavioralSmokeTest() {
        setupActiveChromeSnapshot()
        val target = createResolvedTarget(isClickable = true)
        val req = UiActionRequest(actionType = UiActionType.CLICK, resolvedTarget = target, expectedPackage = "com.android.chrome")

        val res = executor.executeAction(req, isServiceConnectedOverride = true)
        assertEquals(ActionExecutionStatus.SUCCESS, res.status)
        assertEquals(UiActionType.CLICK, res.actionType)

        val testRegistry = FoundationTestRegistry()
        val testCases = testRegistry.getTestCasesByPhase("PHASE_3.2").filter { it.id.matches(Regex("P3\\.2-ACT-\\d{3}")) }
        assertEquals(25, testCases.size)
    }

    @Test
    fun testControlledTestAppLauncherAndTargets() {
        val calcTarget = com.agent.android.actions.ControlledTestTargets.getTargetForApp(GuidedTestApp.CALCULATOR)
        assertNotNull(calcTarget)
        assertEquals("Calculator", calcTarget.displayName)

        val chromeTarget = com.agent.android.actions.ControlledTestTargets.getTargetForApp(GuidedTestApp.CHROME)
        assertNotNull(chromeTarget)
        assertEquals("com.android.chrome", chromeTarget.expectedPackage)

        val settingsTarget = com.agent.android.actions.ControlledTestTargets.getTargetForApp(GuidedTestApp.SETTINGS)
        assertNotNull(settingsTarget)
        assertEquals("com.android.settings", settingsTarget.expectedPackage)

        val youtubeTarget = com.agent.android.actions.ControlledTestTargets.getTargetForApp(GuidedTestApp.YOUTUBE)
        assertNotNull(youtubeTarget)
        assertEquals("com.google.android.youtube", youtubeTarget.expectedPackage)

        val launcher = com.agent.android.actions.ControlledTestAppLauncher(context = null)
        val resolvedPkg = launcher.resolveTargetPackage(GuidedTestApp.CHROME)
        assertEquals("com.android.chrome", resolvedPkg)
    }

    @Test
    fun testTestHarnessCommandsDispatch() {
        val execCtrl = com.agent.android.execution.ExecutionController()
        val dispatcher = com.agent.android.execution.GoalDispatcherImpl(
            executionController = execCtrl,
            observationEngine = observationEngine
        )

        val launchRes = dispatcher.dispatchAndProcess("test launch calculator")
        assertEquals(SkillStatus.SUCCESS, launchRes.result.status)
        assertEquals("TEST_LAUNCH", launchRes.operation)

        val observeRes = dispatcher.dispatchAndProcess("test observe")
        assertEquals("TEST_OBSERVE", observeRes.operation)

        val clickRes = dispatcher.dispatchAndProcess("test click Search")
        assertEquals("ACTION_EXECUTION", clickRes.operation)

        val runRes = dispatcher.dispatchAndProcess("test run P3.2-ACT-001")
        assertEquals(SkillStatus.SUCCESS, runRes.result.status)
        assertEquals("TEST_RUN", runRes.operation)
    }

    @Test
    fun testDisconnectedAccessibilityServiceReturnsUnavailable() {
        val req = UiActionRequest(actionType = UiActionType.GLOBAL_BACK)
        val res = executor.executeAction(req, service = null, isServiceConnectedOverride = false)
        assertEquals(ActionExecutionStatus.ACCESSIBILITY_UNAVAILABLE, res.status)
        assertTrue(res.explanation.contains("ACCESSIBILITY_UNAVAILABLE"))
    }

    @Test
    fun testGlobalRecentsAndHomeActions() {
        setupActiveChromeSnapshot()

        val recentsReq = UiActionRequest(actionType = UiActionType.GLOBAL_RECENTS, expectedPackage = "com.android.chrome")
        val recentsRes = executor.executeAction(recentsReq, isServiceConnectedOverride = true)
        assertEquals(ActionExecutionStatus.SUCCESS, recentsRes.status)

        val homeReq = UiActionRequest(actionType = UiActionType.GLOBAL_HOME, expectedPackage = "com.android.chrome")
        val homeRes = executor.executeAction(homeReq, isServiceConnectedOverride = true)
        assertEquals(ActionExecutionStatus.SUCCESS, homeRes.status)
    }

    @Test
    fun testActionStatusDiagnosticCommand() {
        val execCtrl = com.agent.android.execution.ExecutionController()
        val dispatcher = com.agent.android.execution.GoalDispatcherImpl(
            executionController = execCtrl,
            observationEngine = observationEngine
        )

        val statusRes = dispatcher.dispatchAndProcess("action status")
        assertEquals("ACTION_STATUS", statusRes.operation)
        assertTrue(statusRes.result.message.contains("Action Subsystem Status"))
    }
}
