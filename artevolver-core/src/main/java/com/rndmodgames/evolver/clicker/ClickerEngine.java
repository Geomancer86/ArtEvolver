package com.rndmodgames.evolver.clicker;

import com.rndmodgames.evolver.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.SplittableRandom;

/**
 * Lightweight evolution engine for the Clicker game.
 *
 * Each "click" finds and applies one or more improving two-triangle color swaps
 * using the DeltaFitnessEngine (O(pixels_per_triangle) per evaluation).
 * No tournament, no population — just hill-climbing via user clicks and auto-clickers.
 */
public class ClickerEngine {

    private static final int IMAGE_TYPE = BufferedImage.TYPE_INT_ARGB;

    private final SplittableRandom rng = new SplittableRandom();

    private TriangleList<Triangle> triangles;
    private DeltaFitnessEngine deltaEngine;
    private BufferedImage referenceImage;

    private BufferedImage renderedImage;
    private byte[] cachedJpeg;
    private boolean imageDirty = true;
    private long jpegVersion = 0;

    private long totalSwaps = 0;
    private long totalClicks = 0;
    private boolean initialized = false;

    private int imageWidth;
    private int imageHeight;

    /**
     * Initializes the clicker engine by creating triangle geometry via ImageEvolver,
     * then setting up a DeltaFitnessEngine for fast swap evaluation.
     *
     * @param sourceImage  The resized source/reference image
     * @param palette      The color palette
     * @param gridW        Triangle grid width (e.g. 80)
     * @param gridH        Triangle grid height (e.g. 53)
     * @param triWidth     Base triangle width in pixels
     * @param triHeight    Base triangle height in pixels
     * @param scale        Triangle scale factor
     * @param initMethod   0=Random, 1=Smart, 2=LAP
     */
    public synchronized void init(BufferedImage sourceImage, Palette palette,
                                  int gridW, int gridH,
                                  float triWidth, float triHeight, float scale,
                                  int initMethod) {
        this.referenceImage = sourceImage;
        this.imageWidth = sourceImage.getWidth();
        this.imageHeight = sourceImage.getHeight();

        int savedMethod = ImageEvolver.INITIALIZATION_METHOD;
        boolean savedSmart = ImageEvolver.SMART_INITIALIZATION;
        try {
            ImageEvolver.INITIALIZATION_METHOD = initMethod;
            ImageEvolver.SMART_INITIALIZATION = (initMethod == ImageEvolver.INIT_SMART);

            ImageEvolver evolver = new ImageEvolver(
                    1, gridW, 2, scale, palette,
                    triWidth, triHeight, gridW, gridH);
            evolver.setResizedOriginal(sourceImage);
            evolver.initializeIsosceles();

            this.triangles = evolver.getPopulation().get(0);
        } finally {
            ImageEvolver.INITIALIZATION_METHOD = savedMethod;
            ImageEvolver.SMART_INITIALIZATION = savedSmart;
        }

        this.deltaEngine = new DeltaFitnessEngine(triangles, sourceImage);
        this.totalSwaps = 0;
        this.totalClicks = 0;
        this.imageDirty = true;
        this.cachedJpeg = null;
        this.jpegVersion = 0;
        this.initialized = true;

        System.out.println("[ClickerEngine] Initialized: " + triangles.size()
                + " triangles, fitness=" + String.format("%.4f%%", deltaEngine.getScore() * 100));
    }

    /**
     * Performs a single click: finds and applies improving swaps.
     *
     * @param swapsPerClick  Number of improving swaps to find (1 = base)
     * @param maxAttempts    Max random pairs to try per swap slot
     * @param smartPct       0.0-1.0: probability of targeting worst triangle
     * @param localPct       0.0-1.0: probability of preferring nearby triangle
     * @param bestOfN        If > 1, try N candidates and pick the best improving swap
     * @return result with fitness gain, swaps applied, new fitness
     */
    public synchronized ClickResult performClick(int swapsPerClick, int maxAttempts,
                                                  double smartPct, double localPct, int bestOfN) {
        if (!initialized) return ClickResult.EMPTY;

        int n = triangles.size();
        if (n < 2) return ClickResult.EMPTY;

        double oldScore = deltaEngine.getScore();
        int applied = 0;

        for (int s = 0; s < swapsPerClick; s++) {
            boolean found;
            if (bestOfN > 1) {
                found = findBestOfN(n, maxAttempts, bestOfN, smartPct, localPct);
            } else {
                found = findAndApplySwap(n, maxAttempts, smartPct, localPct);
            }
            if (found) {
                applied++;
            } else {
                break;
            }
        }

        if (applied > 0) {
            imageDirty = true;
            totalSwaps += applied;
        }
        totalClicks++;

        double newScore = deltaEngine.getScore();
        return new ClickResult(applied, newScore - oldScore, newScore);
    }

    private boolean findAndApplySwap(int n, int maxAttempts, double smartPct, double localPct) {
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            int a = pickTriangleA(n, smartPct);
            int b = pickTriangleB(n, a, localPct);

            long delta = deltaEngine.computeSwapDelta(a, b);
            if (delta < 0) {
                deltaEngine.applySwapWithDelta(a, b, delta);
                syncTriangleColors(a, b);
                return true;
            }
        }
        return false;
    }

    private boolean findBestOfN(int n, int maxAttempts, int bestOfN, double smartPct, double localPct) {
        int bestA = -1, bestB = -1;
        long bestDelta = 0;

        int attemptsPerCandidate = Math.max(1, maxAttempts / bestOfN);
        for (int candidate = 0; candidate < bestOfN; candidate++) {
            for (int attempt = 0; attempt < attemptsPerCandidate; attempt++) {
                int a = pickTriangleA(n, smartPct);
                int b = pickTriangleB(n, a, localPct);

                long delta = deltaEngine.computeSwapDelta(a, b);
                if (delta < bestDelta) {
                    bestDelta = delta;
                    bestA = a;
                    bestB = b;
                }
            }
        }

        if (bestDelta < 0) {
            deltaEngine.applySwapWithDelta(bestA, bestB, bestDelta);
            syncTriangleColors(bestA, bestB);
            return true;
        }
        return false;
    }

    private int pickTriangleA(int n, double smartPct) {
        if (smartPct > 0 && rng.nextDouble() < smartPct) {
            return findWorstTriangle(n);
        }
        return rng.nextInt(n);
    }

    private int pickTriangleB(int n, int a, double localPct) {
        if (localPct > 0 && rng.nextDouble() < localPct) {
            int jump = rng.nextInt(1, Math.max(2, n / 20));
            int b = (a + (rng.nextBoolean() ? jump : -jump) + n) % n;
            return b == a ? (a + 1) % n : b;
        }
        int b = rng.nextInt(n);
        while (b == a) b = rng.nextInt(n);
        return b;
    }

    private int findWorstTriangle(int n) {
        int sampleSize = Math.min(50, n);
        int worstIdx = rng.nextInt(n);
        long worstError = deltaEngine.getTriangleError(worstIdx);

        for (int i = 1; i < sampleSize; i++) {
            int idx = rng.nextInt(n);
            long err = deltaEngine.getTriangleError(idx);
            if (err > worstError) {
                worstError = err;
                worstIdx = idx;
            }
        }
        return worstIdx;
    }

    private void syncTriangleColors(int a, int b) {
        Triangle triA = triangles.get(a);
        Triangle triB = triangles.get(b);

        Color tmpColor = triA.getColor();
        triA.setColor(triB.getColor());
        triB.setColor(tmpColor);

        PalleteColor tmpPc = triA.getPalleteColor();
        triA.setPalleteColor(triB.getPalleteColor());
        triB.setPalleteColor(tmpPc);
    }

    /**
     * Renders the current triangle state to a BufferedImage.
     */
    public synchronized BufferedImage getRenderedImage() {
        if (!initialized) return null;
        if (imageDirty || renderedImage == null) {
            renderTriangles();
            imageDirty = false;
        }
        return renderedImage;
    }

    private void renderTriangles() {
        if (renderedImage == null
                || renderedImage.getWidth() != imageWidth
                || renderedImage.getHeight() != imageHeight) {
            renderedImage = new BufferedImage(imageWidth, imageHeight, IMAGE_TYPE);
        }

        Graphics2D g = renderedImage.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, imageWidth, imageHeight);

        for (int i = 0, size = triangles.size(); i < size; i++) {
            Triangle tri = triangles.get(i);
            Color c = tri.getColor();
            if (c != null) {
                g.setColor(c);
                g.fillPolygon(tri);
            }
        }
        g.dispose();
        cachedJpeg = null;
    }

    /**
     * Returns the current rendered image as JPEG bytes, cached until image changes.
     */
    public synchronized byte[] getRenderedImageAsJpeg() {
        if (!initialized) return null;
        getRenderedImage();
        if (cachedJpeg == null) {
            try {
                BufferedImage rgb = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = rgb.createGraphics();
                g.drawImage(renderedImage, 0, 0, null);
                g.dispose();

                ByteArrayOutputStream baos = new ByteArrayOutputStream(32768);
                ImageIO.write(rgb, "jpg", baos);
                cachedJpeg = baos.toByteArray();
                jpegVersion++;
            } catch (IOException e) {
                System.err.println("[ClickerEngine] JPEG encoding failed: " + e.getMessage());
                return null;
            }
        }
        return cachedJpeg;
    }

    /**
     * Resets the engine for prestige: re-creates triangle colors with the given init method.
     */
    public synchronized void reset(int initMethod, Palette palette,
                                   int gridW, int gridH,
                                   float triWidth, float triHeight, float scale) {
        if (referenceImage == null) return;
        init(referenceImage, palette, gridW, gridH, triWidth, triHeight, scale, initMethod);
    }

    public synchronized double getFitness() {
        return initialized ? deltaEngine.getScore() : 0;
    }

    public synchronized long getTotalSwaps() { return totalSwaps; }
    public synchronized long getTotalClicks() { return totalClicks; }
    public synchronized int getTriangleCount() { return initialized ? triangles.size() : 0; }
    public synchronized long getJpegVersion() { return jpegVersion; }
    public boolean isInitialized() { return initialized; }

    public BufferedImage getReferenceImage() { return referenceImage; }

    /**
     * Result of a single click operation.
     */
    public static class ClickResult {
        public static final ClickResult EMPTY = new ClickResult(0, 0, 0);

        public final int swapsApplied;
        public final double fitnessGain;
        public final double newFitness;

        public ClickResult(int swapsApplied, double fitnessGain, double newFitness) {
            this.swapsApplied = swapsApplied;
            this.fitnessGain = fitnessGain;
            this.newFitness = newFitness;
        }
    }
}
