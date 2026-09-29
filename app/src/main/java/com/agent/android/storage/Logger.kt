package com.agent.android.storage

import java.util.ArrayDeque
import java.util.Deque

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}

data class LogEntry(
    val level: LogLevel,
    val category: String,
    val message: String,
    val timestampMs: Long = System.currentTimeMillis()
)

class Logger(private val maxCapacity: Int = 100) {
    private val buffer: Deque<LogEntry> = ArrayDeque()

    @Synchronized
    fun log(level: LogLevel, category: String, message: String) {
        if (buffer.size >= maxCapacity) {
            buffer.pollFirst()
        }
        buffer.addLast(LogEntry(level, category, message))
    }

    fun d(category: String, message: String) = log(LogLevel.DEBUG, category, message)
    fun i(category: String, message: String) = log(LogLevel.INFO, category, message)
    fun w(category: String, message: String) = log(LogLevel.WARN, category, message)
    fun e(category: String, message: String) = log(LogLevel.ERROR, category, message)

    @Synchronized
    fun getLogs(): List<LogEntry> = buffer.toList()

    @Synchronized
    fun clear() = buffer.clear()

    fun capacity(): Int = maxCapacity
    @Synchronized
    fun size(): Int = buffer.size
}
