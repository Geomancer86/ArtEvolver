package com.rndmodgames.evolver.clicker;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.SplittableRandom;

/**
 * 2D pixel grid for retro console mode. Each cell holds a color index
 * into the palette. The grid is a flat array in row-major order.
 *
 * Initialization uses the permutation approach: compute the ideal histogram
 * (nearest palette color for each source pixel), then randomly shuffle the
 * color tokens across the grid. Evolution swaps pixels to sort them.
 */
public class PixelGrid {

    private final int width;
    private final int height;
    private final int totalPixels;
    private final int[] colorIndices;
    private final Color[] palette;
    private final int paletteSize;

    public PixelGrid(int width, int height, Color[] palette) {
        this.width = width;
        this.height = height;
        this.totalPixels = width * height;
        this.colorIndices = new int[totalPixels];
        this.palette = palette;
        this.paletteSize = palette.length;
    }

    /**
     * Quantizes an image to the nearest palette colors and returns the result
     * at native resolution. Useful for previews without starting a game.
     */
    public static BufferedImage quantize(BufferedImage source, Color[] palette, int targetW, int targetH) {
        BufferedImage resized = source;
        if (source.getWidth() != targetW || source.getHeight() != targetH) {
            resized = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resized.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                               RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(source, 0, 0, targetW, targetH, null);
            g.dispose();
        }

        int total = targetW * targetH;
        int[] srcPixels = new int[total];
        resized.getRGB(0, 0, targetW, targetH, srcPixels, 0, targetW);

        int palSize = palette.length;
        int[] palR = new int[palSize], palG = new int[palSize], palB = new int[palSize];
        for (int c = 0; c < palSize; c++) {
            palR[c] = palette[c].getRed();
            palG[c] = palette[c].getGreen();
            palB[c] = palette[c].getBlue();
        }

        int[] outPixels = new int[total];
        for (int i = 0; i < total; i++) {
            int r = (srcPixels[i] >> 16) & 0xff;
            int g = (srcPixels[i] >> 8) & 0xff;
            int b = srcPixels[i] & 0xff;
            int bestIdx = 0, bestDist = Integer.MAX_VALUE;
            for (int c = 0; c < palSize; c++) {
                int dist = Math.abs(r - palR[c]) + Math.abs(g - palG[c]) + Math.abs(b - palB[c]);
                if (dist < bestDist) { bestDist = dist; bestIdx = c; }
            }
            outPixels[i] = palette[bestIdx].getRGB();
        }

        BufferedImage out = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
        out.setRGB(0, 0, targetW, targetH, outPixels, 0, targetW);
        return out;
    }

    /**
     * Quantize and scale up for display. Uses the same scale factor as renderScaled().
     */
    public static BufferedImage quantizeScaled(BufferedImage source, Color[] palette, int targetW, int targetH) {
        BufferedImage native_ = quantize(source, palette, targetW, targetH);
        int scale = Math.max(1, 640 / targetW);
        if (scale <= 1) return native_;

        int sw = targetW * scale, sh = targetH * scale;
        BufferedImage img = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                           RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(native_, 0, 0, sw, sh, null);
        g.dispose();
        return img;
    }

    /**
     * Permutation init: compute nearest-color histogram from reference,
     * build the token pool, then shuffle it across the grid.
     */
    public void initFromReference(BufferedImage reference) {
        int[] refPixels = new int[totalPixels];
        reference.getRGB(0, 0, width, height, refPixels, 0, width);

        int[] histogram = new int[paletteSize];

        int[] palR = new int[paletteSize];
        int[] palG = new int[paletteSize];
        int[] palB = new int[paletteSize];
        for (int c = 0; c < paletteSize; c++) {
            palR[c] = palette[c].getRed();
            palG[c] = palette[c].getGreen();
            palB[c] = palette[c].getBlue();
        }

        for (int i = 0; i < totalPixels; i++) {
            int r = (refPixels[i] >> 16) & 0xff;
            int g = (refPixels[i] >> 8) & 0xff;
            int b = refPixels[i] & 0xff;

            int bestIdx = 0;
            int bestDist = Integer.MAX_VALUE;
            for (int c = 0; c < paletteSize; c++) {
                int dist = Math.abs(r - palR[c]) + Math.abs(g - palG[c]) + Math.abs(b - palB[c]);
                if (dist < bestDist) {
                    bestDist = dist;
                    bestIdx = c;
                }
            }
            histogram[bestIdx]++;
        }

        int tokenIdx = 0;
        for (int c = 0; c < paletteSize; c++) {
            for (int count = 0; count < histogram[c]; count++) {
                colorIndices[tokenIdx++] = c;
            }
        }

        SplittableRandom rng = new SplittableRandom();
        for (int i = totalPixels - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int tmp = colorIndices[i];
            colorIndices[i] = colorIndices[j];
            colorIndices[j] = tmp;
        }
    }

    public void swap(int pixelA, int pixelB) {
        int tmp = colorIndices[pixelA];
        colorIndices[pixelA] = colorIndices[pixelB];
        colorIndices[pixelB] = tmp;
    }

    public int getColorIndex(int pixel) { return colorIndices[pixel]; }
    public Color getColor(int pixel) { return palette[colorIndices[pixel]]; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getTotalPixels() { return totalPixels; }
    public Color[] getPalette() { return palette; }
    public int getPaletteSize() { return paletteSize; }
    public int[] getColorIndices() { return colorIndices; }

    /**
     * Renders the current grid state into a BufferedImage at native resolution.
     */
    public BufferedImage render() {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int[] pixels = new int[totalPixels];
        for (int i = 0; i < totalPixels; i++) {
            pixels[i] = palette[colorIndices[i]].getRGB();
        }
        img.setRGB(0, 0, width, height, pixels, 0, width);
        return img;
    }

    /**
     * Renders at integer scale (nearest-neighbor) so each retro pixel becomes
     * a visible block. Scale is chosen so the output is ~600-800px wide.
     */
    public BufferedImage renderScaled() {
        int scale = Math.max(1, 640 / width);
        if (scale <= 1) return render();

        int sw = width * scale;
        int sh = height * scale;
        BufferedImage img = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_RGB);
        int[] scaledPixels = new int[sw * sh];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = palette[colorIndices[y * width + x]].getRGB();
                for (int dy = 0; dy < scale; dy++) {
                    int row = (y * scale + dy) * sw;
                    for (int dx = 0; dx < scale; dx++) {
                        scaledPixels[row + x * scale + dx] = rgb;
                    }
                }
            }
        }
        img.setRGB(0, 0, sw, sh, scaledPixels, 0, sw);
        return img;
    }

    /**
     * Renders into an existing image buffer (avoids allocation).
     */
    public void renderInto(int[] pixelBuffer) {
        for (int i = 0; i < totalPixels; i++) {
            pixelBuffer[i] = palette[colorIndices[i]].getRGB();
        }
    }
}
