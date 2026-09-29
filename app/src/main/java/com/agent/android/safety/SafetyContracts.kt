package com.agent.android.safety

enum class SafetyStatus {
    SAFE,
    WARNING,
    EMERGENCY_STOP,
    CANCELED
}

data class SafetyState(
    val status: SafetyStatus = SafetyStatus.SAFE,
    val isEmergencyStopActive: Boolean = false,
    val activeReason: String? = null
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
