package com.rndmodgames.evolver.render;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.rndmodgames.evolver.PalleteColor;
import com.rndmodgames.evolver.Triangle;
import com.rndmodgames.evolver.TriangleList;

public class Renderer {

	/**
	 * FFMPEG render:
	 * ./ffmpeg -r 480 -f image2 -s 240x156 -start_number 2 -i D:\Media\ArtEvolver2019\export\%d.png -vframes 13363 -vcodec libx264 -crf 25  -pix_fmt yuv420p campito240x156.mp4
	 * ./ffmpeg -r 480 -f image2 -s 480x312 -start_number 2 -i D:\Media\ArtEvolver2019\export\%d.png -vframes 8900 -vcodec libx264 -crf 10  -pix_fmt yuv420p campito480x312.mp4
	 * ./ffmpeg -r 480 -f image2 -s 720x468 -start_number 4 -i D:\Media\ArtEvolver2019\export\%d.png -vframes 21317 -vcodec libx264 -crf 10  -pix_fmt yuv420p campito720x468.mp4
	 * 
	 * whatsapp video:
	 * 
	 * ./ffmpeg -r 480 -f image2 -s 720x468 -start_number 4 -i D:\Media\ArtEvolver2019\export\%d.png -vframes 21317 -vcodec mpeg4 -b:v 3200k -c:a libmp3lame output.avi
	 * 
	 * 	-i broken.mp4 -c:v libx264 -profile:v baseline -level 3.0 -pix_fmt yuv420p working.mp4
	 *  mpeg4 -b:v 600k -c:a libmp3lame output.avi
	 */
	public static void renderToPNG(TriangleList<Triangle> drawing, String sourceName, String folder, int order, int width, int height, int imageType, float scale) {
		
		BufferedImage export = new BufferedImage((int) (width * scale), (int) (height * scale), imageType);
		
		Graphics g = export.getGraphics();
		Graphics2D g2d = (Graphics2D) g;
		
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				   		     RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		
		AffineTransform tx = new AffineTransform();
	    tx.scale(scale, scale);
		
		for (Triangle triangle : drawing) {
			
			g2d.setTransform(tx);
			g2d.setColor(triangle.getColor());
			g2d.drawPolygon(triangle);
			g2d.fillPolygon(triangle);
		}
		
		try {
            ImageIO.write(export, "png", new File(folder + sourceName + "_" + order + ".png"));
	    } catch (IOException e) {
	    }
		
		g2d.dispose();
		g.dispose();
	}

	/**
	 * Renders a high-resolution paint-by-numbers guide image.
	 * Each triangle is filled with its assigned color, then a second pass
	 * renders the Sherwin-Williams color name centered inside each triangle.
	 * 
	 * The font size scales with the export scale factor so labels remain
	 * readable at any resolution.
	 */
	public static void renderPaintByNumbersPNG(TriangleList<Triangle> drawing, String sourceName, String folder,
	                                            int order, int width, int height, int imageType, float scale) {

		int exportW = (int) (width * scale);
		int exportH = (int) (height * scale);
		BufferedImage export = new BufferedImage(exportW, exportH, imageType);

		Graphics2D g2d = export.createGraphics();

		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				             RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
				             RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		AffineTransform tx = new AffineTransform();
		tx.scale(scale, scale);

		// Pass 1: fill triangles with their colors
		for (Triangle triangle : drawing) {
			g2d.setTransform(tx);
			g2d.setColor(triangle.getColor());
			g2d.drawPolygon(triangle);
			g2d.fillPolygon(triangle);
		}

		// Pass 2: render color name labels
		int baseFontSize = Math.max(2, (int) (3 * scale));
		Font labelFont = new Font("SansSerif", Font.PLAIN, baseFontSize);
		g2d.setFont(labelFont);
		FontMetrics fm = g2d.getFontMetrics();

		g2d.setTransform(tx);

		for (Triangle triangle : drawing) {
			PalleteColor pc = triangle.getPalleteColor();
			if (pc == null || pc.getName() == null) continue;

			int cx = (int) triangle.getBounds().getCenterX();
			int cy = (int) triangle.getBounds().getCenterY();
			if (cx == 0 && cy == 0) continue;

			String name = pc.getName();

			int textW = fm.stringWidth(name);
			int textH = fm.getHeight();

			int drawX = cx - textW / 2;
			int drawY = cy + textH / 4;

			// dark outline for readability on light colors
			g2d.setColor(Color.BLACK);
			g2d.drawString(name, drawX - 1, drawY);
			g2d.drawString(name, drawX + 1, drawY);
			g2d.drawString(name, drawX, drawY - 1);
			g2d.drawString(name, drawX, drawY + 1);

			// white text on top
			g2d.setColor(Color.WHITE);
			g2d.drawString(name, drawX, drawY);
		}

		String filename = folder + sourceName + "_pbn_" + order + ".png";
		try {
			ImageIO.write(export, "png", new File(filename));
			System.out.println("[Renderer] Paint-by-numbers export: " + filename);
		} catch (IOException e) {
			System.err.println("[Renderer] Failed to export: " + filename + " - " + e.getMessage());
		}

		g2d.dispose();
	}

	/**
	 * Renders a clean outline-only guide: white triangles with thin borders
	 * and color names/IDs inside. This produces the actual guide sheet that
	 * can be printed and used for physical tile placement.
	 */
	public static void renderOutlineGuidePNG(TriangleList<Triangle> drawing, String sourceName, String folder,
	                                          int order, int width, int height, int imageType, float scale) {

		int exportW = (int) (width * scale);
		int exportH = (int) (height * scale);
		BufferedImage export = new BufferedImage(exportW, exportH, imageType);

		Graphics2D g2d = export.createGraphics();

		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				             RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
				             RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		AffineTransform tx = new AffineTransform();
		tx.scale(scale, scale);
		g2d.setTransform(tx);

		// White background
		g2d.setColor(Color.WHITE);
		g2d.fillRect(0, 0, width, height);

		// Draw triangle outlines
		g2d.setColor(new Color(180, 180, 180));
		for (Triangle triangle : drawing) {
			g2d.drawPolygon(triangle);
		}

		// Render labels
		int baseFontSize = Math.max(2, (int) (2.5 * scale));
		Font labelFont = new Font("SansSerif", Font.PLAIN, baseFontSize);
		g2d.setFont(labelFont);
		FontMetrics fm = g2d.getFontMetrics();

		g2d.setColor(Color.BLACK);

		for (Triangle triangle : drawing) {
			PalleteColor pc = triangle.getPalleteColor();
			if (pc == null) continue;

			int cx = (int) triangle.getBounds().getCenterX();
			int cy = (int) triangle.getBounds().getCenterY();
			if (cx == 0 && cy == 0) continue;

			String label = pc.getName() != null ? pc.getName() : String.valueOf(pc.getId());

			int textW = fm.stringWidth(label);
			int textH = fm.getHeight();

			g2d.drawString(label, cx - textW / 2, cy + textH / 4);
		}

		String filename = folder + sourceName + "_guide_" + order + ".png";
		try {
			ImageIO.write(export, "png", new File(filename));
			System.out.println("[Renderer] Outline guide export: " + filename);
		} catch (IOException e) {
			System.err.println("[Renderer] Failed to export guide: " + filename + " - " + e.getMessage());
		}

		g2d.dispose();
	}
}
