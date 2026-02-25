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
 * Core mechanic: each click attempts a SINGLE random two-triangle color swap.
 * The swap may improve or worsen fitness — there is no guaranteed success.
 * Upgrades unlock retry cycles (try N, keep best), multi-swap, distance control,
 * and smart targeting — all starting from a pure-random baseline.
 *
 * Always starts with RANDOM (shuffled) initialization for low starting fitness.
 * Smart/LAP initialization is a prestige unlock.
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
    private long successfulSwaps = 0;
    private long totalClicks = 0;
    private boolean initialized = false;

    private int imageWidth;
    private int imageHeight;

    /**
     * Initializes the clicker engine with RANDOM (shuffled) colors for low starting fitness.
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
        boolean savedShuffle = ImageEvolver.SHUFFLE_PALETTE;
        try {
            ImageEvolver.INITIALIZATION_METHOD = initMethod;
            ImageEvolver.SMART_INITIALIZATION = (initMethod == ImageEvolver.INIT_SMART);
            // For random init (method 0), force palette shuffle for truly random low fitness
            ImageEvolver.SHUFFLE_PALETTE = (initMethod == ImageEvolver.INIT_RANDOM);

            ImageEvolver evolver = new ImageEvolver(
                    1, gridW, 2, scale, palette,
                    triWidth, triHeight, gridW, gridH);
            evolver.setResizedOriginal(sourceImage);
            evolver.initializeIsosceles();

            this.triangles = evolver.getPopulation().get(0);
        } finally {
            ImageEvolver.INITIALIZATION_METHOD = savedMethod;
            ImageEvolver.SMART_INITIALIZATION = savedSmart;
            ImageEvolver.SHUFFLE_PALETTE = savedShuffle;
        }

        this.deltaEngine = new DeltaFitnessEngine(triangles, sourceImage);
        this.totalSwaps = 0;
        this.successfulSwaps = 0;
        this.totalClicks = 0;
        this.imageDirty = true;
        this.cachedJpeg = null;
        this.jpegVersion = 0;
        this.initialized = true;

        System.out.println("[ClickerEngine] Initialized: " + triangles.size()
                + " triangles, init=" + initMethod
                + ", fitness=" + String.format("%.4f%%", deltaEngine.getScore() * 100));
    }

    /**
     * Core click mechanic. Each click:
     * - Picks random triangle pairs
     * - With retryCycles=1: tries ONE random swap, applies only if it improves
     * - With retryCycles>1: tries N random swaps, applies the best improving one (if any)
     * - swapDistance limits how far apart the two triangles can be (0 = unlimited)
     * - swapsPerClick controls how many swap operations per click (base 1)
     * - smartPct: probability of targeting worst triangle as candidate A
     *
     * @return result indicating what happened (success/fail, fitness change)
     */
    public synchronized ClickResult performClick(int swapsPerClick, int retryCycles,
                                                  int swapDistance, double smartPct) {
        if (!initialized) return ClickResult.EMPTY;

        int n = triangles.size();
        if (n < 2) return ClickResult.EMPTY;

        double oldScore = deltaEngine.getScore();
        int applied = 0;

        for (int s = 0; s < swapsPerClick; s++) {
            boolean success = attemptSwap(n, retryCycles, swapDistance, smartPct);
            if (success) applied++;
        }

        if (applied > 0) {
            imageDirty = true;
            successfulSwaps += applied;
        }
        totalSwaps += swapsPerClick;
        totalClicks++;

        double newScore = deltaEngine.getScore();
        return new ClickResult(applied, swapsPerClick, newScore - oldScore, newScore);
    }

    /**
     * Single swap attempt with optional retry cycles.
     * With 1 cycle: pick a random pair, apply only if improving.
     * With N cycles: pick N random pairs, apply the most improving one (if any improves).
     */
    private boolean attemptSwap(int n, int retryCycles, int swapDistance, double smartPct) {
        int bestA = -1, bestB = -1;
        long bestDelta = 0;

        for (int cycle = 0; cycle < Math.max(1, retryCycles); cycle++) {
            int a = pickTriangleA(n, smartPct);
            int b = pickTriangleB(n, a, swapDistance);

            long delta = deltaEngine.computeSwapDelta(a, b);
            if (delta < bestDelta) {
                bestDelta = delta;
                bestA = a;
                bestB = b;
            }
        }

        if (bestDelta < 0 && bestA >= 0) {
            deltaEngine.applySwapWithDelta(bestA, bestB, bestDelta);
            syncTriangleColors(bestA, bestB);
            return true;
        }
        return false;
    }

    private int pickTriangleA(int n, double smartPct) {
        if (smartPct > 0 && rng.nextDouble() < smartPct) {
            return sampleWorstTriangle(n);
        }
        return rng.nextInt(n);
    }

    /**
     * Picks triangle B. swapDistance=0 means unlimited (any triangle).
     * swapDistance>0 limits B to be within that index range of A.
     */
    private int pickTriangleB(int n, int a, int swapDistance) {
        if (swapDistance > 0 && swapDistance < n) {
            int offset = rng.nextInt(1, swapDistance + 1);
            int b = (a + (rng.nextBoolean() ? offset : -offset) + n) % n;
            return b == a ? (a + 1) % n : b;
        }
        int b = rng.nextInt(n);
        while (b == a) b = rng.nextInt(n);
        return b;
    }

    private int sampleWorstTriangle(int n) {
        int sampleSize = Math.min(30, n);
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
    public synchronized long getSuccessfulSwaps() { return successfulSwaps; }
    public synchronized long getTotalClicks() { return totalClicks; }
    public synchronized int getTriangleCount() { return initialized ? triangles.size() : 0; }
    public synchronized long getJpegVersion() { return jpegVersion; }
    public boolean isInitialized() { return initialized; }

    public BufferedImage getReferenceImage() { return referenceImage; }

    /**
     * Result of a single click. Tracks both attempted and successful swaps.
     */
    public static class ClickResult {
        public static final ClickResult EMPTY = new ClickResult(0, 0, 0, 0);

        public final int successCount;
        public final int attemptCount;
        public final double fitnessGain;
        public final double newFitness;

        public ClickResult(int successCount, int attemptCount, double fitnessGain, double newFitness) {
            this.successCount = successCount;
            this.attemptCount = attemptCount;
            this.fitnessGain = fitnessGain;
            this.newFitness = newFitness;
        }
    }
}
