package com.rndmodgames.evolver;

import java.util.Random;


public class CrossOver {
	
	public static final Random random = new Random();
	
	public static final float RANDOM_CLOSE_MUTATION_PERCENT = 0.15f;
	public static final float RANDOM_MUTATION_PERCENT 		= 0.15f;

	public static final float RANDOM_CROSSOVER_PERCENT 		= 0.15f;
	public static final float RANDOM_MULTI_MUTATION 		= 0.15f;
	public static final int   RANDOM_MULTI_MUTATION_MAX       = 2;
	
	private int randomJumpDistance;
	private int crossoverMax;
	
	public CrossOver(int randomJumpDistance, int crossoverMax){
		this.randomJumpDistance = randomJumpDistance;
		this.crossoverMax = crossoverMax;
	}
	
	public TriangleList<Triangle> getChild(TriangleList<Triangle> parentA, TriangleList<Triangle> parentB){
		TriangleList<Triangle> child = new TriangleList<Triangle>();

		// base parent chance 50/50
		Boolean isParentA = random.nextBoolean();
		
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
		
		if (random.nextFloat() < RANDOM_CROSSOVER_PERCENT){
			
			int crossovers = ImageEvolver.roll(crossoverMax) + 1;
			
			for (int a = 0; a < crossovers; a++){
				Triangle target = null;
				int origin = ImageEvolver.roll(child.size());
				
				if (isParentA){
					target = parentA.get(origin);
				}else{
					target = parentB.get(origin);
				}

				int dest = -1;
				int count = 0;
				
				for (Triangle triangle : child){
					
					if (triangle.getColor().equals(target.getColor())){
						// found the color, switch positions
						dest = count;
						break;
					}
					
					count++;
				}
				
				if (dest >= 0){
					ImageEvolver.switchColor(child, origin, dest);
				}
			}
		}
		
		if (random.nextFloat() < RANDOM_CLOSE_MUTATION_PERCENT){
			ImageEvolver.switchCloseColor(child, randomJumpDistance);
		}
		
		if (random.nextFloat() < RANDOM_MUTATION_PERCENT){
			ImageEvolver.switchRandomColor(child);
		}
		
		if (random.nextFloat() < RANDOM_MULTI_MUTATION){
			ImageEvolver.switchRandomMultiColor(child, RANDOM_MULTI_MUTATION_MAX);
		}
		
		return child;
	}
}