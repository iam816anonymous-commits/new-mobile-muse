# LocalAgent

An on-device, safety-first Android agent foundation targeting Android 8.1+ (API 27) low-end hardware (Tecno Camon i / Helio P23, 4GB RAM).

---

## 1. Project Overview
`LocalAgent` is currently in **Phase 2.5 (Foundation Completion)**. It provides a deterministic, local-execution runtime on Android that handles goal dispatching, device state observation, system control skills, platform STT/TTS speech abstractions, structured permissions management, and interactive sequential test runner verification.

**CRITICAL DISTINCTION:**
- **CURRENT IMPLEMENTATION (Phases 0 - 2.5):** On-device deterministic skill execution, centralized command registry, safety boundary execution locking, MasterWatchdog timeout enforcement, double Volume-Up hardware panic button, STT/TTS platform wrappers, structured capability/permission detectors, and sequential interactive test runner harness.
- **FUTURE AUTONOMOUS AGENT (Phase 3+):** On-device LLM integration, autonomous planning, UI screen parsing/traversal, DOM crawling, and multi-step autonomous loops. *Zero autonomous AI behavior or cloud APIs exist in the current codebase.*

---

## 2. Project Goals
1. Establish a single-action-at-a-time execution boundary with deterministic safety guarantees.
2. Provide direct hardware and system controls without relying on cloud services or external dependencies.
3. Support resource-constrained hardware (Android 8.1 API 27, 4GB RAM, MediaTek Helio P23).
4. Maintain a 100% test coverage contract mapping every registered command to data-driven test cases in `FoundationTestRegistry`.

---

## 3. Current Architecture

```text
User Input / Interactive Test Runner / Live Command Console
                         │
                         ▼
                CommandRegistry (53 Commands)
                         │
                         ▼
              GoalDispatcherImpl & CommandArguments
                         │
                         ▼
           ExecutionController (Single Execution Lock)
                         │
        ┌────────────────┴────────────────┐
        ▼                                 ▼
MasterWatchdog (6000ms)    LocalAgentAccessibilityService
                                 (Double Vol-Up Panic -> USER_PANIC)
                         │
                         ▼
 Skills / Hardware Controllers / System Control Subsystems
  - CalculatorSkill                - FlashlightController
  - NotesSkill                     - HapticController
  - IntentSkills                   - VolumeController
  - AppLauncherImpl                - ConnectivityControllers
  - ClipboardController            - HardwareObservationControllers
  - NotificationController         - SystemControlControllers
  - UsageStatsController           - SpeechToTextEngine
  - DisplayController              - TextToSpeechEngine
  - CameraController               - PermissionManager
                         │
                         ▼
               Android Framework APIs
                         │
                         ▼
   SkillResult / TestResultStore / EvidenceManager / UI Log
```

---

## 4. Repository Structure

```text
LocalAgent/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/agent/android/
│   │   │   │   ├── actions/          # Action contract abstractions
│   │   │   │   ├── agent/
│   │   │   │   │   ├── device/       # Hardware & system controllers
│   │   │   │   │   └── skills/       # Deterministic skills (Math, Notes, Intents, AppLaunch)
│   │   │   │   ├── commands/         # Production CommandRegistry & argument parsing
│   │   │   │   ├── diagnostics/      # FoundationReadinessEvaluator & status reports
│   │   │   │   ├── execution/        # ExecutionController, GoalDispatcherImpl, State Machine
│   │   │   │   ├── learning/         # Bounded learning schemas & storage
│   │   │   │   ├── observation/     # Observation contract models
│   │   │   │   ├── permissions/      # PermissionRegistry & PermissionManager
│   │   │   │   ├── safety/           # Safety state machine, MasterWatchdog, Test Harness
│   │   │   │   ├── service/          # Accessibility & Notification Listener Services
│   │   │   │   ├── speech/           # Native STT & TTS platform wrappers
│   │   │   │   ├── storage/          # Logger & persistent storage
│   │   │   │   ├── test/             # FoundationTestRegistry, EvidenceManager, TestResultStore
│   │   │   │   └── MainActivity.kt   # 4-Tab Console UI (Test Runner, Permissions, Diagnostics, Console)
│   │   │   ├── res/                  # UI layouts & resource definitions
│   │   │   └── AndroidManifest.xml   # Manifest permissions, services, and activity declarations
│   │   └── test/java/com/agent/android/ # 98 JUnit unit tests
│   └── build.gradle                  # App build configuration (AGP 8.5.2, minSdk 27, targetSdk 34)
├── docs/                             # Comprehensive architecture & module documentation
├── .github/workflows/ci.yml          # GitHub Actions CI pipeline
├── build.gradle                      # Root Gradle build script
├── settings.gradle                   # Gradle settings
├── COMMANDS.md                       # Comprehensive 53-command manual
└── README.md                         # Project overview and reference guide
```

---

## 5. Technology Stack

- **Language:** Kotlin 1.9.24
- **Build System:** Gradle 8.8
- **Android Gradle Plugin (AGP):** 8.5.2
- **JDK Target:** JDK 17
- **Minimum SDK:** API 27 (Android 8.1 Oreo)
- **Compile SDK:** API 34 (Android 14)
- **Target SDK:** API 34 (Android 14)
- **Dependencies:** AndroidX Core KTX, AndroidX AppCompat, Material Components, JUnit 4, Roboelectric (Unit Testing)

---

## 6. Target Device Constraints (Tecno Camon i)

- **OS Version:** Android 8.1 (API 27)
- **SoC:** MediaTek Helio P23 (Octa-core 2.0 GHz)
- **RAM:** 4 GB
- **Resource Constraints:**
  - Zero continuous polling or infinite loops.
  - On-demand HandlerThread sensor sampling with automatic unregistration.
  - Bounded storage for logs (max 100 entries) and test evidence screenshots (max 20 directories).
  - Explicit job cancellation and 6000ms watchdog execution budgets.

---

## 7. Current Capabilities Matrix

| Category | Capability | Status | Permission Required | Special Access | Hardware Required | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| Headless Core | Calculator | IMPLEMENTED_AND_USABLE | None | None | No | Evaluates expressions with precedence, decimal, power, modulo |
| Headless Core | Notes Storage | IMPLEMENTED_PERMISSION_REQUIRED | `WRITE_EXTERNAL_STORAGE` | None | No | Appends entries to persistent local notes file |
| Headless Core | Timer Intent | IMPLEMENTED_AND_USABLE | None | None | No | Launches system Clock timer intent |
| Headless Core | Alarm Intent | IMPLEMENTED_AND_USABLE | None | None | No | Launches system Clock alarm intent |
| Headless Core | Web Search | IMPLEMENTED_AND_USABLE | None | None | No | Launches system web search intent |
| Application | App Launching | IMPLEMENTED_AND_USABLE | None | None | No | Launches apps via exact package -> label -> substring resolution |
| Application | App Discovery | IMPLEMENTED_AND_USABLE | None | None | No | Enumerates installed launchable apps and package info |
| Application | Foreground App | IMPLEMENTED_SPECIAL_ACCESS_REQUIRED | None | `PACKAGE_USAGE_STATS` | No | Queries active foreground app package via UsageStatsManager |
| Device Control | Flashlight | IMPLEMENTED_PERMISSION_REQUIRED | `CAMERA` | None | Torch Flash | Toggles camera flash torch ON/OFF |
| Device Control | Haptics | IMPLEMENTED_AND_USABLE | `VIBRATE` | None | Vibrator | Triggers timed vibration (1-2000ms bounds) |
| Device Control | Volume Streams | IMPLEMENTED_AND_USABLE | None | None | Audio | Gets/sets volume % for MUSIC, RING, ALARM, NOTIFICATION |
| Connectivity | Wi-Fi Status | IMPLEMENTED_AND_USABLE | `ACCESS_WIFI_STATE` | None | Wi-Fi | Queries Wi-Fi adapter state |
| Connectivity | Bluetooth Status | IMPLEMENTED_BUT_OS_RESTRICTED | `BLUETOOTH` | None | Bluetooth | Direct toggle restricted by Android 8.1 OS; opens Settings |
| Observation | Battery Status | IMPLEMENTED_AND_USABLE | None | None | Battery | Reads battery level % and charging status via IntentFilter |
| Observation | Sensor Sampling | IMPLEMENTED_HARDWARE_DEPENDENT | None | None | Sensors | On-demand sampling for Accelerometer, Gyro, Proximity, Light |
| System Control | Screen Brightness | IMPLEMENTED_SPECIAL_ACCESS_REQUIRED | None | `WRITE_SETTINGS` | Display | Queries read-only level; sets brightness with WRITE_SETTINGS |
| System Control | Ringer Mode | IMPLEMENTED_SPECIAL_ACCESS_REQUIRED | None | Notification Policy | Audio | Gets ringer mode; sets Normal/Vibrate/Silent with DND access |
| System Control | Location Status | IMPLEMENTED_PERMISSION_REQUIRED | `ACCESS_FINE_LOCATION` | None | GPS/Network | Queries GPS_PROVIDER and NETWORK_PROVIDER state |
| Device Control | Clipboard | IMPLEMENTED_AND_USABLE | None | None | No | Status, Read, Write, Clear text on system clipboard |
| Observation | Notifications | IMPLEMENTED_SPECIAL_ACCESS_REQUIRED | None | Notification Listener | No | Reads latest notification snapshot via NotificationListenerService |
| Speech | STT (Speech) | IMPLEMENTED_PERMISSION_REQUIRED | `RECORD_AUDIO` | None | Microphone | Built-in SpeechRecognizer wrapper with timeouts |
| Speech | TTS (Speech) | IMPLEMENTED_AND_USABLE | None | None | Speaker | Native TextToSpeech engine initialization and speech output |
| Diagnostics | Accessibility | IMPLEMENTED_SPECIAL_ACCESS_REQUIRED | None | Accessibility Service | No | Native volume key pass-through; double Vol-Up panic stop |

---

## 8. What LocalAgent Can Do NOW

- **Mathematical Calculation:** Evaluate arithmetic expressions (`calculate 12 + 34 * 2`, `calculate (2 + 3) ^ 3`).
- **Local Note Appending:** Persist notes to disk (`note down buy milk`).
- **System Intents:** Trigger timers (`timer 60`), alarms (`alarm 07:30`), and web search (`web search localagent`).
- **App Management:** Launch apps by name/query (`open settings`), list installed apps (`app list`), and inspect package details (`app info com.android.settings`).
- **Hardware Control:** Control flashlight torch (`flashlight on`/`off`), vibrate motor (`vibrate 200`), and adjust volume streams (`volume music 50`).
- **Connectivity Inspection:** Check Wi-Fi state (`wifi status`) and Bluetooth state (`bluetooth status`).
- **Sensor Sampling:** Sample real-time Accelerometer, Gyroscope, Proximity (cm with range metadata), and Ambient Light (lux) readings.
- **System Settings:** Query/set brightness (`brightness 50`), set ringer mode (`ringer vibrate`), and inspect GPS/Network location provider states (`location providers`).
- **Clipboard Control:** Read, write, and clear system clipboard contents (`clipboard write hello`, `clipboard read`).
- **Speech Services:** Perform native text-to-speech (`speak Foundation test successful`) and speech-to-text input listening (`stt listen`).
- **Foundation Diagnostics & Test Runner:** Execute sequential data-driven test cases, capture evidence, persist results, and evaluate 9 readiness gates.

---

## 9. What LocalAgent CANNOT Do Yet (Phase 3+ Exclusions)

- **NO Autonomous AI / LLMs:** Zero Gemini, OpenAI, or external LLM API integrations exist in the repository.
- **NO Autonomous Planning:** No goal decomposition or multi-step autonomous planning loops.
- **NO Screen Crawling / DOM Understanding:** No UI hierarchy tree parsing or recursive accessibility traversal.
- **NO Chrome / Web Automation:** No browser DOM interaction or web crawling.
- **NO Camera Automation / OCR:** No optical character recognition or image processing pipelines.

---

## 10. Android Limitations & Fallbacks

1. **Direct Bluetooth Toggle Restriction (API 27+):**
   - Android 8.1 restricts third-party apps from toggling Bluetooth directly via `BluetoothAdapter.enable()`.
   - *Fallback:* `ConnectivityControllers` returns `BLOCKED / UNSUPPORTED_DIRECT_CONTROL` and launches `Settings.ACTION_BLUETOOTH_SETTINGS`.
2. **Do Not Disturb / Notification Policy Access:**
   - Changing ringer modes (`ringer silent`, `ringer normal`) requires `ACCESS_NOTIFICATION_POLICY`.
   - *Fallback:* Returns `PERMISSION_REQUIRED` with guidance to grant access in Settings -> Do Not Disturb Access.
3. **Write System Settings Access:**
   - Modifying system brightness (`brightness 50`) requires `Settings.System.canWrite(context)`.
   - *Fallback:* Returns `PERMISSION_REQUIRED` with guidance to enable Settings -> Write System Settings.
4. **Usage Statistics Access:**
   - Reading active foreground package (`app.current`) requires `PACKAGE_USAGE_STATS`.
   - *Fallback:* Returns `PERMISSION_REQUIRED` with guidance to enable Settings -> Usage Access.

---

## 11. Permissions Model

Managed centrally via `PermissionRegistry` and `PermissionManager`:
- **Runtime Permissions:** `RECORD_AUDIO`, `CAMERA`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, `READ_PHONE_STATE`.
- **Special Accesses:** `WRITE_SETTINGS`, `ACCESS_NOTIFICATION_POLICY`, `BIND_NOTIFICATION_LISTENER_SERVICE`, `PACKAGE_USAGE_STATS`, `BIND_ACCESSIBILITY_SERVICE`.

---

## 12. Safety Model & Panic Button

- **One Action At A Time:** Enforced globally by `ExecutionController.acquireExecution()`.
- **MasterWatchdog:** Enforces a 6000ms default timeout budget per execution; cancels hanging coroutine jobs safely.
- **Physical Volume Key Pass-Through:** `LocalAgentAccessibilityService.onKeyEvent()` returns `false` for single volume key presses to preserve native Android volume key behavior.
- **Double Volume-Up Panic Gesture:** Detecting two Volume-Up presses within 500ms triggers emergency cancellation (`USER_PANIC`), executes `GLOBAL_ACTION_HOME`, and resets execution state to `IDLE`.

---

## 13. Testing & Evidence System

- **Sequential Test Runner UI:** Interactive 4-tab UI in `MainActivity` executing tests one at a time.
- **Data-Driven Test Registry (`FoundationTestRegistry`):** Contains 82 test cases covering all 53 registered production commands with positive, negative, permission-blocked, hardware-dependent, and physical observation test types.
- **Result Persistence (`TestResultStore`):** Saves test run states in `SharedPreferences` as JSON across activity recreations.
- **Evidence Capture (`EvidenceManager`):** Writes screenshot PNGs and result JSON files to `filesDir/evidence/` with bounded directory rotation (max 20 directories).

---

## 14. Build Instructions

### Gradle Build Commands
```bash
# 1. Run unit test suite (164 unit tests)
./gradlew testDebugUnitTest

# 2. Run Android Lint checks
./gradlew lintDebug

# 3. Assemble debug APK
./gradlew assembleDebug
```

### Output APK
- **Location:** `app/build/outputs/apk/debug/app-debug.apk`
- **File Size:** `~3,499,352 bytes`

---

## 15. GitHub Release & In-Place APK Update Workflow

To support rapid testing directly on physical hardware without USB debugging or ADB, new builds are published to GitHub and installed directly on the device as in-place updates over the existing `LocalAgent` installation.

### Iterative Testing Loop

```text
CODE CHANGE ──► GITHUB REPO ──► GITHUB RELEASE/CI ──► DOWNLOAD APK ON PHONE ──► INSTALL OVER EXISTING ──► TEST
```

### Versioning Strategy

`LocalAgent` uses monotonically increasing `versionCode` values and phase-aligned `versionName` tags in `app/build.gradle.kts`:
- **`0.1` (versionCode 1):** Phase 2 Headless Core & Device Control Complete.
- **`0.2` (versionCode 2):** Phase 3.1 Observation Foundation & Phase 3.2 Target Resolution.
- **`0.3` (versionCode 3):** Phase 3.2 Action Execution Foundation.

### In-Place Application Update Principles

When updating `LocalAgent` by opening the downloaded APK directly on the phone:
1. **Package Identity:** The `applicationId` remains strictly `com.agent.android`.
2. **Signing Identity:** Builds retain matching keystore signatures.
3. **In-Place Preservation:** Android Package Installer updates the app in-place without uninstalling:
   - **Preserved Data:** SQLite databases, `SharedPreferences`, test history, observation settings, evidence files (`filesDir/evidence/`).
   - **Preserved Accesses (Android 8.1+):** Runtime permissions (`CAMERA`, `RECORD_AUDIO`, `STORAGE`, `LOCATION`) and special access grants (`Accessibility Service`, `Notification Listener`, `Usage Access`, `Notification Policy`, `Write System Settings`) remain active across updates.
4. **No Uninstall Required:** Do **NOT** uninstall `LocalAgent` before installing a new GitHub APK update. Uninstalling wipes all app data and revokes granted special access.

---

## 16. CI/CD Pipeline

The project uses GitHub Actions (`.github/workflows/ci.yml`):
1. **Triggers:** Push / PR to main/master branches.
2. **Environment:** Ubuntu-latest with JDK 17 setup.
3. **Pipeline Steps:**
   - Gradle wrapper validation.
   - Run unit tests (`./gradlew testDebugUnitTest`).
   - Run Android Lint (`./gradlew lintDebug`).
   - Assemble debug APK (`./gradlew assembleDebug`).
   - Upload APK artifact.

---

## 16. Development Rules & Phase Roadmap

### Rules
1. Every new command added to `CommandRegistry` **must** have corresponding test coverage added to `FoundationTestRegistry`.
2. All goal executions must route strictly through `GoalDispatcherImpl` and `ExecutionController`.
3. Single volume key events must never be consumed by `LocalAgentAccessibilityService`.

### Roadmap
- **Phases 0 - 2.5 (COMPLETED):** Foundation core, safety boundary, headless skills, hardware controllers, command registry, speech abstractions, permission subsystem, test runner, 100% command test coverage contract.
- **Phase 3 (NEXT):** Local LLM integration, prompt engineering, structured JSON tool calling, context window management.
- **Phase 4 (FUTURE):** Autonomous web/UI interaction, DOM understanding, multi-step goal planning loops.
