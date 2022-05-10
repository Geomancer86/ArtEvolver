package com.rndmodgames.evolver;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.rndmodgames.evolver.render.Renderer;

public class ArtEvolver extends JFrame implements ActionListener, ChangeListener {

	private static final long serialVersionUID = 6291204469421642923L;
	
	private JFrame mainFrame;
	private JPanel imagePanel;
	private Palette pallete;
	
	//
	DecimalFormat df = new DecimalFormat();
	DecimalFormat df4 = new DecimalFormat();
	private static final String PERCENT_SIGN = "%";
	
	/**
	 * MODES
	 */
	public static final int QUICK_MODE                =   0;
	public static final int QUICK_EXTENDED_MODE       =   1;
	public static final int QUICK_EXTENDED_24_THREADS =   2;
	public static final int FASTEST_MODE              =  10;
	public static final int FASTEST_BATCH_MODE        =  11;
	public static final int QUALITY_SMALL_MODE        =  20;
	public static final int BEST_SMALL_MODE           =  30;
	public static final int QUALITY_MODE              =  90;
	public static final int QUALITY_MODE_FULL_THREADS =  91;
	public static final int QUALITY_MODE_STREAM       = 191;
	
	public static int CURRENT_MODE = QUALITY_MODE_STREAM;
//	public static int CURRENT_MODE = QUALITY_MODE;
	
	// 
	public static boolean HIGH_RESOLUTION_EXPORT = false;
	public static boolean ULTRA_HIGH_RESOLUTION_EXPORT = true;
	public static boolean MEGA_HIGH_RESOLUTION_EXPORT = false;
	public static boolean MASTER_RESOLUTION_EXPORT = false;
	
	// default to false
	public static boolean EXPORT_VIDEO = true;
	public static int EXPORT_VIDEO_FRAMES_FPS = 1;
	
	//
	public static boolean HIGH_RESOLUTION_PATREON_BANNER = false;
	
	public static final String [] MODES = new String [200];
	
	static {
	    MODES[0] = "QUICK_MODE";
	    MODES[1] = "QUICK_EXTENDED_MODE";
	    MODES[2] = "QUICK_EXTENDED_24_THREADS";
	    MODES[10] = "FASTEST_MODE";
	    MODES[11] = "FASTEST_BATCH_MODE";
	    MODES[20] = "QUALITY_SMALL_MODE";
	    MODES[30] = "BEST_SMALL_MODE";
	    MODES[90] = "QUALITY_MODE";
	    MODES[91] = "QUALITY_MODE_FULL_THREADS";
	    MODES[191] = "QUALITY_MODE_STREAM";
	}
	
	/**
	 * TODO: Save Parameters for DROPDOWN SIZE SELECT
	 * 
	 * scale = 3
	 * width = 3 * scale
	 * triangles = 80x53
	 */
	float triangleScaleHeight = 3.0f; // 0.25f, 0.5f, 0.66f, 0.75f, 1f, 1.25f, 1.5f, 2f, 2.5f, 3f 
	float triangleScaleWidth = 3.0f;

	float width = 3.0f * triangleScaleWidth;
	float height = 3.0f * triangleScaleHeight;
	
	/**
	 * TODO:
	 *     - parametrize and configure for different aspect ratios:
	 *         - 16:9 [80x50]
	 *         - 4:3
	 *         - 1:1
	 *         - 9:16
	 */
	int widthTriangles  = 80; // 71
	int heightTriangles = 53; // 60

	/**
	 * PARAMETERS:
	 * 
	 * TODO: fix random removal of drawings from population list
	 */
	private int THREADS                 	= 8; // 1-x (32-48 peak)
	private int POPULATION 					= 8; // GeneticEvolver: 2-4096 (multiply by thread count to get the final population number)
	private int CROSSOVER_MAX 				= 2;
	
	/**
	 * DYNAMIC HEALTH PARAMETERS
	 */
	
	// halve parameters on low health default to false
	private boolean HALVE_PARAMETERS_ON_LOW_HEALTH = true;
	
	// will halve parameters if health reaches zero (default is 15)
	private float LOW_HEALTH_HALVE_PARAMETERS_TRESHOLD = 80f; // 7 = good, 4=test
	
	// evolution jumps default to false
	private boolean EVOLUTION_JUMPS_ENABLED = true;
	
	// evolve each 1000 steps default
	private int EVOLVE_HEALTH_CHECKS_ADD_MAX_JUMP_DISTANCE = 2;
	
	// add 1 to max jump distance default
	private int EVOLVE_JUMPS_ADD = 10;
	
	/**
	 * TOTAL_PALLETES
	 * 
     *    - This is the number of times each triangle will be subdivided.
     *    - One of the most efficient ways to use each Palette Rectangle is TOTAL_PALLETES = 4
     *      - This will generate 4 triangles for each color.
     *      - Current code uses equilateral triangles resorting in using squared palletes, when the physical ones 
     *          are rectangular, we should take real world measures into consideration if wanting >efficiency over >symetrism
	 */
	private int TOTAL_PALLETES             	= 4;
	
	private int GUI_FPS = 60; // twitch fps are set to 20
	private int FPS = 120;
	private int EVOLVER_UPDATE_MS = 1000 / FPS;
//	private int EVOLVER_UPDATE_MS = 0;
	private int GUI_UPDATE_MS = 1000 / GUI_FPS;
	
	/**
	 * Keep track of past n iterations result for better indicators
	 */
	private static final int HEALTH_ITERATIONS = 100;
	private final float [] GOOD_ITERATIONS  = new float [HEALTH_ITERATIONS];
	
	/**
	 * TODO: document and benchmark
	 * 
	 *     - each thread will have the random max jump distance set to the index of this array
	 *     - TODO: find the optimum values for optimum drawing generation (fastest speed)
	 *     
	 *     4239/2
	 */
//	private int RANDOM_JUMP_MAX_DISTANCES [] = {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
//	                                            32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 64, 64,  4239/2,  4239/2, 
//	                                            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
//	                                            32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 64, 64, 4239/2, 4239/2,};
	
	private int RANDOM_JUMP_MAX_DISTANCES [] = {2048, 2048, 2048, 2048, //  4
	                                            2048, 2048, 2048, 2048, //  8
	                                            2048, 2048, 2048, 2048, // 12
	                                            2048, 2048, 2048, 2048, // 16 
	                                            1, 1, 1, 1, // 20
	                                            1, 1, 1, 1, // 24
	                                            1, 1, 1, 1, // 28
	                                            1, 1, 1, 1, // 32
	                                            };
	
	private int CROSSOVERS_MAX [] = {1, 2, 4, 8, 16, 32, 64, 128, 256}; 
	
	public static final int IMAGE_TYPE = BufferedImage.TYPE_INT_ARGB;
	
	/**
	 * TODO: document & optimize
	 * 
	 * EVOLVE_ITERATIONS: 
	 */
	public static int EVOLVE_ITERATIONS    = 100;
	private static int MAX_ITERATIONS      = 10000000;

	private static String SEPARATOR = ";";
	private static String EXPORT_FOLDER = "D:\\Media\\ArtEvolver\\";
	
	private String imageSourceName = null;
	private String imageCategory = null;
	private int exportedImages = 0;

	private List <ImageEvolver> evolvers = new ArrayList<>();

	// Timer
	public Timer processTimer;
	
	// Components
	private JFileChooser chooser;
	private JLabel lblScore;
	private JLabel lblAverageScore;
	private JLabel lblPopulation;
	private JLabel labelSequential;
	private JLabel lblIterations;
	private JLabel lblIterationsPerSecond;
	private JLabel lblGoodIterationsPerSecond;
	
	private String path;
	
	private BufferedImage originalImage;
	private BufferedImage resizedOriginal;
	private BufferedImage bestImage;
	
	long start;
	long steps;
	
	long currentFrame = 1L;
	long totalIterations = 0L;
	long goodIterations = 0L;
	long prevGoodIterations = 0L;
	double bestScore = Double.MIN_VALUE;
	double currentScore = Double.MIN_VALUE;
	double averagePopulationScore = 0d;
	long maxJumpDistanceSum = 0l;
	double averageMaxJumpDistance = 0d;
	boolean isDirty = false;
	boolean isRunning = false;
	boolean showSource = false;
	boolean sequential = false;
	
	/**
	 * Benchmark Options
	 *     TODO: If Score reach this threshold and benchmarking is enabled, processing will export and finish
	 */
	
	
	
	/**
	 * TODO: document
	 */
	boolean offlineExport = false;
	public static boolean EXPORT_ENABLED = true;

	private TriangleList<Triangle> bestPop = new TriangleList<Triangle>();

	/**
	 * v2.05: Fitness Based Parent Selection (FBPS)
	 * 
	 * v2.01: Basic Multithreading
	 * 
	 * 			- Main intention is to scale up the speed with the cores in use by firing multiple Evolver instances on separate Threads.
	 * 				- Move the Best Score/Best Image instances to ArtEvolver
	 * 				- Hardcode 2 Evolvers and compare with only one.
	 * 
	 * 
	 * v2.02: Multithread optimizations
	 * 			-  30 THREADS 120 FPS
	 * 
	 * --- OLD DOCS BELOW
	 * 
	 * v1.0 TODO:
	 * 
	 * 	- Tournament mode:
	 * 		- Starting population size
	 * 		- Halve population each n iterations
	 * 			- Halve or different strategy
	 * 
	 * 		- Full Tournament Run (ie: no inter crossover or mutations)
	 * 		- Population Size Benchmarks
	 * 		
	 * 		- Quick way to switch pixels
	 * 		- Quick way to crossover pixels
	 * 		- Quick way to chunk full pixel chunks
	 * 
	 * @throws IOException
	 */
    public ArtEvolver() throws IOException {
        
        super("ArtEvolver 2021 v2.05");

        //
        df.setMaximumFractionDigits(2);
        df.setMinimumFractionDigits(2);
        df4.setMaximumFractionDigits(6);
        df4.setMinimumFractionDigits(6);
        
        // add extra palettes if HIGH RES is enabled
        if (HIGH_RESOLUTION_EXPORT) {
            
            // 8 palettes = 8520 triangles
            TOTAL_PALLETES = 8;
            widthTriangles = 116; // 116
            heightTriangles = 73; // 73
        }
        
        // add extra palettes if ULTRA_HIGH_RESOLUTION_EXPORT is enabled
        if (ULTRA_HIGH_RESOLUTION_EXPORT) {
            
            // 16 palettes = 17040 triangles
            // total pixels is 17010
            TOTAL_PALLETES = 16;
            widthTriangles = 162; // 162
            heightTriangles = 105; // 105
        }
        
        // add extra palettes if MEGA_HIGH_RESOLUTION_PATREON_BANNER is enabled
        if (MEGA_HIGH_RESOLUTION_EXPORT) {
            
            // 32 palettes = 34080 colors in 32 repetitions
            // total pixels is 33972
            TOTAL_PALLETES = 32;
            widthTriangles = 228; // 228
            heightTriangles = 149; // 149
        }
        
        // add extra palettes if MASTER_RESOLUTION_EXPORT is enabled
        if (MASTER_RESOLUTION_EXPORT) {
            
            // 64 palettes = 68160 colors in 64 repetitions
            // total pixels is 68160
            TOTAL_PALLETES = 64;
            widthTriangles = 320; // 320
            heightTriangles = 213; // 213
        }
                
        // Patreon Banner is 1600x400
        if (HIGH_RESOLUTION_PATREON_BANNER) {
            TOTAL_PALLETES = 8;
            widthTriangles = 178; // 116
            heightTriangles = 45; // 73
        }
        
    	switch (CURRENT_MODE) {
    	
    	case QUALITY_MODE_FULL_THREADS:
            THREADS = 24; // 24, 32, 48, 64
            POPULATION = 5;
            triangleScaleHeight = 3f;
            triangleScaleWidth = 3f;
            width = 3.0f * triangleScaleWidth;
            height = 3.0f * triangleScaleHeight;
            break;
    	
        /**
         *  Multipliers and Resolution:
         *  
         *      - 3x =  720 x  468
         *      - 4x =  960 x  624
         *      - 5x = 1200 x  780
         *      - 6x = 1440 x  936
         *      - 7x = 1680 x 1092 [HD]
         */
    	case QUALITY_MODE_STREAM:
    	    
    	    THREADS = 24;
            POPULATION = 8;
            
            triangleScaleHeight = 6f;
            triangleScaleWidth = 6f;
            
            // 4k
//            RANDOM_JUMP_MAX_DISTANCES [0] = 8520 / 2;
//            RANDOM_JUMP_MAX_DISTANCES [1] = 8520 / 2;
//            RANDOM_JUMP_MAX_DISTANCES [2] = 8520 / 2;
//            RANDOM_JUMP_MAX_DISTANCES [3] = 8520 / 2;
            
//            RANDOM_JUMP_MAX_DISTANCES [0] = 17040 / 2;
            RANDOM_JUMP_MAX_DISTANCES [1] = 17040 / 2;
            RANDOM_JUMP_MAX_DISTANCES [2] = 17040 / 2;
            RANDOM_JUMP_MAX_DISTANCES [3] = 17040 / 2;
            RANDOM_JUMP_MAX_DISTANCES [4] = 17040 / 2;
            RANDOM_JUMP_MAX_DISTANCES [5] = 17040 / 2;
            RANDOM_JUMP_MAX_DISTANCES [6] = 17040 / 2;
            RANDOM_JUMP_MAX_DISTANCES [7] = 17040 / 2;
            RANDOM_JUMP_MAX_DISTANCES [8] = 17040 / 2;
            RANDOM_JUMP_MAX_DISTANCES [9] = 17040 / 2;
            RANDOM_JUMP_MAX_DISTANCES [10] = 17040 / 2;
            RANDOM_JUMP_MAX_DISTANCES [11] = 17040 / 2;
            RANDOM_JUMP_MAX_DISTANCES [12] = 17040 / 2;
            RANDOM_JUMP_MAX_DISTANCES [13] = 17040 / 2;
            RANDOM_JUMP_MAX_DISTANCES [14] = 17040 / 2;
            RANDOM_JUMP_MAX_DISTANCES [15] = 17040 / 2;
            
            if (ULTRA_HIGH_RESOLUTION_EXPORT) {
                
                triangleScaleHeight = 4f;
                triangleScaleWidth = 4f;
                
                // 17k
                RANDOM_JUMP_MAX_DISTANCES [0] = 17040 / 2;
                RANDOM_JUMP_MAX_DISTANCES [1] = 17040 / 2;
                RANDOM_JUMP_MAX_DISTANCES [2] = 17040 / 2;
                RANDOM_JUMP_MAX_DISTANCES [3] = 17040 / 2;
                RANDOM_JUMP_MAX_DISTANCES [4] = 17040 / 2;
                RANDOM_JUMP_MAX_DISTANCES [5] = 17040 / 2;
                RANDOM_JUMP_MAX_DISTANCES [6] = 17040 / 2;
                RANDOM_JUMP_MAX_DISTANCES [7] = 17040 / 2;
                RANDOM_JUMP_MAX_DISTANCES [8] = 17040 / 2;
                RANDOM_JUMP_MAX_DISTANCES [9] = 17040 / 2;
                RANDOM_JUMP_MAX_DISTANCES [10] = 17040 / 2;
                RANDOM_JUMP_MAX_DISTANCES [11] = 17040 / 2;
                RANDOM_JUMP_MAX_DISTANCES [12] = 17040 / 2;
                RANDOM_JUMP_MAX_DISTANCES [13] = 17040 / 2;
                RANDOM_JUMP_MAX_DISTANCES [14] = 17040 / 2;
                RANDOM_JUMP_MAX_DISTANCES [15] = 17040 / 2;
            }
            
            if (MEGA_HIGH_RESOLUTION_EXPORT) {
                
                triangleScaleHeight = 3f;
                triangleScaleWidth = 3f;
                
                // 34k
                RANDOM_JUMP_MAX_DISTANCES [0] = 34080 / 2;
                RANDOM_JUMP_MAX_DISTANCES [1] = 34080 / 2;
                RANDOM_JUMP_MAX_DISTANCES [2] = 34080 / 2;
                RANDOM_JUMP_MAX_DISTANCES [3] = 34080 / 2;
                RANDOM_JUMP_MAX_DISTANCES [4] = 34080 / 2;
                RANDOM_JUMP_MAX_DISTANCES [5] = 34080 / 2;
                RANDOM_JUMP_MAX_DISTANCES [6] = 34080 / 2;
                RANDOM_JUMP_MAX_DISTANCES [7] = 34080 / 2;
            }
            
            if (MASTER_RESOLUTION_EXPORT) {
                
                triangleScaleHeight = 2f;
                triangleScaleWidth = 2f;
                
                // TODO WIP
//                RANDOM_JUMP_MAX_DISTANCES [0] = 34080 / 2;
//                RANDOM_JUMP_MAX_DISTANCES [1] = 34080 / 2;
//                RANDOM_JUMP_MAX_DISTANCES [2] = 34080 / 2;
//                RANDOM_JUMP_MAX_DISTANCES [3] = 34080 / 2;
            }
            
            width = 3.0f * triangleScaleWidth;
            height = 3.0f * triangleScaleHeight;
            
            break;
            
    	case QUALITY_MODE:

    	    THREADS = 4;
    	    POPULATION = 4;
            triangleScaleHeight = 3f;
            triangleScaleWidth = 3f;
            
            if (MASTER_RESOLUTION_EXPORT) {
                triangleScaleHeight = 2f;
                triangleScaleWidth = 2f;
            }
            
            width = 3.0f * triangleScaleWidth;
            height = 3.0f * triangleScaleHeight;
              
            break;
            
    	case BEST_SMALL_MODE:
            THREADS = 32;
            MAX_ITERATIONS = 2500000;
            triangleScaleHeight = 0.5f;
            triangleScaleWidth = 0.5f;
            width = 3.0f * triangleScaleWidth;
            height = 3.0f * triangleScaleHeight;
            break;
            
    	case QUALITY_SMALL_MODE:
            THREADS = 32;
            MAX_ITERATIONS = 1000000;
            triangleScaleHeight = 0.5f;
            triangleScaleWidth = 0.5f;
            width = 3.0f * triangleScaleWidth;
            height = 3.0f * triangleScaleHeight;
            break;
    	
    	case QUICK_EXTENDED_MODE:
            THREADS = 16;
            MAX_ITERATIONS = 250000;
            triangleScaleHeight = 0.5f;
            triangleScaleWidth = 0.5f;
            width = 3.0f * triangleScaleWidth;
            height = 3.0f * triangleScaleHeight;
            break;

    	case QUICK_EXTENDED_24_THREADS:
    	    THREADS = 24;
            MAX_ITERATIONS = 250000;
            triangleScaleHeight = 0.5f;
            triangleScaleWidth = 0.5f;
            width = 3.0f * triangleScaleWidth;
            height = 3.0f * triangleScaleHeight;
            break;
            
    	case FASTEST_MODE:
            THREADS = 32;
            MAX_ITERATIONS = 2500;
            triangleScaleHeight = 0.5f;
            triangleScaleWidth = 0.5f;
            width = 3.0f * triangleScaleWidth;
            height = 3.0f * triangleScaleHeight;
            break;
            
    	case FASTEST_BATCH_MODE:
    	    THREADS = 1;
            MAX_ITERATIONS = 5000;
            triangleScaleHeight = 0.5f;
            triangleScaleWidth = 0.5f;
            width = 3.0f * triangleScaleWidth;
            height = 3.0f * triangleScaleHeight;
            break;
            
    	case QUICK_MODE:
    	default:
    	    THREADS = 1;
    	    MAX_ITERATIONS = 25000;
    	    triangleScaleHeight = 0.5f;
    	    triangleScaleWidth = 0.5f;
    	    width = 3.0f * triangleScaleWidth;
    	    height = 3.0f * triangleScaleHeight;
    	    break;
    	}
    	
    	pallete = new Palette("Sherwin-Williams", TOTAL_PALLETES);

    	// Create Evolver instances as configured by the THREADS parameter
    	for (int a = 0; a < THREADS; a ++) {
    		
    		evolvers.add(new ImageEvolver(POPULATION, 
    									  RANDOM_JUMP_MAX_DISTANCES[a],
    									  CROSSOVER_MAX,
    									  triangleScaleHeight,
    									  pallete,
    									  width,
    									  height,
    									  widthTriangles,
    									  heightTriangles));
    		
    		evolvers.get(a).setId((long) a);
    	}

        initComponents();
    }
    
	private void initComponents() {
		
//		Locale.setDefault(Locale.GERMAN);
		
        mainFrame = this;
        mainFrame.setResizable(true);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // init timer
//        processTimer = new Timer(0, new ActionListener() { 
        processTimer = new Timer(EVOLVER_UPDATE_MS, new ActionListener() { 

            @Override
            public void actionPerformed(ActionEvent e) {

				// reset as needed
				totalIterations = 0;
				goodIterations = 0;
				maxJumpDistanceSum = 0;

				// ignore
				if (imagePanel == null || imagePanel.getGraphics() == null) {
				    
				    return;
				}
				
				for (AbstractEvolver currentEvolver : evolvers) {
					
				    // update sequential
				    ((ImageEvolver)currentEvolver).setSecuential(sequential);
				    
					totalIterations += ((ImageEvolver)currentEvolver).getTotalIterations();
					goodIterations += ((ImageEvolver)currentEvolver).getGoodIterations();
					
					// average max jump distance
//					((ImageEvolver)currentEvolver).m
					maxJumpDistanceSum += ((ImageEvolver)currentEvolver).getRandomJumpDistance();

					if (((ImageEvolver)currentEvolver).isDirty()) {

						// Check again with the best ArtEvolver score
						if (((ImageEvolver)currentEvolver).getBestScore() > bestScore) {
							
//							System.out.println("Evolver " + ((ImageEvolver)currentEvolver).getId() + ", iterations: " + ((ImageEvolver)currentEvolver).getTotalIterations() + ", bestScore: " + ((ImageEvolver)currentEvolver).getBestScore());
							
							bestScore = ((ImageEvolver)currentEvolver).getBestScore();
							bestImage = ((ImageEvolver)currentEvolver).getBestImage();
							
							bestPop = ((ImageEvolver)currentEvolver).getBestPop();
														
							isDirty = true;
        				}
						
						// clean dirty
						((ImageEvolver)currentEvolver).setDirty(false);
					}
				}
				
				// draw bestImage to panel
				if (showSource) {
				    
				    // 
				    showSource();
				    
				} else {
				    if (currentFrame % GUI_UPDATE_MS == 0 && isDirty) {
	                    
	                    imagePanel.getGraphics().drawImage(bestImage,
	                                                       32, // TODO: make both offsets dynamic to center in JPanel
	                                                       32,
	                                                       null);
	                    
	                    imagePanel.getGraphics().dispose();
	                    
	                    isDirty = false;
	                }
				}
				
				// sync best images
            	// TODO: set sync speed to get the best performance
				int population = 0;
				
            	for (AbstractEvolver currentEvolver : evolvers) {
            	    
            		if (((ImageEvolver)currentEvolver).getBestScore() < bestScore) {
            		    
            			((ImageEvolver)currentEvolver).setBestPop(bestPop);
            		}
            		
            		population += ((ImageEvolver)currentEvolver).getPopulation().size();
            	}
            	
            	/**
            	 * Keep track of the last n iterations and good iteration count
            	 */
//            	float healthScore = ((float) goodIterations / (float) totalIterations) * 100f;
            	
            	// keep track of only the NEW GOOD ITERATIONS
                GOOD_ITERATIONS[(int) (currentFrame % HEALTH_ITERATIONS)] = (float) (goodIterations - prevGoodIterations) * 100;

            	/**
            	 * This stats only make sense for benchmarking and should keep consistent, for example, printed once each 1 second
            	 */
            	if (currentFrame % HEALTH_ITERATIONS == 0) {
//            	    if (currentFrame % 2 == 0) {

            	    lblScore.setText("S: " + df4.format(bestScore * 100f) + PERCENT_SIGN);
                    
            	    // avoid divisions by zero just in case
                    if (goodIterations > 0 && totalIterations > HEALTH_ITERATIONS) {

                        // health is the average of the last HEALTH_ITERATIONS count
                        lblAverageScore.setText("H: " + df.format(streamAvg(GOOD_ITERATIONS, HEALTH_ITERATIONS)) + PERCENT_SIGN);
                    }
                    
                    // 
                    lblPopulation.setText("Pop: " + population);
                    lblIterations.setText("I: " + goodIterations + "/" + totalIterations);
                    
                    /**
                     * DYNAMIC HEALTH CHECK
                     */
                    if (HALVE_PARAMETERS_ON_LOW_HEALTH) {
                        
                        if ((streamAvg(GOOD_ITERATIONS, HEALTH_ITERATIONS)) <= LOW_HEALTH_HALVE_PARAMETERS_TRESHOLD ) {

                            for (AbstractEvolver currentEvolver : evolvers) {
                                
//                                ((ImageEvolver) currentEvolver).halveGeneralParameters();
                                ((ImageEvolver) currentEvolver).raiseMaxJumpDistance(EVOLVE_JUMPS_ADD);
                            }
                        }
                        
                        
                    }
                    
                    // healthy evolve, add max jumps if count reaches treshold
                    if (EVOLUTION_JUMPS_ENABLED) {

                        if (currentFrame % (EVOLVE_HEALTH_CHECKS_ADD_MAX_JUMP_DISTANCE * HEALTH_ITERATIONS) == 0) {

                            for (AbstractEvolver currentEvolver : evolvers) {
                             
                                ((ImageEvolver) currentEvolver).raiseMaxJumpDistance(-EVOLVE_JUMPS_ADD);
                            }
                        }
                    }

                    // total_iterations, good_iterations, health, best_score, max_jump_average
                    System.out.println(totalIterations 
                                        + "," + goodIterations
                                        + "," + streamAvg(GOOD_ITERATIONS, HEALTH_ITERATIONS)
                                        + "," + bestScore
                                        + "," + ((float) maxJumpDistanceSum / (float) THREADS));
                    
//                    System.out.println("Evolver " + ((ImageEvolver)currentEvolver).getId() + ", iterations: " + ((ImageEvolver)currentEvolver).getTotalIterations() + ", bestScore: " + ((ImageEvolver)currentEvolver).getBestScore());
            	}
            	
            	
            	
            	currentFrame++;
            	prevGoodIterations = goodIterations;
            	
            	/**
            	 * Close and Export for Quick and Quick Extended Modes
            	 */
            	if (CURRENT_MODE != QUALITY_MODE 
            	        && CURRENT_MODE != QUALITY_MODE_FULL_THREADS) {
            	    
            	    if (totalIterations >= MAX_ITERATIONS) {
                        
            	        if (!offlineExport) {
            	        
                            //
            	            renderBestImage();

                            /**
                             * Only call System.exit if required
                             * 
                             * TODO: app keeps running on standalone mode (with window invisible and disposed). FIX
                             */
                            setVisible(false);
                            dispose();
                            
                            offlineExport = true;
            	        }
                    }
            	}
            }
        });
        
        Container container = getContentPane();
        Container menuContainer = new JPanel();
        
        menuContainer.setLayout(new BoxLayout(menuContainer, BoxLayout.Y_AXIS));
        ((JComponent) menuContainer).setBorder(BorderFactory.createEmptyBorder(32, 0, 0, 0));

		imagePanel = new JPanel() {

            private static final long serialVersionUID = -1275189729010345619L;

            @Override
	        protected void paintComponent(Graphics g) {

                //
	            super.paintComponent(g);
	        }
	    };
	    
//	    imagePanel.setBackground(Color.BLUE);

	    JButton loadButton = new JButton("Load");
	    loadButton.setAlignmentX(Component.CENTER_ALIGNMENT);
	    loadButton.addActionListener(this);
	    loadButton.setMinimumSize(new Dimension(160, 24));
	    loadButton.setPreferredSize(new Dimension(160, 24));
	    loadButton.setMaximumSize(new Dimension(160, 24));
	    
	    menuContainer.add(loadButton);
	    
	    JButton startButton = new JButton("Start");
	    startButton.setAlignmentX(Component.CENTER_ALIGNMENT);
      	startButton.addActionListener(this);
      	startButton.setMinimumSize(new Dimension(160, 24));
      	startButton.setPreferredSize(new Dimension(160, 24));
      	startButton.setMaximumSize(new Dimension(160, 24));
      	
      	menuContainer.add(startButton);
      	
        JButton stopButton = new JButton("Stop");
        stopButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        stopButton.addActionListener(this);
        stopButton.setMinimumSize(new Dimension(160, 24));
        stopButton.setPreferredSize(new Dimension(160, 24));
        stopButton.setMaximumSize(new Dimension(160, 24));
        
        menuContainer.add(stopButton);
        
        JButton enableSecuentialButton = new JButton("Secuential");
        enableSecuentialButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        enableSecuentialButton.addActionListener(this);
//        menuContainer.add(enableSecuentialButton);

        JButton source = new JButton("Source");
        source.setAlignmentX(Component.CENTER_ALIGNMENT);
        source.addActionListener(this);
        source.setMinimumSize(new Dimension(160, 24));
        source.setPreferredSize(new Dimension(160, 24));
        source.setMaximumSize(new Dimension(160, 24));
        
        menuContainer.add(source);
        
        JButton export = new JButton("Export");
        export.setAlignmentX(Component.CENTER_ALIGNMENT);
        export.addActionListener(this);
        export.setMinimumSize(new Dimension(160, 24));
        export.setPreferredSize(new Dimension(160, 24));
        export.setMaximumSize(new Dimension(160, 24));

        menuContainer.add(export);
        
        /**
         * HALVE PARAMETERS
         * DOUBLE PARAMETERS
         * TOGGLE SECUENTIAL
         */
        JButton sequential = new JButton("Sequential");
        sequential.setAlignmentX(Component.CENTER_ALIGNMENT);
        sequential.addActionListener(this);
        sequential.setMinimumSize(new Dimension(160, 24));
        sequential.setPreferredSize(new Dimension(160, 24));
        sequential.setMaximumSize(new Dimension(160, 24));
        
        menuContainer.add(sequential);
                
        Container labelContainer = new JPanel();
        
        menuContainer.add(export);
        
        //
		lblScore = new JLabel("S: 0.0");
		lblScore.setHorizontalAlignment(SwingConstants.CENTER);
		lblScore.setMinimumSize(new Dimension(220, 44));
		lblScore.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 20));
		lblScore.setPreferredSize(new Dimension(220, 44));
		lblScore.setMaximumSize(new Dimension(220, 44));

		//
		lblPopulation = new JLabel("Pop: 0");
		lblPopulation.setHorizontalAlignment(SwingConstants.CENTER);
		lblPopulation.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 30));
		lblPopulation.setMinimumSize(new Dimension(220, 44));
		lblPopulation.setPreferredSize(new Dimension(220, 44));
		lblPopulation.setMaximumSize(new Dimension(220, 44));

		//
		lblIterations = new JLabel("I: 0/0");
		lblIterations.setHorizontalAlignment(SwingConstants.CENTER);
		lblIterations.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 20));
		lblIterations.setMinimumSize(new Dimension(220, 44));
		lblIterations.setPreferredSize(new Dimension(220, 44));
		lblIterations.setMaximumSize(new Dimension(220, 44));

		labelSequential = new JLabel("Sequential: OFF");
		labelSequential.setHorizontalAlignment(SwingConstants.CENTER);
		labelSequential.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 20));
		labelSequential.setMinimumSize(new Dimension(220, 44));
		labelSequential.setPreferredSize(new Dimension(220, 44));
		labelSequential.setMaximumSize(new Dimension(220, 44));
		
		//
//		lblIterationsPerSecond = new JLabel("I/second: 0.0");
//		lblIterationsPerSecond.setMinimumSize(new Dimension(160, 24));
//		lblIterationsPerSecond.setPreferredSize(new Dimension(160, 24));
//		lblIterationsPerSecond.setMaximumSize(new Dimension(160, 24));
        
		labelContainer.add(lblPopulation);
        labelContainer.add(lblScore);
		labelContainer.add(lblIterations);
		labelContainer.add(labelSequential);
		
        lblAverageScore = new JLabel("H: 100.00%");
        
        lblAverageScore.setHorizontalAlignment(SwingConstants.CENTER);
        
        lblAverageScore.setMinimumSize(new Dimension(220, 44));
        lblAverageScore.setPreferredSize(new Dimension(220, 44));
        lblAverageScore.setMaximumSize(new Dimension(220, 44));
        lblAverageScore.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 30));
        
        labelContainer.add(lblAverageScore);
		
		menuContainer.add(labelContainer);
		
		// padding
		menuContainer.setPreferredSize(new Dimension(280, 900));
		menuContainer.setSize(new Dimension(280, 900));
		menuContainer.setMaximumSize(new Dimension(280, 900));

		//
      	container.add(menuContainer, BorderLayout.LINE_END);
      	container.add(imagePanel, BorderLayout.CENTER);
	    
      	// NOTE: set minimum size
      	if (triangleScaleWidth < 1f) {

      		setSize(420, 300);
      		
      	} else {
      	    
      		setSize((int) (325 * triangleScaleWidth), (int) (200 * triangleScaleHeight));
      	}

      	//
		chooser = new JFileChooser(new File(System.getProperty("user.dir")));
		chooser.setAcceptAllFileFilterUsed(false);
		
		//
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
					new ArtEvolver();
				} catch (IOException e) {
					e.printStackTrace();
				}
            }
        });
    }

    public void setOfflineSourceImage(String imageName) throws IOException {
        
        try {
            
            File imageFile = new File(imageName);
            
            originalImage = ImageIO.read(imageFile);
            setPath((imageName));

            // 
            imageSourceName = imageFile.getName();
            
            /**
             * Main Category/Tags
             */
            imageCategory = imageFile.getParentFile().getName();

        } catch (Exception localException) {
            
            JOptionPane.showMessageDialog(null, "Unable to Load Image", "Fail", 2);
        }
        
        //
        setSourceImage();
    }

    public void loadImage() throws IOException {

    	this.chooser.resetChoosableFileFilters();
    	
		this.chooser.setFileFilter(new FileNameExtensionFilter("Image Files", new String[] { "jpg", "jpeg", "png", "gif", "bmp" }));

		if (this.chooser.showOpenDialog(this) == 0) {
		    
			try {
			    
				originalImage = ImageIO.read(new File(chooser.getCurrentDirectory().toString() + "\\"	+ chooser.getSelectedFile().getName()));
				setPath((chooser.getCurrentDirectory().toString() + "\\" + chooser.getSelectedFile().getName()));

				// 
				imageSourceName = chooser.getSelectedFile().getName();

			} catch (Exception localException) {
			    
				JOptionPane.showMessageDialog(null, "Unable to Load Image", "Fail", 2);
			}
		}

		//
		setSourceImage();
    }
    
    public void setSourceImage() {
     // Ignore on Select File Window Close (without picking a file)
        if (originalImage == null) {
            
            return;
        }

        /**
         * Resizing code seems to be OK
         */
        int newWidth = (int) (width * widthTriangles);
        int newHeight = (int) (((height * heightTriangles))  - height); // substract last serrated row

        // initialize currentImage and resizedOriginal
        if (resizedOriginal == null){
            
            BufferedImage resizedOriginal = new BufferedImage(newWidth, newHeight, IMAGE_TYPE);
            
            Graphics2D g = resizedOriginal.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                               RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            
            g.drawImage(originalImage,
                        0, 0,
                        newWidth, newHeight,
                        0, 0,
                        originalImage.getWidth(), 
                        originalImage.getHeight(),
                        null);
            
            g.dispose();

            for (AbstractEvolver currentEvolver : evolvers) {
                ((ImageEvolver)currentEvolver).setResizedOriginal(resizedOriginal);
                ((ImageEvolver)currentEvolver).initializeIsosceles();
            }

            this.resizedOriginal = resizedOriginal;
        }
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    /**
     * TODO: this breaks processing if start is pressed twice (or after stopping)
     */
    public void start(){
        
    	start = System.currentTimeMillis();

    	this.isRunning = true;
    	
    	// run() Evolver instances and as configured by THREADS parameter
		for (AbstractEvolver currentEvolver : evolvers) {

		    // Only start Thread once!
            if (!((ImageEvolver) currentEvolver).isStarted) {

                Thread t = new Thread(currentEvolver);
                t.start();
                
                ((ImageEvolver) currentEvolver).isStarted = true;
            }

			((ImageEvolver)currentEvolver).setRunning(true);
		}
		
    	processTimer.start();
    }
    
    public void stop() {
    	
    	// run() Evolver instances and as configured by THREADS parameter
		for (AbstractEvolver currentEvolver : evolvers) {
		    
			((ImageEvolver)currentEvolver).setRunning(false);
		}
    	
    	this.isRunning = false;
    	
    	processTimer.stop();
    }
    
    /**
     * 
     */
    public void showSource() {
        // Draw Original Image for Comparison TODO: move to different button
        // draw bestImage to panel
        imagePanel.getGraphics().drawImage(resizedOriginal,
                                           32, // TODO: make both offsets dynamic to center in JPanel
                                           32,
                                           null);
        
        imagePanel.getGraphics().dispose();
    }
    
    /**
     * 
     */
    public void renderBestImage() {
        
        String [] splitted = imageSourceName.split("\\.");
        
//        String [] splittedPath = path.split("\\");
        long elapsed = System.currentTimeMillis() - start;
        
        System.out.println(imageSourceName + SEPARATOR
//                + THREADS + SEPARATOR
//                + (THREADS * POPULATION) + SEPARATOR
                + (float) elapsed / 1000f + SEPARATOR
                + totalIterations + SEPARATOR
                + goodIterations + SEPARATOR
                + ((float) goodIterations / (float) totalIterations) + SEPARATOR
                + bestScore + SEPARATOR
                + imageCategory);

        if (EXPORT_ENABLED) {
            Renderer.renderToPNG(bestPop,
                    splitted[0],
                    EXPORT_FOLDER,
                    exportedImages,
                    (int) (width * widthTriangles),
                    (int) (height * (heightTriangles - 1)), // do not render last row
                    IMAGE_TYPE,
                    1);
        }
    }
    
	@Override
	public void stateChanged(ChangeEvent e) {

	}

	@Override
	public void actionPerformed(ActionEvent event) {
		
		if (event.getActionCommand().equals("Load")) {
			
		    try {

		        loadImage();
				
			} catch (IOException e) {
			    
			    // ignore close button
//				e.printStackTrace();
			}
		}
		
		if (event.getActionCommand().equals("Start")) {
			start();
		}
		
		if (event.getActionCommand().equals("Stop")) {
			stop();
		}
		
		if (event.getActionCommand().equals("Sequential")) {
		    
		    sequential = !sequential;
		    
		    labelSequential.setText("Sequential: " + (sequential ? "ON" : "OFF"));
		}
		
        if (event.getActionCommand().equals("Source")) {

            showSource = !showSource;
        }
		
		/**
		 * Export
		 */
		if (event.getActionCommand().equals("Export")) {
		    
		    // 
		    if (imageSourceName != null) {
		        
		        renderBestImage();
		        
		        // 
		        exportedImages++;
		    }
		}
	}
	
	// Returns the new average after including x
    static float getAvg(float prevAvg, float x, int n) {
        
        return (prevAvg * n + x) / (n + 1);
    }
	
    // Returns the average of a stream of numbers
    static float streamAvg(float [] array, int n) {
        
        float avg = 0;
        
        for (int i = 0; i < n; i++) {
            
            avg = getAvg(avg, array[i], i);
        }
        
        return avg;
    }
}