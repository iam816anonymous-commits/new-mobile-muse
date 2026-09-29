package com.agent.android.agent.skills

import com.agent.android.agent.device.ConnectivityControllers
import com.agent.android.agent.device.FlashlightController
import com.agent.android.agent.device.HapticController
import com.agent.android.agent.device.HardwareObservationControllers
import com.agent.android.agent.device.VolumeController
import com.agent.android.execution.ExecutionController
import com.agent.android.safety.CancellationReason
import com.agent.android.safety.HarnessSuiteSummary
import com.agent.android.safety.SafetyTestResult

class Phase2HeadlessTestHarness(
    private val executionController: ExecutionController = ExecutionController(),
    private val calculatorSkill: CalculatorSkill = CalculatorSkill(),
    private val notesSkill: NotesSkill = NotesSkill()
) {

    fun testCalculatorBasic(): SafetyTestResult {
        val start = System.currentTimeMillis()
        val res = calculatorSkill.calculate("2 + 3 * 4")
        val passed = res.status == SkillStatus.SUCCESS && res.message == "14"
        return SafetyTestResult("Calculator Precedence (2+3*4)", passed, "14", res.message, System.currentTimeMillis() - start)
    }

    fun testCalculatorParentheses(): SafetyTestResult {
        val start = System.currentTimeMillis()
        val res = calculatorSkill.calculate("(2 + 3) * 4")
        val passed = res.status == SkillStatus.SUCCESS && res.message == "20"
        return SafetyTestResult("Calculator Parentheses ((2+3)*4)", passed, "20", res.message, System.currentTimeMillis() - start)
    }

    fun testCalculatorDivisionByZero(): SafetyTestResult {
        val start = System.currentTimeMillis()
        val res = calculatorSkill.calculate("10 / 0")
        val passed = res.status == SkillStatus.FAILED && res.errorCode == "DIVISION_BY_ZERO"
        return SafetyTestResult("Calculator Division By Zero", passed, "DIVISION_BY_ZERO error", res.message, System.currentTimeMillis() - start)
    }

    fun testNotesEmptyContent(): SafetyTestResult {
        val start = System.currentTimeMillis()
        val res = notesSkill.addNote("")
        val passed = res.status == SkillStatus.FAILED && res.errorCode == "EMPTY_NOTE"
        return SafetyTestResult("Notes Empty Content Validation", passed, "EMPTY_NOTE error", res.message, System.currentTimeMillis() - start)
    }

    fun runAllHeadlessTests(): HarnessSuiteSummary {
        val results = listOf(
            testCalculatorBasic(),
            testCalculatorParentheses(),
            testCalculatorDivisionByZero(),
            testNotesEmptyContent()
        )
        val passed = results.count { it.passed }
        val failed = results.count { !it.passed }
        return HarnessSuiteSummary(passed, failed, results.size, failed == 0, results)
    }
}

class Phase2DeviceTestHarness(
    private val flashlightController: FlashlightController = FlashlightController(null),
    private val hapticController: HapticController = HapticController(null),
    private val volumeController: VolumeController = VolumeController(null),
    private val connectivityControllers: ConnectivityControllers = ConnectivityControllers(null),
    private val hardwareObservationControllers: HardwareObservationControllers = HardwareObservationControllers(null)
) {

    fun testFlashlightNoContext(): SafetyTestResult {
        val start = System.currentTimeMillis()
        val res = flashlightController.setFlashlight(true)
        val passed = res.status == SkillStatus.UNAVAILABLE && res.errorCode == "NO_CONTEXT"
        return SafetyTestResult("Flashlight Context Safeguard", passed, "UNAVAILABLE (NO_CONTEXT)", res.message, System.currentTimeMillis() - start)
    }

    fun testHapticsNoContext(): SafetyTestResult {
        val start = System.currentTimeMillis()
        val res = hapticController.vibrate(300)
        val passed = res.status == SkillStatus.UNAVAILABLE && res.errorCode == "NO_CONTEXT"
        return SafetyTestResult("Haptic Context Safeguard", passed, "UNAVAILABLE (NO_CONTEXT)", res.message, System.currentTimeMillis() - start)
    }

    fun testVolumeNoContext(): SafetyTestResult {
        val start = System.currentTimeMillis()
        val res = volumeController.getVolume()
        val passed = res.status == SkillStatus.UNAVAILABLE
        return SafetyTestResult("Volume Context Safeguard", passed, "UNAVAILABLE", res.message, System.currentTimeMillis() - start)
    }

    fun runAllDeviceTests(): HarnessSuiteSummary {
        val results = listOf(
            testFlashlightNoContext(),
            testHapticsNoContext(),
            testVolumeNoContext()
        )
        val passed = results.count { it.passed }
        val failed = results.count { !it.passed }
        return HarnessSuiteSummary(passed, failed, results.size, failed == 0, results)
    }
}
