# LocalAgent Volume Subsystem Architecture & Key Safety

## Overview
LocalAgent provides structured per-stream volume control for four core Android audio streams:
- `ALARM` (`AudioManager.STREAM_ALARM`)
- `RING` (`AudioManager.STREAM_RING`)
- `NOTIFICATION` (`AudioManager.STREAM_NOTIFICATION`)
- `MUSIC` (`AudioManager.STREAM_MUSIC`)

## Commands Supported per Stream
For each stream (`alarm`, `ring`, `notification`, `music`):
- `volume <stream> status`: Returns complete status (`Stream`, `Current Index`, `Maximum Index`, `Current Percentage`).
- `volume <stream> current`: Returns current index only.
- `volume <stream> maximum`: Returns maximum index only.
- `volume <stream> percentage`: Returns current percentage only.
- `volume <stream> <0..100>`: Sets volume percentage with read-after-write verification.

## Index Calculation & Rounding Model
- Target Index Calculation: `targetIndex = round((percentage / 100.0) * maximumIndex)`
- Percentage Calculation: `percentage = round((currentIndex.toDouble() / maximumIndex.toDouble()) * 100.0)`
- Example (15 Max Index):
  - Index 7 / 15 -> `47%`
  - Index 8 / 15 -> `53%`
  - Index 15 / 15 -> `100%`
  - Index 0 / 15 -> `0%`
- Example (7 Max Index):
  - Index 3 / 7 -> `43%`

## Read-After-Write Verification
After setting a volume stream level via `AudioManager.setStreamVolume()`, `VolumeController` immediately queries `audioManager.getStreamVolume(streamType)` to verify the actual resulting stream index. If the stream index is locked or unchanged by OS policy, `UNSUPPORTED_FIXED_VOLUME` is returned.

## Physical Volume Key Preservation & Panic Safety
1. **Pass-Through**: In `LocalAgentAccessibilityService.onKeyEvent()`, single physical Volume Up, Volume Down, or Volume Mute key presses return `false` immediately, allowing Android OS to handle volume key changes normally.
2. **Panic Gesture**: Only a double Volume-Up press within 500ms is consumed (returns `true`), cancelling active executions and performing `GLOBAL_ACTION_HOME`.
