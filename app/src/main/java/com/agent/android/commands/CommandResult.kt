package com.agent.android.commands

import com.agent.android.agent.skills.SkillStatus

data class CommandResult(
    val commandId: String,
    val status: SkillStatus,
    val message: String,
    val errorCode: String? = null,
    val durationMs: Long = 0L,
    val verificationText: String? = null
)
