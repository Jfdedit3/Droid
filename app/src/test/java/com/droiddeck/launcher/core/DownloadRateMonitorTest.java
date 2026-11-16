package com.droiddeck.launcher.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class DownloadRateMonitorTest {
    private static final long MB = 1024 * 1024;

    @Test public void allowsConnectionWarmup() {
        DownloadRateMonitor monitor = new DownloadRateMonitor(0, 0);
        assertFalse(monitor.shouldSwitch(1024, 100 * MB, 19_000_000_000L));
        assertTrue(monitor.shouldSwitch(2048, 100 * MB, 20_000_000_000L));
    }

    @Test public void preservesFastAndAlmostFinishedDownloads() {
        assertFalse(new DownloadRateMonitor(0, 0).shouldSwitch(4 * MB, 100 * MB, 20_000_000_000L));
        assertFalse(new DownloadRateMonitor(0, 0).shouldSwitch(1024, 4 * MB, 20_000_000_000L));
        assertFalse(new DownloadRateMonitor(0, 0).shouldSwitch(1024, -1, 20_000_000_000L));
    }

    @Test public void detectsSlowdownAfterAFastStart() {
        DownloadRateMonitor monitor = new DownloadRateMonitor(0, 0);
        assertFalse(monitor.shouldSwitch(50 * MB, 100 * MB, 20_000_000_000L));
        assertTrue(monitor.shouldSwitch(50 * MB + 1024, 100 * MB, 40_000_000_000L));
    }

    @Test public void doesNotCountBytesFromAnEarlierResumedDownload() {
        DownloadRateMonitor monitor = new DownloadRateMonitor(0, 50 * MB);
        assertTrue(monitor.shouldSwitch(50 * MB + 1024, 100 * MB, 20_000_000_000L));
    }

    @Test public void throttlesUiUpdatesByTime() {
        DownloadRateMonitor monitor = new DownloadRateMonitor(0, 0);
        assertFalse(monitor.shouldReport(249_000_000L));
        assertTrue(monitor.shouldReport(250_000_000L));
        assertFalse(monitor.shouldReport(251_000_000L));
        assertTrue(monitor.shouldReport(500_000_000L));
    }
}
