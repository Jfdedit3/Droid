package com.droiddeck.launcher.runtime
import android.content.Context
import java.io.File
object LinuxRuntime { fun logDir(context: Context) = File(context.filesDir, "logs") }
