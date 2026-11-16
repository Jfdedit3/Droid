package com.droiddeck.launcher.session
import android.content.Context
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FailureLogsTest {
    @get:Rule val tmp = TemporaryFolder()
    private fun context() = Context(tmp.newFolder(), tmp.newFolder())
    private fun session(context: Context, name: String): File {
        SessionPaths.take()
        val folder = SessionPaths.beginOrCurrent(context, name)
        File(folder, "xwayland-debian.log").writeText("display failed")
        SessionPaths.take()
        return folder
    }
    @Test fun failedSessionRemainsShareableWithLoggingOff() {
        val context = context()
        val folder = session(context, "desktop")
        SessionPaths.release(context, folder, 70)
        assertTrue(folder.isDirectory)
        assertEquals(folder, SessionPaths.lastEnded())
        assertTrue(SessionPaths.sessionFolders(context).contains(folder))
        assertEquals("display failed", File(folder, "xwayland-debian.log").readText())
    }
    @Test fun steamFailureRemainsShareableAfterClosingTheDesktopNormally() {
        val context = context()
        val folder = session(context, "desktop")
        File(folder, "steam-failed").writeText("exit=139\n")
        File(folder, "steam-fedora.log").writeText("Steam failed")
        SessionPaths.release(context, folder, 0)
        assertTrue(SessionPaths.sessionFolders(context).contains(folder))
        assertEquals("exit=139\n", File(folder, ".failed-session").readText())
        assertTrue(File(folder, "steam-fedora.log").isFile)
    }
    @Test fun successfulTemporarySessionIsStillDiscarded() {
        val context = context()
        val folder = session(context, "desktop")
        SessionPaths.release(context, folder, 0)
        assertFalse(folder.exists())
    }
    @Test fun onlyThreeLatestFailuresAreKept() {
        val context = context()
        val folders = (1..5).map { n -> session(context, "desktop$n").also { SessionPaths.release(context, it, 70) } }
        assertEquals(folders.takeLast(3), SessionPaths.sessionFolders(context))
        assertTrue(folders.take(2).none { it.exists() })
    }
    @Test fun collectingAnOldFailureDoesNotClearTheNewSession() {
        val context = context()
        val old = session(context, "old")
        val current = SessionPaths.beginOrCurrent(context, "new")
        try {
            SessionPaths.release(context, old, 70)
            assertEquals(current, SessionPaths.current())
            assertTrue(SessionPaths.sessionFolders(context).contains(old))
        } finally { SessionPaths.take(); SessionPaths.release(context, current) }
    }
}
