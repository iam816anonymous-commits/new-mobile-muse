package com.agent.android.execution

/**
 * Central Execution Boundary Contract.
 *
 * All future goal execution requests (from UI, voice, broadcast, etc.) MUST pass
 * through GoalDispatcher.
 *
 * Direct execution from AccessibilityEvent or background triggers is strictly forbidden.
 */
interface GoalDispatcher {
    /**
     * Dispatches a goal to the agent execution pipeline.
     *
     * @param goal The textual or structured goal to execute.
     * @return True if the goal was accepted into RECEIVING/PLANNING state, false otherwise.
     */
    fun dispatchGoal(goal: String): Boolean

    /**
     * Cancels any currently active goal dispatch and halts execution safely.
     */
    fun cancelCurrentGoal()
}
