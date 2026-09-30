package com.agent.android.agent.device

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.agent.android.agent.skills.SkillResult
import com.agent.android.agent.skills.SkillStatus

class ClipboardController(private val context: Context?) {

    fun getClipboardStatus(): SkillResult {
        val start = System.currentTimeMillis()
        if (context == null) return SkillResult("CLIPBOARD", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")

        return try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: return SkillResult("CLIPBOARD", SkillStatus.UNAVAILABLE, "ClipboardManager unavailable", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")

            val hasPrimaryClip = cm.hasPrimaryClip()
            val text = if (hasPrimaryClip && (cm.primaryClip?.itemCount ?: 0) > 0) {
                cm.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString() ?: ""
            } else ""

            val stateMsg = if (hasPrimaryClip && text.isNotEmpty()) "Clip present (${text.length} chars)" else "Clipboard EMPTY"
            SkillResult("CLIPBOARD", SkillStatus.SUCCESS, "CLIPBOARD_STATUS: $stateMsg", System.currentTimeMillis() - start)
        } catch (e: Exception) {
            SkillResult("CLIPBOARD", SkillStatus.FAILED, "Error querying clipboard: ${e.message}", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")
        }
    }

    fun readClipboard(): SkillResult {
        val start = System.currentTimeMillis()
        if (context == null) return SkillResult("CLIPBOARD", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")

        return try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: return SkillResult("CLIPBOARD", SkillStatus.UNAVAILABLE, "ClipboardManager unavailable", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")

            if (!cm.hasPrimaryClip()) {
                return SkillResult("CLIPBOARD", SkillStatus.SUCCESS, "Clipboard is EMPTY", System.currentTimeMillis() - start)
            }

            val text = cm.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString() ?: ""
            SkillResult("CLIPBOARD", SkillStatus.SUCCESS, "Clipboard content: '$text'", System.currentTimeMillis() - start)
        } catch (e: Exception) {
            SkillResult("CLIPBOARD", SkillStatus.FAILED, "Error reading clipboard: ${e.message}", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")
        }
    }

    fun writeClipboard(text: String): SkillResult {
        val start = System.currentTimeMillis()
        if (context == null) return SkillResult("CLIPBOARD", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")

        return try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: return SkillResult("CLIPBOARD", SkillStatus.UNAVAILABLE, "ClipboardManager unavailable", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")

            val clip = ClipData.newPlainText("LocalAgentClip", text)
            cm.setPrimaryClip(clip)

            SkillResult("CLIPBOARD", SkillStatus.SUCCESS, "Copied text to clipboard (${text.length} chars)", System.currentTimeMillis() - start)
        } catch (e: Exception) {
            SkillResult("CLIPBOARD", SkillStatus.FAILED, "Error writing clipboard: ${e.message}", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")
        }
    }

    fun clearClipboard(): SkillResult {
        val start = System.currentTimeMillis()
        if (context == null) return SkillResult("CLIPBOARD", SkillStatus.UNAVAILABLE, "Context unavailable", System.currentTimeMillis() - start, "NO_CONTEXT")

        return try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: return SkillResult("CLIPBOARD", SkillStatus.UNAVAILABLE, "ClipboardManager unavailable", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")

            val clip = ClipData.newPlainText("", "")
            cm.setPrimaryClip(clip)

            SkillResult("CLIPBOARD", SkillStatus.SUCCESS, "Clipboard cleared", System.currentTimeMillis() - start)
        } catch (e: Exception) {
            SkillResult("CLIPBOARD", SkillStatus.FAILED, "Error clearing clipboard: ${e.message}", System.currentTimeMillis() - start, "HARDWARE_UNAVAILABLE")
        }
    }
}
