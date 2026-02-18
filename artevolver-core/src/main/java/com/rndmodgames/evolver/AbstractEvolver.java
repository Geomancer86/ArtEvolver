package com.rndmodgames.evolver;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 * AbstractEvolver v2 — Optimized bulk pixel comparison
 * 
 * @author Geomancer86
 */
public abstract class AbstractEvolver implements Runnable {

    public abstract void evolve(long start, int iterations);

    private static final double CONSTANT_SCORE_DIVIDER = 255d;
    private static final int CONSTANT_SCORE_MULTIPLIER = 3;

    private int[] pixelsBuf1;
    private int[] pixelsBuf2;

    // Pre-cached reference image pixels — extracted once, reused every iteration
    private int[] referencePixels;
    private int refWidth;
    private int refHeight;

    public void cacheReferencePixels(BufferedImage ref) {
        this.refWidth = ref.getWidth();
        this.refHeight = ref.getHeight();
        int totalPixels = refWidth * refHeight;
        this.referencePixels = new int[totalPixels];
        ref.getRGB(0, 0, refWidth, refHeight, this.referencePixels, 0, refWidth);
    }

    public double compare(BufferedImage img1, BufferedImage img2) {
        int w = img1.getWidth();
        int h = img1.getHeight();
        int totalPixels = w * h;

        int[] px1;
        int[] px2;

        // Use pre-cached reference pixels when dimensions match (hot path)
        if (referencePixels != null && img2.getWidth() == refWidth && img2.getHeight() == refHeight) {
            px2 = referencePixels;
        } else {
            if (pixelsBuf2 == null || pixelsBuf2.length < totalPixels) {
                pixelsBuf2 = new int[totalPixels];
            }
            img2.getRGB(0, 0, w, h, pixelsBuf2, 0, w);
            px2 = pixelsBuf2;
        }

        // Direct DataBuffer access for TYPE_INT_ARGB/RGB (zero-copy)
        boolean directAccess = img1.getType() == BufferedImage.TYPE_INT_ARGB
                || img1.getType() == BufferedImage.TYPE_INT_RGB;

        if (directAccess) {
            px1 = ((DataBufferInt) img1.getRaster().getDataBuffer()).getData();
        } else {
            if (pixelsBuf1 == null || pixelsBuf1.length < totalPixels) {
                pixelsBuf1 = new int[totalPixels];
            }
            img1.getRGB(0, 0, w, h, pixelsBuf1, 0, w);
            px1 = pixelsBuf1;
        }

        long diff = 0;
        int rgb1, rgb2;

        for (int i = 0; i < totalPixels; i++) {
            rgb1 = px1[i];
            rgb2 = px2[i];

            diff += Math.abs(((rgb1 >> 16) & 0xff) - ((rgb2 >> 16) & 0xff));
            diff += Math.abs(((rgb1 >>  8) & 0xff) - ((rgb2 >>  8) & 0xff));
            diff += Math.abs(( rgb1        & 0xff) - ( rgb2        & 0xff));
        }

        double n = (double) totalPixels * CONSTANT_SCORE_MULTIPLIER;
        double p = diff / n / CONSTANT_SCORE_DIVIDER;
        return 1.0d - p;
    }
}
