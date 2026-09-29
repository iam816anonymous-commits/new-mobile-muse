# Phase 1 Initial Repository Audit

**Date**: September 2026
**Target Device**: Tecno Camon i (Android 8.1 / API 27, MediaTek Helio P23, 4 GB RAM)

## Phase 0 Inspection & Audit Summary

| Component / Subsystem | Phase 0 Status | Phase 1 Assessment & Needed Changes |
| :--- | :--- | :--- |
| **`SafetyContracts`** | Basic interfaces defined (`PanicController`, `Watchdog`, `ExecutionCancellation`, `SafetyState`). | Lacks typed cancellation reasons (`CancellationReason` enum). Needs concrete cancellation authority and integration with execution jobs. |
| **Execution Ownership** | `ActionController` enforces one-action-at-a-time via `executeAction`. | Lacks high-level single execution ownership (`ExecutionController`) to lock/release goal execution across asynchronous coroutine scopes. |
| **Watchdog** | Contract defined in `SafetyContracts.kt`. | Needs concrete implementation (`MasterWatchdog`) with configurable `ExecutionTimeoutPolicy` (default 6000ms), active job cancellation, and safe state reset. |
| **Panic Button** | Skeleton defined in `SafetyContracts.kt`. | Accessibility service configuration lacks `flagRequestFilterKeyEvents`. Needs `onKeyEvent()` implementation with 500ms double Volume-Up press detection, idempotent cancellation, and `GLOBAL_ACTION_HOME` dispatch. |
| **Execution State Machine** | `ExecutionStateMachine.kt` (`IDLE`, `RECEIVING`, `PLANNING`, `EXECUTING`, `VERIFYING`, `STOPPING`, `FAILED`). | Intact and working. Needs integration with watchdog and panic safety reset paths. |
| **`LocalAgentAccessibilityService`** | Observation-only skeleton in `service/`. | Intact. Key event filtering (`flagRequestFilterKeyEvents` and `onKeyEvent()`) needs to be added without breaking the observation-only rule of `onAccessibilityEvent()`. |
| **Logger** | Bounded ring-buffer logger (`Logger.kt`, max capacity 100). | Intact. Will log safety, cancellation, watchdog, and panic events. |
| **Unit Tests** | 6 tests passing in `Phase0UnitTest.kt`. | Intact. New unit tests will be added for ownership, cancellation, watchdog, and double-press panic logic. |

## Required Phase 1 Implementation Plan

1. Define `CancellationReason` enum (`USER_PANIC`, `WATCHDOG_TIMEOUT`, `USER_STOP`, `SYSTEM_SHUTDOWN`, `EXECUTION_REPLACED`, `INTERNAL_FAILURE`).
2. Implement `CentralCancellationManager` as the single authoritative cancellation boundary.
3. Implement `ExecutionController` to enforce single active goal execution with try/finally release guarantees.
4. Implement `ExecutionTimeoutPolicy` and `MasterWatchdog` with 6000ms budget and coroutine job cancellation.
5. Update `accessibility_service_config.xml` with `flagRequestFilterKeyEvents` and implement double Volume-Up press (within 500ms) detection in `LocalAgentAccessibilityService.onKeyEvent()`.
6. Integrate panic double-press to trigger `USER_PANIC` cancellation, cancel active jobs, release ownership, reset state, and perform `GLOBAL_ACTION_HOME`.
7. Add comprehensive Phase 1 unit tests and documentation.
