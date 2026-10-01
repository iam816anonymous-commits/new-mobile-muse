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
}
