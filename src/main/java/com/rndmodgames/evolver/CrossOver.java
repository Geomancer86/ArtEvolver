package com.rndmodgames.evolver;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class CrossOver {
	
	public static final Random random = new Random();
	
	// self mutations
	public static final float RANDOM_CLOSE_MUTATION_PERCENT = 0.3f;
	public static final float RANDOM_MUTATION_PERCENT 		= 0.3f;
	public static final float RANDOM_MULTI_MUTATION 		= 0.3f;
	public static final int   RANDOM_MULTI_MUTATION_MAX     = 2;
	public static int         CLOSE_MUTATIONS_PER_CHILD     = 8;
	
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
		setRandomJumpDistance(getRandomJumpDistance() - 1);
//		crossoverMax -= -2;
		
		if (getRandomJumpDistance() <= 0) {
			setRandomJumpDistance(1);
		}
	}
	
	public void incrementParameters() {
		if (getRandomJumpDistance() > 128) {
			return;
		}
		
		setRandomJumpDistance(getRandomJumpDistance() + 1);
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
		
		ImageEvolver.switchCloseColor(child, getRandomJumpDistance());
		
		return child;
	}

	/**
	 * Creates a Child Drawing between two Parent Drawings
	 */
	public TriangleList<Triangle> getChild(TriangleList<Triangle> parentA, TriangleList<Triangle> parentB) {
		
		TriangleList<Triangle> child = new TriangleList<Triangle>();
		TriangleList<Triangle> missingPixels = new TriangleList<Triangle>();
		List<Color> missingColors = new ArrayList<Color>();
		
		// 
//		if (true) {
//			child = getGeneticChild(parentA, parentB, 1);
//		}
			
		/**
		 * Full CrossOver (parents are already random so we generate the child in the given order)
		 * 
		 * 	- Gene Sequence Size: how many Pixels in a row
		 *  
		 *  - TODO: optimize
		 */
		if (random.nextFloat() < RANDOM_CROSSOVER_PERCENT){
//			int geneSize = parentA.size() / 2; // Half the starting Pixels of Parent A
//			int maxGenes = 4; // This number should be taken from TOTAL_PALLETES parameter
//			int count = 0;
//			
//			for (int a = 0; a < geneSize; a++){
//				child.add(parentA.get(a));
//			}
//			
//			// Now we need to add the non duplicated Pixels from Parent B, starting from the last index
//			for (int a = geneSize; a < parentB.size(); a++){
//				
//				// make sure we dont have more than max colors per gene
//				count = 0;
//				for (int b = 0; b < parentB.size(); b++) {
//					if (parentB.get(b).getColor().equals(parentA.get(a).getColor())) {
//						count++;
//						if (count == maxGenes) {
//							continue;
//						}
//					}
//				}
//				
//				if (count < maxGenes) {
//					child.add(parentB.get(a));
//				} else {
//					// Skipping repeated Pixel
//					// We need to fill this voids
//					missingPixels.add(parentB.get(a));
//					missingColors.add(parentB.get(a).getColor());
//				}
//			}
//			
//			for (int a = 0; a < missingPixels.size(); a++) {
//				missingPixels.get(a).setColor(missingColors.get(a));
//				child.add(missingPixels.get(a));
//			}
			
			child = getGeneticChild(parentA, parentB, 1);

			boolean notEvolved = true;
			
			while(notEvolved) {
				if (random.nextFloat() < RANDOM_CROSSOVER_CLOSE_MUTATION_PERCENT){
					for (int a = 0; a < CLOSE_CROSSOVER_MUTATIONS_PER_CHILD; a++) {
						ImageEvolver.switchCloseColor(child, this.randomJumpDistance);
					}
					
					notEvolved = false;
				}
				
				if (random.nextFloat() < RANDOM_CROSSOVER_MUTATION_PERCENT){
					ImageEvolver.switchRandomColor(child);
					
					notEvolved = false;
				}
			
				if (random.nextFloat() < RANDOM_CROSSOVER_MULTI_MUTATION){
					ImageEvolver.switchRandomMultiColor(child, RANDOM_CROSSOVER_MULTI_MUTATION_MAX);
					
					notEvolved = false;
				}
			}
			
			return child;
		}

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
						notEvolved = false;
						break;
					}
					
					dest++;
				}
			}
			
			if (random.nextFloat() < RANDOM_CLOSE_MUTATION_PERCENT){
				for (int a = 0; a < CLOSE_MUTATIONS_PER_CHILD; a++) {
					ImageEvolver.switchCloseColor(child, this.randomJumpDistance);
				}
				
				notEvolved = false;
			}
			
			if (random.nextFloat() < RANDOM_MUTATION_PERCENT){
				ImageEvolver.switchRandomColor(child);
				notEvolved = false;
			}
		
			if (random.nextFloat() < RANDOM_MULTI_MUTATION){
				ImageEvolver.switchRandomMultiColor(child, RANDOM_MULTI_MUTATION_MAX);
				notEvolved = false;
			}
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
}