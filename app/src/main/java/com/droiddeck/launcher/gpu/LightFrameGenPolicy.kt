package com.droiddeck.launcher.gpu

class LightFrameGenPolicy {
    var enabled = false
        private set
    private var stableSamples = 0
    private var baselineFps = 0f
    private var retryAtMs = 0L

    fun sample(sourceFps: Float, refreshHz: Float, thermalStatus: Int, powerSave: Boolean, nowMs: Long): Boolean {
        if (!sourceFps.isFinite() || !refreshHz.isFinite() || refreshHz < 1f || sourceFps < 0f) {
            enabled = false
            stableSamples = 0
            return false
        }
        if (powerSave || thermalStatus >= 2) {
            enabled = false
            stableSamples = 0
            retryAtMs = nowMs + 15_000L
            return false
        }
        if (enabled && sourceFps > 0f && sourceFps < baselineFps * 0.8f) {
            enabled = false
            stableSamples = 0
            retryAtMs = nowMs + 30_000L
            return false
        }
        if (sourceFps < 18f || sourceFps >= refreshHz * 0.75f) {
            enabled = false
            stableSamples = 0
            return false
        }
        if (enabled) return true
        if (nowMs < retryAtMs || sourceFps < 24f || sourceFps > refreshHz * 0.55f) {
            stableSamples = 0
            return false
        }
        if (++stableSamples >= 3) {
            baselineFps = sourceFps
            enabled = true
        }
        return enabled
    }
}
