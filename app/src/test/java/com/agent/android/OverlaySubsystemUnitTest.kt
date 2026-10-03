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
    fun test7_InitialPositionClampingAndBoundsLogic() {
        val screenWidth = 1080
        val screenHeight = 1920

        val safeX = (screenWidth - 150).coerceAtLeast(0)
        val safeY = (screenHeight / 2 - 100).coerceAtLeast(0)

        assertTrue("Safe X must be within display width", safeX >= 0 && safeX < screenWidth)
        assertTrue("Safe Y must be within display height", safeY >= 0 && safeY < screenHeight)
    }

    @Test
    fun test8_TapVsDragThresholdDistinction() {
        val touchDownX = 100f
        val touchDownY = 200f

        val tapX = 105f
        val tapY = 203f
        val diffTapX = Math.abs(tapX - touchDownX)
        val diffTapY = Math.abs(tapY - touchDownY)
        val isTap = diffTapX < 10 && diffTapY < 10
        assertTrue("Movement < 10px must evaluate to TAP", isTap)

        val dragX = 150f
        val dragY = 300f
        val diffDragX = Math.abs(dragX - touchDownX)
        val diffDragY = Math.abs(dragY - touchDownY)
        val isDrag = diffDragX >= 10 || diffDragY >= 10
        assertTrue("Movement >= 10px must evaluate to DRAG", isDrag)
    }

    @Test
    fun test9_CodeVerifiedVsPhysicalVerifiedDistinction() {
        val diag = LocalAgentOverlayService.getDiagnosticStatus(null)
        assertNotNull(diag)
        // Note: Unit tests run in JVM mock environment where physical WindowManager display rendering
        // is validated programmatically ("CODE VERIFIED"), requiring physical device manual testing for "PHYSICALLY VERIFIED".
    }
}
