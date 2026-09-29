# Phase 2 Hardware Capabilities Matrix

**Target Device**: Tecno Camon i (Android 8.1 / API 27, MediaTek Helio P23, 4 GB RAM)

| Capability | Detection API | Status on API 27 |
| :--- | :--- | :--- |
| **Camera Torch (Flashlight)** | `CameraManager.cameraIdList` | AVAILABLE |
| **Vibrator (Haptics)** | `Vibrator.hasVibrator()` | AVAILABLE |
| **Stream Volume** | `AudioManager.getStreamVolume()` | AVAILABLE |
| **Wi-Fi Query / Toggle** | `WifiManager.isWifiEnabled` | AVAILABLE (Direct toggle subject to OS restrictions) |
| **Bluetooth Query / Toggle** | `BluetoothAdapter.getDefaultAdapter()` | AVAILABLE (Direct toggle subject to OS restrictions) |
| **Battery Information** | `Intent.ACTION_BATTERY_CHANGED` | AVAILABLE |
| **Accelerometer / Gyroscope / Proximity / Light** | `SensorManager.getDefaultSensor()` | AVAILABLE (On-demand single sample with timeout) |
