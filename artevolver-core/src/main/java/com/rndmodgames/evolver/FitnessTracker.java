package com.rndmodgames.evolver;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks fitness over time for a single contestant, computing velocity
 * (fitness gain per second), acceleration, and projected fitness.
 *
 * Uses a configurable sliding window so that recent performance is
 * weighted over lifetime averages. This is critical for fairly evaluating
 * newcomers against established contestants in the evolutionary tournament.
 */
public class FitnessTracker {

    private final List<Snapshot> snapshots = new ArrayList<>();
    private int velocityWindowMs = 30_000;

    public FitnessTracker() {}

    public FitnessTracker(int velocityWindowSeconds) {
        this.velocityWindowMs = velocityWindowSeconds * 1000;
    }

    public void setVelocityWindowSeconds(int seconds) {
        this.velocityWindowMs = seconds * 1000;
    }

    /** Records a fitness observation at the current time. */
    public void addSnapshot(double fitness, long totalIterations) {
        snapshots.add(new Snapshot(System.currentTimeMillis(), fitness, totalIterations));
    }

    /** Records a fitness observation at a specific timestamp. */
    public void addSnapshot(long timestampMs, double fitness, long totalIterations) {
        snapshots.add(new Snapshot(timestampMs, fitness, totalIterations));
    }

    public int getSnapshotCount() {
        return snapshots.size();
    }

    public double getLatestFitness() {
        if (snapshots.isEmpty()) return 0;
        return snapshots.get(snapshots.size() - 1).fitness;
    }

    /**
     * Fitness gain per second over the recent window.
     * Returns 0 if insufficient data or time span.
     */
    public double getVelocity() {
        return getVelocityOverWindow(velocityWindowMs);
    }

    /**
     * Rate of change of velocity (fitness/sec^2).
     * Computed by comparing the velocity of the first half of the window
     * to the velocity of the second half.
     */
    public double getAcceleration() {
        if (snapshots.size() < 4) return 0;

        long now = snapshots.get(snapshots.size() - 1).timestampMs;
        long windowStart = now - velocityWindowMs;
        int midIdx = -1;
        long midTime = windowStart + velocityWindowMs / 2;

        for (int i = snapshots.size() - 1; i >= 0; i--) {
            if (snapshots.get(i).timestampMs <= midTime) {
                midIdx = i;
                break;
            }
        }

        if (midIdx < 1) return 0;

        double v1 = velocityBetween(findFirst(windowStart), midIdx);
        double v2 = velocityBetween(midIdx, snapshots.size() - 1);

        long dt1 = snapshots.get(midIdx).timestampMs - snapshots.get(findFirst(windowStart)).timestampMs;
        long dt2 = snapshots.get(snapshots.size() - 1).timestampMs - snapshots.get(midIdx).timestampMs;

        if (dt1 == 0 || dt2 == 0) return 0;
        double timeMidpoint = (dt1 + dt2) / 2000.0;
        if (timeMidpoint == 0) return 0;
        return (v2 - v1) / timeMidpoint;
    }

    /**
     * Projects fitness forward by the given number of seconds using current velocity.
     */
    public double getProjectedFitness(double secondsAhead) {
        double vel = getVelocity();
        double current = getLatestFitness();
        return current + vel * secondsAhead;
    }

    /**
     * Average iterations per second over the full runtime.
     */
    public double getIterationsPerSecond() {
        if (snapshots.size() < 2) return 0;
        Snapshot first = snapshots.get(0);
        Snapshot last = snapshots.get(snapshots.size() - 1);
        long dtMs = last.timestampMs - first.timestampMs;
        if (dtMs <= 0) return 0;
        return (last.totalIterations - first.totalIterations) / (dtMs / 1000.0);
    }

    /**
     * Peak fitness ever recorded.
     */
    public double getPeakFitness() {
        double peak = 0;
        for (Snapshot s : snapshots) {
            if (s.fitness > peak) peak = s.fitness;
        }
        return peak;
    }

    /**
     * Peak velocity ever observed (sampled each time velocity is checked).
     * Computed by scanning through the history with overlapping windows.
     */
    public double getPeakVelocity() {
        if (snapshots.size() < 2) return 0;
        double peak = 0;
        for (int i = 1; i < snapshots.size(); i++) {
            long dt = snapshots.get(i).timestampMs - snapshots.get(i - 1).timestampMs;
            if (dt <= 0) continue;
            double v = (snapshots.get(i).fitness - snapshots.get(i - 1).fitness) / (dt / 1000.0);
            if (v > peak) peak = v;
        }
        return peak;
    }

    /** Elapsed seconds since the first snapshot. */
    public double getElapsedSeconds() {
        if (snapshots.size() < 2) return 0;
        return (snapshots.get(snapshots.size() - 1).timestampMs - snapshots.get(0).timestampMs) / 1000.0;
    }

    // --- Internal ---

    private double getVelocityOverWindow(long windowMs) {
        if (snapshots.size() < 2) return 0;

        long now = snapshots.get(snapshots.size() - 1).timestampMs;
        long windowStart = now - windowMs;
        int startIdx = findFirst(windowStart);

        return velocityBetween(startIdx, snapshots.size() - 1);
    }

    private int findFirst(long minTimestamp) {
        for (int i = 0; i < snapshots.size(); i++) {
            if (snapshots.get(i).timestampMs >= minTimestamp) return i;
        }
        return 0;
    }

    private double velocityBetween(int fromIdx, int toIdx) {
        if (fromIdx >= toIdx || fromIdx < 0 || toIdx >= snapshots.size()) return 0;
        Snapshot a = snapshots.get(fromIdx);
        Snapshot b = snapshots.get(toIdx);
        long dtMs = b.timestampMs - a.timestampMs;
        if (dtMs <= 0) return 0;
        return (b.fitness - a.fitness) / (dtMs / 1000.0);
    }

    public static class Snapshot {
        public final long timestampMs;
        public final double fitness;
        public final long totalIterations;

        Snapshot(long timestampMs, double fitness, long totalIterations) {
            this.timestampMs = timestampMs;
            this.fitness = fitness;
            this.totalIterations = totalIterations;
        }
    }
}
