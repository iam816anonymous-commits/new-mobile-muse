# Observation Model

## Principles

The observation model is tailored for low-memory Android 8.1 devices (4 GB RAM):

1. **Lightweight Snapshots**:
   `ScreenSnapshot` captures basic screen metadata without holding bitmap images or full UI element hierarchies in memory.
2. **Observation-Only Accessibility Service**:
   `LocalAgentAccessibilityService` receives `AccessibilityEvent`s purely for status monitoring. It never triggers action execution.
3. **No Heavy Traversal**:
   Recursive DOM/UI element tree traversal, continuous OCR polling, screen recording, and full-screen screenshot buffering are strictly prohibited.
4. **Visible Interaction Indicator Model**:
   `InteractionIndicatorModel` defines a lightweight target/action indicator model that can be shown briefly over target elements during future visual action execution.
