package com.rndmodgames.evolver;

import javax.swing.Timer;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Prehistoric Mode — Progressive Evolution Tournament.
 *
 * Gamifies the evolutionary tournament by starting from the most primitive
 * configuration possible (1 thread, random init, legacy evolve, random swaps
 * only) and progressively unlocking capabilities through geological "eras".
 *
 * Each era introduces new algorithm features, more threads, and more contestants,
 * creating a visible narrative of technological progression. At Era 7 ("Age of
 * Intelligence"), the existing EvolutionaryTournament system activates for
 * automated meta-optimization.
 */
public class PrehistoricMode {

    // ════════════════════════════════════════════════════════════════
    //  ERA DEFINITIONS
    // ════════════════════════════════════════════════════════════════

    public static final int ERA_COUNT = 8;

    public static final String[] ERA_NAMES = {
        "Primordial Soup",
        "Single Cell",
        "Multicellular",
        "Cambrian Explosion",
        "Age of Fish",
        "Age of Reptiles",
        "Age of Mammals",
        "Age of Intelligence",
    };

    public static final String[] ERA_DESCRIPTIONS = {
        "1 thread, random init, random swaps only — pure brute-force",
        "Grid swaps + close mutations unlocked",
        "Smart init, population=2, crossover enabled",
        "Delta evolution + targeted swaps — major inflection point",
        "2 threads, full mutation suite",
        "Scaling threads, preset strategies introduced",
        "Full thread allocation, all strategies deployed",
        "Evolutionary tournament activates — meta-optimization begins",
    };

    /** Which capabilities are available at each era (bitmask-style for display). */
    public static final String[][] ERA_CAPABILITIES = {
        {"Random Init", "Random Swaps", "Legacy Evolve"},
        {"Random Init", "Random+Grid+Close Swaps", "Legacy Evolve"},
        {"Smart Init", "All Swaps", "Crossover", "Pop=2", "Legacy Evolve"},
        {"Smart Init", "All Swaps", "Crossover", "Targeted", "Delta Evolve"},
        {"Smart Init", "Full Mutations", "Delta Evolve", "2 Threads"},
        {"Smart Init", "Full Mutations", "Delta Evolve", "Scaling Threads", "Presets"},
        {"Smart Init", "Full Mutations", "Delta Evolve", "Full Threads", "All Presets"},
        {"Full Config", "Evo Tournament", "Meta-GA", "Multi-Spawn", "Adaptive"},
    };

    // ════════════════════════════════════════════════════════════════
    //  STATE
    // ════════════════════════════════════════════════════════════════

    private final ArtEvolver artEvolver;
    private final List<TournamentContestant> contestants;
    private int currentEra = 0;
    private boolean active = false;
    private boolean autoAdvance = false;
    private int eraDurationSeconds = 60;
    private long eraStartMs = 0;
    private Timer autoAdvanceTimer;

    private int nextPresetIndex = 0;
    private int nextContestantId = 1;

    private final List<EraRecord> history = new ArrayList<>();

    // Thread budget grows with eras
    private int threadsPerContestant = 1;
    private int maxThreadsBudget;

    // Strategy names (same as Quick Setup, reused for preset pool)
    static final String[] PRESET_STRATEGIES = {
        "Balanced", "Aggressive Explorer", "Grid Refiner", "Targeted Precision",
        "Heavy Random", "Close Mutation Focus", "Fast Convergence", "Wide Search",
    };

    // ════════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ════════════════════════════════════════════════════════════════

    public PrehistoricMode(ArtEvolver artEvolver, List<TournamentContestant> contestants) {
        this.artEvolver = artEvolver;
        this.contestants = contestants;
        this.maxThreadsBudget = Math.max(2, Runtime.getRuntime().availableProcessors() - 2);
    }

    // ════════════════════════════════════════════════════════════════
    //  START / STOP
    // ════════════════════════════════════════════════════════════════

    public boolean start() {
        if (active) return false;
        if (artEvolver.getResizedOriginal() == null) return false;

        active = true;
        currentEra = 0;
        nextPresetIndex = 0;
        nextContestantId = 1;
        threadsPerContestant = 1;
        history.clear();

        // Clear existing contestants
        for (TournamentContestant c : new ArrayList<>(contestants)) {
            if (c.isRunning()) c.stop();
            c.dispose();
        }
        contestants.clear();

        enterEra(0);
        return true;
    }

    public void stop() {
        active = false;
        if (autoAdvanceTimer != null) {
            autoAdvanceTimer.stop();
            autoAdvanceTimer = null;
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  ERA PROGRESSION
    // ════════════════════════════════════════════════════════════════

    private void enterEra(int era) {
        currentEra = era;
        eraStartMs = System.currentTimeMillis();

        System.out.println("[Prehistoric] ═══ ERA " + era + ": " + ERA_NAMES[era] + " ═══");
        System.out.println("[Prehistoric] " + ERA_DESCRIPTIONS[era]);

        EraRecord rec = new EraRecord(era, ERA_NAMES[era]);
        rec.timestamp = System.currentTimeMillis();
        rec.aliveCount = (int) contestants.stream().filter(c -> !c.isEliminated()).count();

        switch (era) {
            case 0: spawnPrimordialContestant(); break;
            case 1: spawnEra1Contestant(); break;
            case 2: spawnEra2Contestant(); break;
            case 3: spawnEra3Contestant(); break;
            case 4: spawnEra4Contestant(); break;
            case 5: spawnEra5Contestant(); break;
            case 6: spawnEra6Contestants(); break;
            case 7: activateIntelligenceEra(); break;
        }

        rec.aliveCountAfter = (int) contestants.stream().filter(c -> !c.isEliminated()).count();
        history.add(rec);

        // Restart auto-advance timer if enabled
        restartAutoAdvanceTimer();

        artEvolver.refreshContestantCombo();
        if (artEvolver.getTournamentManagerWindow() != null) {
            artEvolver.getTournamentManagerWindow().refreshTable();
        }
    }

    public boolean advanceEra() {
        if (!active) return false;
        if (currentEra >= ERA_COUNT - 1) return false;

        enterEra(currentEra + 1);
        return true;
    }

    // ════════════════════════════════════════════════════════════════
    //  ERA-SPECIFIC CONTESTANT SPAWNING
    // ════════════════════════════════════════════════════════════════

    private void spawnPrimordialContestant() {
        EvolutionConfig cfg = EvolutionConfig.createPrimordial();
        cfg.name = "Primordial-1";
        spawnAndStart(cfg);
    }

    private void spawnEra1Contestant() {
        EvolutionConfig cfg = EvolutionConfig.createEra1();
        cfg.name = "Cell-" + nextContestantId;
        spawnAndStart(cfg);
    }

    private void spawnEra2Contestant() {
        EvolutionConfig cfg = EvolutionConfig.createEra2();
        cfg.name = "Multi-" + nextContestantId;
        spawnAndStart(cfg);
    }

    private void spawnEra3Contestant() {
        EvolutionConfig cfg = EvolutionConfig.createEra3();
        cfg.name = "Cambrian-" + nextContestantId;
        spawnAndStart(cfg);
    }

    private void spawnEra4Contestant() {
        threadsPerContestant = 2;
        EvolutionConfig cfg = EvolutionConfig.createEra4();
        cfg.threads = threadsPerContestant;
        cfg.name = "Fish-" + nextContestantId;
        spawnAndStart(cfg);
    }

    private void spawnEra5Contestant() {
        threadsPerContestant = Math.min(threadsPerContestant + 1, maxThreadsBudget / 2);
        EvolutionConfig cfg = EvolutionConfig.createEra5(threadsPerContestant);
        cfg.name = "Reptile-" + nextContestantId;
        spawnAndStart(cfg);
    }

    private void spawnEra6Contestants() {
        int availableThreads = maxThreadsBudget;
        int aliveCount = (int) contestants.stream().filter(c -> !c.isEliminated()).count();
        int threadsUsed = aliveCount * threadsPerContestant;
        int remaining = availableThreads - threadsUsed;

        threadsPerContestant = Math.max(2, availableThreads / (aliveCount + 4));

        int toSpawn = Math.min(3, remaining / Math.max(1, threadsPerContestant));
        toSpawn = Math.max(1, toSpawn);

        for (int i = 0; i < toSpawn; i++) {
            EvolutionConfig cfg = EvolutionConfig.createEra6(threadsPerContestant);
            cfg.name = "Mammal-" + nextContestantId;
            if (nextPresetIndex < PRESET_STRATEGIES.length) {
                cfg = applyPresetStrategy(cfg, nextPresetIndex);
                nextPresetIndex++;
            }
            spawnAndStart(cfg);
        }
    }

    private void activateIntelligenceEra() {
        System.out.println("[Prehistoric] Era 7: Activating Evolutionary Tournament...");

        TournamentManagerWindow tmw = artEvolver.getTournamentManagerWindow();
        if (tmw != null && tmw.getEvoTournament() == null) {
            tmw.createEvoTournament();
        }
        if (tmw != null && tmw.getEvoTournament() != null) {
            EvolutionaryTournament evo = tmw.getEvoTournament();
            evo.setAdaptiveCutoff(true);
            if (!evo.isRunning()) {
                evo.start();
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  MANUAL ADD BUTTONS
    // ════════════════════════════════════════════════════════════════

    /**
     * Adds +1 thread to all alive contestants (user-triggered).
     * @return new thread count per contestant
     */
    public int addThread() {
        threadsPerContestant = Math.min(threadsPerContestant + 1, maxThreadsBudget);
        System.out.println("[Prehistoric] +1 thread -> " + threadsPerContestant + " threads/contestant");
        return threadsPerContestant;
    }

    /**
     * Adds a new contestant using the next preset strategy from the pool.
     * Applies the current era's capabilities as constraints.
     * @return name of the new contestant, or null if no image loaded
     */
    public String addPresetContestant() {
        if (!active || artEvolver.getResizedOriginal() == null) return null;

        EvolutionConfig base = createConfigForCurrentEra();
        base.threads = threadsPerContestant;

        if (nextPresetIndex < PRESET_STRATEGIES.length) {
            base = applyPresetStrategy(base, nextPresetIndex);
            nextPresetIndex++;
        } else {
            base.name = "Preset-" + nextContestantId + " (recycled)";
            base = applyPresetStrategy(base, nextContestantId % PRESET_STRATEGIES.length);
        }

        spawnAndStart(base);
        return base.name;
    }

    /**
     * Breeds a new contestant from the top 2 alive performers.
     * @return name of the new contestant, or null if not enough contestants
     */
    public String addEvolvedContestant() {
        if (!active || artEvolver.getResizedOriginal() == null) return null;

        List<TournamentContestant> alive = new ArrayList<>();
        for (TournamentContestant c : contestants) {
            if (!c.isEliminated() && c.getBestScore() > 0) alive.add(c);
        }
        if (alive.size() < 2) return null;

        alive.sort((a, b) -> Double.compare(b.getBestScore(), a.getBestScore()));
        TournamentContestant parentA = alive.get(0);
        TournamentContestant parentB = alive.get(1);

        EvolutionConfig childConfig = simpleBreed(parentA.getConfig(), parentB.getConfig());
        childConfig.threads = threadsPerContestant;

        // Respect current era constraints
        constrainToEra(childConfig, currentEra);

        String childName = "Evolved-" + nextContestantId
                + " (" + parentA.getName().substring(0, Math.min(4, parentA.getName().length()))
                + "x" + parentB.getName().substring(0, Math.min(4, parentB.getName().length())) + ")";
        childConfig.name = childName;

        spawnAndStart(childConfig);
        System.out.println("[Prehistoric] Bred evolved contestant: " + childName);
        return childName;
    }

    public boolean arePresetsExhausted() {
        return nextPresetIndex >= PRESET_STRATEGIES.length;
    }

    // ════════════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════════════

    private void spawnAndStart(EvolutionConfig cfg) {
        cfg.chartColor = null; // Let TournamentContestant assign
        TournamentContestant c = new TournamentContestant(
                "pre" + nextContestantId, cfg.name);
        nextContestantId++;
        c.setConfig(cfg);
        cfg.chartColor = c.getChartColor();
        contestants.add(c);

        BufferedImage resized = artEvolver.getResizedOriginal();
        if (resized != null) {
            try {
                c.createEvolvers(artEvolver.getPallete(),
                        artEvolver.getTriangleWidth(), artEvolver.getTriangleHeight(),
                        artEvolver.getWidthTriangles(), artEvolver.getHeightTriangles(),
                        artEvolver.getTriangleScaleHeight(), artEvolver.getJumpDistances());
                c.initializeWithImage(resized);
                c.start();
            } catch (Exception ex) {
                System.err.println("[Prehistoric] Failed to start " + cfg.name + ": " + ex.getMessage());
            }
        }

        // Ensure the process timer is running
        if (!artEvolver.isRunning()) {
            artEvolver.setTournamentMode(true);
            artEvolver.startProcessTimer();
        }

        artEvolver.refreshContestantCombo();
        System.out.println("[Prehistoric] Spawned: " + cfg.name
                + " [" + cfg.threads + "T, " + (cfg.useDeltaEvolution ? "delta" : "legacy")
                + ", init=" + cfg.initializationMethod + "]");
    }

    private EvolutionConfig createConfigForCurrentEra() {
        switch (currentEra) {
            case 0: return EvolutionConfig.createPrimordial();
            case 1: return EvolutionConfig.createEra1();
            case 2: return EvolutionConfig.createEra2();
            case 3: return EvolutionConfig.createEra3();
            case 4: return EvolutionConfig.createEra4();
            case 5: return EvolutionConfig.createEra5(threadsPerContestant);
            case 6: return EvolutionConfig.createEra6(threadsPerContestant);
            default: return EvolutionConfig.createEra7(threadsPerContestant);
        }
    }

    /** Ensures a config doesn't use features beyond the current era. */
    private void constrainToEra(EvolutionConfig cfg, int era) {
        if (era < 3) {
            cfg.useDeltaEvolution = false;
            cfg.targetedSwapAttempts = 0;
        }
        if (era < 2) {
            cfg.blockCrossoverEnabled = false;
            cfg.initializationMethod = 0;
            cfg.population = 1;
            cfg.crossoverMax = 1;
        }
        if (era < 1) {
            cfg.gridMutationChances = 0;
            cfg.closeMutationChances = 0;
        }
        if (era < 4) {
            cfg.threads = 1;
        }
    }

    private EvolutionConfig applyPresetStrategy(EvolutionConfig base, int strategyIndex) {
        EvolutionConfig cfg = base.clone();
        int idx = strategyIndex % PRESET_STRATEGIES.length;
        cfg.name = PRESET_STRATEGIES[idx];

        switch (idx) {
            case 0: break; // Balanced — use base
            case 1: // Aggressive Explorer
                cfg.gridMutationChances = Math.max(4, cfg.gridMutationChances / 4);
                cfg.randomMutationChances = cfg.randomMutationChances * 4;
                cfg.randomMutationPercent = Math.min(1f, cfg.randomMutationPercent * 4);
                cfg.targetedSwapAttempts = Math.max(0, cfg.targetedSwapAttempts / 2);
                break;
            case 2: // Grid Refiner
                cfg.gridMutationChances = cfg.gridMutationChances * 4;
                cfg.gridMutationDecay = cfg.gridMutationDecay * 0.5f;
                cfg.randomMutationChances = Math.max(10, cfg.randomMutationChances / 4);
                cfg.targetedSwapAttempts = cfg.targetedSwapAttempts * 2;
                break;
            case 3: // Targeted Precision
                cfg.targetedSwapAttempts = cfg.targetedSwapAttempts * 4;
                cfg.gridMutationChances = Math.max(4, cfg.gridMutationChances / 2);
                cfg.randomMutationChances = Math.max(10, cfg.randomMutationChances / 2);
                break;
            case 4: // Heavy Random
                cfg.randomMutationChances = cfg.randomMutationChances * 8;
                cfg.randomMutationPercent = Math.min(1f, cfg.randomMutationPercent * 2);
                cfg.gridMutationChances = 0;
                cfg.targetedSwapAttempts = Math.max(0, cfg.targetedSwapAttempts / 4);
                break;
            case 5: // Close Mutation Focus
                cfg.closeMutationChances = cfg.closeMutationChances * 6;
                cfg.closeMutationPercent = Math.min(1f, cfg.closeMutationPercent * 4);
                cfg.gridMutationChances = Math.max(4, cfg.gridMutationChances / 2);
                cfg.randomMutationChances = Math.max(10, cfg.randomMutationChances / 4);
                break;
            case 6: // Fast Convergence
                cfg.gridMutationChances = cfg.gridMutationChances * 2;
                cfg.targetedSwapAttempts = cfg.targetedSwapAttempts * 3;
                cfg.closeMutationChances = cfg.closeMutationChances * 2;
                break;
            case 7: // Wide Search
                cfg.population = Math.max(cfg.population, 4);
                cfg.gridMutationChances = cfg.gridMutationChances * 2;
                cfg.randomMutationChances = cfg.randomMutationChances * 2;
                break;
        }

        constrainToEra(cfg, currentEra);
        return cfg;
    }

    /** Simple BLX-alpha crossover + Gaussian mutation for breeding outside the EvolutionaryTournament. */
    private static final java.util.SplittableRandom BREED_RNG = new java.util.SplittableRandom();
    private static final float BLX_ALPHA = 0.5f;

    private EvolutionConfig simpleBreed(EvolutionConfig cfgA, EvolutionConfig cfgB) {
        float[] genesA = cfgA.toGeneArray();
        float[] genesB = cfgB.toGeneArray();
        float[] child = new float[EvolutionConfig.GENE_COUNT];
        float[] min = EvolutionConfig.getGeneMin();
        float[] max = EvolutionConfig.getGeneMax();

        for (int i = 0; i < child.length; i++) {
            float lo = Math.min(genesA[i], genesB[i]);
            float hi = Math.max(genesA[i], genesB[i]);
            float d = hi - lo;
            float cLo = lo - BLX_ALPHA * d;
            float cHi = hi + BLX_ALPHA * d;
            child[i] = cLo + BREED_RNG.nextFloat() * (cHi - cLo);
            // Mutation
            if (BREED_RNG.nextFloat() < 0.3f) {
                float range = max[i] - min[i];
                child[i] += (float)(BREED_RNG.nextGaussian() * 0.15 * range);
            }
            child[i] = Math.max(min[i], Math.min(max[i], child[i]));
        }
        return EvolutionConfig.fromGeneArray(child, cfgA);
    }

    private void restartAutoAdvanceTimer() {
        if (autoAdvanceTimer != null) {
            autoAdvanceTimer.stop();
            autoAdvanceTimer = null;
        }
        if (autoAdvance && currentEra < ERA_COUNT - 1) {
            autoAdvanceTimer = new Timer(eraDurationSeconds * 1000, e -> advanceEra());
            autoAdvanceTimer.setRepeats(false);
            autoAdvanceTimer.start();
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  ACCESSORS
    // ════════════════════════════════════════════════════════════════

    public boolean isActive() { return active; }
    public int getCurrentEra() { return currentEra; }
    public String getCurrentEraName() { return ERA_NAMES[Math.min(currentEra, ERA_COUNT - 1)]; }
    public String getCurrentEraDescription() { return ERA_DESCRIPTIONS[Math.min(currentEra, ERA_COUNT - 1)]; }
    public String[] getCurrentCapabilities() { return ERA_CAPABILITIES[Math.min(currentEra, ERA_COUNT - 1)]; }
    public boolean isAutoAdvance() { return autoAdvance; }
    public int getEraDurationSeconds() { return eraDurationSeconds; }
    public int getThreadsPerContestant() { return threadsPerContestant; }
    public int getMaxThreadsBudget() { return maxThreadsBudget; }
    public List<EraRecord> getHistory() { return history; }
    public int getNextPresetIndex() { return nextPresetIndex; }
    public boolean isAtFinalEra() { return currentEra >= ERA_COUNT - 1; }

    public void setAutoAdvance(boolean auto) {
        this.autoAdvance = auto;
        restartAutoAdvanceTimer();
    }

    public void setEraDurationSeconds(int seconds) {
        this.eraDurationSeconds = Math.max(10, seconds);
    }

    public void setMaxThreadsBudget(int threads) {
        this.maxThreadsBudget = Math.max(2, threads);
    }

    public int getSecondsUntilNextEra() {
        if (!active || !autoAdvance || currentEra >= ERA_COUNT - 1) return -1;
        long elapsed = (System.currentTimeMillis() - eraStartMs) / 1000;
        return Math.max(0, eraDurationSeconds - (int) elapsed);
    }

    // ════════════════════════════════════════════════════════════════
    //  ERA RECORD (for history display)
    // ════════════════════════════════════════════════════════════════

    public static class EraRecord {
        public final int era;
        public final String eraName;
        public long timestamp;
        public int aliveCount;
        public int aliveCountAfter;

        EraRecord(int era, String name) {
            this.era = era;
            this.eraName = name;
        }

        @Override
        public String toString() {
            return "Era " + era + ": " + eraName
                    + " (" + aliveCount + " -> " + aliveCountAfter + " contestants)";
        }
    }
}
