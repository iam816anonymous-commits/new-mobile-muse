# LocalAgent — GitHub APK Update Workflow

This document details the GitHub APK release and update workflow for LocalAgent.

## Core Workflow Design

LocalAgent testing uses direct APK download and in-place updating on physical Android devices.

```
Code Change → CI Build & Gate Checks → GitHub Release APK → Direct Phone Download → In-Place Update
```

No ADB, USB debugging, or Android Studio device deployment is required or assumed.

---

## In-Place Update Compatibility Contract

For Android to perform an **in-place update** (installing a new APK directly over an existing installation without uninstalling or losing application data), all of the following conditions must be met:

1. **Package Identity (`applicationId`)**:
   - `applicationId`: `com.agent.android`
   - Must remain identical across all builds.

2. **Signing Identity (`signingConfig`)**:
   - Both debug and release APKs are signed with the project's shared keystore (`app/debug.keystore`).
   - Every GitHub Release APK and local build shares the same cryptographic signature.

3. **Version Progression (`versionCode` & `versionName`)**:
   - `versionCode` strictly increments with every release milestone.
   - `versionName` reflects the current Phase milestone:
     - `0.1` (versionCode 1) — Phase 2 Complete
     - `0.2` (versionCode 2) — Phase 3.1 Observation Foundation
     - `0.3` (versionCode 3) — Phase 3.2 Action Execution Foundation

4. **Android Manifest Component Stability**:
   - Service component names (`com.agent.android.service.LocalAgentAccessibilityService`, `com.agent.android.service.LocalAgentNotificationListenerService`) remain constant across updates.

---

## State & Data Preservation During Update

When an APK is updated in place, Android retains all persistent application state under `/data/data/com.agent.android/`:

- **SQLite Databases**:
  - `TestResultStore` (foundation test execution history)
  - `LearningStore` (schema and rule persistence)
- **SharedPreferences**:
  - App settings, onboarding state, and toggle preferences
- **App Storage**:
  - Notes (`NotesSkill` storage)
  - Observation evidence snapshots and logs

---

## Permission & Special Access Survival Matrix

| Permission / Access Type | Survives Update? | Behavior on Android 8.1+ (API 27+) |
| :--- | :---: | :--- |
| **Runtime Permissions** (`CAMERA`, `RECORD_AUDIO`, `WRITE_EXTERNAL_STORAGE`, `ACCESS_FINE_LOCATION`) | **YES** | Granted permissions persist automatically across in-place updates. |
| **Accessibility Service** (`LocalAgentAccessibilityService`) | **YES** | Service binding is preserved as long as package name and service component declaration remain intact. |
| **Notification Listener** (`LocalAgentNotificationListenerService`) | **YES** | Notification access remains granted across updates when package identity and signature match. |
| **Usage Stats Access** (`PACKAGE_USAGE_STATS`) | **YES** | Special access granted in Settings -> Usage Access survives updates. |
| **Write Settings Access** (`WRITE_SETTINGS`) | **YES** | System write settings permission persists. |
| **Do Not Disturb / Notification Policy Access** | **YES** | Notification policy access persists. |
| **Device Admin** (`LocalAgentAdminReceiver`) | **YES** | User-activated Device Admin status persists. |

*Note:* If an OS security policy or manufacturer-specific ROM resets a special access toggle, `PermissionManager` in LocalAgent will dynamically evaluate the status and prompt with a guided Settings fallback.

---

## GitHub Release Steps for Developers

1. **Verify Release Gates**:
   - Run `./gradlew testDebugUnitTest`
   - Run `./gradlew lintDebug`
   - Run `./gradlew assembleRelease`
2. **Commit Code and Push**:
   - Push commits or release tag to GitHub repository.
3. **Download & Update**:
   - Download `app-release.apk` (or `app-debug.apk`) directly on the Android device.
   - Tap the downloaded APK to install.
   - Android will prompt to update the existing application in place.
