package com.agent.android

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.agent.android.agent.device.ConnectivityControllers
import com.agent.android.agent.device.FlashlightController
import com.agent.android.agent.device.HapticController
import com.agent.android.agent.device.HardwareObservationControllers
import com.agent.android.agent.device.VolumeController
import com.agent.android.agent.skills.CalculatorSkill
import com.agent.android.agent.skills.IntentSkills
import com.agent.android.agent.skills.NotesSkill
import com.agent.android.execution.DispatchDetails
import com.agent.android.execution.ExecutionController
import com.agent.android.execution.GoalDispatcherImpl
import com.agent.android.safety.HarnessSuiteSummary
import com.agent.android.safety.Phase1SafetyTestHarness
import com.agent.android.service.LocalAgentAccessibilityService
import com.agent.android.storage.Logger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.ArrayDeque
import java.util.Deque

data class HistoryEntry(
    val timestamp: String,
    val command: String,
    val status: String,
    val durationMs: Long,
    val errorCode: String?
)

class MainActivity : Activity() {

    private val executionController = ExecutionController()
    private val logger = Logger()
    private lateinit var testHarness: Phase1SafetyTestHarness
    private lateinit var goalDispatcher: GoalDispatcherImpl

    private val historyLog: Deque<HistoryEntry> = ArrayDeque()

    private lateinit var tvAgentStatus: TextView
    private lateinit var tvExecutionState: TextView
    private lateinit var tvSafetyStatus: TextView
    private lateinit var tvAccessibilityStatus: TextView
    private lateinit var btnEnableAccessibility: Button

    private lateinit var etLiveCommand: EditText
    private lateinit var btnExecuteLiveCommand: Button
    private lateinit var btnTestConcurrency: Button
    private lateinit var btnClearConsole: Button
    private lateinit var tvLiveConsoleDisplay: TextView
    private lateinit var tvExecutionHistoryLog: TextView

    private lateinit var tvSuiteSummary: TextView
    private lateinit var btnRunAllTests: Button
    private lateinit var btnClearResults: Button
    private lateinit var tvLogArea: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        testHarness = Phase1SafetyTestHarness(executionController, logger)
        val calc = CalculatorSkill()
        val notes = NotesSkill(this)
        val intents = IntentSkills(this)
        val flash = FlashlightController(this)
        val haptics = HapticController(this)
        val volume = VolumeController(this)
        val conn = ConnectivityControllers(this)
        val obs = HardwareObservationControllers(this)

        goalDispatcher = GoalDispatcherImpl(executionController, calc, notes, intents, flash, haptics, volume, conn, obs)

        tvAgentStatus = findViewById(R.id.tvAgentStatus)
        tvExecutionState = findViewById(R.id.tvExecutionState)
        tvSafetyStatus = findViewById(R.id.tvSafetyStatus)
        tvAccessibilityStatus = findViewById(R.id.tvAccessibilityStatus)
        btnEnableAccessibility = findViewById(R.id.btnEnableAccessibility)

        etLiveCommand = findViewById(R.id.etLiveCommand)
        btnExecuteLiveCommand = findViewById(R.id.btnExecuteLiveCommand)
        btnTestConcurrency = findViewById(R.id.btnTestConcurrency)
        btnClearConsole = findViewById(R.id.btnClearConsole)
        tvLiveConsoleDisplay = findViewById(R.id.tvLiveConsoleDisplay)
        tvExecutionHistoryLog = findViewById(R.id.tvExecutionHistoryLog)

        tvSuiteSummary = findViewById(R.id.tvSuiteSummary)
        btnRunAllTests = findViewById(R.id.btnRunAllTests)
        btnClearResults = findViewById(R.id.btnClearResults)
        tvLogArea = findViewById(R.id.tvLogArea)

        btnEnableAccessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        btnExecuteLiveCommand.setOnClickListener {
            val cmd = etLiveCommand.text.toString()
            if (cmd.isNotBlank()) {
                executeLiveCommand(cmd)
            }
        }

        btnTestConcurrency.setOnClickListener {
            runConcurrencyTest()
        }

        btnClearConsole.setOnClickListener {
            etLiveCommand.setText("")
            tvLiveConsoleDisplay.text = "COMMAND: -\nOPERATION: -\nCONTROLLER: -\nSTATUS: IDLE\nVERIFICATION: -\nDURATION: - ms"
            historyLog.clear()
            tvExecutionHistoryLog.text = "No recent executions."
            logger.i("UI", "Console and history cleared.")
            updateUIState()
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

        logger.i("UI", "Control plane UI launched with Live Command Console.")
        updateUIState()
    }

    override fun onResume() {
        super.onResume()
        updateUIState()
    }

    private fun executeLiveCommand(command: String) {
        val start = System.currentTimeMillis()
        val details = goalDispatcher.dispatchAndProcess(command)
        val success = goalDispatcher.dispatchGoal(command)

        val duration = System.currentTimeMillis() - start
        val statusText = if (success) "SUCCESS" else "FAILED (${details.result.status})"

        tvLiveConsoleDisplay.text = """
            COMMAND: ${details.command}
            OPERATION: ${details.operation}
            CONTROLLER: ${details.controllerName}
            STATUS: $statusText
            RESULT: ${details.result.message}
            VERIFICATION: ${details.verificationText}
            ERROR CODE: ${details.result.errorCode ?: "NONE"}
            DURATION: ${details.result.durationMs} ms
        """.trimIndent()

        addHistoryEntry(HistoryEntry(
            timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date()),
            command = details.command,
            status = details.result.status.name,
            durationMs = details.result.durationMs,
            errorCode = details.result.errorCode
        ))

        updateUIState()
    }

    private fun runConcurrencyTest() {
        val firstAcquired = executionController.acquireExecution()
        val secondAcquired = executionController.acquireExecution()

        val resultMsg = "Concurrency Lock Test:\nCommand 1: ${if (firstAcquired) "ACCEPTED" else "REJECTED"}\nCommand 2: ${if (secondAcquired) "ACCEPTED (ERROR)" else "REJECTED (CORRECT)"}"
        tvLiveConsoleDisplay.text = resultMsg

        if (firstAcquired) {
            executionController.releaseExecution()
        }

        addHistoryEntry(HistoryEntry(
            timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date()),
            command = "CONCURRENCY_TEST",
            status = if (firstAcquired && !secondAcquired) "PASS" else "FAIL",
            durationMs = 0L,
            errorCode = if (secondAcquired) "LOCK_FAILED" else null
        ))

        updateUIState()
    }

    private fun addHistoryEntry(entry: HistoryEntry) {
        if (historyLog.size >= 20) {
            historyLog.pollFirst()
        }
        historyLog.addLast(entry)

        val sb = StringBuilder()
        for (h in historyLog) {
            sb.append("[${h.timestamp}] ${h.command} -> ${h.status} (${h.durationMs}ms)")
            if (h.errorCode != null) sb.append(" [Err: ${h.errorCode}]")
            sb.append("\n")
        }
        tvExecutionHistoryLog.text = sb.toString()
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
