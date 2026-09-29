package com.agent.android.safety

enum class CancellationReason {
    NONE,
    USER_PANIC,
    WATCHDOG_TIMEOUT,
    USER_STOP,
    SYSTEM_SHUTDOWN,
    EXECUTION_REPLACED,
    INTERNAL_FAILURE
}

enum class SafetyStatus {
    SAFE_IDLE,
    EXECUTION_ACTIVE,
    STOPPING,
    PANIC,
    WATCHDOG_TIMEOUT,
    FAILED
}

data class SafetyState(
    val status: SafetyStatus = SafetyStatus.SAFE_IDLE,
    val isEmergencyStopActive: Boolean = false,
    val activeReason: CancellationReason = CancellationReason.NONE
)

interface PanicController {
    fun triggerPanic(reason: String)
    fun isPanicActive(): Boolean
    fun resetPanic()
}

interface Watchdog {
    fun startMonitoring(actionId: String, timeoutMs: Long)
    fun resetTimer()
    fun stopMonitoring()
}

interface ExecutionCancellation {
    fun requestCancellation(reason: String)
    fun isCancellationRequested(): Boolean
}

class CentralCancellationManager : ExecutionCancellation {

    @Volatile
    private var isCancellationRequested: Boolean = false

    @Volatile
    private var activeReason: CancellationReason = CancellationReason.NONE

    override fun requestCancellation(reason: String) {
        requestCancellation(CancellationReason.INTERNAL_FAILURE)
    }

    @Synchronized
    fun requestCancellation(reason: CancellationReason) {
        if (!isCancellationRequested) {
            isCancellationRequested = true
            activeReason = reason
        }
    }

    override fun isCancellationRequested(): Boolean = isCancellationRequested

    fun getCancellationReason(): CancellationReason = activeReason

    fun throwIfCancellationRequested() {
        if (isCancellationRequested) {
            throw CancellationException("Execution cancelled. Reason: $activeReason")
        }
    }

    @Synchronized
    fun reset() {
        isCancellationRequested = false
        activeReason = CancellationReason.NONE
    }

    class CancellationException(message: String) : RuntimeException(message)
}
