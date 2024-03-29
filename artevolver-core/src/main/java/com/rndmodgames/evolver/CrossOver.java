package com.rndmodgames.evolver;

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
    public static float GRID_MUTATION_CHANCES = 32; // default is 32
    public static float GRID_MUTATION_PERCENT = 1f; // default is 1
    public static float GRID_MUTATION_DECAY  =  1f / 10; // default is 1
	
    /**
     * Fully Random Crossover
     */
    public static int RANDOM_MUTATION_CHANCES =  1000; // default is 1000
    public static float RANDOM_MUTATION_PERCENT = 1f / 1000f; // default is 1 each 100
    public static float RANDOM_MUTATION_CHANCES_SUBSTRACT =  1f / 100f; // default is 1 each 100
    
	/**
	 * Random Close Crossover
	 */
	public static int RANDOM_CLOSE_MUTATION_CHANCES =  20; // default is 1 each 1000
	public static float RANDOM_CLOSE_MUTATION_PERCENT = 1f / 10000; // default is 1 each 10000

	/**
	 * Random Grid Crossover
	 */
	public static int RANDOM_GRID_MUTATION_CHANCES = 10; // default is 1
    public static float RANDOM_GRID_MUTATION_PERCENT = 1f / 10000; // default is 1
	
	public static       int   TOTAL_GRIDS                   =  24; // NEEDS TO BE == THREADS
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
		
		// set grid size dynamically defaults to false
		boolean dynamicGridSize = true;
		
		/**
		 * TODO: this needs to be the number of triangles or colors
		 */
		if (dynamicGridSize) {
//		    DEFAULT_GRID_SIZE = (ArtEvolver.heightTriangles * ArtEvolver.widthTriangles) / TOTAL_GRIDS;
//		    DEFAULT_GRID_SIZE = 6048 / TOTAL_GRIDS;
//		    DEFAULT_GRID_SIZE = 23868 / TOTAL_GRIDS;
//		    DEFAULT_GRID_SIZE = 6006 / TOTAL_GRIDS;
//		    DEFAULT_GRID_SIZE = 6048 / TOTAL_GRIDS;
//		    DEFAULT_GRID_SIZE = (102*57) / TOTAL_GRIDS;
		    
//		    DEFAULT_GRID_SIZE = (38*39) / TOTAL_GRIDS; // 1 palette
//		    DEFAULT_GRID_SIZE = (54*55) / TOTAL_GRIDS; // 2 palettes
//		    DEFAULT_GRID_SIZE = (66*67) / TOTAL_GRIDS; // 3 palettes
//		    DEFAULT_GRID_SIZE = (76*77) / TOTAL_GRIDS; // 4 palettes
//		    DEFAULT_GRID_SIZE = (86*87) / TOTAL_GRIDS; // 5 palettes
//		    DEFAULT_GRID_SIZE = (94*95) / TOTAL_GRIDS; // 6 palettes
//		    DEFAULT_GRID_SIZE = (110*111) / TOTAL_GRIDS; // 8 palettes
//		    DEFAULT_GRID_SIZE = (156*157) / TOTAL_GRIDS; // 16 palettes
		    
		    // RECTANGULAR
		    DEFAULT_GRID_SIZE = (80*53) / TOTAL_GRIDS; // 1 palettes
//		    DEFAULT_GRID_SIZE = (66*45) / TOTAL_GRIDS; // 2 palettes
//		    DEFAULT_GRID_SIZE = (96*63) / TOTAL_GRIDS; // 4 palettes
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
			
			Triangle copy = new Triangle(triangle.getxPoly(), triangle.getyPoly(), triangle.getLenght(), triangle.getColor(), triangle.getPalleteColor());
			child.add(copy);
			
		}
		
		ImageEvolver.switchCloseColor(child, getRandomJumpDistance());
		
		return child;
	}

	/**
	 * Creates a Child Drawing between two Parent Drawings
	 */
	public TriangleList<Triangle> getChild(TriangleList<Triangle> parentA, TriangleList<Triangle> parentB, int evolverId) {
		
		// TODO static or pool
		TriangleList<Triangle> child = new TriangleList<Triangle>();

		// base parent chance 50/50
		boolean isParentA = ImageEvolver.random.nextBoolean();
		
		if (isParentA){
			for (Triangle triangle : parentA){
				Triangle copy = new Triangle(triangle.getxPoly(), triangle.getyPoly(), triangle.getLenght(), triangle.getColor(), triangle.getPalleteColor());
				child.add(copy);
			}
		}else{
			for (Triangle triangle : parentB){
				Triangle copy = new Triangle(triangle.getxPoly(), triangle.getyPoly(), triangle.getLenght(), triangle.getColor(), triangle.getPalleteColor());
				child.add(copy);
			}
		}

		/**
		 * Random Close Crossover
		 */
        for (int a = 0; a < RANDOM_CLOSE_MUTATION_CHANCES; a++) {

            if (ImageEvolver.random.nextFloat() < RANDOM_CLOSE_MUTATION_PERCENT) {

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
            if (ImageEvolver.random.nextFloat() < RANDOM_MUTATION_PERCENT) {
    
                ImageEvolver.switchRandomColor(child);

            }
	    }
		
		/**
		 * Grid Crossovers
		 */
        for (int a = 0; a < GRID_MUTATION_CHANCES; a++) {

            if (ImageEvolver.random.nextFloat() < GRID_MUTATION_PERCENT) {

                //
                ImageEvolver.switchGridColor(child, evolverId, DEFAULT_GRID_SIZE);
            }
        }
        
        /**
         * Random Grid Crossovers
         */
        for (int a = 0; a < RANDOM_GRID_MUTATION_CHANCES; a++) {

            if (ImageEvolver.random.nextFloat() < RANDOM_GRID_MUTATION_PERCENT) {

                //
                ImageEvolver.switchGridColor(child, ImageEvolver.roll(TOTAL_GRIDS), DEFAULT_GRID_SIZE);
            }
        }

		return child;
	}

	public TriangleList<Triangle> getSecuentialChild(TriangleList<Triangle> parent, int startTriangle, int targetTriangle) {
		TriangleList<Triangle> child = new TriangleList<Triangle>();
		
		for (Triangle triangle : parent){
			Triangle copy = new Triangle(triangle.getxPoly(), triangle.getyPoly(), triangle.getLenght(), triangle.getColor(), triangle.getPalleteColor());
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
    public static void halveGridSize() {

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