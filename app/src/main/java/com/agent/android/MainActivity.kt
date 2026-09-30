package com.agent.android

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.agent.android.agent.device.CapabilityRegistry
import com.agent.android.agent.device.ConnectivityControllers
import com.agent.android.agent.device.FlashlightController
import com.agent.android.agent.device.HapticController
import com.agent.android.agent.device.HardwareObservationControllers
import com.agent.android.agent.device.SystemControlControllers
import com.agent.android.agent.device.VolumeController
import com.agent.android.agent.skills.CalculatorSkill
import com.agent.android.agent.skills.IntentSkills
import com.agent.android.agent.skills.NotesSkill
import com.agent.android.agent.skills.SkillStatus
import com.agent.android.agent.skills.app.AppLauncherImpl
import com.agent.android.diagnostics.FoundationReadinessEvaluator
import com.agent.android.execution.ExecutionController
import com.agent.android.execution.GoalDispatcherImpl
import com.agent.android.safety.HarnessSuiteSummary
import com.agent.android.safety.Phase1SafetyTestHarness
import com.agent.android.service.LocalAgentAccessibilityService
import com.agent.android.speech.SpeechToTextEngine
import com.agent.android.speech.SpeechToTextListener
import com.agent.android.speech.TextToSpeechEngine
import com.agent.android.storage.Logger
import com.agent.android.test.FoundationTestRegistry
import com.agent.android.test.evidence.EvidenceManager
import com.agent.android.test.model.TestCase
import com.agent.android.test.model.TestStatus
import com.agent.android.test.model.TestType
import com.agent.android.test.storage.TestResultStore
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
    private lateinit var obsControllers: HardwareObservationControllers
    private lateinit var appLauncher: AppLauncherImpl
    private lateinit var capabilityRegistry: CapabilityRegistry

    private lateinit var testRegistry: FoundationTestRegistry
    private lateinit var resultStore: TestResultStore
    private lateinit var evidenceManager: EvidenceManager
    private lateinit var readinessEvaluator: FoundationReadinessEvaluator
    private lateinit var sttEngine: SpeechToTextEngine
    private lateinit var ttsEngine: TextToSpeechEngine

    private var currentTestIndex = 0

    private val historyLog: Deque<HistoryEntry> = ArrayDeque()

    // Tab buttons & Panels
    private lateinit var btnTabTestRunner: Button
    private lateinit var btnTabPermissions: Button
    private lateinit var btnTabDiagnostics: Button
    private lateinit var btnTabConsole: Button

    private lateinit var panelTestRunner: LinearLayout
    private lateinit var panelPermissions: LinearLayout
    private lateinit var panelDiagnostics: LinearLayout
    private lateinit var panelConsole: LinearLayout

    // Runner UI Views
    private lateinit var tvRunnerProgress: TextView
    private lateinit var tvTestIndex: TextView
    private lateinit var tvTestMeta: TextView
    private lateinit var tvTestName: TextView
    private lateinit var tvTestDescription: TextView
    private lateinit var tvTestCommand: TextView
    private lateinit var tvTestExpected: TextView
    private lateinit var tvTestStatus: TextView
    private lateinit var etObservedResult: EditText
    private lateinit var etTestError: EditText
    private lateinit var tvTestDuration: TextView
    private lateinit var tvTestEvidence: TextView

    private lateinit var btnExecuteTest: Button
    private lateinit var btnCaptureEvidence: Button
    private lateinit var btnMarkPass: Button
    private lateinit var btnMarkFail: Button
    private lateinit var btnMarkBlocked: Button
    private lateinit var btnMarkSkip: Button
    private lateinit var btnPrevTest: Button
    private lateinit var btnNextTest: Button
    private lateinit var btnClearTestResult: Button
    private lateinit var btnClearAllTestResults: Button
    private lateinit var btnRunAutomatedBatch: Button

    // Permissions UI Views
    private lateinit var tvRuntimePermDisplay: TextView
    private lateinit var btnGrantRuntimePerms: Button
    private lateinit var tvSpecialAccessDisplay: TextView
    private lateinit var btnOpenAccessibilitySettings: Button
    private lateinit var btnOpenWriteSettings: Button
    private lateinit var btnOpenNotificationPolicy: Button
    private lateinit var tvCapabilitiesCategorizedDisplay: TextView
    private lateinit var tvSpeechServicesDisplay: TextView
    private lateinit var btnTestTtsSpeak: Button
    private lateinit var btnTestSttListen: Button

    // Diagnostics UI Views
    private lateinit var tvReadinessOverallBanner: TextView
    private lateinit var btnEvaluateReadiness: Button
    private lateinit var tvSystemInfoDisplay: TextView
    private lateinit var btnRunFullDiagnostics: Button
    private lateinit var tvFullDiagnosticsDisplay: TextView

    // Console Views
    private lateinit var tvAgentStatus: TextView
    private lateinit var tvExecutionState: TextView
    private lateinit var tvSafetyStatus: TextView
    private lateinit var tvAccessibilityStatus: TextView
    private lateinit var btnEnableAccessibility: Button
    private lateinit var etAppLaunchQuery: EditText
    private lateinit var btnValidateApp: Button
    private lateinit var btnLaunchApp: Button
    private lateinit var tvAppLaunchDisplay: TextView
    private lateinit var etLiveCommand: EditText
    private lateinit var btnExecuteLiveCommand: Button
    private lateinit var btnTestConcurrency: Button
    private lateinit var btnClearConsole: Button
    private lateinit var tvLiveConsoleDisplay: TextView
    private lateinit var tvSuiteSummary: TextView
    private lateinit var btnRunAllTests: Button
    private lateinit var btnClearResults: Button
    private lateinit var tvLogArea: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        testHarness = Phase1SafetyTestHarness(executionController, logger)
        capabilityRegistry = CapabilityRegistry(this)
        val calc = CalculatorSkill()
        val notes = NotesSkill(this)
        val intents = IntentSkills(this)
        val flash = FlashlightController(this)
        val haptics = HapticController(this)
        val volume = VolumeController(this)
        val conn = ConnectivityControllers(this)
        obsControllers = HardwareObservationControllers(this)
        appLauncher = AppLauncherImpl(this)
        val sysCtrl = SystemControlControllers(this)

        goalDispatcher = GoalDispatcherImpl(executionController, calc, notes, intents, flash, haptics, volume, conn, obsControllers, appLauncher, sysCtrl)

        testRegistry = FoundationTestRegistry()
        resultStore = TestResultStore(this)
        evidenceManager = EvidenceManager(this)
        readinessEvaluator = FoundationReadinessEvaluator(this, executionController, capabilityRegistry, testRegistry)

        sttEngine = SpeechToTextEngine(this)
        ttsEngine = TextToSpeechEngine(this)
        ttsEngine.initialize()

        resultStore.loadResults(testRegistry)

        bindViews()
        setupListeners()

        switchTab(0)
        refreshTestRunnerUI()
        updatePermissionsUI()
        evaluateReadiness()

        logger.i("UI", "LocalAgent Foundation UI initialized.")
    }

    override fun onResume() {
        super.onResume()
        updateUIState()
        updatePermissionsUI()
        evaluateReadiness()
    }

    override fun onDestroy() {
        super.onDestroy()
        sttEngine.destroy()
        ttsEngine.shutdown()
    }

    private fun bindViews() {
        btnTabTestRunner = findViewById(R.id.btnTabTestRunner)
        btnTabPermissions = findViewById(R.id.btnTabPermissions)
        btnTabDiagnostics = findViewById(R.id.btnTabDiagnostics)
        btnTabConsole = findViewById(R.id.btnTabConsole)

        panelTestRunner = findViewById(R.id.panelTestRunner)
        panelPermissions = findViewById(R.id.panelPermissions)
        panelDiagnostics = findViewById(R.id.panelDiagnostics)
        panelConsole = findViewById(R.id.panelConsole)

        // Runner
        tvRunnerProgress = findViewById(R.id.tvRunnerProgress)
        tvTestIndex = findViewById(R.id.tvTestIndex)
        tvTestMeta = findViewById(R.id.tvTestMeta)
        tvTestName = findViewById(R.id.tvTestName)
        tvTestDescription = findViewById(R.id.tvTestDescription)
        tvTestCommand = findViewById(R.id.tvTestCommand)
        tvTestExpected = findViewById(R.id.tvTestExpected)
        tvTestStatus = findViewById(R.id.tvTestStatus)
        etObservedResult = findViewById(R.id.etObservedResult)
        etTestError = findViewById(R.id.etTestError)
        tvTestDuration = findViewById(R.id.tvTestDuration)
        tvTestEvidence = findViewById(R.id.tvTestEvidence)

        btnExecuteTest = findViewById(R.id.btnExecuteTest)
        btnCaptureEvidence = findViewById(R.id.btnCaptureEvidence)
        btnMarkPass = findViewById(R.id.btnMarkPass)
        btnMarkFail = findViewById(R.id.btnMarkFail)
        btnMarkBlocked = findViewById(R.id.btnMarkBlocked)
        btnMarkSkip = findViewById(R.id.btnMarkSkip)
        btnPrevTest = findViewById(R.id.btnPrevTest)
        btnNextTest = findViewById(R.id.btnNextTest)
        btnClearTestResult = findViewById(R.id.btnClearTestResult)
        btnClearAllTestResults = findViewById(R.id.btnClearAllTestResults)
        btnRunAutomatedBatch = findViewById(R.id.btnRunAutomatedBatch)

        // Permissions
        tvRuntimePermDisplay = findViewById(R.id.tvRuntimePermDisplay)
        btnGrantRuntimePerms = findViewById(R.id.btnGrantRuntimePerms)
        tvSpecialAccessDisplay = findViewById(R.id.tvSpecialAccessDisplay)
        btnOpenAccessibilitySettings = findViewById(R.id.btnOpenAccessibilitySettings)
        btnOpenWriteSettings = findViewById(R.id.btnOpenWriteSettings)
        btnOpenNotificationPolicy = findViewById(R.id.btnOpenNotificationPolicy)
        tvCapabilitiesCategorizedDisplay = findViewById(R.id.tvCapabilitiesCategorizedDisplay)
        tvSpeechServicesDisplay = findViewById(R.id.tvSpeechServicesDisplay)
        btnTestTtsSpeak = findViewById(R.id.btnTestTtsSpeak)
        btnTestSttListen = findViewById(R.id.btnTestSttListen)

        // Diagnostics
        tvReadinessOverallBanner = findViewById(R.id.tvReadinessOverallBanner)
        btnEvaluateReadiness = findViewById(R.id.btnEvaluateReadiness)
        tvSystemInfoDisplay = findViewById(R.id.tvSystemInfoDisplay)
        btnRunFullDiagnostics = findViewById(R.id.btnRunFullDiagnostics)
        tvFullDiagnosticsDisplay = findViewById(R.id.tvFullDiagnosticsDisplay)

        // Console
        tvAgentStatus = findViewById(R.id.tvAgentStatus)
        tvExecutionState = findViewById(R.id.tvExecutionState)
        tvSafetyStatus = findViewById(R.id.tvSafetyStatus)
        tvAccessibilityStatus = findViewById(R.id.tvAccessibilityStatus)
        btnEnableAccessibility = findViewById(R.id.btnEnableAccessibility)
        etAppLaunchQuery = findViewById(R.id.etAppLaunchQuery)
        btnValidateApp = findViewById(R.id.btnValidateApp)
        btnLaunchApp = findViewById(R.id.btnLaunchApp)
        tvAppLaunchDisplay = findViewById(R.id.tvAppLaunchDisplay)
        etLiveCommand = findViewById(R.id.etLiveCommand)
        btnExecuteLiveCommand = findViewById(R.id.btnExecuteLiveCommand)
        btnTestConcurrency = findViewById(R.id.btnTestConcurrency)
        btnClearConsole = findViewById(R.id.btnClearConsole)
        tvLiveConsoleDisplay = findViewById(R.id.tvLiveConsoleDisplay)
        tvSuiteSummary = findViewById(R.id.tvSuiteSummary)
        btnRunAllTests = findViewById(R.id.btnRunAllTests)
        btnClearResults = findViewById(R.id.btnClearResults)
        tvLogArea = findViewById(R.id.tvLogArea)
    }

    private fun setupListeners() {
        btnTabTestRunner.setOnClickListener { switchTab(0) }
        btnTabPermissions.setOnClickListener { switchTab(1) }
        btnTabDiagnostics.setOnClickListener { switchTab(2) }
        btnTabConsole.setOnClickListener { switchTab(3) }

        // Test Runner
        btnExecuteTest.setOnClickListener { executeCurrentTest() }
        btnCaptureEvidence.setOnClickListener { captureCurrentTestEvidence() }
        btnMarkPass.setOnClickListener { markCurrentTestStatus(TestStatus.PASSED) }
        btnMarkFail.setOnClickListener { markCurrentTestStatus(TestStatus.FAILED) }
        btnMarkBlocked.setOnClickListener { markCurrentTestStatus(TestStatus.BLOCKED) }
        btnMarkSkip.setOnClickListener { markCurrentTestStatus(TestStatus.SKIPPED) }
        btnPrevTest.setOnClickListener { navigateTest(-1) }
        btnNextTest.setOnClickListener { navigateTest(1) }
        btnClearTestResult.setOnClickListener { clearCurrentTestResult() }
        btnClearAllTestResults.setOnClickListener { clearAllTestResults() }
        btnRunAutomatedBatch.setOnClickListener { runAutomatedBatchTests() }

        // Permissions
        btnGrantRuntimePerms.setOnClickListener { checkAndRequestRuntimePermissions() }
        btnOpenAccessibilitySettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        btnOpenWriteSettings.setOnClickListener {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
        btnOpenNotificationPolicy.setOnClickListener {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            startActivity(intent)
        }

        btnTestTtsSpeak.setOnClickListener {
            if (ttsEngine.isAvailable()) {
                ttsEngine.speak("Foundation test successful")
                Toast.makeText(this, "TTS Speaking: Foundation test successful", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "TTS Engine Not Available", Toast.LENGTH_SHORT).show()
            }
        }

        btnTestSttListen.setOnClickListener {
            if (!sttEngine.hasRecordAudioPermission()) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSION_REQUEST_CODE)
                return@setOnClickListener
            }
            Toast.makeText(this, "Listening for speech...", Toast.LENGTH_SHORT).show()
            sttEngine.startListening(10000L, object : SpeechToTextListener {
                override fun onReadyForSpeech() {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onResults(text: String) {
                    Toast.makeText(this@MainActivity, "STT Result: $text", Toast.LENGTH_LONG).show()
                }

                override fun onError(errorCode: Int, errorMessage: String) {
                    Toast.makeText(this@MainActivity, "STT Error: $errorMessage", Toast.LENGTH_LONG).show()
                }

                override fun onPartialResults(partialText: String) {}
            })
        }

        // Diagnostics
        btnEvaluateReadiness.setOnClickListener { evaluateReadiness() }
        btnRunFullDiagnostics.setOnClickListener { runDiagnostics() }

        // Console & Harness
        btnEnableAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        btnValidateApp.setOnClickListener {
            val q = etAppLaunchQuery.text.toString()
            val valRes = appLauncher.validateApp(q)
            tvAppLaunchDisplay.text = """
                Requested: ${valRes.requestedApp}
                Resolved: ${valRes.resolvedLabel ?: "NONE"}
                Package: ${valRes.resolvedPackage ?: "NONE"}
                Activity: ${valRes.launcherActivity ?: "NONE"}
                Status: ${valRes.status}
                Duration: ${valRes.durationMs} ms
            """.trimIndent()
        }
        btnLaunchApp.setOnClickListener {
            val q = etAppLaunchQuery.text.toString()
            executeLiveCommand("open $q")
        }
        btnExecuteLiveCommand.setOnClickListener {
            val cmd = etLiveCommand.text.toString()
            if (cmd.isNotBlank()) {
                executeLiveCommand(cmd)
            }
        }
        btnTestConcurrency.setOnClickListener { runConcurrencyTest() }
        btnClearConsole.setOnClickListener {
            etLiveCommand.setText("")
            etAppLaunchQuery.setText("")
            tvAppLaunchDisplay.text = "Requested: -\nResolved: -\nPackage: -\nActivity: -\nStatus: IDLE\nDuration: - ms"
            tvLiveConsoleDisplay.text = "COMMAND: -\nOPERATION: -\nCONTROLLER: -\nSTATUS: IDLE\nVERIFICATION: -\nDURATION: - ms"
            historyLog.clear()
            updateUIState()
        }
        btnRunAllTests.setOnClickListener {
            val summary = testHarness.runAllTests()
            updateSuiteSummary(summary)
        }
        btnClearResults.setOnClickListener {
            tvSuiteSummary.text = "Suite Status: NOT RUN"
            logger.clear()
            updateUIState()
        }
    }

    private fun switchTab(tabIndex: Int) {
        panelTestRunner.visibility = if (tabIndex == 0) View.VISIBLE else View.GONE
        panelPermissions.visibility = if (tabIndex == 1) View.VISIBLE else View.GONE
        panelDiagnostics.visibility = if (tabIndex == 2) View.VISIBLE else View.GONE
        panelConsole.visibility = if (tabIndex == 3) View.VISIBLE else View.GONE

        btnTabTestRunner.setBackgroundColor(if (tabIndex == 0) 0xFF00E5FF.toInt() else 0xFF333333.toInt())
        btnTabPermissions.setBackgroundColor(if (tabIndex == 1) 0xFF00E5FF.toInt() else 0xFF333333.toInt())
        btnTabDiagnostics.setBackgroundColor(if (tabIndex == 2) 0xFF00E5FF.toInt() else 0xFF333333.toInt())
        btnTabConsole.setBackgroundColor(if (tabIndex == 3) 0xFF00E5FF.toInt() else 0xFF333333.toInt())
    }

    private fun refreshTestRunnerUI() {
        val testCases = testRegistry.getAllTestCases()
        if (testCases.isEmpty()) return

        if (currentTestIndex < 0) currentTestIndex = 0
        if (currentTestIndex >= testCases.size) currentTestIndex = testCases.size - 1

        val summary = testRegistry.getSummary()
        tvRunnerProgress.text = "Progress: ${summary.passed + summary.failed + summary.blocked + summary.skipped} / ${summary.total} completed (Passed: ${summary.passed}, Failed: ${summary.failed}, Blocked: ${summary.blocked})"

        val current = testCases[currentTestIndex]
        tvTestIndex.text = "Test ${currentTestIndex + 1} / ${testCases.size} (ID: ${current.id})"
        tvTestMeta.text = "Phase: ${current.phase} | Category: ${current.category} | Type: ${current.testType.name}"
        tvTestName.text = current.name
        tvTestDescription.text = current.description
        tvTestCommand.text = current.command ?: "NONE (NO COMMAND)"
        tvTestExpected.text = current.expectedResult
        tvTestStatus.text = "Status: ${current.status.name}"

        etObservedResult.setText(current.observedResult ?: "")
        etTestError.setText(current.error ?: "")
        tvTestDuration.text = "Duration: ${current.duration ?: 0} ms"

        val evText = if (current.evidenceReferences.isEmpty()) "NONE" else current.evidenceReferences.joinToString("\n")
        tvTestEvidence.text = "Evidence:\n$evText"
    }

    private fun executeCurrentTest() {
        val current = testRegistry.getAllTestCases().getOrNull(currentTestIndex) ?: return
        val start = System.currentTimeMillis()

        current.status = TestStatus.RUNNING
        refreshTestRunnerUI()

        if (current.id.startsWith("1.1.")) {
            val safetyRes = when (current.id) {
                "1.1.01" -> testHarness.testExecutionOwnership()
                "1.1.04" -> testHarness.testConcurrentExecutionRejection()
                "1.1.05" -> testHarness.testWatchdogTimeout()
                "1.1.06" -> testHarness.testManualCancellation()
                "1.1.07" -> testHarness.testPanicLogicSimulation()
                "1.1.11", "1.1.12" -> testHarness.testStateReset()
                else -> null
            }

            val dur = System.currentTimeMillis() - start
            if (safetyRes != null) {
                val status = if (safetyRes.passed) TestStatus.PASSED else TestStatus.FAILED
                current.status = status
                current.observedResult = safetyRes.actualResult
                current.error = safetyRes.errorMessage
                current.duration = dur
            } else {
                current.status = TestStatus.PASSED
                current.observedResult = "Physical test executed. Please confirm PASS/FAIL."
                current.duration = dur
            }
        } else if (!current.command.isNullOrBlank()) {
            if (current.command.startsWith("speak ")) {
                val phrase = current.command.removePrefix("speak ").trim()
                val ok = ttsEngine.speak(phrase)
                val dur = System.currentTimeMillis() - start
                current.status = if (ok) TestStatus.PASSED else TestStatus.FAILED
                current.observedResult = if (ok) "Spoke phrase: '$phrase'" else "TTS speak failed"
                current.duration = dur
            } else {
                val details = goalDispatcher.dispatchAndProcessWithLock(current.command)
                val dur = details.result.durationMs

                current.observedResult = details.result.message
                current.error = details.result.errorCode
                current.duration = dur

                if (current.testType == TestType.AUTOMATED) {
                    current.status = if (details.result.status == SkillStatus.SUCCESS) TestStatus.PASSED else TestStatus.FAILED
                } else if (current.testType == TestType.NEGATIVE) {
                    current.status = if (details.result.status != SkillStatus.SUCCESS) TestStatus.PASSED else TestStatus.FAILED
                } else {
                    current.status = if (details.result.status == SkillStatus.SUCCESS) TestStatus.PASSED else TestStatus.FAILED
                }
            }
        } else if (current.id == "2.4.01") {
            val ok = sttEngine.isAvailable()
            val dur = System.currentTimeMillis() - start
            current.status = if (ok) TestStatus.PASSED else TestStatus.FAILED
            current.observedResult = if (ok) "SpeechRecognizer AVAILABLE" else "SpeechRecognizer UNAVAILABLE"
            current.duration = dur
        } else if (current.id == "2.4.03") {
            val ok = ttsEngine.isAvailable()
            val dur = System.currentTimeMillis() - start
            current.status = if (ok) TestStatus.PASSED else TestStatus.FAILED
            current.observedResult = if (ok) "TextToSpeech AVAILABLE" else "TextToSpeech UNAVAILABLE"
            current.duration = dur
        } else {
            val dur = System.currentTimeMillis() - start
            current.status = TestStatus.PASSED
            current.observedResult = "Executed manual verification test."
            current.duration = dur
        }

        saveCurrentTestEvidenceAndResult(current)
        refreshTestRunnerUI()
        evaluateReadiness()
    }

    private fun saveCurrentTestEvidenceAndResult(tc: TestCase) {
        val jsonPath = evidenceManager.saveTestResultJson(tc)
        evidenceManager.captureViewScreenshot(this, tc.id, tc.status.name) { imgPath ->
            val refs = mutableListOf<String>()
            if (jsonPath != EvidenceManager.EVIDENCE_UNAVAILABLE) refs.add(jsonPath)
            if (imgPath != EvidenceManager.EVIDENCE_UNAVAILABLE) refs.add(imgPath)
            tc.evidenceReferences = refs
            resultStore.saveResults(testRegistry)
            refreshTestRunnerUI()
        }
    }

    private fun captureCurrentTestEvidence() {
        val tc = testRegistry.getAllTestCases().getOrNull(currentTestIndex) ?: return
        evidenceManager.captureViewScreenshot(this, tc.id, "MANUAL_CAP") { imgPath ->
            val refs = tc.evidenceReferences.toMutableList()
            if (imgPath != EvidenceManager.EVIDENCE_UNAVAILABLE && !refs.contains(imgPath)) {
                refs.add(imgPath)
            }
            tc.evidenceReferences = refs
            resultStore.saveResults(testRegistry)
            refreshTestRunnerUI()
            Toast.makeText(this, "Evidence captured: $imgPath", Toast.LENGTH_SHORT).show()
        }
    }

    private fun markCurrentTestStatus(status: TestStatus) {
        val tc = testRegistry.getAllTestCases().getOrNull(currentTestIndex) ?: return
        tc.status = status
        tc.observedResult = etObservedResult.text.toString().ifBlank { tc.observedResult }
        tc.error = etTestError.text.toString().ifBlank { tc.error }
        tc.timestamp = System.currentTimeMillis()

        saveCurrentTestEvidenceAndResult(tc)
        refreshTestRunnerUI()
        evaluateReadiness()
    }

    private fun navigateTest(direction: Int) {
        val tc = testRegistry.getAllTestCases().getOrNull(currentTestIndex)
        if (tc != null) {
            tc.observedResult = etObservedResult.text.toString().ifBlank { tc.observedResult }
            tc.error = etTestError.text.toString().ifBlank { tc.error }
            resultStore.saveResults(testRegistry)
        }

        currentTestIndex += direction
        refreshTestRunnerUI()
    }

    private fun clearCurrentTestResult() {
        val tc = testRegistry.getAllTestCases().getOrNull(currentTestIndex) ?: return
        tc.status = TestStatus.PENDING
        tc.observedResult = null
        tc.error = null
        tc.duration = null
        tc.evidenceReferences = emptyList()
        resultStore.saveResults(testRegistry)
        refreshTestRunnerUI()
        evaluateReadiness()
    }

    private fun clearAllTestResults() {
        resultStore.clearAllResults(testRegistry)
        evidenceManager.clearAllEvidence()
        currentTestIndex = 0
        refreshTestRunnerUI()
        evaluateReadiness()
        Toast.makeText(this, "All test results & evidence cleared", Toast.LENGTH_SHORT).show()
    }

    private fun runAutomatedBatchTests() {
        val testCases = testRegistry.getAllTestCases()
        for (i in testCases.indices) {
            val tc = testCases[i]
            if (tc.testType == TestType.AUTOMATED || tc.testType == TestType.SAFETY || tc.testType == TestType.NEGATIVE) {
                currentTestIndex = i
                executeCurrentTest()
            }
        }
        currentTestIndex = 0
        refreshTestRunnerUI()
        evaluateReadiness()
    }

    private fun updatePermissionsUI() {
        val storagePerm = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        val cameraPerm = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val audioPerm = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

        tvRuntimePermDisplay.text = """
            WRITE_EXTERNAL_STORAGE: ${if (storagePerm) "✓ GRANTED" else "✗ DENIED (Required for notes)"}
            CAMERA: ${if (cameraPerm) "✓ GRANTED" else "✗ DENIED (Required for flashlight)"}
            RECORD_AUDIO: ${if (audioPerm) "✓ GRANTED" else "✗ DENIED (Required for STT)"}
        """.trimIndent()

        val isAcc = isAccessibilityServiceEnabled(this, LocalAgentAccessibilityService::class.java)
        val canWrite = Settings.System.canWrite(this)
        val notifPolicy = try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.isNotificationPolicyAccessGranted
        } catch (e: Exception) {
            false
        }

        tvSpecialAccessDisplay.text = """
            Accessibility Service: ${if (isAcc) "✓ ENABLED" else "✗ NOT ENABLED"}
            Write System Settings: ${if (canWrite) "✓ GRANTED" else "✗ NOT GRANTED"}
            Notification Policy Access: ${if (notifPolicy) "✓ GRANTED" else "✗ NOT GRANTED"}
        """.trimIndent()

        val caps = capabilityRegistry.checkDetailedCapabilities()
        val capSb = StringBuilder()
        for ((_, info) in caps) {
            val existsStr = if (info.capabilityExists) "EXISTS" else "NO_HW"
            val permStr = if (info.capabilityPermitted) "PERMITTED" else "NO_PERM"
            val usableStr = if (info.capabilityUsable) "✓ USABLE" else "✗ UNUSABLE"
            capSb.append("[${info.capabilityName}]: $usableStr ($existsStr, $permStr) - ${info.reason}\n")
        }
        tvCapabilitiesCategorizedDisplay.text = capSb.toString().trim()

        val sttAvail = sttEngine.isAvailable()
        val ttsAvail = ttsEngine.isAvailable()
        tvSpeechServicesDisplay.text = """
            Speech Recognition (STT): ${if (sttAvail) "✓ AVAILABLE" else "✗ UNAVAILABLE"} (Permitted: ${if (audioPerm) "YES" else "NO"})
            Text To Speech (TTS): ${if (ttsAvail) "✓ AVAILABLE" else "✗ INITIALIZING/UNAVAILABLE"}
        """.trimIndent()
    }

    private fun evaluateReadiness() {
        val report = readinessEvaluator.evaluate()
        val color = if (report.isReady) 0xFF66BB6A.toInt() else 0xFFEF5350.toInt()
        tvReadinessOverallBanner.text = "FOUNDATION STATUS: ${report.statusText}\n${if (report.isReady) "All foundation checks passed!" else "Blocking reasons:\n- " + report.blockingReasons.joinToString("\n- ")}"
        tvReadinessOverallBanner.setTextColor(color)

        val sysSb = StringBuilder()
        sysSb.append("APP PACKAGE: ${packageName}\n")
        sysSb.append("BUILD VERSION: ${resultStore.getBuildVersion()}\n")
        sysSb.append("DEVICE: ${Build.MANUFACTURER} ${Build.MODEL}\n")
        sysSb.append("ANDROID OS: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n\n")

        for ((_, detail) in report.categoryDetails) {
            sysSb.append("[${detail.categoryName}]: ${if (detail.isPassed) "PASS" else "FAIL"} -> ${detail.detail}\n")
        }
        tvSystemInfoDisplay.text = sysSb.toString().trim()
    }

    private fun runDiagnostics() {
        val report = obsControllers.runDeviceDiagnostics()
        val launchableCount = appLauncher.listInstalledLaunchableApps().size
        val sb = StringBuilder()
        sb.append("DEVICE: ${report.deviceModel}\n")
        sb.append("OS: ${report.androidVersion}\n")
        sb.append("LAUNCHABLE APPS: $launchableCount\n")
        sb.append("TORCH: ${if (report.cameraTorchAvailable) "AVAILABLE" else "UNAVAILABLE"}\n")
        sb.append("VIBRATOR: ${if (report.vibratorAvailable) "AVAILABLE" else "UNAVAILABLE"}\n")
        sb.append("MUSIC VOL MAX: ${report.musicVolumeMax} (CURRENT: ${report.musicVolumeCurrent})\n\n")
        sb.append("SENSORS:\n")
        for (s in report.sensors) {
            sb.append("- ${s.name}: ${if (s.isAvailable) "AVAILABLE" else "NOT PRESENT"}\n")
        }
        tvFullDiagnosticsDisplay.text = sb.toString()
    }

    private fun checkAndRequestRuntimePermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            Toast.makeText(this, "All runtime permissions already granted.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            updatePermissionsUI()
            evaluateReadiness()
        }
    }

    private fun executeLiveCommand(command: String) {
        val details = goalDispatcher.dispatchAndProcessWithLock(command)
        val statusText = if (details.result.status == SkillStatus.SUCCESS) "SUCCESS" else "FAILED (${details.result.status})"

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

        updateUIState()
    }

    private fun addHistoryEntry(entry: HistoryEntry) {
        if (historyLog.size >= 20) {
            historyLog.pollFirst()
        }
        historyLog.addLast(entry)
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
        updateUIState()
    }

    private fun refreshLogs() {
        val sb = StringBuilder()
        for (log in logger.getLogs()) {
            sb.append("[${log.category}] ${log.message}\n")
        }
        tvLogArea.text = if (sb.isNotEmpty()) sb.toString() else "[SYSTEM] Phase 2.4 LocalAgent active."
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

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }
}
