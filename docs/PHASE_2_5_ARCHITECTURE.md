# LocalAgent Phase 2.5 Architecture

## Overview
Phase 2.5 establishes the final pre-Phase-3 platform capability contracts and execution boundaries. It expands platform controllers without introducing autonomous planning, LLMs, or unconstrained screen automation.

```
User / Test Runner / STT
          |
          v
   CommandRegistry (53 Registered Production Commands)
          |
          v
   GoalDispatcherImpl (Permission & Argument Validation)
          |
          v
   ExecutionController (Single Execution Lock, Watchdog, Cancellation)
          |
  +-------+--------------------+------------------------+
  |                            |                        |
  v                            v                        v
Device Controllers         Platform Controllers      Skills & Speech
- Flashlight               - Clipboard              - Calculator
- Haptics                  - NotificationListener   - Notes
- Volume                   - UsageStats             - IntentSkills
- Connectivity             - Camera                 - STT / TTS
- SystemControls           - Display & Geometry
                           - Network & Power
                           - DeviceStateSnapshot
```

## Architectural Directives
1. **Observation-Only Events**: Observation sources (Accessibility, NotificationListener, Clipboard, SensorManager) MUST NEVER directly trigger autonomous actions or loops.
2. **Foreground-Only Policy**: No background services or autonomous background loops.
3. **Explicit Consent & Least Privilege**: Screen capture, notification listener, and usage stats require explicit user consent via system settings.
