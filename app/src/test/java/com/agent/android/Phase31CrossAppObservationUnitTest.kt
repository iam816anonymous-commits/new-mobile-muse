package com.agent.android

import com.agent.android.observation.AccessibilityObservationEngine
import com.agent.android.observation.ObservationMode
import com.agent.android.test.FoundationTestRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase31CrossAppObservationUnitTest {

    @Test
    fun testObservationEngineModeTransitions() {
        val engine = AccessibilityObservationEngine()
        assertEquals(ObservationMode.READY, engine.observationMode)

        engine.startObservationMode()
        assertEquals(ObservationMode.OBSERVING, engine.observationMode)

        engine.stopObservationMode()
        assertEquals(ObservationMode.STOPPED, engine.observationMode)

        engine.resetToReadyMode()
        assertEquals(ObservationMode.READY, engine.observationMode)
    }

    @Test
    fun testCrossAppTestRegistryIntegrity() {
        val registry = FoundationTestRegistry()
        val xappTests = registry.getAllTestCases().filter { it.id.startsWith("P3.1-XAPP") }

        assertEquals("Must contain 8 cross-app validation tests", 8, xappTests.size)

        val xappIds = xappTests.map { it.id }
        assertEquals("No duplicate XAPP test IDs permitted", xappIds.size, xappIds.toSet().size)

        for (tc in xappTests) {
            assertEquals("PHASE_3.1", tc.phase)
            assertEquals("CROSS_APP", tc.category)
            assertTrue("XAPP tests require physical/manual verification", tc.requiresPhysicalVerification)
        }
    }

    @Test
    fun testObservationEventDebounceSafeguardWhenStopped() {
        val engine = AccessibilityObservationEngine()
        engine.stopObservationMode()

        engine.handleAccessibilityEvent("com.android.settings", 32)
        assertNull(engine.getLastExternalSnapshot())
    }
}
