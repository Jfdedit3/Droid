package com.droiddeck.launcher.session

object Xclipse920Policy {
    fun matches(xclipse: Boolean, hardware: String, soc: String, gpu: String): Boolean =
        xclipse && (listOf(hardware, soc).any {
            it.equals("s5e9925", true) || Regex("(?i)(samsung)?exynos[ _-]*2200").matches(it.trim())
        } ||
            Regex("(?i).*\\bxclipse\\s*920\\b.*").matches(gpu))

    val session: Map<String, Any> = linkedMapOf(
        "displayResolution.steam" to "540p",
        "displayResolution.run" to "540p",
        "displayResolution.lxqt" to "720p",
        "hdr.steam" to false, "hdr.run" to false, "hdr.lxqt" to false,
        "upscaler" to 0,
        "screenEffects" to "{}",
        "textureAnisotropy" to 0, "textureLodBias" to "0",
        "gpuDriverMode" to "auto",
        "androidDriver" to "", "linuxDriver" to "",
        "linuxDriver.steam" to "", "linuxDriver.desktop" to "",
        "vkBridge" to false, "venus" to false, "steamGpuCompositing" to false,
        "fexPreset" to "PERFORMANCE_TSO", "forceSsbs" to false,
        "fastSync" to true, "fsyncFirst" to false, "syncFallback" to true,
        "prootFastPath" to true, "prootNoSeccomp" to false,
        "clientCpusOverride" to false, "clientCpus" to "", "gameCpus" to "",
        "gameThreadHints" to true,
        "glThread" to true, "noGlError" to true, "zinkLazy" to true,
        "gamescopeRealtime" to false, "gpuClockPin" to false,
        "ramCapMb" to 0, "vramCapMb" to 0, "zramClient" to true,
        "directAudio" to true, "lowLatency" to true,
        "mangoapp" to false, "hud" to false, "hudDeck" to false, "bottomHud" to false,
        "memoryHud" to "off", "telemetryAuto" to false,
        "animations" to false,
        "storageDiagnostics" to false, "vac_diagnostics" to false,
        "logs" to false, "gameDebugLogsEnabled" to false,
        "detailedGraphicsLogs" to false,
        "steamDownloadsInBackground" to false,
        "vsyncThread" to true, "renderOnCommit" to true,
    )

    val frameGen: Map<String, Any> = mapOf("engine" to "off", "target" to 0, "multiplier" to 2)
}
