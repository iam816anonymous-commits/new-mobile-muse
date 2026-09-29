package com.agent.android.execution

import com.agent.android.agent.skills.CalculatorSkill
import com.agent.android.agent.skills.IntentSkills
import com.agent.android.agent.skills.NotesSkill
import com.agent.android.agent.skills.SkillResult
import com.agent.android.agent.skills.SkillStatus
import com.agent.android.safety.CancellationReason

class GoalDispatcherImpl(
    val executionController: ExecutionController,
    private val calculatorSkill: CalculatorSkill = CalculatorSkill(),
    private val notesSkill: NotesSkill = NotesSkill(),
    private val intentSkills: IntentSkills? = null
) : GoalDispatcher {

    override fun dispatchGoal(goal: String): Boolean {
        if (!executionController.acquireExecution()) {
            return false
        }
        return try {
            val result = processGoal(goal)
            executionController.releaseExecution(
                if (result.status == SkillStatus.SUCCESS) CancellationReason.NONE else CancellationReason.INTERNAL_FAILURE
            )
            result.status == SkillStatus.SUCCESS
        } catch (e: Exception) {
            executionController.releaseExecution(CancellationReason.INTERNAL_FAILURE)
            false
        }
    }

    override fun cancelCurrentGoal() {
        executionController.resetToSafeState(CancellationReason.USER_STOP)
    }

    fun processGoal(goal: String): SkillResult {
        val trimmed = goal.trim()
        val lower = trimmed.lowercase()

        return when {
            lower.startsWith("calculate") -> {
                val expr = trimmed.substringAfter("calculate").trim()
                calculatorSkill.calculate(expr)
            }
            lower.startsWith("note down") -> {
                val content = trimmed.substringAfter("note down").trim()
                notesSkill.addNote(content)
            }
            lower.startsWith("timer") -> {
                val minStr = trimmed.substringAfter("timer").replace("[^0-9]".toRegex(), "")
                val min = minStr.toIntOrNull() ?: 0
                intentSkills?.setTimer(min) ?: SkillResult("SET_TIMER", SkillStatus.UNAVAILABLE, "No Activity context", 0L)
            }
            lower.startsWith("alarm") || lower.startsWith("set alarm") -> {
                val timeStr = trimmed.substringAfter("alarm").trim()
                val parts = timeStr.split(":")
                val h = parts.getOrNull(0)?.toIntOrNull() ?: -1
                val m = parts.getOrNull(1)?.toIntOrNull() ?: -1
                intentSkills?.setAlarm(h, m) ?: SkillResult("SET_ALARM", SkillStatus.UNAVAILABLE, "No Activity context", 0L)
            }
            lower.startsWith("search") || lower.startsWith("web search") -> {
                val query = if (lower.startsWith("search")) trimmed.substringAfter("search").trim() else trimmed.substringAfter("web search").trim()
                intentSkills?.webSearch(query) ?: SkillResult("WEB_SEARCH", SkillStatus.UNAVAILABLE, "No Activity context", 0L)
            }
            else -> {
                SkillResult("UNKNOWN", SkillStatus.INVALID_GOAL, "Unrecognized deterministic goal", 0L, "UNKNOWN_COMMAND")
            }
        }
    }
}
