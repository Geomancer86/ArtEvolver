package com.rndmodgames.evolver;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents one node in the contestant ancestry tree, inspired by pedigree
 * tracking in animal breeding (BLUP) and phylogenetic tracking in digital
 * evolution (Phylotrack).
 *
 * Each node stores the contestant's config snapshot, peak performance metrics,
 * and links to parent(s) and children. The tree persists even after contestants
 * are eliminated, enabling multi-generational breeding and lineage analysis.
 *
 * Lineage fitness is computed as a decay-weighted average of peak fitness across
 * ancestors, so parameter configurations whose descendants consistently perform
 * well are favored for parent selection.
 */
public class LineageNode {

    private final String contestantId;
    private final String contestantName;
    private final int generation;
    private final EvolutionConfig configSnapshot;

    private double peakFitness;
    private double peakVelocity;
    private boolean eliminated;

    private LineageNode parentA;
    private LineageNode parentB;
    private final List<LineageNode> children = new ArrayList<>();

    public LineageNode(String contestantId, String contestantName, int generation,
                       EvolutionConfig configSnapshot) {
        this.contestantId = contestantId;
        this.contestantName = contestantName;
        this.generation = generation;
        this.configSnapshot = (configSnapshot != null) ? configSnapshot.clone() : null;
    }

    // --- Ancestry traversal ---

    /**
     * Computes lineage fitness: a decay-weighted average of peak fitness across
     * this node and its ancestors, up to maxDepth generations back.
     *
     * lineageFitness = Σ (peakFitness[ancestor] × decay^depth) / Σ (decay^depth)
     *
     * @param decay factor per generation (0-1, e.g. 0.7 means grandparent counts 49%)
     * @param maxDepth maximum generations back to traverse
     */
    public double getLineageFitness(double decay, int maxDepth) {
        double[] result = new double[2];
        accumulateLineage(this, 0, decay, maxDepth, result, true);
        return result[1] > 0 ? result[0] / result[1] : peakFitness;
    }

    /**
     * Computes lineage velocity: same decay-weighted average applied to peak velocities.
     */
    public double getLineageVelocity(double decay, int maxDepth) {
        double[] result = new double[2];
        accumulateLineage(this, 0, decay, maxDepth, result, false);
        return result[1] > 0 ? result[0] / result[1] : peakVelocity;
    }

    private void accumulateLineage(LineageNode node, int depth, double decay,
                                   int maxDepth, double[] result, boolean useFitness) {
        if (node == null || depth > maxDepth) return;

        double weight = Math.pow(decay, depth);
        double value = useFitness ? node.peakFitness : node.peakVelocity;
        result[0] += value * weight;
        result[1] += weight;

        if (node.parentA != null) {
            accumulateLineage(node.parentA, depth + 1, decay, maxDepth, result, useFitness);
        }
        if (node.parentB != null) {
            accumulateLineage(node.parentB, depth + 1, decay, maxDepth, result, useFitness);
        }
    }

    /**
     * Returns all ancestors up to maxDepth, including this node.
     * Useful for inbreeding detection and ancestral crossover gene blending.
     */
    public List<LineageNode> getAncestors(int maxDepth) {
        List<LineageNode> ancestors = new ArrayList<>();
        collectAncestors(this, 0, maxDepth, ancestors);
        return ancestors;
    }

    private void collectAncestors(LineageNode node, int depth, int maxDepth,
                                  List<LineageNode> out) {
        if (node == null || depth > maxDepth) return;
        out.add(node);
        collectAncestors(node.parentA, depth + 1, maxDepth, out);
        collectAncestors(node.parentB, depth + 1, maxDepth, out);
    }

    /**
     * Checks if this node shares a common ancestor with another node within
     * the given depth. Used for inbreeding prevention.
     */
    public boolean sharesAncestorWith(LineageNode other, int withinDepth) {
        if (other == null) return false;
        List<LineageNode> myAncestors = getAncestors(withinDepth);
        List<LineageNode> theirAncestors = other.getAncestors(withinDepth);
        for (LineageNode mine : myAncestors) {
            for (LineageNode theirs : theirAncestors) {
                if (mine.contestantId.equals(theirs.contestantId)) return true;
            }
        }
        return false;
    }

    /**
     * Returns gene arrays from ancestors up to maxDepth, with associated
     * decay weights. Index 0 is this node (weight=1), then parents, etc.
     */
    public List<WeightedGenes> getAncestralGenes(double decay, int maxDepth) {
        List<WeightedGenes> genes = new ArrayList<>();
        collectGenes(this, 0, decay, maxDepth, genes);
        return genes;
    }

    private void collectGenes(LineageNode node, int depth, double decay,
                              int maxDepth, List<WeightedGenes> out) {
        if (node == null || depth > maxDepth || node.configSnapshot == null) return;
        out.add(new WeightedGenes(node.configSnapshot.toGeneArray(), Math.pow(decay, depth)));
        collectGenes(node.parentA, depth + 1, decay, maxDepth, out);
        collectGenes(node.parentB, depth + 1, decay, maxDepth, out);
    }

    /**
     * Returns a formatted ancestry string for display.
     */
    public String getAncestryString(int maxDepth) {
        StringBuilder sb = new StringBuilder();
        formatAncestry(this, 0, maxDepth, sb, "");
        return sb.toString();
    }

    private void formatAncestry(LineageNode node, int depth, int maxDepth,
                                StringBuilder sb, String prefix) {
        if (node == null || depth > maxDepth) return;
        if (depth > 0) sb.append(prefix);
        sb.append(node.contestantName);
        if (node.peakFitness > 0) {
            sb.append(String.format(" (%.2f%%)", node.peakFitness * 100));
        }
        if (depth < maxDepth) {
            if (node.parentA != null) {
                sb.append("\n");
                formatAncestry(node.parentA, depth + 1, maxDepth, sb, prefix + "  ├─ ");
            }
            if (node.parentB != null) {
                sb.append("\n");
                formatAncestry(node.parentB, depth + 1, maxDepth, sb, prefix + "  └─ ");
            }
        }
    }

    // --- Accessors ---

    public String getContestantId() { return contestantId; }
    public String getContestantName() { return contestantName; }
    public int getGeneration() { return generation; }
    public EvolutionConfig getConfigSnapshot() { return configSnapshot; }
    public double getPeakFitness() { return peakFitness; }
    public void setPeakFitness(double peakFitness) { this.peakFitness = peakFitness; }
    public double getPeakVelocity() { return peakVelocity; }
    public void setPeakVelocity(double peakVelocity) { this.peakVelocity = peakVelocity; }
    public boolean isEliminated() { return eliminated; }
    public void setEliminated(boolean eliminated) { this.eliminated = eliminated; }
    public LineageNode getParentA() { return parentA; }
    public void setParentA(LineageNode parentA) { this.parentA = parentA; }
    public LineageNode getParentB() { return parentB; }
    public void setParentB(LineageNode parentB) { this.parentB = parentB; }
    public List<LineageNode> getChildren() { return children; }

    public void addChild(LineageNode child) {
        children.add(child);
    }

    /**
     * Gene array + weight pair for multi-generational crossover.
     */
    public static class WeightedGenes {
        public final float[] genes;
        public final double weight;

        public WeightedGenes(float[] genes, double weight) {
            this.genes = genes;
            this.weight = weight;
        }
    }
}
