package com.agent.android

import com.agent.android.agent.skills.SkillStatus
import com.agent.android.commands.CommandRegistry
import com.agent.android.execution.ExecutionController
import com.agent.android.execution.GoalDispatcherImpl
import com.agent.android.storage.LocalAgentLogger
import com.agent.android.storage.LogLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LocalAgentLoggerUnitTest {

    private lateinit var registry: CommandRegistry
    private lateinit var goalDispatcher: GoalDispatcherImpl
    private lateinit var executionController: ExecutionController
    private lateinit var logger: LocalAgentLogger

    @Before
    fun setUp() {
        registry = CommandRegistry()
        executionController = ExecutionController()
        logger = LocalAgentLogger(null)
        goalDispatcher = GoalDispatcherImpl(
            executionController = executionController,
            commandRegistry = registry,
            persistentLogger = logger
        )
    }

    @Test
    fun test1_CorrelationIdGeneration() {
        val cid1 = logger.generateCorrelationId()
        val cid2 = logger.generateCorrelationId()

        assertNotNull(cid1)
        assertNotNull(cid2)
        assertTrue(cid1 != cid2)
        assertTrue(cid1.contains("-"))
    }

    @Test
    fun test2_SensitiveDataSanitization() {
        val rawMsg = "User entered password secret123 with pin 4321"
        val sanitized = logger.sanitizeSensitiveData(rawMsg)

        assertTrue(sanitized.contains("password=***REDACTED***"))
        assertTrue(sanitized.contains("pin=***REDACTED***"))
        assertTrue(!sanitized.contains("secret123"))
        assertTrue(!sanitized.contains("4321"))
    }

    @Test
    fun test3_LogCommandRegistrations() {
        assertNotNull("logs.recent must be registered", registry.getCommandById("logs.recent"))
        assertNotNull("logs.errors must be registered", registry.getCommandById("logs.errors"))
        assertNotNull("logs.command must be registered", registry.getCommandById("logs.command"))
        assertNotNull("logs.clear must be registered", registry.getCommandById("logs.clear"))
    }

    @Test
    fun test4_LogCommandsExecution() {
        val detailsRecent = goalDispatcher.dispatchAndProcessWithLock("logs recent")
        assertEquals(SkillStatus.SUCCESS, detailsRecent.result.status)

        val detailsErrors = goalDispatcher.dispatchAndProcessWithLock("logs errors")
        assertEquals(SkillStatus.SUCCESS, detailsErrors.result.status)

        val detailsCmd = goalDispatcher.dispatchAndProcessWithLock("logs command 7F2A90C1")
        assertEquals(SkillStatus.SUCCESS, detailsCmd.result.status)

        val detailsClear = goalDispatcher.dispatchAndProcessWithLock("logs clear")
        assertEquals(SkillStatus.SUCCESS, detailsClear.result.status)
    }
}
