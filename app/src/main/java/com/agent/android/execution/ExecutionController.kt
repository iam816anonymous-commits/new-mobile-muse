package com.agent.android.execution

import com.agent.android.safety.CancellationReason
import com.agent.android.safety.CentralCancellationManager
import com.agent.android.safety.SafetyState
import com.agent.android.safety.SafetyStatus
import com.agent.android.storage.Logger
import kotlinx.coroutines.Job
import java.util.concurrent.atomic.AtomicReference

class ExecutionController(
    val stateMachine: ExecutionStateMachine = ExecutionStateMachine(),
    val cancellationManager: CentralCancellationManager = CentralCancellationManager(),
    private val logger: Logger = Logger()
) {

    @Volatile
    private var isExecutionActive: Boolean = false

    private val activeJobRef = AtomicReference<Job?>(null)

    @Volatile
    var safetyState: SafetyState = SafetyState(SafetyStatus.SAFE_IDLE)
        private set

    @Synchronized
    fun acquireExecution(job: Job? = null): Boolean {
        if (isExecutionActive) {
            logger.w("ExecutionController", "Acquisition rejected: Execution is already active.")
            return false
        }
        isExecutionActive = true
        activeJobRef.set(job)
        cancellationManager.reset()
        stateMachine.transitionTo(ExecutionState.RECEIVING)
        safetyState = SafetyState(SafetyStatus.EXECUTION_ACTIVE)
        logger.i("ExecutionController", "Execution acquired successfully.")
        return true
    }

    fun setActiveJob(job: Job) {
        activeJobRef.set(job)
    }

    fun getActiveJob(): Job? = activeJobRef.get()

    fun isExecuting(): Boolean = isExecutionActive

    @Synchronized
    fun releaseExecution(reason: CancellationReason = CancellationReason.NONE) {
        try {
            val job = activeJobRef.getAndSet(null)
            job?.cancel()

            if (stateMachine.currentState != ExecutionState.IDLE) {
                if (stateMachine.currentState != ExecutionState.STOPPING && stateMachine.currentState != ExecutionState.FAILED) {
                    stateMachine.transitionTo(ExecutionState.STOPPING)
                }
                stateMachine.transitionTo(ExecutionState.IDLE)
            }

            safetyState = when (reason) {
                CancellationReason.USER_PANIC -> SafetyState(SafetyStatus.PANIC, true, reason)
                CancellationReason.WATCHDOG_TIMEOUT -> SafetyState(SafetyStatus.WATCHDOG_TIMEOUT, false, reason)
                CancellationReason.INTERNAL_FAILURE -> SafetyState(SafetyStatus.FAILED, false, reason)
                else -> SafetyState(SafetyStatus.SAFE_IDLE, false, CancellationReason.NONE)
            }

            logger.i("ExecutionController", "Execution released safely. Reason: $reason")
        } finally {
            isExecutionActive = false
        }
    }

    fun resetToSafeState(reason: CancellationReason = CancellationReason.NONE) {
        cancellationManager.requestCancellation(reason)
        releaseExecution(reason)
    }
}
