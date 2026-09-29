# Phase 2.1 Device Diagnostics Architecture

## System Diagnostic Capabilities

The `runDeviceDiagnostics()` method in `HardwareObservationControllers.kt` generates a comprehensive `FullDeviceReport` model inspecting physical device hardware and OS capabilities:

1. **OS & Model**: Android version, API level, manufacturer, device model.
2. **Camera Torch**: `CameraManager` torch availability check.
3. **Vibrator**: `Vibrator.hasVibrator()` check.
4. **Audio Streams**: Music stream min/max/current volume indices.
5. **Intent Resolution**: `PackageManager` resolution status for `AlarmClock.ACTION_SET_TIMER`, `AlarmClock.ACTION_SET_ALARM`, and `Intent.ACTION_WEB_SEARCH`.
6. **Sensor Diagnostic Array**: For Accelerometer, Gyroscope, Proximity, Light, and Magnetometer:
   * Presence / `isAvailable` boolean
   * Sensor name & vendor
   * Hardware version & power consumption (mA)
   * Sensor resolution & maximum range

## On-Demand Sensor Sampling Lifecycle

```text
User Command / Diagnostic Request
              │
              ▼
   getDefaultSensor(type)
              │
      ├── NULL? ──► Return UNSUPPORTED (NO_SENSOR)
      │
      ▼
registerListener(listener, Sensor, SENSOR_DELAY_NORMAL)
              │
      ├── Failed? ──► Return FAILED (REGISTRATION_FAILED)
      │
      ▼
await first sample with timeout (1000ms)
              │
      ├── Timeout? ──► Return FAILED (SENSOR_TIMEOUT)
      │
      ▼
  finally { unregisterListener(listener) }  ◄── Mandatory Cleanup
```
