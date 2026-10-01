# Phase 3.1 — Platform & Hardware Limitations (Android API 27)

## 1. Overview
LocalAgent targets Android 8.1 / API 27 hardware (specifically low-end devices like the Tecno Camon i with MediaTek Helio P23 and 4 GB RAM). This document details inherent Android platform and hardware constraints so that system limitations are correctly represented rather than falsely reported as implementation bugs.

## 2. Platform & API Limitations

### API 28+ Features Not Available on API 27
- **`AccessibilityEvent.TYPE_WINDOWS_CHANGED`**: Introduced in API 28 (Android 9.0). On API 27, window changes are detected via `TYPE_WINDOW_STATE_CHANGED` and `TYPE_WINDOW_CONTENT_CHANGED` combined with 1000ms debounced polling.
- **Unexposed System Surfaces**: On certain Android 8.1 OEM builds, system dialogs, notification shades, or secure overlays may choose not to expose node content to accessibility services for security or privacy reasons.

### Exposed vs. Unexposed Window States
The observation engine distinguishes:
- **`EXPOSED`**: Accessibility framework returned a valid `AccessibilityNodeInfo` tree.
- **`NOT_EXPOSED` / `WINDOW_NOT_EXPOSED`**: Window exists on screen but Android accessibility framework returned null root node.
- **`UNAVAILABLE` / `NO_ACTIVE_WINDOW`**: No active window detected.
- **`UNSUPPORTED_API`**: Requested API feature requires a higher Android SDK version.

## 3. Low-End Hardware Safeguards
To guarantee smooth operation on 4 GB RAM devices:
- **Node Count Limit (`MAX_NODE_LIMIT = 500`)**: Tree traversal stops immediately upon reaching 500 nodes.
- **Depth Limit (`MAX_DEPTH_LIMIT = 30`)**: Tree traversal stops at 30 nested levels.
- **Node Recycling (`nodeInfo.recycle()`)**: Every `AccessibilityNodeInfo` object obtained during traversal is recycled in `finally` blocks to prevent memory leaks and garbage collection pauses.
- **Debounce Interval (`DEBOUNCE_INTERVAL_MS = 1000L`)**: Minimum 1 second interval between background event captures to prevent event storms or ANRs.

## 4. Perception Boundary (Phase 3.1 Non-Scope)
Phase 3.1 is strictly **READ-ONLY perception**. The following capabilities are explicitly NOT part of Phase 3.1:
- Zero UI clicks, taps, long presses, or gestures.
- Zero text typing or soft keyboard injection.
- Zero autonomous goal planning or LLM integration.
- Zero computer vision, OCR, or image processing dependencies.
