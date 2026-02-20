package com.rndmodgames.evolver;

/**
 * Represents a single stage (gear) in a multi-stage evolution strategy.
 * Each stage has its own mutation/crossover parameters and a trigger condition
 * that determines when to advance to the next stage.
 *
 * The last stage's trigger is ignored — it runs until the contestant finishes.
 */
public class EvolutionStage implements Cloneable {

    public enum TriggerType {
        TIME,       // Advance after N seconds of total contestant age
        STALE,      // Advance when velocity drops below threshold
        FITNESS     // Advance when fitness exceeds threshold
    }

    private String name;
    private EvolutionConfig config;
    private TriggerType triggerType = TriggerType.TIME;
    private double triggerValue = 20.0;

    public EvolutionStage() {}

    public EvolutionStage(String name, EvolutionConfig config, TriggerType triggerType, double triggerValue) {
        this.name = name;
        this.config = config;
        this.triggerType = triggerType;
        this.triggerValue = triggerValue;
    }

    /** Creates a terminal stage (last stage — trigger is never evaluated). */
    public EvolutionStage(String name, EvolutionConfig config) {
        this.name = name;
        this.config = config;
        this.triggerType = TriggerType.TIME;
        this.triggerValue = 999999;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public EvolutionConfig getConfig() { return config; }
    public void setConfig(EvolutionConfig config) { this.config = config; }
    public TriggerType getTriggerType() { return triggerType; }
    public void setTriggerType(TriggerType triggerType) { this.triggerType = triggerType; }
    public double getTriggerValue() { return triggerValue; }
    public void setTriggerValue(double triggerValue) { this.triggerValue = triggerValue; }

    /**
     * Encodes this stage's mutation parameters + trigger value as a gene array.
     * Format: [9 config genes, triggerValue] = 10 floats.
     * Trigger type is encoded separately or fixed.
     */
    public float[] toGeneArray() {
        float[] cfgGenes = config.toGeneArray();
        float[] genes = new float[cfgGenes.length + 1];
        System.arraycopy(cfgGenes, 0, genes, 0, cfgGenes.length);
        genes[cfgGenes.length] = (float) triggerValue;
        return genes;
    }

    public static EvolutionStage fromGeneArray(float[] genes, EvolutionConfig template,
                                                TriggerType type, String name) {
        float[] cfgGenes = new float[EvolutionConfig.GENE_COUNT];
        System.arraycopy(genes, 0, cfgGenes, 0, EvolutionConfig.GENE_COUNT);
        EvolutionConfig cfg = EvolutionConfig.fromGeneArray(cfgGenes, template);
        double trigger = Math.max(0, genes[EvolutionConfig.GENE_COUNT]);
        EvolutionStage stage = new EvolutionStage(name, cfg, type, trigger);
        return stage;
    }

    public static final int GENES_PER_STAGE = EvolutionConfig.GENE_COUNT + 1; // 10

    @Override
    public EvolutionStage clone() {
        try {
            EvolutionStage copy = (EvolutionStage) super.clone();
            if (config != null) copy.config = config.clone();
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
