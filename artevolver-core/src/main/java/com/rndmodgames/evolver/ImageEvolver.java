package com.rndmodgames.evolver;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.SplittableRandom;
import java.util.stream.Stream;

/**
 * ImageEvolver v1
 * 
 * This is the current/latest ImageEvolver used by ArtEvolver
 * 
 * @author Geomancer86
 */
public class ImageEvolver extends AbstractEvolver {

	private static final ThreadLocal<SplittableRandom> THREAD_RANDOM =
			ThreadLocal.withInitial(SplittableRandom::new);

	public static SplittableRandom random() {
		return THREAD_RANDOM.get();
	}
	
	public final boolean KILL_PARENTS = false;

	// default is true
	public static boolean SHUFFLE_PALETTE = false;

	public static boolean SMART_INITIALIZATION = true;

	public static boolean VALIDATE_PERMUTATION = true;
	
	public final static DecimalFormat DEFAULT_DECIMAL_FORMAT = new DecimalFormat("##.###################");
	
	private Long id = null;
	private BufferedImage resizedOriginal;
	private BufferedImage currentImage;
	private BufferedImage bestImage;
	private double bestScore = Double.MIN_VALUE;
	private double averageScore = Double.MIN_VALUE;

	private int population;
	private int randomJumpDistance;
	private int crossoverMax;
	private int totalIterations;
	private int goodIterations;

	private float scale;
	private float height;
	private float width;

	private Palette pallete;

	private int triangleHeight;
	private int triangleWidth;

	private CrossOver crossOver;

	// The population for this Evolver instance
	private List<TriangleList<Triangle>> pop = new TriangleList<TriangleList<Triangle>>();

	private boolean isDirty = true;
	private boolean exportNextAndClose = false;

	public ImageEvolver(int population, int randomJumpDistance, int crossoverMax, float scale, Palette pallete,
	                    float width, float height, int triangleWidth, int triangleHeight) {
		
		this.population = population;
		this.scale = scale * pallete.totalPalletes / 2;
		this.pallete = pallete;
		this.height = height;
		this.width = width;
		this.setRandomJumpDistance(randomJumpDistance);
		this.crossoverMax = crossoverMax;

		this.triangleHeight = triangleHeight;
		this.triangleWidth = triangleWidth;

		// keep track of instance
		initCrossOver(this);
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	public Long getId() {
		return this.id;
	}

	public int getTriangleWidth() {
		return triangleWidth;
	}

	public int getTriangleHeight() {
		return triangleHeight;
	}

	/**
	 * @param evolverInstance
	 */
	public void initCrossOver(ImageEvolver evolverInstance) {
	    
		crossOver = new CrossOver(getRandomJumpDistance(), crossoverMax, evolverInstance);
	}

	/**
	 * Initialize the triangles from a TXT file listing all the triangle coordinates and colors
	 * 
	 * @param filename
	 * @throws IOException 
	 */
	public void initializeFromFile(String filename, double scale) throws IOException {
	    
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
				
				xPoly[0]= (int) (Integer.parseInt(splitted[1]) * scale);
				xPoly[1]= (int) (Integer.parseInt(splitted[2]) * scale);
				xPoly[2]= (int) (Integer.parseInt(splitted[3]) * scale);
				
				yPoly[0]= (int) (Integer.parseInt(splitted[4]) * scale);
				yPoly[1]= (int) (Integer.parseInt(splitted[5]) * scale);
				yPoly[2]= (int) (Integer.parseInt(splitted[6]) * scale);
				
				Color color = new Color(Integer.parseInt(splitted[7]),
										Integer.parseInt(splitted[8]),
										Integer.parseInt(splitted[9]));
				
				Triangle triangle = new Triangle(xPoly, yPoly, 3, null, color);
				triangles.add(triangle);
			});
			
			// add all triangles twice (we need a n pop)
			pop.add(triangles);
		}

		double scoreA = 0d;

		for (TriangleList<Triangle> triangles : pop) {
			BufferedImage rendered = renderTriangles(triangles);
			scoreA = compare(rendered, resizedOriginal);
			triangles.setScore(scoreA);
		}
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
				if (SHUFFLE_PALETTE) {
				    for (int k = 0; k < triangles.size() * randomMult; k++){
				        switchColor(triangles, roll(triangles.size()), roll(triangles.size()));
				    }
				}

				pop.add(triangles);
			}
		}

		if (SMART_INITIALIZATION && resizedOriginal != null) {
			for (TriangleList<Triangle> triangles : pop) {
				smartAssignColors(triangles);
			}
		}

		if (VALIDATE_PERMUTATION) {
			for (int i = 0; i < pop.size(); i++) {
				assertPermutation(pop.get(i), "initializeIsosceles pop[" + i + "]");
			}
		}

		double scoreA = 0d;
		for (TriangleList<Triangle> triangles : pop) {
			BufferedImage rendered = renderTriangles(triangles);
			scoreA = compare(rendered, resizedOriginal);
			triangles.setScore(scoreA);
		}

		// Comparator used only once, no need to extract
		Collections.sort(pop, new TrianglesComparator());
	}

	/**
	 * Assigns palette colors to triangles by matching each triangle's region
	 * to the nearest available palette color based on average source image RGB.
	 *
	 * Greedy O(triangles * palette_colors) approach to the color assignment problem.
	 */
	private void smartAssignColors(TriangleList<Triangle> triangles) {
		int imgW = resizedOriginal.getWidth();
		int imgH = resizedOriginal.getHeight();

		int n = triangles.size();

		int[] targetR = new int[n];
		int[] targetG = new int[n];
		int[] targetB = new int[n];

		for (int i = 0; i < n; i++) {
			Triangle tri = triangles.get(i);
			int cx = (tri.getxPoly()[0] + tri.getxPoly()[1] + tri.getxPoly()[2]) / 3;
			int cy = (tri.getyPoly()[0] + tri.getyPoly()[1] + tri.getyPoly()[2]) / 3;

			int sampleCount = 0;
			long rSum = 0, gSum = 0, bSum = 0;

			int[][] offsets = {
				{cx, cy},
				{tri.getxPoly()[0], tri.getyPoly()[0]},
				{tri.getxPoly()[1], tri.getyPoly()[1]},
				{tri.getxPoly()[2], tri.getyPoly()[2]},
				{(cx + tri.getxPoly()[0]) / 2, (cy + tri.getyPoly()[0]) / 2},
				{(cx + tri.getxPoly()[1]) / 2, (cy + tri.getyPoly()[1]) / 2},
				{(cx + tri.getxPoly()[2]) / 2, (cy + tri.getyPoly()[2]) / 2}
			};

			for (int[] pt : offsets) {
				int px = Math.max(0, Math.min(pt[0], imgW - 1));
				int py = Math.max(0, Math.min(pt[1], imgH - 1));
				int rgb = resizedOriginal.getRGB(px, py);
				rSum += (rgb >> 16) & 0xff;
				gSum += (rgb >> 8) & 0xff;
				bSum += rgb & 0xff;
				sampleCount++;
			}

			targetR[i] = (int) (rSum / sampleCount);
			targetG[i] = (int) (gSum / sampleCount);
			targetB[i] = (int) (bSum / sampleCount);
		}

		Color[] currentColors = new Color[n];
		for (int i = 0; i < n; i++) {
			currentColors[i] = triangles.get(i).getColor();
		}

		Integer[] sortedTriangles = new Integer[n];
		for (int i = 0; i < n; i++) sortedTriangles[i] = i;

		Arrays.sort(sortedTriangles, (a, b) -> {
			int satA = Math.max(targetR[a], Math.max(targetG[a], targetB[a]))
					 - Math.min(targetR[a], Math.min(targetG[a], targetB[a]));
			int satB = Math.max(targetR[b], Math.max(targetG[b], targetB[b]))
					 - Math.min(targetR[b], Math.min(targetG[b], targetB[b]));
			return Integer.compare(satB, satA);
		});

		boolean[] colorUsed = new boolean[n];
		int[] assignment = new int[n];
		Arrays.fill(assignment, -1);

		for (int idx : sortedTriangles) {
			int bestColor = -1;
			int bestDist = Integer.MAX_VALUE;

			int tr = targetR[idx], tg = targetG[idx], tb = targetB[idx];

			for (int c = 0; c < n; c++) {
				if (colorUsed[c]) continue;
				if (currentColors[c] == null) continue;

				int dr = tr - currentColors[c].getRed();
				int dg = tg - currentColors[c].getGreen();
				int db = tb - currentColors[c].getBlue();
				int dist = dr * dr + dg * dg + db * db;

				if (dist < bestDist) {
					bestDist = dist;
					bestColor = c;
				}
			}

			if (bestColor >= 0) {
				assignment[idx] = bestColor;
				colorUsed[bestColor] = true;
			}
		}

		Color[] assignedColors = new Color[n];
		for (int i = 0; i < n; i++) {
			if (assignment[i] >= 0) {
				assignedColors[i] = currentColors[assignment[i]];
			} else {
				assignedColors[i] = currentColors[i];
			}
		}
		for (int i = 0; i < n; i++) {
			triangles.get(i).setColor(assignedColors[i]);
		}
	}

	/**
	 * Targeted mutation: finds the worst-matching triangle and swaps its color
	 * with the triangle that currently holds the closest matching color.
	 * Much more effective than random swaps.
	 */
	/**
	 * Targeted mutation: finds the worst-matching triangle and swaps its color
	 * with the triangle that yields the best net fitness improvement for both positions.
	 */
	public static void targetedSwap(TriangleList<Triangle> triangles,
	                                  BufferedImage sourceImage, int attempts) {
		int imgW = sourceImage.getWidth();
		int imgH = sourceImage.getHeight();
		int n = triangles.size();
		if (n < 2) return;

		int[] cxArr = null;
		int[] cyArr = null;
		int[] srcR = null, srcG = null, srcB = null;

		if (cxArr == null) {
			cxArr = new int[n];
			cyArr = new int[n];
			srcR = new int[n];
			srcG = new int[n];
			srcB = new int[n];
			for (int i = 0; i < n; i++) {
				Triangle tri = triangles.get(i);
				cxArr[i] = Math.max(0, Math.min(
					(tri.getxPoly()[0] + tri.getxPoly()[1] + tri.getxPoly()[2]) / 3, imgW - 1));
				cyArr[i] = Math.max(0, Math.min(
					(tri.getyPoly()[0] + tri.getyPoly()[1] + tri.getyPoly()[2]) / 3, imgH - 1));
				int rgb = sourceImage.getRGB(cxArr[i], cyArr[i]);
				srcR[i] = (rgb >> 16) & 0xff;
				srcG[i] = (rgb >> 8) & 0xff;
				srcB[i] = rgb & 0xff;
			}
		}

		for (int attempt = 0; attempt < attempts; attempt++) {
			int worstIdx = -1;
			int worstDist = -1;

			int startIdx = random().nextInt(n);
			int checkCount = Math.min(n, 384);

			for (int k = 0; k < checkCount; k++) {
				int i = (startIdx + k) % n;
				Triangle tri = triangles.get(i);
				if (tri.getColor() == null) continue;

				int dr = srcR[i] - tri.getColor().getRed();
				int dg = srcG[i] - tri.getColor().getGreen();
				int db = srcB[i] - tri.getColor().getBlue();
				int dist = dr * dr + dg * dg + db * db;

				if (dist > worstDist) {
					worstDist = dist;
					worstIdx = i;
				}
			}

			if (worstIdx < 0) continue;

			Triangle worstTri = triangles.get(worstIdx);
			Color worstColor = worstTri.getColor();
			int wR = worstColor.getRed(), wG = worstColor.getGreen(), wB = worstColor.getBlue();

			int bestSwap = -1;
			int bestImprovement = 0;

			int searchStart = random().nextInt(n);
			int searchCount = Math.min(512, n);

			for (int k = 0; k < searchCount; k++) {
				int j = (searchStart + k) % n;
				if (j == worstIdx) continue;
				Triangle cand = triangles.get(j);
				if (cand.getColor() == null) continue;

				int cR = cand.getColor().getRed();
				int cG = cand.getColor().getGreen();
				int cB = cand.getColor().getBlue();

				int beforeA = (srcR[worstIdx] - wR) * (srcR[worstIdx] - wR)
				            + (srcG[worstIdx] - wG) * (srcG[worstIdx] - wG)
				            + (srcB[worstIdx] - wB) * (srcB[worstIdx] - wB);
				int beforeB = (srcR[j] - cR) * (srcR[j] - cR)
				            + (srcG[j] - cG) * (srcG[j] - cG)
				            + (srcB[j] - cB) * (srcB[j] - cB);

				int afterA = (srcR[worstIdx] - cR) * (srcR[worstIdx] - cR)
				           + (srcG[worstIdx] - cG) * (srcG[worstIdx] - cG)
				           + (srcB[worstIdx] - cB) * (srcB[worstIdx] - cB);
				int afterB = (srcR[j] - wR) * (srcR[j] - wR)
				           + (srcG[j] - wG) * (srcG[j] - wG)
				           + (srcB[j] - wB) * (srcB[j] - wB);

				int improvement = (beforeA + beforeB) - (afterA + afterB);

				if (improvement > bestImprovement) {
					bestImprovement = improvement;
					bestSwap = j;
				}
			}

			if (bestSwap >= 0) {
				worstTri.setColor(triangles.get(bestSwap).getColor());
				triangles.get(bestSwap).setColor(worstColor);
			}
		}
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

				// randomize unless smart init will handle it
				if (!SMART_INITIALIZATION || resizedOriginal == null) {
					for (int k = 0; k < triangles.size() * randomMult; k++) {
						switchColor(triangles, roll(triangles.size()), roll(triangles.size()));
					}
				}

				pop.add(triangles);
			}
		}

		if (SMART_INITIALIZATION && resizedOriginal != null) {
			for (TriangleList<Triangle> triangles : pop) {
				smartAssignColors(triangles);
			}
		}

		if (VALIDATE_PERMUTATION) {
			for (int i = 0; i < pop.size(); i++) {
				assertPermutation(pop.get(i), "initialize pop[" + i + "]");
			}
		}

		double scoreA = 0d;
		for (TriangleList<Triangle> triangles : pop) {
			BufferedImage rendered = renderTriangles(triangles);
			scoreA = compare(rendered, resizedOriginal);
			triangles.setScore(scoreA);
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

	public static void switchColor(List<Triangle> triangles, int a, int b) {
		Triangle origin = triangles.get(a);
		Triangle dest = triangles.get(b);

		Color aux = origin.getColor();
		origin.setColor(dest.getColor());
		dest.setColor(aux);
	}

	/**
	 * 
	 * @param triangles
	 */
	public static void switchRandomColor(List<Triangle> triangles) {
		Triangle origin = triangles.get(roll(triangles.size()));
		Triangle dest = triangles.get(roll(triangles.size()));

		Color aux = origin.getColor();
		origin.setColor(dest.getColor());
		dest.setColor(aux);
	}

	/**
	 * Switches a random color limited by a grid size and grid square x,y coordinates
	 * @param triangles
	 * @param width
	 * @param height
	 * @param x
	 * @param y
	 */
	public static void switchGridColor(List<Triangle> triangles, int id, int gridSize) {

		// calculate x,y limits for each grid (per ID)
		int a = roll(gridSize) + (gridSize * id);
		int b = roll(gridSize) + (gridSize * id);
		
//		System.out.println("gridSize: " + gridSize + ", a: " + a + ", b:" + b);
		
		if (a >= triangles.size() || b >= triangles.size()) {
		    System.out.println("grid outside population check parameters");
		    System.out.println("gridSize: " + gridSize + ", a: " + a + ", b:" + b + ", triangles: " + triangles.size());
		    // ignore for now
		    return;
		}
		
		while (b == a) {
			a = roll(gridSize) + (gridSize * id);
		}
		
		Triangle origin = triangles.get(a);
		Triangle dest = triangles.get(b);

		Color aux = origin.getColor();
		origin.setColor(dest.getColor());
		dest.setColor(aux);
	}

	public static void switchCloseColor(TriangleList<Triangle> triangles, int randomJumpDistance) {
		
		int pos = roll(triangles.size());
		int des = 0;
		int jump = roll(randomJumpDistance);

		if (random().nextBoolean()) {
			des = pos + jump;
		} else {
			des = pos - jump;
		}

		if (des < 0) {
			des = triangles.size() - (jump - pos) - 1;

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
		return random().nextInt(n);
	}

	/**
	 * Validates that a TriangleList satisfies basic structural integrity:
	 *   1. No null triangles
	 *   2. No null colors
	 *   3. Color count matches triangle count (no missing assignments)
	 *
	 * Note: the palette may contain duplicate RGB values (different paint chips
	 * with identical digital colors), so we cannot check RGB uniqueness.
	 * The true constraint is that each physical palette chip is used exactly once,
	 * which is maintained by the swap-only mutation operators.
	 *
	 * @return null if valid, or a description of the violation
	 */
	public static String validatePermutation(TriangleList<Triangle> triangles) {
		int n = triangles.size();
		if (n == 0) return "Empty triangle list";

		int nullTriangles = 0;
		int nullColors = 0;

		for (int i = 0; i < n; i++) {
			Triangle tri = triangles.get(i);
			if (tri == null) { nullTriangles++; continue; }
			if (tri.getColor() == null) { nullColors++; }
		}

		if (nullTriangles > 0) return nullTriangles + " null triangle(s)";
		if (nullColors > 0) return nullColors + " null color(s) found";

		return null; // structurally valid
	}

	/**
	 * Deep validation: checks that the color multiset is preserved (same colors
	 * before and after mutations). Call with a reference set from initialization.
	 *
	 * @param triangles the current triangle list
	 * @param referenceColors the original Color array from initialization (sorted by RGB)
	 * @return null if valid, or a description of the mismatch
	 */
	public static String validateColorMultiset(TriangleList<Triangle> triangles, int[] referenceRGBs) {
		int n = triangles.size();
		if (n != referenceRGBs.length) {
			return "Size mismatch: " + n + " triangles vs " + referenceRGBs.length + " reference colors";
		}

		int[] currentRGBs = new int[n];
		for (int i = 0; i < n; i++) {
			Triangle tri = triangles.get(i);
			if (tri == null || tri.getColor() == null) {
				return "Null at index " + i;
			}
			currentRGBs[i] = tri.getColor().getRGB();
		}

		Arrays.sort(currentRGBs);

		for (int i = 0; i < n; i++) {
			if (currentRGBs[i] != referenceRGBs[i]) {
				return String.format("Color multiset mismatch at sorted position %d: " +
					"expected #%06X, got #%06X", i,
					referenceRGBs[i] & 0xFFFFFF, currentRGBs[i] & 0xFFFFFF);
			}
		}

		return null;
	}

	/**
	 * Captures the sorted RGB multiset from a triangle list for use with validateColorMultiset.
	 */
	public static int[] captureColorMultiset(TriangleList<Triangle> triangles) {
		int n = triangles.size();
		int[] rgbs = new int[n];
		for (int i = 0; i < n; i++) {
			Triangle tri = triangles.get(i);
			rgbs[i] = (tri != null && tri.getColor() != null) ? tri.getColor().getRGB() : 0;
		}
		Arrays.sort(rgbs);
		return rgbs;
	}

	/**
	 * Validates permutation and logs result. Returns true if valid.
	 */
	public static boolean assertPermutation(TriangleList<Triangle> triangles, String context) {
		String error = validatePermutation(triangles);
		if (error != null) {
			System.err.println("[PERMUTATION VIOLATION] " + context + ": " + error);
			return false;
		}
		return true;
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

	public BufferedImage getResizedOriginal() {
		return resizedOriginal;
	}

	public void setResizedOriginal(BufferedImage resizedOriginal) {
		this.resizedOriginal = resizedOriginal;
		cacheReferencePixels(resizedOriginal);
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
	
	public double getAverageScore() {
		return averageScore;
	}

	public void setAverageScore(double averageScore) {
		this.averageScore = averageScore;
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

	private TriangleList<Triangle> parentA;
	private TriangleList<Triangle> parentB;
	private TriangleList<Triangle> childA;

	// Reusable image buffer for fitness evaluation (Optimization 2)
	private BufferedImage reusableChildImage;
	private Graphics reusableChildGraphics;

	private void ensureReusableImage() {
		if (reusableChildImage == null
				|| reusableChildImage.getWidth() != resizedOriginal.getWidth()
				|| reusableChildImage.getHeight() != resizedOriginal.getHeight()) {
			if (reusableChildGraphics != null) {
				reusableChildGraphics.dispose();
			}
			reusableChildImage = new BufferedImage(
					resizedOriginal.getWidth(),
					resizedOriginal.getHeight(),
					ArtEvolver.IMAGE_TYPE);
			reusableChildGraphics = reusableChildImage.getGraphics();
		}
	}

	private BufferedImage renderTriangles(TriangleList<Triangle> triangles) {
		ensureReusableImage();
		reusableChildGraphics.clearRect(0, 0, reusableChildImage.getWidth(), reusableChildImage.getHeight());
		for (int i = 0, size = triangles.size(); i < size; i++) {
			Triangle triangle = triangles.get(i);
			if (triangle.getColor() != null) {
				reusableChildGraphics.setColor(triangle.getColor());
				reusableChildGraphics.fillPolygon(triangle);
			}
		}
		return reusableChildImage;
	}

	private BufferedImage bestImageBuffer;
	private Graphics bestImageGraphics;

	private BufferedImage renderTrianglesToNewImage(TriangleList<Triangle> triangles) {
		int w = resizedOriginal.getWidth();
		int h = resizedOriginal.getHeight();
		if (bestImageBuffer == null || bestImageBuffer.getWidth() != w || bestImageBuffer.getHeight() != h) {
			if (bestImageGraphics != null) bestImageGraphics.dispose();
			bestImageBuffer = new BufferedImage(w, h, ArtEvolver.IMAGE_TYPE);
			bestImageGraphics = bestImageBuffer.getGraphics();
		}
		bestImageGraphics.clearRect(0, 0, w, h);
		for (int i = 0, size = triangles.size(); i < size; i++) {
			Triangle triangle = triangles.get(i);
			if (triangle.getColor() != null) {
				bestImageGraphics.setColor(triangle.getColor());
				bestImageGraphics.fillPolygon(triangle);
			}
		}
		return bestImageBuffer;
	}

	public void evolveGreedy(long start) {

		// start with position 0
		// measure, keep score
		// repeat until trying all the elements
	}

	private int GEN_SIZE = 8;
	
	public void evolve2(long start, int iterations) {
		
//		long beforeChild = System.currentTimeMillis();
		
		int rollA, rollB;
		int popSize = pop.size();
		
		for (int a = 0; a < iterations; a++) {

			rollA = 0;
			rollB = 0;
	
			while (rollA == rollB) {
				rollA = roll(popSize);
				rollB = roll(popSize);
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
							   new DecimalFormatSymbols(Locale.ITALIAN)).format(bestScore));
		}
	}
	
	public void updateFitness(TriangleList<Triangle> mutatedChild) {

		BufferedImage rendered = renderTriangles(mutatedChild);
		double scoreC = compare(rendered, resizedOriginal);
		mutatedChild.setScore(scoreC);

		if (scoreC < bestScore) {
			return;
		}

		Double currentWorstScore = Double.MAX_VALUE;
		int actualWorstPosition = 0;
		int currentWorstPosition = 0;
		int popSize = pop.size();
		
		for (;currentWorstPosition < popSize; currentWorstPosition++) {
			if (pop.get(currentWorstPosition).getScore() < currentWorstScore) {
				currentWorstScore = pop.get(currentWorstPosition).getScore();
				actualWorstPosition = currentWorstPosition;
			}
		}
		
		if (scoreC > bestScore) {
			bestScore = scoreC;
			bestImage = renderTrianglesToNewImage(mutatedChild);
			goodIterations++;
			
			pop.remove(actualWorstPosition);
			pop.add(mutatedChild);

			isDirty = true;
		}
	}
	
	/**
	 * DEFAULT
	 * 
	 *     - secuential = false;
	 *     - parentAlwaysA = true;
	 *     - flipParents5050 = false;
	 * 
	 */
	private boolean secuential = false;
	private boolean randomSecuential = false;
	private boolean secuentialHorizontal = false;
	private int currentParentA = 0;
	private int currentParentB = 1;
	private int startTriangle = 0;
	private int targetTriangle = 1;
	
	private boolean parentAlwaysA = true;
	private boolean flipParents5050 = false;

	public void switchSecuential() {
		secuential = !secuential;
		randomSecuential = !randomSecuential;
	}
	
	private double scoreC;
	
	/**
	 * Initial Random Population: - order by score - get top 10% - mix and mutate
	 * 
	 */
	@Override
	public void evolve(long start, int iterations) {

//		long evolveThen = System.currentTimeMillis();

		int popSize = pop.size();

//	synchronized (pop) {
		for (int a = 0; a < iterations; a++) {

			/**
			 * Non Secuental: standard roll [FULLY RANDOM]
			 */
			if (!secuential) {
				// TEST : always pick the best as ParentA
				// TEST2: always pick the best as ParentA, and replace the worst, not the Parent
				// TEST3: always pick the worst as ParentA
				int rollA = popSize - 1;
				int rollB = roll(popSize - 1);

				while ((rollA == rollB)) {
//					rollA = roll(popSize);
					rollB = roll(popSize - 1);
				}

				/**
				 * IF TRUE
				 */
				if (parentAlwaysA) {
				    
				    parentA = pop.get(rollA);
				    parentB = pop.get(rollB);
				    
				} else {
				    
				    /**
				     * 
				     */
				    if (flipParents5050) {
				        
				        // random parent
				        if (random().nextDouble() > 0.5d) {
				            parentA = pop.get(rollA);
	                        parentB = pop.get(rollB);
				        } else {
				            parentA = pop.get(rollB);
	                        parentB = pop.get(rollA);
				        }
				        
				    } else {
				        
				        // parent is b
	                    parentA = pop.get(rollB);
	                    parentB = pop.get(rollA);
				    }
				}
				
			} else {
				
//				parent is always 0, order by score so 0 is always the best
			    
			    /**
			     * FITNESS BASED PARENT SELECTION
			     * 
			     * - population should be ordered
			     * - position 0 is best fitness
			     */
			    
			    // default is true
			    boolean sortPopulation = true;
			    
			    if (sortPopulation) {
			        Collections.sort(pop, new TrianglesComparator());
			    }
			    
			    boolean isSelectedParentA = false;
			    int selectedId = 0;
			    
			    // fitness based is default true
			    boolean fitnessBasedEnabled = true;
			    
			    // default is false
			    boolean fitnessPickLowerScore = false;
			    
			    // START OF FITNESS BASED
			    if (fitnessBasedEnabled) {
			    
    			    float fitnessRoll = (float) random().nextDouble();
    			    
    			    while(!isSelectedParentA) {
    			        
    			        /**
    			         * Higher fitness have higher chances to be picked
    			         */
    			        if (fitnessPickLowerScore) {
    			            
    			            if (fitnessRoll <= pop.get(selectedId).getScore()) {
    	                        
    	                        parentA = pop.get(selectedId);
    	                        isSelectedParentA = true;
    	                        
    	                    } else {
    	                        
    	                        selectedId++;
    	                        fitnessRoll = (float) random().nextDouble();
    	                    }
    			            
    			        } else {
    			            
    			            if (fitnessRoll >= pop.get(selectedId).getScore()) {
    	                        
    	                        parentA = pop.get(selectedId);
    	                        isSelectedParentA = true;
    	                        
    	                    } else {
    	                        
    	                        selectedId++;
    	                        fitnessRoll = (float) random().nextDouble();
    	                    }
    			        }
    			    }
    			    
    			    boolean isSelectedParentB = false;
    			    int selectedBId = 0;
    			    
    			    fitnessRoll = (float) random().nextDouble();
    			    
    			    while(!isSelectedParentB) {
                        
    			        if (fitnessPickLowerScore) {
    			            
    			            /**
    	                     * Higher fitness have higher chances to be picked
    	                     */
    	                    if (fitnessRoll <= pop.get(selectedId).getScore() && selectedBId != selectedId) {
    	                        
    	                        parentB = pop.get(selectedBId);
    	                        isSelectedParentB = true;
    	                        
    	                    } else {
    	                        
    	                        selectedBId++;
    	                        fitnessRoll = (float) random().nextDouble();
    	                    }
    			            
    			        } else {
    			            
    			            /**
    	                     * Higher fitness have higher chances to be picked
    	                     */
    	                    if (fitnessRoll >= pop.get(selectedId).getScore() && selectedBId != selectedId) {
    	                        
    	                        parentB = pop.get(selectedBId);
    	                        isSelectedParentB = true;
    	                        
    	                    } else {
    	                        
    	                        selectedBId++;
    	                        fitnessRoll = (float) random().nextDouble();
    	                    }
    			        }
                    }
			    
    			 // END OF FITNESS BASED
    			    
			    } else {
			        
			        // pick both best parents
			        parentA = pop.get(0);
			        parentB = pop.get(1);
			    }

			}

			if (!secuential) {
				childA = crossOver.getChild(parentA, parentB, this.id.intValue());
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

			double scoreA = 0d;

			if (parentA.getScore() == null || parentA.getScore() <= 0d) {
				BufferedImage renderedParent = renderTriangles(parentA);
				scoreA = compare(renderedParent, resizedOriginal);
				parentA.setScore(scoreA);
			} else {
				scoreA = parentA.getScore();
			}

			if (VALIDATE_PERMUTATION && totalIterations % 10000 == 0) {
				assertPermutation(childA, "evolve child iter=" + totalIterations);
			}

			BufferedImage renderedChild = renderTriangles(childA);
			double scoreC = compare(renderedChild, resizedOriginal);
			childA.setScore(scoreC);
			
//			if (secuential) {
//			    
//			    Collections.sort(pop, new TrianglesComparator());
//			}
			
			// Just in case parent is not evaluated, and it's the first best score
			if (scoreA > bestScore) {
				bestScore = scoreA;
				bestImage = renderTrianglesToNewImage(parentA);
				goodIterations++;

				isDirty = true;
			}

			// this is ok default to false, when a better image is found the better parent (position zero) is replaced
			// NOTE: keep both set to false for optimal results
			boolean killWorst = false;
			boolean insertBetterChildFirst = false;
			
			// BETTER IMAGE
			if (scoreC > bestScore) {
				bestScore = scoreC;
				bestImage = renderTrianglesToNewImage(childA);
				goodIterations++;
				
				// NOTE: worst cases will be taken care by the Tournament Optimizations
				if (killWorst) {
				    
				    // remove last, should be ordered
				    pop.remove(popSize - 1);
				} else {
				
				    // by default we kill the best parent?
				    pop.remove(0);
				}

				if (insertBetterChildFirst) {
				    
				    pop.add(0, childA);
				    
				} else {
				    
				    // default
				    pop.add(childA);
				}

				isDirty = true;
				
//				Collections.sort(pop, new TrianglesComparator());
				// Renderer.renderToPNG(childA, goodIterations, imgChildA.getWidth(), imgChildA.getHeight(), ArtEvolver.IMAGE_TYPE);
				
				if (exportNextAndClose) {
					// export for future resuming
					for (int bb = 0; bb < childA.size(); bb++) {
						System.out.println(bb + "," + childA.get(bb).toString());
					}
					
					System.exit(0);
				}
			} else if (scoreC > scoreA) {
				
				// IF CHILDREN IS BETTER THAN PARENT, KEEP IT AND KILL THE PARENT
				
				goodIterations++;
				
				if (killWorst) {
				    
				    // remove last, should be ordered
                    pop.remove(popSize - 1);
				    
				} else {
				    
				    // by default we kill the best parent?
				    pop.remove(parentA);
				}
				
				if (insertBetterChildFirst) {
                    
                    pop.add(0, childA);
                    
                } else {
                    
                    // default
                    pop.add(childA);
                }
			}

			totalIterations++;

			boolean tournamentEnabled = true;
			boolean crossoverHalvingEnabled = false;
			int tournamentRoundSize = 80;
			
			// close mutations per child default to false
			boolean closeMutationsTournamentEnabled = true;
			
			if (tournamentEnabled && totalIterations % tournamentRoundSize == 0) {

			    CrossOver.halveGridSize();
			    crossOver.halveParameters();
			    
	             //
//                if (crossoverHalvingEnabled) {
//                    
//                    crossOver.halveParameters();
//                    this.randomJumpDistance = crossOver.getRandomJumpDistance();
//                }
			    
//	              long now = System.currentTimeMillis();
//////
//	              System.out.println("id: " + id + " - i: " + totalIterations
//	                               + " - good: " + goodIterations
//	                               + " - p: " + pop.size()
//	                               + " - jump: " + crossOver.getRandomJumpDistance()
//	                               + " - cross: " + crossoverMax
//	                               + " - best: " + DEFAULT_DECIMAL_FORMAT.format(bestScore)
//	                               + " - total time: " + DEFAULT_DECIMAL_FORMAT.format(((float) (now - start)) / 1000f) + " seconds");

//	              System.out.println(DEFAULT_DECIMAL_FORMAT.format(bestScore));
                

                
                /**
                 * v.1.0.0 optimizations
                 *  - CLOSE_MUTATIONS_PER_CHILD * pop
                 */
                
//                if (closeMutationsTournamentEnabled) {
                
//	                if (totalIterations == 2500 * pop.size() * factor) {
//	                    CrossOver.CLOSE_MUTATIONS_PER_CHILD = CrossOver.CLOSE_MUTATIONS_PER_CHILD / 2;
//	                }
//	                
//	                if (totalIterations == 15000 * pop.size() * factor) {
//	                    CrossOver.CLOSE_MUTATIONS_PER_CHILD = CrossOver.CLOSE_MUTATIONS_PER_CHILD / 2;
//	                }
//	                
//	                if (totalIterations == 35000 * pop.size() * factor) {
//	                    CrossOver.CLOSE_MUTATIONS_PER_CHILD = CrossOver.CLOSE_MUTATIONS_PER_CHILD / 2;
//	                }
//	                
//	                if (totalIterations == 75000 * pop.size() * factor) {
//	                    CrossOver.CLOSE_MUTATIONS_PER_CHILD = CrossOver.CLOSE_MUTATIONS_PER_CHILD / 2;
//	                }
//	                
//	                if (totalIterations % 75000 * 2 == 0) {
//	                    CrossOver.CLOSE_MUTATIONS_PER_CHILD = CrossOver.CLOSE_MUTATIONS_PER_CHILD / 2;
//	                }
//	                
//	                if (CrossOver.CLOSE_MUTATIONS_PER_CHILD < 1) {
//	                    CrossOver.CLOSE_MUTATIONS_PER_CHILD = 1;
//	                }
//                }
            }
		}
//	}
//		long evolveNow = System.currentTimeMillis();
//		System.out.println("evolve with " + iterations + " iterations took " + (float)(evolveNow - evolveThen) / 1000f + " seconds");
	}

	// --- Delta Fitness Evolution ---

	private DeltaFitnessEngine deltaEngine;
	private boolean useDeltaEvolution = true;

	public void setUseDeltaEvolution(boolean use) {
		this.useDeltaEvolution = use;
	}

	/**
	 * Initializes the DeltaFitnessEngine for the best individual.
	 * Must be called after initialization and before evolveDelta().
	 */
	public void initDeltaEngine() {
		if (pop.isEmpty() || resizedOriginal == null) return;
		TriangleList<Triangle> best = pop.get(pop.size() - 1);
		deltaEngine = new DeltaFitnessEngine(best, resizedOriginal);
	}

	/**
	 * Delta-based evolution: evaluates swaps in O(pixels_per_triangle) instead
	 * of O(total_pixels). Works in-place on the best individual — no deep copies,
	 * no rendering, no full-image comparison.
	 *
	 * A single call performs 'iterations' rounds. Each round does:
	 *   1. Grid-localized swaps (spatially coherent exploration)
	 *   2. Random global swaps (diversity / escape local optima)
	 *   3. Targeted swaps (source-image-guided refinement)
	 *
	 * Periodically syncs colors back to the TriangleList and renders for UI.
	 */
	public void evolveDelta(long start, int iterations) {
		if (deltaEngine == null) return;

		int n = deltaEngine.getTriangleCount();
		if (n < 2) return;

		TriangleList<Triangle> best = pop.get(pop.size() - 1);
		SplittableRandom r = random();

		int gridSize = Math.max(1, n / CrossOver.TOTAL_GRIDS);
		int numGrids = CrossOver.TOTAL_GRIDS;

		for (int iter = 0; iter < iterations; iter++) {

			int acceptedThisIter = 0;

			// --- Grid-localized swaps ---
			int gridSwaps = (int) CrossOver.GRID_MUTATION_CHANCES;
			for (int g = 0; g < gridSwaps; g++) {
				int gridId = r.nextInt(numGrids);
				int base = gridId * gridSize;
				int a = base + r.nextInt(gridSize);
				int b = base + r.nextInt(gridSize);
				if (a >= n || b >= n || a == b) continue;
				if (deltaEngine.trySwap(a, b)) acceptedThisIter++;
			}

			// --- Random global swaps ---
			float expectedSwaps = CrossOver.RANDOM_MUTATION_CHANCES * CrossOver.RANDOM_MUTATION_PERCENT;
			int randomSwapCount;
			if (expectedSwaps >= 1f) {
				randomSwapCount = (int) expectedSwaps;
			} else {
				randomSwapCount = (r.nextFloat() < expectedSwaps) ? 1 : 0;
			}
			for (int s = 0; s < randomSwapCount; s++) {
				int a = r.nextInt(n);
				int b = r.nextInt(n);
				if (a == b) continue;
				if (deltaEngine.trySwap(a, b)) acceptedThisIter++;
			}

			// --- Targeted swaps (guided by source image centroid matching) ---
			int targetedAttempts = CrossOver.TARGETED_SWAP_ATTEMPTS;
			for (int t = 0; t < targetedAttempts; t++) {
				int worstIdx = -1;
				long worstDelta = Long.MIN_VALUE;

				int startIdx = r.nextInt(n);
				int checkCount = Math.min(n, 384);

				for (int k = 0; k < checkCount; k++) {
					int i = (startIdx + k) % n;
					long selfDelta = computeTriangleSelfDiff(i);
					if (selfDelta > worstDelta) {
						worstDelta = selfDelta;
						worstIdx = i;
					}
				}

				if (worstIdx < 0) continue;

				int bestSwapPartner = -1;
				long bestDelta = 0;

				int searchStart = r.nextInt(n);
				int searchCount = Math.min(512, n);

				for (int k = 0; k < searchCount; k++) {
					int j = (searchStart + k) % n;
					if (j == worstIdx) continue;
					long delta = deltaEngine.computeSwapDelta(worstIdx, j);
					if (delta < bestDelta) {
						bestDelta = delta;
						bestSwapPartner = j;
					}
				}

				if (bestSwapPartner >= 0) {
					deltaEngine.applySwapWithDelta(worstIdx, bestSwapPartner, bestDelta);
					acceptedThisIter++;
				}
			}

			if (acceptedThisIter > 0) {
				goodIterations++;
			}

			totalIterations++;

			// Sync colors back to TriangleList every N iterations for UI display
			if (totalIterations % 50 == 0) {
				syncDeltaToTriangles(best);
				double newScore = deltaEngine.getScore();
				best.setScore(newScore);

				if (newScore > bestScore) {
					bestScore = newScore;
					bestImage = renderTrianglesToNewImage(best);
					isDirty = true;
				}
			}
		}
	}

	/**
	 * Computes how poorly triangle i is matched (its contribution to total diff).
	 * Higher = worse match = better candidate for targeted swap.
	 */
	private long computeTriangleSelfDiff(int triIdx) {
		return deltaEngine.getTriangleError(triIdx);
	}

	/**
	 * Copies delta engine's internal color state back to the TriangleList.
	 */
	private void syncDeltaToTriangles(TriangleList<Triangle> triangles) {
		int n = triangles.size();
		for (int i = 0; i < n; i++) {
			Triangle tri = triangles.get(i);
			int r = deltaEngine.getCurrentColorR(i);
			int g = deltaEngine.getCurrentColorG(i);
			int b = deltaEngine.getCurrentColorB(i);
			tri.setColor(new Color(r, g, b));
		}
	}

	volatile boolean isStarted = false;
	volatile boolean isRunning = false;
	
	public void setRunning(boolean running) {
		this.isRunning = running;
	}
	
	public TriangleList<Triangle> getBestPop(){
	    
	    //
	    if (this.pop.size() == 1) {
	        return this.pop.get(0);
	    }
	    
		return this.pop.get(this.pop.size() - 1);
	}
	
	public void setBestPop(TriangleList<Triangle> e) {
		
		// Replaces the best with the best (this should be ordered)
		this.pop.add(e);
		
//		Collections.sort(pop, new TrianglesComparator());
		
		this.pop.remove(0);
	}
	
	@Override
	public void run() {
		long start = System.currentTimeMillis();
		while (true) {
			if (isRunning) {
				try {
					if (useDeltaEvolution && deltaEngine != null) {
						evolveDelta(start, ArtEvolver.EVOLVE_ITERATIONS);
					} else {
						evolve(start, ArtEvolver.EVOLVE_ITERATIONS);
					}
				} catch (Exception e) {
					// resilient: ignore and retry
				}
			} else {
				try {
					Thread.sleep(50);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		}
	}

	public void raiseMaxJumpDistance(int eVOLVE_JUMPS_ADD) {
	    
	    if (this.randomJumpDistance >= 1) {
	        
	        crossOver.incrementParameters(eVOLVE_JUMPS_ADD);
	        this.randomJumpDistance = crossOver.getRandomJumpDistance();
	        
	    } else {
	        
	        crossOver.setRandomJumpDistance(1);
	        this.randomJumpDistance = 1;
	    }
	}
	
	public void halveGeneralParameters() {
	    crossOver.halveParameters();
        this.randomJumpDistance = crossOver.getRandomJumpDistance();
	}
	
    public int getRandomJumpDistance() {
        return randomJumpDistance;
    }

    public void setRandomJumpDistance(int randomJumpDistance) {
        this.randomJumpDistance = randomJumpDistance;
    }

    public boolean isSecuential() {
        return secuential;
    }

    public void setSecuential(boolean secuential) {
        this.secuential = secuential;
    }
}