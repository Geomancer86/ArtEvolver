package com.rndmodgames.evolver;

import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.*;
import javax.swing.Timer;

/**
 * Meta-genetic algorithm that evolves the GA parameters themselves.
 *
 * v2 — Major redesign addressing the "newcomer problem":
 *   - Composite ranking: weighted blend of fitness, velocity, acceleration, lineage
 *   - Grace period: new contestants are immune from culling for N ticks
 *   - Fitness velocity tracking: measures improvement rate, not just absolute score
 *   - Ancestry tree (LineageNode): full pedigree for multi-generational breeding
 *   - Ancestral crossover: blends genes from grandparents with decaying weight
 *   - Inbreeding prevention: avoids crossing closely related contestants
 *   - All parameters are configurable and future meta-optimizable
 */
public class EvolutionaryTournament {

    private final ArtEvolver artEvolver;
    private final List<TournamentContestant> contestants;
    private Timer cullTimer;
    private Timer lifespanTimer;

    private int generation = 0;
    private boolean running = false;
    private int nextId = 100;

    // --- Core timing ---
    private int cutoffSeconds = 10;
    private long lastTickMs = 0;

    // --- Breeding ---
    private float mutationRate = 0.3f;
    private float mutationStrength = 0.2f;
    private int minContestants = 3;

    // --- Grace period ---
    private int gracePeriodTicks = 2;

    // --- Dynamic tournament ---
    private int spawnsPerTick = 1;
    private boolean adaptiveCutoff = true;
    private int adaptiveCutoffMin = 5;
    private int adaptiveCutoffMax = 300;

    // --- Contestant lifespan cap (0 = disabled) ---
    private int maxLifespanSeconds = 60;

    // --- Promoted pool (hall of fame) ---
    private int maxPromoted = 10;
    private int presetInjectionInterval = 3;
    private int spawnsSinceLastPreset = 0;

    // --- Ranking strategy ---
    public enum RankingStrategy { BALANCED, VELOCITY_FIRST, FITNESS_FIRST, AUTO }
    private RankingStrategy rankingStrategy = RankingStrategy.AUTO;
    private int autoTransitionGen = 10;

    // --- Composite ranking base weights (used by BALANCED, modified by other strategies) ---
    private float fitnessWeight = 0.35f;
    private float velocityWeight = 0.40f;
    private float accelerationWeight = 0.05f;
    private float lineageWeight = 0.20f;

    // --- Lineage / ancestry ---
    private double lineageDecay = 0.7;
    private int ancestryDepth = 3;
    private boolean useAncestralCrossover = true;
    private int velocityWindowSeconds = 30;

    // --- Best-ever tracking ---
    private double bestEverScore = 0;
    private String bestEverName = "";
    private EvolutionConfig bestEverConfig = null;

    // --- Convergence detection ---
    private int stalledGenerations = 0;
    private static final int CONVERGENCE_STALL_LIMIT = 5;
    private static final double CONVERGENCE_IMPROVEMENT_THRESHOLD = 0.0001;
    private double previousBestScore = 0;
    private boolean converged = false;

    // --- Lineage tree root registry ---
    private final Map<String, LineageNode> lineageRegistry = new LinkedHashMap<>();

    private static final float BLX_ALPHA = 0.5f;
    private static final Random RNG = new Random();
    private static final DecimalFormat DF4 = new DecimalFormat("0.0000");
    private static final DecimalFormat DF2 = new DecimalFormat("0.00");

    private final List<GenerationRecord> history = new ArrayList<>();

    public EvolutionaryTournament(ArtEvolver artEvolver, List<TournamentContestant> contestants) {
        this.artEvolver = artEvolver;
        this.contestants = contestants;
    }

    /**
     * @return true if successfully started, false if preconditions not met
     */
    public boolean start() {
        if (running) return false;
        long aliveCount = getAlive().size();
        if (aliveCount < minContestants) {
            System.err.println("[EvoTournament] Cannot start: need " + minContestants
                    + " alive contestants, have " + aliveCount);
            return false;
        }
        running = true;
        converged = false;
        stalledGenerations = 0;
        previousBestScore = 0;
        lastTickMs = System.currentTimeMillis();

        ensureLineageNodes();

        cullTimer = new Timer(cutoffSeconds * 1000, e -> onCutoffTick());
        cullTimer.setInitialDelay(cutoffSeconds * 1000);
        cullTimer.setRepeats(true);
        cullTimer.start();

        if (maxLifespanSeconds > 0) {
            startLifespanEnforcer();
        }
        System.out.println("[EvoTournament] Started — cutoff every " + cutoffSeconds
                + "s, pop=" + aliveCount + ", grace=" + gracePeriodTicks + " ticks"
                + ", spawns=" + spawnsPerTick
                + ", adaptive=" + (adaptiveCutoff ? "ON [" + adaptiveCutoffMin + "-" + adaptiveCutoffMax + "s]" : "OFF")
                + ", ranking=" + rankingStrategy
                + (maxLifespanSeconds > 0 ? ", lifespan=" + maxLifespanSeconds + "s" : "")
                + ", maxPromoted=" + maxPromoted
                + (presetInjectionInterval > 0 ? ", presetInject=every " + presetInjectionInterval + " spawns" : "")
                + (rankingStrategy == RankingStrategy.AUTO ? ", autoTransition=" + autoTransitionGen + " gens" : ""));
        return true;
    }

    public void stop() {
        running = false;
        if (cullTimer != null) {
            cullTimer.stop();
            cullTimer = null;
        }
        if (lifespanTimer != null) {
            lifespanTimer.stop();
            lifespanTimer = null;
        }
        System.out.println("[EvoTournament] Stopped at generation " + generation);
    }

    /**
     * Starts a fast-polling timer (every 5s) that hard-enforces the lifespan cap.
     * Any active contestant past maxLifespanSeconds is immediately promoted,
     * regardless of the cull cycle, grace period, or minimum population constraints.
     */
    private void startLifespanEnforcer() {
        if (lifespanTimer != null) { lifespanTimer.stop(); }
        lifespanTimer = new Timer(5_000, e -> enforceLifespanCap());
        lifespanTimer.setRepeats(true);
        lifespanTimer.start();
    }

    private void enforceLifespanCap() {
        if (!running || maxLifespanSeconds <= 0) return;
        long now = System.currentTimeMillis();
        for (TournamentContestant c : new ArrayList<>(contestants)) {
            if (c.isFinished()) continue;
            long startMs = c.getStartTimeMs();
            if (startMs <= 0) continue;
            long ageSec = (now - startMs) / 1000;
            if (ageSec >= maxLifespanSeconds) {
                System.out.println("[EvoTournament] HARD CAP: promoting " + c.getName()
                        + " after " + ageSec + "s (limit " + maxLifespanSeconds + "s)");
                c.promote(generation);
                rotatePromotedPool();
            }
        }
    }

    public int getSecondsUntilNextTick() {
        if (!running || lastTickMs == 0) return -1;
        long elapsed = (System.currentTimeMillis() - lastTickMs) / 1000;
        return Math.max(0, cutoffSeconds - (int) elapsed);
    }

    /**
     * Ensures all existing contestants have LineageNode entries.
     * Called at start() and when new contestants are added manually.
     */
    public void ensureLineageNodes() {
        for (TournamentContestant c : contestants) {
            if (c.getLineageNode() == null) {
                LineageNode node = new LineageNode(c.getId(), c.getName(),
                        c.getGeneration(), c.getConfig());
                c.setLineageNode(node);
                lineageRegistry.put(c.getId(), node);
            }
        }
    }

    /** Active contestants: not eliminated and not promoted. */
    private List<TournamentContestant> getAlive() {
        List<TournamentContestant> alive = new ArrayList<>();
        for (TournamentContestant c : contestants) {
            if (!c.isFinished()) alive.add(c);
        }
        return alive;
    }

    /** Promoted contestants available for breeding (hall of fame). */
    private List<TournamentContestant> getPromoted() {
        List<TournamentContestant> promoted = new ArrayList<>();
        for (TournamentContestant c : contestants) {
            if (c.isPromoted()) promoted.add(c);
        }
        return promoted;
    }

    /** All potential parents: alive + promoted. */
    private List<TournamentContestant> getBreedingPool() {
        List<TournamentContestant> pool = new ArrayList<>();
        for (TournamentContestant c : contestants) {
            if (!c.isEliminated()) pool.add(c);
        }
        return pool;
    }

    // ═══════════════════════════════════════════════════════════
    //  CORE TICK — runs every cutoffSeconds
    // ═══════════════════════════════════════════════════════════

    private void onCutoffTick() {
        List<TournamentContestant> alive = getAlive();
        if (!running || alive.size() < minContestants) return;

        lastTickMs = System.currentTimeMillis();
        generation++;

        for (TournamentContestant c : alive) {
            c.decrementGraceTicks();
        }

        boolean anyReady = alive.stream().anyMatch(c -> c.getBestScore() > 0);
        if (!anyReady) {
            System.out.println("[EvoTournament] Gen " + generation
                    + ": Skipping — no contestants have scores yet");
            GenerationRecord rec = new GenerationRecord(generation);
            rec.skipped = true;
            rec.skipReason = "No contestants have fitness scores yet";
            rec.timestamp = System.currentTimeMillis();
            history.add(rec);
            return;
        }

        for (TournamentContestant c : alive) {
            if (c.getLineageNode() != null) {
                c.getLineageNode().setPeakFitness(c.getFitnessTracker().getPeakFitness());
                c.getLineageNode().setPeakVelocity(c.getFitnessTracker().getPeakVelocity());
            }
        }

        // === Composite ranking ===
        List<ScoredContestant> scored = computeCompositeScores(alive);
        scored.sort((a, b) -> Double.compare(b.compositeScore, a.compositeScore));

        ScoredContestant bestScored = scored.get(0);
        TournamentContestant best = bestScored.contestant;

        if (best.getBestScore() > bestEverScore) {
            bestEverScore = best.getBestScore();
            bestEverName = best.getName();
            bestEverConfig = best.getConfig().clone();
        }

        double improvement = best.getBestScore() - previousBestScore;
        if (improvement < CONVERGENCE_IMPROVEMENT_THRESHOLD) {
            stalledGenerations++;
        } else {
            stalledGenerations = 0;
        }
        previousBestScore = best.getBestScore();
        if (stalledGenerations >= CONVERGENCE_STALL_LIMIT) {
            converged = true;
        }

        // === Multi-spawn: cull N worst, breed N replacements ===
        int maxCulls = Math.min(spawnsPerTick, countEligibleForCull(scored));
        if (maxCulls == 0) {
            System.out.println("[EvoTournament] Gen " + generation
                    + ": Skipping — all contestants are under grace period");
            GenerationRecord rec = new GenerationRecord(generation);
            rec.skipped = true;
            rec.skipReason = "All contestants under grace period protection";
            rec.timestamp = System.currentTimeMillis();
            history.add(rec);
            return;
        }

        // Ensure we keep at least minContestants alive after culling
        maxCulls = Math.min(maxCulls, alive.size() - minContestants);
        if (maxCulls < 1) maxCulls = 1;

        StringBuilder cullLog = new StringBuilder();
        StringBuilder breedLog = new StringBuilder();
        String lastCulledName = null;
        double lastCulledScore = 0, lastCulledComposite = 0, lastCulledVelocity = 0;
        int lastCulledGen = 0;
        boolean lastCulledByLifespan = false;
        String lastParentA = null, lastParentB = null, lastChildName = null;
        String lastChildParams = null;

        for (int spawn = 0; spawn < maxCulls; spawn++) {
            List<TournamentContestant> currentAlive = getAlive();
            if (currentAlive.size() <= minContestants) break;

            List<ScoredContestant> currentScored = computeCompositeScores(currentAlive);
            currentScored.sort((a, b) -> Double.compare(b.compositeScore, a.compositeScore));

            ScoredContestant worstScored = findWorstEligible(currentScored);
            if (worstScored == null) break;

            TournamentContestant worst = worstScored.contestant;
            lastCulledName = worst.getName();
            lastCulledScore = worst.getBestScore();
            lastCulledComposite = worstScored.compositeScore;
            lastCulledVelocity = worst.getFitnessTracker().getVelocity();
            lastCulledGen = worst.getGeneration();
            lastCulledByLifespan = isExpired(worst);

            // Use full breeding pool (alive + promoted) for parent selection
            List<TournamentContestant> breedingPool = getBreedingPool();
            List<ScoredContestant> breedPoolScored = computeCompositeScores(breedingPool);
            breedPoolScored.sort((a, b) -> Double.compare(b.compositeScore, a.compositeScore));

            TournamentContestant parentA = selectParent(breedPoolScored, worst);
            TournamentContestant parentB = selectParentDiverse(breedPoolScored, worst, parentA);

            EvolutionConfig childConfig;
            String childName;
            spawnsSinceLastPreset++;

            // Preset injection: periodically introduce an untried preset strategy
            EvolutionConfig presetConfig = tryGetUntriedPreset();
            if (presetConfig != null && presetInjectionInterval > 0
                    && spawnsSinceLastPreset >= presetInjectionInterval) {
                childConfig = presetConfig;
                childConfig.threads = worst.getConfig().threads;
                childName = "G" + generation
                        + (maxCulls > 1 ? (char)('a' + spawn) : "")
                        + "-P-" + childConfig.name.substring(0, Math.min(6, childConfig.name.length()));
                spawnsSinceLastPreset = 0;
                lastParentA = "[preset]";
                lastParentB = childConfig.name;
            } else {
                // Normal breeding from the pool
                if (useAncestralCrossover && parentA.getLineageNode() != null
                        && parentB.getLineageNode() != null) {
                    childConfig = breedWithAncestry(parentA, parentB);
                } else {
                    childConfig = breedConfigs(parentA.getConfig(), parentB.getConfig());
                }
                childConfig.threads = worst.getConfig().threads;
                childName = "G" + generation
                        + (maxCulls > 1 ? (char)('a' + spawn) : "") + "-"
                        + parentA.getName().substring(0, Math.min(3, parentA.getName().length()))
                        + "x"
                        + parentB.getName().substring(0, Math.min(3, parentB.getName().length()));
                lastParentA = parentA.getName();
                lastParentB = parentB.getName();
            }

            // Lifespan-expired: promote (hall of fame). Poor performance: eliminate.
            if (lastCulledByLifespan) {
                worst.promote(generation);
                rotatePromotedPool();
            } else {
                worst.eliminate(generation);
            }

            spawnChild(childName, childConfig, parentA, parentB);

            lastChildName = childName;
            lastChildParams = childConfig.toSummary();

            if (spawn > 0) { cullLog.append(", "); breedLog.append(", "); }
            cullLog.append(worst.getName());
            breedLog.append(childName);
        }

        // Adaptive cutoff: if converged, shorten interval; if improving, lengthen
        if (adaptiveCutoff) {
            adaptCutoffInterval();
        }

        double[] rawScores = alive.stream().mapToDouble(TournamentContestant::getBestScore).toArray();
        double avgScore = Arrays.stream(rawScores).average().orElse(0);

        GenerationRecord rec = new GenerationRecord(generation);
        rec.timestamp = System.currentTimeMillis();
        rec.culledName = maxCulls > 1 ? cullLog.toString() : (lastCulledName != null ? lastCulledName : "");
        rec.culledScore = lastCulledScore;
        rec.culledComposite = lastCulledComposite;
        rec.culledVelocity = lastCulledVelocity;
        rec.culledGeneration = lastCulledGen;
        rec.parentA = lastParentA != null ? lastParentA : "";
        rec.parentB = lastParentB != null ? lastParentB : "";
        rec.childName = maxCulls > 1 ? breedLog.toString() : (lastChildName != null ? lastChildName : "");
        rec.childParams = lastChildParams != null ? lastChildParams : "";
        rec.childGraceTicks = gracePeriodTicks;
        rec.spawnsThisTick = maxCulls;
        rec.bestName = best.getName();
        rec.bestScore = best.getBestScore();
        rec.bestComposite = bestScored.compositeScore;
        rec.bestVelocity = best.getFitnessTracker().getVelocity();
        rec.worstScore = lastCulledScore;
        rec.avgScore = avgScore;
        rec.aliveCount = getAlive().size();
        rec.bestEverScore = bestEverScore;
        rec.bestEverName = bestEverName;
        rec.converged = converged;
        rec.stalledGens = stalledGenerations;
        rec.ancestralCrossover = useAncestralCrossover;
        rec.currentCutoff = cutoffSeconds;
        rec.culledByLifespan = lastCulledByLifespan;
        rec.presetInjected = (lastParentA != null && lastParentA.equals("[preset]"));
        rec.rankingMode = rankingStrategy.name();
        rec.effectiveWeights = getEffectiveWeights();
        rec.promotedCount = getPromoted().size();
        // Track previous generation's average for delta comparison
        if (!history.isEmpty()) {
            GenerationRecord prev = history.get(history.size() - 1);
            if (!prev.skipped) rec.prevAvgScore = prev.avgScore;
        }
        history.add(rec);

        artEvolver.refreshContestantCombo();
        if (artEvolver.getTournamentManagerWindow() != null) {
            artEvolver.getTournamentManagerWindow().refreshTable();
        }

        System.out.println("[EvoTournament] === Generation " + generation + " ==="
                + (maxCulls > 1 ? " (" + maxCulls + " spawns)" : ""));
        System.out.println("[EvoTournament] Composite ranking: " + formatCompositeRanking(scored));
        System.out.println("[EvoTournament] Culled: " + cullLog);
        System.out.println("[EvoTournament] Bred: " + breedLog
                + (useAncestralCrossover ? " [ancestral crossover]" : "")
                + " grace=" + gracePeriodTicks);
        System.out.println("[EvoTournament] Best: " + best.getName()
                + " (fit=" + DF4.format(best.getBestScore() * 100) + "%, vel="
                + DF4.format(best.getFitnessTracker().getVelocity() * 100) + "/s)  Avg: "
                + DF4.format(avgScore * 100) + "%");
        if (adaptiveCutoff) {
            System.out.println("[EvoTournament] Adaptive cutoff: " + cutoffSeconds + "s");
        }
        if (converged) {
            System.out.println("[EvoTournament] CONVERGENCE DETECTED — "
                    + stalledGenerations + " generations without significant improvement");
        }
    }

    /** Checks if a contestant has exceeded its maximum lifespan. */
    private boolean isExpired(TournamentContestant c) {
        if (maxLifespanSeconds <= 0) return false;
        long startMs = c.getStartTimeMs();
        if (startMs <= 0) return false;
        return (System.currentTimeMillis() - startMs) / 1000 >= maxLifespanSeconds;
    }

    /**
     * Finds the worst eligible contestant for culling.
     * Expired contestants (exceeded lifespan) are always prioritized, even if they rank well.
     * Among non-expired, picks the lowest composite score that isn't protected.
     */
    private ScoredContestant findWorstEligible(List<ScoredContestant> scored) {
        // First pass: look for expired contestants (oldest first)
        ScoredContestant oldestExpired = null;
        for (int i = scored.size() - 1; i >= 0; i--) {
            ScoredContestant sc = scored.get(i);
            if (isExpired(sc.contestant)) {
                if (oldestExpired == null || sc.contestant.getStartTimeMs() < oldestExpired.contestant.getStartTimeMs()) {
                    oldestExpired = sc;
                }
            }
        }
        if (oldestExpired != null) return oldestExpired;

        // Second pass: normal worst by composite score (not protected)
        ScoredContestant worst = null;
        for (int i = scored.size() - 1; i >= 0; i--) {
            ScoredContestant sc = scored.get(i);
            if (!sc.contestant.isProtected()) {
                if (worst == null || sc.compositeScore < worst.compositeScore
                        || (sc.compositeScore == worst.compositeScore
                            && sc.contestant.getGeneration() < worst.contestant.getGeneration())) {
                    worst = sc;
                }
            }
        }
        return worst;
    }

    private int countEligibleForCull(List<ScoredContestant> scored) {
        int count = 0;
        for (ScoredContestant sc : scored) {
            if (!sc.contestant.isProtected() || isExpired(sc.contestant)) count++;
        }
        return count;
    }

    /** Parent selection with inbreeding prevention. */
    private TournamentContestant selectParentDiverse(List<ScoredContestant> ranked,
                                                      TournamentContestant exclude,
                                                      TournamentContestant otherParent) {
        TournamentContestant candidate = selectParent(ranked, exclude);
        int attempts = 0;
        while (candidate == otherParent && ranked.size() > 2 && attempts < 10) {
            candidate = selectParent(ranked, exclude);
            attempts++;
        }
        if (otherParent.getLineageNode() != null && candidate.getLineageNode() != null) {
            int inbreedAttempts = 0;
            while (otherParent.getLineageNode().sharesAncestorWith(candidate.getLineageNode(), 2)
                    && inbreedAttempts < 5) {
                candidate = selectParent(ranked, exclude);
                inbreedAttempts++;
            }
        }
        return candidate;
    }

    /** Creates, initializes, and starts a new child contestant. */
    private TournamentContestant spawnChild(String childName, EvolutionConfig childConfig,
                                             TournamentContestant parentA,
                                             TournamentContestant parentB) {
        TournamentContestant child = new TournamentContestant("evo" + nextId++, childName);
        child.setConfig(childConfig);
        childConfig.name = childName;
        childConfig.chartColor = child.getChartColor();
        child.setGeneration(generation);
        child.setParentage(parentA.getName() + " x " + parentB.getName());
        child.setGraceTicks(gracePeriodTicks);
        child.getFitnessTracker().setVelocityWindowSeconds(velocityWindowSeconds);

        LineageNode childNode = new LineageNode(child.getId(), childName,
                generation, childConfig);
        childNode.setParentA(parentA.getLineageNode());
        childNode.setParentB(parentB.getLineageNode());
        if (parentA.getLineageNode() != null) parentA.getLineageNode().addChild(childNode);
        if (parentB.getLineageNode() != null) parentB.getLineageNode().addChild(childNode);
        child.setLineageNode(childNode);
        lineageRegistry.put(child.getId(), childNode);

        contestants.add(child);

        BufferedImage resized = artEvolver.getResizedOriginal();
        if (resized != null) {
            try {
                child.createEvolvers(artEvolver.getPallete(),
                        artEvolver.getTriangleWidth(), artEvolver.getTriangleHeight(),
                        artEvolver.getWidthTriangles(), artEvolver.getHeightTriangles(),
                        artEvolver.getTriangleScaleHeight(), artEvolver.getJumpDistances());
                child.initializeWithImage(resized);
                child.start();
            } catch (Exception ex) {
                System.err.println("[EvoTournament] Failed to start child "
                        + childName + ": " + ex.getMessage());
            }
        }
        return child;
    }

    /**
     * Adaptive cutoff: shortens interval when converged (to explore faster),
     * lengthens when improving well (to let contestants build more data).
     */
    /**
     * Adapts the cutoff interval based on tournament health.
     * Strategy: start fast (machine gun), slow down as contestants differentiate,
     * speed up again when stalled/converged to churn through bad configs faster.
     */
    private void adaptCutoffInterval() {
        int newCutoff = cutoffSeconds;

        if (stalledGenerations >= 5) {
            // Heavily stalled: drop fast to flush out the pool
            newCutoff = Math.max(adaptiveCutoffMin, cutoffSeconds * 2 / 3);
        } else if (stalledGenerations >= 2) {
            // Mildly stalled: gentle decrease
            newCutoff = Math.max(adaptiveCutoffMin, cutoffSeconds - 5);
        } else if (stalledGenerations == 0 && generation > 1) {
            // Improving: give contestants more time to differentiate
            // Ramp up faster in early generations, slower once established
            int increment = (cutoffSeconds < 30) ? 10 : 5;
            newCutoff = Math.min(adaptiveCutoffMax, cutoffSeconds + increment);
        }

        if (newCutoff != cutoffSeconds) {
            int old = cutoffSeconds;
            setCutoffSeconds(newCutoff);
            System.out.println("[EvoTournament] Adaptive cutoff: " + old + "s -> " + newCutoff + "s"
                    + (stalledGenerations > 0 ? " (stalled " + stalledGenerations + " gens)" : " (improving)"));
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  PROMOTED POOL & PRESET INJECTION
    // ═══════════════════════════════════════════════════════════

    /**
     * Keeps the promoted pool at maxPromoted size by removing the worst promoted.
     */
    private void rotatePromotedPool() {
        List<TournamentContestant> promoted = getPromoted();
        if (promoted.size() <= maxPromoted) return;

        promoted.sort((a, b) -> Double.compare(a.getFinalScore(), b.getFinalScore()));
        while (promoted.size() > maxPromoted) {
            TournamentContestant worst = promoted.remove(0);
            worst.eliminate(generation);
            System.out.println("[EvoTournament] Promoted pool rotation: " + worst.getName()
                    + " retired (score " + DF2.format(worst.getFinalScore() * 100) + "%)");
        }
    }

    /**
     * Checks all preset strategies against existing contestants.
     * Returns a fresh preset config for a strategy that hasn't been used yet,
     * or null if all presets have been tried.
     */
    private EvolutionConfig tryGetUntriedPreset() {
        if (artEvolver.getTournamentManagerWindow() == null) return null;

        java.util.Set<String> usedStrategies = new java.util.HashSet<>();
        for (TournamentContestant c : contestants) {
            if (c.getConfig() != null && c.getConfig().name != null) {
                usedStrategies.add(c.getConfig().name);
            }
        }

        TournamentManagerWindow tmw = artEvolver.getTournamentManagerWindow();
        for (int i = 0; i < tmw.getStrategyCount(); i++) {
            String stratName = tmw.getStrategyName(i);
            if (!usedStrategies.contains(stratName)) {
                EvolutionConfig base = EvolutionConfig.fromCurrentSettings();
                artEvolver.populateConfigFromUI(base);
                EvolutionConfig preset = tmw.applyStrategyPublic(base, i);
                System.out.println("[EvoTournament] Injecting untried preset: " + stratName);
                return preset;
            }
        }
        return null;
    }

    // ═══════════════════════════════════════════════════════════
    //  COMPOSITE SCORING
    // ═══════════════════════════════════════════════════════════

    /**
     * Computes effective weights based on the active ranking strategy.
     * AUTO interpolates from velocity-heavy to fitness-heavy over autoTransitionGen generations.
     */
    private float[] getEffectiveWeights() {
        float wFit = fitnessWeight, wVel = velocityWeight, wAcc = accelerationWeight, wLin = lineageWeight;

        switch (rankingStrategy) {
            case VELOCITY_FIRST:
                wFit = 0.10f; wVel = 0.60f; wAcc = 0.15f; wLin = 0.15f;
                break;
            case FITNESS_FIRST:
                wFit = 0.65f; wVel = 0.15f; wAcc = 0.05f; wLin = 0.15f;
                break;
            case AUTO:
                // Early: velocity-heavy. Late: fitness-heavy. Linear interpolation.
                float t = Math.min(1.0f, (float) generation / Math.max(1, autoTransitionGen));
                wFit  = 0.10f + t * 0.55f;  // 0.10 -> 0.65
                wVel  = 0.60f - t * 0.45f;  // 0.60 -> 0.15
                wAcc  = 0.15f - t * 0.10f;  // 0.15 -> 0.05
                wLin  = 0.15f;              // constant
                break;
            case BALANCED:
            default:
                break;
        }
        return new float[] { wFit, wVel, wAcc, wLin };
    }

    private List<ScoredContestant> computeCompositeScores(List<TournamentContestant> alive) {
        double[] fitnesses = new double[alive.size()];
        double[] velocities = new double[alive.size()];
        double[] accelerations = new double[alive.size()];
        double[] lineageScores = new double[alive.size()];

        for (int i = 0; i < alive.size(); i++) {
            TournamentContestant c = alive.get(i);
            fitnesses[i] = c.getBestScore();
            velocities[i] = c.getFitnessTracker().getVelocity();
            accelerations[i] = c.getFitnessTracker().getAcceleration();
            lineageScores[i] = (c.getLineageNode() != null)
                    ? c.getLineageNode().getLineageFitness(lineageDecay, ancestryDepth)
                    : fitnesses[i];
        }

        double[] normFit = normalize(fitnesses);
        double[] normVel = normalize(velocities);
        double[] normAcc = normalize(accelerations);
        double[] normLin = normalize(lineageScores);

        float[] w = getEffectiveWeights();

        List<ScoredContestant> result = new ArrayList<>();
        for (int i = 0; i < alive.size(); i++) {
            double composite = w[0] * normFit[i]
                    + w[1] * normVel[i]
                    + w[2] * normAcc[i]
                    + w[3] * normLin[i];
            result.add(new ScoredContestant(alive.get(i), composite,
                    normFit[i], normVel[i], normAcc[i], normLin[i]));
        }
        return result;
    }

    private double[] normalize(double[] values) {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (double v : values) {
            if (v < min) min = v;
            if (v > max) max = v;
        }
        double range = max - min;
        double[] result = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = (range > 0) ? (values[i] - min) / range : 0.5;
        }
        return result;
    }

    // ═══════════════════════════════════════════════════════════
    //  PARENT SELECTION (composite-score aware)
    // ═══════════════════════════════════════════════════════════

    private TournamentContestant selectParent(List<ScoredContestant> ranked,
                                              TournamentContestant exclude) {
        int topHalf = Math.max(2, ranked.size() / 2);
        for (int i = 0; i < 20; i++) {
            ScoredContestant a = ranked.get(RNG.nextInt(topHalf));
            ScoredContestant b = ranked.get(RNG.nextInt(topHalf));
            ScoredContestant pick = (a.compositeScore >= b.compositeScore) ? a : b;
            if (pick.contestant != exclude) return pick.contestant;
        }
        for (ScoredContestant sc : ranked) {
            if (sc.contestant != exclude) return sc.contestant;
        }
        return ranked.get(0).contestant;
    }

    // ═══════════════════════════════════════════════════════════
    //  BREEDING — standard and multi-generational
    // ═══════════════════════════════════════════════════════════

    EvolutionConfig breedConfigs(EvolutionConfig cfgA, EvolutionConfig cfgB) {
        float[] genesA = cfgA.toGeneArray();
        float[] genesB = cfgB.toGeneArray();
        float[] childGenes = crossover(genesA, genesB);
        childGenes = mutate(childGenes);
        return EvolutionConfig.fromGeneArray(childGenes, cfgA);
    }

    /**
     * Multi-generational breeding: collects genes from ancestors of both parents
     * with decaying weights, then performs weighted-average crossover + mutation.
     */
    private EvolutionConfig breedWithAncestry(TournamentContestant parentA,
                                               TournamentContestant parentB) {
        List<LineageNode.WeightedGenes> genesA =
                parentA.getLineageNode().getAncestralGenes(lineageDecay, ancestryDepth);
        List<LineageNode.WeightedGenes> genesB =
                parentB.getLineageNode().getAncestralGenes(lineageDecay, ancestryDepth);

        float[] blended = blendAncestralGenes(genesA, genesB);
        blended = mutate(blended);
        return EvolutionConfig.fromGeneArray(blended, parentA.getConfig());
    }

    /**
     * Blends multiple ancestor gene arrays from both parent lineages using
     * weighted averaging. Closer ancestors (higher weight) dominate, but
     * grandparents/great-grandparents still contribute.
     */
    private float[] blendAncestralGenes(List<LineageNode.WeightedGenes> lineA,
                                        List<LineageNode.WeightedGenes> lineB) {
        float[] result = new float[EvolutionConfig.GENE_COUNT];

        for (int g = 0; g < EvolutionConfig.GENE_COUNT; g++) {
            double totalWeight = 0;
            double weightedSum = 0;

            for (LineageNode.WeightedGenes wg : lineA) {
                if (wg.genes.length > g) {
                    weightedSum += wg.genes[g] * wg.weight;
                    totalWeight += wg.weight;
                }
            }
            for (LineageNode.WeightedGenes wg : lineB) {
                if (wg.genes.length > g) {
                    weightedSum += wg.genes[g] * wg.weight;
                    totalWeight += wg.weight;
                }
            }

            float base = (totalWeight > 0) ? (float) (weightedSum / totalWeight) : 0;

            // Add BLX-alpha exploration around the ancestral centroid
            float[] parentA = lineA.isEmpty() ? new float[EvolutionConfig.GENE_COUNT] : lineA.get(0).genes;
            float[] parentB = lineB.isEmpty() ? new float[EvolutionConfig.GENE_COUNT] : lineB.get(0).genes;
            float lo = Math.min(parentA.length > g ? parentA[g] : base, parentB.length > g ? parentB[g] : base);
            float hi = Math.max(parentA.length > g ? parentA[g] : base, parentB.length > g ? parentB[g] : base);
            float d = hi - lo;
            float cLo = base - BLX_ALPHA * d;
            float cHi = base + BLX_ALPHA * d;
            result[g] = cLo + RNG.nextFloat() * (cHi - cLo);
        }

        return result;
    }

    private float[] crossover(float[] a, float[] b) {
        float[] child = new float[EvolutionConfig.GENE_COUNT];
        for (int i = 0; i < child.length; i++) {
            float lo = Math.min(a[i], b[i]);
            float hi = Math.max(a[i], b[i]);
            float d = hi - lo;
            float cLo = lo - BLX_ALPHA * d;
            float cHi = hi + BLX_ALPHA * d;
            child[i] = cLo + RNG.nextFloat() * (cHi - cLo);
        }
        return child;
    }

    private float[] mutate(float[] genes) {
        float[] min = EvolutionConfig.getGeneMin();
        float[] max = EvolutionConfig.getGeneMax();
        for (int i = 0; i < genes.length; i++) {
            if (RNG.nextFloat() < mutationRate) {
                float range = max[i] - min[i];
                float delta = (float) (RNG.nextGaussian() * mutationStrength * range);
                genes[i] = Math.max(min[i], Math.min(max[i], genes[i] + delta));
            }
        }
        return genes;
    }

    // ═══════════════════════════════════════════════════════════
    //  FORMATTING
    // ═══════════════════════════════════════════════════════════

    private String formatCompositeRanking(List<ScoredContestant> ranked) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ranked.size(); i++) {
            ScoredContestant sc = ranked.get(i);
            if (i > 0) sb.append(", ");
            sb.append(i + 1).append(". ").append(sc.contestant.getName())
              .append(" [").append(DF2.format(sc.compositeScore))
              .append(sc.contestant.isProtected() ? " \u2B50" : "")
              .append("]");
        }
        return sb.toString();
    }

    // ═══════════════════════════════════════════════════════════
    //  ACCESSORS
    // ═══════════════════════════════════════════════════════════

    public boolean isRunning() { return running; }
    public int getGeneration() { return generation; }
    public boolean isConverged() { return converged; }
    public int getStalledGenerations() { return stalledGenerations; }
    public double getBestEverScore() { return bestEverScore; }
    public String getBestEverName() { return bestEverName; }
    public EvolutionConfig getBestEverConfig() { return bestEverConfig; }
    public List<GenerationRecord> getHistory() { return Collections.unmodifiableList(history); }
    public Map<String, LineageNode> getLineageRegistry() { return Collections.unmodifiableMap(lineageRegistry); }

    public int getCutoffSeconds() { return cutoffSeconds; }
    public void setCutoffSeconds(int s) {
        this.cutoffSeconds = Math.max(5, s);
        if (cullTimer != null && running) {
            cullTimer.stop();
            cullTimer = new Timer(cutoffSeconds * 1000, e -> onCutoffTick());
            cullTimer.setRepeats(true);
            lastTickMs = System.currentTimeMillis();
            cullTimer.start();
        }
    }
    public float getMutationRate() { return mutationRate; }
    public void setMutationRate(float r) { this.mutationRate = Math.max(0f, Math.min(1f, r)); }
    public float getMutationStrength() { return mutationStrength; }
    public void setMutationStrength(float s) { this.mutationStrength = Math.max(0f, Math.min(1f, s)); }
    public int getMinContestants() { return minContestants; }
    public void setMinContestants(int n) { this.minContestants = Math.max(2, n); }
    public int getGracePeriodTicks() { return gracePeriodTicks; }
    public void setGracePeriodTicks(int t) { this.gracePeriodTicks = Math.max(0, t); }
    public float getFitnessWeight() { return fitnessWeight; }
    public void setFitnessWeight(float w) { this.fitnessWeight = w; }
    public float getVelocityWeight() { return velocityWeight; }
    public void setVelocityWeight(float w) { this.velocityWeight = w; }
    public float getAccelerationWeight() { return accelerationWeight; }
    public void setAccelerationWeight(float w) { this.accelerationWeight = w; }
    public float getLineageWeight() { return lineageWeight; }
    public void setLineageWeight(float w) { this.lineageWeight = w; }
    public double getLineageDecay() { return lineageDecay; }
    public void setLineageDecay(double d) { this.lineageDecay = Math.max(0, Math.min(1, d)); }
    public int getAncestryDepth() { return ancestryDepth; }
    public void setAncestryDepth(int d) { this.ancestryDepth = Math.max(1, d); }
    public boolean isUseAncestralCrossover() { return useAncestralCrossover; }
    public void setUseAncestralCrossover(boolean b) { this.useAncestralCrossover = b; }
    public int getVelocityWindowSeconds() { return velocityWindowSeconds; }
    public void setVelocityWindowSeconds(int s) { this.velocityWindowSeconds = Math.max(5, s); }
    public int getSpawnsPerTick() { return spawnsPerTick; }
    public void setSpawnsPerTick(int n) { this.spawnsPerTick = Math.max(1, n); }
    public boolean isAdaptiveCutoff() { return adaptiveCutoff; }
    public void setAdaptiveCutoff(boolean b) { this.adaptiveCutoff = b; }
    public int getAdaptiveCutoffMin() { return adaptiveCutoffMin; }
    public void setAdaptiveCutoffMin(int s) { this.adaptiveCutoffMin = Math.max(5, s); }
    public int getAdaptiveCutoffMax() { return adaptiveCutoffMax; }
    public void setAdaptiveCutoffMax(int s) { this.adaptiveCutoffMax = Math.max(30, s); }
    public int getMaxLifespanSeconds() { return maxLifespanSeconds; }
    public void setMaxLifespanSeconds(int s) {
        this.maxLifespanSeconds = Math.max(0, s);
        if (running && s > 0 && lifespanTimer == null) {
            startLifespanEnforcer();
        } else if (s <= 0 && lifespanTimer != null) {
            lifespanTimer.stop();
            lifespanTimer = null;
        }
    }
    public int getMaxPromoted() { return maxPromoted; }
    public void setMaxPromoted(int n) { this.maxPromoted = Math.max(1, n); }
    public int getPresetInjectionInterval() { return presetInjectionInterval; }
    public void setPresetInjectionInterval(int n) { this.presetInjectionInterval = Math.max(0, n); }
    public RankingStrategy getRankingStrategy() { return rankingStrategy; }
    public void setRankingStrategy(RankingStrategy s) { this.rankingStrategy = s; }
    public int getAutoTransitionGen() { return autoTransitionGen; }
    public void setAutoTransitionGen(int g) { this.autoTransitionGen = Math.max(1, g); }
    public float[] getCurrentEffectiveWeights() { return getEffectiveWeights(); }

    // ═══════════════════════════════════════════════════════════
    //  INNER CLASSES
    // ═══════════════════════════════════════════════════════════

    /** Contestant + its computed composite score breakdown. */
    private static class ScoredContestant {
        final TournamentContestant contestant;
        final double compositeScore;
        final double normFitness;
        final double normVelocity;
        final double normAcceleration;
        final double normLineage;

        ScoredContestant(TournamentContestant contestant, double compositeScore,
                         double normFitness, double normVelocity,
                         double normAcceleration, double normLineage) {
            this.contestant = contestant;
            this.compositeScore = compositeScore;
            this.normFitness = normFitness;
            this.normVelocity = normVelocity;
            this.normAcceleration = normAcceleration;
            this.normLineage = normLineage;
        }
    }

    /**
     * Record of one evolutionary generation — enriched with velocity/composite data.
     */
    public static class GenerationRecord {
        public final int generation;
        public long timestamp;
        public boolean skipped;
        public String skipReason;

        public String culledName;
        public double culledScore;
        public double culledComposite;
        public double culledVelocity;
        public int culledGeneration;
        public String parentA;
        public String parentB;
        public String childName;
        public String childParams;
        public int childGraceTicks;

        public String bestName;
        public double bestScore;
        public double bestComposite;
        public double bestVelocity;
        public double worstScore;
        public double avgScore;
        public double prevAvgScore;
        public int aliveCount;
        public double bestEverScore;
        public String bestEverName;
        public boolean converged;
        public int stalledGens;
        public boolean ancestralCrossover;
        public int spawnsThisTick = 1;
        public int currentCutoff;
        public boolean culledByLifespan;
        public boolean presetInjected;
        public String rankingMode;
        public float[] effectiveWeights;
        public int promotedCount;

        GenerationRecord(int gen) {
            this.generation = gen;
        }

        @Override
        public String toString() {
            if (skipped) {
                return "Gen " + generation + ": SKIPPED — " + skipReason;
            }
            String spawnStr = spawnsThisTick > 1 ? " [" + spawnsThisTick + " spawns]" : "";
            String cutoffStr = currentCutoff > 0 ? " cutoff=" + currentCutoff + "s" : "";
            return "Gen " + generation + spawnStr + ": "
                    + culledName + " (fit=" + DF2_STATIC.format(culledScore * 100)
                    + "% vel=" + DF4_STATIC.format(culledVelocity * 100)
                    + "/s comp=" + DF2_STATIC.format(culledComposite) + ") CULLED"
                    + " \u2192 " + childName + " bred [" + parentA + " \u00D7 " + parentB + "]"
                    + (ancestralCrossover ? " [ancestry]" : "")
                    + " grace=" + childGraceTicks
                    + "  |  Best: " + bestName + " " + DF2_STATIC.format(bestScore * 100) + "%"
                    + " vel=" + DF4_STATIC.format(bestVelocity * 100) + "/s"
                    + "  Avg: " + DF2_STATIC.format(avgScore * 100) + "%"
                    + "  Alive: " + aliveCount + cutoffStr;
        }

        /**
         * Returns a multi-line, human-readable narrative of this generation's events.
         * Acts as a "smart announcer" with commentary on trends and promise.
         */
        public String toNarrative() {
            StringBuilder sb = new StringBuilder();
            sb.append("--- Generation ").append(generation).append(" ---\n");

            if (skipped) {
                sb.append("  Tournament paused: ").append(skipReason).append("\n");
                return sb.toString();
            }

            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss");
            sb.append("  ").append(sdf.format(new java.util.Date(timestamp)));
            sb.append("  |  ").append(aliveCount).append(" active");
            if (spawnsThisTick > 1) sb.append("  |  ").append(spawnsThisTick).append(" spawns");
            sb.append("\n");

            // Score summary with delta from previous gen
            sb.append("  Avg: ").append(DF2_STATIC.format(avgScore * 100)).append("%");
            if (prevAvgScore > 0) {
                double delta = (avgScore - prevAvgScore) * 100;
                if (delta > 0.001) sb.append(" (+").append(DF4_STATIC.format(delta)).append(")");
                else if (delta < -0.001) sb.append(" (").append(DF4_STATIC.format(delta)).append(")");
                else sb.append(" (flat)");
            }
            sb.append("  |  Best: ").append(DF2_STATIC.format(bestScore * 100)).append("%");
            sb.append("  |  Spread: ").append(DF2_STATIC.format((bestScore - worstScore) * 100)).append("pp\n");

            // Leader with commentary
            sb.append("  \uD83C\uDFC6 ").append(bestName);
            if (bestVelocity > 0.001) {
                sb.append(" accelerating at +").append(DF4_STATIC.format(bestVelocity * 100)).append("%/s");
            } else if (bestVelocity > 0) {
                sb.append(" gaining steadily");
            } else {
                sb.append(" at plateau");
            }
            sb.append('\n');

            // Convergence/stall warning
            if (converged) {
                sb.append("  \u26A0 CONVERGED — population stalled for ").append(stalledGens).append(" generations\n");
            } else if (stalledGens >= 2) {
                sb.append("  \u26A0 Stalling (").append(stalledGens).append(" gens without improvement)\n");
            }

            // Culling with story
            if (culledByLifespan) {
                sb.append("  \uD83C\uDFC5 Promoted ").append(culledName)
                  .append(" (").append(DF2_STATIC.format(culledScore * 100)).append("%, time served)");
                if (promotedCount > 0) sb.append("  [").append(promotedCount).append(" in hall of fame]");
                sb.append("\n");
            } else {
                sb.append("  \u2694 ");
                if (spawnsThisTick > 1) {
                    sb.append(spawnsThisTick).append(" eliminated: ").append(culledName);
                } else {
                    sb.append("Eliminated ").append(culledName);
                }
                sb.append(" (").append(DF2_STATIC.format(culledScore * 100)).append("%");
                if (culledVelocity > 0) sb.append(", was still improving");
                else if (culledVelocity < -0.0001) sb.append(", declining");
                else sb.append(", stagnant");
                if (culledGeneration > 0) sb.append(", born Gen ").append(culledGeneration);
                sb.append(")\n");
            }

            // Breeding / replacement
            if (presetInjected) {
                sb.append("  \uD83C\uDFB2 Injected preset: ").append(childName)
                  .append(" [untried strategy]\n");
            } else {
                sb.append("  \u2728 ").append(childName);
                sb.append(" = ").append(parentA).append(" x ").append(parentB);
                if (ancestralCrossover) sb.append(" +ancestry");
                sb.append(" (").append(childGraceTicks).append(" grace)\n");
            }

            // Best ever tracker
            if (bestEverScore > bestScore * 1.001) {
                sb.append("  \u2B50 Record: ").append(bestEverName)
                  .append(" ").append(DF2_STATIC.format(bestEverScore * 100)).append("%\n");
            }

            // Ranking mode
            if (rankingMode != null) {
                sb.append("  \uD83C\uDFAF Ranking: ").append(rankingMode);
                if (effectiveWeights != null) {
                    sb.append(" [fit=").append(DF2_STATIC.format(effectiveWeights[0]))
                      .append(" vel=").append(DF2_STATIC.format(effectiveWeights[1]))
                      .append(" acc=").append(DF2_STATIC.format(effectiveWeights[2]))
                      .append(" lin=").append(DF2_STATIC.format(effectiveWeights[3]))
                      .append("]");
                }
                sb.append('\n');
            }

            // Cycle timing
            if (currentCutoff > 0) {
                sb.append("  \u23F1 Next in ").append(currentCutoff).append("s");
                if (currentCutoff <= 10) sb.append(" (machine gun)");
                else if (currentCutoff < 30) sb.append(" (fast)");
                else if (currentCutoff > 120) sb.append(" (extended)");
                sb.append('\n');
            }

            return sb.toString();
        }

        private static final DecimalFormat DF2_STATIC = new DecimalFormat("0.00");
        private static final DecimalFormat DF4_STATIC = new DecimalFormat("0.0000");
    }
}
