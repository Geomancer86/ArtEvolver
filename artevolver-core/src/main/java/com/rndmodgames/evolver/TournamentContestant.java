package com.rndmodgames.evolver;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates one independent evolution run in Tournament Mode.
 * Each contestant has its own set of ImageEvolver threads, its own
 * EvolutionConfig, and tracks its own best score/image independently.
 */
public class TournamentContestant {

    private final String id;
    private String name;
    private Color chartColor;
    private EvolutionConfig config;

    private final List<ImageEvolver> evolvers = new ArrayList<>();
    private TriangleList<Triangle> bestPop = new TriangleList<>();
    private BufferedImage bestImage;
    private double bestScore = 0;
    private long totalIterations;
    private long goodIterations;
    private boolean running;
    private long startTimeMs;

    private static final Color[] PRESET_COLORS = {
        new Color(80, 200, 120),   // green
        new Color(66, 133, 244),   // blue
        new Color(234, 67, 53),    // red
        new Color(251, 188, 4),    // yellow
        new Color(171, 71, 188),   // purple
        new Color(0, 188, 212),    // cyan
        new Color(255, 112, 67),   // deep orange
        new Color(124, 179, 66),   // light green
        new Color(255, 167, 38),   // orange
        new Color(141, 110, 99),   // brown
    };

    private static int colorIndex = 0;

    public TournamentContestant(String id, String name) {
        this.id = id;
        this.name = name;
        this.chartColor = PRESET_COLORS[colorIndex++ % PRESET_COLORS.length];
        this.config = EvolutionConfig.fromCurrentSettings();
    }

    /**
     * Creates the ImageEvolver instances for this contestant.
     * Must be called after the source image has been loaded and a palette is available.
     */
    public void createEvolvers(Palette palette, float width, float height,
                               int widthTriangles, int heightTriangles,
                               float triangleScaleHeight, int[] jumpDistances) {
        evolvers.clear();
        bestScore = 0;
        bestImage = null;
        bestPop = new TriangleList<>();
        totalIterations = 0;
        goodIterations = 0;

        for (int i = 0; i < config.threads; i++) {
            int jumpDist = (jumpDistances != null && i < jumpDistances.length)
                    ? jumpDistances[i]
                    : widthTriangles * heightTriangles;
            ImageEvolver ev = new ImageEvolver(
                    config.population, jumpDist, config.crossoverMax,
                    triangleScaleHeight, palette, width, height,
                    widthTriangles, heightTriangles);
            ev.setId((long) i);
            ev.setConfig(config);
            ev.setUseDeltaEvolution(config.useDeltaEvolution);
            evolvers.add(ev);
        }
    }

    /**
     * Initializes all evolvers with the source image (triangles + delta engine).
     */
    public void initializeWithImage(BufferedImage resizedOriginal) {
        ImageEvolver.INITIALIZATION_METHOD = config.initializationMethod;
        ImageEvolver.SMART_INITIALIZATION = (config.initializationMethod == 1);

        for (ImageEvolver ev : evolvers) {
            ev.setResizedOriginal(resizedOriginal);
            ev.initializeIsosceles();
            ev.initDeltaEngine();
        }
    }

    /** Starts all evolver threads that haven't been started yet. */
    public void start() {
        running = true;
        startTimeMs = System.currentTimeMillis();
        for (ImageEvolver ev : evolvers) {
            if (!ev.isStarted) {
                Thread t = new Thread(ev);
                t.setDaemon(true);
                t.setName("Contestant-" + name + "-Evolver-" + ev.getId());
                t.start();
                ev.isStarted = true;
            }
            ev.isRunning = true;
        }
    }

    /** Pauses all evolvers (threads remain alive but idle). */
    public void stop() {
        running = false;
        for (ImageEvolver ev : evolvers) {
            ev.isRunning = false;
        }
    }

    /**
     * Polls all evolvers and updates the best score/image.
     * Called by the ArtEvolver process timer.
     * @return true if a new best was found this cycle
     */
    public boolean updateBest() {
        totalIterations = 0;
        goodIterations = 0;
        boolean improved = false;

        for (ImageEvolver ev : evolvers) {
            totalIterations += ev.getTotalIterations();
            goodIterations += ev.getGoodIterations();

            if (ev.isDirty()) {
                double evScore = ev.getBestScore();
                if (evScore > 0 && evScore > bestScore) {
                    bestScore = evScore;
                    bestImage = ev.getBestImage();
                    bestPop = ev.getBestPop();
                    improved = true;
                    ev.setDirty(false);
                } else if (evScore > 0) {
                    ev.setDirty(false);
                }
                // leave isDirty=true if evolver has no real score yet
            }
        }

        if (improved) {
            for (ImageEvolver ev : evolvers) {
                if (ev.getBestScore() < bestScore) {
                    ev.setBestPop(bestPop);
                }
            }
        }
        return improved;
    }

    // --- Accessors ---

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Color getChartColor() { return chartColor; }
    public void setChartColor(Color c) { this.chartColor = c; }
    public EvolutionConfig getConfig() { return config; }
    public void setConfig(EvolutionConfig config) { this.config = config; }
    public List<ImageEvolver> getEvolvers() { return evolvers; }
    public double getBestScore() { return bestScore; }
    public BufferedImage getBestImage() { return bestImage; }
    public TriangleList<Triangle> getBestPop() { return bestPop; }
    public long getTotalIterations() { return totalIterations; }
    public long getGoodIterations() { return goodIterations; }
    public boolean isRunning() { return running; }
    public long getStartTimeMs() { return startTimeMs; }
}
