package com.droiddeck.launcher.runtime

import android.content.Context
import android.os.Environment
import android.system.Os
import com.droiddeck.launcher.R
import com.droiddeck.launcher.core.FileUtils
import com.droiddeck.launcher.core.RootfsArchive
import com.droiddeck.launcher.core.FedoraGamingPayload
import com.droiddeck.launcher.session.SessionService
import java.io.File
import java.security.MessageDigest

object FedoraDesktop {
    const val PROGRAM = "/usr/libexec/droiddeck-fedora/droiddeck-fedora-xfce"
    const val ASSET = "bundled/fedora-xfce-arm64.tgz"
    const val SHA256 = "6697d531d24394b6cac448b0d1dcd833826219fe7b6b18199dfc49257e2a2175"
    const val VERSION = "fedora43-xfce-v1"
    private const val MAX_EXPANDED = 2L * 1024 * 1024 * 1024

    fun selected(mode: String?, program: String?) = mode == SessionService.MODE_DESKTOP && program == PROGRAM
    fun root(context: Context) = File(context.filesDir, "fedora-xfce")
    fun installed(context: Context) = FileUtils.readString(File(root(context), ".droiddeck-version")) == VERSION &&
        File(root(context), "usr/bin/xfce4-session").canExecute() && File(root(context), "usr/bin/Xwayland").canExecute()

    @Synchronized
    fun install(context: Context, progress: LinuxRuntimeInstaller.ProgressListener): String? {
        if (installed(context)) return null
        val target = root(context)
        val staging = File(context.filesDir, ".fedora-xfce-staging")
        try {
            check(!target.exists()) { "Incomplete Fedora installation; remove Fedora before retrying" }
            FileUtils.delete(staging)
            check(context.filesDir.usableSpace >= MAX_EXPANDED) { context.getString(R.string.fedora_space) }
            progress.onProgress(context.getString(R.string.fedora_verify), -1)
            val digest = MessageDigest.getInstance("SHA-256")
            context.assets.open(ASSET).buffered(65536).use { input ->
                val buffer = ByteArray(65536)
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    digest.update(buffer, 0, n)
                }
            }
            check(digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) } == SHA256) { "Fedora checksum mismatch" }
            progress.onProgress(context.getString(R.string.fedora_extract), -1)
            context.assets.open(ASSET).use { RootfsArchive.extract(it, staging, MAX_EXPANDED) }
            for (path in listOf("root", "tmp", "run/droiddeck", "dev/shm", "mnt/shared", "opt/brave.com/brave"))
                File(staging, path).mkdirs()
            Os.chmod(File(staging, "tmp").path, 0x3ff)
            for (name in listOf("droiddeck-fedora-xfce", "droiddeck-brave-browser")) {
                val output = File(staging, "usr/libexec/droiddeck-fedora/$name")
                check(FileUtils.copyAsset(context, "linuxfs/usr/local/bin/$name", output)) { "Missing desktop launcher" }
                check(output.setExecutable(true, false))
            }
            check(FileUtils.copyAsset(context, "linuxfs/usr/local/share/applications/droiddeck-brave.desktop",
                File(staging, "usr/share/applications/droiddeck-brave.desktop")))
            val browserEntry = File(staging, "usr/share/applications/droiddeck-brave.desktop")
            check(FileUtils.writeString(browserEntry, browserEntry.readText().replace(
                "/usr/local/bin/droiddeck-brave-browser", "/usr/libexec/droiddeck-fedora/droiddeck-brave-browser")))
            val uid = android.os.Process.myUid()
            for ((path, before, after) in listOf(
                Triple("etc/passwd", "root:x:0:0:", "root:x:$uid:$uid:"),
                Triple("etc/group", "root:x:0:", "root:x:$uid:"),
            )) {
                val file = File(staging, path)
                check(FileUtils.writeString(file, file.readText().replace(before, after)))
            }
            check(File(staging, "usr/bin/xfce4-session").canExecute() && File(staging, "usr/bin/Xwayland").canExecute())
            check(FileUtils.writeString(File(staging, ".droiddeck-version"), VERSION))
            check(staging.renameTo(target)) { "Cannot finish Fedora installation" }
            progress.onProgress(context.getString(R.string.fedora_ready), 100)
            return null
        } catch (e: Exception) {
            return e.message ?: e.toString()
        } finally {
            FileUtils.delete(staging)
        }
    }

    @Synchronized
    fun remove(context: Context) {
        FileUtils.delete(root(context))
        FileUtils.delete(File(context.filesDir, ".fedora-xfce-staging"))
    }

    fun command(context: Context, sessionRoot: File, runtimeDir: File, sessionDir: File,
                environment: List<String>, extraBinds: List<String>, terminal: Boolean = false): List<String> {
        check(installed(context)) { "Fedora/Xfce is not installed" }
        val ubuntu = LinuxRuntime.rootDir(context)
        if (!terminal) {
            val gamingHash = "c5cc15dfc3a630a8f68ba61e60e8e93d14a80eb682e53986e8be52717af19e17"
            if (!FedoraGamingPayload.ready(root(context), gamingHash)) {
                context.assets.open("bundled/fedora-gaming-arm64.tgz").use {
                    FedoraGamingPayload.install(it, root(context), File(context.filesDir, ".fedora-gaming-staging"), gamingHash)
                }
            }
            val launcher = File(root(context), PROGRAM.removePrefix("/"))
            check(FileUtils.copyAsset(context, "linuxfs/usr/local/bin/droiddeck-fedora-xfce", launcher)) { "Cannot update Fedora launcher" }
            check(launcher.setExecutable(true, false))
            val tools = File(root(context), "usr/libexec/droiddeck-fedora/droiddeck-fedora-tools")
            check(FileUtils.copyAsset(context, "linuxfs/usr/local/bin/droiddeck-fedora-tools", tools))
            check(tools.setExecutable(true, false))
            val steam = File(root(context), "usr/libexec/droiddeck-fedora/droiddeck-fedora-steam")
            check(FileUtils.copyAsset(context, "linuxfs/usr/local/bin/droiddeck-fedora-steam", steam))
            check(steam.setExecutable(true, false))
            // Xfce invokes this absolute path rather than looking up PATH.
            val iceauth = File(root(context), "usr/bin/iceauth")
            val iceauthReal = File(root(context), "usr/bin/iceauth.droiddeck-real")
            if (!iceauthReal.exists()) check(iceauth.renameTo(iceauthReal)) { "Cannot preserve Fedora iceauth" }
            check(FileUtils.copyAsset(context, "linuxfs/usr/local/bin/droiddeck-fedora-iceauth", iceauth))
            check(iceauth.setExecutable(true, false))
            val browser = File(root(context), "usr/libexec/droiddeck-fedora/droiddeck-brave-browser")
            check(FileUtils.copyAsset(context, "linuxfs/usr/local/bin/droiddeck-brave-browser", browser))
            check(browser.setExecutable(true, false))
            for (name in listOf("fedora-wallpaper.png", "fedora-steam.png")) {
                check(FileUtils.copyAsset(context, "linuxfs/usr/local/share/droiddeck/$name",
                    File(root(context), "usr/share/droiddeck-fedora/$name")))
            }
            for (relative in listOf("root/.local/share/Steam", "root/.config/droiddeck", "root/.local/share/droiddeck-fex"))
                File(ubuntu, relative).mkdirs()
            check(FileUtils.writeString(File(ubuntu, "etc/droiddeck/fedora-preload"),
                "/usr/local/lib/libddsession.so\n/usr/local/lib/libfakeinput.so\n"))
            BraveDesktop.install(context, ubuntu)
        }
        val binds = ArrayList(LinuxRuntime.binds(context, sessionRoot, runtimeDir,
            Environment.getExternalStorageDirectory(), extraBinds))
        fun share(relative: String) {
            val file = File(ubuntu, relative)
            if (file.exists()) binds.add("$file:/$relative")
        }
        for (relative in listOf("usr/local",
            "etc/droiddeck", "root/.local/share/Steam", "root/.local/share/droiddeck-fex",
            "root/.config/droiddeck", "root/.droiddeck-x64", "run/udev/data",
            "root/.dd-proton-extra", "root/.dd-steam-urls")) share(relative)
        for (kind in listOf("implicit_layer.d", "explicit_layer.d")) {
            File(ubuntu, "usr/share/vulkan/$kind").listFiles()?.filter {
                it.isFile && it.name.lowercase().contains("droiddeck") && it.extension == "json"
            }?.forEach { share("usr/share/vulkan/$kind/${it.name}") }
        }
        File(ubuntu, "opt").listFiles()?.forEach { share("opt/${it.name}") }
        val shared = Environment.getExternalStorageDirectory()
        if (shared.isDirectory) binds.add("$shared:/mnt/shared")
        val guest = arrayListOf("/usr/bin/env", "-i", "HOME=/root", "USER=root", "LOGNAME=root",
            "PATH=/usr/libexec/droiddeck-fedora:${LinuxRuntime.GUEST_PATH}", "LANG=C.UTF-8", "TERM=xterm-256color",
            "XDG_RUNTIME_DIR=/run/droiddeck", "WAYLAND_DISPLAY=wayland-0")
        guest.addAll(environment.filter { it.contains('=') && !it.startsWith("LD_PRELOAD=") &&
            it.substringBefore('=') !in setOf("HOME", "USER", "LOGNAME", "PATH", "XDG_RUNTIME_DIR", "WAYLAND_DISPLAY", "XDG_SESSION_TYPE", "DISPLAY") })
        if (terminal) guest.addAll(listOf("DISPLAY=:1", "/bin/bash", "-i")) else guest.add(PROGRAM)
        if (!terminal) LinuxRuntime.rememberSession(root(context), binds)
        return LinuxRuntime.commandWithBinds(context, root(context), binds, guest)
    }
}
