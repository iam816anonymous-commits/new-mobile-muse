package com.agent.android.storage

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

data class PersistentLogEntry(
    val id: Long = 0L,
    val timestampMs: Long = System.currentTimeMillis(),
    val correlationId: String,
    val source: String,
    val category: String,
    val level: LogLevel,
    val message: String,
    val detailsJson: String? = null
)

class LocalAgentLogger(context: Context?) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    private val counter = AtomicLong(0L)

    override fun onCreate(db: SQLiteDatabase) {
        val createTableSql = """
            CREATE TABLE IF NOT EXISTS $TABLE_LOGS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TIMESTAMP INTEGER NOT NULL,
                $COL_CORRELATION_ID TEXT NOT NULL,
                $COL_SOURCE TEXT NOT NULL,
                $COL_CATEGORY TEXT NOT NULL,
                $COL_LEVEL TEXT NOT NULL,
                $COL_MESSAGE TEXT NOT NULL,
                $COL_DETAILS_JSON TEXT
            )
        """.trimIndent()
        db.execSQL(createTableSql)
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_correlation ON $TABLE_LOGS ($COL_CORRELATION_ID)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_timestamp ON $TABLE_LOGS ($COL_TIMESTAMP)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LOGS")
        onCreate(db)
    }

    @Synchronized
    fun generateCorrelationId(): String {
        val uuid = UUID.randomUUID().toString().replace("-", "").take(8).uppercase()
        val seq = counter.incrementAndGet() % 1000
        return "$uuid-$seq"
    }

    @Synchronized
    fun log(
        source: String,
        level: LogLevel,
        category: String,
        message: String,
        detailsJson: String? = null,
        correlationId: String = "SYSTEM"
    ): Long {
        val sanitizedMessage = sanitizeSensitiveData(message)
        val sanitizedDetails = detailsJson?.let { sanitizeSensitiveData(it) }

        // Also output to Logcat for live debugging
        when (level) {
            LogLevel.DEBUG -> Log.d(category, "[$source][$correlationId] $sanitizedMessage")
            LogLevel.INFO -> Log.i(category, "[$source][$correlationId] $sanitizedMessage")
            LogLevel.WARN -> Log.w(category, "[$source][$correlationId] $sanitizedMessage")
            LogLevel.ERROR -> Log.e(category, "[$source][$correlationId] $sanitizedMessage")
        }

        return try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COL_TIMESTAMP, System.currentTimeMillis())
                put(COL_CORRELATION_ID, correlationId)
                put(COL_SOURCE, source)
                put(COL_CATEGORY, category)
                put(COL_LEVEL, level.name)
                put(COL_MESSAGE, sanitizedMessage)
                put(COL_DETAILS_JSON, sanitizedDetails)
            }
            val rowId = db.insert(TABLE_LOGS, null, values)
            trimOldLogsIfNeeded(db)
            rowId
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist log entry to SQLite: ${e.message}")
            -1L
        }
    }

    fun d(category: String, message: String, correlationId: String = "SYSTEM", source: String = "INTERNAL") =
        log(source, LogLevel.DEBUG, category, message, null, correlationId)

    fun i(category: String, message: String, correlationId: String = "SYSTEM", source: String = "INTERNAL") =
        log(source, LogLevel.INFO, category, message, null, correlationId)

    fun w(category: String, message: String, correlationId: String = "SYSTEM", source: String = "INTERNAL") =
        log(source, LogLevel.WARN, category, message, null, correlationId)

    fun e(category: String, message: String, correlationId: String = "SYSTEM", source: String = "INTERNAL") =
        log(source, LogLevel.ERROR, category, message, null, correlationId)

    @Synchronized
    fun getRecentLogs(limit: Int = 100): List<PersistentLogEntry> {
        val list = mutableListOf<PersistentLogEntry>()
        try {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_LOGS,
                null,
                null,
                null,
                null,
                null,
                "$COL_ID DESC",
                limit.toString()
            )
            cursor.use { c ->
                while (c.moveToNext()) {
                    list.add(cursorToEntry(c))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying recent logs: ${e.message}")
        }
        return list
    }

    @Synchronized
    fun getErrorLogs(limit: Int = 50): List<PersistentLogEntry> {
        val list = mutableListOf<PersistentLogEntry>()
        try {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_LOGS,
                null,
                "$COL_LEVEL = ?",
                arrayOf(LogLevel.ERROR.name),
                null,
                null,
                "$COL_ID DESC",
                limit.toString()
            )
            cursor.use { c ->
                while (c.moveToNext()) {
                    list.add(cursorToEntry(c))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying error logs: ${e.message}")
        }
        return list
    }

    @Synchronized
    fun getLogsForCorrelationId(correlationId: String): List<PersistentLogEntry> {
        val list = mutableListOf<PersistentLogEntry>()
        try {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_LOGS,
                null,
                "$COL_CORRELATION_ID = ?",
                arrayOf(correlationId),
                null,
                null,
                "$COL_ID ASC",
                null
            )
            cursor.use { c ->
                while (c.moveToNext()) {
                    list.add(cursorToEntry(c))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying correlation logs: ${e.message}")
        }
        return list
    }

    @Synchronized
    fun clearLogs() {
        try {
            writableDatabase.delete(TABLE_LOGS, null, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing logs: ${e.message}")
        }
    }

    @Synchronized
    fun getLogCount(): Long {
        return try {
            val db = readableDatabase
            val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_LOGS", null)
            cursor.use { c ->
                if (c.moveToFirst()) c.getLong(0) else 0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    private fun trimOldLogsIfNeeded(db: SQLiteDatabase) {
        try {
            val countCursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_LOGS", null)
            val currentCount = countCursor.use { c ->
                if (c.moveToFirst()) c.getLong(0) else 0L
            }
            if (currentCount > MAX_RETENTION_ROWS) {
                val deleteSql = """
                    DELETE FROM $TABLE_LOGS WHERE $COL_ID IN (
                        SELECT $COL_ID FROM $TABLE_LOGS
                        WHERE $COL_LEVEL IN ('DEBUG', 'INFO')
                        ORDER BY $COL_ID ASC
                        LIMIT ${currentCount - MAX_RETENTION_ROWS + 100}
                    )
                """.trimIndent()
                db.execSQL(deleteSql)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error auto-trimming log table: ${e.message}")
        }
    }

    private fun cursorToEntry(c: android.database.Cursor): PersistentLogEntry {
        val id = c.getLong(c.getColumnIndexOrThrow(COL_ID))
        val time = c.getLong(c.getColumnIndexOrThrow(COL_TIMESTAMP))
        val cid = c.getString(c.getColumnIndexOrThrow(COL_CORRELATION_ID))
        val src = c.getString(c.getColumnIndexOrThrow(COL_SOURCE))
        val cat = c.getString(c.getColumnIndexOrThrow(COL_CATEGORY))
        val lvl = LogLevel.valueOf(c.getString(c.getColumnIndexOrThrow(COL_LEVEL)))
        val msg = c.getString(c.getColumnIndexOrThrow(COL_MESSAGE))
        val json = c.getString(c.getColumnIndexOrThrow(COL_DETAILS_JSON))
        return PersistentLogEntry(id, time, cid, src, cat, lvl, msg, json)
    }

    fun sanitizeSensitiveData(text: String): String {
        if (text.isBlank()) return text
        var s = text
        val sensitivePatterns = listOf(
            "(?i)password[\\s=:]+[^\\s]+" to "password=***REDACTED***",
            "(?i)secret[\\s=:]+[^\\s]+" to "secret=***REDACTED***",
            "(?i)token[\\s=:]+[^\\s]+" to "token=***REDACTED***",
            "(?i)pin[\\s=:]+[0-9]+" to "pin=***REDACTED***"
        )
        for ((pattern, replacement) in sensitivePatterns) {
            s = s.replace(pattern.toRegex(), replacement)
        }
        return s
    }

    companion object {
        private const val TAG = "LocalAgentLogger"
        private const val DATABASE_NAME = "localagent_logs.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_LOGS = "agent_logs"
        private const val MAX_RETENTION_ROWS = 1000L

        private const val COL_ID = "id"
        private const val COL_TIMESTAMP = "timestamp_ms"
        private const val COL_CORRELATION_ID = "correlation_id"
        private const val COL_SOURCE = "source"
        private const val COL_CATEGORY = "category"
        private const val COL_LEVEL = "level"
        private const val COL_MESSAGE = "message"
        private const val COL_DETAILS_JSON = "details_json"

        @Volatile
        var instance: LocalAgentLogger? = null
            private set

        fun getOrCreate(context: Context?): LocalAgentLogger {
            return instance ?: synchronized(this) {
                instance ?: LocalAgentLogger(context?.applicationContext ?: context).also { instance = it }
            }
        }
    }
}
