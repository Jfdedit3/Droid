package com.droiddeck.launcher.gpu

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.widget.Toast
import com.droiddeck.launcher.R
import com.droiddeck.launcher.wayland.CompositorHost
import com.droiddeck.launcher.wayland.WaylandCompositor

internal object LightFrameGenRuntime {
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private var pending: Runnable? = null
    private var generation = 0L
    @Volatile private var armed = false
    val generating: Boolean get() = armed

    fun stop() {
        ++generation
        pending?.let { handler.removeCallbacks(it) }
        pending = null
        if (armed) WaylandCompositor.nativeSetFrameGenArmed(false, 0, 0)
        armed = false
    }

    fun start(context: Context, refreshHz: Float) {
        stop()
        val token = generation
        val app = context.applicationContext
        val power = app.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val policy = LightFrameGenPolicy()
        var lastFrames = WaylandCompositor.gameFrameCount()
        var lastMs = SystemClock.elapsedRealtime()
        val task = object : Runnable {
            override fun run() {
                synchronized(FrameGen) {
                if (token != generation) return@synchronized
                val now = SystemClock.elapsedRealtime()
                val frames = WaylandCompositor.gameFrameCount()
                val elapsed = now - lastMs
                val fps = if (elapsed > 0 && frames >= lastFrames) (frames - lastFrames) * 1000f / elapsed else 0f
                lastFrames = frames
                lastMs = now
                val available = CompositorHost.isStarted
                val problem = if (available) WaylandCompositor.nativeFrameGenProblem() else -1
                if (problem > 0) {
                    stop()
                    Toast.makeText(app, R.string.frame_gen_light_unavailable, Toast.LENGTH_LONG).show()
                    return@synchronized
                }
                val thermal = if (Build.VERSION.SDK_INT >= 29)
                    runCatching { power?.currentThermalStatus ?: 0 }.getOrDefault(2) else 0
                val interactive = runCatching { power?.isInteractive != false }.getOrDefault(false)
                val powerSave = runCatching { power?.isPowerSaveMode == true }.getOrDefault(true)
                val next = policy.sample(
                    if (available && problem == 0 && interactive) fps else 0f,
                    refreshHz, thermal, powerSave, now,
                )
                if (next != armed) {
                    armed = next
                    WaylandCompositor.nativeSetFrameGenArmed(next, 2, 0)
                }
                handler.postDelayed(this, 1_000L)
                }
            }
        }
        pending = task
        handler.postDelayed(task, 1_000L)
    }
}
