# Connectivity Controls & Android Restrictions

## Wi-Fi & Bluetooth Controls on API 27

* **Direct Toggling Restrictions**:
  Android restricts non-system apps from toggling Wi-Fi and Bluetooth state directly on certain device configurations or target SDK levels.
* **Fallback Strategy**:
  `ConnectivityControllers` attempts public `setWifiEnabled()` and `BluetoothAdapter.enable()` / `disable()` APIs.
  If the operation returns `false` or throws `SecurityException`, LocalAgent returns `SkillStatus.UNSUPPORTED` with `errorCode = "UNSUPPORTED_DIRECT_CONTROL"` without crashing or attempting shell/root/reflection hacks.
