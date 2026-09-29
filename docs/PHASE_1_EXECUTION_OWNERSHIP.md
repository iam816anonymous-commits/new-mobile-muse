# Execution Ownership & State Synchronization

## Invariant: Single Active Execution

`ExecutionController` enforces that only one execution context may hold ownership at any given time.

### Acquisition
```kotlin
val acquired = executionController.acquireExecution(job)
if (!acquired) {
    // Concurrent execution request rejected
}
```

### Release & Cleanup Guarantee
```kotlin
try {
    // Run action execution
} finally {
    executionController.releaseExecution(reason)
}
```

`releaseExecution()` guarantees that:
* Active coroutine `Job` is cancelled.
* State machine transitions toward `STOPPING` → `IDLE`.
* `SafetyState` is updated appropriately.
* `isExecutionActive` lock is released.
