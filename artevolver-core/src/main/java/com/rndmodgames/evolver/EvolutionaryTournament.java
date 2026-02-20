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

    private int generation = 0;
    private boolean running = false;
    private int nextId = 100;

    // --- Core timing ---
    private int cutoffSeconds = 60;
    private long lastTickMs = 0;

    // --- Breeding ---
    private float mutationRate = 0.3f;
    private float mutationStrength = 0.2f;
    private int minContestants = 3;

    // --- Grace period ---
    private int gracePeriodTicks = 1;

    // --- Composite ranking weights (must sum to ~1.0) ---
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
        System.out.println("[EvoTournament] Started — cutoff every " + cutoffSeconds
                + "s, pop=" + aliveCount + ", grace=" + gracePeriodTicks + " ticks"
                + ", weights: fit=" + fitnessWeight + " vel=" + velocityWeight
                + " acc=" + accelerationWeight + " lin=" + lineageWeight);
        return true;
    }

    public void stop() {
        running = false;
        if (cullTimer != null) {
            cullTimer.stop();
            cullTimer = null;
        }
        System.out.println("[EvoTournament] Stopped at generation " + generation);
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

    private List<TournamentContestant> getAlive() {
        List<TournamentContestant> alive = new ArrayList<>();
        for (TournamentContestant c : contestants) {
            if (!c.isEliminated()) alive.add(c);
        }
        return alive;
    }

    // ═══════════════════════════════════════════════════════════
    //  CORE TICK — runs every cutoffSeconds
    // ═══════════════════════════════════════════════════════════

    private void onCutoffTick() {
        List<TournamentContestant> alive = getAlive();
        if (!running || alive.size() < minContestants) return;

        lastTickMs = System.currentTimeMillis();
        generation++;

        // Decrement grace ticks for all alive contestants
        for (TournamentContestant c : alive) {
            c.decrementGraceTicks();
        }

        // Check if any contestant has meaningful data
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

        // Update lineage peak metrics from fitness trackers
        for (TournamentContestant c : alive) {
            if (c.getLineageNode() != null) {
                c.getLineageNode().setPeakFitness(c.getFitnessTracker().getPeakFitness());
                c.getLineageNode().setPeakVelocity(c.getFitnessTracker().getPeakVelocity());
            }
        }

        // === Composite ranking ===
        List<ScoredContestant> scored = computeCompositeScores(alive);
        scored.sort((a, b) -> Double.compare(b.compositeScore, a.compositeScore));

        // Find best (by composite) and worst (eligible for culling)
        ScoredContestant bestScored = scored.get(0);
        TournamentContestant best = bestScored.contestant;

        // Find worst among non-protected contestants
        ScoredContestant worstScored = null;
        for (int i = scored.size() - 1; i >= 0; i--) {
            ScoredContestant sc = scored.get(i);
            if (!sc.contestant.isProtected()) {
                worstScored = sc;
                break;
            }
        }

        if (worstScored == null) {
            System.out.println("[EvoTournament] Gen " + generation
                    + ": Skipping — all contestants are under grace period");
            GenerationRecord rec = new GenerationRecord(generation);
            rec.skipped = true;
            rec.skipReason = "All contestants under grace period protection";
            rec.timestamp = System.currentTimeMillis();
            history.add(rec);
            return;
        }

        TournamentContestant worst = worstScored.contestant;

        // Tie-breaking: if multiple share the worst composite score, prefer oldest
        double worstComposite = worstScored.compositeScore;
        for (int i = scored.size() - 2; i >= 0; i--) {
            ScoredContestant sc = scored.get(i);
            if (sc.compositeScore > worstComposite) break;
            if (!sc.contestant.isProtected() && sc.contestant.getGeneration() < worst.getGeneration()) {
                worst = sc.contestant;
                worstScored = sc;
            }
        }

        // Best-ever tracking (by raw fitness, not composite)
        if (best.getBestScore() > bestEverScore) {
            bestEverScore = best.getBestScore();
            bestEverName = best.getName();
            bestEverConfig = best.getConfig().clone();
        }

        // Convergence detection
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

        // === Cull the worst ===
        String culledName = worst.getName();
        double culledScore = worst.getBestScore();
        double culledComposite = worstScored.compositeScore;
        double culledVelocity = worst.getFitnessTracker().getVelocity();
        int culledGen = worst.getGeneration();

        // === Breed replacement ===
        TournamentContestant parentA = selectParent(scored, worst);
        TournamentContestant parentB = selectParent(scored, worst);

        // Inbreeding prevention
        int attempts = 0;
        while (parentB == parentA && alive.size() > 2 && attempts < 10) {
            parentB = selectParent(scored, worst);
            attempts++;
        }
        if (parentA.getLineageNode() != null && parentB.getLineageNode() != null) {
            int inbreedAttempts = 0;
            while (parentA.getLineageNode().sharesAncestorWith(parentB.getLineageNode(), 2)
                    && inbreedAttempts < 5) {
                parentB = selectParent(scored, worst);
                inbreedAttempts++;
            }
        }

        EvolutionConfig childConfig;
        if (useAncestralCrossover && parentA.getLineageNode() != null
                && parentB.getLineageNode() != null) {
            childConfig = breedWithAncestry(parentA, parentB);
        } else {
            childConfig = breedConfigs(parentA.getConfig(), parentB.getConfig());
        }
        childConfig.threads = worst.getConfig().threads;

        String childName = "G" + generation + "-"
                + parentA.getName().substring(0, Math.min(3, parentA.getName().length()))
                + "x"
                + parentB.getName().substring(0, Math.min(3, parentB.getName().length()));

        worst.eliminate(generation);

        TournamentContestant child = new TournamentContestant("evo" + nextId++, childName);
        child.setConfig(childConfig);
        childConfig.name = childName;
        childConfig.chartColor = child.getChartColor();
        child.setGeneration(generation);
        child.setParentage(parentA.getName() + " x " + parentB.getName());
        child.setGraceTicks(gracePeriodTicks);
        child.getFitnessTracker().setVelocityWindowSeconds(velocityWindowSeconds);

        // Build lineage node for child
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
                System.err.println("[EvoTournament] Failed to start child " + childName + ": " + ex.getMessage());
            }
        }

        // Build generation record
        double[] rawScores = alive.stream().mapToDouble(TournamentContestant::getBestScore).toArray();
        double avgScore = Arrays.stream(rawScores).average().orElse(0);

        GenerationRecord rec = new GenerationRecord(generation);
        rec.timestamp = System.currentTimeMillis();
        rec.culledName = culledName;
        rec.culledScore = culledScore;
        rec.culledComposite = culledComposite;
        rec.culledVelocity = culledVelocity;
        rec.culledGeneration = culledGen;
        rec.parentA = parentA.getName();
        rec.parentB = parentB.getName();
        rec.childName = childName;
        rec.childParams = childConfig.toSummary();
        rec.childGraceTicks = gracePeriodTicks;
        rec.bestName = best.getName();
        rec.bestScore = best.getBestScore();
        rec.bestComposite = bestScored.compositeScore;
        rec.bestVelocity = best.getFitnessTracker().getVelocity();
        rec.worstScore = culledScore;
        rec.avgScore = avgScore;
        rec.aliveCount = getAlive().size();
        rec.bestEverScore = bestEverScore;
        rec.bestEverName = bestEverName;
        rec.converged = converged;
        rec.ancestralCrossover = useAncestralCrossover;
        history.add(rec);

        artEvolver.refreshContestantCombo();
        if (artEvolver.getTournamentManagerWindow() != null) {
            artEvolver.getTournamentManagerWindow().refreshTable();
        }

        System.out.println("[EvoTournament] === Generation " + generation + " ===");
        System.out.println("[EvoTournament] Composite ranking: " + formatCompositeRanking(scored));
        System.out.println("[EvoTournament] Culled: " + culledName
                + " (fit=" + DF4.format(culledScore * 100) + "%, vel=" + DF4.format(culledVelocity * 100)
                + "/s, comp=" + DF4.format(culledComposite) + ", gen " + culledGen + ")");
        System.out.println("[EvoTournament] Bred: " + childName
                + " from [" + parentA.getName() + " x " + parentB.getName() + "]"
                + (useAncestralCrossover ? " [ancestral crossover]" : "")
                + " grace=" + gracePeriodTicks);
        System.out.println("[EvoTournament] Best: " + best.getName()
                + " (fit=" + DF4.format(best.getBestScore() * 100) + "%, vel="
                + DF4.format(best.getFitnessTracker().getVelocity() * 100) + "/s)  Avg: "
                + DF4.format(avgScore * 100) + "%");
        if (converged) {
            System.out.println("[EvoTournament] CONVERGENCE DETECTED — "
                    + stalledGenerations + " generations without significant improvement");
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  COMPOSITE SCORING
    // ═══════════════════════════════════════════════════════════

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

        List<ScoredContestant> result = new ArrayList<>();
        for (int i = 0; i < alive.size(); i++) {
            double composite = fitnessWeight * normFit[i]
                    + velocityWeight * normVel[i]
                    + accelerationWeight * normAcc[i]
                    + lineageWeight * normLin[i];
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

    private EvolutionConfig breedConfigs(EvolutionConfig cfgA, EvolutionConfig cfgB) {
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
        this.cutoffSeconds = Math.max(10, s);
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
        public int aliveCount;
        public double bestEverScore;
        public String bestEverName;
        public boolean converged;
        public boolean ancestralCrossover;

        GenerationRecord(int gen) {
            this.generation = gen;
        }

        @Override
        public String toString() {
            if (skipped) {
                return "Gen " + generation + ": SKIPPED — " + skipReason;
            }
            return "Gen " + generation + ": "
                    + culledName + " (fit=" + DF2_STATIC.format(culledScore * 100)
                    + "% vel=" + DF4_STATIC.format(culledVelocity * 100)
                    + "/s comp=" + DF2_STATIC.format(culledComposite) + ") CULLED"
                    + " → " + childName + " bred [" + parentA + " × " + parentB + "]"
                    + (ancestralCrossover ? " [ancestry]" : "")
                    + " grace=" + childGraceTicks
                    + "  |  Best: " + bestName + " " + DF2_STATIC.format(bestScore * 100) + "%"
                    + " vel=" + DF4_STATIC.format(bestVelocity * 100) + "/s"
                    + "  Avg: " + DF2_STATIC.format(avgScore * 100) + "%"
                    + "  Alive: " + aliveCount;
        }

        private static final DecimalFormat DF2_STATIC = new DecimalFormat("0.00");
        private static final DecimalFormat DF4_STATIC = new DecimalFormat("0.0000");
    }
}
