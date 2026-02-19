package com.rndmodgames.evolver;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Pre-computes triangle pixel masks and provides O(pixels_per_triangle)
 * delta fitness evaluation instead of O(total_pixels) full-image comparison.
 *
 * For a typical 5700-triangle / 1.85M-pixel canvas, a single swap evaluation
 * touches ~650 pixels instead of 1.85M — roughly 3000x fewer operations.
 *
 * Accounts for background pixels (those not covered by any triangle are black
 * in the rendered image, contributing |refR|+|refG|+|refB| to the total diff).
 *
 * Usage:
 *   engine = new DeltaFitnessEngine(triangles, referenceImage);
 *   double score = engine.getScore();
 *   long delta = engine.computeSwapDelta(i, j);
 *   if (delta < 0) engine.applySwap(i, j);
 */
public class DeltaFitnessEngine {

    private final int imageWidth;
    private final int imageHeight;
    private final int totalPixels;

    private final int[] refR, refG, refB;

    private final int[][] trianglePixelIndices;
    private final int triangleCount;

    private long totalDiff;

    private int[] currentColorR, currentColorG, currentColorB;
    private PalleteColor[] currentPalleteColors;

    public DeltaFitnessEngine(TriangleList<Triangle> triangles, BufferedImage referenceImage) {
        this.imageWidth = referenceImage.getWidth();
        this.imageHeight = referenceImage.getHeight();
        this.totalPixels = imageWidth * imageHeight;
        this.triangleCount = triangles.size();

        int[] refPixels = new int[totalPixels];
        referenceImage.getRGB(0, 0, imageWidth, imageHeight, refPixels, 0, imageWidth);

        refR = new int[totalPixels];
        refG = new int[totalPixels];
        refB = new int[totalPixels];
        for (int i = 0; i < totalPixels; i++) {
            refR[i] = (refPixels[i] >> 16) & 0xff;
            refG[i] = (refPixels[i] >> 8) & 0xff;
            refB[i] = refPixels[i] & 0xff;
        }

        trianglePixelIndices = buildTriangleMasks(triangles);

        currentColorR = new int[triangleCount];
        currentColorG = new int[triangleCount];
        currentColorB = new int[triangleCount];
        currentPalleteColors = new PalleteColor[triangleCount];
        for (int t = 0; t < triangleCount; t++) {
            Color c = triangles.get(t).getColor();
            if (c != null) {
                currentColorR[t] = c.getRed();
                currentColorG[t] = c.getGreen();
                currentColorB[t] = c.getBlue();
            }
            currentPalleteColors[t] = triangles.get(t).getPalleteColor();
        }

        totalDiff = computeFullDiff();
    }

    /**
     * Builds all triangle masks in a single render pass by encoding each
     * triangle's index into its fill color (index+1, so 0 = background).
     */
    private int[][] buildTriangleMasks(TriangleList<Triangle> triangles) {
        BufferedImage idImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = idImage.createGraphics();
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, imageWidth, imageHeight);

        for (int t = 0; t < triangleCount; t++) {
            int encoded = t + 1;
            g2.setColor(new Color((encoded >> 16) & 0xff, (encoded >> 8) & 0xff, encoded & 0xff));
            g2.fillPolygon(triangles.get(t));
        }
        g2.dispose();

        int[] idPixels = new int[totalPixels];
        idImage.getRGB(0, 0, imageWidth, imageHeight, idPixels, 0, imageWidth);

        int[] counts = new int[triangleCount];
        for (int i = 0; i < totalPixels; i++) {
            int id = (idPixels[i] & 0xFFFFFF);
            if (id > 0 && id <= triangleCount) {
                counts[id - 1]++;
            }
        }

        int[][] masks = new int[triangleCount][];
        for (int t = 0; t < triangleCount; t++) {
            masks[t] = new int[counts[t]];
            counts[t] = 0;
        }

        for (int i = 0; i < totalPixels; i++) {
            int id = (idPixels[i] & 0xFFFFFF);
            if (id > 0 && id <= triangleCount) {
                int t = id - 1;
                masks[t][counts[t]++] = i;
            }
        }

        return masks;
    }

    /**
     * Computes the full diff matching AbstractEvolver.compare() exactly.
     * Background pixels (not in any triangle) are black (0,0,0) so they
     * contribute |refR|+|refG|+|refB| each.
     */
    private long computeFullDiff() {
        long backgroundDiff = 0;
        for (int i = 0; i < totalPixels; i++) {
            backgroundDiff += refR[i] + refG[i] + refB[i];
        }

        long triangleAdjustment = 0;
        for (int t = 0; t < triangleCount; t++) {
            int cr = currentColorR[t], cg = currentColorG[t], cb = currentColorB[t];
            for (int px : trianglePixelIndices[t]) {
                int bgContrib = refR[px] + refG[px] + refB[px];
                int colorContrib = Math.abs(refR[px] - cr) + Math.abs(refG[px] - cg) + Math.abs(refB[px] - cb);
                triangleAdjustment += colorContrib - bgContrib;
            }
        }

        return backgroundDiff + triangleAdjustment;
    }

    /**
     * Computes the change in total diff if colors at triangle indices i and j
     * are swapped. Returns negative value if the swap improves fitness.
     * Does NOT modify state — call applySwap() to commit.
     */
    public long computeSwapDelta(int triA, int triB) {
        int aR = currentColorR[triA], aG = currentColorG[triA], aB = currentColorB[triA];
        int bR = currentColorR[triB], bG = currentColorG[triB], bB = currentColorB[triB];

        if (aR == bR && aG == bG && aB == bB) return 0;

        long delta = 0;

        for (int px : trianglePixelIndices[triA]) {
            int rr = refR[px], rg = refG[px], rb = refB[px];
            delta -= Math.abs(rr - aR) + Math.abs(rg - aG) + Math.abs(rb - aB);
            delta += Math.abs(rr - bR) + Math.abs(rg - bG) + Math.abs(rb - bB);
        }

        for (int px : trianglePixelIndices[triB]) {
            int rr = refR[px], rg = refG[px], rb = refB[px];
            delta -= Math.abs(rr - bR) + Math.abs(rg - bG) + Math.abs(rb - bB);
            delta += Math.abs(rr - aR) + Math.abs(rg - aG) + Math.abs(rb - aB);
        }

        return delta;
    }

    /**
     * Commits a swap: updates internal color tracking and totalDiff.
     */
    public void applySwap(int triA, int triB) {
        long delta = computeSwapDelta(triA, triB);
        totalDiff += delta;

        int tmpR = currentColorR[triA], tmpG = currentColorG[triA], tmpB = currentColorB[triA];
        currentColorR[triA] = currentColorR[triB];
        currentColorG[triA] = currentColorG[triB];
        currentColorB[triA] = currentColorB[triB];
        currentColorR[triB] = tmpR;
        currentColorG[triB] = tmpG;
        currentColorB[triB] = tmpB;

        PalleteColor tmpPc = currentPalleteColors[triA];
        currentPalleteColors[triA] = currentPalleteColors[triB];
        currentPalleteColors[triB] = tmpPc;
    }

    /**
     * Commits a swap using a pre-computed delta (avoids recomputing).
     */
    public void applySwapWithDelta(int triA, int triB, long delta) {
        totalDiff += delta;

        int tmpR = currentColorR[triA], tmpG = currentColorG[triA], tmpB = currentColorB[triA];
        currentColorR[triA] = currentColorR[triB];
        currentColorG[triA] = currentColorG[triB];
        currentColorB[triA] = currentColorB[triB];
        currentColorR[triB] = tmpR;
        currentColorG[triB] = tmpG;
        currentColorB[triB] = tmpB;

        PalleteColor tmpPc = currentPalleteColors[triA];
        currentPalleteColors[triA] = currentPalleteColors[triB];
        currentPalleteColors[triB] = tmpPc;
    }

    /**
     * Tries a swap: if it improves fitness (delta < 0), commits it and returns true.
     */
    public boolean trySwap(int triA, int triB) {
        long delta = computeSwapDelta(triA, triB);
        if (delta < 0) {
            applySwapWithDelta(triA, triB, delta);
            return true;
        }
        return false;
    }

    public long getTotalDiff() {
        return totalDiff;
    }

    /**
     * Converts totalDiff to the same 0..1 score as AbstractEvolver.compare().
     */
    public double getScore() {
        return 1.0 - ((double) totalDiff / ((double) totalPixels * 3.0 * 255.0));
    }

    /**
     * Syncs the engine's color state from a TriangleList (e.g., after external mutation).
     * Also recomputes totalDiff from scratch.
     */
    public void syncFromTriangles(TriangleList<Triangle> triangles) {
        for (int t = 0; t < triangleCount; t++) {
            Color c = triangles.get(t).getColor();
            if (c != null) {
                currentColorR[t] = c.getRed();
                currentColorG[t] = c.getGreen();
                currentColorB[t] = c.getBlue();
            }
            currentPalleteColors[t] = triangles.get(t).getPalleteColor();
        }
        totalDiff = computeFullDiff();
    }

    public int getTriangleCount() {
        return triangleCount;
    }

    public int getPixelCount(int triangleIndex) {
        return trianglePixelIndices[triangleIndex].length;
    }

    public int getImageWidth() {
        return imageWidth;
    }

    public int getImageHeight() {
        return imageHeight;
    }

    /**
     * Returns the absolute pixel error for a single triangle (how badly it matches the reference).
     * Higher = worse match.
     */
    public long getTriangleError(int triIdx) {
        int cr = currentColorR[triIdx], cg = currentColorG[triIdx], cb = currentColorB[triIdx];
        long err = 0;
        for (int px : trianglePixelIndices[triIdx]) {
            err += Math.abs(refR[px] - cr) + Math.abs(refG[px] - cg) + Math.abs(refB[px] - cb);
        }
        return err;
    }

    public int[] getTrianglePixelIndices(int triIdx) {
        return trianglePixelIndices[triIdx];
    }

    public int[] getRefR() { return refR; }
    public int[] getRefG() { return refG; }
    public int[] getRefB() { return refB; }

    public int getCurrentColorR(int triIdx) {
        return currentColorR[triIdx];
    }

    public int getCurrentColorG(int triIdx) {
        return currentColorG[triIdx];
    }

    public int getCurrentColorB(int triIdx) {
        return currentColorB[triIdx];
    }

    public PalleteColor getPalleteColor(int triIdx) {
        return currentPalleteColors[triIdx];
    }
}
