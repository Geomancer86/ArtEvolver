package com.rndmodgames.evolver;

import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.*;
import javax.swing.Timer;

/**
 * Meta-genetic algorithm that evolves the GA parameters themselves.
 *
 * At regular intervals (cutoff), contestants are ranked by fitness,
 * the worst is culled, and a bred offspring replaces it. Over time
 * the system converges on optimal GA parameters for the given image.
 */
public class EvolutionaryTournament {

    private final ArtEvolver artEvolver;
    private final List<TournamentContestant> contestants;
    private Timer cullTimer;

    private int generation = 0;
    private int cutoffSeconds = 60;
    private float mutationRate = 0.3f;
    private float mutationStrength = 0.2f;
    private int minContestants = 3;
    private boolean running = false;
    private int nextId = 100;

    private long lastTickMs = 0;
    private double bestEverScore = 0;
    private String bestEverName = "";
    private EvolutionConfig bestEverConfig = null;
    private int stalledGenerations = 0;
    private static final int CONVERGENCE_STALL_LIMIT = 5;
    private static final double CONVERGENCE_IMPROVEMENT_THRESHOLD = 0.0001;
    private double previousBestScore = 0;
    private boolean converged = false;

    private static final float BLX_ALPHA = 0.5f;
    private static final Random RNG = new Random();
    private static final DecimalFormat DF4 = new DecimalFormat("0.0000");
    private static final DecimalFormat DF2 = new DecimalFormat("0.00");

    private final List<GenerationRecord> history = new ArrayList<>();

    public EvolutionaryTournament(ArtEvolver artEvolver, List<TournamentContestant> contestants) {
        this.artEvolver = artEvolver;
        this.contestants = contestants;
    }

    public void start() {
        if (running) return;
        if (contestants.size() < minContestants) return;
        running = true;
        converged = false;
        stalledGenerations = 0;
        previousBestScore = 0;
        lastTickMs = System.currentTimeMillis();

        cullTimer = new Timer(cutoffSeconds * 1000, e -> onCutoffTick());
        cullTimer.setRepeats(true);
        cullTimer.start();
        System.out.println("[EvoTournament] Started — cutoff every " + cutoffSeconds
                + "s, pop=" + contestants.size());
    }

    public void stop() {
        running = false;
        if (cullTimer != null) {
            cullTimer.stop();
            cullTimer = null;
        }
        System.out.println("[EvoTournament] Stopped at generation " + generation);
    }

    /**
     * Returns seconds remaining until next cull tick, or -1 if not running.
     */
    public int getSecondsUntilNextTick() {
        if (!running || lastTickMs == 0) return -1;
        long elapsed = (System.currentTimeMillis() - lastTickMs) / 1000;
        return Math.max(0, cutoffSeconds - (int) elapsed);
    }

    private void onCutoffTick() {
        if (!running || contestants.size() < minContestants) return;

        lastTickMs = System.currentTimeMillis();
        generation++;

        List<TournamentContestant> ranked = new ArrayList<>(contestants);
        ranked.sort((a, b) -> Double.compare(b.getBestScore(), a.getBestScore()));

        boolean anyReady = ranked.stream().anyMatch(c -> c.getBestScore() > 0);
        if (!anyReady) {
            System.out.println("[EvoTournament] Gen " + generation
                    + ": Skipping cull — no contestants have scores yet");
            GenerationRecord rec = new GenerationRecord(generation);
            rec.skipped = true;
            rec.skipReason = "No contestants have fitness scores yet";
            rec.timestamp = System.currentTimeMillis();
            history.add(rec);
            return;
        }

        TournamentContestant best = ranked.get(0);
        TournamentContestant worst = ranked.get(ranked.size() - 1);

        if (best.getBestScore() > 0 && best.getBestScore() == worst.getBestScore()) {
            worst = ranked.get(ranked.size() - 1);
            for (int i = ranked.size() - 1; i > 0; i--) {
                if (ranked.get(i).getGeneration() <= worst.getGeneration()) {
                    worst = ranked.get(i);
                }
            }
        }

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

        String culledName = worst.getName();
        double culledScore = worst.getBestScore();
        int culledGen = worst.getGeneration();

        TournamentContestant parentA = selectParent(ranked);
        TournamentContestant parentB = selectParent(ranked);
        int attempts = 0;
        while (parentB == parentA && ranked.size() > 2 && attempts < 10) {
            parentB = selectParent(ranked);
            attempts++;
        }

        EvolutionConfig childConfig = breedConfigs(parentA.getConfig(), parentB.getConfig());
        childConfig.threads = worst.getConfig().threads;

        String childName = "G" + generation + "-"
                + parentA.getName().substring(0, Math.min(3, parentA.getName().length()))
                + "x"
                + parentB.getName().substring(0, Math.min(3, parentB.getName().length()));

        worst.dispose();
        contestants.remove(worst);
        if (artEvolver.getFitnessChartWindow() != null) {
            artEvolver.getFitnessChartWindow().clearSeries(worst.getId());
        }

        TournamentContestant child = new TournamentContestant("evo" + nextId++, childName);
        child.setConfig(childConfig);
        childConfig.name = childName;
        childConfig.chartColor = child.getChartColor();
        child.setGeneration(generation);
        child.setParentage(parentA.getName() + " x " + parentB.getName());

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

        double[] scores = ranked.stream().mapToDouble(TournamentContestant::getBestScore).toArray();
        double avgScore = Arrays.stream(scores).average().orElse(0);

        GenerationRecord rec = new GenerationRecord(generation);
        rec.timestamp = System.currentTimeMillis();
        rec.culledName = culledName;
        rec.culledScore = culledScore;
        rec.culledGeneration = culledGen;
        rec.parentA = parentA.getName();
        rec.parentB = parentB.getName();
        rec.childName = childName;
        rec.childParams = childConfig.toSummary();
        rec.bestName = best.getName();
        rec.bestScore = best.getBestScore();
        rec.worstScore = culledScore;
        rec.avgScore = avgScore;
        rec.aliveCount = contestants.size();
        rec.bestEverScore = bestEverScore;
        rec.bestEverName = bestEverName;
        rec.converged = converged;
        history.add(rec);

        artEvolver.refreshContestantCombo();
        if (artEvolver.getTournamentManagerWindow() != null) {
            artEvolver.getTournamentManagerWindow().refreshTable();
        }

        System.out.println("[EvoTournament] === Generation " + generation + " ===");
        System.out.println("[EvoTournament] Ranking: " + formatRanking(ranked));
        System.out.println("[EvoTournament] Culled: " + culledName
                + " (" + DF4.format(culledScore * 100) + "%, gen " + culledGen + ")");
        System.out.println("[EvoTournament] Bred: " + childName
                + " from [" + parentA.getName() + " x " + parentB.getName() + "]");
        System.out.println("[EvoTournament] Best: " + best.getName()
                + " (" + DF4.format(best.getBestScore() * 100) + "%)  Avg: "
                + DF4.format(avgScore * 100) + "%");
        if (converged) {
            System.out.println("[EvoTournament] CONVERGENCE DETECTED — "
                    + stalledGenerations + " generations without significant improvement");
        }
    }

    private String formatRanking(List<TournamentContestant> ranked) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ranked.size(); i++) {
            TournamentContestant c = ranked.get(i);
            if (i > 0) sb.append(", ");
            sb.append(i + 1).append(". ").append(c.getName())
              .append(" ").append(DF2.format(c.getBestScore() * 100)).append("%");
        }
        return sb.toString();
    }

    private TournamentContestant selectParent(List<TournamentContestant> ranked) {
        int topHalf = Math.max(2, ranked.size() / 2);
        TournamentContestant a = ranked.get(RNG.nextInt(topHalf));
        TournamentContestant b = ranked.get(RNG.nextInt(topHalf));
        return (a.getBestScore() >= b.getBestScore()) ? a : b;
    }

    private EvolutionConfig breedConfigs(EvolutionConfig cfgA, EvolutionConfig cfgB) {
        float[] genesA = cfgA.toGeneArray();
        float[] genesB = cfgB.toGeneArray();
        float[] childGenes = crossover(genesA, genesB);
        childGenes = mutate(childGenes);
        return EvolutionConfig.fromGeneArray(childGenes, cfgA);
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

    // --- Accessors ---

    public boolean isRunning() { return running; }
    public int getGeneration() { return generation; }
    public boolean isConverged() { return converged; }
    public int getStalledGenerations() { return stalledGenerations; }
    public double getBestEverScore() { return bestEverScore; }
    public String getBestEverName() { return bestEverName; }
    public EvolutionConfig getBestEverConfig() { return bestEverConfig; }
    public List<GenerationRecord> getHistory() { return Collections.unmodifiableList(history); }

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

    /**
     * Record of one evolutionary generation.
     */
    public static class GenerationRecord {
        public final int generation;
        public long timestamp;
        public boolean skipped;
        public String skipReason;

        public String culledName;
        public double culledScore;
        public int culledGeneration;
        public String parentA;
        public String parentB;
        public String childName;
        public String childParams;

        public String bestName;
        public double bestScore;
        public double worstScore;
        public double avgScore;
        public int aliveCount;
        public double bestEverScore;
        public String bestEverName;
        public boolean converged;

        GenerationRecord(int gen) {
            this.generation = gen;
        }

        @Override
        public String toString() {
            if (skipped) {
                return "Gen " + generation + ": SKIPPED — " + skipReason;
            }
            return "Gen " + generation + ": "
                    + culledName + " (" + DF2_STATIC.format(culledScore * 100) + "%) CULLED"
                    + " → " + childName + " bred [" + parentA + " × " + parentB + "]"
                    + "  |  Best: " + bestName + " " + DF2_STATIC.format(bestScore * 100) + "%"
                    + "  Avg: " + DF2_STATIC.format(avgScore * 100) + "%";
        }

        private static final DecimalFormat DF2_STATIC = new DecimalFormat("0.00");
    }
}
