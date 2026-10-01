package com.agent.android

import com.agent.android.commands.CommandCategory
import com.agent.android.commands.CommandRegistry
import com.agent.android.commands.CommandStatus
import com.agent.android.execution.ExecutionController
import com.agent.android.execution.GoalDispatcherImpl
import com.agent.android.test.FoundationTestRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase24CommandRegistryUnitTest {

    @Test
    fun testCommandUniquenessAndMetadataValidity() {
        val registry = CommandRegistry()
        val allCmds = registry.getAllCommands()

        assertTrue("Command registry must contain at least 30 commands", allCmds.size >= 30)

        val idSet = mutableSetOf<String>()
        for (cmd in allCmds) {
            assertNotNull("CommandId must not be null", cmd.commandId)
            assertTrue("CommandId must not be empty", cmd.commandId.isNotBlank())
            assertTrue("CommandId '${cmd.commandId}' must be unique", idSet.add(cmd.commandId))

            assertNotNull("Name must not be null", cmd.name)
            assertNotNull("Category must not be null", cmd.category)
            assertNotNull("Description must not be null", cmd.description)
            assertNotNull("Syntax must not be null", cmd.syntax)
            assertNotNull("HandlerIdentifier must not be null", cmd.handlerIdentifier)
            assertTrue("Examples must not be empty", cmd.examples.isNotEmpty())
        }
    }

    @Test
    fun testGoalDispatcherCommandResolution() {
        val controller = ExecutionController()
        val dispatcher = GoalDispatcherImpl(controller)

        val calcDetails = dispatcher.dispatchAndProcessWithLock("calculate 10 + 20")
        assertEquals("CALCULATE", calcDetails.operation)
        assertEquals("CalculatorSkill", calcDetails.controllerName)

        val flashDetails = dispatcher.dispatchAndProcessWithLock("flashlight on")
        assertEquals("FLASHLIGHT", flashDetails.operation)

        val unknownDetails = dispatcher.dispatchAndProcessWithLock("fake_unknown_cmd_123")
        assertEquals("UNKNOWN", unknownDetails.operation)
        assertEquals("UNKNOWN_COMMAND", unknownDetails.result.errorCode)
    }

    @Test
    fun testVolumeCommandResolutionSeparation() {
        val registry = CommandRegistry()

        val statusCmd = registry.findCommandForInput("volume music status")
        assertNotNull(statusCmd)
        assertEquals("volume.music.status", statusCmd?.commandId)

        val setCmd = registry.findCommandForInput("volume music 50")
        assertNotNull(setCmd)
        assertEquals("volume.music.set", setCmd?.commandId)

        val currentCmd = registry.findCommandForInput("volume music current")
        assertNotNull(currentCmd)
        assertEquals("volume.music.current", currentCmd?.commandId)

        val maxCmd = registry.findCommandForInput("volume music maximum")
        assertNotNull(maxCmd)
        assertEquals("volume.music.maximum", maxCmd?.commandId)

        val pctCmd = registry.findCommandForInput("volume music percentage")
        assertNotNull(pctCmd)
        assertEquals("volume.music.percentage", pctCmd?.commandId)
    }

    @Test
    fun testArgumentParsing() {
        val registry = CommandRegistry()

        val calcDef = registry.getCommandById("calculator.calculate")
        assertNotNull(calcDef)
        val calcArgs = registry.parseArguments("calculate (25 + 5) * 2", calcDef!!)
        assertEquals("(25 + 5) * 2", calcArgs.getString("expression"))

        val timerDef = registry.getCommandById("timer.create")
        assertNotNull(timerDef)
        val timerArgs = registry.parseArguments("timer 120", timerDef!!)
        assertEquals(120, timerArgs.getInt("seconds"))

        val volDef = registry.getCommandById("volume.music.set")
        assertNotNull(volDef)
        val volArgs = registry.parseArguments("volume music 75", volDef!!)
        assertEquals(75, volArgs.getInt("percentage"))
    }

    @Test
    fun testCrossValidationTestRegistryCommandMapping() {
        val registry = CommandRegistry()
        val testRegistry = FoundationTestRegistry()

        val registeredIds = registry.getAllCommands().map { it.commandId }.toSet()
        val allTestCases = testRegistry.getAllTestCases()

        for (tc in allTestCases) {
            if (tc.commandId.isNotEmpty() && tc.commandId != "unknown.command" && tc.commandId != "sensor.unknown") {
                assertTrue(
                    "Test case '${tc.id}' references commandId '${tc.commandId}' which must exist in CommandRegistry",
                    registeredIds.contains(tc.commandId)
                )
            }
        }
    }
}
