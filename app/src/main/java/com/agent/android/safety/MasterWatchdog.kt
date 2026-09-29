package com.agent.android.safety

import com.agent.android.execution.ExecutionController
import com.agent.android.storage.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ExecutionTimeoutPolicy(
    val masterTimeoutMs: Long = 6000L
)

class MasterWatchdog(
    private val executionController: ExecutionController,
    private val policy: ExecutionTimeoutPolicy = ExecutionTimeoutPolicy(),
    private val logger: Logger = Logger(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : Watchdog {

    @Volatile
    private var timerJob: Job? = null

    override fun startMonitoring(actionId: String, timeoutMs: Long) {
        val effectiveTimeout = if (timeoutMs > 0) timeoutMs else policy.masterTimeoutMs
        stopMonitoring()

        logger.i("Watchdog", "Starting watchdog monitoring for $actionId (Budget: ${effectiveTimeout}ms)")

        timerJob = scope.launch {
            delay(effectiveTimeout)
            triggerTimeout(actionId)
        }
    }

    override fun resetTimer() {
        stopMonitoring()
    }

    override fun stopMonitoring() {
        timerJob?.cancel()
        timerJob = null
    }

    fun triggerTimeout(actionId: String) {
        logger.e("Watchdog", "Watchdog timeout fired for $actionId. Requesting cancellation.")
        executionController.cancellationManager.requestCancellation(CancellationReason.WATCHDOG_TIMEOUT)
        executionController.getActiveJob()?.cancel()
        executionController.releaseExecution(CancellationReason.WATCHDOG_TIMEOUT)
    }
}
