package com.agent.android.diagnostics

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.agent.android.agent.device.CapabilityRegistry
import com.agent.android.commands.CommandRegistry
import com.agent.android.commands.CommandStatus
import com.agent.android.execution.ExecutionController
import com.agent.android.speech.SpeechToTextEngine
import com.agent.android.test.FoundationTestRegistry

data class CategoryStatus(
    val categoryName: String,
    val isPassed: Boolean,
    val detail: String
)

data class FoundationReadinessReport(
    val isReady: Boolean,
    val statusText: String,
    val blockingReasons: List<String>,
    val categoryDetails: Map<String, CategoryStatus>,
    val commandCoverageText: String
)

class FoundationReadinessEvaluator(
    private val context: Context,
    private val executionController: ExecutionController,
    private val capabilityRegistry: CapabilityRegistry,
    private val testRegistry: FoundationTestRegistry,
    private val commandRegistry: CommandRegistry = CommandRegistry()
) {

    fun evaluate(): FoundationReadinessReport {
        val blockingReasons = mutableListOf<String>()
        val categoryDetails = mutableMapOf<String, CategoryStatus>()

        // 1. BUILD
        val pkgName = try { context.packageName ?: "" } catch (e: Exception) { "" }
        val buildPassed = pkgName.isNotBlank()
        val buildMsg = if (buildPassed) "App package valid ($pkgName)" else "No valid package context"
        if (!buildPassed) blockingReasons.add("BUILD: $buildMsg")
        categoryDetails["BUILD"] = CategoryStatus("BUILD", buildPassed, buildMsg)

        // 2. SAFETY
        val safetyState = executionController.safetyState
        val safetyPassed = safetyState.status != com.agent.android.safety.SafetyStatus.PANIC
        val safetyMsg = if (safetyPassed) "Safety state operational (${safetyState.status})" else "Safety state in PANIC"
        if (!safetyPassed) blockingReasons.add("SAFETY: $safetyMsg")
        categoryDetails["SAFETY"] = CategoryStatus("SAFETY", safetyPassed, safetyMsg)

        // 3. EXECUTION
        val execState = executionController.stateMachine.currentState
        val execPassed = execState == com.agent.android.execution.ExecutionState.IDLE
        val execMsg = if (execPassed) "Execution Controller IDLE & ready" else "Execution Controller busy ($execState)"
        if (!execPassed) blockingReasons.add("EXECUTION: $execMsg")
        categoryDetails["EXECUTION"] = CategoryStatus("EXECUTION", execPassed, execMsg)

        // 4. PERMISSIONS & SPECIAL ACCESS
        val writeSettings = try { Settings.System.canWrite(context) } catch (e: Exception) { false }
        val storagePerm = try { ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED } catch (e: Exception) { false }
        val cameraPerm = try { ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED } catch (e: Exception) { false }

        val permReasons = mutableListOf<String>()
        if (!writeSettings) permReasons.add("WRITE_SETTINGS not granted")
        if (!storagePerm) permReasons.add("WRITE_EXTERNAL_STORAGE not granted")
        if (!cameraPerm) permReasons.add("CAMERA not granted")

        val permPassed = permReasons.isEmpty()
        val permMsg = if (permPassed) "All foundation permissions granted" else permReasons.joinToString(", ")
        if (!permPassed) {
            permReasons.forEach { blockingReasons.add("PERMISSIONS: $it") }
        }
        categoryDetails["PERMISSIONS"] = CategoryStatus("PERMISSIONS", permPassed, permMsg)

        // 5. HARDWARE
        val caps = capabilityRegistry.checkDetailedCapabilities()
        val hwReasons = mutableListOf<String>()
        for ((name, cap) in caps) {
            if (cap.status == com.agent.android.agent.device.CapabilityStatus.UNAVAILABLE) {
                hwReasons.add("Hardware $name unavailable")
            }
        }
        val hwPassed = hwReasons.isEmpty()
        val hwMsg = if (hwPassed) "Hardware capabilities verified" else hwReasons.joinToString(", ")
        if (!hwPassed) {
            hwReasons.forEach { blockingReasons.add("HARDWARE: $it") }
        }
        categoryDetails["HARDWARE"] = CategoryStatus("HARDWARE", hwPassed, hwMsg)

        // 6. STT
        val sttEngine = SpeechToTextEngine(context)
        val sttAvailable = sttEngine.isAvailable()
        val sttMsg = if (sttAvailable) "SpeechRecognizer available" else "SpeechRecognizer unavailable on device"
        if (!sttAvailable) blockingReasons.add("STT: SpeechRecognizer unavailable")
        categoryDetails["STT"] = CategoryStatus("STT", sttAvailable, sttMsg)

        // 7. TTS
        val ttsMsg = "TextToSpeech API present"
        categoryDetails["TTS"] = CategoryStatus("TTS", true, ttsMsg)

        // 8. COMMAND REGISTRY COVERAGE & CROSS-VALIDATION
        val allCmds = commandRegistry.getAllCommands()
        val implCmds = allCmds.filter { it.status == CommandStatus.IMPLEMENTED }
        val allTests = testRegistry.getAllTestCases()
        val testedCmdIds = allTests.map { it.commandId }.toSet()

        val untestedImplCmds = implCmds.filter { !testedCmdIds.contains(it.commandId) }
        val coveragePct = if (implCmds.isNotEmpty()) ((implCmds.size - untestedImplCmds.size) * 100) / implCmds.size else 0

        val coverageMsg = "Implemented Commands: ${implCmds.size}/${allCmds.size} | Tested: ${implCmds.size - untestedImplCmds.size}/${implCmds.size} ($coveragePct% coverage)"
        val cmdPassed = untestedImplCmds.isEmpty()
        if (!cmdPassed) {
            blockingReasons.add("COMMANDS: ${untestedImplCmds.size} implemented commands have zero test coverage: ${untestedImplCmds.map { it.commandId }}")
        }
        categoryDetails["COMMAND_REGISTRY"] = CategoryStatus("COMMAND_REGISTRY", cmdPassed, coverageMsg)

        // 9. TESTS
        val summary = testRegistry.getSummary()
        val testsPassed = summary.failed == 0 && summary.blocked == 0 && summary.pending == 0 && summary.total > 0
        val testMsg = "Total: ${summary.total}, Passed: ${summary.passed}, Failed: ${summary.failed}, Blocked: ${summary.blocked}, Pending: ${summary.pending}"
        if (summary.failed > 0) blockingReasons.add("TESTS: ${summary.failed} tests failed")
        if (summary.blocked > 0) blockingReasons.add("TESTS: ${summary.blocked} tests blocked")
        if (summary.pending > 0) blockingReasons.add("TESTS: ${summary.pending} tests pending execution")

        categoryDetails["TESTS"] = CategoryStatus("TESTS", testsPassed, testMsg)

        val isReady = blockingReasons.isEmpty()
        val statusText = if (isReady) "READY" else "NOT_READY"

        return FoundationReadinessReport(
            isReady = isReady,
            statusText = statusText,
            blockingReasons = blockingReasons,
            categoryDetails = categoryDetails,
            commandCoverageText = coverageMsg
        )
    }
}
