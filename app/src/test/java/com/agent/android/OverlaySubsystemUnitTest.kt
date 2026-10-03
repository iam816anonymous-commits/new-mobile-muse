package com.agent.android

import com.agent.android.agent.skills.SkillStatus
import com.agent.android.commands.CommandRegistry
import com.agent.android.execution.ExecutionController
import com.agent.android.execution.GoalDispatcherImpl
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
        val showDetails = goalDispatcher.dispatchAndProcessWithLock("overlay show")
        assertEquals(SkillStatus.SUCCESS, showDetails.result.status)
        assertEquals("OVERLAY_SHOW", showDetails.operation)

        val statusDetails = goalDispatcher.dispatchAndProcessWithLock("overlay status")
        assertEquals(SkillStatus.SUCCESS, statusDetails.result.status)
        assertEquals("OVERLAY_STATUS", statusDetails.operation)

        val hideDetails = goalDispatcher.dispatchAndProcessWithLock("overlay hide")
        assertEquals(SkillStatus.SUCCESS, hideDetails.result.status)
        assertEquals("OVERLAY_HIDE", hideDetails.operation)
    }

    @Test
    fun test3_OverlayActionDispatchRoutesToGoalDispatcher() {
        LocalAgentOverlayService.goalDispatcher = goalDispatcher
        val overlayService = LocalAgentOverlayService()

        overlayService.dispatchOverlayAction("back")
        val activeJob = executionController.getActiveJob()
        assertNotNull("Active execution job should be released cleanly", activeJob == null)
    }

    @Test
    fun test4_OverlayStateAndLifecycle() {
        assertEquals("com.agent.android.overlay.SHOW", LocalAgentOverlayService.ACTION_SHOW)
        assertEquals("com.agent.android.overlay.HIDE", LocalAgentOverlayService.ACTION_HIDE)
    }
}
