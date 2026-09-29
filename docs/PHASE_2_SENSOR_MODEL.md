# Phase 2 Sensor Sampling Model

## Resource & Battery Safeguards

1. **No Continuous Sensor Listening**:
   Sensors are NEVER registered continuously in background tasks.
2. **On-Demand Sampling Flow**:
   ```text
   Request Sample → Check Hardware Availability → Register Listener → Await 1st Sample or Timeout (1000ms) → Unregister Listener → Return Result
   ```
3. **Guaranteed Unregistration**:
   `SensorManager.unregisterListener()` is invoked in `finally` blocks to guarantee listener cleanup on completion, timeout, or cancellation.
