package com.rndmodgames.evolver;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.SplittableRandom;
import java.util.stream.Stream;

import com.rndmodgames.evolver.render.Renderer;

public class ImageEvolver extends AbstractEvolver {

//	public static final MersenneTwisterFast random = new MersenneTwisterFast();
	public static final SplittableRandom random = new SplittableRandom();
	public static final boolean KILL_PARENTS = false;

	private BufferedImage resizedOriginal;
	private BufferedImage currentImage;
	private BufferedImage bestImage;
	private double bestScore = Double.MIN_VALUE;

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
	private boolean exportNextAndClose = false;

	@SuppressWarnings("static-access")
	public ImageEvolver(int population, int randomJumpDistance, int crossoverMax, float scale, Pallete pallete,
			float width, float height, int triangleWidth, int triangleHeight) {
		
		this.population = population;
		this.scale = scale * pallete.totalPalletes / 2;
		this.pallete = pallete;
		this.height = height;
		this.width = width;
		this.randomJumpDistance = randomJumpDistance;
		this.crossoverMax = crossoverMax;

		this.triangleHeight = triangleHeight;
		this.triangleWidth = triangleWidth;

		initCrossOver();
	}

	public void initCrossOver() {
		crossOver = new CrossOver(randomJumpDistance, crossoverMax);
	}

	/**
	 * Initialize the triangles from a TXT file listing all the triangle coordinates and colors
	 * @param filename
	 * @throws IOException 
	 */
	public void initializeFromFile(String filename) throws IOException {
		URL url = getClass().getResource("../../../" + filename);
		File file = new File(url.getPath());

		try (Stream<String>stream = Files.lines(file.toPath(), StandardCharsets.UTF_8)) {
			
			TriangleList<Triangle> triangles = new TriangleList<Triangle>();
			
			stream.forEach((line)->{
				
				// strip all crap and only leave the commas ,
				line = line.replaceAll("\\[", "").replaceAll("\\]", "").replaceAll(" ", "").trim();
				
				String [] splitted = line.split(",");
				
				int xPoly[] = new int[3];
				int yPoly[] = new int[3];
				
				xPoly[0]= Integer.parseInt(splitted[1]);
				xPoly[1]= Integer.parseInt(splitted[2]);
				xPoly[2]= Integer.parseInt(splitted[3]);
				
				yPoly[0]= Integer.parseInt(splitted[4]);
				yPoly[1]= Integer.parseInt(splitted[5]);
				yPoly[2]= Integer.parseInt(splitted[6]);
				
				Color color = new Color(Integer.parseInt(splitted[7]),
										Integer.parseInt(splitted[8]),
										Integer.parseInt(splitted[9]));
				
				Triangle triangle = new Triangle(xPoly, yPoly, 3, null, color);
				triangles.add(triangle);
			});
			
			// add all triangles twice (we need a n pop)
			pop.add(triangles);
		}

		BufferedImage imgParentA = null;
		Graphics g = null;
		double scoreA = 0d;

		for (TriangleList<Triangle> triangles : pop) {

			imgParentA = new BufferedImage(resizedOriginal.getWidth(), resizedOriginal.getHeight(),
					ArtEvolver.IMAGE_TYPE);
			g = imgParentA.getGraphics();

			for (Triangle triangle : triangles) {
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
	
	/**
	 * Initialize the triangles as Isosceles
	 */
	public void initializeIsosceles() {

		int preGenerations = 1;
		int randomMult = 1;

		for (int kk = 0; kk < preGenerations; kk++) {
			for (int i = 0; i < population; i++) {
				TriangleList<Triangle> triangles = new TriangleList<Triangle>();
				int count = 0;
				int position = 0;

				for (int a = 0; a < triangleWidth; a++) {
					for (int b = 0; b < triangleHeight; b++) {

						int xPoly[] = new int[3];
						int yPoly[] = new int[3];

						// reset triangle position
						if (position == 2) {
							position = 0;
						}

						if (position == 0) {
							if (a % 2 == 0) {
								// NORTH
								// (avoid serrated graph, NOTE: depends on triangles and xy)
								if (b < (triangleHeight - 1)) {
									xPoly[0] = (int) (width * a);
									xPoly[1] = (int) ((width * a) + (width * 2));
									xPoly[2] = (int) ((width * a) + (width ));
	
									yPoly[0] = (int) ((height * b));
									yPoly[1] = (int) ((height * b));
									yPoly[2] = (int) ((height * b) + (height));
								}
							} else {
								// SOUTH
								xPoly[0] = (int) ((width * a) - (width ));
								xPoly[1] = (int) ((width * a));
								xPoly[2] = (int) ((width * a) + (width ));

								yPoly[0] = (int) ((height * b) + (height));
								yPoly[1] = (int) ((height * b));
								yPoly[2] = (int) ((height * b) + (height));
							}
						} else if (position == 1) {
							if (a % 2 == 0) {
								// EAST
								xPoly[0] = (int) ((width * a) + (width * 2));
								xPoly[1] = (int) ((width * a) + (width));
								xPoly[2] = (int) ((width * a) + (width * 2));

								yPoly[0] = (int) ((height * b) - (height));
								yPoly[1] = (int) ((height * b));
								yPoly[2] = (int) ((height * b) + (height));
							} else {
								// WEST
								xPoly[0] = (int) ((width * a) - (width));
								xPoly[1] = (int) ((width * a));
								xPoly[2] = (int) ((width * a) - (width));

								yPoly[0] = (int) ((height * b));
								yPoly[1] = (int) ((height * b) - (height));
								yPoly[2] = (int) ((height * b) - (height * 2));
							}
						}

						position++;

						Long colorId = null;
						Color color = null;
						
						if (pallete.getColor(count) != null) {
							colorId = pallete.getColor(count).id;
							color = pallete.getColor(count).color;
						}

						Triangle triangle = new Triangle(xPoly, yPoly, 3, colorId, color);
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

				imgParentA = new BufferedImage(resizedOriginal.getWidth(), resizedOriginal.getHeight(),
						ArtEvolver.IMAGE_TYPE);
				g = imgParentA.getGraphics();

				for (Triangle triangle : triangles) {
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

//		System.out.println("total pixels is " + pop.get(0).size());

		// keep only defined population
		pop = pop.subList(0, population);
	}

	public void initialize() {

		int preGenerations = 1;
		int randomMult = 1;

		for (int kk = 0; kk < preGenerations; kk++) {

//			pallete.randomize();
//			pallete.orderByLuminescence();
//			pallete.orderByBLUE();

			for (int i = 0; i < population; i++) {
				TriangleList<Triangle> triangles = new TriangleList<Triangle>();
				int count = 0;

				for (int a = 0; a < triangleWidth; a++) {
					for (int b = 0; b < triangleHeight; b++) {
						int xPoly[] = new int[3];
						int yPoly[] = new int[3];

						if (b % 2 == 0) {
							xPoly[0] = (int) (width * scale * a);
							xPoly[1] = (int) ((width * scale * a) + (width * scale));
							xPoly[2] = (int) (width * scale * a);

							yPoly[0] = (int) ((height * scale * b));
							yPoly[1] = (int) ((height * scale * b));
							yPoly[2] = (int) ((height * scale * b) + (height * scale));
						} else {
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
						if (pallete.getColor(count) != null) {
							color = pallete.getColor(count).color;
						}

						Triangle triangle = new Triangle(xPoly, yPoly, 3, color);
						triangles.add(triangle);

						count++;
					}
				}

				// randomize
				for (int k = 0; k < triangles.size() * randomMult; k++) {
					switchColor(triangles, roll(triangles.size()), roll(triangles.size()));
				}

				pop.add(triangles);
			}

			BufferedImage imgParentA = null;
			Graphics g = null;
			double scoreA = 0d;

			for (TriangleList<Triangle> triangles : pop) {

				imgParentA = new BufferedImage(resizedOriginal.getWidth(), resizedOriginal.getHeight(),
						ArtEvolver.IMAGE_TYPE);
				
				g = imgParentA.getGraphics();

				for (Triangle triangle : triangles) {
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

//		System.out.println("total pixels is " + pop.get(0).size());

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

	public static void switchColor(List<Triangle> triangles, int a, int b) {
		Triangle origin = triangles.get(a);
		Triangle dest = triangles.get(b);

		Color aux = origin.getColor();
		origin.setColor(dest.getColor());
		dest.setColor(aux);
	}

	public static void switchRandomColor(List<Triangle> triangles) {
		Triangle origin = triangles.get(roll(triangles.size()));
		Triangle dest = triangles.get(roll(triangles.size()));

		Color aux = origin.getColor();
		origin.setColor(dest.getColor());
		dest.setColor(aux);
	}

	// TODO implement randomization
	public static void switchRandomMultiColor(List<Triangle> triangles, int maxTriangles) {

		int origin = roll(triangles.size());
		int dest = roll(triangles.size());

		if (origin >= triangles.size() - 1) {
			origin = triangles.size() - 2;
		}

		if (dest >= triangles.size() - 1) {
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

	public static void switchCloseColor(TriangleList<Triangle> triangles, int randomJumpDistance) {
		
		int pos = roll(triangles.size());
		int des = 0;
		int jump = roll(randomJumpDistance);

		if (random.nextBoolean()) {
			des = pos + jump;
		} else {
			des = pos - jump;
		}

//		System.out.println("pos " + pos + " jump " + jump + " des " + des);
		
		if (des < 0) {
			des = triangles.size() - (jump - pos) - 1;
//			des = (triangles.size() - 1) - pos - jump;
			
		} 
		
		if (des >= triangles.size()) {
			des = 0 + (triangles.size() - jump);
			
		}

		Triangle origin = triangles.get(pos);
		Triangle dest = triangles.get(des);

		Color aux = origin.getColor();
		origin.setColor(dest.getColor());
		dest.setColor(aux);
	}

	public static int roll(int n) {
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

	public boolean isExportNextAndClose() {
		return exportNextAndClose;
	}

	public void setExportNextAndClose(boolean exportNextAndClose) {
		this.exportNextAndClose = exportNextAndClose;
	}

	/**
	 * Extracted Objects to avoid creation during cycles
	 */

	static BufferedImage imgParentA;
	static BufferedImage imgChildA;
	static Graphics g;
	static TriangleList<Triangle> parentA;
	static TriangleList<Triangle> parentB;
	static TriangleList<Triangle> childA;

	public void evolveGreedy(long start) {

		// start with position 0
		// measure, keep score
		// repeat until trying all the elements
	}

	private int GEN_SIZE = 8;
	
	public void evolve2(long start, int iterations) {
		
//		long beforeChild = System.currentTimeMillis();
		
		int rollA, rollB;
		
		for (int a = 0; a < iterations; a++) {

			rollA = 0;
			rollB = 0;
	
			while (rollA == rollB) {
				rollA = roll(pop.size());
				rollB = roll(pop.size());
			}
	
			parentA = pop.get(rollA);
			parentB = pop.get(rollB);
		
			childA = crossOver.getGeneticChild(parentA, parentB, GEN_SIZE);
			TriangleList<Triangle> mutatedChild = crossOver.mutate(childA);
			
			updateFitness(mutatedChild);
			updateStats();
		}
	}
	
	public void updateStats() {
		totalIterations++;

		if (totalIterations % ((population / 2) * 1000) == 0) {
			System.out.println(new DecimalFormat("####.###################", 
							   new DecimalFormatSymbols(Locale.ITALIAN))
					  .format(bestScore));
		}
	}
	
	public void updateFitness(TriangleList<Triangle> mutatedChild) {
		
//		System.out.println("update fitness");
		
		/**
		 * TODO-NA: instead of new image, clear and reuse
		 */
		// score childA
//		if (imgChildA == null) {
			imgChildA = new BufferedImage(resizedOriginal.getWidth(),
					  resizedOriginal.getHeight(),
					  ArtEvolver.IMAGE_TYPE);
//		} else {
//			g.clearRect(0, 0, imgChildA.getWidth(), imgChildA.getHeight());
//		}

		g = imgChildA.getGraphics();

		// Iterator childA
		for (Triangle triangle : mutatedChild) {
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

		double scoreC = compare(imgChildA, resizedOriginal);
		mutatedChild.setScore(scoreC);

		// if score less than better, return
		if (scoreC < bestScore) {
			return;
		} else {
			System.out.println("score: " + scoreC + ", bestScore: " + bestScore);
		}

		Double currentWorstScore = Double.MAX_VALUE;
		int actualWorstPosition = 0;
		int currentWorstPosition = 0;
		
		for (;currentWorstPosition < pop.size(); currentWorstPosition++) {
			if (pop.get(currentWorstPosition).getScore() < currentWorstScore) {
				currentWorstScore = pop.get(currentWorstPosition).getScore();
				actualWorstPosition = currentWorstPosition;
			}
		}
		
		if (scoreC > bestScore) {
			bestScore = scoreC;
			bestImage = imgChildA;
			goodIterations++;
			
			pop.remove(actualWorstPosition);
			
			// add a copy! of child a
			pop.add(mutatedChild);

			isDirty = true;
		}
		
		// replace Child with worst element / worstParent
	}
	
	private boolean secuential = false;
	private boolean randomSecuential = false;
	private boolean secuentialHorizontal = false;
	private int currentParentA = 0;
	private int currentParentB = 1;
	private int startTriangle = 0;
	private int targetTriangle = 1;

	public void switchSecuential() {
		secuential = !secuential;
		randomSecuential = !randomSecuential;
	}
	
	/**
	 * Initial Random Population: - order by score - get top 10% - mix and mutate
	 * 
	 */
	@Override
	public void evolve(long start, int iterations) {

//		long evolveThen = System.currentTimeMillis();

		for (int a = 0; a < iterations; a++) {

			if (!secuential) {
				int rollA = 0;
				int rollB = 0;

				while (rollA == rollB) {
					rollA = roll(pop.size());
					rollB = roll(pop.size());
				}

				parentA = pop.get(rollA);
				parentB = pop.get(rollB);
			} else {
				
				// parent is always 0, order by score so 0 is always the best
				parentA = pop.get(currentParentA);
				parentB = pop.get(currentParentB);
			}

			if (!secuential) {
				childA = crossOver.getChild(parentA, parentB);
			} else {

				childA = crossOver.getSecuentialChild(parentA, startTriangle, targetTriangle);

				if (!randomSecuential) {
					// cycle
					targetTriangle++;

					if (targetTriangle == parentA.size()) {
						targetTriangle = 0;
						startTriangle++;

						if (startTriangle == parentA.size()) {
							startTriangle = 0;
						}
					}
				} else {

					startTriangle++;

					if (startTriangle >= parentA.size()) {
						startTriangle = 0;
					}

					if (!secuentialHorizontal) {
						targetTriangle = startTriangle + 1;
						
						if (targetTriangle >= parentA.size()) {
							targetTriangle = 0;
						}
						
					} else {
						targetTriangle = startTriangle + triangleHeight;
						
						if (targetTriangle >= parentA.size()) {
							targetTriangle = 0 + triangleHeight - (targetTriangle - parentA.size());
						}
					}
				}
			}

			imgParentA = null;
			g = null;
			double scoreA = 0d;

			if (parentA.getScore() <= 0d) {
				imgParentA = new BufferedImage(resizedOriginal.getWidth(), resizedOriginal.getHeight(),
						ArtEvolver.IMAGE_TYPE);
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
			} else {
				scoreA = parentA.getScore();
			}

			if (g != null) {
				g.dispose();
			}

			imgChildA = new BufferedImage(resizedOriginal.getWidth(),
										  resizedOriginal.getHeight(),
										  ArtEvolver.IMAGE_TYPE);
			
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

			double scoreC = compare(imgChildA, resizedOriginal);
			childA.setScore(scoreC);

			// Just in case parent is not evaluated, and it's the first best score
			if (scoreA > bestScore) {
				bestScore = scoreA;
				bestImage = imgParentA;
				goodIterations++;

				isDirty = true;
			}

			/**
			 * Kill worst parent only
			 * 
			 * TODO: iterate all drawings, and replace the actual worst, not the parent,
			 * 	 as the parent might be one of the better results already
			 */
			Double currentWorstScore = Double.MAX_VALUE;
			int actualWorstPosition = 0;
			int currentWorstPosition = 0;
			
			for (;currentWorstPosition < pop.size(); currentWorstPosition++) {
				if (pop.get(currentWorstPosition).getScore() < currentWorstScore) {
					currentWorstScore = pop.get(currentWorstPosition).getScore();
					actualWorstPosition = currentWorstPosition;
				}
			}

			// BETTER IMAGE
			if (scoreC > bestScore) {
				bestScore = scoreC;
				bestImage = imgChildA;
				goodIterations++;
				
				pop.remove(actualWorstPosition);
				pop.add(childA);

				isDirty = true;
				
//				Renderer.renderToPNG(childA, goodIterations, imgChildA.getWidth(), imgChildA.getHeight(), ArtEvolver.IMAGE_TYPE);
				
				if (exportNextAndClose) {
					// export for future resuming
					for (int bb = 0; bb < childA.size(); bb++) {
						System.out.println(bb + "," + childA.get(bb).toString());
					}
					
					System.exit(0);
				}
			}

			totalIterations++;

//			if (totalIterations % ((population / 2) * 1000) == 0) {
			if (totalIterations % 1000 == 0){

//				long now = System.currentTimeMillis();
//				System.out.println("i: " + totalIterations
//								 + " - good: " + goodIterations
//								 + " - p: " + pop.size()
//								 + " - jump: " + crossOver.getRandomJumpDistance()
//								 + " - cross: " + crossoverMax
//								 + " - best: " + bestScore
//								 + " - total time: " + ((float) (now - start) / 1000f) + " seconds");

				System.out
						.println(new DecimalFormat("####.###################", new DecimalFormatSymbols(Locale.ITALIAN))
								.format(bestScore));

//				switchSecuential();
				
//				crossOver.halveParameters();
				
				/**
				 * 100000: 256/256
				 */
//				if (totalIterations % (population * 500) == 0) {
//					crossOver.halveParameters();
//					crossOver.incrementParameters();
//				}
//				if (totalIterations % 100000 == 0) {
//					crossOver.halveParameters();
//				}
			}
		}
//		long evolveNow = System.currentTimeMillis();
//		System.out.println("evolve with " + iterations + " iterations took " + (float)(evolveNow - evolveThen) / 1000f + " seconds");
	}
}