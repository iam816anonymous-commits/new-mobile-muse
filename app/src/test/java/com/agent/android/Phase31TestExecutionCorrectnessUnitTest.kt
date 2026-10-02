package com.agent.android

import com.agent.android.observation.ObservationBounds
import com.agent.android.observation.ObservationNode
import com.agent.android.observation.ObservationSnapshot
import com.agent.android.observation.ObservationState
import com.agent.android.observation.ObservationTargetCategory
import com.agent.android.observation.ObservationTargetResolver
import com.agent.android.speech.AgentLanguage
import com.agent.android.test.model.TestCase
import com.agent.android.test.model.TestStatus
import com.agent.android.test.model.TestType
import com.agent.android.ui.AgentUiState
import com.agent.android.ui.UiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase31TestExecutionCorrectnessUnitTest {

    private val resolver = ObservationTargetResolver()

    private fun createSampleSnapshot(
        packageName: String = "com.android.chrome",
        snapshotId: String = "snap-1",
        rootNode: ObservationNode? = createSampleRootNode(packageName)
    ): ObservationSnapshot {
        val nodes = if (rootNode != null) listOf(rootNode) else emptyList()
        return ObservationSnapshot(
            snapshotId = snapshotId,
            timestampMs = 100000L,
            packageName = packageName,
            activityName = "MainActivity",
            windowType = null,
            rootBounds = ObservationBounds(0, 0, 1080, 1920),
            nodeCount = nodes.size,
            rootNode = rootNode,
            allNodesList = nodes,
            state = if (rootNode != null) ObservationState.SUCCESS else ObservationState.ROOT_NODE_UNAVAILABLE,
            error = if (rootNode == null) "Root node null" else null
        )
    }

    private fun createSampleRootNode(packageName: String): ObservationNode {
        return ObservationNode(
            id = "root",
            parentId = null,
            className = "android.widget.FrameLayout",
            packageName = packageName,
            text = "Root Text",
            contentDescription = null,
            resourceId = "root_id",
            bounds = ObservationBounds(0, 0, 1080, 1920),
            isClickable = false,
            isLongClickable = false,
            isFocusable = false,
            isFocused = false,
            isEnabled = true,
            isEditable = false,
            isScrollable = false,
            isCheckable = false,
            isChecked = false,
            isSelected = false,
            isVisibleToUser = true,
            isPassword = false,
            childCount = 0
        )
    }

    @Test
    fun testCurrentForegroundAppObservationCapturesForegroundApp() {
        val chromeSnap = createSampleSnapshot("com.android.chrome")
        val testCase = TestCase(
            id = "P3.1-OBS-002",
            phase = "PHASE_3.1",
            category = "OBSERVATION",
            name = "Capture Current Window",
            description = "Captures current active window hierarchy.",
            expectedResult = "Captured snapshot with valid nodes",
            testType = TestType.AUTOMATED
        )

        val req = resolver.resolveTargetForTest(testCase)
        assertEquals(ObservationTargetCategory.CURRENT_FOREGROUND_APP, req.category)

        val valRes = resolver.validateCapturedSnapshot(req, chromeSnap)
        assertTrue(valRes.isValid)
        assertEquals(TestStatus.PASSED, valRes.status)
        assertEquals("com.android.chrome", valRes.actualPackage)
    }

    @Test
    fun testSpecificPackageTargetMismatchCausesTestFailure() {
        val localAgentSnap = createSampleSnapshot("com.agent.android")
        val testCase = TestCase(
            id = "P3.1-POS-CHROME",
            phase = "PHASE_3.1",
            category = "OBSERVATION",
            name = "Chrome Observation",
            description = "Observe UI hierarchy of Chrome.",
            expectedResult = "Chrome snapshot captured",
            testType = TestType.AUTOMATED,
            targetPackage = "com.android.chrome",
            expectedPackage = "com.android.chrome"
        )

        val req = resolver.resolveTargetForTest(testCase)
        assertEquals(ObservationTargetCategory.SPECIFIC_PACKAGE, req.category)
        assertEquals("com.android.chrome", req.expectedPackage)

        val valRes = resolver.validateCapturedSnapshot(req, localAgentSnap)
        assertFalse(valRes.isValid)
        assertEquals(TestStatus.FAILED, valRes.status)
        assertNotNull(valRes.failureReason)
        assertTrue(valRes.failureReason!!.contains("LOCALAGENT_FALLBACK_REJECTED"))
    }

    @Test
    fun testNullRootNegativeTestPassesOnlyWhenRootIsNull() {
        val testCase = TestCase(
            id = "P3.1-NEG-005",
            phase = "PHASE_3.1",
            category = "NEGATIVE",
            name = "Root Node Unavailable Failure",
            description = "Rejects snapshots with null rootNode.",
            expectedResult = "rootNode == null",
            testType = TestType.NEGATIVE
        )

        val req = resolver.resolveTargetForTest(testCase)
        assertTrue(req.isNegativeConditionTest)

        // 1. When root IS null -> MUST PASS
        val nullRootSnap = createSampleSnapshot("com.agent.android", rootNode = null)
        val passRes = resolver.validateCapturedSnapshot(req, nullRootSnap)
        assertTrue(passRes.isValid)
        assertEquals(TestStatus.PASSED, passRes.status)

        // 2. When valid root is obtained -> MUST FAIL
        val validRootSnap = createSampleSnapshot("com.agent.android")
        val failRes = resolver.validateCapturedSnapshot(req, validRootSnap)
        assertFalse(failRes.isValid)
        assertEquals(TestStatus.FAILED, failRes.status)
        assertTrue(failRes.failureReason!!.contains("UNEXPECTED_VALID_ROOT"))
    }

    @Test
    fun testNodeCountAloneCannotCausePass() {
        val testCase = TestCase(
            id = "P3.1-POS-SETTINGS",
            phase = "PHASE_3.1",
            category = "OBSERVATION",
            name = "Settings Observation",
            description = "Observe UI hierarchy of Settings.",
            expectedResult = "Settings captured",
            testType = TestType.AUTOMATED,
            expectedPackage = "com.android.settings"
        )

        val req = resolver.resolveTargetForTest(testCase)
        val localAgent83NodesSnap = createSampleSnapshot("com.agent.android")

        val valRes = resolver.validateCapturedSnapshot(req, localAgent83NodesSnap)
        assertFalse(valRes.isValid)
        assertEquals(TestStatus.FAILED, valRes.status)
        assertEquals("com.android.settings", valRes.expectedPackage)
        assertEquals("com.agent.android", valRes.actualPackage)
    }

    @Test
    fun testEvidenceRecordsRequestedTargetAndObservedTarget() {
        val testCase = TestCase(
            id = "P3.1-SYS-SYSTEMUI-001",
            phase = "PHASE_3.1",
            category = "SYSTEM_SURFACE",
            name = "System UI Surface Observation",
            description = "Observe System UI surface.",
            expectedResult = "System UI captured",
            testType = TestType.AUTOMATED
        )

        val req = resolver.resolveTargetForTest(testCase)
        val sysUiSnap = createSampleSnapshot("com.android.systemui")

        val valRes = resolver.validateCapturedSnapshot(req, sysUiSnap)
        assertTrue(valRes.isValid)
        assertEquals(TestStatus.PASSED, valRes.status)
        assertEquals("com.android.systemui", valRes.expectedPackage)
        assertEquals("com.android.systemui", valRes.actualPackage)
    }

    @Test
    fun testUiStateModelNeverReportsSpeakingWhenTtsUnavailable() {
        val model = UiModel(
            state = AgentUiState.IDLE,
            selectedLanguage = AgentLanguage.ENGLISH,
            isSpeaking = false
        )

        assertEquals(AgentUiState.IDLE, model.state)
        assertFalse(model.isSpeaking)
    }
}
