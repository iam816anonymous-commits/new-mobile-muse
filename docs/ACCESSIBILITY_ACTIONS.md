# LocalAgent Accessibility Actions Foundation

## Overview
`AccessibilityActionController` defines future Phase 3 gesture and navigation action contracts (`tap`, `swipe`, `long_press`, `scroll`, `back`, `home`, `recents`).

## Architectural Boundary
- Actions are contract definitions only in Phase 2.5.
- `LocalAgentAccessibilityService.onAccessibilityEvent()` remains strictly observation-only.
- All future action execution must route through `GoalDispatcher` -> `ExecutionController` -> `Safety`.
