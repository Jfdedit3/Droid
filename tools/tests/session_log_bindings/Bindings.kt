package com.droiddeck.launcher.session
import android.content.Context
import java.io.File
object SessionState { var mode = "lxqt"; var program: String? = null }
object SessionPrefs { fun logsEnabled(context: Context) = false }
object EnhancedDiagnostics {
    fun isArmed(context: Context) = false
    fun clearRecordFor(context: Context, folder: File) {}
    fun consumeInto(context: Context, folder: File) = false
}
object SessionFiles { fun logDirectory(context: Context) = File(context.filesDir, "logs") }
object SessionService { const val MODE_RUN = "run"; const val MODE_DESKTOP = "lxqt" }
