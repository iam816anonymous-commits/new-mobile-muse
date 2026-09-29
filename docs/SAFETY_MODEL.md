# Safety Model

## Architectural Invariants

Every action in LocalAgent MUST route through central safety contracts:

```text
ALL FUTURE ACTION EXECUTION
        ↓
CENTRAL EXECUTION BOUNDARY
        ↓
CENTRAL CANCELLATION / SAFETY BOUNDARY
```

## Safety Contracts

1. **`PanicController`**: High-priority emergency stop interface.
2. **`Watchdog`**: Execution monitoring interface for future action timeouts.
3. **`ExecutionCancellation`**: Standardized cancellation request boundary.
4. **`SafetyState`**: State representation (`SAFE`, `WARNING`, `EMERGENCY_STOP`, `CANCELED`).

## Phase 0 Rules

* Physical Volume-Up panic button implementation is deferred to Phase 1.
* No skill or future module may create its own independent, unmonitored cancellation mechanism.
