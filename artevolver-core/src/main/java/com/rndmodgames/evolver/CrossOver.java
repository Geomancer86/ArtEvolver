package com.rndmodgames.evolver;

import java.util.Random;


public class CrossOver {
	
	public static final Random random = new Random();
	
	// self mutations
	public static final float GRID_MUTATION_PERCENT         = 1f;
	public static final float RANDOM_CLOSE_MUTATION_PERCENT = 1f; // default is 1f
	public static final float RANDOM_MUTATION_PERCENT 		= 1f; // default is 1f
	public static final float RANDOM_MULTI_MUTATION 		= 1f; // default is 1f
	public static final int   RANDOM_MULTI_MUTATION_MAX     = 0; // default is 0
	public static final int   CLOSE_MUTATIONS_PER_CHILD     = 0; // default is 0
	public static final int   DEFAULT_GRID_SIZE             = 256; // default is 128
	
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
	
	public CrossOver(int randomJumpDistance, int crossoverMax){
		this.setRandomJumpDistance(randomJumpDistance);
		this.crossoverMax = crossoverMax;
	}
	
	public void halveParameters() {
	    
	    setRandomJumpDistance(getRandomJumpDistance() / 2);

		if (getRandomJumpDistance() <= 0) {
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
	 * Creates a Child Drawing between two Parent Drawings
	 */
	public TriangleList<Triangle> getChild(TriangleList<Triangle> parentA, TriangleList<Triangle> parentB, int evolverId) {
		
		// TODO static or pool
		TriangleList<Triangle> child = new TriangleList<Triangle>();

		// base parent chance 50/50
		boolean isParentA = random.nextBoolean();
		
		if (isParentA){
			for (Triangle triangle : parentA){
				Triangle copy = new Triangle(triangle.getxPoly(), triangle.getyPoly(), triangle.getLenght(), triangle.getColor());
				child.add(copy);
			}
		}else{
			for (Triangle triangle : parentB){
				Triangle copy = new Triangle(triangle.getxPoly(), triangle.getyPoly(), triangle.getLenght(), triangle.getColor());
				child.add(copy);
			}
		}

		// default to 1
		int gridMutationChances = 1;
		
		// default to 3
		int randomCloseMutationChances = 3;
		
		// default is 1
		int randomMutationChances = 1;

		boolean notEvolved = true;
		
		while(notEvolved) {
		    
            for (int a = 0; a < randomCloseMutationChances; a++) {
    
                if (random.nextFloat() < RANDOM_CLOSE_MUTATION_PERCENT) {
    
                    for (int b = 0; b < CLOSE_MUTATIONS_PER_CHILD; b++) {
                        ImageEvolver.switchCloseColor(child, this.randomJumpDistance);
                    }
    
                    notEvolved = false;
                }
            }
		}
		
		// reset
		notEvolved = true;
		
		while(notEvolved) {
		
		    for (int a = 0; a < randomMutationChances; a++) {
                
		        //
                if (random.nextFloat() < RANDOM_MUTATION_PERCENT) {
        
                    ImageEvolver.switchRandomColor(child);
                    notEvolved = false;
        
                }
		    }
		}
		
		// reset
        notEvolved = true;

        while (notEvolved) {

            for (int a = 0; a < gridMutationChances; a++) {

                if (random.nextFloat() < GRID_MUTATION_PERCENT) {

                    //
                    ImageEvolver.switchGridColor(child, evolverId, DEFAULT_GRID_SIZE);
                    notEvolved = false;
                }
            }
        }
		
//		System.out.println("goodRandomCloseMutations: " + goodRandomCloseMutations);
		
//		boolean notEvolved = true;
		
		// 
//		while(notEvolved) {
//			
//			if (random.nextFloat() < GRID_MUTATION_PERCENT){
//			    
//				ImageEvolver.switchGridColor(child, evolverId);
//				notEvolved = false;
//				
//			}
//			
//			if (random.nextFloat() < RANDOM_CLOSE_MUTATION_PERCENT){
//			    
//				for (int a = 0; a < CLOSE_MUTATIONS_PER_CHILD; a++) {
//					ImageEvolver.switchCloseColor(child, this.randomJumpDistance);
//				}
//				
//				notEvolved = false;
//				
//			}
//			
//			if (random.nextFloat() < RANDOM_MUTATION_PERCENT){
//			    
//				ImageEvolver.switchRandomColor(child);
//				notEvolved = false;
//				
//			}
//		
//			if (random.nextFloat() < RANDOM_MULTI_MUTATION){
//			    
//				ImageEvolver.switchRandomMultiColor(child, RANDOM_MULTI_MUTATION_MAX);
//				notEvolved = false;
//				
//			}
//		}
		
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
}