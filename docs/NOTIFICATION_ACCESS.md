# LocalAgent Notification Listener Subsystem

## Overview
`LocalAgentNotificationListenerService` extends `NotificationListenerService` to provide observation-only notification snapshots when enabled by the user in Android Settings.

## Commands
- `notification status`: Queries service connection and permission state.
- `notification latest`: Reads latest received notification snapshot (`packageName`, `title`, `text`, `postTimeMs`).

## Safety Rules
- Returns `BLOCKED / PERMISSION_REQUIRED` when Notification Listener Access is missing.
- No continuous storage of notification history or message bodies.
- Does NOT execute actions or trigger autonomous loops on notification events.
