# LocalAgent Hardware Capabilities Registry

## Overview
`CapabilityRegistry` provides three-tier detection for every hardware feature:
1. `capabilityExists`: Physical hardware presence on device.
2. `capabilityPermitted`: Granted permissions / special access settings.
3. `capabilityUsable`: `capabilityExists && capabilityPermitted`.

## Supported Hardware Capabilities

| Capability | Hardware Check | Permission Check | Notes |
|---|---|---|---|
| Flashlight | `CameraManager.cameraIdList` | `Manifest.permission.CAMERA` | Toggles camera torch |
| Vibration | `Vibrator.hasVibrator()` | None | Bounds 1-2000ms |
| Volume Streams | `AudioManager` | None | MUSIC, RING, ALARM, NOTIFICATION, SYSTEM |
| Wi-Fi | `FEATURE_WIFI` | None | Reads status; direct toggle restricted on Android 10+ |
| Bluetooth | `FEATURE_BLUETOOTH` | None | Reads status; direct toggle restricted on Android 10+ |
| Accelerometer | `Sensor.TYPE_ACCELEROMETER` | None | 3-axis acceleration sampling |
| Gyroscope | `Sensor.TYPE_GYROSCOPE` | None | 3-axis rotation rate sampling |
| Proximity | `Sensor.TYPE_PROXIMITY` | None | Distance / near-far sampling |
| Light | `Sensor.TYPE_LIGHT` | None | Illuminance (lux) sampling |
| Speech-to-Text | `SpeechRecognizer.isRecognitionAvailable` | `Manifest.permission.RECORD_AUDIO` | Android built-in speech recognition |
| Text-to-Speech | `TextToSpeech` engine | None | Android built-in TTS |
