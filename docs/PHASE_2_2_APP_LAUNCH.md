# Phase 2.2 Safe App Launch Architecture

## Overview

Phase 2.2 provides safe, deterministic application discovery, validation, and one-shot launch capabilities for LocalAgent on Android 8.1 / API 27 (Tecno Camon i) without accessibility UI automation or post-launch interaction.

```text
User Command: "open Settings"
              │
              ▼
     GoalDispatcherImpl ◄────── Open Command Parsing ("open <app>")
              │
              ▼
    ExecutionController ◄────── Try / Finally Single Execution Ownership Lock
              │
              ▼
      AppLauncherImpl   ◄────── PackageManager Query & App Resolution
              │
              ├── 1. Exact Package Name Match?
              ├── 2. Exact Application Label Match?
              ├── 3. Case-Insensitive / Substring Label Match?
              └── Ambiguous Matches (>1)? ──► Return AMBIGUOUS_APP
              │
              ▼
   getLaunchIntentForPackage() + startActivity(launchIntent)
              │
              ▼
    AppLaunchResult (SUCCESS) ──► Agent STOPS (NO Post-Launch Automation)
```

## Architectural Safeguards & One-Shot Rule

1. **One-Shot Launch Execution**:
   LocalAgent resolves the application, constructs a valid launch intent, and dispatches it via `startActivity()`. Immediately after dispatching the intent, LocalAgent releases the `ExecutionController` lock and returns to `IDLE`.
2. **Zero Post-Launch Automation**:
   LocalAgent performs NO screen crawling, coordinate tapping, gesture injection, OCR, camera shutter searching, or app UI interaction after launch.
3. **Deterministic Resolution & Ambiguity Handling**:
   If multiple installed applications match a requested user label (e.g. two apps matching "Camera"), `AppLauncherImpl` returns `AMBIGUOUS_APP` with details instead of guessing.
4. **Safety Integration**:
   Every app launch passes through `ExecutionController` lock, `CentralCancellationManager`, and `MasterWatchdog`.
