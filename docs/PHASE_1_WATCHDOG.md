# Master Watchdog Architecture

## Purpose

The Master Watchdog guarantees that no execution can run indefinitely or freeze the agent.

## Timeout Policy & Default Budget

* Default Master Timeout Budget: `6000ms` (`ExecutionTimeoutPolicy`).
* Configurable per action, allowing future phases to adjust action budgets without refactoring watchdog mechanics.

## Cancellation & Job Propagation

When the watchdog timer fires:
1. `CentralCancellationManager.requestCancellation(CancellationReason.WATCHDOG_TIMEOUT)` is called.
2. The active coroutine `Job` associated with the execution is directly cancelled.
3. `ExecutionController.releaseExecution(CancellationReason.WATCHDOG_TIMEOUT)` safely transitions state to `IDLE` and updates `SafetyState` to `WATCHDOG_TIMEOUT`.
4. Execution ownership is released.
