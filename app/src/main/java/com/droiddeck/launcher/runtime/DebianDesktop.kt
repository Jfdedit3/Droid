package com.droiddeck.launcher.runtime

import android.content.Context
import com.droiddeck.launcher.core.FileUtils
import java.io.File

object DebianDesktop {
    fun installed(context: Context) = File(context.filesDir, "debian-xfce").isDirectory

    @Synchronized
    fun remove(context: Context) {
        FileUtils.delete(File(context.filesDir, "debian-xfce"))
        FileUtils.delete(File(context.filesDir, ".debian-xfce-staging"))
    }
}
