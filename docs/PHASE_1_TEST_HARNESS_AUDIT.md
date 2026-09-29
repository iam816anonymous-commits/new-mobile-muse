# Phase 1 Test Harness Initial Audit

**Date**: September 2026
**Target Hardware**: Tecno Camon i (Android 8.1 / API 27, MediaTek Helio P23, 4 GB RAM)

## Existing Safety Components Audit

| Component | Class Name / Location | Description & Architecture Role |
| :--- | :--- | :--- |
| **Central Cancellation Manager** | `CentralCancellationManager` (`safety/SafetyContracts.kt`) | Central authority tracking `CancellationReason` (`USER_PANIC`, `WATCHDOG_TIMEOUT`, `USER_STOP`, `SYSTEM_SHUTDOWN`, `EXECUTION_REPLACED`, `INTERNAL_FAILURE`). |
| **Execution Ownership Controller** | `ExecutionController` (`execution/ExecutionController.kt`) | Enforces `At most ONE goal execution active at a time`. Locks active jobs and safely releases ownership in `finally` blocks. |
| **Master Watchdog** | `MasterWatchdog` (`safety/MasterWatchdog.kt`) | Monitors executions against `ExecutionTimeoutPolicy` (default 6000ms). Fires timeout cancellation and releases ownership. |
| **Accessibility Service & Key Event Filter** | `LocalAgentAccessibilityService` (`service/LocalAgentAccessibilityService.kt`) | Intercepts physical Volume-Up key events (`flagRequestFilterKeyEvents`). Double press within 500ms triggers `USER_PANIC` emergency stop, cancels active jobs, releases ownership, and dispatches `GLOBAL_ACTION_HOME`. |
| **Control Plane UI** | `MainActivity` (`MainActivity.kt` & `activity_main.xml`) | Jarvis-style control plane dashboard displaying status, execution state, accessibility configuration button, and event log area. |

## Test Harness Connection Strategy

The new `Phase1SafetyTestHarness` will directly connect to these production components:
1. `ExecutionController`: Tests single ownership acquisition, rejection of concurrent requests, and release upon success or exception.
2. `CentralCancellationManager`: Tests manual cancellation requests and propagation.
3. `MasterWatchdog`: Tests watchdog timeouts using a configurable 250ms test timeout without altering the production 6000ms default budget.
4. `LocalAgentAccessibilityService`: Simulates the central `USER_PANIC` cancellation and emergency stop path (`PANIC LOGIC SIMULATION`).

## Emulator / VM Testing Limitations

* Physical Volume-Up key double-press hardware gestures cannot be reliably reproduced in standard Android emulators or headless CI environments.
* The test harness provides a deterministic `PANIC LOGIC SIMULATION` that exercises the exact `CentralCancellationManager` and `ExecutionController` panic cancellation code path.
* Hardware Volume-Up double-press testing is labeled `MANUAL DEVICE TEST REQUIRED` in the UI to clearly distinguish simulated panic tests from physical device button verification.
