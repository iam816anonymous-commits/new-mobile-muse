package com.agent.android

import com.agent.android.actions.ActionExecutionStatus
import com.agent.android.actions.UiActionExecutor
import com.agent.android.observation.AccessibilityObservationEngine
import com.agent.android.actions.UiActionRequest
import com.agent.android.actions.UiActionType
import com.agent.android.actions.UiTargetValidator
import com.agent.android.observation.ObservationBounds
import com.agent.android.observation.ObservationNode
import com.agent.android.observation.ObservationSnapshot
import com.agent.android.observation.ObservationState
import com.agent.android.target.ResolvedTarget
import com.agent.android.target.TargetMatchReason
import com.agent.android.test.FoundationTestRegistry
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
        isEnabled: Boolean = true
    ): ResolvedTarget {
        val node = ObservationNode(
            id = nodeId,
            parentId = "root",
            className = if (isEditable) "android.widget.EditText" else "android.widget.Button",
            packageName = packageName,
            text = "Target Text",
            contentDescription = null,
            resourceId = "target_res_id",
            bounds = ObservationBounds(10, 10, 200, 100),
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
            bounds = ObservationBounds(10, 10, 200, 100),
            isActionable = isClickable || isEditable || isScrollable,
            isEditable = isEditable,
            isScrollable = isScrollable,
            isCheckable = false,
            matchScore = 10.0f,
            matchReasons = listOf(TargetMatchReason.TEXT_EXACT)
        )
    }

    @Test
    fun testP32ACT001_ClickValidTarget() {
        setupActiveChromeSnapshot()
        val target = createResolvedTarget(isClickable = true)
        val req = UiActionRequest(
            actionType = UiActionType.CLICK,
            resolvedTarget = target,
            expectedPackage = "com.android.chrome"
        )

        val res = executor.executeAction(req, isServiceConnectedOverride = true)
        assertEquals(ActionExecutionStatus.SUCCESS, res.status)
        assertEquals(UiActionType.CLICK, res.actionType)
        assertTrue(res.explanation.contains("ACTION_CLICK executed"))
    }

    @Test
    fun testP32ACT002_LongClickValidTarget() {
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
    fun testP32ACT003_TextInputValidEditableTarget() {
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
    fun testP32ACT004_ScrollValidTarget() {
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
    fun testP32ACT005_GlobalBackAction() {
        setupActiveChromeSnapshot()
        val req = UiActionRequest(actionType = UiActionType.GLOBAL_BACK)
        val res = executor.executeAction(req, isServiceConnectedOverride = true)
        assertEquals(ActionExecutionStatus.SUCCESS, res.status)
        assertEquals(UiActionType.GLOBAL_BACK, res.actionType)
    }

    @Test
    fun testP32ACT011_ActionResultJsonSerialization() {
        setupActiveChromeSnapshot()
        val target = createResolvedTarget()
        val req = UiActionRequest(actionType = UiActionType.CLICK, resolvedTarget = target)
        val res = executor.executeAction(req, isServiceConnectedOverride = true)

        val jsonObj = res.toJsonObject()
        val restored = com.agent.android.actions.UiActionResult.fromJsonObject(jsonObj)

        assertEquals(res.requestId, restored.requestId)
        assertEquals(ActionExecutionStatus.SUCCESS, restored.status)
        assertEquals(UiActionType.CLICK, restored.actionType)
    }

    @Test
    fun testP32NEG001_TargetDoesNotExist() {
        setupActiveChromeSnapshot()
        val req = UiActionRequest(actionType = UiActionType.CLICK, resolvedTarget = null, expectedPackage = "com.android.chrome")
        val res = executor.executeAction(req, isServiceConnectedOverride = true)

        assertEquals(ActionExecutionStatus.TARGET_NOT_FOUND, res.status)
        assertTrue(res.explanation.contains("TARGET_NOT_FOUND"))
    }

    @Test
    fun testP32NEG002_WrongPackageDetected() {
        // Foreground app is YouTube, but request expects Chrome -> WRONG_PACKAGE!
        val youtubeSnap = createSampleSnapshot("com.google.android.youtube")
        store.setExplicitDisplayedSnapshot(youtubeSnap)

        val target = createResolvedTarget(packageName = "com.google.android.youtube")
        val req = UiActionRequest(actionType = UiActionType.CLICK, resolvedTarget = target, expectedPackage = "com.android.chrome")

        val res = executor.executeAction(req, isServiceConnectedOverride = true)
        assertEquals(ActionExecutionStatus.WRONG_PACKAGE, res.status)
    }

    @Test
    fun testP32NEG003_WrongForegroundAppLocalAgent() {
        val target = createResolvedTarget(packageName = "com.android.chrome")
        val req = UiActionRequest(actionType = UiActionType.CLICK, resolvedTarget = target, expectedPackage = "com.android.chrome")

        val localAgentSnap = createSampleSnapshot(packageName = "com.agent.android")
        val valRes = validator.validateActionPreconditions(req, localAgentSnap)

        assertEquals(false, valRes.isValid)
        assertEquals(ActionExecutionStatus.WRONG_FOREGROUND_APP, valRes.status)
    }

    @Test
    fun testP32NEG004_TargetIsDisabled() {
        val target = createResolvedTarget(isEnabled = false)
        val req = UiActionRequest(actionType = UiActionType.CLICK, resolvedTarget = target, expectedPackage = "com.android.chrome")

        val snap = createSampleSnapshot(packageName = "com.android.chrome", nodes = listOf(target.node))
        val valRes = validator.validateActionPreconditions(req, snap)

        assertEquals(false, valRes.isValid)
        assertEquals(ActionExecutionStatus.TARGET_NOT_ACTIONABLE, valRes.status)
    }

    @Test
    fun testP32NEG005_TargetNotClickable() {
        val target = createResolvedTarget(isClickable = false)
        val req = UiActionRequest(actionType = UiActionType.CLICK, resolvedTarget = target, expectedPackage = "com.android.chrome")

        val snap = createSampleSnapshot(packageName = "com.android.chrome", nodes = listOf(target.node))
        val valRes = validator.validateActionPreconditions(req, snap)

        assertEquals(false, valRes.isValid)
        assertEquals(ActionExecutionStatus.ACTION_UNSUPPORTED, valRes.status)
    }

    @Test
    fun testP32NEG006_TargetNotEditableForTextInput() {
        val target = createResolvedTarget(isClickable = true, isEditable = false)
        val req = UiActionRequest(actionType = UiActionType.TEXT_INPUT, textInput = "Text", resolvedTarget = target, expectedPackage = "com.android.chrome")

        val snap = createSampleSnapshot(packageName = "com.android.chrome", nodes = listOf(target.node))
        val valRes = validator.validateActionPreconditions(req, snap)

        assertEquals(false, valRes.isValid)
        assertEquals(ActionExecutionStatus.ACTION_UNSUPPORTED, valRes.status)
    }

    @Test
    fun testP32NEG007_StaleTargetSnapshotRejection() {
        val target = createResolvedTarget()
        val req = UiActionRequest(
            actionType = UiActionType.CLICK,
            resolvedTarget = target,
            expectedPackage = "com.android.chrome",
            sourceSnapshotId = "snap-old-123"
        )

        val snap = createSampleSnapshot(packageName = "com.android.chrome", snapshotId = "snap-new-456")
        val valRes = validator.validateActionPreconditions(req, snap, activeSnapshotId = "snap-new-456")

        assertEquals(false, valRes.isValid)
        assertEquals(ActionExecutionStatus.TARGET_STALE, valRes.status)
    }

    @Test
    fun testP32NEG008_AccessibilityServiceUnavailable() {
        val target = createResolvedTarget()
        val req = UiActionRequest(actionType = UiActionType.CLICK, resolvedTarget = target)

        val snap = createSampleSnapshot()
        val valRes = validator.validateActionPreconditions(req, snap, isServiceConnected = false)

        assertEquals(false, valRes.isValid)
        assertEquals(ActionExecutionStatus.ACCESSIBILITY_UNAVAILABLE, valRes.status)
    }

    @Test
    fun testFoundationTestRegistryPhase32ActionCases() {
        val testRegistry = FoundationTestRegistry()
        val p32ActionCases = testRegistry.getTestCasesByPhase("PHASE_3.2").filter { it.id.startsWith("P3.2-ACT-") }
        val p32NegCases = testRegistry.getTestCasesByPhase("PHASE_3.2").filter { it.id.startsWith("P3.2-NEG-") }

        assertEquals(13, p32ActionCases.size)
        assertEquals(12, p32NegCases.size)
    }
}
