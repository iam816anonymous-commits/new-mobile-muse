# Phase 1 Safety Harness Architecture

## Overview

Phase 1 establishes the central safety harness for LocalAgent, making every future execution path interruptible, bounded, centrally cancellable, and recoverable to a known safe state.

## Core Safety Invariants

1. **Centralized Cancellation**:
   All cancellation requests flow through `CentralCancellationManager` using strongly typed `CancellationReason` enums.
2. **Single Active Execution Ownership**:
   `ExecutionController` locks and unlocks goal execution to guarantee `At most ONE goal execution active at a time`. Ownership is guaranteed to be released in `finally` blocks upon completion, exception, cancellation, timeout, or panic.
3. **Master Watchdog**:
   `MasterWatchdog` enforces the `ExecutionTimeoutPolicy` (default 6000ms), canceling active coroutine `Job`s and releasing ownership if a deadline is exceeded.
4. **Hardware Panic Button**:
   `LocalAgentAccessibilityService` filters physical key events (`flagRequestFilterKeyEvents`). Pressing Volume-Up twice within 500ms triggers immediate emergency stop, cancels active jobs, releases ownership, performs `GLOBAL_ACTION_HOME`, and resets safety state idempotently.
