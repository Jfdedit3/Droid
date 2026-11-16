package com.droiddeck.launcher.gpu

import org.junit.Assert.*
import org.junit.Test

class LightFrameGenPolicyTest {
    private fun ready(policy: LightFrameGenPolicy, fps: Float = 30f, hz: Float = 60f, start: Long = 0) {
        assertFalse(policy.sample(fps, hz, 0, false, start + 1_000))
        assertFalse(policy.sample(fps, hz, 0, false, start + 2_000))
        assertTrue(policy.sample(fps, hz, 0, false, start + 3_000))
    }

    @Test fun waitsForStableSourceFramesBeforeAddingGpuWork() {
        val policy = LightFrameGenPolicy()
        assertFalse(policy.enabled)
        ready(policy)
    }

    @Test fun skipsLowFpsHighFpsAndSlowDisplays() {
        for ((fps, hz) in listOf(12f to 60f, 60f to 60f, 100f to 120f, 25f to 30f)) {
            val policy = LightFrameGenPolicy()
            repeat(5) { assertFalse(policy.sample(fps, hz, 0, false, it * 1_000L)) }
        }
    }

    @Test fun scalesTheEligibleRangeWithTheDisplay() {
        ready(LightFrameGenPolicy(), fps = 60f, hz = 120f)
        ready(LightFrameGenPolicy(), fps = 45f, hz = 90f)
    }

    @Test fun usesHysteresisInsteadOfFlappingAroundTheEntryThreshold() {
        val policy = LightFrameGenPolicy()
        ready(policy)
        assertTrue(policy.sample(36f, 60f, 0, false, 4_000))
        assertFalse(policy.sample(46f, 60f, 0, false, 5_000))
        assertFalse(policy.sample(36f, 60f, 0, false, 6_000))
    }

    @Test fun suspendsOnHeatAndWaitsBeforeRecovering() {
        val policy = LightFrameGenPolicy()
        ready(policy)
        assertFalse(policy.sample(30f, 60f, 2, false, 4_000))
        assertFalse(policy.sample(30f, 60f, 0, false, 18_000))
        ready(policy, start = 18_000)
    }

    @Test fun detectsWhenInterpolationHurtsSourcePerformance() {
        val policy = LightFrameGenPolicy()
        ready(policy)
        assertFalse(policy.sample(23f, 60f, 0, false, 4_000))
        assertFalse(policy.sample(30f, 60f, 0, false, 33_000))
        ready(policy, start = 33_000)
    }

    @Test fun pausesInPowerSavingModeAndWithNoFrames() {
        val policy = LightFrameGenPolicy()
        ready(policy)
        assertFalse(policy.sample(30f, 60f, 0, true, 4_000))
        assertFalse(policy.sample(30f, 60f, 0, false, 5_000))
        assertFalse(policy.sample(0f, 60f, 0, false, 6_000))
    }

    @Test fun rejectsInvalidMeasurementsAndResetsTheWarmup() {
        val policy = LightFrameGenPolicy()
        policy.sample(30f, 60f, 0, false, 1_000)
        assertFalse(policy.sample(Float.NaN, 60f, 0, false, 2_000))
        assertFalse(policy.sample(30f, Float.POSITIVE_INFINITY, 0, false, 3_000))
        ready(policy, start = 3_000)
    }
}
