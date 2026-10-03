package com.agent.android

import android.view.accessibility.AccessibilityNodeInfo
import com.agent.android.actions.ActionExecutionStatus
import com.agent.android.actions.UiActionExecutor
import com.agent.android.actions.UiActionRequest
import com.agent.android.actions.UiActionType
import com.agent.android.commands.CommandRegistry
import com.agent.android.execution.ExecutionController
import com.agent.android.execution.GoalDispatcherImpl
import com.agent.android.observation.AccessibilityObservationEngine
import com.agent.android.observation.ObservationBounds
import com.agent.android.observation.ObservationNode
import com.agent.android.observation.ObservationSnapshot
import com.agent.android.observation.ObservationSnapshotStore
import com.agent.android.observation.ObservationState
import com.agent.android.target.TargetQuery
import com.agent.android.target.TargetResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class Phase3IntegrationUnitTest {

    private lateinit var store: ObservationSnapshotStore
    private lateinit var observationEngine: AccessibilityObservationEngine
    private lateinit var targetResolver: TargetResolver
    private lateinit var actionExecutor: UiActionExecutor
    private lateinit var commandRegistry: CommandRegistry
    private lateinit var goalDispatcher: GoalDispatcherImpl
    private lateinit var executionController: ExecutionController

    @Before
    fun setUp() {
        store = ObservationSnapshotStore()
        observationEngine = AccessibilityObservationEngine(snapshotStore = store)
        targetResolver = TargetResolver()
        actionExecutor = UiActionExecutor(observationEngine = observationEngine)
        commandRegistry = CommandRegistry()
        executionController = ExecutionController()
        goalDispatcher = GoalDispatcherImpl(
            executionController = executionController,
            commandRegistry = commandRegistry,
            observationEngine = observationEngine,
            targetResolver = targetResolver
        )
    }

    private fun createNode(
        id: String,
        className: String = "android.widget.TextView",
        packageName: String = "com.example.app",
        text: String? = null,
        isClickable: Boolean = false,
        isLongClickable: Boolean = false,
        isEnabled: Boolean = true,
        isEditable: Boolean = false,
        isScrollable: Boolean = false
    ): ObservationNode {
        return ObservationNode(
            id = id,
            parentId = "root",
            className = className,
            packageName = packageName,
            text = text,
            contentDescription = null,
            resourceId = "res_$id",
            bounds = ObservationBounds(10, 10, 100, 50),
            isClickable = isClickable,
            isLongClickable = isLongClickable,
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
    }

    private fun createSampleSnapshot(
        snapshotId: String = "snap-100",
        packageName: String = "com.example.app",
        nodes: List<ObservationNode>
    ): ObservationSnapshot {
        val root = createNode("root", "android.widget.FrameLayout", packageName)
        return ObservationSnapshot(
            snapshotId = snapshotId,
            timestampMs = System.currentTimeMillis(),
            packageName = packageName,
            activityName = "com.example.app.MainActivity",
            windowType = null,
            rootBounds = ObservationBounds(0, 0, 1080, 1920),
            nodeCount = nodes.size + 1,
            rootNode = root,
            allNodesList = listOf(root) + nodes,
            state = ObservationState.SUCCESS,
            error = null
        )
    }

    @Test
    fun test1_ClickIntegration_PipelineSuccess() {
        val node = createNode("node-button-1", "android.widget.Button", "com.example.app", "Submit", isClickable = true)
        val initialSnapshot = createSampleSnapshot(snapshotId = "snap-click-1", nodes = listOf(node))
        store.setExplicitDisplayedSnapshot(initialSnapshot)

        val resolved = targetResolver.resolve(initialSnapshot, TargetQuery(text = "Submit"))
        assertTrue("Target should resolve", resolved.status == com.agent.android.target.TargetResolutionStatus.RESOLVED)
        assertNotNull("ResolvedTarget must exist", resolved.resolvedTarget)

        val request = UiActionRequest(
            actionType = UiActionType.CLICK,
            targetQueryText = "Submit",
            resolvedTarget = resolved.resolvedTarget,
            expectedPackage = "com.example.app",
            sourceSnapshotId = initialSnapshot.snapshotId
        )

        val result = actionExecutor.executeAction(
            request = request,
            isServiceConnectedOverride = true
        )

        assertEquals("Action result must be SUCCESS", ActionExecutionStatus.SUCCESS, result.status)
        assertEquals("Action type must match CLICK", UiActionType.CLICK, result.actionType)
    }

    @Test
    fun test2_LongClickIntegration_PipelineSuccess() {
        val node = createNode("node-item-2", "android.widget.TextView", "com.example.app", "Long Press Me", isLongClickable = true)
        val snapshot = createSampleSnapshot(snapshotId = "snap-longclick-1", nodes = listOf(node))
        store.setExplicitDisplayedSnapshot(snapshot)

        val resolved = targetResolver.resolve(snapshot, TargetQuery(text = "Long Press Me"))
        val request = UiActionRequest(
            actionType = UiActionType.LONG_CLICK,
            targetQueryText = "Long Press Me",
            resolvedTarget = resolved.resolvedTarget,
            expectedPackage = "com.example.app",
            sourceSnapshotId = snapshot.snapshotId
        )

        val result = actionExecutor.executeAction(
            request = request,
            isServiceConnectedOverride = true
        )

        assertEquals("Execution status must be SUCCESS", ActionExecutionStatus.SUCCESS, result.status)
    }

    @Test
    fun test3_TextInputIntegration_EditableValidationAndExecution() {
        val node = createNode("node-input-3", "android.widget.EditText", "com.example.app", "Search", isEditable = true)
        val snapshot = createSampleSnapshot(snapshotId = "snap-text-1", nodes = listOf(node))
        store.setExplicitDisplayedSnapshot(snapshot)

        val resolved = targetResolver.resolve(snapshot, TargetQuery(text = "Search"))
        assertTrue("Target should resolve editable node", resolved.status == com.agent.android.target.TargetResolutionStatus.RESOLVED)

        val request = UiActionRequest(
            actionType = UiActionType.TEXT_INPUT,
            targetQueryText = "Search",
            textInput = "Hello LocalAgent",
            resolvedTarget = resolved.resolvedTarget,
            expectedPackage = "com.example.app",
            sourceSnapshotId = snapshot.snapshotId
        )

        val result = actionExecutor.executeAction(
            request = request,
            isServiceConnectedOverride = true
        )

        assertEquals("Execution status must be SUCCESS", ActionExecutionStatus.SUCCESS, result.status)
    }

    @Test
    fun test4_ScrollIntegration_ForwardAndBackward() {
        val node = createNode("node-list-4", "android.widget.ScrollView", "com.example.app", text = "ListContainer", isScrollable = true)
        val snapshot = createSampleSnapshot(snapshotId = "snap-scroll-1", nodes = listOf(node))
        store.setExplicitDisplayedSnapshot(snapshot)

        val resolved = targetResolver.resolve(snapshot, TargetQuery(text = "ListContainer"))
        assertTrue("Target should resolve scrollable node", resolved.status == com.agent.android.target.TargetResolutionStatus.RESOLVED)

        val forwardReq = UiActionRequest(
            actionType = UiActionType.SCROLL_FORWARD,
            targetQueryText = "ListContainer",
            resolvedTarget = resolved.resolvedTarget,
            expectedPackage = "com.example.app",
            sourceSnapshotId = snapshot.snapshotId
        )

        val forwardResult = actionExecutor.executeAction(
            request = forwardReq,
            isServiceConnectedOverride = true
        )

        assertEquals("Status must be SUCCESS", ActionExecutionStatus.SUCCESS, forwardResult.status)

        val backwardReq = UiActionRequest(
            actionType = UiActionType.SCROLL_BACKWARD,
            targetQueryText = "ListContainer",
            resolvedTarget = resolved.resolvedTarget,
            expectedPackage = "com.example.app",
            sourceSnapshotId = snapshot.snapshotId
        )

        val backwardResult = actionExecutor.executeAction(
            request = backwardReq,
            isServiceConnectedOverride = true
        )

        assertEquals("Status must be SUCCESS", ActionExecutionStatus.SUCCESS, backwardResult.status)
    }

    @Test
    fun test5_GlobalActionIntegration_BackHomeRecents() {
        val snapshot = createSampleSnapshot(snapshotId = "snap-global-1", nodes = emptyList())
        store.setExplicitDisplayedSnapshot(snapshot)

        val backReq = UiActionRequest(actionType = UiActionType.GLOBAL_BACK)
        val backRes = actionExecutor.executeAction(backReq, isServiceConnectedOverride = true)
        assertEquals("BACK status must be SUCCESS", ActionExecutionStatus.SUCCESS, backRes.status)

        val homeReq = UiActionRequest(actionType = UiActionType.GLOBAL_HOME)
        val homeRes = actionExecutor.executeAction(homeReq, isServiceConnectedOverride = true)
        assertEquals("HOME status must be SUCCESS", ActionExecutionStatus.SUCCESS, homeRes.status)

        val recentsReq = UiActionRequest(actionType = UiActionType.GLOBAL_RECENTS)
        val recentsRes = actionExecutor.executeAction(recentsReq, isServiceConnectedOverride = true)
        assertEquals("RECENTS status must be SUCCESS", ActionExecutionStatus.SUCCESS, recentsRes.status)
    }

    @Test
    fun test6_StaleSnapshotProtection_RejectsAction() {
        val activeSnap = createSampleSnapshot(snapshotId = "snap-active-999", nodes = emptyList())
        store.startTestRun("P3.2-INTEG-001")
        store.setExplicitDisplayedSnapshot(activeSnap)

        val staleReq = UiActionRequest(
            actionType = UiActionType.CLICK,
            targetQueryText = "Button",
            sourceSnapshotId = "snap-stale-000"
        )

        val result = actionExecutor.executeAction(staleReq, isServiceConnectedOverride = true)
        assertEquals("Stale snapshot request must be rejected", ActionExecutionStatus.TARGET_STALE, result.status)
        assertFalse("State changed must be false for stale target", result.stateChanged)
    }

    @Test
    fun test7_PackageMismatch_RejectsAction() {
        val currentSnap = createSampleSnapshot(packageName = "com.example.currentapp", nodes = emptyList())
        store.setExplicitDisplayedSnapshot(currentSnap)

        val req = UiActionRequest(
            actionType = UiActionType.CLICK,
            targetQueryText = "Button",
            expectedPackage = "com.example.otherapp"
        )

        val result = actionExecutor.executeAction(req, isServiceConnectedOverride = true)
        assertEquals("Package mismatch request must be rejected", ActionExecutionStatus.WRONG_PACKAGE, result.status)
    }

    @Test
    fun test8_ServiceDisconnection_ReturnsAccessibilityUnavailable() {
        val req = UiActionRequest(actionType = UiActionType.GLOBAL_BACK)

        val result = actionExecutor.executeAction(req, service = null, isServiceConnectedOverride = false)
        assertEquals("Disconnected service must evaluate to ACCESSIBILITY_UNAVAILABLE", ActionExecutionStatus.ACCESSIBILITY_UNAVAILABLE, result.status)
        assertFalse("State changed must be false", result.stateChanged)
    }

    @Test
    fun test9_ReObservation_SnapshotIdentityDiffers() {
        val node1 = createNode("1", text = "State 1")
        val snap1 = createSampleSnapshot(snapshotId = "snap-v1", nodes = listOf(node1))
        store.setExplicitDisplayedSnapshot(snap1)

        val node2 = createNode("1", text = "State 2")
        val snap2 = createSampleSnapshot(snapshotId = "snap-v2", nodes = listOf(node2))
        store.setExplicitDisplayedSnapshot(snap2)

        assertNotEquals("Pre and post action snapshot IDs must differ", snap1.snapshotId, snap2.snapshotId)
    }

    @Test
    fun test11_OverlayActionIntegration() {
        val showRes = goalDispatcher.dispatchAndProcessWithLock("overlay show")
        assertEquals("OVERLAY_SHOW", showRes.operation)

        val hideRes = goalDispatcher.dispatchAndProcessWithLock("overlay hide")
        assertEquals("OVERLAY_HIDE", hideRes.operation)

        val statusRes = goalDispatcher.dispatchAndProcessWithLock("overlay status")
        assertEquals("OVERLAY_STATUS", statusRes.operation)
    }

    @Test
    fun test10_ConsoleCommandRouting_AllActionCommands() {
        val button = createNode("btn-1", "android.widget.Button", "com.example.app", "Submit", isClickable = true)
        val snap = createSampleSnapshot(nodes = listOf(button))
        store.setExplicitDisplayedSnapshot(snap)

        val backDetails = goalDispatcher.dispatchAndProcessWithLock("action back")
        assertEquals("Console 'action back' should attempt execution", "ACTION_EXECUTION", backDetails.operation)

        val homeDetails = goalDispatcher.dispatchAndProcessWithLock("action home")
        assertEquals("Console 'action home' should attempt execution", "ACTION_EXECUTION", homeDetails.operation)

        val recentsDetails = goalDispatcher.dispatchAndProcessWithLock("action recents")
        assertEquals("Console 'action recents' should attempt execution", "ACTION_EXECUTION", recentsDetails.operation)

        val clickDetails = goalDispatcher.dispatchAndProcessWithLock("click Submit")
        assertEquals("Console 'click Submit' should attempt execution", "ACTION_EXECUTION", clickDetails.operation)

        val scrollDetails = goalDispatcher.dispatchAndProcessWithLock("scroll forward")
        assertEquals("Console 'scroll forward' should attempt execution", "ACTION_EXECUTION", scrollDetails.operation)

        val statusDetails = goalDispatcher.dispatchAndProcessWithLock("action status")
        assertEquals("Console 'action status' should execute status query", "ACTION_STATUS", statusDetails.operation)
    }
}
