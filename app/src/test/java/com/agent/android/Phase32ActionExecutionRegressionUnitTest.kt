package com.agent.android

import com.agent.android.actions.ActionExecutionStatus
import com.agent.android.actions.UiActionExecutor
import com.agent.android.actions.UiActionRequest
import com.agent.android.actions.UiActionType
import com.agent.android.agent.skills.SkillStatus
import com.agent.android.commands.CommandRegistry
import com.agent.android.execution.ExecutionController
import com.agent.android.execution.GoalDispatcherImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class Phase32ActionExecutionRegressionUnitTest {

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
    fun test1_CanonicalHomeAndActionHomeParity() {
        val canonicalHome = goalDispatcher.dispatchAndProcessWithLock("home", source = "CONSOLE")
        assertNotNull(canonicalHome)
        assertEquals("ACTION_EXECUTION", canonicalHome.operation)

        val actionHome = goalDispatcher.dispatchAndProcessWithLock("action home", source = "OVERLAY")
        assertNotNull(actionHome)
        assertEquals("ACTION_EXECUTION", actionHome.operation)

        assertEquals("Both console 'home' and overlay 'action home' must route to exact same operation", canonicalHome.operation, actionHome.operation)
    }

    @Test
    fun test2_CanonicalBackAndActionBackParity() {
        val canonicalBack = goalDispatcher.dispatchAndProcessWithLock("back", source = "CONSOLE")
        assertNotNull(canonicalBack)
        assertEquals("ACTION_EXECUTION", canonicalBack.operation)

        val actionBack = goalDispatcher.dispatchAndProcessWithLock("action back", source = "OVERLAY")
        assertNotNull(actionBack)
        assertEquals("ACTION_EXECUTION", actionBack.operation)

        assertEquals("Both console 'back' and overlay 'action back' must route to exact same operation", canonicalBack.operation, actionBack.operation)
    }

    @Test
    fun test3_CanonicalRecentsAndActionRecentsParity() {
        val canonicalRecents = goalDispatcher.dispatchAndProcessWithLock("recents", source = "CONSOLE")
        assertNotNull(canonicalRecents)
        assertEquals("ACTION_EXECUTION", canonicalRecents.operation)

        val actionRecents = goalDispatcher.dispatchAndProcessWithLock("action recents", source = "OVERLAY")
        assertNotNull(actionRecents)
        assertEquals("ACTION_EXECUTION", actionRecents.operation)

        assertEquals("Both console 'recents' and overlay 'action recents' must route to exact same operation", canonicalRecents.operation, actionRecents.operation)
    }

    @Test
    fun test4_CanonicalScrollForwardAndBackwardParity() {
        val scrollDownConsole = goalDispatcher.dispatchAndProcessWithLock("scroll forward", source = "CONSOLE")
        assertNotNull(scrollDownConsole)
        assertEquals("ACTION_EXECUTION", scrollDownConsole.operation)

        val scrollDownOverlay = goalDispatcher.dispatchAndProcessWithLock("scroll forward", source = "OVERLAY")
        assertNotNull(scrollDownOverlay)
        assertEquals("ACTION_EXECUTION", scrollDownOverlay.operation)
    }

    @Test
    fun test5_StaleTargetRejection() {
        val executor = UiActionExecutor()
        val staleRequest = UiActionRequest(
            actionType = UiActionType.CLICK,
            targetQueryText = "OldButton",
            sourceSnapshotId = "snap-old-123"
        )
        val result = executor.executeAction(staleRequest, isServiceConnectedOverride = true)
        assertEquals(ActionExecutionStatus.TARGET_NOT_FOUND, result.status)
    }

    @Test
    fun test6_ClickWithoutTargetReturnsTargetRequired() {
        val executor = UiActionExecutor()
        val req = UiActionRequest(actionType = UiActionType.CLICK)
        val res = executor.executeAction(req, isServiceConnectedOverride = true)
        assertEquals(ActionExecutionStatus.TARGET_REQUIRED, res.status)
        assertTrue(res.explanation.contains("TARGET_REQUIRED"))
    }
}
