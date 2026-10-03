package com.agent.android

import com.agent.android.actions.ActionExecutionStatus
import com.agent.android.actions.UiActionExecutor
import com.agent.android.actions.UiActionRequest
import com.agent.android.actions.UiActionType
import com.agent.android.actions.UiTargetValidator
import com.agent.android.agent.skills.SkillStatus
import com.agent.android.commands.CommandRegistry
import com.agent.android.execution.ExecutionController
import com.agent.android.execution.GoalDispatcherImpl
import com.agent.android.observation.ObservationBounds
import com.agent.android.observation.ObservationNode
import com.agent.android.observation.ObservationSnapshot
import com.agent.android.observation.ObservationState
import com.agent.android.observation.WindowClassification
import com.agent.android.overlay.LocalAgentOverlayService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OverlaySubsystemUnitTest {

    private lateinit var registry: CommandRegistry
    private lateinit var goalDispatcher: GoalDispatcherImpl
    private lateinit var executionController: ExecutionController

    @Before
    fun setUp() {
        registry = CommandRegistry()
        executionController = ExecutionController()
        goalDispatcher = GoalDispatcherImpl(
            executionController = executionController,
            commandRegistry = registry
        )
    }

    @Test
    fun test1_OverlayCommandRegistrations() {
        assertNotNull("overlay.show must be registered", registry.getCommandById("overlay.show"))
        assertNotNull("overlay.hide must be registered", registry.getCommandById("overlay.hide"))
        assertNotNull("overlay.status must be registered", registry.getCommandById("overlay.status"))
    }

    @Test
    fun test2_OverlayCommandDispatch() {
        val showDetails = goalDispatcher.dispatchAndProcessWithLock("overlay show", source = "OVERLAY")
        assertEquals(SkillStatus.SUCCESS, showDetails.result.status)
        assertEquals("OVERLAY_SHOW", showDetails.operation)

        val statusDetails = goalDispatcher.dispatchAndProcessWithLock("overlay status", source = "OVERLAY")
        assertEquals(SkillStatus.SUCCESS, statusDetails.result.status)
        assertEquals("OVERLAY_STATUS", statusDetails.operation)

        val hideDetails = goalDispatcher.dispatchAndProcessWithLock("overlay hide", source = "OVERLAY")
        assertEquals(SkillStatus.SUCCESS, hideDetails.result.status)
        assertEquals("OVERLAY_HIDE", hideDetails.operation)
    }

    @Test
    fun test3_OverlayActionDispatchRoutesToGoalDispatcher() {
        LocalAgentOverlayService.goalDispatcher = goalDispatcher
        val overlayService = LocalAgentOverlayService()

        overlayService.dispatchOverlayAction("action.back")
        val activeJob = executionController.getActiveJob()
        assertTrue("Active execution job should be released cleanly", activeJob == null)
    }

    @Test
    fun test4_OverlayStateAndLifecycle() {
        assertEquals("com.agent.android.overlay.SHOW", LocalAgentOverlayService.ACTION_SHOW)
        assertEquals("com.agent.android.overlay.HIDE", LocalAgentOverlayService.ACTION_HIDE)
    }

    @Test
    fun test5_OverlayPermissionAndIntentHelpers() {
        val showCmd = registry.getCommandById("overlay.show")
        assertNotNull("overlay.show should be registered", showCmd)
        assertEquals("DIAGNOSTICS", showCmd?.category?.name)
    }

    @Test
    fun test6_OverlayDiagnosticStatusFormatting() {
        val diag = LocalAgentOverlayService.getDiagnosticStatus(null)
        assertTrue(diag.contains("Movable Action Overlay Subsystem Diagnostics"))
        assertTrue(diag.contains("Overlay Service:"))
        assertTrue(diag.contains("Overlay Permission:"))
        assertTrue(diag.contains("Overlay View:"))
        assertTrue(diag.contains("Overlay Visibility:"))
        assertTrue(diag.contains("WindowManager:"))
        assertTrue(diag.contains("Position:"))
        assertTrue(diag.contains("Size:"))
        assertTrue(diag.contains("Last Error:"))
    }

    @Test
    fun test7_ClickWithoutTargetReturnsTargetRequired() {
        val details = goalDispatcher.dispatchAndProcessWithLock("action click", source = "OVERLAY")
        assertEquals(SkillStatus.FAILED, details.result.status)
        assertEquals("TARGET_REQUIRED", details.result.errorCode)
        assertTrue(details.result.message.contains("TARGET_REQUIRED"))
    }

    @Test
    fun test8_LongClickWithoutTargetReturnsTargetRequired() {
        val details = goalDispatcher.dispatchAndProcessWithLock("action long_click", source = "OVERLAY")
        assertEquals(SkillStatus.FAILED, details.result.status)
        assertEquals("TARGET_REQUIRED", details.result.errorCode)
        assertTrue(details.result.message.contains("TARGET_REQUIRED"))
    }

    @Test
    fun test9_ScrollWithoutScrollableContainerReturnsNoScrollableTarget() {
        val req = UiActionRequest(actionType = UiActionType.SCROLL_FORWARD)
        val mockNode = ObservationNode(
            id = "node-0",
            parentId = null,
            className = "android.widget.LinearLayout",
            packageName = "com.android.settings",
            text = null,
            contentDescription = null,
            resourceId = null,
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
            childCount = 0
        )
        val mockSnapshot = ObservationSnapshot(
            timestampMs = System.currentTimeMillis(),
            snapshotId = "snap-1",
            packageName = "com.android.settings",
            activityName = ".Settings",
            windowType = "APPLICATION",
            rootBounds = ObservationBounds(0, 0, 1080, 1920),
            nodeCount = 1,
            rootNode = mockNode,
            allNodesList = listOf(mockNode),
            state = ObservationState.SUCCESS,
            classification = WindowClassification.APPLICATION
        )
        val validator = UiTargetValidator()
        val valRes = validator.validateActionPreconditions(req, currentSnapshot = mockSnapshot, isServiceConnected = true)
        assertEquals(ActionExecutionStatus.NO_SCROLLABLE_TARGET, valRes.status)
        assertTrue(valRes.explanation.contains("NO_SCROLLABLE_TARGET"))
    }

    @Test
    fun test10_GlobalActionsWhenServiceDisconnectedReturnsAccessibilityUnavailable() {
        val details = goalDispatcher.dispatchAndProcessWithLock("action back", source = "OVERLAY")
        assertEquals(SkillStatus.UNAVAILABLE, details.result.status)
        assertEquals("ACCESSIBILITY_UNAVAILABLE", details.result.errorCode)
    }

    @Test
    fun test11_ObserveAndStatusOverlayCommandsParity() {
        val obsDetails = goalDispatcher.dispatchAndProcessWithLock("observe current", source = "OVERLAY")
        assertNotNull(obsDetails)

        val statusDetails = goalDispatcher.dispatchAndProcessWithLock("action status", source = "OVERLAY")
        assertNotNull(statusDetails)
    }
}
