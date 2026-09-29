# Phase 1 Safety Verification Harness

## Purpose & Architecture

The `Phase1SafetyTestHarness` provides deterministic, sequential test execution for the Phase 1 safety components on-device or in emulator/VM environments where physical hardware gestures cannot be easily triggered.

```text
Phase1SafetyTestHarness
        │
        ├── testExecutionOwnership()             (IDLE → acquire → release → IDLE)
        ├── testConcurrentExecutionRejection()   (1st ACCEPTED, 2nd REJECTED)
        ├── testManualCancellation()             (Cancel active Job & release)
        ├── testWatchdogTimeout()                (Inject 250ms timeout; verify cancel & release)
        ├── testPanicLogicSimulation()           (Simulate USER_PANIC central path)
        └── testStateReset()                     (Verify new execution accepted after failure)
```

## Production vs Test Watchdog Timeout

* **Production Watchdog**: Uses `ExecutionTimeoutPolicy(masterTimeoutMs = 6000L)`.
* **Test Harness Watchdog**: Injects `ExecutionTimeoutPolicy(masterTimeoutMs = 250L)` so automated tests execute in milliseconds without altering production settings.

## Simulated Panic vs Physical Panic Gesture

* **`PANIC LOGIC SIMULATION`**: Exercises `CentralCancellationManager`, active job cancellation, state reset, and `SafetyStatus.PANIC` update.
* **Physical Panic Gesture**: Intercepts physical Volume-Up key events (`flagRequestFilterKeyEvents`) in `LocalAgentAccessibilityService.onKeyEvent()`. Requires physical device testing (`MANUAL DEVICE TEST REQUIRED` in UI).

## Test Isolation & Cleanup Rules

* Every test resets cancellation state, releases execution locks, and resets state machine before and after execution.
* Tests run strictly sequentially (`runAllTests()`).
