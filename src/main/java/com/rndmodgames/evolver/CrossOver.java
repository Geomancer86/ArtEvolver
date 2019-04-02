package com.rndmodgames.evolver;

import java.util.ArrayList;
import java.util.Random;


public class CrossOver {
	
	public static final Random random = new Random();
	
	public static final float RANDOM_CLOSE_MUTATION_PERCENT = 0.6f;
	public static final float RANDOM_MUTATION_PERCENT 		= 0.3f;

	public static final float RANDOM_CROSSOVER_PERCENT 		= 0.3f;
	public static final float RANDOM_MULTI_MUTATION 		= 0.1f;
	public static final int   RANDOM_MULTI_MUTATION_MAX     = 2;
	
	private int randomJumpDistance;
	private int crossoverMax;
	
	public CrossOver(int randomJumpDistance, int crossoverMax){
		this.randomJumpDistance = randomJumpDistance;
		this.crossoverMax = crossoverMax;
	}
	
	public void halveParameters() {
		randomJumpDistance -= 4;
//		crossoverMax -= -2;
		
		if (randomJumpDistance <= 0) {
			randomJumpDistance = 1;
		}
	}
	
	public void incrementParameters() {
		if (randomJumpDistance > 128) {
			return;
		}
		
		randomJumpDistance +=1;
	}
	
	private static TriangleList<Triangle> unusedColors = new TriangleList<>();
	
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
				child.get(a).setColor(unusedColors.get(currentUnusedColor).color);
				
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
		
		ImageEvolver.switchCloseColor(child, randomJumpDistance);
		
		return child;
	}
	
	public TriangleList<Triangle> getChild(TriangleList<Triangle> parentA, TriangleList<Triangle> parentB) {
		
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
		
		boolean notEvolved = true;
		
		while(notEvolved) {
			if (random.nextFloat() < RANDOM_CROSSOVER_PERCENT){
				
				int crossovers = ImageEvolver.roll(crossoverMax) + 1;
	//			int crossovers = crossoverMax;
				
				for (int a = 0; a < crossovers; a++){
					Triangle target = null;
					int origin = ImageEvolver.roll(child.size());
					
					if (isParentA){
						target = parentA.get(origin);
					}else{
						target = parentB.get(origin);
					}
	
					int dest = 0;
					
					for (Triangle triangle : child){
						
						if (triangle.getColor().equals(target.getColor())){
							// found the color, switch positions
							ImageEvolver.switchColor(child, origin, dest);
							dest++;
							notEvolved = false;
							break;
						}
					}
				}
			}
		
//			if (random.nextFloat() < RANDOM_CLOSE_MUTATION_PERCENT){
//				ImageEvolver.switchCloseColor(child, randomJumpDistance);
//				notEvolved = false;
//			}
//		
//			if (random.nextFloat() < RANDOM_MUTATION_PERCENT){
//				ImageEvolver.switchRandomColor(child);
//				notEvolved = false;
//			}
		
//			if (random.nextFloat() < RANDOM_MULTI_MUTATION){
				ImageEvolver.switchRandomMultiColor(child, RANDOM_MULTI_MUTATION_MAX);
				notEvolved = false;
//			}
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
}