package com.rndmodgames.artevolver;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.rndmodgames.evolver.ArtEvolverTools;
import com.rndmodgames.evolver.DeltaFitnessEngine;
import com.rndmodgames.evolver.ImageEvolver;
import com.rndmodgames.evolver.LAPSolver;
import com.rndmodgames.evolver.Triangle;
import com.rndmodgames.evolver.TriangleList;
import com.rndmodgames.evolver.benchmark.BenchmarkLogger;
import com.rndmodgames.evolver.benchmark.BenchmarkRunner;

/**
 * Automated benchmark tests that produce CSV output files in the benchmarks/ directory.
 *
 * Run with: mvn test -pl artevolver-core -Dtest=BenchmarkTest
 *
 * Results are written to benchmarks/ as timestamped CSV files and printed to stdout.
 */
@Tag("slow")
class BenchmarkTest {

    private static final String TEST_IMAGE = "./src/test/resources/000_zeldathumb-1920-789452.jpg";

    /**
     * Quick smoke test: 1 thread, 1000 iterations, small grid.
     * Validates the benchmark pipeline works end-to-end.
     */
    @Test
    void quickBenchmark() throws IOException, URISyntaxException {

        BenchmarkRunner.Result result = new BenchmarkRunner.Builder()
                .imagePath(TEST_IMAGE)
                .totalIterations(1_000)
                .snapshotInterval(200)
                .threads(1)
                .population(2)
                .palettes(1)
                .widthTriangles(38)
                .heightTriangles(39)
                .triangleScale(1f)
                .label("quick_smoke")
                .build()
                .run();

        System.out.println(result.summary);

        assertNotNull(result.csvPath);
        assertTrue(new File(result.csvPath).exists(), "CSV file should exist");
        assertTrue(result.finalScore > 0, "Score should improve from zero");
        assertTrue(result.snapshots.size() >= 2, "Should have multiple snapshots");
    }

    /**
     * Single-thread baseline: measures raw per-thread evolution speed
     * without any threading overhead.
     */
    @Test
    void singleThreadBaseline() throws IOException, URISyntaxException {

        BenchmarkRunner.Result result = new BenchmarkRunner.Builder()
                .imagePath(TEST_IMAGE)
                .totalIterations(5_000)
                .snapshotInterval(500)
                .threads(1)
                .population(2)
                .palettes(1)
                .widthTriangles(38)
                .heightTriangles(39)
                .triangleScale(1f)
                .label("single_thread_baseline")
                .build()
                .run();

        System.out.println(result.summary);

        assertTrue(result.finalScore > 0);
    }

    /**
     * Multi-thread scaling test: compares 1, 2, 4, 8 threads
     * at the same total iteration count to measure scaling efficiency.
     */
    @Test
    void threadScalingBenchmark() throws IOException, URISyntaxException {

        int[] threadCounts = {1, 2, 4, 8};
        int totalIterations = 4_000;
        BenchmarkRunner.Result[] results = new BenchmarkRunner.Result[threadCounts.length];

        for (int i = 0; i < threadCounts.length; i++) {
            results[i] = new BenchmarkRunner.Builder()
                    .imagePath(TEST_IMAGE)
                    .totalIterations(totalIterations)
                    .snapshotInterval(500)
                    .threads(threadCounts[i])
                    .population(2)
                    .palettes(1)
                    .widthTriangles(38)
                    .heightTriangles(39)
                    .triangleScale(1f)
                    .label("scaling_" + threadCounts[i] + "T")
                    .build()
                    .run();
        }

        System.out.println("\n=== THREAD SCALING RESULTS ===");
        System.out.printf("  %-10s %12s %12s %15s%n", "Threads", "Time (s)", "Score", "Iter/sec");
        System.out.printf("  %-10s %12s %12s %15s%n", "-------", "--------", "-----", "--------");

        for (int i = 0; i < threadCounts.length; i++) {
            BenchmarkRunner.Result r = results[i];
            BenchmarkLogger.Snapshot last = r.snapshots.get(r.snapshots.size() - 1);
            System.out.printf("  %-10d %12.2f %12.8f %15.0f%n",
                    threadCounts[i], last.elapsedSeconds, last.bestScore, last.iterationsPerSecond);
        }

        System.out.println("==============================\n");

        if (results.length >= 2) {
            String comparison = BenchmarkLogger.compare(
                    results[0].snapshots, "1 thread",
                    results[results.length - 1].snapshots, threadCounts[threadCounts.length - 1] + " threads");
            System.out.println(comparison);
        }
    }

    /**
     * Population size benchmark: compares pop=2, pop=4, pop=8
     * to find the sweet spot between diversity and overhead.
     */
    @Test
    void populationSizeBenchmark() throws IOException, URISyntaxException {

        int[] popSizes = {2, 4, 8};
        BenchmarkRunner.Result[] results = new BenchmarkRunner.Result[popSizes.length];

        for (int i = 0; i < popSizes.length; i++) {
            results[i] = new BenchmarkRunner.Builder()
                    .imagePath(TEST_IMAGE)
                    .totalIterations(5_000)
                    .snapshotInterval(500)
                    .threads(4)
                    .population(popSizes[i])
                    .palettes(1)
                    .widthTriangles(38)
                    .heightTriangles(39)
                    .triangleScale(1f)
                    .label("pop_" + popSizes[i])
                    .build()
                    .run();
        }

        System.out.println("\n=== POPULATION SIZE RESULTS ===");
        System.out.printf("  %-10s %12s %12s %15s%n", "Pop Size", "Time (s)", "Score", "Iter/sec");
        System.out.printf("  %-10s %12s %12s %15s%n", "--------", "--------", "-----", "--------");

        for (int i = 0; i < popSizes.length; i++) {
            BenchmarkRunner.Result r = results[i];
            BenchmarkLogger.Snapshot last = r.snapshots.get(r.snapshots.size() - 1);
            System.out.printf("  %-10d %12.2f %12.8f %15.0f%n",
                    popSizes[i], last.elapsedSeconds, last.bestScore, last.iterationsPerSecond);
        }

        System.out.println("===============================\n");
    }

    /**
     * Validates that the color permutation constraint is maintained through
     * initialization and evolution: the exact same multiset of colors must
     * be present before and after mutations (swap-only operators preserve this).
     * Uses 38x39=1482 triangles with 1 palette (1535 colors > 1482 triangles).
     */
    @Test
    void permutationIntegrity() throws IOException, URISyntaxException {

        ImageEvolver evolver = ArtEvolverTools.getDefaultImageEvolver(
            1, 2, 2, 2, "000_zeldathumb-1920-789452.jpg", false,
            38, 39, 1f);

        System.out.println("[PERMUTATION TEST] Population size: " + evolver.getPopulation().size());

        int[][] referenceMultisets = new int[evolver.getPopulation().size()][];

        for (int i = 0; i < evolver.getPopulation().size(); i++) {
            TriangleList<Triangle> member = evolver.getPopulation().get(i);
            System.out.println("  pop[" + i + "] triangles: " + member.size());

            String structError = ImageEvolver.validatePermutation(member);
            assertNull(structError, "Structural error pop[" + i + "] after init: " + structError);

            referenceMultisets[i] = ImageEvolver.captureColorMultiset(member);
        }

        evolver.setRunning(true);
        evolver.evolve(System.currentTimeMillis(), 1000);

        for (int i = 0; i < evolver.getPopulation().size(); i++) {
            TriangleList<Triangle> member = evolver.getPopulation().get(i);

            String structError = ImageEvolver.validatePermutation(member);
            assertNull(structError, "Structural error pop[" + i + "] after evolve: " + structError);

            String multisetError = ImageEvolver.validateColorMultiset(member, referenceMultisets[i]);
            assertNull(multisetError,
                "Color multiset changed in pop[" + i + "] after 1000 iterations: " + multisetError);
        }

        System.out.println("[PASS] Color multiset integrity maintained through init + 1000 iterations");
    }

    /**
     * Validates DeltaFitnessEngine produces identical scores to full render+compare,
     * and benchmarks the speedup for in-place swap evaluation.
     */
    @Test
    void deltaFitnessAccuracy() throws IOException, URISyntaxException {

        ImageEvolver evolver = ArtEvolverTools.getDefaultImageEvolver(
            1, 2, 2, 2, "000_zeldathumb-1920-789452.jpg", false,
            38, 39, 1f);

        TriangleList<Triangle> individual = evolver.getPopulation().get(0);

        double fullScore = individual.getScore();
        System.out.printf("[DELTA TEST] Full render+compare score: %.10f%n", fullScore);

        long t0 = System.nanoTime();
        DeltaFitnessEngine engine = new DeltaFitnessEngine(individual, evolver.getResizedOriginal());
        long buildMs = (System.nanoTime() - t0) / 1_000_000;
        System.out.printf("[DELTA TEST] Mask build time: %d ms%n", buildMs);

        double deltaScore = engine.getScore();
        System.out.printf("[DELTA TEST] Delta engine score:        %.10f%n", deltaScore);
        System.out.printf("[DELTA TEST] Difference:                %.2e%n", Math.abs(fullScore - deltaScore));

        assertTrue(Math.abs(fullScore - deltaScore) < 0.001,
            "Delta score should match full score within 0.001, got diff=" +
            Math.abs(fullScore - deltaScore));

        int swapCount = 10_000;
        int n = engine.getTriangleCount();
        java.util.concurrent.ThreadLocalRandom rng = java.util.concurrent.ThreadLocalRandom.current();

        t0 = System.nanoTime();
        int accepted = 0;
        for (int i = 0; i < swapCount; i++) {
            int a = rng.nextInt(n);
            int b = rng.nextInt(n);
            if (a == b) continue;
            if (engine.trySwap(a, b)) accepted++;
        }
        long deltaMs = (System.nanoTime() - t0) / 1_000_000;

        System.out.printf("[DELTA TEST] %d swap evaluations in %d ms = %.0f swaps/sec%n",
            swapCount, deltaMs, swapCount * 1000.0 / deltaMs);
        System.out.printf("[DELTA TEST] Accepted: %d (%.1f%%)%n", accepted, accepted * 100.0 / swapCount);
        System.out.printf("[DELTA TEST] Final delta score: %.10f (gain: %.6f)%n",
            engine.getScore(), engine.getScore() - fullScore);

        assertTrue(engine.getScore() >= fullScore,
            "Hill-climbing should not decrease score");
    }

    /**
     * Head-to-head comparison: delta evolution vs legacy render+compare evolution.
     * Both run for a fixed wall-clock duration and we compare iterations and score gain.
     */
    @Test
    void deltaVsLegacyBenchmark() throws IOException, URISyntaxException {

        int testDurationMs = 5000;

        // --- Legacy (render + compare) ---
        ImageEvolver legacyEvolver = ArtEvolverTools.getDefaultImageEvolver(
            1, 2, 2, 2, "000_zeldathumb-1920-789452.jpg", false,
            38, 39, 1f);
        legacyEvolver.setUseDeltaEvolution(false);

        double legacyStartScore = legacyEvolver.getBestScore();
        long legacyStartIter = legacyEvolver.getTotalIterations();

        long t0 = System.nanoTime();
        long deadline = t0 + (long) testDurationMs * 1_000_000L;
        while (System.nanoTime() < deadline) {
            legacyEvolver.evolve(0L, 10);
        }
        long legacyElapsed = (System.nanoTime() - t0) / 1_000_000;
        long legacyIters = legacyEvolver.getTotalIterations() - legacyStartIter;
        double legacyGain = legacyEvolver.getBestScore() - legacyStartScore;

        System.out.printf("[BENCHMARK] === Legacy (render+compare) ===%n");
        System.out.printf("[BENCHMARK] Duration: %d ms%n", legacyElapsed);
        System.out.printf("[BENCHMARK] Iterations: %d (%.0f iter/sec)%n",
            legacyIters, legacyIters * 1000.0 / legacyElapsed);
        System.out.printf("[BENCHMARK] Score: %.10f -> %.10f (gain: %.6f)%n",
            legacyStartScore, legacyEvolver.getBestScore(), legacyGain);

        // --- Delta evolution ---
        ImageEvolver deltaEvolver = ArtEvolverTools.getDefaultImageEvolver(
            1, 2, 2, 2, "000_zeldathumb-1920-789452.jpg", false,
            38, 39, 1f);
        deltaEvolver.initDeltaEngine();
        deltaEvolver.setUseDeltaEvolution(true);

        double deltaStartScore = deltaEvolver.getBestScore();
        long deltaStartIter = deltaEvolver.getTotalIterations();

        t0 = System.nanoTime();
        deadline = t0 + (long) testDurationMs * 1_000_000L;
        while (System.nanoTime() < deadline) {
            deltaEvolver.evolveDelta(0L, 10);
        }
        long deltaElapsed = (System.nanoTime() - t0) / 1_000_000;
        long deltaIters = deltaEvolver.getTotalIterations() - deltaStartIter;
        double deltaGain = deltaEvolver.getBestScore() - deltaStartScore;

        System.out.printf("[BENCHMARK] === Delta Evolution ===%n");
        System.out.printf("[BENCHMARK] Duration: %d ms%n", deltaElapsed);
        System.out.printf("[BENCHMARK] Iterations: %d (%.0f iter/sec)%n",
            deltaIters, deltaIters * 1000.0 / deltaElapsed);
        System.out.printf("[BENCHMARK] Score: %.10f -> %.10f (gain: %.6f)%n",
            deltaStartScore, deltaEvolver.getBestScore(), deltaGain);

        // --- Comparison ---
        double iterSpeedup = (deltaIters * 1000.0 / deltaElapsed) / (legacyIters * 1000.0 / legacyElapsed);
        double gainSpeedup = deltaGain / Math.max(legacyGain, 1e-15);
        System.out.printf("[BENCHMARK] === Speedup ===%n");
        System.out.printf("[BENCHMARK] Iteration throughput: %.1fx faster%n", iterSpeedup);
        System.out.printf("[BENCHMARK] Score gain: %.1fx more gain%n", gainSpeedup);

        assertTrue(deltaIters > legacyIters, "Delta should achieve more iterations");
    }

    /**
     * Verifies LAP solver correctness on small known cost matrices
     * where the optimal assignment can be determined by inspection.
     */
    @Test
    void lapSolverUnitTest() {
        // 2x2: cost = [[1, 9], [9, 1]] → optimal: row0→col0, row1→col1 (cost=2)
        int[] cost2 = {1, 9, 9, 1};
        int[] result2 = LAPSolver.solve(cost2, 2);
        assertEquals(0, result2[0], "Row 0 should map to col 0");
        assertEquals(1, result2[1], "Row 1 should map to col 1");

        // 3x3: cost = [[10, 5, 13], [3, 7, 15], [6, 12, 8]]
        // Optimal: row0→col1(5), row1→col0(3), row2→col2(8) = 16
        int[] cost3 = {10, 5, 13, 3, 7, 15, 6, 12, 8};
        int[] result3 = LAPSolver.solve(cost3, 3);
        int totalCost = cost3[result3[0]] + cost3[3 + result3[1]] + cost3[6 + result3[2]];
        assertEquals(16, totalCost, "3x3 optimal cost should be 16");
        assertTrue(result3[0] != result3[1] && result3[1] != result3[2] && result3[0] != result3[2],
            "All assignments must be distinct columns");

        // 1x1: trivial
        int[] cost1 = {42};
        int[] result1 = LAPSolver.solve(cost1, 1);
        assertEquals(0, result1[0]);

        // 4x4 identity-like: diagonal is cheapest
        int[] cost4 = {
            0, 100, 100, 100,
            100, 0, 100, 100,
            100, 100, 0, 100,
            100, 100, 100, 0
        };
        int[] result4 = LAPSolver.solve(cost4, 4);
        assertArrayEquals(new int[]{0, 1, 2, 3}, result4, "Diagonal should be optimal");

        System.out.println("[LAP UNIT] All small matrix tests passed");
    }

    /**
     * Tests the LAP solver: builds cost matrix, solves, and compares score
     * against smart initialization and random initialization.
     */
    @Test
    void lapSolverOptimal() throws IOException, URISyntaxException {

        int savedMethod = ImageEvolver.INITIALIZATION_METHOD;
        boolean savedSmart = ImageEvolver.SMART_INITIALIZATION;

        try {
            // Random init
            ImageEvolver.INITIALIZATION_METHOD = ImageEvolver.INIT_RANDOM;
            ImageEvolver.SMART_INITIALIZATION = false;
            ImageEvolver randomEvolver = ArtEvolverTools.getDefaultImageEvolver(
                1, 2, 2, 2, "000_zeldathumb-1920-789452.jpg", false,
                38, 39, 1f);
            double randomScore = randomEvolver.getPopulation().get(0).getScore();
            System.out.printf("[LAP TEST] Random init score:  %.10f%n", randomScore);

            // Smart init
            ImageEvolver.INITIALIZATION_METHOD = ImageEvolver.INIT_SMART;
            ImageEvolver.SMART_INITIALIZATION = true;
            ImageEvolver smartEvolver = ArtEvolverTools.getDefaultImageEvolver(
                1, 2, 2, 2, "000_zeldathumb-1920-789452.jpg", false,
                38, 39, 1f);
            double smartScore = smartEvolver.getPopulation().get(0).getScore();
            System.out.printf("[LAP TEST] Smart init score:   %.10f%n", smartScore);

            // LAP optimal
            ImageEvolver.INITIALIZATION_METHOD = ImageEvolver.INIT_LAP_OPTIMAL;
            long t0 = System.nanoTime();
            ImageEvolver lapEvolver = ArtEvolverTools.getDefaultImageEvolver(
                1, 2, 2, 2, "000_zeldathumb-1920-789452.jpg", false,
                38, 39, 1f);
            long lapMs = (System.nanoTime() - t0) / 1_000_000;
            double lapScore = lapEvolver.getPopulation().get(0).getScore();
            System.out.printf("[LAP TEST] LAP optimal score:  %.10f (in %d ms)%n", lapScore, lapMs);

            System.out.printf("[LAP TEST] LAP vs Smart gain:  +%.6f%n", lapScore - smartScore);
            System.out.printf("[LAP TEST] LAP vs Random gain: +%.6f%n", lapScore - randomScore);

            assertTrue(lapScore >= smartScore,
                "LAP should be >= smart init score");
            assertTrue(lapScore > randomScore,
                "LAP should be > random init score");
        } finally {
            ImageEvolver.INITIALIZATION_METHOD = savedMethod;
            ImageEvolver.SMART_INITIALIZATION = savedSmart;
        }
    }
}
