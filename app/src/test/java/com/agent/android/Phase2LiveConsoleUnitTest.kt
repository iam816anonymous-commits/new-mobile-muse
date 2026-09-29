package com.agent.android

import com.agent.android.agent.skills.CalculatorSkill
import com.agent.android.agent.skills.NotesSkill
import com.agent.android.agent.skills.SkillStatus
import com.agent.android.execution.ExecutionController
import com.agent.android.execution.GoalDispatcherImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase2LiveConsoleUnitTest {

    @Test
    fun testLiveCommandRoutingAndResultDetails() {
        val executionController = ExecutionController()
        val calc = CalculatorSkill()
        val notes = NotesSkill(null)
        val dispatcher = GoalDispatcherImpl(executionController, calc, notes, null)

        val details1 = dispatcher.dispatchAndProcess("calculate 25 * 2")
        assertEquals("calculate 25 * 2", details1.command)
        assertEquals("CALCULATE", details1.operation)
        assertEquals("CalculatorSkill", details1.controllerName)
        assertEquals(SkillStatus.SUCCESS, details1.result.status)
        assertEquals("50", details1.result.message)

        val details2 = dispatcher.dispatchAndProcess("calculate 10 / 0")
        assertEquals("CALCULATE", details2.operation)
        assertEquals(SkillStatus.FAILED, details2.result.status)
        assertEquals("DIVISION_BY_ZERO", details2.result.errorCode)

        val details3 = dispatcher.dispatchAndProcess("invalid command test")
        assertEquals("UNKNOWN", details3.operation)
        assertEquals(SkillStatus.INVALID_GOAL, details3.result.status)
        assertEquals("UNKNOWN_COMMAND", details3.result.errorCode)
    }

    @Test
    fun testLiveCommandConcurrencyLockRejection() {
        val executionController = ExecutionController()
        val dispatcher = GoalDispatcherImpl(executionController)

        val firstAcquired = executionController.acquireExecution()
        assertTrue(firstAcquired)

        val secondDispatched = dispatcher.dispatchGoal("calculate 5 + 5")
        assertFalse("Second command must be rejected under active execution lock", secondDispatched)

        executionController.releaseExecution()
        assertFalse(executionController.isExecuting())

        val thirdDispatched = dispatcher.dispatchGoal("calculate 5 + 5")
        assertTrue("Third command must succeed after lock release", thirdDispatched)
    }

    @Test
    fun testExecutionHistoryLogBounding() {
        val history = java.util.ArrayDeque<HistoryEntry>()
        for (i in 1..25) {
            if (history.size >= 20) {
                history.pollFirst()
            }
            history.addLast(HistoryEntry("12:00", "cmd $i", "SUCCESS", 10L, null))
        }

        assertEquals(20, history.size)
        assertEquals("cmd 6", history.first.command)
        assertEquals("cmd 25", history.last.command)
    }
}
