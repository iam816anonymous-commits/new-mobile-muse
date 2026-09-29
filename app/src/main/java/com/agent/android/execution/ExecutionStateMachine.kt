package com.agent.android.execution

class ExecutionStateMachine(initialState: ExecutionState = ExecutionState.IDLE) {

    var currentState: ExecutionState = initialState
        private set

    private val validTransitions: Map<ExecutionState, Set<ExecutionState>> = mapOf(
        ExecutionState.IDLE to setOf(ExecutionState.RECEIVING, ExecutionState.STOPPING, ExecutionState.FAILED),
        ExecutionState.RECEIVING to setOf(ExecutionState.PLANNING, ExecutionState.STOPPING, ExecutionState.FAILED),
        ExecutionState.PLANNING to setOf(ExecutionState.EXECUTING, ExecutionState.STOPPING, ExecutionState.FAILED),
        ExecutionState.EXECUTING to setOf(ExecutionState.VERIFYING, ExecutionState.STOPPING, ExecutionState.FAILED),
        ExecutionState.VERIFYING to setOf(ExecutionState.IDLE, ExecutionState.EXECUTING, ExecutionState.STOPPING, ExecutionState.FAILED),
        ExecutionState.STOPPING to setOf(ExecutionState.IDLE, ExecutionState.FAILED),
        ExecutionState.FAILED to setOf(ExecutionState.IDLE)
    )

    fun canTransitionTo(targetState: ExecutionState): Boolean {
        return validTransitions[currentState]?.contains(targetState) == true
    }

    fun transitionTo(targetState: ExecutionState): Boolean {
        if (canTransitionTo(targetState)) {
            currentState = targetState
            return true
        }
        return false
    }

    fun reset() {
        currentState = ExecutionState.IDLE
    }
}
