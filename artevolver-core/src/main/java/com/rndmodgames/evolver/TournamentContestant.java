package com.rndmodgames.evolver;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
    private final List<Thread> evolverThreads = new ArrayList<>();
    private TriangleList<Triangle> bestPop = new TriangleList<>();
    private volatile BufferedImage bestImage;
    private volatile double bestScore = 0;
    private long totalIterations;
    private long goodIterations;
    private volatile boolean running;
    private long startTimeMs;
    private int generation;
    private String parentage = "initial";
    private String breedType = "";
    private boolean eliminated = false;
    private boolean promoted = false;
    private int eliminatedAtGeneration = -1;
    private int promotedAtGeneration = -1;
    private double finalScore = 0;
    private int graceTicks = 0;
    private final FitnessTracker fitnessTracker = new FitnessTracker();
    private LineageNode lineageNode;

    // --- Multi-stage (geared) evolution ---
    private int currentStageIndex = 0;
    private long lastStageChangeMs = 0;
    private int totalStageShifts = 0;

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

    private static final AtomicInteger colorIndex = new AtomicInteger(0);

    public TournamentContestant(String id, String name) {
        this.id = id;
        this.name = name;
        this.chartColor = PRESET_COLORS[colorIndex.getAndIncrement() % PRESET_COLORS.length];
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
     * Renders an initial best image immediately so the UI never shows a blank.
     */
    public void initializeWithImage(BufferedImage resizedOriginal) {
        ImageEvolver.INITIALIZATION_METHOD = config.initializationMethod;
        ImageEvolver.SMART_INITIALIZATION = (config.initializationMethod == 1);

        for (ImageEvolver ev : evolvers) {
            ev.setResizedOriginal(resizedOriginal);
            ev.initializeIsosceles();
            ev.initDeltaEngine();
        }

        // Render an initial image from the first evolver's best population
        if (!evolvers.isEmpty()) {
            ImageEvolver first = evolvers.get(0);
            TriangleList<Triangle> initPop = first.getBestPop();
            if (initPop != null) {
                bestImage = first.renderTrianglesToNewImage(initPop);
                bestScore = initPop.getScore();
                bestPop = initPop;
                first.setDirty(true);
            }
        }
    }

    /** Starts all evolver threads that haven't been started yet. */
    public void start() {
        running = true;
        startTimeMs = System.currentTimeMillis();
        if (lastStageChangeMs == 0) lastStageChangeMs = startTimeMs;
        for (ImageEvolver ev : evolvers) {
            if (!ev.isStarted) {
                Thread t = new Thread(ev);
                t.setDaemon(true);
                t.setName("Contestant-" + name + "-Evolver-" + ev.getId());
                t.start();
                ev.isStarted = true;
                evolverThreads.add(t);
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

    /** Fully stops and disposes all evolver threads. Used when culling. */
    public void dispose() {
        stop();
        for (Thread t : evolverThreads) {
            t.interrupt();
        }
        evolverThreads.clear();
        evolvers.clear();
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
                    BufferedImage img = ev.getBestImage();
                    if (img != null) bestImage = img;
                    bestPop = ev.getBestPop();
                    improved = true;
                    ev.setDirty(false);
                } else if (evScore > 0) {
                    ev.setDirty(false);
                }
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
    public int getGeneration() { return generation; }
    public void setGeneration(int generation) { this.generation = generation; }
    public String getParentage() { return parentage; }
    public void setParentage(String parentage) { this.parentage = parentage; }
    public String getBreedType() { return breedType; }
    public void setBreedType(String breedType) { this.breedType = breedType; }
    public boolean isEliminated() { return eliminated; }
    public boolean isPromoted() { return promoted; }
    public boolean isFinished() { return eliminated || promoted; }
    public int getEliminatedAtGeneration() { return eliminatedAtGeneration; }
    public int getPromotedAtGeneration() { return promotedAtGeneration; }
    public double getFinalScore() { return finalScore; }

    public int getGraceTicks() { return graceTicks; }
    public void setGraceTicks(int graceTicks) { this.graceTicks = graceTicks; }
    public void decrementGraceTicks() { if (graceTicks > 0) graceTicks--; }
    public boolean isProtected() { return graceTicks > 0; }
    public FitnessTracker getFitnessTracker() { return fitnessTracker; }
    public LineageNode getLineageNode() { return lineageNode; }
    public void setLineageNode(LineageNode node) { this.lineageNode = node; }

    // ═══════════════════════════════════════════════════════════
    //  MULTI-STAGE (GEAR) TRANSITION ENGINE
    // ═══════════════════════════════════════════════════════════

    public boolean isMultiStage() { return config != null && config.isMultiStage(); }
    public int getCurrentStageIndex() { return currentStageIndex; }
    public int getTotalStages() { return config != null ? config.getStageCount() : 1; }
    public boolean hasMoreStages() {
        return isMultiStage() && currentStageIndex < config.getStages().size() - 1;
    }
    public boolean isOnLastStage() { return !hasMoreStages(); }

    public String getCurrentStageName() {
        if (!isMultiStage()) return "";
        java.util.List<EvolutionStage> stages = config.getStages();
        if (currentStageIndex < stages.size()) return stages.get(currentStageIndex).getName();
        return "";
    }

    /**
     * Checks if the current stage's trigger condition is met and advances if so.
     * Called from the ArtEvolver process timer alongside updateBest().
     * @return true if a stage transition occurred
     */
    public boolean checkStageTransition() {
        if (!isMultiStage() || !running || isFinished()) return false;
        java.util.List<EvolutionStage> stages = config.getStages();
        if (currentStageIndex >= stages.size() - 1) return false;

        EvolutionStage current = stages.get(currentStageIndex);
        boolean shouldAdvance = false;

        switch (current.getTriggerType()) {
            case TIME:
                long ageSec = (System.currentTimeMillis() - startTimeMs) / 1000;
                shouldAdvance = ageSec >= current.getTriggerValue();
                break;
            case STALE:
                double vel = fitnessTracker.getVelocity();
                double elapsed = fitnessTracker.getElapsedSeconds();
                shouldAdvance = elapsed >= 5 && Math.abs(vel) < current.getTriggerValue();
                break;
            case FITNESS:
                shouldAdvance = bestScore >= current.getTriggerValue();
                break;
        }

        if (shouldAdvance) {
            advanceStage();
            return true;
        }
        return false;
    }

    /**
     * Forces advancement to the next stage. Hot-swaps mutation parameters on
     * the live config object — evolvers pick up the new values immediately.
     */
    public void advanceStage() {
        if (!hasMoreStages()) return;
        currentStageIndex++;
        lastStageChangeMs = System.currentTimeMillis();
        totalStageShifts++;
        EvolutionStage next = config.getStages().get(currentStageIndex);
        applyStage(next);
        System.out.println("[Stage] " + name + " shifted to gear "
                + (currentStageIndex + 1) + "/" + config.getStages().size()
                + ": " + next.getName());
    }

    /**
     * Copies stage mutation parameters into the live config. Evolvers read these
     * fields by reference every batch, so the change takes effect immediately.
     * Thread/population/crossover settings are NOT changed (would require restart).
     */
    private void applyStage(EvolutionStage stage) {
        EvolutionConfig sc = stage.getConfig();
        config.gridMutationChances = sc.gridMutationChances;
        config.gridMutationDecay = sc.gridMutationDecay;
        config.gridMutationPercent = sc.gridMutationPercent;
        config.randomMutationChances = sc.randomMutationChances;
        config.randomMutationPercent = sc.randomMutationPercent;
        config.closeMutationChances = sc.closeMutationChances;
        config.closeMutationPercent = sc.closeMutationPercent;
        config.randomGridMutationChances = sc.randomGridMutationChances;
        config.randomGridMutationPercent = sc.randomGridMutationPercent;
        config.targetedSwapAttempts = sc.targetedSwapAttempts;
    }

    public long getLastStageChangeMs() { return lastStageChangeMs; }
    public int getTotalStageShifts() { return totalStageShifts; }

    /** Marks this contestant as eliminated (poor performance). Frees resources. */
    public void eliminate(int atGeneration) {
        this.eliminated = true;
        this.promoted = false; // Clear promoted flag if demoted from hall of fame
        this.eliminatedAtGeneration = atGeneration;
        if (this.finalScore <= 0) this.finalScore = bestScore;
        if (lineageNode != null) {
            lineageNode.setEliminated(true);
            lineageNode.setPeakFitness(fitnessTracker.getPeakFitness());
            lineageNode.setPeakVelocity(fitnessTracker.getPeakVelocity());
        }
        dispose();
    }

    /**
     * Promotes this contestant to the hall of fame. Frees compute resources but
     * keeps config and score available for breeding. Promoted contestants can
     * be selected as parents for future generations.
     */
    public void promote(int atGeneration) {
        this.promoted = true;
        this.promotedAtGeneration = atGeneration;
        this.finalScore = bestScore;
        if (lineageNode != null) {
            lineageNode.setPeakFitness(fitnessTracker.getPeakFitness());
            lineageNode.setPeakVelocity(fitnessTracker.getPeakVelocity());
        }
        dispose();
    }
}
