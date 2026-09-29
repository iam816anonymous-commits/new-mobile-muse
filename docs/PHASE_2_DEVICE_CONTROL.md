# Phase 2 Device & Hardware Control Architecture

## Hardware & Connectivity Operations

1. **Flashlight (`FlashlightController.kt`)**: Uses `CameraManager.setTorchMode()`.
2. **Haptics (`HapticController.kt`)**: Uses `Vibrator` API. Durations are capped between 50ms and 2000ms.
3. **Volume (`VolumeController.kt`)**: Queries and sets `AudioManager` stream volumes (`STREAM_MUSIC`, etc.).
4. **Connectivity (`ConnectivityControllers.kt`)**: Queries `WifiManager` & `BluetoothAdapter`. If direct toggle is restricted by Android OS, returns `UNSUPPORTED_DIRECT_CONTROL`.
5. **Battery Observation (`HardwareObservationControllers.kt`)**: Read-only `Intent.ACTION_BATTERY_CHANGED` receiver query.
6. **On-Demand Sensor Sampling (`HardwareObservationControllers.kt`)**:
   * Registers `SensorEventListener` for a single sample.
   * Enforces a 1000ms timeout with `withTimeoutOrNull`.
   * Unregisters listener in `finally` block.
