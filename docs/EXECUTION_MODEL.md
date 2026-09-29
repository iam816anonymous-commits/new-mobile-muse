# Execution Model

## Deterministic Execution State Machine

Execution flow is governed by `ExecutionStateMachine` with explicit state transitions:

```text
IDLE → RECEIVING → PLANNING → EXECUTING → VERIFYING → IDLE
  └───────┴───────────┴───────────┴───────────┴────────→ STOPPING / FAILED → IDLE
```

### Valid Transition Table

* **IDLE**: May transition to `RECEIVING`, `STOPPING`, `FAILED`.
* **RECEIVING**: May transition to `PLANNING`, `STOPPING`, `FAILED`.
* **PLANNING**: May transition to `EXECUTING`, `STOPPING`, `FAILED`.
* **EXECUTING**: May transition to `VERIFYING`, `STOPPING`, `FAILED`.
* **VERIFYING**: May transition to `IDLE`, `EXECUTING`, `STOPPING`, `FAILED`.
* **STOPPING**: May transition to `IDLE`, `FAILED`.
* **FAILED**: May transition to `IDLE`.

Invalid state transitions are rejected with a boolean `false` return.

## Goal Dispatcher Boundary

All execution requests must enter through `GoalDispatcher`. No automatic, event-driven loop triggers are allowed.
