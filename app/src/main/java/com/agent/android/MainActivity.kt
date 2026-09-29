package com.agent.android

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Button
import android.widget.TextView
import com.agent.android.execution.ExecutionState
import com.agent.android.execution.ExecutionStateMachine
import com.agent.android.service.LocalAgentAccessibilityService
import com.agent.android.storage.Logger

class MainActivity : Activity() {

    private val stateMachine = ExecutionStateMachine()
    private val logger = Logger()

    private lateinit var tvAgentStatus: TextView
    private lateinit var tvExecutionState: TextView
    private lateinit var tvAccessibilityStatus: TextView
    private lateinit var btnEnableAccessibility: Button
    private lateinit var tvLogArea: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvAgentStatus = findViewById(R.id.tvAgentStatus)
        tvExecutionState = findViewById(R.id.tvExecutionState)
        tvAccessibilityStatus = findViewById(R.id.tvAccessibilityStatus)
        btnEnableAccessibility = findViewById(R.id.btnEnableAccessibility)
        tvLogArea = findViewById(R.id.tvLogArea)

        btnEnableAccessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        logger.i("UI", "Control plane UI launched.")
        updateUIState()
    }

    override fun onResume() {
        super.onResume()
        updateUIState()
    }

    private fun updateUIState() {
        val currentState = stateMachine.currentState
        tvAgentStatus.text = "Agent Status: ${currentState.name}"
        tvExecutionState.text = "Execution State: ${currentState.name}"

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

    private fun refreshLogs() {
        val sb = StringBuilder()
        for (log in logger.getLogs()) {
            sb.append("[${log.category}] ${log.message}\n")
        }
        tvLogArea.text = if (sb.isNotEmpty()) sb.toString() else "[SYSTEM] Phase 0 LocalAgent initialized."
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
