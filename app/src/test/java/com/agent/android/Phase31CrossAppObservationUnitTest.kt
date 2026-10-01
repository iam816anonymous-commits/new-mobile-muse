package com.agent.android

import com.agent.android.observation.AccessibilityObservationEngine
import com.agent.android.observation.ExternalAppTestValidator
import com.agent.android.observation.ObservationBounds
import com.agent.android.observation.ObservationNode
import com.agent.android.observation.ObservationSnapshot
import com.agent.android.observation.ObservationState
import com.agent.android.test.model.TestStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class Phase31CrossAppObservationUnitTest {

    private lateinit var validator: ExternalAppTestValidator

    @Before
    fun setUp() {
        validator = ExternalAppTestValidator()
    }

    @Test
    fun testEngineValidationWithDisconnectedService() {
        val engine = AccessibilityObservationEngine()
        val res = validator.validateEngine(engine)

        assertEquals(TestStatus.BLOCKED, res.status)
        assertEquals("ACCESSIBILITY SERVICE NOT CONNECTED", res.summaryText)
        assertTrue(res.errorDetails?.contains("Accessibility service is disabled") == true)
    }

    @Test
    fun testExternalValidationWithNullSnapshot() {
        val res = validator.validateExternalAppSnapshot(null)

        assertEquals(TestStatus.BLOCKED, res.status)
        assertEquals("NO EXTERNAL SNAPSHOT CAPTURED", res.summaryText)
    }

    @Test
    fun testExternalValidationRejectingSelfPackage() {
        val snap = ObservationSnapshot(
            timestampMs = System.currentTimeMillis(),
            packageName = "com.agent.android",
            activityName = "com.agent.android.MainActivity",
            windowType = null,
            rootBounds = ObservationBounds(0, 0, 720, 1280),
            nodeCount = 15,
            rootNode = ObservationNode("1", null, "android.widget.FrameLayout", "com.agent.android", null, null, null, ObservationBounds(0, 0, 720, 1280), false, false, false, false, true, false, false, false, false, false, true, false, 0),
            allNodesList = emptyList(),
            state = ObservationState.SUCCESS,
            error = null
        )

        val res = validator.validateExternalAppSnapshot(snap)

        assertEquals(TestStatus.FAILED, res.status)
        assertEquals("TARGET APP IS LOCALAGENT", res.summaryText)
    }

    @Test
    fun testExternalValidationRejectingSystemUIAndLauncher() {
        val sysUiSnap = ObservationSnapshot(
            timestampMs = System.currentTimeMillis(),
            packageName = "com.android.systemui",
            activityName = "com.android.systemui.StatusBar",
            windowType = null,
            rootBounds = ObservationBounds(0, 0, 720, 1280),
            nodeCount = 5,
            rootNode = ObservationNode("1", null, "android.widget.FrameLayout", "com.android.systemui", null, null, null, ObservationBounds(0, 0, 720, 1280), false, false, false, false, true, false, false, false, false, false, true, false, 0),
            allNodesList = emptyList(),
            state = ObservationState.SUCCESS,
            error = null
        )

        val launcherSnap = ObservationSnapshot(
            timestampMs = System.currentTimeMillis(),
            packageName = "com.google.android.apps.nexuslauncher",
            activityName = "com.google.android.apps.nexuslauncher.NexusLauncherActivity",
            windowType = null,
            rootBounds = ObservationBounds(0, 0, 720, 1280),
            nodeCount = 8,
            rootNode = ObservationNode("1", null, "android.widget.FrameLayout", "com.google.android.apps.nexuslauncher", null, null, null, ObservationBounds(0, 0, 720, 1280), false, false, false, false, true, false, false, false, false, false, true, false, 0),
            allNodesList = emptyList(),
            state = ObservationState.SUCCESS,
            error = null
        )

        val res1 = validator.validateExternalAppSnapshot(sysUiSnap)
        val res2 = validator.validateExternalAppSnapshot(launcherSnap)

        assertEquals(TestStatus.FAILED, res1.status)
        assertEquals(TestStatus.FAILED, res2.status)
        assertEquals("INVALID TARGET: SYSTEM UI / LAUNCHER", res1.summaryText)
        assertEquals("INVALID TARGET: SYSTEM UI / LAUNCHER", res2.summaryText)
    }

    @Test
    fun testExternalValidationAcceptingValidExternalTarget() {
        val calcSnap = ObservationSnapshot(
            timestampMs = System.currentTimeMillis(),
            packageName = "com.google.android.calculator",
            activityName = "com.android.calculator2.Calculator",
            windowType = null,
            rootBounds = ObservationBounds(0, 0, 720, 1280),
            nodeCount = 32,
            rootNode = ObservationNode("1", null, "android.widget.LinearLayout", "com.google.android.calculator", null, null, null, ObservationBounds(0, 0, 720, 1280), false, false, false, false, true, false, false, false, false, false, true, false, 0),
            allNodesList = emptyList(),
            state = ObservationState.SUCCESS,
            error = null
        )

        val res = validator.validateExternalAppSnapshot(calcSnap)

        assertEquals(TestStatus.PASSED, res.status)
        assertEquals("EXTERNAL APP OBSERVATION VALIDATED", res.summaryText)
        assertEquals("com.google.android.calculator", res.targetPackage)
        assertEquals("com.android.calculator2.Calculator", res.targetActivity)
        assertEquals(32, res.nodeCount)
        assertNotNull(res.evidencePath)
    }

    @Test
    fun testObservationEnginePackageExclusionRules() {
        val engine = AccessibilityObservationEngine()

        assertTrue("LocalAgent self package must be excluded", engine.isExcludedExternalPackage("com.agent.android"))
        assertTrue("System UI must be excluded", engine.isExcludedExternalPackage("com.android.systemui"))
        assertTrue("Nexus Launcher must be excluded", engine.isExcludedExternalPackage("com.google.android.apps.nexuslauncher"))
        assertTrue("Generic launcher must be excluded", engine.isExcludedExternalPackage("com.android.launcher3"))
        assertTrue("Recents view must be excluded", engine.isExcludedExternalPackage("com.android.systemui.recents"))

        assertEquals(false, engine.isExcludedExternalPackage("com.android.chrome"))
        assertEquals(false, engine.isExcludedExternalPackage("com.google.android.youtube"))
        assertEquals(false, engine.isExcludedExternalPackage("com.android.settings"))
        assertEquals(false, engine.isExcludedExternalPackage("com.google.android.calculator"))
    }

    @Test
    fun testStopObservationModeSnapshotPreservation() {
        val engine = AccessibilityObservationEngine()
        engine.startObservationMode()
        assertEquals(com.agent.android.observation.ObservationMode.OBSERVING, engine.observationMode)

        engine.stopObservationMode()
        assertEquals(com.agent.android.observation.ObservationMode.STOPPED, engine.observationMode)
    }

    @Test
    fun testGuidedTestAppMetadataAndTestIds() {
        assertEquals("P3.1-EXT-001", com.agent.android.observation.GuidedTestApp.CHROME.testId)
        assertEquals("P3.1-EXT-002", com.agent.android.observation.GuidedTestApp.YOUTUBE.testId)
        assertEquals("P3.1-EXT-003", com.agent.android.observation.GuidedTestApp.SETTINGS.testId)
        assertEquals("P3.1-EXT-004", com.agent.android.observation.GuidedTestApp.CALCULATOR.testId)

        assertEquals("com.android.chrome", com.agent.android.observation.GuidedTestApp.CHROME.staticPackage)
        assertEquals("com.google.android.youtube", com.agent.android.observation.GuidedTestApp.YOUTUBE.staticPackage)
        assertEquals("com.android.settings", com.agent.android.observation.GuidedTestApp.SETTINGS.staticPackage)
        assertEquals(null, com.agent.android.observation.GuidedTestApp.CALCULATOR.staticPackage)
    }

    @Test
    fun testStopDoesNotCaptureNewSnapshot() {
        val engine = AccessibilityObservationEngine()
        val initialSnap = engine.getDisplayedSnapshot()

        engine.stopObservationMode()

        assertEquals(com.agent.android.observation.ObservationMode.STOPPED, engine.observationMode)
        assertEquals(initialSnap, engine.getDisplayedSnapshot())
    }

    @Test
    fun testStopPreservesLastExternalSnapshot() {
        val engine = AccessibilityObservationEngine()
        val chromeSnap = ObservationSnapshot(
            timestampMs = System.currentTimeMillis(),
            packageName = "com.android.chrome",
            activityName = "org.chromium.chrome.browser.ChromeTabbedActivity",
            windowType = null,
            rootBounds = ObservationBounds(0, 0, 1080, 1920),
            nodeCount = 200,
            rootNode = ObservationNode("1", null, "android.widget.FrameLayout", "com.android.chrome", null, null, null, ObservationBounds(0, 0, 1080, 1920), false, false, false, false, true, false, false, false, false, false, true, false, 0),
            allNodesList = emptyList(),
            state = ObservationState.SUCCESS,
            error = null
        )

        engine.snapshotStore.updateFromCapture(chromeSnap)
        assertEquals("com.android.chrome", engine.getDisplayedSnapshot()?.packageName)

        engine.stopObservationMode()

        assertEquals(com.agent.android.observation.ObservationMode.STOPPED, engine.observationMode)
        assertEquals("com.android.chrome", engine.getDisplayedSnapshot()?.packageName)
        assertEquals("com.android.chrome", engine.getLastExternalSnapshot()?.packageName)
    }

    @Test
    fun testLocalAgentDoesNotOverwriteExternalSnapshot() {
        val engine = AccessibilityObservationEngine()
        val chromeSnap = ObservationSnapshot(
            timestampMs = 100000L,
            packageName = "com.android.chrome",
            activityName = "org.chromium.chrome.browser.ChromeTabbedActivity",
            windowType = null,
            rootBounds = ObservationBounds(0, 0, 1080, 1920),
            nodeCount = 150,
            rootNode = ObservationNode("1", null, "android.widget.FrameLayout", "com.android.chrome", null, null, null, ObservationBounds(0, 0, 1080, 1920), false, false, false, false, true, false, false, false, false, false, true, false, 0),
            allNodesList = emptyList(),
            state = ObservationState.SUCCESS,
            error = null
        )
        engine.snapshotStore.updateFromCapture(chromeSnap)

        val localAgentSnap = ObservationSnapshot(
            timestampMs = 100500L,
            packageName = "com.agent.android",
            activityName = "com.agent.android.MainActivity",
            windowType = null,
            rootBounds = ObservationBounds(0, 0, 1080, 1920),
            nodeCount = 45,
            rootNode = ObservationNode("1", null, "android.widget.LinearLayout", "com.agent.android", null, null, null, ObservationBounds(0, 0, 1080, 1920), false, false, false, false, true, false, false, false, false, false, true, false, 0),
            allNodesList = emptyList(),
            state = ObservationState.SUCCESS,
            error = null
        )
        engine.snapshotStore.updateFromCapture(localAgentSnap)

        assertEquals("com.android.chrome", engine.getLastExternalSnapshot()?.packageName)
        assertEquals("com.android.chrome", engine.getDisplayedSnapshot()?.packageName)
        assertEquals(150, engine.getDisplayedSnapshot()?.nodeCount)
    }

    @Test
    fun testClearActuallyClearsSnapshot() {
        val engine = AccessibilityObservationEngine()
        val settingsSnap = ObservationSnapshot(
            timestampMs = System.currentTimeMillis(),
            packageName = "com.android.settings",
            activityName = "com.android.settings.Settings",
            windowType = null,
            rootBounds = ObservationBounds(0, 0, 1080, 1920),
            nodeCount = 80,
            rootNode = null,
            allNodesList = emptyList(),
            state = ObservationState.SUCCESS,
            error = null
        )
        engine.snapshotStore.updateFromCapture(settingsSnap)
        assertNotNull(engine.getDisplayedSnapshot())

        engine.clearLastSnapshot()

        assertEquals(null, engine.getDisplayedSnapshot())
        assertEquals(null, engine.getLastExternalSnapshot())
    }

    @Test
    fun testLateObservationCallbackCannotOverwriteAfterStop() {
        val engine = AccessibilityObservationEngine()
        engine.startObservationMode()
        val activeSession = engine.snapshotStore.getSessionId()

        val chromeSnap = ObservationSnapshot(
            timestampMs = System.currentTimeMillis(),
            packageName = "com.android.chrome",
            activityName = "Chrome",
            windowType = null,
            rootBounds = ObservationBounds(0, 0, 1080, 1920),
            nodeCount = 100,
            rootNode = null,
            allNodesList = emptyList(),
            state = ObservationState.SUCCESS,
            error = null
        )
        engine.snapshotStore.updateFromCapture(chromeSnap, activeSession)
        assertEquals("com.android.chrome", engine.getDisplayedSnapshot()?.packageName)

        // User presses STOP
        engine.stopObservationMode()

        // Late callback with old session token
        val lateLocalAgentSnap = ObservationSnapshot(
            timestampMs = System.currentTimeMillis() + 500,
            packageName = "com.agent.android",
            activityName = "MainActivity",
            windowType = null,
            rootBounds = ObservationBounds(0, 0, 1080, 1920),
            nodeCount = 50,
            rootNode = null,
            allNodesList = emptyList(),
            state = ObservationState.SUCCESS,
            error = null
        )
        engine.snapshotStore.updateFromCapture(lateLocalAgentSnap, activeSession)

        // Late callback must be discarded!
        assertEquals("com.android.chrome", engine.getDisplayedSnapshot()?.packageName)
        assertEquals("com.android.chrome", engine.getLastExternalSnapshot()?.packageName)
    }

    @Test
    fun testDisplayedSnapshotUsesStoredPackage() {
        val snap = ObservationSnapshot(
            timestampMs = System.currentTimeMillis(),
            packageName = "com.google.android.youtube",
            activityName = "WatchActivity",
            windowType = null,
            rootBounds = ObservationBounds(0, 0, 1080, 1920),
            nodeCount = 300,
            rootNode = null,
            allNodesList = emptyList(),
            state = ObservationState.SUCCESS,
            error = null
        )

        val store = com.agent.android.observation.ObservationSnapshotStore()
        store.setExplicitDisplayedSnapshot(snap)

        assertEquals("com.google.android.youtube", store.displayedSnapshot?.packageName)
        assertEquals("com.google.android.youtube", store.lastValidExternalSnapshot?.packageName)
    }

    @Test
    fun testCardBAndObservationFoundationShareCanonicalSnapshot() {
        val sharedStore = com.agent.android.observation.ObservationSnapshotStore()
        val engine = AccessibilityObservationEngine(sharedStore)

        val externalSnap = ObservationSnapshot(
            timestampMs = System.currentTimeMillis(),
            packageName = "com.android.calculator2",
            activityName = "Calculator",
            windowType = null,
            rootBounds = ObservationBounds(0, 0, 1080, 1920),
            nodeCount = 42,
            rootNode = null,
            allNodesList = emptyList(),
            state = ObservationState.SUCCESS,
            error = null
        )

        sharedStore.updateFromCapture(externalSnap)

        assertEquals(sharedStore.displayedSnapshot, engine.getDisplayedSnapshot())
        assertEquals("com.android.calculator2", engine.getDisplayedSnapshot()?.packageName)
    }

    @Test
    fun testRepeatedStartStopProducesDeterministicState() {
        val engine = AccessibilityObservationEngine()

        for (i in 1..5) {
            engine.startObservationMode()
            assertEquals(com.agent.android.observation.ObservationMode.OBSERVING, engine.observationMode)

            engine.stopObservationMode()
            assertEquals(com.agent.android.observation.ObservationMode.STOPPED, engine.observationMode)
        }
    }
}
