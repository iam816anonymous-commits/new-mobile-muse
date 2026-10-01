# Phase 3.1 — Accessibility Observation Data Models

## 1. Data Model Overview
The observation subsystem represents visible user interface hierarchies as immutable data structures.

## 2. Core Models

### `ObservationNode`
Represents an individual UI element in the accessibility tree.
- `id`: Stable string ID (`node-1`, `node-2`).
- `parentId`: Nullable string ID of parent node.
- `className`: Widget class name (e.g. `android.widget.TextView`, `android.widget.Button`).
- `packageName`: Package name exposing the node (e.g. `com.android.chrome`).
- `text`: Visible string text content.
- `contentDescription`: Accessibility content description.
- `resourceId`: View resource ID (e.g. `com.android.settings:id/title`).
- `bounds`: `ObservationBounds` (`left`, `top`, `right`, `bottom`).
- `isClickable`: Boolean indicating click capability.
- `isLongClickable`: Boolean indicating long click capability.
- `isFocusable` / `isFocused`: Focus state properties.
- `isEnabled`: Enabled/disabled state.
- `isEditable`: Editable text input state.
- `isScrollable`: Scroll container property.
- `isCheckable` / `isChecked` / `isSelected`: Toggle and selection states.
- `isVisibleToUser`: Visibility flag.
- `isPassword`: Sensitive password field flag.
- `childCount`: Number of child nodes.
- `children`: List of child `ObservationNode` objects.

### `ObservationSnapshot`
Represents a complete captured screen/window state.
- `timestampMs`: Epoch millisecond timestamp of capture.
- `packageName`: Package name of captured root window.
- `activityName`: Activity class name.
- `windowType`: Window type metadata.
- `rootBounds`: Bounding rectangle of root window.
- `nodeCount`: Total nodes captured in tree (max 500).
- `rootNode`: Root `ObservationNode`.
- `allNodesList`: Flattened list of all nodes for rapid lookup.
- `state`: `ObservationState` (`SUCCESS`, `ACCESSIBILITY_DISABLED`, `NO_ACTIVE_WINDOW`, `NO_ACCESSIBLE_WINDOWS`, `ROOT_NODE_UNAVAILABLE`, `WINDOW_NOT_EXPOSED`, `TARGET_NOT_FOUND`, `OBSERVATION_FAILED`, `CANCELLED`).
- `classification`: `WindowClassification` (`APPLICATION`, `LOCAL_AGENT`, `SYSTEM_UI`, `LAUNCHER`, `RECENTS`, `SETTINGS`, `NOTIFICATION_SURFACE`, `QUICK_SETTINGS`, `SYSTEM_DIALOG`, `OVERLAY`, `UNKNOWN`).
- `source`: `ObservationSource` (`LIVE_ACTIVE_WINDOW`, `ACCESSIBLE_WINDOW_QUERY`, `GUIDED_TEST`, `EXPLICIT_CAPTURE`, `RESTORED_SNAPSHOT`).
- `scope`: `ObservationScope` (`CURRENT_WINDOW`, `TARGET_APPLICATION`, `SYSTEM_UI`, `LOCAL_AGENT`, `ACCESSIBLE_WINDOWS`).
- `error`: Nullable error message.

### `ObservedWindow`
Represents an interactive window discovered via `AccessibleWindowProvider`.
- `windowId`: Integer window ID.
- `packageName`: Package name.
- `activityName`: Window class or activity name.
- `windowType`: `AccessibilityWindowInfo` type integer.
- `layer`: Z-order layer integer.
- `bounds`: `ObservationBounds`.
- `isActive` / `isFocused` / `isAccessibilityFocused`: Boolean flags.
- `classification`: `WindowClassification`.
- `rootNode`: Nullable root node.
- `timestampMs`: Capture timestamp.

## 3. JSON Serialization
Both `ObservationNode` and `ObservationSnapshot` support bidirectional JSON serialization via `toJsonObject()`, `toJsonString()`, `fromJsonObject()`, and `fromJsonString()`.
