package com.agent.android.observation

import java.util.concurrent.atomic.AtomicLong

class ObservationSnapshotStore {

    @Volatile
    var currentLiveSnapshot: ObservationSnapshot? = null
        private set

    @Volatile
    var lastValidExternalSnapshot: ObservationSnapshot? = null
        private set

    @Volatile
    var displayedSnapshot: ObservationSnapshot? = null
        private set

    @Volatile
    var activeTestRunId: String? = null
        private set

    @Volatile
    var activeTestRunSnapshot: ObservationSnapshot? = null
        private set

    @Volatile
    var observationMode: ObservationMode = ObservationMode.READY
        private set

    private val activeSessionId = AtomicLong(1L)

    fun getSessionId(): Long = activeSessionId.get()

    @Synchronized
    fun startTestRun(runId: String) {
        activeTestRunId = runId
        activeTestRunSnapshot = null
    }

    @Synchronized
    fun endTestRun() {
        activeTestRunId = null
    }

    @Synchronized
    fun startObservationMode(): Long {
        val newSession = activeSessionId.incrementAndGet()
        observationMode = ObservationMode.OBSERVING
        return newSession
    }

    @Synchronized
    fun stopObservationMode(): Long {
        val newSession = activeSessionId.incrementAndGet()
        observationMode = ObservationMode.STOPPED
        // STOP OBSERVATION MUST NOT CAPTURE A NEW SNAPSHOT!
        // Preserve lastValidExternalSnapshot as displayedSnapshot if present.
        displayedSnapshot = lastValidExternalSnapshot ?: currentLiveSnapshot
        return newSession
    }

    @Synchronized
    fun resetToReadyMode() {
        activeSessionId.incrementAndGet()
        observationMode = ObservationMode.READY
    }

    @Synchronized
    fun updateFromCapture(snapshot: ObservationSnapshot, sessionToken: Long? = null) {
        if (sessionToken != null && sessionToken != activeSessionId.get()) {
            // Invalidate late or queued accessibility callbacks from previous sessions
            return
        }

        currentLiveSnapshot = snapshot
        val pkg = snapshot.packageName

        if (activeTestRunId != null && snapshot.testRunId == activeTestRunId) {
            activeTestRunSnapshot = snapshot
        }

        if (snapshot.state == ObservationState.SUCCESS && !isExcludedExternalPackage(pkg)) {
            lastValidExternalSnapshot = snapshot
            displayedSnapshot = snapshot
        } else {
            // LocalAgent / SystemUI / Excluded package capture
            // Keep lastValidExternalSnapshot intact!
            displayedSnapshot = lastValidExternalSnapshot ?: snapshot
        }
    }

    @Synchronized
    fun setExplicitDisplayedSnapshot(snapshot: ObservationSnapshot?) {
        displayedSnapshot = snapshot
        if (snapshot != null && snapshot.state == ObservationState.SUCCESS && !isExcludedExternalPackage(snapshot.packageName)) {
            lastValidExternalSnapshot = snapshot
        }
    }

    @Synchronized
    fun clearSnapshots() {
        currentLiveSnapshot = null
        lastValidExternalSnapshot = null
        displayedSnapshot = null
        activeTestRunSnapshot = null
    }

    fun isExcludedExternalPackage(pkgName: String?): Boolean {
        if (pkgName == null || pkgName == "UNKNOWN") return true
        if (pkgName == "com.agent.android") return true
        if (pkgName == "com.android.systemui") return true
        if (pkgName.contains("launcher", ignoreCase = true)) return true
        if (pkgName.contains("recents", ignoreCase = true)) return true
        return false
    }
}
