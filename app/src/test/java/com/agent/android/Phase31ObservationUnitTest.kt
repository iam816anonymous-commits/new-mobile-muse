package com.agent.android

import com.agent.android.observation.AccessibilityObservationEngine
import com.agent.android.observation.ObservationBounds
import com.agent.android.observation.ObservationNode
import com.agent.android.observation.ObservationSnapshot
import com.agent.android.observation.ObservationState
import com.agent.android.test.FoundationTestRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase31ObservationUnitTest {

    @Test
    fun testObservationNodeJsonSerialization() {
        val bounds = ObservationBounds(10, 20, 100, 200)
        val child = ObservationNode(
            id = "node-2",
            parentId = "node-1",
            className = "android.widget.TextView",
            packageName = "com.agent.android",
            text = "Submit",
            contentDescription = "Submit Button",
            resourceId = "com.agent.android:id/btnSubmit",
            bounds = ObservationBounds(10, 50, 100, 90),
            isClickable = true,
            isLongClickable = false,
            isFocusable = true,
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

        val rootNode = ObservationNode(
            id = "node-1",
            parentId = null,
            className = "android.widget.LinearLayout",
            packageName = "com.agent.android",
            text = null,
            contentDescription = null,
            resourceId = null,
            bounds = bounds,
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
            childCount = 1,
            children = listOf(child)
        )

        val jsonObj = rootNode.toJsonObject()
        val parsedNode = ObservationNode.fromJsonObject(jsonObj)

        assertEquals("node-1", parsedNode.id)
        assertEquals("android.widget.LinearLayout", parsedNode.className)
        assertEquals(1, parsedNode.children.size)

        val parsedChild = parsedNode.children[0]
        assertEquals("node-2", parsedChild.id)
        assertEquals("Submit", parsedChild.text)
        assertEquals("com.agent.android:id/btnSubmit", parsedChild.resourceId)
        assertTrue(parsedChild.isClickable)
    }

    @Test
    fun testObservationSnapshotJsonSerialization() {
        val bounds = ObservationBounds(0, 0, 1080, 1920)
        val snapshot = ObservationSnapshot(
            timestampMs = 1721510200000L,
            packageName = "com.android.settings",
            activityName = "com.android.settings.Settings",
            windowType = null,
            rootBounds = bounds,
            nodeCount = 5,
            rootNode = null,
            allNodesList = emptyList(),
            state = ObservationState.SUCCESS,
            error = null
        )

        val jsonStr = snapshot.toJsonString()
        val deserialized = ObservationSnapshot.fromJsonString(jsonStr)

        assertEquals(1721510200000L, deserialized.timestampMs)
        assertEquals("com.android.settings", deserialized.packageName)
        assertEquals("com.android.settings.Settings", deserialized.activityName)
        assertEquals(ObservationState.SUCCESS, deserialized.state)
    }

    @Test
    fun testAccessibilityObservationEngineNullServiceSafeguard() {
        val engine = AccessibilityObservationEngine()
        val metadata = engine.getObservationMetadata()

        assertEquals(false, metadata.isServiceConnected)
        assertEquals(false, metadata.isRootAvailable)

        val snapshot = engine.captureCurrentScreen()
        assertEquals(ObservationState.ACCESSIBILITY_DISABLED, snapshot.state)
        assertNotNull(snapshot.error)
    }

    @Test
    fun testPhase31TestRegistryIsolation() {
        val testRegistry = FoundationTestRegistry()

        val p2Tests = testRegistry.getTestCasesByPhase("PHASE_2")
        assertTrue("Phase 2 test suite must contain tests", p2Tests.isNotEmpty())

        val p31CoreTests = testRegistry.getTestCasesByPhase("PHASE_3.1").filter { it.id.startsWith("P3.1-OBS-") }
        assertEquals("Phase 3.1 core suite must contain 25 observation tests", 25, p31CoreTests.size)

        val p31XappTests = testRegistry.getTestCasesByPhase("PHASE_3.1").filter { it.id.startsWith("P3.1-XAPP-") }
        assertEquals("Phase 3.1 XAPP suite must contain 8 cross-app validation tests", 8, p31XappTests.size)

        for (tc in p31CoreTests) {
            assertTrue("Phase 3.1 core test IDs must start with P3.1-OBS-", tc.id.startsWith("P3.1-OBS-"))
            assertEquals("PHASE_3.1", tc.phase)
        }
    }

    @Test
    fun testObservationEngineConstants() {
        assertEquals(500, AccessibilityObservationEngine.MAX_NODE_LIMIT)
        assertEquals(30, AccessibilityObservationEngine.MAX_DEPTH_LIMIT)
    }
}
