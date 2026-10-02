package com.agent.android

import com.agent.android.agent.device.CapabilityRegistry
import com.agent.android.agent.device.CapabilityStatus
import com.agent.android.diagnostics.FoundationReadinessEvaluator
import com.agent.android.execution.ExecutionController
import com.agent.android.speech.SpeechToTextEngine
import com.agent.android.speech.TextToSpeechEngine
import com.agent.android.test.FoundationTestRegistry
import com.agent.android.test.evidence.EvidenceManager
import com.agent.android.test.model.TestCase
import com.agent.android.test.model.TestStatus
import com.agent.android.test.model.TestType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase24FoundationUnitTest {

    @Test
    fun testTestCaseModelAndStatusTransitions() {
        val testCase = TestCase(
            id = "TEST-01",
            phase = "PHASE_2.4",
            category = "UNIT_TEST",
            name = "Sample Test",
            description = "Tests status transitions",
            command = "calculate 1 + 1",
            expectedResult = "Result: 2",
            testType = TestType.AUTOMATED
        )

        assertEquals("TEST-01", testCase.id)
        assertEquals(TestStatus.PENDING, testCase.status)

        testCase.status = TestStatus.RUNNING
        assertEquals(TestStatus.RUNNING, testCase.status)

        testCase.status = TestStatus.PASSED
        testCase.observedResult = "Result: 2"
        testCase.duration = 15L
        assertEquals(TestStatus.PASSED, testCase.status)
        assertEquals("Result: 2", testCase.observedResult)
        assertEquals(15L, testCase.duration)
    }

    @Test
    fun testFoundationTestRegistryPopulation() {
        val registry = FoundationTestRegistry()
        val allCases = registry.getAllTestCases()

        assertTrue("Registry must contain at least 35 test cases", allCases.size >= 35)

        val safetyCases = registry.getTestCasesByCategory("SAFETY")
        assertTrue("Safety category test cases must be present", safetyCases.isNotEmpty())

        val mathCase = registry.getTestCaseById("2.1.01")
        assertNotNull("Calculate integer test case must exist", mathCase)
        assertEquals("calculate 12 + 34", mathCase?.command)

        val summaryBefore = registry.getSummary()
        assertEquals(allCases.size, summaryBefore.total)
        assertEquals(allCases.size, summaryBefore.pending)

        registry.updateTestCase(
            id = "2.1.01",
            status = TestStatus.PASSED,
            observedResult = "Result: 46",
            error = null,
            duration = 10L
        )

        val summaryAfter = registry.getSummary()
        assertEquals(1, summaryAfter.passed)
        assertEquals(allCases.size - 1, summaryAfter.pending)

        registry.clearAllResults()
        val summaryCleared = registry.getSummary()
        assertEquals(0, summaryCleared.passed)
        assertEquals(allCases.size, summaryCleared.pending)
    }

    @Test
    fun testEvidenceManagerEscapingAndFormat() {
        val manager = EvidenceManager(android.content.ContextWrapper(null))

        val dummyCase = TestCase(
            id = "9.9.01",
            phase = "PHASE_2.4",
            category = "TEST",
            name = "Test \"Escaping\" \nNewline",
            description = "Desc",
            command = "test",
            expectedResult = "OK",
            testType = TestType.AUTOMATED,
            status = TestStatus.PASSED,
            observedResult = "Observed \"quotes\"",
            error = "None",
            duration = 5L
        )

        val path = manager.saveTestResultJson(dummyCase)
        assertNotNull(path)
    }

    @Test
    fun testFlashlightControllerMultiTorchAndDiagnostics() {
        val nullController = com.agent.android.agent.device.FlashlightController(null)

        val nullDiag = nullController.getTorchDiagnostic()
        assertFalse(nullDiag.capabilityExists)
        assertFalse(nullDiag.capabilityPermitted)
        assertFalse(nullDiag.capabilityUsable)
        assertTrue(nullDiag.summaryText.contains("EXISTS=false"))

        val statusRes = nullController.setFlashlightTarget("status")
        assertEquals(com.agent.android.agent.skills.SkillStatus.SUCCESS, statusRes.status)

        val invalidRes = nullController.setFlashlightTarget("invalid_target")
        assertEquals(com.agent.android.agent.skills.SkillStatus.FAILED, invalidRes.status)
        assertEquals("INVALID_TARGET", invalidRes.errorCode)

        val frontResNull = nullController.setFlashlightTarget("front")
        assertEquals("NO_CONTEXT", frontResNull.errorCode)

        // Mock/Fake Provider with Dual Torches (Back: "0", Front: "1")
        val fakeProvider = object : com.agent.android.agent.device.TorchHardwareProvider {
            val activeTorches = mutableSetOf<String>()
            override fun getTorchDiagnostic(): com.agent.android.agent.device.TorchMappingDiagnostic {
                return com.agent.android.agent.device.TorchMappingDiagnostic(
                    backCameraIds = listOf("0"),
                    frontCameraIds = listOf("1"),
                    otherCameraIds = emptyList(),
                    capabilityExists = true,
                    capabilityPermitted = true,
                    capabilityUsable = true
                )
            }

            override fun setTorchMode(cameraId: String, enabled: Boolean) {
                if (enabled) activeTorches.add(cameraId) else activeTorches.remove(cameraId)
            }
        }

        val dummyContext = android.content.ContextWrapper(null)
        val multiTorchController = com.agent.android.agent.device.FlashlightController(dummyContext, fakeProvider)

        val dualDiag = multiTorchController.getTorchDiagnostic()
        assertTrue(dualDiag.capabilityExists)
        assertTrue(dualDiag.capabilityPermitted)
        assertTrue(dualDiag.capabilityUsable)
        assertEquals(listOf("0"), dualDiag.backCameraIds)
        assertEquals(listOf("1"), dualDiag.frontCameraIds)

        val backRes = multiTorchController.setFlashlightTarget("back", true)
        assertEquals(com.agent.android.agent.skills.SkillStatus.SUCCESS, backRes.status)
        assertTrue(fakeProvider.activeTorches.contains("0"))

        val frontRes = multiTorchController.setFlashlightTarget("front", true)
        assertEquals(com.agent.android.agent.skills.SkillStatus.SUCCESS, frontRes.status)
        assertTrue(fakeProvider.activeTorches.contains("1"))

        val offRes = multiTorchController.setFlashlightTarget("off", false)
        assertEquals(com.agent.android.agent.skills.SkillStatus.SUCCESS, offRes.status)
        assertTrue(fakeProvider.activeTorches.isEmpty())

        val bothRes = multiTorchController.setFlashlightTarget("both", true)
        assertEquals(com.agent.android.agent.skills.SkillStatus.SUCCESS, bothRes.status)
        assertTrue(fakeProvider.activeTorches.contains("0") && fakeProvider.activeTorches.contains("1"))
    }

    @Test
    fun testEnhancedCapabilityRegistry() {
        val registry = CapabilityRegistry(null)
        val detailed = registry.checkDetailedCapabilities()

        assertNotNull(detailed["FLASHLIGHT"])
        assertNotNull(detailed["VIBRATION"])
        assertNotNull(detailed["VOLUME"])
        assertNotNull(detailed["STT"])
        assertNotNull(detailed["TTS"])

        val flashlightInfo = detailed["FLASHLIGHT"]
        assertNotNull(flashlightInfo?.capabilityExists)
        assertNotNull(flashlightInfo?.capabilityPermitted)
        assertNotNull(flashlightInfo?.capabilityUsable)
    }

    @Test
    fun testFoundationReadinessEvaluatorWithPendingTests() {
        val controller = ExecutionController()
        val registry = CapabilityRegistry(null)
        val testRegistry = FoundationTestRegistry()

        val evaluator = FoundationReadinessEvaluator(
            context = android.content.ContextWrapper(null),
            executionController = controller,
            capabilityRegistry = registry,
            testRegistry = testRegistry
        )

        val report = evaluator.evaluate()
        assertEquals("NOT_READY", report.statusText)
        assertFalse("Report must not be ready when tests are pending", report.isReady)
        assertTrue("Blocking reasons must contain pending test info", report.blockingReasons.any { it.contains("TESTS") })
    }

    @Test
    fun testSpeechToTextEngineAvailabilityCheck() {
        val sttEngine = SpeechToTextEngine(android.content.ContextWrapper(null))
        assertFalse("STT permission should be false when context is null", sttEngine.hasRecordAudioPermission())
    }

    @Test
    fun testTextToSpeechEngineAvailabilityCheck() {
        val ttsEngine = TextToSpeechEngine(android.content.ContextWrapper(null))
        assertFalse("TTS engine should not be available before initialization", ttsEngine.isAvailable())
    }
}
