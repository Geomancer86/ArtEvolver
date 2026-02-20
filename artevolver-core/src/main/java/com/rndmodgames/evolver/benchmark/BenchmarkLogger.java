package com.rndmodgames.evolver.benchmark;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Structured benchmark data logger that writes CSV files with timestamped metrics.
 *
 * Replaces ad-hoc System.out.println benchmarking with machine-readable output
 * suitable for automated analysis, charting, and regression detection.
 *
 * Thread-safe: all public methods synchronize on the instance.
 */
public class BenchmarkLogger implements AutoCloseable {

    private static final String CSV_HEADER =
            "elapsed_s,total_iterations,good_iterations,health_pct,best_score,"
            + "iter_per_sec,good_iter_per_sec,threads,population,triangles,mode";

    private static final DateTimeFormatter FILE_TS =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final PrintWriter writer;
    private final String filePath;
    private final long startTimeMs;
    private final List<Snapshot> snapshots = new ArrayList<>();

    /**
     * A single point-in-time measurement captured during evolution.
     */
    public static class Snapshot {
        public final double elapsedSeconds;
        public final long totalIterations;
        public final long goodIterations;
        public final double healthPercent;
        public final double bestScore;
        public final double iterationsPerSecond;
        public final double goodIterationsPerSecond;
        public final int threads;
        public final int population;
        public final int triangles;
        public final String mode;

        public Snapshot(double elapsedSeconds, long totalIterations, long goodIterations,
                        double healthPercent, double bestScore, double iterationsPerSecond,
                        double goodIterationsPerSecond, int threads, int population,
                        int triangles, String mode) {
            this.elapsedSeconds = elapsedSeconds;
            this.totalIterations = totalIterations;
            this.goodIterations = goodIterations;
            this.healthPercent = healthPercent;
            this.bestScore = bestScore;
            this.iterationsPerSecond = iterationsPerSecond;
            this.goodIterationsPerSecond = goodIterationsPerSecond;
            this.threads = threads;
            this.population = population;
            this.triangles = triangles;
            this.mode = mode;
        }
    }

    public BenchmarkLogger(String outputDir, String runLabel) throws IOException {
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String timestamp = LocalDateTime.now().format(FILE_TS);
        String safeLabel = runLabel.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        String fileName = "benchmark_" + safeLabel + "_" + timestamp + ".csv";
        this.filePath = new File(dir, fileName).getAbsolutePath();
        this.startTimeMs = System.currentTimeMillis();

        this.writer = new PrintWriter(new BufferedWriter(new FileWriter(filePath)));
        writer.println(CSV_HEADER);
        writer.flush();
    }

    /**
     * Records a snapshot and writes it to the CSV file.
     */
    public synchronized void record(long totalIterations, long goodIterations,
                                     double healthPercent, double bestScore,
                                     int threads, int population, int triangles,
                                     String mode) {
        double elapsedSec = (System.currentTimeMillis() - startTimeMs) / 1000.0;
        double iterPerSec = elapsedSec > 0 ? totalIterations / elapsedSec : 0;
        double goodPerSec = elapsedSec > 0 ? goodIterations / elapsedSec : 0;

        Snapshot snap = new Snapshot(elapsedSec, totalIterations, goodIterations,
                healthPercent, bestScore, iterPerSec, goodPerSec,
                threads, population, triangles, mode);
        snapshots.add(snap);

        writer.printf("%.3f,%d,%d,%.4f,%.10f,%.2f,%.2f,%d,%d,%d,%s%n",
                snap.elapsedSeconds, snap.totalIterations, snap.goodIterations,
                snap.healthPercent, snap.bestScore, snap.iterationsPerSecond,
                snap.goodIterationsPerSecond, snap.threads, snap.population,
                snap.triangles, snap.mode);
        writer.flush();
    }

    /**
     * Returns the path to the CSV file being written.
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * Returns all recorded snapshots (for programmatic analysis).
     */
    public synchronized List<Snapshot> getSnapshots() {
        return new ArrayList<>(snapshots);
    }

    /**
     * Generates a human-readable summary of the benchmark run.
     */
    public synchronized String generateSummary() {
        if (snapshots.isEmpty()) {
            return "No data recorded.";
        }

        Snapshot first = snapshots.get(0);
        Snapshot last = snapshots.get(snapshots.size() - 1);

        double peakScore = snapshots.stream()
                .mapToDouble(s -> s.bestScore)
                .max().orElse(0);
        double avgIterPerSec = snapshots.stream()
                .mapToDouble(s -> s.iterationsPerSecond)
                .average().orElse(0);
        double avgGoodPerSec = snapshots.stream()
                .mapToDouble(s -> s.goodIterationsPerSecond)
                .average().orElse(0);
        double avgHealth = snapshots.stream()
                .mapToDouble(s -> s.healthPercent)
                .average().orElse(0);

        StringBuilder sb = new StringBuilder();
        sb.append("=== BENCHMARK SUMMARY ===\n");
        sb.append(String.format("  Run duration     : %.1f seconds\n", last.elapsedSeconds));
        sb.append(String.format("  Data points      : %d\n", snapshots.size()));
        sb.append(String.format("  Mode             : %s\n", last.mode));
        sb.append(String.format("  Threads          : %d\n", last.threads));
        sb.append(String.format("  Population/thread: %d\n", last.population));
        sb.append(String.format("  Triangles        : %d\n", last.triangles));
        sb.append("\n  --- Score ---\n");
        sb.append(String.format("  Initial score    : %.10f\n", first.bestScore));
        sb.append(String.format("  Final score      : %.10f\n", last.bestScore));
        sb.append(String.format("  Peak score       : %.10f\n", peakScore));
        sb.append(String.format("  Score gain       : %.10f\n", last.bestScore - first.bestScore));
        sb.append("\n  --- Throughput ---\n");
        sb.append(String.format("  Total iterations : %,d\n", last.totalIterations));
        sb.append(String.format("  Good iterations  : %,d\n", last.goodIterations));
        sb.append(String.format("  Avg iter/sec     : %,.0f\n", avgIterPerSec));
        sb.append(String.format("  Avg good iter/sec: %,.0f\n", avgGoodPerSec));
        sb.append("\n  --- Health ---\n");
        sb.append(String.format("  Avg health       : %.2f%%\n", avgHealth));
        sb.append(String.format("  Final health     : %.2f%%\n", last.healthPercent));
        sb.append("\n  CSV output       : " + filePath + "\n");
        sb.append("=========================\n");

        return sb.toString();
    }

    @Override
    public synchronized void close() {
        if (writer != null) {
            writer.close();
        }
    }

    /**
     * Compares two benchmark CSV summary sets and prints a side-by-side report.
     */
    public static String compare(List<Snapshot> runA, String labelA,
                                  List<Snapshot> runB, String labelB) {
        if (runA.isEmpty() || runB.isEmpty()) {
            return "Cannot compare: one or both runs have no data.";
        }

        Snapshot lastA = runA.get(runA.size() - 1);
        Snapshot lastB = runB.get(runB.size() - 1);

        double avgIterA = runA.stream().mapToDouble(s -> s.iterationsPerSecond).average().orElse(0);
        double avgIterB = runB.stream().mapToDouble(s -> s.iterationsPerSecond).average().orElse(0);

        StringBuilder sb = new StringBuilder();
        sb.append("=== BENCHMARK COMPARISON ===\n");
        sb.append(String.format("  %-28s %18s %18s %10s\n", "Metric", labelA, labelB, "Delta"));
        sb.append(String.format("  %-28s %18s %18s %10s\n", "---", "---", "---", "---"));
        sb.append(compareLine("Final score", lastA.bestScore, lastB.bestScore, "%.10f"));
        sb.append(compareLine("Total iterations", (double) lastA.totalIterations, (double) lastB.totalIterations, "%,.0f"));
        sb.append(compareLine("Good iterations", (double) lastA.goodIterations, (double) lastB.goodIterations, "%,.0f"));
        sb.append(compareLine("Avg iter/sec", avgIterA, avgIterB, "%,.0f"));
        sb.append(compareLine("Duration (s)", lastA.elapsedSeconds, lastB.elapsedSeconds, "%.1f"));
        sb.append("============================\n");

        return sb.toString();
    }

    private static String compareLine(String metric, double a, double b, String fmt) {
        double delta = b - a;
        String sign = delta >= 0 ? "+" : "";
        return String.format("  %-28s %18s %18s %10s\n",
                metric,
                String.format(fmt, a),
                String.format(fmt, b),
                sign + String.format(fmt, delta));
    }
}
