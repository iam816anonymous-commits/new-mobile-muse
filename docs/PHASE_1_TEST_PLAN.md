# Phase 1 Test Plan & Execution Procedures

## Unit Test Coverage (`Phase1SafetyHarnessTest.kt`)

1. **`testSingleExecutionOwnershipAcquisitionAndRelease`**: Verifies acquisition lock and rejection of concurrent execution requests.
2. **`testExecutionOwnershipReleaseOnException`**: Verifies ownership release and `SafetyState.FAILED` update during unhandled exceptions.
3. **`testCentralizedCancellationManager`**: Verifies cancellation request, typed reason preservation, and exception throwing.
4. **`testMasterWatchdogTimeoutFiresAndCancelsExecution`**: Verifies 100ms watchdog timeout firing, job cancellation, and `SafetyState.WATCHDOG_TIMEOUT` state update.
5. **`testMasterWatchdogCancellationWhenJobCompletesNormally`**: Verifies normal job completion cancels watchdog monitoring before timeout.
6. **`testDoubleVolumeUpPanicGestureDetection`**: Verifies double Volume-Up press within 500ms triggers panic, cancels execution, and returns true.
7. **`testVolumeUpOutsideThresholdDoesNotTriggerPanic`**: Verifies Volume-Up press > 500ms apart does not trigger panic.
8. **`testUnrelatedKeysDoNotTriggerPanic`**: Verifies Volume-Down key events do not trigger panic.

## Manual Physical Device Verification Procedures

### Procedure A: Idle Panic Test
1. Install debug APK on Tecno Camon i (Android 8.1).
2. Enable `LocalAgentAccessibilityService` in Android Settings.
3. Rapidly double-press Volume-Up while agent is idle.
4. Verify device executes `GLOBAL_ACTION_HOME` and no app crash occurs.

### Procedure B: Active Execution Panic Test
1. Launch test execution in LocalAgent.
2. Rapidly double-press Volume-Up during execution.
3. Verify execution immediately cancels, ownership is released, and Home screen is shown.

### Procedure C: Watchdog Timeout Test
1. Launch 10-second test job.
2. Observe watchdog firing at 6000ms.
3. Verify job cancellation, ownership release, and `WATCHDOG_TIMEOUT` log entry.
