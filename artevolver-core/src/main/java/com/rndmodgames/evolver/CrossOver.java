package com.rndmodgames.evolver;

import java.awt.Color;
import java.util.concurrent.ThreadLocalRandom;

public class CrossOver {
	
    // keep track of the evolver to update parameters live
    ImageEvolver evolverInstance;

	// self mutations
	public static       float MAX_RANDOM_MUTATION_PERCENT  =  1f; // default is 1f
	public static       float RANDOM_MUTATION_PERCENT_ADD  =  1f / 10000f; // default is 1 each 10000
	public static final float RANDOM_MULTI_MUTATION 		= 1f; // default is 1f
	public static final int   RANDOM_MULTI_MUTATION_MAX     = 0; // default is 0
	public static final int   CLOSE_MUTATIONS_PER_CHILD     = 0; // default is 0
	
	/**
     * Grid Crossover
     * 
     * MAIN
     */
    public static volatile float GRID_MUTATION_CHANCES = 32; // default is 32
    public static float GRID_MUTATION_PERCENT = 1f; // default is 1
    public static float GRID_MUTATION_DECAY  =  1f / 10; // default is 1
	
    /**
     * Fully Random Crossover
     */
    public static volatile int RANDOM_MUTATION_CHANCES =  1000; // default is 1000
    public static float RANDOM_MUTATION_PERCENT = 1f / 1000f; // default is 1 each 100
    public static float RANDOM_MUTATION_CHANCES_SUBSTRACT =  1f / 100f; // default is 1 each 100
    
	/**
	 * Random Close Crossover
	 */
	public static volatile int RANDOM_CLOSE_MUTATION_CHANCES =  20; // default is 1 each 1000
	public static float RANDOM_CLOSE_MUTATION_PERCENT = 1f / 10000; // default is 1 each 10000

	/**
	 * Random Grid Crossover
	 */
	public static int RANDOM_GRID_MUTATION_CHANCES = 10; // default is 1
    public static float RANDOM_GRID_MUTATION_PERCENT = 1f / 10000; // default is 1
	
	public static int TARGETED_SWAP_ATTEMPTS = 12;
	public static boolean CROSSOVER_BLOCK_ENABLED = true;

	public static       int   TOTAL_GRIDS                   =  8; // NEEDS TO BE == THREADS
	public static       int   DEFAULT_GRID_SIZE             = 256; // default is 4260
	public static       int   MINIMUM_GRID_SIZE             =   2; // default is 4260
	
	// crossover mutations
	public static final float RANDOM_CROSSOVER_PERCENT 				  = -0.01f;
	public static final float RANDOM_CROSSOVER_CLOSE_MUTATION_PERCENT = 0.3f;
	public static final float RANDOM_CROSSOVER_MUTATION_PERCENT 	  = 0.3f;
	public static final float RANDOM_CROSSOVER_MULTI_MUTATION 		  = 0.3f;
	public static final int   RANDOM_CROSSOVER_MULTI_MUTATION_MAX     = 2;
	public static final int   CLOSE_CROSSOVER_MUTATIONS_PER_CHILD     = 2;
	
	// common mutations
	
	
	private int randomJumpDistance;
	private int crossoverMax;
	
	/**
	 * Main constructor
	 * 
	 * @param randomJumpDistance
	 * @param crossoverMax
	 */
	public CrossOver(int randomJumpDistance, int crossoverMax, ImageEvolver evolverInstance){
	    
		this.setRandomJumpDistance(randomJumpDistance);
		this.crossoverMax = crossoverMax;
		
		// keep track
		this.evolverInstance = evolverInstance;

		int totalTriangles = evolverInstance.getTriangleWidth() * evolverInstance.getTriangleHeight();
		if (totalTriangles > 0 && TOTAL_GRIDS > 0) {
			DEFAULT_GRID_SIZE = Math.max(2, totalTriangles / TOTAL_GRIDS);
		}
	}
	
	public void halveParameters() {
	    
	    setRandomJumpDistance((int) (getRandomJumpDistance() / 2));

		if (getRandomJumpDistance() <= 1) {
			setRandomJumpDistance(1);
		}
	}
	
	/**
	 * TODO: validate max distance before raising number
	 * 
	 * @param distance
	 */
	public void incrementParameters(int distance) {

		setRandomJumpDistance(getRandomJumpDistance() + distance);
	}
	
	private TriangleList<Triangle> unusedColors = new TriangleList<>();
	
	/**
	 * We can do 2 iterations for parent a and b, or just one and keep controlling for repeated colors on just one iteration
	 * 
	 * 
	 */
	public TriangleList<Triangle> getGeneticChild(TriangleList<Triangle> parentA, TriangleList<Triangle> parentB, int genSize) {
		
		TriangleList<Triangle> child = new TriangleList<Triangle>();
		
		// fill with empty triangles
		for (int a = 0; a < parentA.size(); a++) {
			child.add(null);
		}
		
		unusedColors.clear();
		
		// iterate parentA, add odd genSize rows
		boolean sideA = true;
		
		// iterate full child, add only gen sized chunks of triangles from parent a
		for (int a = 0; a < parentA.size(); a++) {
			
			if (sideA) {
				child.set(a, parentA.get(a));
				child.get(a).setColor(parentA.get(a).getColor());
			}
			
			// switch sides
			if (a % genSize == 0) {
				sideA = !sideA;
			}
		}
		
		// switch side
		sideA = false;
		
		// iterate again, fill chunks with parent B
		for (int a = 0; a < parentB.size(); a++) {
			
			if (sideA) {
				if (!child.contains(parentB.get(a))) {
					
					child.set(a, parentB.get(a));
					child.get(a).setColor(parentB.get(a).getColor());

				} else {
					// add to unused colors
					unusedColors.add(parentB.get(a));
				}
			} else {
				// add to unused colors
				unusedColors.add(parentB.get(a));
			}
			
			// switch sides
			if (a % genSize == 0) {
				sideA = !sideA;
			}
		}
		
//		System.out.println("unusedColors " + unusedColors.size());
		
		// iterate last time, add missing colors, in order/randomly
		int currentUnusedColor = unusedColors.size() - 1;
		
		for (int a = 0; a < child.size(); a++) {
			
			// if triangle is null, set any triangle from that position on parentA and set colors with unused color
			if (child.get(a) == null) {
				
				child.set(a, parentA.get(a));
				child.get(a).setColor(unusedColors.get(currentUnusedColor).getColor());
				
				unusedColors.remove(currentUnusedColor);
				
				currentUnusedColor--;
				
				if (currentUnusedColor <= 0) {
					return child;
				}
			}
			
		}

		return child;
	}
	
	public TriangleList<Triangle> mutate(TriangleList<Triangle> parent){

		TriangleList<Triangle> child = new TriangleList<Triangle>();
		
		for (Triangle triangle : parent){
			
			if (triangle == null) {
				System.out.println("NULL TRIANGLE!");
				System.exit(0);
			}
			
			if (triangle.getColor() == null) {
				System.out.println("NULL COLOR!");
				System.exit(0);
			}
			
			Triangle copy = new Triangle(triangle.getxPoly(), triangle.getyPoly(), triangle.getLenght(), triangle.getColor());
			child.add(copy);
			
		}
		
		ImageEvolver.switchCloseColor(child, getRandomJumpDistance());
		
		return child;
	}

	/**
	 * Creates a Child Drawing between two Parent Drawings using spatial block crossover.
	 * Copies primary parent, then injects a spatial region from the secondary parent,
	 * swapping colors to maintain the permutation constraint.
	 */
	public TriangleList<Triangle> getChild(TriangleList<Triangle> parentA, TriangleList<Triangle> parentB, int evolverId) {
		
		TriangleList<Triangle> child = new TriangleList<Triangle>();

		ThreadLocalRandom r = ThreadLocalRandom.current();
		int n = parentA.size();

		boolean isParentA = r.nextBoolean();
		TriangleList<Triangle> primary = isParentA ? parentA : parentB;
		TriangleList<Triangle> secondary = isParentA ? parentB : parentA;
		
		for (Triangle triangle : primary) {
			Triangle copy = new Triangle(triangle.getxPoly(), triangle.getyPoly(), triangle.getLenght(), triangle.getColor());
			child.add(copy);
		}

		if (n > 4 && CROSSOVER_BLOCK_ENABLED) {
			int blockSize = Math.max(2, n / r.nextInt(4, 12));
			int blockStart = r.nextInt(n);

			java.util.HashMap<Integer, Integer> colorIndex = new java.util.HashMap<>(n * 2);
			for (int i = 0; i < n; i++) {
				Color c = child.get(i).getColor();
				if (c != null) {
					colorIndex.put(c.getRGB(), i);
				}
			}

			for (int k = 0; k < blockSize; k++) {
				int idx = (blockStart + k) % n;
				Color wantColor = secondary.get(idx).getColor();
				if (wantColor == null) continue;

				Color currentColor = child.get(idx).getColor();
				if (currentColor != null && currentColor.getRGB() == wantColor.getRGB()) continue;

				Integer holderIdx = colorIndex.get(wantColor.getRGB());
				if (holderIdx == null) continue;

				child.get(holderIdx.intValue()).setColor(currentColor);
				child.get(idx).setColor(wantColor);

				if (currentColor != null) {
					colorIndex.put(currentColor.getRGB(), holderIdx);
				}
				colorIndex.put(wantColor.getRGB(), idx);
			}
		}

		/**
		 * Random Close Crossover
		 */
        for (int a = 0; a < RANDOM_CLOSE_MUTATION_CHANCES; a++) {

            if (r.nextFloat() < RANDOM_CLOSE_MUTATION_PERCENT) {

                for (int b = 0; b < CLOSE_MUTATIONS_PER_CHILD; b++) {
                    ImageEvolver.switchCloseColor(child, this.randomJumpDistance);
                }
            }
        }

		/**
		 * Fully Random Crossovers
		 */
	    for (int a = 0; a < RANDOM_MUTATION_CHANCES; a++) {
            
	        //
            if (r.nextFloat() < RANDOM_MUTATION_PERCENT) {
    
                ImageEvolver.switchRandomColor(child);

            }
	    }
		
		/**
		 * Grid Crossovers
		 */
        for (int a = 0; a < GRID_MUTATION_CHANCES; a++) {

            if (r.nextFloat() < GRID_MUTATION_PERCENT) {

                //
                ImageEvolver.switchGridColor(child, evolverId, DEFAULT_GRID_SIZE);
            }
        }
        
        /**
         * Random Grid Crossovers
         */
        for (int a = 0; a < RANDOM_GRID_MUTATION_CHANCES; a++) {

            if (r.nextFloat() < RANDOM_GRID_MUTATION_PERCENT) {

                //
                ImageEvolver.switchGridColor(child, ImageEvolver.roll(TOTAL_GRIDS), DEFAULT_GRID_SIZE);
            }
        }

        /**
         * Targeted Swap (v3.1): guided mutation using source image analysis.
         * Finds worst-matching triangles and swaps toward better colors.
         */
        if (evolverInstance != null && evolverInstance.getResizedOriginal() != null) {
            ImageEvolver.targetedSwap(child, evolverInstance.getResizedOriginal(), TARGETED_SWAP_ATTEMPTS);
        }

		return child;
	}

	public TriangleList<Triangle> getSecuentialChild(TriangleList<Triangle> parent, int startTriangle, int targetTriangle) {
		TriangleList<Triangle> child = new TriangleList<Triangle>();
		
		for (Triangle triangle : parent){
			Triangle copy = new Triangle(triangle.getxPoly(), triangle.getyPoly(), triangle.getLenght(), triangle.getColor());
			child.add(copy);
		}
		
		ImageEvolver.switchColor(child, startTriangle, targetTriangle);
		
		return child;
	}

	public int getRandomJumpDistance() {
		return randomJumpDistance;
	}

	public void setRandomJumpDistance(int randomJumpDistance) {
		this.randomJumpDistance = randomJumpDistance;
	}

	/**
	 * 
	 */
    public static synchronized void halveGridSize() {

        RANDOM_MUTATION_CHANCES -= RANDOM_MUTATION_CHANCES_SUBSTRACT;
        
        GRID_MUTATION_CHANCES -= GRID_MUTATION_DECAY;
        
        if (GRID_MUTATION_CHANCES < 1) {
            
            GRID_MUTATION_CHANCES = 1;
        }
        
        if (RANDOM_MUTATION_CHANCES <= 10) {
            
            RANDOM_MUTATION_CHANCES = 10;
        }
    }
}