# System Architecture

## Overview

LocalAgent is a stability-first autonomous Android agent designed explicitly for low-resource hardware:
* **Target Hardware**: Tecno Camon i (MediaTek Helio P23, 4 GB RAM, Android 8.1 / API 27).
* **Architecture Strategy**: Phased rebuild with strict contract isolation.

## Component Layout

```text
LocalAgent/
├── service/
│   └── LocalAgentAccessibilityService  (OBSERVATION ONLY)
├── execution/
│   ├── GoalDispatcher                 (Central Goal Entry Boundary)
│   ├── ExecutionState                 (IDLE, RECEIVING, PLANNING, EXECUTING, VERIFYING, STOPPING, FAILED)
│   └── ExecutionStateMachine          (Deterministic State Transitions)
├── safety/
│   ├── SafetyContracts                (PanicController, Watchdog, ExecutionCancellation, SafetyState)
├── actions/
│   └── ActionContracts                (Action, ActionResult, ActionController [ONE ACTION AT A TIME])
├── observation/
│   └── ObservationContracts           (ScreenObserver, ScreenSnapshot, InteractiveElement, InteractionIndicatorModel)
├── storage/
│   ├── LearningStore                  (SAF Shared Storage Abstraction)
│   └── Logger                         (Bounded Ring-Buffer Logger)
└── learning/
    └── LearningSchemas                (Versioned JSON Contracts)
```

## Phase 0 Architectural Boundaries

1. **Accessibility Observation Isolation**:
   `onAccessibilityEvent()` is strictly observation-only. It NEVER dispatches goals, executes actions, launches applications, or triggers planning.
2. **Execution Boundary**:
   All goal executions MUST enter through `GoalDispatcher`. Direct triggers from background listeners or events are forbidden.
3. **Action Execution Boundary**:
   `ActionController` strictly enforces `ONE ACTION AT A TIME`.
