package com.agent.android

import com.agent.android.agent.skills.CalculatorSkill
import com.agent.android.agent.skills.NotesSkill
import com.agent.android.agent.skills.Phase2HeadlessTestHarness
import com.agent.android.agent.skills.SkillStatus
import com.agent.android.execution.ExecutionController
import com.agent.android.execution.GoalDispatcherImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase2HeadlessUnitTest {

    @Test
    fun testCalculatorExpressionPrecedenceAndParentheses() {
        val calc = CalculatorSkill()
        val res1 = calc.calculate("2 + 3 * 4")
        assertEquals(SkillStatus.SUCCESS, res1.status)
        assertEquals("14", res1.message)

        val res2 = calc.calculate("(2 + 3) * 4")
        assertEquals(SkillStatus.SUCCESS, res2.status)
        assertEquals("20", res2.message)

        val res3 = calc.calculate("10.5 / 2")
        assertEquals(SkillStatus.SUCCESS, res3.status)
        assertEquals("5.25", res3.message)
    }

    @Test
    fun testCalculatorErrorHandling() {
        val calc = CalculatorSkill()
        val divZero = calc.calculate("10 / 0")
        assertEquals(SkillStatus.FAILED, divZero.status)
        assertEquals("DIVISION_BY_ZERO", divZero.errorCode)

        val malformed = calc.calculate("(2 + 3")
        assertEquals(SkillStatus.FAILED, malformed.status)
        assertEquals("INVALID_EXPRESSION", malformed.errorCode)
    }

    @Test
    fun testNotesSkillEmptyContent() {
        val notes = NotesSkill(null)
        val res = notes.addNote("   ")
        assertEquals(SkillStatus.FAILED, res.status)
        assertEquals("EMPTY_NOTE", res.errorCode)
    }

    @Test
    fun testGoalDispatcherRoutingAndSafetyWrapping() {
        val executionController = ExecutionController()
        val calc = CalculatorSkill()
        val notes = NotesSkill(null)
        val dispatcher = GoalDispatcherImpl(executionController, calc, notes, null)

        val success = dispatcher.dispatchGoal("calculate 25 * 2")
        assertTrue(success)

        val unknown = dispatcher.dispatchAndProcess("invalid command").result
        assertEquals(SkillStatus.INVALID_GOAL, unknown.status)
    }

    @Test
    fun testPhase2HeadlessTestHarnessSuite() {
        val harness = Phase2HeadlessTestHarness()
        val summary = harness.runAllHeadlessTests()
        assertEquals(4, summary.totalCount)
        assertEquals(4, summary.passedCount)
        assertTrue(summary.overallPassed)
    }
}
