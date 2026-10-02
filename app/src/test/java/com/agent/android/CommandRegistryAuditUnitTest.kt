package com.agent.android

import com.agent.android.commands.CommandRegistry
import com.agent.android.commands.CommandStatus
import com.agent.android.execution.ExecutionController
import com.agent.android.execution.GoalDispatcherImpl
import com.agent.android.observation.AccessibilityObservationEngine
import com.agent.android.test.FoundationTestRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandRegistryAuditUnitTest {

    @Test
    fun testCommandDiscoveryHelpAndCommands() {
        val execCtrl = ExecutionController()
        val obsEngine = AccessibilityObservationEngine()
        val dispatcher = GoalDispatcherImpl(
            executionController = execCtrl,
            observationEngine = obsEngine
        )

        val helpRes = dispatcher.dispatchAndProcess("help")
        assertEquals(com.agent.android.agent.skills.SkillStatus.SUCCESS, helpRes.result.status)
        assertTrue(helpRes.result.message.contains("Available Categories"))

        val helpActionRes = dispatcher.dispatchAndProcess("help action")
        assertEquals(com.agent.android.agent.skills.SkillStatus.SUCCESS, helpActionRes.result.status)

        val commandsRes = dispatcher.dispatchAndProcess("commands")
        assertEquals(com.agent.android.agent.skills.SkillStatus.SUCCESS, commandsRes.result.status)
        assertTrue(commandsRes.result.message.contains("Registered Commands"))
    }

    @Test
    fun testNamespacedObservationCommands() {
        val execCtrl = ExecutionController()
        val obsEngine = AccessibilityObservationEngine()
        val dispatcher = GoalDispatcherImpl(
            executionController = execCtrl,
            observationEngine = obsEngine
        )

        val startRes = dispatcher.dispatchAndProcess("observe start")
        assertEquals(com.agent.android.agent.skills.SkillStatus.SUCCESS, startRes.result.status)

        val currentRes = dispatcher.dispatchAndProcess("observe current")
        assertNotNull(currentRes.result)

        val nodesRes = dispatcher.dispatchAndProcess("observe nodes")
        assertNotNull(nodesRes.result)

        val stopRes = dispatcher.dispatchAndProcess("observe stop")
        assertEquals(com.agent.android.agent.skills.SkillStatus.SUCCESS, stopRes.result.status)
    }

    @Test
    fun testSystemAndUiStateCommands() {
        val execCtrl = ExecutionController()
        val dispatcher = GoalDispatcherImpl(executionController = execCtrl)

        val sysRes = dispatcher.dispatchAndProcess("system status")
        assertEquals(com.agent.android.agent.skills.SkillStatus.SUCCESS, sysRes.result.status)

        val uiRes = dispatcher.dispatchAndProcess("ui state")
        assertEquals(com.agent.android.agent.skills.SkillStatus.SUCCESS, uiRes.result.status)
    }

    @Test
    fun testCommandRegistryTestCoverageIntegrity() {
        val registry = CommandRegistry()
        val testRegistry = FoundationTestRegistry()

        val implementedCmds = registry.getAllCommands().filter { it.status == CommandStatus.IMPLEMENTED }
        val testCases = testRegistry.getAllTestCases()
        val testedCmdIds = testCases.map { it.commandId }.toSet()

        val uncovered = implementedCmds.filter { !testedCmdIds.contains(it.commandId) }
        assertTrue("Every implemented command must have test coverage. Uncovered: ${uncovered.map { it.commandId }}", uncovered.isEmpty())
    }
}
