package com.agent.android.execution

enum class ExecutionState {
    IDLE,
    RECEIVING,
    PLANNING,
    EXECUTING,
    VERIFYING,
    STOPPING,
    FAILED
}
