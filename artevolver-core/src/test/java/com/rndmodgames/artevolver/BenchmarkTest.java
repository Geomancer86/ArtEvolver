package com.rndmodgames.artevolver;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;

import com.rndmodgames.evolver.ArtEvolverTools;
import com.rndmodgames.evolver.ImageEvolver;
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
}
