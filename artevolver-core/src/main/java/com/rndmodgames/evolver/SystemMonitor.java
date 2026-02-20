package com.rndmodgames.evolver;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.text.DecimalFormat;

/**
 * Real-time system resource monitor using JMX and java.lang.management.
 * Provides CPU load, RAM usage, disk usage, and thread counts without
 * external dependencies.
 *
 * All methods are thread-safe and designed for periodic polling from a Swing Timer.
 */
public class SystemMonitor {

    private static final DecimalFormat DF1 = new DecimalFormat("0.0");
    private static final DecimalFormat DF0 = new DecimalFormat("#,##0");

    private final OperatingSystemMXBean osMxBean;
    private final MemoryMXBean memMxBean;
    private final ThreadMXBean threadMxBean;
    private final Runtime runtime;
    private final int availableProcessors;

    // Cached values (updated on poll())
    private double cpuLoadProcess = -1;
    private double cpuLoadSystem = -1;
    private long heapUsedMB;
    private long heapMaxMB;
    private long nativeUsedMB;
    private long totalPhysicalMemMB;
    private long freePhysicalMemMB;
    private long diskTotalMB;
    private long diskFreeMB;
    private int threadCount;
    private int peakThreadCount;
    private long lastPollMs;

    public SystemMonitor() {
        this.osMxBean = ManagementFactory.getOperatingSystemMXBean();
        this.memMxBean = ManagementFactory.getMemoryMXBean();
        this.threadMxBean = ManagementFactory.getThreadMXBean();
        this.runtime = Runtime.getRuntime();
        this.availableProcessors = runtime.availableProcessors();
        poll();
    }

    /**
     * Refreshes all cached metrics. Call from a timer (e.g., every 2-5 seconds).
     */
    public void poll() {
        lastPollMs = System.currentTimeMillis();

        // CPU load (JMX com.sun extension — available on HotSpot/OpenJDK)
        if (osMxBean instanceof com.sun.management.OperatingSystemMXBean sunOs) {
            cpuLoadProcess = sunOs.getProcessCpuLoad() * 100.0;
            cpuLoadSystem = sunOs.getCpuLoad() * 100.0;
            totalPhysicalMemMB = sunOs.getTotalMemorySize() / (1024 * 1024);
            freePhysicalMemMB = sunOs.getFreeMemorySize() / (1024 * 1024);
        } else {
            cpuLoadProcess = osMxBean.getSystemLoadAverage();
            cpuLoadSystem = -1;
            totalPhysicalMemMB = -1;
            freePhysicalMemMB = -1;
        }

        // JVM Heap
        MemoryUsage heap = memMxBean.getHeapMemoryUsage();
        heapUsedMB = heap.getUsed() / (1024 * 1024);
        heapMaxMB = heap.getMax() / (1024 * 1024);

        // JVM total (heap + off-heap) via Runtime
        nativeUsedMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);

        // Disk (workspace root)
        File workDir = new File(System.getProperty("user.dir"));
        diskTotalMB = workDir.getTotalSpace() / (1024 * 1024);
        diskFreeMB = workDir.getUsableSpace() / (1024 * 1024);

        // Threads
        threadCount = threadMxBean.getThreadCount();
        peakThreadCount = threadMxBean.getPeakThreadCount();
    }

    // ════════════════════════════════════════════════════════════════
    //  PERCENTAGES (for resource-limit checks)
    // ════════════════════════════════════════════════════════════════

    /** Process CPU usage as 0-100%. Returns -1 if unavailable. */
    public double getCpuUsagePercent() { return cpuLoadProcess; }

    /** System-wide CPU usage as 0-100%. Returns -1 if unavailable. */
    public double getSystemCpuPercent() { return cpuLoadSystem; }

    /** JVM heap usage as 0-100%. */
    public double getHeapUsagePercent() {
        return heapMaxMB > 0 ? (100.0 * heapUsedMB / heapMaxMB) : 0;
    }

    /** Physical RAM usage as 0-100%. Returns -1 if unavailable. */
    public double getRamUsagePercent() {
        if (totalPhysicalMemMB <= 0) return -1;
        return 100.0 * (totalPhysicalMemMB - freePhysicalMemMB) / totalPhysicalMemMB;
    }

    /** Disk usage as 0-100%. */
    public double getDiskUsagePercent() {
        return diskTotalMB > 0 ? (100.0 * (diskTotalMB - diskFreeMB) / diskTotalMB) : 0;
    }

    /** Active JVM thread count as % of available processors. */
    public double getThreadUtilizationPercent() {
        return 100.0 * threadCount / availableProcessors;
    }

    // ════════════════════════════════════════════════════════════════
    //  RAW VALUES
    // ════════════════════════════════════════════════════════════════

    public int getAvailableProcessors() { return availableProcessors; }
    public long getHeapUsedMB() { return heapUsedMB; }
    public long getHeapMaxMB() { return heapMaxMB; }
    public long getNativeUsedMB() { return nativeUsedMB; }
    public long getTotalPhysicalMemMB() { return totalPhysicalMemMB; }
    public long getFreePhysicalMemMB() { return freePhysicalMemMB; }
    public long getDiskTotalMB() { return diskTotalMB; }
    public long getDiskFreeMB() { return diskFreeMB; }
    public int getThreadCount() { return threadCount; }
    public int getPeakThreadCount() { return peakThreadCount; }

    // ════════════════════════════════════════════════════════════════
    //  RESOURCE LIMIT CHECKS
    // ════════════════════════════════════════════════════════════════

    /**
     * Returns true if adding more work is safe given the specified limits.
     * Any limit <= 0 means "unlimited" for that resource.
     * Uses a 10% safety margin below the limit because JMX readings lag.
     */
    public boolean canAddWork(double maxCpuPercent, double maxRamPercent, double maxHeapPercent) {
        double cpuMargin = maxCpuPercent * 0.9;
        if (maxCpuPercent > 0 && cpuLoadProcess >= 0 && cpuLoadProcess > cpuMargin) return false;
        if (maxCpuPercent > 0 && cpuLoadSystem >= 0 && cpuLoadSystem > cpuMargin) return false;
        if (maxRamPercent > 0 && getRamUsagePercent() >= 0 && getRamUsagePercent() > maxRamPercent) return false;
        if (maxHeapPercent > 0 && getHeapUsagePercent() > maxHeapPercent) return false;
        return true;
    }

    /**
     * Returns the number of "free" processor threads based on current CPU usage.
     * E.g., if 16 cores and 50% CPU, ~8 threads are free.
     */
    public int estimateFreeThreads(double maxCpuPercent) {
        double usedPct = cpuLoadProcess >= 0 ? cpuLoadProcess : 50;
        double allowedPct = maxCpuPercent > 0 ? maxCpuPercent : 90;
        double headroom = Math.max(0, allowedPct - usedPct) / 100.0;
        return Math.max(0, (int) (availableProcessors * headroom));
    }

    // ════════════════════════════════════════════════════════════════
    //  FORMATTED STRINGS (for UI display)
    // ════════════════════════════════════════════════════════════════

    public String formatCpu() {
        if (cpuLoadProcess < 0) return "CPU: N/A";
        return "CPU: " + DF1.format(cpuLoadProcess) + "% proc"
                + (cpuLoadSystem >= 0 ? " / " + DF1.format(cpuLoadSystem) + "% sys" : "")
                + " (" + availableProcessors + " cores)";
    }

    public String formatRam() {
        String heap = "Heap: " + DF0.format(heapUsedMB) + "/" + DF0.format(heapMaxMB) + " MB"
                + " (" + DF1.format(getHeapUsagePercent()) + "%)";
        if (totalPhysicalMemMB > 0) {
            long usedPhys = totalPhysicalMemMB - freePhysicalMemMB;
            heap += "  |  RAM: " + DF0.format(usedPhys) + "/" + DF0.format(totalPhysicalMemMB) + " MB"
                    + " (" + DF1.format(getRamUsagePercent()) + "%)";
        }
        return heap;
    }

    public String formatDisk() {
        long usedDisk = diskTotalMB - diskFreeMB;
        return "Disk: " + DF0.format(usedDisk) + "/" + DF0.format(diskTotalMB) + " MB"
                + " (" + DF1.format(getDiskUsagePercent()) + "% used)"
                + "  Free: " + DF0.format(diskFreeMB) + " MB";
    }

    public String formatThreads() {
        return "Threads: " + threadCount + " active (peak " + peakThreadCount + ")";
    }

    /** One-line summary for status bars. */
    public String formatCompact() {
        StringBuilder sb = new StringBuilder();
        if (cpuLoadProcess >= 0) sb.append("CPU ").append(DF1.format(cpuLoadProcess)).append("%");
        sb.append("  Heap ").append(DF0.format(heapUsedMB)).append("/").append(DF0.format(heapMaxMB)).append("MB");
        if (totalPhysicalMemMB > 0) {
            sb.append("  RAM ").append(DF1.format(getRamUsagePercent())).append("%");
        }
        sb.append("  Thr ").append(threadCount);
        return sb.toString();
    }

    @Override
    public String toString() {
        return formatCpu() + "\n" + formatRam() + "\n" + formatDisk() + "\n" + formatThreads();
    }
}
