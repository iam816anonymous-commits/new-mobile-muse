# LocalAgent Central Command Registry

## Overview
Phase 2.4 introduces the production-owned **Central Command Registry** (`com.agent.android.commands.CommandRegistry`). The Command Registry is the single source of truth for all commands supported by LocalAgent.

## Architecture Pipeline

```
User Input / UI / STT
         |
         v
  CommandRegistry (findCommandForInput & parseArguments)
         |
         v
  CommandDefinition & CommandArguments
         |
         v
  GoalDispatcherImpl
         |
         v
  ExecutionController (Lock & Watchdog)
         |
         v
  Skill / Device Controller / System Controller
         |
         v
  CommandResult
```

## Command Definition Schema (`CommandDefinition`)
Each command contains:
- `commandId`: Unique stable identifier (e.g. `calculator.calculate`, `volume.music.set`).
- `name`: Human-readable name.
- `category`: `CommandCategory` enum (`SAFETY`, `HEADLESS_CORE`, `APPLICATION`, `DEVICE`, `CONNECTIVITY`, `OBSERVATION`, `SYSTEM_CONTROLS`, `SPEECH`, `DIAGNOSTICS`).
- `description`: Detailed explanation of command behavior.
- `status`: `CommandStatus` enum (`IMPLEMENTED`, `PARTIAL`, `UNSUPPORTED`, `PLANNED`).
- `syntax`: Command syntax template.
- `examples`: List of valid command execution examples.
- `argumentSchema`: Argument field names.
- `requirement`: `CommandRequirement` (required runtime permissions, special access, hardware capabilities, system services, accessibility, physical observation, state changing, app launching).
- `handlerIdentifier`: Target production class handler.

## Registered Command Inventory

| Command ID | Category | Status | Example | Required Capabilities / Permissions | Handler |
|---|---|---|---|---|---|
| `safety.status` | SAFETY | IMPLEMENTED | `safety status` | None | ExecutionController |
| `safety.cancel` | SAFETY | IMPLEMENTED | `safety cancel` | None | ExecutionController |
| `safety.panic` | SAFETY | IMPLEMENTED | `panic` | Accessibility Service | LocalAgentAccessibilityService |
| `calculator.calculate` | HEADLESS_CORE | IMPLEMENTED | `calculate 12 + 34` | None | CalculatorSkill |
| `notes.append` | HEADLESS_CORE | IMPLEMENTED | `note append buy milk` | WRITE_EXTERNAL_STORAGE | NotesSkill |
| `timer.create` | HEADLESS_CORE | IMPLEMENTED | `timer 60` | Clock Intent | IntentSkills |
| `alarm.create` | HEADLESS_CORE | IMPLEMENTED | `alarm 07:30` | Clock Intent | IntentSkills |
| `web.search` | HEADLESS_CORE | IMPLEMENTED | `web search localagent` | Search Intent | IntentSkills |
| `app.launch` | APPLICATION | IMPLEMENTED | `open settings` | PackageManager | AppLauncherImpl |
| `flashlight.status` | DEVICE | IMPLEMENTED | `flashlight status` | FLASHLIGHT | FlashlightController |
| `flashlight.on` | DEVICE | IMPLEMENTED | `flashlight on` | FLASHLIGHT, CAMERA | FlashlightController |
| `flashlight.off` | DEVICE | IMPLEMENTED | `flashlight off` | FLASHLIGHT, CAMERA | FlashlightController |
| `haptics.status` | DEVICE | IMPLEMENTED | `vibrate status` | VIBRATION | HapticController |
| `haptics.vibrate` | DEVICE | IMPLEMENTED | `vibrate 200` | VIBRATION | HapticController |
| `volume.music.status` | DEVICE | IMPLEMENTED | `volume music status` | VOLUME | VolumeController |
| `volume.music.set` | DEVICE | IMPLEMENTED | `volume music 50` | VOLUME | VolumeController |
| `volume.ring.status` | DEVICE | IMPLEMENTED | `volume ring status` | VOLUME | VolumeController |
| `volume.ring.set` | DEVICE | IMPLEMENTED | `volume ring 50` | VOLUME | VolumeController |
| `volume.notification.status` | DEVICE | IMPLEMENTED | `volume notification status` | VOLUME | VolumeController |
| `volume.notification.set` | DEVICE | IMPLEMENTED | `volume notification 50` | VOLUME | VolumeController |
| `volume.alarm.status` | DEVICE | IMPLEMENTED | `volume alarm status` | VOLUME | VolumeController |
| `volume.alarm.set` | DEVICE | IMPLEMENTED | `volume alarm 50` | VOLUME | VolumeController |
| `volume.system.status` | DEVICE | IMPLEMENTED | `volume system status` | VOLUME | VolumeController |
| `volume.system.set` | DEVICE | IMPLEMENTED | `volume system 50` | VOLUME | VolumeController |
| `wifi.status` | CONNECTIVITY | IMPLEMENTED | `wifi status` | WIFI | ConnectivityControllers |
| `wifi.on` | CONNECTIVITY | IMPLEMENTED | `wifi on` | WIFI | ConnectivityControllers |
| `wifi.off` | CONNECTIVITY | IMPLEMENTED | `wifi off` | WIFI | ConnectivityControllers |
| `bluetooth.status` | CONNECTIVITY | IMPLEMENTED | `bluetooth status` | BLUETOOTH | ConnectivityControllers |
| `bluetooth.on` | CONNECTIVITY | IMPLEMENTED | `bluetooth on` | BLUETOOTH | ConnectivityControllers |
| `bluetooth.off` | CONNECTIVITY | IMPLEMENTED | `bluetooth off` | BLUETOOTH | ConnectivityControllers |
| `battery.status` | OBSERVATION | IMPLEMENTED | `battery status` | BATTERY | SystemControlControllers |
| `sensor.accelerometer.sample` | OBSERVATION | IMPLEMENTED | `sensor accelerometer` | ACCELEROMETER | HardwareObservationControllers |
| `sensor.gyroscope.sample` | OBSERVATION | IMPLEMENTED | `sensor gyroscope` | GYROSCOPE | HardwareObservationControllers |
| `sensor.proximity.sample` | OBSERVATION | IMPLEMENTED | `sensor proximity` | PROXIMITY | HardwareObservationControllers |
| `sensor.light.sample` | OBSERVATION | IMPLEMENTED | `sensor light` | LIGHT | HardwareObservationControllers |
| `brightness.status` | SYSTEM_CONTROLS | IMPLEMENTED | `brightness status` | BRIGHTNESS | SystemControlControllers |
| `brightness.set` | SYSTEM_CONTROLS | IMPLEMENTED | `brightness 128` | WRITE_SETTINGS | SystemControlControllers |
| `ringer.status` | SYSTEM_CONTROLS | IMPLEMENTED | `ringer status` | RINGER | SystemControlControllers |
| `ringer.normal` | SYSTEM_CONTROLS | IMPLEMENTED | `ringer normal` | RINGER | SystemControlControllers |
| `ringer.vibrate` | SYSTEM_CONTROLS | IMPLEMENTED | `ringer vibrate` | RINGER | SystemControlControllers |
| `ringer.silent` | SYSTEM_CONTROLS | IMPLEMENTED | `ringer silent` | Notification Policy Access | SystemControlControllers |
| `location.status` | SYSTEM_CONTROLS | IMPLEMENTED | `location status` | LOCATION_STATUS | SystemControlControllers |
| `stt.status` | SPEECH | IMPLEMENTED | `stt status` | STT | SpeechToTextEngine |
| `stt.listen` | SPEECH | IMPLEMENTED | `stt listen` | STT, RECORD_AUDIO | SpeechToTextEngine |
| `stt.cancel` | SPEECH | IMPLEMENTED | `stt cancel` | STT | SpeechToTextEngine |
| `tts.status` | SPEECH | IMPLEMENTED | `tts status` | TTS | TextToSpeechEngine |
| `tts.speak` | SPEECH | IMPLEMENTED | `speak Hello` | TTS | TextToSpeechEngine |
| `tts.stop` | SPEECH | IMPLEMENTED | `tts stop` | TTS | TextToSpeechEngine |
| `permissions.status` | DIAGNOSTICS | IMPLEMENTED | `permissions status` | None | CapabilityRegistry |
| `capabilities.status` | DIAGNOSTICS | IMPLEMENTED | `capabilities status` | None | CapabilityRegistry |
| `accessibility.status` | DIAGNOSTICS | IMPLEMENTED | `accessibility status` | None | LocalAgentAccessibilityService |
| `diagnostics.status` | DIAGNOSTICS | IMPLEMENTED | `diagnostics status` | None | HardwareObservationControllers |
| `diagnostics.readiness` | DIAGNOSTICS | IMPLEMENTED | `readiness status` | None | FoundationReadinessEvaluator |
