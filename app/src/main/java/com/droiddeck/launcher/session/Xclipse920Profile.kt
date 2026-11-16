package com.droiddeck.launcher.session

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.droiddeck.launcher.gpu.SystemVulkanDriver
import com.droiddeck.launcher.gpu.VulkanInfo

object Xclipse920Profile {
    internal const val APPLIED = "xclipse920ProfileV1"
    private const val SNAPSHOT = "snapshotSaved"

    fun supported(): Boolean = Xclipse920Policy.matches(
            SystemVulkanDriver.isXclipse(), Build.HARDWARE,
            if (Build.VERSION.SDK_INT >= 31) Build.SOC_MODEL else "",
            VulkanInfo.cachedOrNull()?.get("device").orEmpty(),
        )

    fun initialize(context: Context) {
        if (!supported()) return
        apply(
            context.getSharedPreferences("session", Context.MODE_PRIVATE),
            context.getSharedPreferences("frame_gen", Context.MODE_PRIVATE),
            context.getSharedPreferences("xclipse920-backup-session", Context.MODE_PRIVATE),
            context.getSharedPreferences("xclipse920-backup-frame-gen", Context.MODE_PRIVATE),
        )
    }

    fun restore(context: Context): Boolean {
        if (SessionState.running) return false
        return restore(
            context.getSharedPreferences("session", Context.MODE_PRIVATE),
            context.getSharedPreferences("frame_gen", Context.MODE_PRIVATE),
            context.getSharedPreferences("xclipse920-backup-session", Context.MODE_PRIVATE),
            context.getSharedPreferences("xclipse920-backup-frame-gen", Context.MODE_PRIVATE),
        )
    }

    @Synchronized
    internal fun restore(
        session: SharedPreferences, frameGen: SharedPreferences,
        sessionBackup: SharedPreferences, frameGenBackup: SharedPreferences,
    ): Boolean {
        if (!sessionBackup.getBoolean(SNAPSHOT, false) || !frameGenBackup.getBoolean(SNAPSHOT, false)) return false
        fun restoreKeys(target: SharedPreferences, backup: SharedPreferences, keys: Set<String>): Boolean {
            return write(target, backup.all.filterKeys { it in keys }.mapValues { requireNotNull(it.value) }, keys)
        }
        if (!restoreKeys(frameGen, frameGenBackup, Xclipse920Policy.frameGen.keys)) return false
        if (!restoreKeys(session, sessionBackup, Xclipse920Policy.session.keys)) return false
        return write(session, mapOf(APPLIED to true, "xclipse920ProfileActive" to false))
    }

    @Synchronized
    internal fun apply(
        session: SharedPreferences, frameGen: SharedPreferences,
        sessionBackup: SharedPreferences, frameGenBackup: SharedPreferences,
    ): Boolean {
        if (session.getBoolean(APPLIED, false)) return false
        if (!snapshot(session, sessionBackup, Xclipse920Policy.session.keys) ||
            !snapshot(frameGen, frameGenBackup, Xclipse920Policy.frameGen.keys)) return false
        if (!write(frameGen, Xclipse920Policy.frameGen)) return false
        return write(session, Xclipse920Policy.session + mapOf(APPLIED to true, "xclipse920ProfileActive" to true))
    }

    private fun snapshot(source: SharedPreferences, backup: SharedPreferences, keys: Set<String>): Boolean {
        if (backup.getBoolean(SNAPSHOT, false)) return true
        return write(backup, source.all.filterKeys { it in keys }.mapValues { requireNotNull(it.value) } + (SNAPSHOT to true))
    }

    internal fun write(prefs: SharedPreferences, values: Map<String, Any>, remove: Set<String> = emptySet()): Boolean {
        val editor = prefs.edit()
        remove.forEach { editor.remove(it) }
        for ((key, value) in values) when (value) {
            is Boolean -> editor.putBoolean(key, value)
            is Int -> editor.putInt(key, value)
            is String -> editor.putString(key, value)
            is Long -> editor.putLong(key, value)
            is Float -> editor.putFloat(key, value)
            is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
            else -> error("Unsupported preference type for $key")
        }
        return editor.commit()
    }
}
