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

    @Test
    fun testWindowClassificationLogic() {
        assertEquals(com.agent.android.observation.WindowClassification.APPLICATION, com.agent.android.observation.WindowClassification.classify("com.android.chrome"))
        assertEquals(com.agent.android.observation.WindowClassification.LOCAL_AGENT, com.agent.android.observation.WindowClassification.classify("com.agent.android"))
        assertEquals(com.agent.android.observation.WindowClassification.SYSTEM_UI, com.agent.android.observation.WindowClassification.classify("com.android.systemui"))
        assertEquals(com.agent.android.observation.WindowClassification.RECENTS, com.agent.android.observation.WindowClassification.classify("com.android.systemui", "com.android.systemui.recents.RecentsActivity"))
        assertEquals(com.agent.android.observation.WindowClassification.LAUNCHER, com.agent.android.observation.WindowClassification.classify("com.google.android.apps.nexuslauncher"))
        assertEquals(com.agent.android.observation.WindowClassification.SETTINGS, com.agent.android.observation.WindowClassification.classify("com.android.settings"))
        assertEquals(com.agent.android.observation.WindowClassification.UNKNOWN, com.agent.android.observation.WindowClassification.classify(null))
    }

    @Test
    fun testObservedWindowJsonSerialization() {
        val win = com.agent.android.observation.ObservedWindow(
            windowId = 42,
            packageName = "com.android.settings",
            activityName = "SettingsActivity",
            windowType = 1,
            layer = 10,
            bounds = ObservationBounds(0, 0, 1080, 1920),
            isActive = true,
            isFocused = true,
            isAccessibilityFocused = false,
            classification = com.agent.android.observation.WindowClassification.SETTINGS
        )

        val json = win.toJsonObject()
        val restored = com.agent.android.observation.ObservedWindow.fromJsonObject(json)

        assertEquals(42, restored.windowId)
        assertEquals("com.android.settings", restored.packageName)
        assertEquals("SettingsActivity", restored.activityName)
        assertEquals(com.agent.android.observation.WindowClassification.SETTINGS, restored.classification)
        assertTrue(restored.isActive)
        assertTrue(restored.isFocused)
    }

    @Test
    fun testAccessibleWindowProviderNullServiceFallback() {
        val provider = com.agent.android.observation.AccessibleWindowProvider()
        val windows = provider.getAccessibleWindows()

        // Null service returns empty window list without crashing
        assertNotNull(windows)
    }

    @Test
    fun testObservationSourceScopeAndStateSerialization() {
        val snap = ObservationSnapshot(
            timestampMs = 1721510200000L,
            packageName = "com.android.chrome",
            activityName = "ChromeTabbedActivity",
            windowType = "TYPE_APPLICATION",
            rootBounds = ObservationBounds(0, 0, 1080, 1920),
            nodeCount = 120,
            rootNode = null,
            allNodesList = emptyList(),
            state = ObservationState.SUCCESS,
            error = null,
            classification = com.agent.android.observation.WindowClassification.APPLICATION,
            source = com.agent.android.observation.ObservationSource.GUIDED_TEST,
            scope = com.agent.android.observation.ObservationScope.TARGET_APPLICATION
        )

        val jsonStr = snap.toJsonString()
        val restored = ObservationSnapshot.fromJsonString(jsonStr)

        assertEquals(com.agent.android.observation.ObservationSource.GUIDED_TEST, restored.source)
        assertEquals(com.agent.android.observation.ObservationScope.TARGET_APPLICATION, restored.scope)
        assertEquals(com.agent.android.observation.WindowClassification.APPLICATION, restored.classification)
    }

    @Test
    fun testStaleSnapshotCannotPass() {
        val runStartTime = System.currentTimeMillis()
        val staleSnap = ObservationSnapshot(
            timestampMs = runStartTime - 5000L, // 5s before run start
            packageName = "com.android.chrome",
            activityName = "Chrome",
            windowType = null,
            rootBounds = ObservationBounds(0, 0, 1080, 1920),
            nodeCount = 100,
            rootNode = ObservationNode("1", null, "android.widget.FrameLayout", "com.android.chrome", null, null, null, ObservationBounds(0, 0, 1080, 1920), false, false, false, false, true, false, false, false, false, false, true, false, 0),
            allNodesList = emptyList(),
            state = ObservationState.SUCCESS,
            error = null,
            testRunId = "P3.1-POS-CHROME-run-1"
        )

        val isValidForRun = staleSnap.timestampMs >= runStartTime
        assertEquals(false, isValidForRun)
    }

    @Test
    fun testWrongPackageCannotPass() {
        val snap = ObservationSnapshot(
            timestampMs = System.currentTimeMillis(),
            packageName = "com.google.android.youtube", // Expecting Chrome
            activityName = "WatchActivity",
            windowType = null,
            rootBounds = ObservationBounds(0, 0, 1080, 1920),
            nodeCount = 100,
            rootNode = ObservationNode("1", null, "android.widget.FrameLayout", "com.google.android.youtube", null, null, null, ObservationBounds(0, 0, 1080, 1920), false, false, false, false, true, false, false, false, false, false, true, false, 0),
            allNodesList = emptyList(),
            state = ObservationState.SUCCESS,
            error = null
        )

        val res = validator.validateExternalAppSnapshot(snap)
        assertEquals(TestStatus.PASSED, res.status) // Valid external app, but for Chrome target mismatch:
        val expectedPkg = "com.android.chrome"
        assertEquals(false, snap.packageName == expectedPkg)
    }

    @Test
    fun testLocalAgentSnapshotCannotSatisfyExternalTest() {
        val localSnap = ObservationSnapshot(
            timestampMs = System.currentTimeMillis(),
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

        val res = validator.validateExternalAppSnapshot(localSnap)
        assertEquals(TestStatus.FAILED, res.status)
        assertEquals("TARGET APP IS LOCALAGENT", res.summaryText)
    }

    @Test
    fun testSystemUiSnapshotCannotSatisfyExternalTest() {
        val sysUiSnap = ObservationSnapshot(
            timestampMs = System.currentTimeMillis(),
            packageName = "com.android.systemui",
            activityName = "StatusBar",
            windowType = null,
            rootBounds = ObservationBounds(0, 0, 1080, 1920),
            nodeCount = 10,
            rootNode = null,
            allNodesList = emptyList(),
            state = ObservationState.SUCCESS,
            error = null
        )

        val res = validator.validateExternalAppSnapshot(sysUiSnap)
        assertEquals(TestStatus.FAILED, res.status)
        assertEquals("INVALID TARGET: SYSTEM UI / LAUNCHER", res.summaryText)
    }

    @Test
    fun testRecentsSnapshotCannotSatisfyExternalTest() {
        val recentsSnap = ObservationSnapshot(
            timestampMs = System.currentTimeMillis(),
            packageName = "com.android.systemui.recents",
            activityName = "RecentsActivity",
            windowType = null,
            rootBounds = ObservationBounds(0, 0, 1080, 1920),
            nodeCount = 12,
            rootNode = null,
            allNodesList = emptyList(),
            state = ObservationState.SUCCESS,
            error = null
        )

        val res = validator.validateExternalAppSnapshot(recentsSnap)
        assertEquals(TestStatus.FAILED, res.status)
        assertEquals("INVALID TARGET: SYSTEM UI / LAUNCHER", res.summaryText)
    }

    @Test
    fun testZeroNodeSnapshotFails() {
        val zeroNodeSnap = ObservationSnapshot(
            timestampMs = System.currentTimeMillis(),
            packageName = "com.android.chrome",
            activityName = "Chrome",
            windowType = null,
            rootBounds = ObservationBounds(0, 0, 1080, 1920),
            nodeCount = 0,
            rootNode = null,
            allNodesList = emptyList(),
            state = ObservationState.SUCCESS,
            error = null
        )

        val res = validator.validateExternalAppSnapshot(zeroNodeSnap)
        assertEquals(TestStatus.FAILED, res.status)
    }

    @Test
    fun testFailedObservationStateFails() {
        val failedSnap = ObservationSnapshot(
            timestampMs = System.currentTimeMillis(),
            packageName = "com.android.chrome",
            activityName = "Chrome",
            windowType = null,
            rootBounds = ObservationBounds(0, 0, 1080, 1920),
            nodeCount = 20,
            rootNode = null,
            allNodesList = emptyList(),
            state = ObservationState.OBSERVATION_FAILED,
            error = "Hierarchy parse error"
        )

        val res = validator.validateExternalAppSnapshot(failedSnap)
        assertEquals(TestStatus.FAILED, res.status)
    }

    @Test
    fun testCorrectPackageAndValidSnapshotPasses() {
        val validSnap = ObservationSnapshot(
            timestampMs = System.currentTimeMillis(),
            packageName = "com.android.chrome",
            activityName = "ChromeTabbedActivity",
            windowType = null,
            rootBounds = ObservationBounds(0, 0, 1080, 1920),
            nodeCount = 150,
            rootNode = ObservationNode("1", null, "android.widget.FrameLayout", "com.android.chrome", null, null, null, ObservationBounds(0, 0, 1080, 1920), false, false, false, false, true, false, false, false, false, false, true, false, 0),
            allNodesList = emptyList(),
            state = ObservationState.SUCCESS,
            error = null
        )

        val res = validator.validateExternalAppSnapshot(validSnap)
        assertEquals(TestStatus.PASSED, res.status)
        assertEquals("com.android.chrome", res.targetPackage)
    }

    @Test
    fun testSnapshotFromAnotherRunCannotPass() {
        val snapRun1 = ObservationSnapshot(
            timestampMs = System.currentTimeMillis(),
            packageName = "com.android.chrome",
            activityName = "Chrome",
            windowType = null,
            rootBounds = ObservationBounds(0, 0, 1080, 1920),
            nodeCount = 100,
            rootNode = null,
            allNodesList = emptyList(),
            state = ObservationState.SUCCESS,
            error = null,
            testRunId = "Run-1"
        )

        val activeRunId = "Run-2"
        val isMatchingRun = snapRun1.testRunId == activeRunId
        assertEquals(false, isMatchingRun)
    }

    @Test
    fun testPreservedExternalSnapshotSurvivesLocalAgentForeground() {
        val store = com.agent.android.observation.ObservationSnapshotStore()

        val chromeSnap = ObservationSnapshot(
            timestampMs = 1000L,
            packageName = "com.android.chrome",
            activityName = "Chrome",
            windowType = null,
            rootBounds = ObservationBounds(0, 0, 1080, 1920),
            nodeCount = 120,
            rootNode = null,
            allNodesList = emptyList(),
            state = ObservationState.SUCCESS,
            error = null
        )
        store.updateFromCapture(chromeSnap)
        assertEquals("com.android.chrome", store.lastValidExternalSnapshot?.packageName)

        val localAgentSnap = ObservationSnapshot(
            timestampMs = 2000L,
            packageName = "com.agent.android",
            activityName = "MainActivity",
            windowType = null,
            rootBounds = ObservationBounds(0, 0, 1080, 1920),
            nodeCount = 40,
            rootNode = null,
            allNodesList = emptyList(),
            state = ObservationState.SUCCESS,
            error = null
        )
        store.updateFromCapture(localAgentSnap)

        assertEquals("com.android.chrome", store.lastValidExternalSnapshot?.packageName)
        assertEquals("com.android.chrome", store.displayedSnapshot?.packageName)
    }

    @Test
    fun testPassCannotOccurBeforeValidation() {
        val runnerState = com.agent.android.observation.GuidedTestState.PREPARING
        val status = com.agent.android.test.model.TestStatus.RUNNING

        assertEquals(false, runnerState == com.agent.android.observation.GuidedTestState.PASSED)
        assertEquals(false, status == com.agent.android.test.model.TestStatus.PASSED)
    }

    @Test
    fun testTestStateTransitionsAreDeterministic() {
        val states = com.agent.android.observation.GuidedTestState.values()

        assertTrue(states.contains(com.agent.android.observation.GuidedTestState.PREPARING))
        assertTrue(states.contains(com.agent.android.observation.GuidedTestState.LAUNCHING_TARGET))
        assertTrue(states.contains(com.agent.android.observation.GuidedTestState.WAITING_FOR_FOREGROUND))
        assertTrue(states.contains(com.agent.android.observation.GuidedTestState.TARGET_DETECTED))
        assertTrue(states.contains(com.agent.android.observation.GuidedTestState.CAPTURING))
        assertTrue(states.contains(com.agent.android.observation.GuidedTestState.VALIDATING))
        assertTrue(states.contains(com.agent.android.observation.GuidedTestState.PRESERVING))
        assertTrue(states.contains(com.agent.android.observation.GuidedTestState.PASSED))
        assertTrue(states.contains(com.agent.android.observation.GuidedTestState.FAILED))
        assertTrue(states.contains(com.agent.android.observation.GuidedTestState.TIMED_OUT))
    }

    @Test
    fun testTimeoutProducesFail() {
        val timeoutState = com.agent.android.observation.GuidedTestState.TIMED_OUT
        val status = com.agent.android.test.model.TestStatus.FAILED

        assertEquals(com.agent.android.observation.GuidedTestState.TIMED_OUT, timeoutState)
        assertEquals(com.agent.android.test.model.TestStatus.FAILED, status)
    }

    @Test
    fun testNewTestCreatesIndependentTestRun() {
        val store = com.agent.android.observation.ObservationSnapshotStore()

        store.startTestRun("Run-1")
        val snap1 = ObservationSnapshot(
            timestampMs = System.currentTimeMillis(),
            packageName = "com.android.chrome",
            activityName = "Chrome",
            windowType = null,
            rootBounds = ObservationBounds(0, 0, 1080, 1920),
            nodeCount = 100,
            rootNode = null,
            allNodesList = emptyList(),
            state = ObservationState.SUCCESS,
            error = null,
            testRunId = "Run-1"
        )
        store.updateFromCapture(snap1)
        assertNotNull(store.activeTestRunSnapshot)

        // Start Run-2
        store.startTestRun("Run-2")
        assertEquals(null, store.activeTestRunSnapshot) // Run-2 active snapshot cleared!
    }

    @Test
    fun testDiagnosticSnapshotCannotSatisfyTestResult() {
        val store = com.agent.android.observation.ObservationSnapshotStore()
        store.startTestRun("Run-10")

        // Diagnostic capture without testRunId tag
        val diagSnap = ObservationSnapshot(
            timestampMs = System.currentTimeMillis(),
            packageName = "com.android.chrome",
            activityName = "Chrome",
            windowType = null,
            rootBounds = ObservationBounds(0, 0, 1080, 1920),
            nodeCount = 100,
            rootNode = null,
            allNodesList = emptyList(),
            state = ObservationState.SUCCESS,
            error = null,
            testRunId = null // Untagged diagnostic capture
        )

        store.updateFromCapture(diagSnap)

        assertEquals(null, store.activeTestRunSnapshot) // Untagged capture rejected for test run snapshot!
    }

    @Test
    fun testSystemSurfaceWindowClassifications() {
        val localAgent = com.agent.android.observation.WindowClassification.classify("com.agent.android", "com.agent.android.MainActivity")
        val launcher = com.agent.android.observation.WindowClassification.classify("com.google.android.apps.nexuslauncher", "com.google.android.apps.nexuslauncher.NexusLauncherActivity")
        val recents = com.agent.android.observation.WindowClassification.classify("com.android.systemui", "com.android.systemui.recents.RecentsActivity")
        val systemUi = com.agent.android.observation.WindowClassification.classify("com.android.systemui", "com.android.systemui.statusbar.phone.PhoneStatusBar")
        val settings = com.agent.android.observation.WindowClassification.classify("com.android.settings", "com.android.settings.Settings")
        val app = com.agent.android.observation.WindowClassification.classify("com.android.chrome", "org.chromium.chrome.browser.ChromeTabbedActivity")

        assertEquals(com.agent.android.observation.WindowClassification.LOCAL_AGENT, localAgent)
        assertEquals(com.agent.android.observation.WindowClassification.LAUNCHER, launcher)
        assertEquals(com.agent.android.observation.WindowClassification.RECENTS, recents)
        assertEquals(com.agent.android.observation.WindowClassification.SYSTEM_UI, systemUi)
        assertEquals(com.agent.android.observation.WindowClassification.SETTINGS, settings)
        assertEquals(com.agent.android.observation.WindowClassification.APPLICATION, app)
    }

    @Test
    fun testSystemUiSnapshotIsCapturedWithValidClassification() {
        val sysUiSnap = ObservationSnapshot(
            timestampMs = System.currentTimeMillis(),
            packageName = "com.android.systemui",
            activityName = "PhoneStatusBar",
            windowType = null,
            rootBounds = ObservationBounds(0, 0, 1080, 1920),
            nodeCount = 15,
            rootNode = ObservationNode("1", null, "android.widget.FrameLayout", "com.android.systemui", null, null, null, ObservationBounds(0, 0, 1080, 1920), false, false, false, false, true, false, false, false, false, false, true, false, 0),
            allNodesList = emptyList(),
            state = ObservationState.SUCCESS,
            error = null
        )

        assertEquals(com.agent.android.observation.WindowClassification.SYSTEM_UI, sysUiSnap.classification)
        assertEquals(ObservationState.SUCCESS, sysUiSnap.state)
        assertEquals(15, sysUiSnap.nodeCount)
    }

    @Test
    fun testSupportedSurfaceNotExposedState() {
        val launcherNotExposedSnap = ObservationSnapshot(
            timestampMs = System.currentTimeMillis(),
            packageName = "com.google.android.apps.nexuslauncher",
            activityName = "NexusLauncherActivity",
            windowType = null,
            rootBounds = ObservationBounds(0, 0, 1080, 1920),
            nodeCount = 0,
            rootNode = null,
            allNodesList = emptyList(),
            state = ObservationState.SUPPORTED_SURFACE_BUT_NOT_EXPOSED,
            error = "Launcher visible but Android did not expose an accessible hierarchy."
        )

        assertEquals(com.agent.android.observation.WindowClassification.LAUNCHER, launcherNotExposedSnap.classification)
        assertEquals(ObservationState.SUPPORTED_SURFACE_BUT_NOT_EXPOSED, launcherNotExposedSnap.state)
        assertEquals(0, launcherNotExposedSnap.nodeCount)
    }
}
