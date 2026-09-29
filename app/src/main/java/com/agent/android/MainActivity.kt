package com.agent.android

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Button
import android.widget.TextView
import com.agent.android.execution.ExecutionController
import com.agent.android.safety.HarnessSuiteSummary
import com.agent.android.safety.Phase1SafetyTestHarness
import com.agent.android.safety.SafetyTestResult
import com.agent.android.service.LocalAgentAccessibilityService
import com.agent.android.storage.Logger

class MainActivity : Activity() {

    private val executionController = ExecutionController()
    private val logger = Logger()
    private lateinit var testHarness: Phase1SafetyTestHarness

    private lateinit var tvAgentStatus: TextView
    private lateinit var tvExecutionState: TextView
    private lateinit var tvSafetyStatus: TextView
    private lateinit var tvAccessibilityStatus: TextView
    private lateinit var btnEnableAccessibility: Button
    private lateinit var tvSuiteSummary: TextView
    private lateinit var tvLogArea: TextView

    private lateinit var btnRunAllTests: Button
    private lateinit var btnClearResults: Button
    private lateinit var btnTestOwnership: Button
    private lateinit var btnTestConcurrent: Button
    private lateinit var btnTestCancellation: Button
    private lateinit var btnTestWatchdog: Button
    private lateinit var btnTestPanic: Button
    private lateinit var btnTestStateReset: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        testHarness = Phase1SafetyTestHarness(executionController, logger)

        tvAgentStatus = findViewById(R.id.tvAgentStatus)
        tvExecutionState = findViewById(R.id.tvExecutionState)
        tvSafetyStatus = findViewById(R.id.tvSafetyStatus)
        tvAccessibilityStatus = findViewById(R.id.tvAccessibilityStatus)
        btnEnableAccessibility = findViewById(R.id.btnEnableAccessibility)
        tvSuiteSummary = findViewById(R.id.tvSuiteSummary)
        tvLogArea = findViewById(R.id.tvLogArea)

        btnRunAllTests = findViewById(R.id.btnRunAllTests)
        btnClearResults = findViewById(R.id.btnClearResults)
        btnTestOwnership = findViewById(R.id.btnTestOwnership)
        btnTestConcurrent = findViewById(R.id.btnTestConcurrent)
        btnTestCancellation = findViewById(R.id.btnTestCancellation)
        btnTestWatchdog = findViewById(R.id.btnTestWatchdog)
        btnTestPanic = findViewById(R.id.btnTestPanic)
        btnTestStateReset = findViewById(R.id.btnTestStateReset)

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
            logger.clear()
            logger.i("UI", "Test results cleared.")
            updateUIState()
        }

        btnTestOwnership.setOnClickListener { displaySingleTestResult(testHarness.testExecutionOwnership()) }
        btnTestConcurrent.setOnClickListener { displaySingleTestResult(testHarness.testConcurrentExecutionRejection()) }
        btnTestCancellation.setOnClickListener { displaySingleTestResult(testHarness.testManualCancellation()) }
        btnTestWatchdog.setOnClickListener { displaySingleTestResult(testHarness.testWatchdogTimeout()) }
        btnTestPanic.setOnClickListener { displaySingleTestResult(testHarness.testPanicLogicSimulation()) }
        btnTestStateReset.setOnClickListener { displaySingleTestResult(testHarness.testStateReset()) }

        logger.i("UI", "Control plane UI launched with Safety Test Panel.")
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

    private fun displaySingleTestResult(result: SafetyTestResult) {
        val status = if (result.passed) "PASS" else "FAIL"
        logger.i("Harness", "[$status] ${result.testName} (${result.durationMs}ms) - ${result.actualResult}")
        updateUIState()
    }

    private fun refreshLogs() {
        val sb = StringBuilder()
        for (log in logger.getLogs()) {
            sb.append("[${log.category}] ${log.message}\n")
        }
        tvLogArea.text = if (sb.isNotEmpty()) sb.toString() else "[SYSTEM] Phase 1 LocalAgent active."
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
