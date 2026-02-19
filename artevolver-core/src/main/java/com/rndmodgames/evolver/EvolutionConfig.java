package com.rndmodgames.evolver;

import java.awt.Color;

/**
 * Encapsulates all tunable evolution parameters for a single contestant.
 * Replaces the static fields in CrossOver and ArtEvolver with instance-level
 * configuration, allowing multiple independent evolution runs with different
 * parameter sets (Tournament Mode).
 *
 * Default values match the original static defaults for backward compatibility.
 */
public class EvolutionConfig implements Cloneable {

    public String name = "Default";
    public Color chartColor = new Color(80, 200, 120);

    // --- Grid Mutation ---
    public float gridMutationChances = 32;
    public float gridMutationPercent = 1.0f;
    public float gridMutationDecay = 0.1f;

    // --- Random Mutation ---
    public int randomMutationChances = 1000;
    public float randomMutationPercent = 0.001f;

    // --- Close Mutation ---
    public int closeMutationChances = 20;
    public float closeMutationPercent = 0.0001f;

    // --- Grid Random Mutation ---
    public int randomGridMutationChances = 10;
    public float randomGridMutationPercent = 0.0001f;

    // --- Targeted Swap ---
    public int targetedSwapAttempts = 12;

    // --- Crossover ---
    public boolean blockCrossoverEnabled = true;

    // --- Population ---
    public int threads = 24;
    public int population = 2;
    public int crossoverMax = 2;

    // --- Evolution ---
    public int evolveIterations = 2;
    public boolean useDeltaEvolution = true;
    public int initializationMethod = 1; // 0=Random, 1=Smart, 2=LAP
    public boolean validatePermutation = true;

    public EvolutionConfig() {}

    /**
     * Creates a config populated from the current static/global settings.
     */
    public static EvolutionConfig fromCurrentSettings() {
        EvolutionConfig cfg = new EvolutionConfig();
        cfg.gridMutationChances = CrossOver.GRID_MUTATION_CHANCES;
        cfg.gridMutationPercent = CrossOver.GRID_MUTATION_PERCENT;
        cfg.gridMutationDecay = CrossOver.GRID_MUTATION_DECAY;
        cfg.randomMutationChances = CrossOver.RANDOM_MUTATION_CHANCES;
        cfg.randomMutationPercent = CrossOver.RANDOM_MUTATION_PERCENT;
        cfg.closeMutationChances = CrossOver.RANDOM_CLOSE_MUTATION_CHANCES;
        cfg.closeMutationPercent = CrossOver.RANDOM_CLOSE_MUTATION_PERCENT;
        cfg.randomGridMutationChances = CrossOver.RANDOM_GRID_MUTATION_CHANCES;
        cfg.randomGridMutationPercent = CrossOver.RANDOM_GRID_MUTATION_PERCENT;
        cfg.targetedSwapAttempts = CrossOver.TARGETED_SWAP_ATTEMPTS;
        cfg.blockCrossoverEnabled = CrossOver.CROSSOVER_BLOCK_ENABLED;
        cfg.validatePermutation = ImageEvolver.VALIDATE_PERMUTATION;
        return cfg;
    }

    /**
     * Writes this config's values back to the static fields (for backward compat).
     */
    public void applyToStaticFields() {
        CrossOver.GRID_MUTATION_CHANCES = gridMutationChances;
        CrossOver.GRID_MUTATION_PERCENT = gridMutationPercent;
        CrossOver.GRID_MUTATION_DECAY = gridMutationDecay;
        CrossOver.RANDOM_MUTATION_CHANCES = randomMutationChances;
        CrossOver.RANDOM_MUTATION_PERCENT = randomMutationPercent;
        CrossOver.RANDOM_CLOSE_MUTATION_CHANCES = closeMutationChances;
        CrossOver.RANDOM_CLOSE_MUTATION_PERCENT = closeMutationPercent;
        CrossOver.RANDOM_GRID_MUTATION_CHANCES = randomGridMutationChances;
        CrossOver.RANDOM_GRID_MUTATION_PERCENT = randomGridMutationPercent;
        CrossOver.TARGETED_SWAP_ATTEMPTS = targetedSwapAttempts;
        CrossOver.CROSSOVER_BLOCK_ENABLED = blockCrossoverEnabled;
        ImageEvolver.VALIDATE_PERMUTATION = validatePermutation;
    }

    public String toSummary() {
        return "Grid=" + (int) gridMutationChances
                + " Rnd=" + randomMutationChances
                + " Close=" + closeMutationChances
                + " Tgt=" + targetedSwapAttempts
                + " Pop=" + population
                + " Thr=" + threads;
    }

    // --- Gene array support for evolutionary parameter breeding ---

    public static final int GENE_COUNT = 9;

    private static final float[] GENE_MIN = {
        0f,     // gridMutationChances
        0f,     // gridMutationDecay
        0f,     // randomMutationChances
        0f,     // randomMutationPercent
        0f,     // closeMutationChances
        0f,     // closeMutationPercent
        0f,     // targetedSwapAttempts
        1f,     // population
        1f,     // crossoverMax
    };

    private static final float[] GENE_MAX = {
        256f,   // gridMutationChances
        1f,     // gridMutationDecay
        10000f, // randomMutationChances
        0.1f,   // randomMutationPercent
        200f,   // closeMutationChances
        0.01f,  // closeMutationPercent
        128f,   // targetedSwapAttempts
        8f,     // population
        8f,     // crossoverMax
    };

    public static float[] getGeneMin() { return GENE_MIN.clone(); }
    public static float[] getGeneMax() { return GENE_MAX.clone(); }

    public float[] toGeneArray() {
        return new float[] {
            gridMutationChances,
            gridMutationDecay,
            (float) randomMutationChances,
            randomMutationPercent,
            (float) closeMutationChances,
            closeMutationPercent,
            (float) targetedSwapAttempts,
            (float) population,
            (float) crossoverMax,
        };
    }

    /**
     * Creates a config from a gene array, copying non-gene fields from template.
     */
    public static EvolutionConfig fromGeneArray(float[] genes, EvolutionConfig template) {
        EvolutionConfig cfg = template.clone();
        cfg.gridMutationChances = clampGene(genes[0], 0);
        cfg.gridMutationDecay   = clampGene(genes[1], 1);
        cfg.randomMutationChances = (int) clampGene(genes[2], 2);
        cfg.randomMutationPercent = clampGene(genes[3], 3);
        cfg.closeMutationChances  = (int) clampGene(genes[4], 4);
        cfg.closeMutationPercent  = clampGene(genes[5], 5);
        cfg.targetedSwapAttempts  = (int) clampGene(genes[6], 6);
        cfg.population            = Math.max(1, (int) clampGene(genes[7], 7));
        cfg.crossoverMax          = Math.max(1, (int) clampGene(genes[8], 8));
        return cfg;
    }

    private static float clampGene(float value, int geneIndex) {
        return Math.max(GENE_MIN[geneIndex], Math.min(GENE_MAX[geneIndex], value));
    }

    @Override
    public EvolutionConfig clone() {
        try {
            return (EvolutionConfig) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
