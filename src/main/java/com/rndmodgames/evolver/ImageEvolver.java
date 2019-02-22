package com.rndmodgames.evolver;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SplittableRandom;

public class ImageEvolver {

//	public static final MersenneTwisterFast random = new MersenneTwisterFast();
	public static final SplittableRandom random = new SplittableRandom();
	public static final boolean KILL_PARENTS = false;
	
	private BufferedImage resizedOriginal;
	private BufferedImage currentImage;
	private BufferedImage bestImage;
	private double bestScore;
	
	private int population;
	private int randomJumpDistance;
	private int crossoverMax;
	private int totalIterations;
	private int goodIterations;
	
	private float scale;
	private float height;
	private float width;
	
	private Pallete pallete;
	
	private static int triangleHeight;
	private static int triangleWidth;
	
	CrossOver crossOver;
	
	// synchronize for multithreading
	private List<TriangleList<Triangle>> pop = new TriangleList<TriangleList<Triangle>>();
	
	private boolean isDirty = true;
	
	@SuppressWarnings("static-access")
	public ImageEvolver(int population, int randomJumpDistance, int crossoverMax, float scale, Pallete pallete, float width, float height, int triangleWidth, int triangleHeight){
		this.population = population;
		this.scale = scale;
		this.pallete = pallete;
		this.height = height;
		this.width = width;
		this.randomJumpDistance = randomJumpDistance;
		this.crossoverMax = crossoverMax;
		
		this.triangleHeight = triangleHeight;
		this.triangleWidth = triangleWidth;
		
		initCrossOver();
	}
	
	public void initCrossOver(){
		crossOver = new CrossOver(randomJumpDistance, crossoverMax);
	}
	
	public void initialize(){
		
		int preGenerations = 1;
		int randomMult = 1;
		
		for (int kk = 0; kk < preGenerations; kk++){
			
//			pallete.randomize();
//			pallete.orderByLuminescence();
//			pallete.orderByBLUE();
			
			for (int i = 0; i < population; i++){
				TriangleList <Triangle> triangles = new TriangleList<Triangle>();
				int count = 0;
				
				for (int a = 0; a < triangleWidth; a++){
		    		for (int b = 0; b < triangleHeight; b++){
		    			int xPoly [] = new int [3];
		    			int yPoly [] = new int [3];
	
		    			if (b % 2 == 0){
							xPoly[0] = (int) (width * scale * a);
							xPoly[1] = (int) ((width * scale * a) + (width * scale));
							xPoly[2] = (int) (width * scale * a);
							
							yPoly[0] = (int) ((height * scale * b));
							yPoly[1] = (int) ((height * scale * b));
							yPoly[2] = (int) ((height * scale * b) + (height * scale));
		    			}else{
		    				xPoly[0] = (int) ((width * scale * a));
							xPoly[1] = (int) ((width * scale * a) + (width * scale));
							xPoly[2] = (int) ((width * scale * a) + (width * scale));
							
							yPoly[0] = (int) ((height * scale * b));
							yPoly[1] = (int) ((height * scale * b));
							yPoly[2] = (int) ((height * scale * b) - (height * scale));
		    			}
		    			
		    			// dynamic row shifting
						yPoly[0] -= (height * scale) * (b / 2);
						yPoly[1] -= (height * scale) * (b / 2);
						yPoly[2] -= (height * scale) * (b / 2);
	
		    			Color color = null;
		    			if (pallete.getColor(count) != null){
		    				color = pallete.getColor(count).color;
		    			}
	
		        		Triangle triangle = new Triangle(xPoly, yPoly, 3, color);
		        		triangles.add(triangle);
		        		
		        		count++;
		        	}
		    	}

				// randomize
				for (int k = 0; k < triangles.size() * randomMult; k++){
					switchColor(triangles, roll(triangles.size()), roll(triangles.size()));
				}
	
				pop.add(triangles);
			}

			BufferedImage imgParentA = null;
        	Graphics g = null;
        	double scoreA = 0d;
        	
        	for (TriangleList<Triangle> triangles : pop) {
        		
        		imgParentA = new BufferedImage(resizedOriginal.getWidth(), resizedOriginal.getHeight(), ArtEvolver.IMAGE_TYPE); 
    			g = imgParentA.getGraphics();
    			
        		for (Triangle triangle : triangles){
    				if (triangle.getColor() != null) {
    					g.setColor(triangle.getColor());
    					g.drawPolygon(triangle);
    					g.fillPolygon(triangle);
    				} else {
    					g.setColor(Color.BLUE);
    					g.drawPolygon(triangle);
    				}
        		}
        		
        		scoreA = compare(imgParentA, resizedOriginal);
        		triangles.setScore(scoreA);
        	}
        	
        	g.dispose();
		}
		
		// Comparator used only once, no need to extract
		Collections.sort(pop, new TrianglesComparator());
		
		// keep only defined population
    	pop = pop.subList(0, population);
	}
	
	@SuppressWarnings("rawtypes")
	public static class TrianglesComparator implements Comparator<TriangleList> {
	    @Override
	    public int compare(TriangleList o1, TriangleList o2) {
	    	return o1.getScore().compareTo(o2.getScore());
	    }
	}
	
	public static void switchColor(List<Triangle> triangles, int a, int b){
		Triangle origin = triangles.get(a);
		Triangle dest = triangles.get(b);
		
		Color aux = origin.getColor();
		origin.setColor(dest.getColor());
		dest.setColor(aux);
	}
	
	public static void switchRandomColor(List<Triangle> triangles){
		Triangle origin = triangles.get(roll(triangles.size()));
		Triangle dest = triangles.get(roll(triangles.size()));
		
		Color aux = origin.getColor();
		origin.setColor(dest.getColor());
		dest.setColor(aux);
	}
	
	// TODO implement randomization
	public static void switchRandomMultiColor(List<Triangle> triangles, int maxTriangles){
		
		int origin = roll(triangles.size());
		int dest = roll(triangles.size());
		
		if (origin >= triangles.size() - 1){
			origin = triangles.size() - 2;
		}
		
		if (dest >= triangles.size() - 1){
			dest = triangles.size() - 2;
		}
		
		Triangle triangle1 = triangles.get(origin);
		Triangle triangle2 = triangles.get(origin + 1);
		
		Triangle triangle3 = triangles.get(dest);
		Triangle triangle4 = triangles.get(dest + 1);

		Color aux = triangle1.getColor();
		triangle1.setColor(triangle3.getColor());
		triangle3.setColor(aux);
		
		Color aux2 = triangle2.getColor();
		triangle2.setColor(triangle4.getColor());
		triangle4.setColor(aux2);
	}
	
	public static void switchCloseColor(TriangleList<Triangle> triangles, int randomMutationsMax){
		
		int pos = roll(triangles.size());
		int des = 0;
		int jump = roll(randomMutationsMax);
		
		if(random.nextBoolean()){
			des = pos + jump;
		}else{
			des = pos - jump;
		}

		if (des < 0){
			des = (triangles.size() - 1) - pos - jump;
		}

		if (des >= triangles.size()){
			des = 0 + (triangles.size() - pos + jump);
		}
		
		Triangle origin = triangles.get(pos);
		Triangle dest = triangles.get(des);
		
		Color aux = origin.getColor();
		origin.setColor(dest.getColor());
		dest.setColor(aux);
	}
	
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
	
	public double compare(BufferedImage img1, BufferedImage img2) {

		long compareThen = System.currentTimeMillis();
		
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
					
					float r1 = ((rgb1 >> 16) & 0xff) * 1f; 	// 0.299f
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

		double n = width1 * height1 * 3;
		double p = diff / n / 255.0;
		
		long compareNow = System.currentTimeMillis();
		System.out.println("compare took " + (float)(compareNow - compareThen) / 1000f + " seconds");
		
		return 1 - p;
	}
	
	public static int roll(int n){
		return random.nextInt(n);
	}

	public int getPopulationSize() {
		return population;
	}

	public void setPopulationSize(int population) {
		this.population = population;
	}

	public List<TriangleList<Triangle>> getPopulation() {
		return pop;
	}

	public void setPopulation(List<TriangleList<Triangle>> pop) {
		this.pop = pop;
	}

	public BufferedImage getResizedOriginal() {
		return resizedOriginal;
	}

	public void setResizedOriginal(BufferedImage resizedOriginal) {
		this.resizedOriginal = resizedOriginal;
	}

	public BufferedImage getCurrentImage() {
		return currentImage;
	}

	public void setCurrentImage(BufferedImage currentImage) {
		this.currentImage = currentImage;
	}

	public BufferedImage getBestImage() {
		return bestImage;
	}

	public void setBestImage(BufferedImage bestImage) {
		this.bestImage = bestImage;
	}

	public int getTotalIterations() {
		return totalIterations;
	}

	public void setTotalIterations(int totalIterations) {
		this.totalIterations = totalIterations;
	}

	public int getGoodIterations() {
		return goodIterations;
	}

	public void setGoodIterations(int goodIterations) {
		this.goodIterations = goodIterations;
	}

	public double getBestScore() {
		return bestScore;
	}

	public void setBestScore(double bestScore) {
		this.bestScore = bestScore;
	}

	public boolean isDirty() {
		return isDirty;
	}

	public void setDirty(boolean isDirty) {
		this.isDirty = isDirty;
	}

	/**
	 * Extracted Objects to avoid creation during cycles
	 */
	
	static BufferedImage imgParentA;
	static BufferedImage imgChildA;
	static Graphics g;
	static TriangleList <Triangle> parentA;
	static TriangleList <Triangle> parentB;
	static TriangleList <Triangle> childA;
	
	/**
	 * Initial Random Population:
	 * 	- order by score
	 * 	- get top 10%
	 * 	- mix and mutate
	 * 	
	 */
	public void evolve(long start, int iterations) {
		
//		long evolveThen = System.currentTimeMillis();
		
		for (int a = 0; a < iterations; a++){
			
			int rollA = 0;
			int rollB = 0;
			
			while (rollA == rollB){
				rollA = roll(pop.size());
				rollB = roll(pop.size());
			}
			
			parentA = pop.get(rollA);
			parentB = pop.get(rollB);
			
			childA = crossOver.getChild(parentA, parentB);

        	imgParentA = null;
        	g = null;
        	double scoreA = 0d;

        	if (parentA.getScore() <= 0d){
        		imgParentA = new BufferedImage(resizedOriginal.getWidth(), resizedOriginal.getHeight(), ArtEvolver.IMAGE_TYPE); 
    			g = imgParentA.getGraphics();
    			
    			// Iterator parentA
    			for (Triangle triangle : parentA) {
    				if (triangle.getColor() != null) {
    					g.setColor(triangle.getColor());
    					g.drawPolygon(triangle);
    					g.fillPolygon(triangle);
    				} else {
    					g.setColor(Color.BLUE);
    					g.drawPolygon(triangle);
    				}
    			}
        		
        		scoreA = compare(imgParentA, resizedOriginal);
        		parentA.setScore(scoreA);
        	}else{
        		scoreA = parentA.getScore();
        	}

			imgChildA = new BufferedImage(resizedOriginal.getWidth(), resizedOriginal.getHeight(), ArtEvolver.IMAGE_TYPE); 
			g = imgChildA.getGraphics();
			
			// Iterator childA
			for (Triangle triangle : childA) {
				if (triangle.getColor() != null) {
					g.setColor(triangle.getColor());
					g.drawPolygon(triangle);
					g.fillPolygon(triangle);
				} else {
					g.setColor(Color.BLUE);
					g.drawPolygon(triangle);
				}
			}
			
			g.dispose();

			double scoreC = compare(imgChildA,  resizedOriginal);
			childA.setScore(scoreC);
			
			if (scoreA > bestScore){
				bestScore = scoreA;
				bestImage = imgParentA;
				goodIterations++;
				
				isDirty = true;
			}
			
			// KILL PARENT (50/50) IF BEST
			if (scoreC > scoreA){
				pop.remove(parentA);
				pop.add(childA);
				goodIterations++;
			}
			
			// BETTER IMAGE
			if (scoreC > bestScore){
				bestScore = scoreC;
				bestImage = imgChildA;
				goodIterations++;
				
				isDirty = true;
			}
			
			totalIterations++;

			if (totalIterations % 1000 == 0){
				
				long now = System.currentTimeMillis();
				
				System.out.println("i: " + totalIterations
								 + " - p: " + pop.size()
								 + " - jump: " + randomJumpDistance
								 + " - cross: " + crossoverMax
								 + " - best: " + bestScore
								 + " - total time: " + ((float) (now - start) / 1000f) + " seconds");
			}
		}

//		long evolveNow = System.currentTimeMillis();
//		System.out.println("evolve with " + iterations + " iterations took " + (float)(evolveNow - evolveThen) / 1000f + " seconds");
	}
}