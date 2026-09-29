package com.agent.android.storage

import android.net.Uri

interface LearningStore {
    fun initialize(storageUri: Uri?): Boolean
    fun readRecord(recordId: String): String?
    fun appendRecord(recordId: String, content: String): Boolean
    fun updateRecord(recordId: String, content: String): Boolean
    fun exportData(): String
    fun importData(data: String): Boolean
    fun integrityCheck(): Boolean
    fun isStorageAvailable(): Boolean
}
