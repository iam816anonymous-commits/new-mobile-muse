# LocalAgent Platform Capabilities Inventory (Phase 2.5)

| Capability | Controller | Requirements / Permissions | Status |
|---|---|---|---|
| Clipboard | `ClipboardController` | `ClipboardManager` | IMPLEMENTED |
| Notifications | `NotificationController` | `NotificationListenerService` | IMPLEMENTED |
| Foreground App / Usage | `UsageStatsController` | `USAGE_STATS_ACCESS` | IMPLEMENTED |
| App Discovery | `AppDiscoveryController` | `PackageManager` | IMPLEMENTED |
| Display Geometry | `DisplayController` | `WindowManager` / `DisplayMetrics` | IMPLEMENTED |
| Screen Capture | `ScreenCaptureController` | `MediaProjection` (User Consent) | IMPLEMENTED |
| Camera Info | `CameraController` | `CameraManager` / `CAMERA` | IMPLEMENTED |
| Network State | `NetworkController` | `ConnectivityManager` | IMPLEMENTED |
| Location Providers | `LocationController` | `LocationManager` | IMPLEMENTED |
| Power / Interactivity | `PowerStateController` | `PowerManager` | IMPLEMENTED |
| Device State Snapshot | `DeviceStateController` | Aggregated State Query | IMPLEMENTED |
| Accessibility Actions | `AccessibilityActionController` | Action Contracts (Tap, Swipe, Scroll, Back, Home) | FOUNDATION_ONLY |
| Text Input | `TextInputController` | Input Contracts (Set, Append, Clear, Paste) | FOUNDATION_ONLY |
| Visualizer | `InteractionVisualizer` | Visual Overlay Contracts | FOUNDATION_ONLY |
