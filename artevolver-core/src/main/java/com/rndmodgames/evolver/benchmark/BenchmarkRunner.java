package com.rndmodgames.evolver.benchmark;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import com.rndmodgames.evolver.ArtEvolver;
import com.rndmodgames.evolver.ImageEvolver;
import com.rndmodgames.evolver.Palette;

/**
 * Automated benchmark runner for ArtEvolver evolution engine.
 *
 * Runs headless (no GUI) evolution for a fixed number of iterations,
 * recording structured metrics at regular intervals. Produces CSV output
 * files and human-readable summary reports.
 *
 * Usage from tests or command line:
 * <pre>
 *   BenchmarkRunner runner = new BenchmarkRunner.Builder()
 *       .imagePath("./src/test/resources/000_zeldathumb-1920-789452.jpg")
 *       .totalIterations(10_000)
 *       .snapshotInterval(500)
 *       .threads(8)
 *       .population(2)
 *       .build();
 *
 *   BenchmarkRunner.Result result = runner.run();
 *   System.out.println(result.summary);
 * </pre>
 */
public class BenchmarkRunner {

    private final String imagePath;
    private final int totalIterations;
    private final int snapshotInterval;
    private final int threads;
    private final int population;
    private final int palettes;
    private final int widthTriangles;
    private final int heightTriangles;
    private final float triangleScale;
    private final String outputDir;
    private final String label;
    private final boolean shufflePopulation;

    private BenchmarkRunner(Builder builder) {
        this.imagePath = builder.imagePath;
        this.totalIterations = builder.totalIterations;
        this.snapshotInterval = builder.snapshotInterval;
        this.threads = builder.threads;
        this.population = builder.population;
        this.palettes = builder.palettes;
        this.widthTriangles = builder.widthTriangles;
        this.heightTriangles = builder.heightTriangles;
        this.triangleScale = builder.triangleScale;
        this.outputDir = builder.outputDir;
        this.label = builder.label;
        this.shufflePopulation = builder.shufflePopulation;
    }

    public static class Result {
        public final String summary;
        public final String csvPath;
        public final List<BenchmarkLogger.Snapshot> snapshots;
        public final double finalScore;
        public final long elapsedMs;

        Result(String summary, String csvPath,
               List<BenchmarkLogger.Snapshot> snapshots,
               double finalScore, long elapsedMs) {
            this.summary = summary;
            this.csvPath = csvPath;
            this.snapshots = snapshots;
            this.finalScore = finalScore;
            this.elapsedMs = elapsedMs;
        }
    }

    /**
     * Executes the benchmark run. Blocks until all iterations complete.
     */
    public Result run() throws IOException, URISyntaxException {
        Palette palette = new Palette("Sherwin-Williams", palettes);

        float width = 3.0f * triangleScale;
        float height = 3.0f * triangleScale;

        int randomJumpMaxDistance = 2;
        int crossoverMax = 2;

        List<ImageEvolver> evolvers = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            ImageEvolver ev = new ImageEvolver(
                    population, randomJumpMaxDistance, crossoverMax,
                    triangleScale, palette, width, height,
                    widthTriangles, heightTriangles);
            ev.setId((long) i);
            evolvers.add(ev);
        }

        File imageFile = new File(imagePath);
        BufferedImage originalImage = ImageIO.read(imageFile);

        int newWidth = (int) (width * widthTriangles);
        int newHeight = (int) ((height * heightTriangles) - height);

        BufferedImage resizedOriginal = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g2d = resizedOriginal.createGraphics();
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                             java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight,
                       0, 0, originalImage.getWidth(), originalImage.getHeight(), null);
        g2d.dispose();

        ImageEvolver.SHUFFLE_PALETTE = shufflePopulation;
        for (ImageEvolver ev : evolvers) {
            ev.setResizedOriginal(resizedOriginal);
            ev.initialize();
        }

        String modeName = threads + "T_" + population + "P_" + (widthTriangles * heightTriangles) + "tri";

        try (BenchmarkLogger logger = new BenchmarkLogger(outputDir, label)) {

            long startMs = System.currentTimeMillis();
            int iterationsPerThread = totalIterations / threads;
            int batchSize = Math.min(snapshotInterval / threads, iterationsPerThread);
            if (batchSize < 1) batchSize = 1;

            int completedIterations = 0;
            double bestScoreGlobal = Double.MIN_VALUE;
            long goodIterationsGlobal = 0;

            while (completedIterations < totalIterations) {
                int batchThisRound = Math.min(batchSize, iterationsPerThread - (completedIterations / threads));
                if (batchThisRound < 1) break;

                for (ImageEvolver ev : evolvers) {
                    ev.evolve(startMs, batchThisRound);
                }

                completedIterations += batchThisRound * threads;
                long totalIter = 0;
                long goodIter = 0;
                for (ImageEvolver ev : evolvers) {
                    totalIter += ev.getTotalIterations();
                    goodIter += ev.getGoodIterations();
                    if (ev.getBestScore() > bestScoreGlobal) {
                        bestScoreGlobal = ev.getBestScore();
                    }
                }
                goodIterationsGlobal = goodIter;

                double healthPct = totalIter > 0
                        ? ((double) goodIter / totalIter) * 100.0
                        : 0;

                logger.record(totalIter, goodIter, healthPct, bestScoreGlobal,
                        threads, population, widthTriangles * heightTriangles, modeName);
            }

            long elapsedMs = System.currentTimeMillis() - startMs;
            String summary = logger.generateSummary();

            return new Result(summary, logger.getFilePath(),
                    logger.getSnapshots(), bestScoreGlobal, elapsedMs);
        }
    }

    public static class Builder {
        private String imagePath = "./src/test/resources/000_zeldathumb-1920-789452.jpg";
        private int totalIterations = 10_000;
        private int snapshotInterval = 500;
        private int threads = 1;
        private int population = 2;
        private int palettes = 1;
        private int widthTriangles = 38;
        private int heightTriangles = 39;
        private float triangleScale = 1f;
        private String outputDir = "benchmarks";
        private String label = "default";
        private boolean shufflePopulation = false;

        public Builder imagePath(String val) { imagePath = val; return this; }
        public Builder totalIterations(int val) { totalIterations = val; return this; }
        public Builder snapshotInterval(int val) { snapshotInterval = val; return this; }
        public Builder threads(int val) { threads = val; return this; }
        public Builder population(int val) { population = val; return this; }
        public Builder palettes(int val) { palettes = val; return this; }
        public Builder widthTriangles(int val) { widthTriangles = val; return this; }
        public Builder heightTriangles(int val) { heightTriangles = val; return this; }
        public Builder triangleScale(float val) { triangleScale = val; return this; }
        public Builder outputDir(String val) { outputDir = val; return this; }
        public Builder label(String val) { label = val; return this; }
        public Builder shufflePopulation(boolean val) { shufflePopulation = val; return this; }

        public BenchmarkRunner build() {
            return new BenchmarkRunner(this);
        }
    }
}
