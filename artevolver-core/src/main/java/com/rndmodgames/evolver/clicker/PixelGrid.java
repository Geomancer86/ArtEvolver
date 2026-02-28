package com.rndmodgames.evolver.clicker;

import java.awt.Color;
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
     * Permutation init: compute nearest-color histogram from reference,
     * build the token pool, then shuffle it across the grid.
     */
    public void initFromReference(BufferedImage reference) {
        int[] refPixels = new int[totalPixels];
        reference.getRGB(0, 0, width, height, refPixels, 0, width);

        int[] histogram = new int[paletteSize];
        int[] idealAssignment = new int[totalPixels];

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
            idealAssignment[i] = bestIdx;
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
     * Renders the current grid state into a BufferedImage.
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
     * Renders into an existing image buffer (avoids allocation).
     */
    public void renderInto(int[] pixelBuffer) {
        for (int i = 0; i < totalPixels; i++) {
            pixelBuffer[i] = palette[colorIndices[i]].getRGB();
        }
    }
}
