package com.agent.android.commands

data class CommandArguments(
    val commandId: String,
    val rawCommand: String,
    val params: Map<String, String> = emptyMap()
) {
    fun getString(key: String): String? = params[key]
    fun getInt(key: String, default: Int = 0): Int = params[key]?.toIntOrNull() ?: default
    fun getLong(key: String, default: Long = 0L): Long = params[key]?.toLongOrNull() ?: default
    fun getBoolean(key: String, default: Boolean = false): Boolean = params[key]?.toBooleanStrictOrNull() ?: default
}
