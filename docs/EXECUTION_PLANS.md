# Future Composite Execution Plan Architecture

## 1. Overview
This architectural contract prepares LocalAgent to represent multi-step composite goals (e.g. "Turn on flashlight for 1 minute") without creating duplicate combination commands or modifying the atomic command registry.

## 2. Core Abstractions
- **COMMAND (`CommandDefinition`)**: Atomic single capability registered in `CommandRegistry` (e.g., `flashlight.on`, `flashlight.off`, `volume.music.set`).
- **EXECUTION STEP (`ExecutionStep`)**: Single step referencing a `commandId`, structured arguments, timeout, wait duration, or `isWaitPrimitive` flag.
- **EXECUTION PLAN (`ExecutionPlan`)**: Ordered list of `ExecutionStep` instances with a plan ID, goal description, failure policy, and plan status.
- **WAIT PRIMITIVE**: First-class cancellable wait primitive (`isWaitPrimitive = true`, `waitDurationMs`) executed without Thread.sleep on the UI thread.
- **FAILURE POLICY (`FailurePolicy`)**: `STOP_ON_FAILURE`, `CONTINUE_ON_FAILURE`, `RETRY`, `ABORT`.

## 3. Example Composition
Goal: "Turn on the flashlight for 1 minute"
```json
{
  "planId": "plan-flash-60s",
  "goalDescription": "Turn on flashlight for 1 minute",
  "failurePolicy": "STOP_ON_FAILURE",
  "steps": [
    {
      "stepId": "s1",
      "commandId": "flashlight.on",
      "arguments": {}
    },
    {
      "stepId": "s2",
      "commandId": "wait",
      "isWaitPrimitive": true,
      "waitDurationMs": 60000,
      "arguments": { "durationMs": "60000" }
    },
    {
      "stepId": "s3",
      "commandId": "flashlight.off",
      "arguments": {}
    }
  ]
}
```

## 4. Execution Boundary
All step executions route through the existing `GoalDispatcher` and `ExecutionController` safety boundary.
