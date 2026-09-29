# Roadmap & Phase Evolution

## Phased Development Strategy

* **Phase 0 (Completed)**:
  * Android build foundation (`minSdk = 27`).
  * GitHub Actions CI workflow (`.github/workflows/android.yml`).
  * AccessibilityService observation skeleton.
  * Deterministic execution state machine (`ExecutionStateMachine`).
  * Safety and Action contracts (`ONE ACTION AT A TIME`).
  * Bounded logger & persistent learning contracts (`LearningStore`).
  * Jarvis-style UI shell & permission onboarding.

* **Phase 1 (Next Milestone)**:
  * Physical Volume-Up panic button integration with `PanicController`.
  * Concrete 6-second execution watchdog timer.
  * Single elementary action execution through `ActionController`.

* **Future Phases**:
  * Phase 2: Elementary UI observation & single-target matching.
  * Phase 3: Goal planning & deterministic action sequencing.
  * Phase 4: Persistent learning store integration & workflow execution.
