//package com.rndmodgames.evolver;
//
//import java.awt.Color;
//import java.awt.Graphics;
//import java.awt.Image;
//import java.awt.image.BufferedImage;
//import java.util.Collections;
//import java.util.List;
//
//import com.rndmodgames.evolver.ImageEvolver.TrianglesComparator;
//
//public class GreedyEvolver extends AbstractEvolver {
//
//	private BufferedImage resizedOriginal;
//	private BufferedImage currentImage;
//	private BufferedImage bestImage;
//	private double bestScore;
//	
//	private int population;
//	private int randomJumpDistance;
//	private int crossoverMax;
//	private int totalIterations;
//	private int goodIterations;
//	
//	private float scale;
//	private float height;
//	private float width;
//	
//	private Pallete pallete;
//	
//	private int triangleHeight;
//	private int triangleWidth;
//	
//	private List<TriangleList<Triangle>> pop = new TriangleList<TriangleList<Triangle>>();
//	
//	private boolean isDirty = true;
//	
//	// greedy
//	// current position
//	// current round
//	// best scores
//
//	private int current = 0;
//	
//	public GreedyEvolver (int population, int randomJumpDistance, int crossoverMax, float scale, Pallete pallete, float width, float height, int triangleWidth, int triangleHeight) {
//		
//		this.population = population;
//		this.scale = scale * pallete.totalPalletes / 2;
//		this.pallete = pallete;
//		this.height = height;
//		this.width = width;
//		this.randomJumpDistance = randomJumpDistance;
//		this.crossoverMax = crossoverMax;
//		
//		this.triangleHeight = triangleHeight;
//		this.triangleWidth = triangleWidth;
//	}
//	
//	/**
//	 * Initialize the triangles as equilateral
//	 */
//	public void initializeEquilateral() {
//
//		int preGenerations = 1;
//		int randomMult = 1;
//		
//		for (int kk = 0; kk < preGenerations; kk++){
//			for (int i = 0; i < population; i++){
//				TriangleList <Triangle> triangles = new TriangleList<Triangle>();
//				int count = 0;
//				int position = 0;
//				
//				for (int a = 0; a < triangleWidth; a++){
//		    		for (int b = 0; b < triangleHeight; b++){
//		    			
//		    			int xPoly [] = new int [3];
//		    			int yPoly [] = new int [3];
//		    			
//		    			// reset triangle position
//		    			if (position == 2) {
//		    				position = 0;
//		    			}
//
//		    			if (position == 0) {
//		    				if (a % 2 == 0) {
//		    					// NORTH
//		    					xPoly[0] = (int) (width * scale * a);
//								xPoly[1] = (int) ((width * scale * a) + (width * scale * 2));
//								xPoly[2] = (int) ((width * scale * a) + (width * scale));
//								
//								yPoly[0] = (int) ((height * scale * b));
//								yPoly[1] = (int) ((height * scale * b));
//								yPoly[2] = (int) ((height * scale * b) + (height * scale));
//		    				} else {
//		    					// SOUTH
//		    					xPoly[0] = (int) ((width * scale * a) - (width * scale));
//								xPoly[1] = (int) ((width * scale * a));
//								xPoly[2] = (int) ((width * scale * a) + (width * scale));
//								
//								yPoly[0] = (int) ((height * scale * b) + (height * scale));
//								yPoly[1] = (int) ((height * scale * b) );
//								yPoly[2] = (int) ((height * scale * b) + (height * scale));
//		    				}
//		    			} else if (position == 1) {
//		    				if (a % 2 == 0) {
//		    					// EAST
//			    				xPoly[0] = (int) ((width * scale * a) + (width * scale * 2));
//								xPoly[1] = (int) ((width * scale * a) + (width * scale));
//								xPoly[2] = (int) ((width * scale * a) + (width * scale * 2));
//								
//								yPoly[0] = (int) ((height * scale * b) - (height * scale));
//								yPoly[1] = (int) ((height * scale * b));
//								yPoly[2] = (int) ((height * scale * b) + (height * scale));
//		    				} else {
//		    					// WEST
//		    					xPoly[0] = (int) ((width * scale * a) - (width * scale));
//								xPoly[1] = (int) ((width * scale * a));
//								xPoly[2] = (int) ((width * scale * a) - (width * scale));
//								
//		    					yPoly[0] = (int) ((height * scale * b));
//								yPoly[1] = (int) ((height * scale * b) - (height * scale));
//								yPoly[2] = (int) ((height * scale * b) - (height * scale * 2));
//		    				}
//		    			}
//		    			
//		    			position++;
//	
//		    			Color color = null;
//		    			if (pallete.getColor(count) != null){
//		    				color = pallete.getColor(count).color;
//		    			}
//	
//		        		Triangle triangle = new Triangle(xPoly, yPoly, 3, null);
//		        		triangles.add(triangle);
//		        		
//		        		count++;
//		    		}
//				}
//				
//				// randomize
////				for (int k = 0; k < triangles.size() * randomMult; k++){
////					switchColor(triangles, roll(triangles.size()), roll(triangles.size()));
////				}
//	
//				pop.add(triangles);
//			}
//			
////			BufferedImage imgParentA = null;
////        	Graphics g = null;
////        	double scoreA = 0d;
////        	
////        	for (TriangleList<Triangle> triangles : pop) {
////        		
////        		imgParentA = new BufferedImage(resizedOriginal.getWidth(), resizedOriginal.getHeight(), ArtEvolver.IMAGE_TYPE); 
////    			g = imgParentA.getGraphics();
////    			
////        		for (Triangle triangle : triangles){
////    				if (triangle.getColor() != null) {
////    					g.setColor(triangle.getColor());
////    					g.drawPolygon(triangle);
////    					g.fillPolygon(triangle);
////    				} else {
////    					g.setColor(Color.BLUE); // TODO: parametrize background color for Greedy Evolver
////    					g.drawPolygon(triangle);
////    					g.fillPolygon(triangle);
////    				}
////        		}
////        		
////        		scoreA = compare(imgParentA, resizedOriginal);
////        		triangles.setScore(scoreA);
////        	}
//        	
////        	bestImage=imgParentA;
////        	g.dispose();
//		}
//
//		System.out.println("total pixels is " + pop.get(0).size());
//		
//		// keep only defined population
//    	pop = pop.subList(0, population);
//	}
//	
//	int currentTriangle = 0;
//	int currentColorPosition = 0;
//	int bestColorPosition = 0;
//	
//	@Override
//	public void evolve(long start, int iterations) {
//
//		BufferedImage imgParentA = null;
//    	Graphics g = null;
//    	double scoreA = 0d;
//
//    	imgParentA = new BufferedImage(resizedOriginal.getWidth(), resizedOriginal.getHeight(), ArtEvolver.IMAGE_TYPE); 
//		g = imgParentA.getGraphics();
//    	
//    	/**
//    	 * Very inefficient double nested iterator
//    	 */
//    	for (TriangleList<Triangle> triangles : pop) {
//    		
//    		Triangle triangle = triangles.get(currentTriangle);
//    		
//			triangle.setColor(pallete.getColor(currentColorPosition).getColor());
//			g.setColor(triangle.getColor());
//			g.drawPolygon(triangle);
//			g.fillPolygon(triangle);
//    		
//    		scoreA = compare(imgParentA, resizedOriginal);
//    		
//    		if (scoreA > bestScore) {
//    			
//    			System.out.println("current bestScore: " + scoreA);
//    			
//    			goodIterations++;
//    			bestScore = scoreA;
//    			bestImage = imgParentA;
//    			this.isDirty = true;
//    		}
//    		
////    		pallete.removeColor(currentColorPosition);
//    		currentTriangle++;
//    		currentColorPosition++;
//    		
//    		if (currentTriangle > triangles.size() - 1) {
//    			currentTriangle = 0;
//    		}
//    		
//    		if (currentColorPosition > triangles.size() - 1) {
//    			currentColorPosition = 0;
//    		}
//
//    		triangles.setScore(scoreA);
//    	}
//
//    	g.dispose();
//    	
//		current++;
//		totalIterations++;
//
//		if (totalIterations % 1000 == 0){
//			
//			long now = System.currentTimeMillis();
//			System.out.println("i: " + totalIterations
//							 + " - good: " + goodIterations
//							 + " - p: " + pop.size()
//							 + " - jump: " + randomJumpDistance
//							 + " - cross: " + crossoverMax
//							 + " - best: " + bestScore
//							 + " - total time: " + ((float) (now - start) / 1000f) + " seconds");
//			
////			System.out.println(bestScore);
//		}
//	}
//
//	public boolean isDirty() {
//		return isDirty;
//	}
//
//	public int getTotalIterations() {
//		return totalIterations;
//	}
//
//	public double getBestScore() {
//		return bestScore;
//	}
//
//	public int getGoodIterations() {
//		return goodIterations;
//	}
//
//	public void setDirty(boolean isDirty) {
//		this.isDirty = isDirty;
//	}
//
//	public Image getBestImage() {
//		return bestImage;
//	}
//
//	public void setResizedOriginal(BufferedImage resizedOriginal) {
//		this.resizedOriginal = resizedOriginal;
//	}
//
//	public void setCurrentImage(BufferedImage currentImage) {
//		this.currentImage = currentImage;
//	}
//
//	public Image getResizedOriginal() {
//		return this.resizedOriginal;
//	}
//}