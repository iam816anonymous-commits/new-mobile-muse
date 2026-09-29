package com.agent.android.safety

import com.agent.android.execution.ExecutionController
import com.agent.android.execution.ExecutionState
import com.agent.android.storage.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

data class SafetyTestResult(
    val testName: String,
    val passed: Boolean,
    val expectedResult: String,
    val actualResult: String,
    val durationMs: Long,
    val errorMessage: String? = null
)

data class HarnessSuiteSummary(
    val passedCount: Int,
    val failedCount: Int,
    val totalCount: Int,
    val overallPassed: Boolean,
    val results: List<SafetyTestResult>
)

class Phase1SafetyTestHarness(
    private val executionController: ExecutionController = ExecutionController(),
    private val logger: Logger = Logger(),
    private val testScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {

    private fun resetTestEnvironment() {
        executionController.cancellationManager.reset()
        if (executionController.isExecuting()) {
            executionController.releaseExecution(CancellationReason.NONE)
        }
        executionController.stateMachine.reset()
    }

    fun testExecutionOwnership(): SafetyTestResult {
        val start = System.currentTimeMillis()
        resetTestEnvironment()
        return try {
            val acquired = executionController.acquireExecution()
            if (!acquired) {
                return SafetyTestResult(
                    "Execution Ownership", false,
                    "Acquire lock successfully", "Lock acquisition rejected",
                    System.currentTimeMillis() - start, "Failed to acquire execution lock"
                )
            }
            val isExecuting = executionController.isExecuting()
            executionController.releaseExecution()
            val stateAfterRelease = executionController.stateMachine.currentState
            val isExecutingAfterRelease = executionController.isExecuting()

            val passed = isExecuting && !isExecutingAfterRelease && stateAfterRelease == ExecutionState.IDLE
            SafetyTestResult(
                testName = "Execution Ownership",
                passed = passed,
                expectedResult = "Acquired=true, Executing=true -> Released=true, State=IDLE",
                actualResult = "Acquired=$acquired, Executing=$isExecuting -> Released=${!isExecutingAfterRelease}, State=$stateAfterRelease",
                durationMs = System.currentTimeMillis() - start
            )
        } catch (e: Exception) {
            SafetyTestResult(
                "Execution Ownership", false, "Successful execution ownership cycle",
                "Exception thrown: ${e.message}", System.currentTimeMillis() - start, e.message
            )
        } finally {
            resetTestEnvironment()
        }
    }

    fun testConcurrentExecutionRejection(): SafetyTestResult {
        val start = System.currentTimeMillis()
        resetTestEnvironment()
        return try {
            val firstAcquired = executionController.acquireExecution()
            val secondAcquired = executionController.acquireExecution()

            val passed = firstAcquired && !secondAcquired && executionController.isExecuting()
            SafetyTestResult(
                testName = "Concurrent Execution Rejection",
                passed = passed,
                expectedResult = "First=ACCEPTED, Second=REJECTED, Active=1",
                actualResult = "First=$firstAcquired, Second=$secondAcquired, IsExecuting=${executionController.isExecuting()}",
                durationMs = System.currentTimeMillis() - start
            )
        } catch (e: Exception) {
            SafetyTestResult(
                "Concurrent Execution Rejection", false, "1st ACCEPTED, 2nd REJECTED",
                "Exception thrown: ${e.message}", System.currentTimeMillis() - start, e.message
            )
        } finally {
            resetTestEnvironment()
        }
    }

    fun testManualCancellation(): SafetyTestResult {
        val start = System.currentTimeMillis()
        resetTestEnvironment()
        return try {
            val job = Job()
            executionController.acquireExecution(job)

            executionController.cancellationManager.requestCancellation(CancellationReason.USER_STOP)
            executionController.getActiveJob()?.cancel()
            executionController.releaseExecution(CancellationReason.USER_STOP)

            val isCancelled = executionController.cancellationManager.isCancellationRequested()
            val isJobCancelled = job.isCancelled
            val isExecuting = executionController.isExecuting()

            val passed = isCancelled && isJobCancelled && !isExecuting
            SafetyTestResult(
                testName = "Manual Cancellation",
                passed = passed,
                expectedResult = "CancellationRequested=true, JobCancelled=true, Executing=false",
                actualResult = "CancellationRequested=$isCancelled, JobCancelled=$isJobCancelled, Executing=$isExecuting",
                durationMs = System.currentTimeMillis() - start
            )
        } catch (e: Exception) {
            SafetyTestResult(
                "Manual Cancellation", false, "Job cancelled & ownership released",
                "Exception thrown: ${e.message}", System.currentTimeMillis() - start, e.message
            )
        } finally {
            resetTestEnvironment()
        }
    }

    fun testWatchdogTimeout(): SafetyTestResult {
        val start = System.currentTimeMillis()
        resetTestEnvironment()
        return try {
            val job = Job()
            executionController.acquireExecution(job)

            val testPolicy = ExecutionTimeoutPolicy(masterTimeoutMs = 250L)
            val watchdog = MasterWatchdog(executionController, testPolicy, logger, testScope)

            watchdog.startMonitoring("action-test-harness", 250L)
            runBlocking { delay(350L) }

            val cancellationRequested = executionController.cancellationManager.isCancellationRequested()
            val cancellationReason = executionController.cancellationManager.getCancellationReason()
            val isExecuting = executionController.isExecuting()

            val passed = cancellationRequested && cancellationReason == CancellationReason.WATCHDOG_TIMEOUT && !isExecuting
            SafetyTestResult(
                testName = "Watchdog Timeout",
                passed = passed,
                expectedResult = "Cancelled=true, Reason=WATCHDOG_TIMEOUT, Executing=false",
                actualResult = "Cancelled=$cancellationRequested, Reason=$cancellationReason, Executing=$isExecuting",
                durationMs = System.currentTimeMillis() - start
            )
        } catch (e: Exception) {
            SafetyTestResult(
                "Watchdog Timeout", false, "Watchdog fires & releases execution",
                "Exception thrown: ${e.message}", System.currentTimeMillis() - start, e.message
            )
        } finally {
            resetTestEnvironment()
        }
    }

    fun testPanicLogicSimulation(): SafetyTestResult {
        val start = System.currentTimeMillis()
        resetTestEnvironment()
        return try {
            val job = Job()
            executionController.acquireExecution(job)

            // Simulate central panic cancellation path
            executionController.cancellationManager.requestCancellation(CancellationReason.USER_PANIC)
            executionController.getActiveJob()?.cancel()
            executionController.releaseExecution(CancellationReason.USER_PANIC)

            val isCancelled = executionController.cancellationManager.isCancellationRequested()
            val reason = executionController.cancellationManager.getCancellationReason()
            val isExecuting = executionController.isExecuting()
            val status = executionController.safetyState.status

            val passed = isCancelled && reason == CancellationReason.USER_PANIC && !isExecuting && status == SafetyStatus.PANIC
            SafetyTestResult(
                testName = "Panic Logic Simulation",
                passed = passed,
                expectedResult = "Reason=USER_PANIC, Status=PANIC, Executing=false",
                actualResult = "Reason=$reason, Status=$status, Executing=$isExecuting",
                durationMs = System.currentTimeMillis() - start
            )
        } catch (e: Exception) {
            SafetyTestResult(
                "Panic Logic Simulation", false, "Panic path resets state cleanly",
                "Exception thrown: ${e.message}", System.currentTimeMillis() - start, e.message
            )
        } finally {
            resetTestEnvironment()
        }
    }

    fun testStateReset(): SafetyTestResult {
        val start = System.currentTimeMillis()
        resetTestEnvironment()
        return try {
            // First run ending in failure
            executionController.acquireExecution()
            executionController.releaseExecution(CancellationReason.INTERNAL_FAILURE)

            // Attempt new execution after failure
            val newAcquired = executionController.acquireExecution()
            val isExecutingNew = executionController.isExecuting()
            executionController.releaseExecution()

            val passed = newAcquired && isExecutingNew && !executionController.isExecuting()
            SafetyTestResult(
                testName = "State Reset",
                passed = passed,
                expectedResult = "New execution accepted after failure",
                actualResult = "NewAcquired=$newAcquired, ExecutingNew=$isExecutingNew",
                durationMs = System.currentTimeMillis() - start
            )
        } catch (e: Exception) {
            SafetyTestResult(
                "State Reset", false, "System recovers & accepts new execution",
                "Exception thrown: ${e.message}", System.currentTimeMillis() - start, e.message
            )
        } finally {
            resetTestEnvironment()
        }
    }

    fun runAllTests(): HarnessSuiteSummary {
        val results = mutableListOf<SafetyTestResult>()
        results.add(testExecutionOwnership())
        results.add(testConcurrentExecutionRejection())
        results.add(testManualCancellation())
        results.add(testWatchdogTimeout())
        results.add(testPanicLogicSimulation())
        results.add(testStateReset())

        val passedCount = results.count { it.passed }
        val failedCount = results.count { !it.passed }
        val totalCount = results.size

        return HarnessSuiteSummary(
            passedCount = passedCount,
            failedCount = failedCount,
            totalCount = totalCount,
            overallPassed = failedCount == 0,
            results = results
        )
    }
}
