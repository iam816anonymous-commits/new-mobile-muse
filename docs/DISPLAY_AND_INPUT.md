# LocalAgent Display & Device Snapshot Subsystems

## Display (`DisplayController`)
Queries real display metrics (`widthPx`, `heightPx`, `densityDpi`, `orientation`, `rotation`) using `WindowManager.defaultDisplay.getRealMetrics()`.

## Device Snapshot (`DeviceStateController`)
Aggregates unified state snapshot from existing controllers (battery, network, display, power state).
