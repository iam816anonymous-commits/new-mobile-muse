package com.agent.android

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Button
import android.widget.TextView
import com.agent.android.agent.skills.CalculatorSkill
import com.agent.android.agent.skills.IntentSkills
import com.agent.android.agent.skills.NotesSkill
import com.agent.android.agent.skills.Phase2DeviceTestHarness
import com.agent.android.agent.skills.Phase2HeadlessTestHarness
import com.agent.android.execution.ExecutionController
import com.agent.android.execution.GoalDispatcherImpl
import com.agent.android.safety.HarnessSuiteSummary
import com.agent.android.safety.Phase1SafetyTestHarness
import com.agent.android.safety.SafetyTestResult
import com.agent.android.service.LocalAgentAccessibilityService
import com.agent.android.storage.Logger

class MainActivity : Activity() {

    private val executionController = ExecutionController()
    private val logger = Logger()
    private lateinit var testHarness: Phase1SafetyTestHarness

    private lateinit var phase2HeadlessHarness: Phase2HeadlessTestHarness
    private lateinit var phase2DeviceHarness: Phase2DeviceTestHarness
    private lateinit var goalDispatcher: GoalDispatcherImpl

    private lateinit var tvAgentStatus: TextView
    private lateinit var tvExecutionState: TextView
    private lateinit var tvSafetyStatus: TextView
    private lateinit var tvAccessibilityStatus: TextView
    private lateinit var btnEnableAccessibility: Button
    private lateinit var tvSuiteSummary: TextView
    private lateinit var tvPhase2Status: TextView
    private lateinit var tvLogArea: TextView

    private lateinit var btnRunAllTests: Button
    private lateinit var btnClearResults: Button
    private lateinit var btnRunPhase2HeadlessTests: Button
    private lateinit var btnRunPhase2DeviceTests: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        testHarness = Phase1SafetyTestHarness(executionController, logger)
        val calc = CalculatorSkill()
        val notes = NotesSkill(this)
        val intents = IntentSkills(this)
        goalDispatcher = GoalDispatcherImpl(executionController, calc, notes, intents)

        phase2HeadlessHarness = Phase2HeadlessTestHarness(executionController, calc, notes)
        phase2DeviceHarness = Phase2DeviceTestHarness()

        tvAgentStatus = findViewById(R.id.tvAgentStatus)
        tvExecutionState = findViewById(R.id.tvExecutionState)
        tvSafetyStatus = findViewById(R.id.tvSafetyStatus)
        tvAccessibilityStatus = findViewById(R.id.tvAccessibilityStatus)
        btnEnableAccessibility = findViewById(R.id.btnEnableAccessibility)
        tvSuiteSummary = findViewById(R.id.tvSuiteSummary)
        tvPhase2Status = findViewById(R.id.tvPhase2Status)
        tvLogArea = findViewById(R.id.tvLogArea)

        btnRunAllTests = findViewById(R.id.btnRunAllTests)
        btnClearResults = findViewById(R.id.btnClearResults)
        btnRunPhase2HeadlessTests = findViewById(R.id.btnRunPhase2HeadlessTests)
        btnRunPhase2DeviceTests = findViewById(R.id.btnRunPhase2DeviceTests)

        btnEnableAccessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        btnRunAllTests.setOnClickListener {
            val summary = testHarness.runAllTests()
            updateSuiteSummary(summary)
        }

        btnClearResults.setOnClickListener {
            tvSuiteSummary.text = "Suite Status: NOT RUN (Passed: 0, Failed: 0, Total: 0)"
            tvPhase2Status.text = "Phase 2 Status: IDLE (Select operation or run tests)"
            logger.clear()
            logger.i("UI", "Test results cleared.")
            updateUIState()
        }

        btnRunPhase2HeadlessTests.setOnClickListener {
            val summary = phase2HeadlessHarness.runAllHeadlessTests()
            tvPhase2Status.text = "Headless Core Tests: Passed ${summary.passedCount}/${summary.totalCount}"
            logger.i("Harness", "Phase 2 Headless Core Suite Executed. Overall Passed: ${summary.overallPassed}")
            updateUIState()
        }

        btnRunPhase2DeviceTests.setOnClickListener {
            val summary = phase2DeviceHarness.runAllDeviceTests()
            tvPhase2Status.text = "Device Control Tests: Passed ${summary.passedCount}/${summary.totalCount}"
            logger.i("Harness", "Phase 2 Device Control Suite Executed. Overall Passed: ${summary.overallPassed}")
            updateUIState()
        }

        logger.i("UI", "Control plane UI launched with Phase 2 panels.")
        updateUIState()
    }

    override fun onResume() {
        super.onResume()
        updateUIState()
    }

    private fun updateUIState() {
        val state = executionController.stateMachine.currentState
        val safety = executionController.safetyState
        tvAgentStatus.text = "Agent Status: ${state.name}"
        tvExecutionState.text = "Execution State: ${state.name}"
        tvSafetyStatus.text = "Safety Status: ${safety.status.name}"

        val isAccEnabled = isAccessibilityServiceEnabled(this, LocalAgentAccessibilityService::class.java)
        if (isAccEnabled) {
            tvAccessibilityStatus.text = "Accessibility Service: ENABLED"
            tvAccessibilityStatus.setTextColor(0xFF66BB6A.toInt())
            btnEnableAccessibility.text = "Accessibility Configured"
        } else {
            tvAccessibilityStatus.text = "Accessibility Service: NOT ENABLED"
            tvAccessibilityStatus.setTextColor(0xFFEF5350.toInt())
            btnEnableAccessibility.text = "Configure Accessibility Service"
        }

        refreshLogs()
    }

    private fun updateSuiteSummary(summary: HarnessSuiteSummary) {
        val overallText = if (summary.overallPassed) "PASS" else "FAIL"
        val color = if (summary.overallPassed) 0xFF66BB6A.toInt() else 0xFFEF5350.toInt()
        tvSuiteSummary.text = "PHASE 1 TEST RESULTS\nPassed: ${summary.passedCount} | Failed: ${summary.failedCount} | Total: ${summary.totalCount}\nOverall: $overallText"
        tvSuiteSummary.setTextColor(color)

        for (res in summary.results) {
            val status = if (res.passed) "PASS" else "FAIL"
            logger.i("Harness", "[$status] ${res.testName} (${res.durationMs}ms) - ${res.actualResult}")
        }
        updateUIState()
    }

    private fun refreshLogs() {
        val sb = StringBuilder()
        for (log in logger.getLogs()) {
            sb.append("[${log.category}] ${log.message}\n")
        }
        tvLogArea.text = if (sb.isNotEmpty()) sb.toString() else "[SYSTEM] Phase 2 LocalAgent active."
    }

    private fun isAccessibilityServiceEnabled(context: Context, service: Class<*>): Boolean {
        val expectedComponentName = "${context.packageName}/${service.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)

        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(expectedComponentName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }
}
