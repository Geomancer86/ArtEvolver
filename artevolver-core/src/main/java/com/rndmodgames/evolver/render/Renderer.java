package com.rndmodgames.evolver.render;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

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
		
		// TODO: parametrize image mode
		BufferedImage export = new BufferedImage((int) (width * scale), (int) (height * scale), imageType);
		
		Graphics g = export.getGraphics();
		Graphics2D g2d = (Graphics2D) g;
		
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				   		     RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		
		/**
		 * ANTIALIASING doesn't look good at 480x312
		 */
//		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
//							  RenderingHints.VALUE_ANTIALIAS_ON);
		
		AffineTransform tx = new AffineTransform();
	    tx.scale(scale, scale);
		
		for (Triangle triangle : drawing) {
			
			g2d.setTransform(tx);
			g2d.setColor(triangle.getColor());
			g2d.drawPolygon(triangle);
			g2d.fillPolygon(triangle);
		}
		
		try {
		    
		    System.out.println("width : " + width);
		    System.out.println("height: " + height);
		    
		    //
		    System.out.println("target: " + folder + sourceName + "_" + order + ".png");
		    
		    // 
            ImageIO.write(export, "png", new File(folder + sourceName + "_" + order + ".png"));

	    } catch (IOException e) {
	        
            e.printStackTrace();
	    }
		
		//
		g2d.dispose();
		g.dispose();
	}
}
