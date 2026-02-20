package com.rndmodgames.evolver;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

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

    // --- Multi-stage (geared) evolution ---
    private List<EvolutionStage> stages = null;

    public EvolutionConfig() {}

    public boolean isMultiStage() { return stages != null && stages.size() > 1; }
    public List<EvolutionStage> getStages() { return stages; }
    public void setStages(List<EvolutionStage> stages) { this.stages = stages; }
    public int getStageCount() { return stages != null ? stages.size() : 1; }

    // ════════════════════════════════════════════════════════════════
    //  Prehistoric Mode — Era-specific config factories
    // ════════════════════════════════════════════════════════════════

    /**
     * Era 0 — Primordial Soup: the absolute minimum. Random init, legacy evolve,
     * only random swaps, no crossover, no intelligence. Pure brute-force random walk.
     */
    public static EvolutionConfig createPrimordial() {
        EvolutionConfig cfg = new EvolutionConfig();
        cfg.name = "Primordial";
        cfg.threads = 1;
        cfg.population = 1;
        cfg.crossoverMax = 1;
        cfg.useDeltaEvolution = false;
        cfg.initializationMethod = 0;
        cfg.blockCrossoverEnabled = false;
        cfg.gridMutationChances = 0;
        cfg.gridMutationPercent = 0;
        cfg.gridMutationDecay = 0;
        cfg.closeMutationChances = 0;
        cfg.closeMutationPercent = 0;
        cfg.randomGridMutationChances = 0;
        cfg.randomGridMutationPercent = 0;
        cfg.targetedSwapAttempts = 0;
        cfg.randomMutationChances = 1000;
        cfg.randomMutationPercent = 0.01f;
        cfg.evolveIterations = 1;
        cfg.validatePermutation = true;
        return cfg;
    }

    /**
     * Era 1 — Single Cell: adds grid swaps and close mutations on top of primordial.
     */
    public static EvolutionConfig createEra1() {
        EvolutionConfig cfg = createPrimordial();
        cfg.name = "Single Cell";
        cfg.gridMutationChances = 16;
        cfg.gridMutationPercent = 1.0f;
        cfg.gridMutationDecay = 0.1f;
        cfg.closeMutationChances = 10;
        cfg.closeMutationPercent = 0.0001f;
        return cfg;
    }

    /**
     * Era 2 — Multicellular: smart init, population=2, crossover enabled.
     */
    public static EvolutionConfig createEra2() {
        EvolutionConfig cfg = createEra1();
        cfg.name = "Multicellular";
        cfg.initializationMethod = 1;
        cfg.population = 2;
        cfg.crossoverMax = 2;
        cfg.blockCrossoverEnabled = true;
        cfg.evolveIterations = 2;
        return cfg;
    }

    /**
     * Era 3 — Cambrian Explosion: delta evolution + targeted swaps. Major inflection.
     */
    public static EvolutionConfig createEra3() {
        EvolutionConfig cfg = createEra2();
        cfg.name = "Cambrian";
        cfg.useDeltaEvolution = true;
        cfg.targetedSwapAttempts = 8;
        cfg.randomGridMutationChances = 5;
        cfg.randomGridMutationPercent = 0.0001f;
        return cfg;
    }

    /**
     * Era 4 — Age of Fish: 2 threads, full mutation suite.
     */
    public static EvolutionConfig createEra4() {
        EvolutionConfig cfg = createEra3();
        cfg.name = "Age of Fish";
        cfg.threads = 2;
        cfg.gridMutationChances = 32;
        cfg.randomMutationChances = 1000;
        cfg.randomMutationPercent = 0.001f;
        cfg.closeMutationChances = 20;
        cfg.closeMutationPercent = 0.0001f;
        cfg.targetedSwapAttempts = 12;
        cfg.randomGridMutationChances = 10;
        cfg.randomGridMutationPercent = 0.0001f;
        return cfg;
    }

    /**
     * Era 5+ — Age of Reptiles: thread count scales, preset strategies appear.
     */
    public static EvolutionConfig createEra5(int threadCount) {
        EvolutionConfig cfg = createEra4();
        cfg.name = "Age of Reptiles";
        cfg.threads = Math.max(2, threadCount);
        return cfg;
    }

    /**
     * Era 6 — Age of Mammals: full thread allocation, all strategies deployed.
     */
    public static EvolutionConfig createEra6(int threadCount) {
        EvolutionConfig cfg = createEra5(threadCount);
        cfg.name = "Age of Mammals";
        return cfg;
    }

    /**
     * Era 7 — Age of Intelligence: fully evolved, evolutionary tournament activates.
     */
    public static EvolutionConfig createEra7(int threadCount) {
        EvolutionConfig cfg = createEra6(threadCount);
        cfg.name = "Intelligence";
        return cfg;
    }

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
        String base = "Grid=" + (int) gridMutationChances
                + " Rnd=" + randomMutationChances
                + " Close=" + closeMutationChances
                + " Tgt=" + targetedSwapAttempts
                + " Pop=" + population
                + " Thr=" + threads;
        if (isMultiStage()) {
            base += " [" + stages.size() + " gears]";
        }
        return base;
    }

    /**
     * Returns the full gene array for multi-stage configs.
     * Format: [stage0 genes(9) + trigger(1), stage1 genes + trigger, ...]
     * For single-stage, returns the standard 9-gene array.
     */
    public float[] toMultiStageGeneArray() {
        if (!isMultiStage()) return toGeneArray();
        float[] result = new float[stages.size() * EvolutionStage.GENES_PER_STAGE];
        int offset = 0;
        for (EvolutionStage stage : stages) {
            float[] sg = stage.toGeneArray();
            System.arraycopy(sg, 0, result, offset, sg.length);
            offset += EvolutionStage.GENES_PER_STAGE;
        }
        return result;
    }

    /**
     * Creates a multi-stage config from a flat gene array.
     * @param stageCount number of stages to decode
     * @param triggerTypes trigger type for each stage
     */
    public static EvolutionConfig fromMultiStageGeneArray(float[] genes, int stageCount,
            EvolutionStage.TriggerType[] triggerTypes, EvolutionConfig template) {
        EvolutionConfig cfg = template.clone();
        List<EvolutionStage> stageList = new ArrayList<>(stageCount);
        int offset = 0;
        String[] defaultNames = {"Start", "Mid", "Endgame", "Stage 4", "Stage 5",
                                  "Stage 6", "Stage 7", "Stage 8"};
        for (int i = 0; i < stageCount; i++) {
            float[] stageGenes = new float[EvolutionStage.GENES_PER_STAGE];
            System.arraycopy(genes, offset, stageGenes, 0,
                    Math.min(stageGenes.length, genes.length - offset));
            offset += EvolutionStage.GENES_PER_STAGE;
            String sname = i < defaultNames.length ? defaultNames[i] : "Stage " + (i + 1);
            EvolutionStage.TriggerType tt = (triggerTypes != null && i < triggerTypes.length)
                    ? triggerTypes[i] : EvolutionStage.TriggerType.TIME;
            stageList.add(EvolutionStage.fromGeneArray(stageGenes, template, tt, sname));
        }
        cfg.setStages(stageList);
        // Apply stage 0 config as the active config
        if (!stageList.isEmpty()) {
            EvolutionConfig s0 = stageList.get(0).getConfig();
            cfg.gridMutationChances = s0.gridMutationChances;
            cfg.gridMutationDecay = s0.gridMutationDecay;
            cfg.randomMutationChances = s0.randomMutationChances;
            cfg.randomMutationPercent = s0.randomMutationPercent;
            cfg.closeMutationChances = s0.closeMutationChances;
            cfg.closeMutationPercent = s0.closeMutationPercent;
            cfg.targetedSwapAttempts = s0.targetedSwapAttempts;
        }
        return cfg;
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
            EvolutionConfig copy = (EvolutionConfig) super.clone();
            if (stages != null) {
                copy.stages = new ArrayList<>(stages.size());
                for (EvolutionStage s : stages) {
                    copy.stages.add(s.clone());
                }
            }
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
