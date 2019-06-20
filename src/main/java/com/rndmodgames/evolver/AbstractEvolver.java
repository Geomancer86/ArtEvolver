package com.rndmodgames.evolver;

import java.awt.image.BufferedImage;

public abstract class AbstractEvolver {

	public abstract void evolve(long start, int iterations);
	
	/**
	 * Extracted to avoid recreation
	 */
	private static int rgb1;
	private static int rgb2;
	private static int r1;
	private static int g1;
	private static int b1;
	private static int r2;
	private static int g2;
	private static int b2;
	private static int diff;
	private static double n;
	private static double p;
	
	private static final double CONSTANT_SCORE_DIVIDER = 255d;
	private static final int CONSTANT_SCORE_MULTIPLIER = 3;
	private static final int CONSTANT_SCORE_ONE = 1;

	public double compare(BufferedImage img1, BufferedImage img2) {

//		long compareThen = System.currentTimeMillis();
		
		int width1 = img1.getWidth(null);
		int width2 = img2.getWidth(null);
		int height1 = img1.getHeight(null);
		int height2 = img2.getHeight(null);
		
//		System.out.println("width1: " + width1 + ", height1: " + height1 + ", width2: " + width2 + ", height2: " + height2);
		
		if ((width1 != width2) || (height1 != height2)) {
			System.err.println("Error: Images dimensions mismatch");
			return 0;
		}
		
		boolean fitnessByColor = true;
		diff = 0;
		
		if (fitnessByColor){
			for (int y = 0; y < height1; y++) {
				for (int x = 0; x < width1; x++) {
					rgb1 = img1.getRGB(x, y);
					rgb2 = img2.getRGB(x, y);
					r1 = (rgb1 >> 16) & 0xff;
					g1 = (rgb1 >> 8) & 0xff;
					b1 = (rgb1) & 0xff;
					r2 = (rgb2 >> 16) & 0xff;
					g2 = (rgb2 >> 8) & 0xff;
					b2 = (rgb2) & 0xff;
					diff += Math.abs(r1 - r2);
					diff += Math.abs(g1 - g2);
					diff += Math.abs(b1 - b2);
				}
			}
		}else{
			for (int y = 0; y < height1; y++) {
				for (int x = 0; x < width1; x++) {
					rgb1 = img1.getRGB(x, y);
					rgb2 = img2.getRGB(x, y);
					
					float r1 = ((rgb1 >> 16) & 0xff) * 1; 	// 0.299f
					float g1 = ((rgb1 >> 8) & 0xff) * 0f;	// 0.587f
					float b1 = ((rgb1) & 0xff) * 0f; 		// 0.114f
					
					float r2 = ((rgb2 >> 16) & 0xff) * 1f;
					float g2 = ((rgb2 >> 8) & 0xff) * 0f;
					float b2 = ((rgb2) & 0xff) * 0f;
					
					diff += Math.abs(r1 - r2);
					diff += Math.abs(g1 - g2);
					diff += Math.abs(b1 - b2);
				}
			}
		}

		n = width1 * height1 * CONSTANT_SCORE_MULTIPLIER;
		p = diff / n / CONSTANT_SCORE_DIVIDER;
		
//		long compareNow = System.currentTimeMillis();
//		System.out.println("compare took " + (float)(compareNow - compareThen) / 1000f + " seconds");
		
		return CONSTANT_SCORE_ONE - p;
	}
}
