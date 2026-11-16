package com.droiddeck.launcher.session

import android.content.SharedPreferences
import java.lang.reflect.Proxy
import org.junit.Assert.*
import org.junit.Test

class Xclipse920ProfileTest {
    private class Memory(initial: Map<String, Any> = emptyMap(), var fail: Boolean = false) {
        val values = initial.toMutableMap()
        val prefs: SharedPreferences = Proxy.newProxyInstance(
            SharedPreferences::class.java.classLoader, arrayOf(SharedPreferences::class.java),
        ) { _, method, args ->
            when (method.name) {
                "getAll" -> values.toMap()
                "getBoolean" -> values[args!![0]] ?: args[1]
                "edit" -> editor()
                else -> error("Unexpected preference call: ${method.name}")
            }
        } as SharedPreferences

        private fun editor(): SharedPreferences.Editor {
            val pending = mutableMapOf<String, Any>()
            val removed = mutableSetOf<String>()
            lateinit var proxy: SharedPreferences.Editor
            proxy = Proxy.newProxyInstance(
                SharedPreferences.Editor::class.java.classLoader, arrayOf(SharedPreferences.Editor::class.java),
            ) { _, method, args ->
                when {
                    method.name.startsWith("put") -> { pending[args!![0] as String] = args[1]; proxy }
                    method.name == "remove" -> { removed += args!![0] as String; proxy }
                    method.name == "commit" -> {
                        if (!fail) { removed.forEach { values.remove(it) }; values.putAll(pending) }
                        !fail
                    }
                    else -> error("Unexpected editor call: ${method.name}")
                }
            } as SharedPreferences.Editor
            return proxy
        }
    }

    @Test fun detectionRequiresXclipseAndIdentifiesOnlyThe920() {
        assertTrue(Xclipse920Policy.matches(true, "s5e9925", "", ""))
        assertTrue(Xclipse920Policy.matches(true, "", "Exynos 2200", ""))
        assertTrue(Xclipse920Policy.matches(true, "", "S5E9925", ""))
        assertTrue(Xclipse920Policy.matches(true, "samsungexynos2200", "", ""))
        assertTrue(Xclipse920Policy.matches(true, "", "", "Samsung Xclipse 920"))
        assertFalse(Xclipse920Policy.matches(false, "s5e9925", "Exynos 2200", "Xclipse 920"))
        assertFalse(Xclipse920Policy.matches(true, "s5e9945", "Exynos 2400", "Xclipse 940"))
        assertFalse(Xclipse920Policy.matches(true, "", "", "Xclipse"))
        assertFalse(Xclipse920Policy.matches(true, "", "", "Xclipse 9200"))
    }

    @Test fun applyingOnceChangesExistingSettingsButPreservesLaterEditsAndUnrelatedData() {
        val session = Memory(mapOf("upscaler" to 4, "displayResolution.steam" to "1080p", "account" to "keep"))
        val frame = Memory(mapOf("engine" to "lsfg", "other" to true))
        val backup = Memory(); val frameBackup = Memory()
        assertTrue(Xclipse920Profile.apply(session.prefs, frame.prefs, backup.prefs, frameBackup.prefs))
        assertEquals("540p", session.values["displayResolution.steam"])
        assertEquals(0, session.values["upscaler"])
        assertEquals("off", frame.values["engine"])
        assertEquals("keep", session.values["account"])
        assertEquals(true, frame.values["other"])
        session.values["upscaler"] = 4
        session.values["gpuDriverMode"] = "manual"
        session.values["androidDriver"] = "radv-display"
        session.values["linuxDriver"] = "radv-runtime"
        assertFalse(Xclipse920Profile.apply(session.prefs, frame.prefs, backup.prefs, frameBackup.prefs))
        assertEquals(4, session.values["upscaler"])
        assertEquals("1080p", backup.values["displayResolution.steam"])
        assertEquals("manual", session.values["gpuDriverMode"])
        assertEquals("radv-display", session.values["androidDriver"])
        assertEquals("radv-runtime", session.values["linuxDriver"])
    }

    @Test fun restoreReinstatesMissingAndPresentKeysAndDoesNotReapplyOnNextStart() {
        val session = Memory(mapOf("upscaler" to 4, "account" to "keep"))
        val frame = Memory(mapOf("engine" to "lsfg"))
        val backup = Memory(); val frameBackup = Memory()
        assertTrue(Xclipse920Profile.apply(session.prefs, frame.prefs, backup.prefs, frameBackup.prefs))
        assertTrue(Xclipse920Profile.restore(session.prefs, frame.prefs, backup.prefs, frameBackup.prefs))
        assertEquals(4, session.values["upscaler"])
        assertFalse(session.values.containsKey("ramCapMb"))
        assertFalse(session.values.containsKey("displayResolution.steam"))
        assertEquals("lsfg", frame.values["engine"])
        assertEquals("keep", session.values["account"])
        assertFalse(Xclipse920Profile.apply(session.prefs, frame.prefs, backup.prefs, frameBackup.prefs))
    }

    @Test fun aFailedBackupLeavesTheOriginalSettingsAlone() {
        val session = Memory(mapOf("upscaler" to 4)); val frame = Memory(mapOf("engine" to "lsfg"))
        assertFalse(Xclipse920Profile.apply(session.prefs, frame.prefs, Memory(fail = true).prefs, Memory().prefs))
        assertEquals(mapOf("upscaler" to 4), session.values)
        assertEquals(mapOf("engine" to "lsfg"), frame.values)
    }

    @Test fun aFailedApplyCanRetryWithoutOverwritingTheOriginalSnapshot() {
        val session = Memory(mapOf("upscaler" to 4), fail = true)
        val frame = Memory(mapOf("engine" to "lsfg"))
        val backup = Memory(); val frameBackup = Memory()
        assertFalse(Xclipse920Profile.apply(session.prefs, frame.prefs, backup.prefs, frameBackup.prefs))
        session.fail = false
        assertTrue(Xclipse920Profile.apply(session.prefs, frame.prefs, backup.prefs, frameBackup.prefs))
        assertTrue(Xclipse920Profile.restore(session.prefs, frame.prefs, backup.prefs, frameBackup.prefs))
        assertEquals("lsfg", frame.values["engine"])
        assertEquals(4, session.values["upscaler"])
    }

    @Test fun restoringWithoutABackupDoesNothing() {
        val session = Memory(mapOf("upscaler" to 4))
        assertFalse(Xclipse920Profile.restore(session.prefs, Memory().prefs, Memory().prefs, Memory().prefs))
        assertEquals(mapOf("upscaler" to 4), session.values)
    }
}
