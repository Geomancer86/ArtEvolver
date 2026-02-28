package com.rndmodgames.evolver.clicker;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * O(1) per-swap delta fitness engine for pixel grids.
 *
 * Much simpler than DeltaFitnessEngine (triangles): each pixel is exactly
 * one cell, so a swap touches exactly 2 pixels. No masks needed.
 *
 * Fitness = 1.0 - (totalDiff / (width * height * 3 * 255))
 * Same formula as the triangle engine for consistent scoring.
 */
public class PixelFitnessEngine {

    private final int totalPixels;
    private final int[] refR, refG, refB;
    private final int[] palR, palG, palB;

    private final PixelGrid grid;
    private long totalDiff;

    public PixelFitnessEngine(PixelGrid grid, BufferedImage referenceImage) {
        this.grid = grid;
        this.totalPixels = grid.getTotalPixels();

        int[] refPixels = new int[totalPixels];
        referenceImage.getRGB(0, 0, grid.getWidth(), grid.getHeight(), refPixels, 0, grid.getWidth());

        refR = new int[totalPixels];
        refG = new int[totalPixels];
        refB = new int[totalPixels];
        for (int i = 0; i < totalPixels; i++) {
            refR[i] = (refPixels[i] >> 16) & 0xff;
            refG[i] = (refPixels[i] >> 8) & 0xff;
            refB[i] = refPixels[i] & 0xff;
        }

        Color[] palette = grid.getPalette();
        int palSize = palette.length;
        palR = new int[palSize];
        palG = new int[palSize];
        palB = new int[palSize];
        for (int c = 0; c < palSize; c++) {
            palR[c] = palette[c].getRed();
            palG[c] = palette[c].getGreen();
            palB[c] = palette[c].getBlue();
        }

        totalDiff = computeFullDiff();
    }

    private long computeFullDiff() {
        long diff = 0;
        int[] indices = grid.getColorIndices();
        for (int i = 0; i < totalPixels; i++) {
            int c = indices[i];
            diff += Math.abs(refR[i] - palR[c]) + Math.abs(refG[i] - palG[c]) + Math.abs(refB[i] - palB[c]);
        }
        return diff;
    }

    /**
     * O(1) swap delta: only 2 pixels change. Returns negative if swap improves fitness.
     */
    public long computeSwapDelta(int pxA, int pxB) {
        int cA = grid.getColorIndex(pxA);
        int cB = grid.getColorIndex(pxB);
        if (cA == cB) return 0;

        long delta = 0;

        // Pixel A: remove old (cA), add new (cB)
        delta -= Math.abs(refR[pxA] - palR[cA]) + Math.abs(refG[pxA] - palG[cA]) + Math.abs(refB[pxA] - palB[cA]);
        delta += Math.abs(refR[pxA] - palR[cB]) + Math.abs(refG[pxA] - palG[cB]) + Math.abs(refB[pxA] - palB[cB]);

        // Pixel B: remove old (cB), add new (cA)
        delta -= Math.abs(refR[pxB] - palR[cB]) + Math.abs(refG[pxB] - palG[cB]) + Math.abs(refB[pxB] - palB[cB]);
        delta += Math.abs(refR[pxB] - palR[cA]) + Math.abs(refG[pxB] - palG[cA]) + Math.abs(refB[pxB] - palB[cA]);

        return delta;
    }

    public void applySwap(int pxA, int pxB) {
        totalDiff += computeSwapDelta(pxA, pxB);
        grid.swap(pxA, pxB);
    }

    public void applySwapWithDelta(int pxA, int pxB, long delta) {
        totalDiff += delta;
        grid.swap(pxA, pxB);
    }

    public boolean trySwap(int pxA, int pxB) {
        long delta = computeSwapDelta(pxA, pxB);
        if (delta < 0) {
            applySwapWithDelta(pxA, pxB, delta);
            return true;
        }
        return false;
    }

    public double getScore() {
        return 1.0 - ((double) totalDiff / ((double) totalPixels * 3.0 * 255.0));
    }

    public long getTotalDiff() { return totalDiff; }
    public int getTotalPixels() { return totalPixels; }

    /**
     * Per-pixel error for smart targeting (pick worst pixel).
     */
    public long getPixelError(int px) {
        int c = grid.getColorIndex(px);
        return Math.abs(refR[px] - palR[c]) + Math.abs(refG[px] - palG[c]) + Math.abs(refB[px] - palB[c]);
    }
}
