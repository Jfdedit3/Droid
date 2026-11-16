package com.droiddeck.launcher.core;

final class DownloadRateMonitor {
    static final long GRACE_NANOS = 20_000_000_000L;
    static final long MIN_BYTES_PER_SECOND = 128 * 1024;
    static final long MIN_REMAINING_BYTES = 8L * 1024 * 1024;
    static final long REPORT_INTERVAL_NANOS = 250_000_000L;
    private long started;
    private long initialBytes;
    private long lastReport;

    DownloadRateMonitor(long now, long initialBytes) {
        this.started = now;
        this.lastReport = now;
        this.initialBytes = initialBytes;
    }

    boolean shouldSwitch(long written, long total, long now) {
        long elapsed = now - started;
        if (elapsed < GRACE_NANOS || total < written || total - written <= MIN_REMAINING_BYTES) return false;
        double rate = (written - initialBytes) / (elapsed / 1_000_000_000.0);
        boolean slow = rate < MIN_BYTES_PER_SECOND;
        started = now;
        initialBytes = written;
        return slow;
    }

    boolean shouldReport(long now) {
        if (now - lastReport < REPORT_INTERVAL_NANOS) return false;
        lastReport = now;
        return true;
    }
}
