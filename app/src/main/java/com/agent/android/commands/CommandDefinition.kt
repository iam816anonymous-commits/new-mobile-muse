package com.agent.android.commands

data class CommandDefinition(
    val commandId: String,
    val name: String,
    val category: CommandCategory,
    val description: String,
    val status: CommandStatus,
    val syntax: String,
    val examples: List<String>,
    val argumentSchema: List<String> = emptyList(),
    val requirement: CommandRequirement = CommandRequirement(),
    val handlerIdentifier: String
)
