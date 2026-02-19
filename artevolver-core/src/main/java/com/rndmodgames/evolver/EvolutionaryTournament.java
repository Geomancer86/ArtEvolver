package com.rndmodgames.evolver;

import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.*;
import javax.swing.Timer;

/**
 * Meta-genetic algorithm that evolves the GA parameters themselves.
 *
 * At regular intervals (cutoff), the worst-performing contestant is culled
 * and replaced by an offspring bred from the survivors' parameter DNA via
 * BLX-alpha crossover + Gaussian mutation.
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

    private static final float BLX_ALPHA = 0.5f;
    private static final Random RNG = new Random();
    private static final DecimalFormat DF = new DecimalFormat("0.0000");

    private final List<GenerationRecord> history = new ArrayList<>();

    public EvolutionaryTournament(ArtEvolver artEvolver, List<TournamentContestant> contestants) {
        this.artEvolver = artEvolver;
        this.contestants = contestants;
    }

    public void start() {
        if (running) return;
        if (contestants.size() < minContestants) return;
        running = true;
        generation = 0;

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

    private void onCutoffTick() {
        if (!running || contestants.size() < minContestants) return;

        generation++;
        System.out.println("[EvoTournament] === Generation " + generation + " ===");

        List<TournamentContestant> ranked = new ArrayList<>(contestants);
        ranked.sort((a, b) -> Double.compare(b.getBestScore(), a.getBestScore()));

        TournamentContestant worst = ranked.get(ranked.size() - 1);
        String culledName = worst.getName();
        double culledScore = worst.getBestScore();

        TournamentContestant parentA = selectParent(ranked);
        TournamentContestant parentB = selectParent(ranked);
        while (parentB == parentA && ranked.size() > 2) {
            parentB = selectParent(ranked);
        }

        EvolutionConfig childConfig = breedConfigs(parentA.getConfig(), parentB.getConfig());
        childConfig.threads = worst.getConfig().threads;

        String childName = "Gen" + generation + "-"
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

        GenerationRecord rec = new GenerationRecord(generation, culledName, culledScore,
                parentA.getName(), parentB.getName(), childName, childConfig.toSummary());
        history.add(rec);

        artEvolver.refreshContestantCombo();
        if (artEvolver.getTournamentManagerWindow() != null) {
            artEvolver.getTournamentManagerWindow().refreshTable();
        }

        System.out.println("[EvoTournament] Culled: " + culledName
                + " (" + DF.format(culledScore * 100) + "%)");
        System.out.println("[EvoTournament] Bred: " + childName
                + " from [" + parentA.getName() + " x " + parentB.getName() + "]");
        System.out.println("[EvoTournament] Best: " + ranked.get(0).getName()
                + " (" + DF.format(ranked.get(0).getBestScore() * 100) + "%)");
    }

    /**
     * Tournament selection: pick 2 random from the top half, return the better.
     */
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

    /**
     * BLX-alpha crossover: for each gene, pick a random value in [min-alpha*d, max+alpha*d].
     */
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
    public List<GenerationRecord> getHistory() { return Collections.unmodifiableList(history); }

    public int getCutoffSeconds() { return cutoffSeconds; }
    public void setCutoffSeconds(int s) { this.cutoffSeconds = Math.max(10, s); }
    public float getMutationRate() { return mutationRate; }
    public void setMutationRate(float r) { this.mutationRate = Math.max(0f, Math.min(1f, r)); }
    public float getMutationStrength() { return mutationStrength; }
    public void setMutationStrength(float s) { this.mutationStrength = Math.max(0f, Math.min(1f, s)); }
    public int getMinContestants() { return minContestants; }
    public void setMinContestants(int n) { this.minContestants = Math.max(2, n); }

    /**
     * Record of one evolutionary generation (cull + breed event).
     */
    public static class GenerationRecord {
        public final int generation;
        public final String culledName;
        public final double culledScore;
        public final String parentA;
        public final String parentB;
        public final String childName;
        public final String childParams;

        GenerationRecord(int gen, String culled, double score,
                         String pA, String pB, String child, String params) {
            this.generation = gen;
            this.culledName = culled;
            this.culledScore = score;
            this.parentA = pA;
            this.parentB = pB;
            this.childName = child;
            this.childParams = params;
        }

        @Override
        public String toString() {
            return "Gen " + generation + ": culled " + culledName
                    + " (" + new DecimalFormat("0.00").format(culledScore * 100) + "%)"
                    + " -> bred " + childName + " [" + parentA + " x " + parentB + "]";
        }
    }
}
