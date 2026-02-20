package com.rndmodgames.evolver;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.rndmodgames.evolver.benchmark.BenchmarkLogger;
import com.rndmodgames.evolver.exporter.PaintByColorsExporter;
import com.rndmodgames.evolver.render.Renderer;

public class ArtEvolver extends JFrame implements ActionListener, ChangeListener {

	private static final long serialVersionUID = 6291204469421642923L;
	
	public JFrame mainFrame;
	private JPanel imagePanel;
	private Palette pallete;
	
	/**
	 * Structured benchmark logging (v3.1). Set BENCHMARK_LOGGING = true to write CSV files.
	 */
	public static boolean BENCHMARK_LOGGING = true;
	public static String BENCHMARK_OUTPUT_DIR = "benchmarks";
	private BenchmarkLogger benchmarkLogger;
	
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
	
	// default to false
	public static boolean TOURNAMENT_MODE_PRINT = false;
	
//	public static int CURRENT_MODE = QUALITY_MODE_STREAM;
	public static int CURRENT_MODE = QUALITY_MODE;
//	public static int CURRENT_MODE = FASTEST_MODE;
	
	// 
	public static boolean HIGH_RESOLUTION_EXPORT = false;
	public static boolean ULTRA_HIGH_RESOLUTION_EXPORT = false;
	public static boolean MEGA_HIGH_RESOLUTION_EXPORT = false;
	public static boolean MASTER_RESOLUTION_EXPORT = false;
	
	// default to false
	public static boolean EXPORT_VIDEO = false;
	public static int EXPORT_VIDEO_FRAMES_FPS = 1;
	public static boolean VIDEO_FULL_HD_RESOLUTION_EXPORT = false;
    public static boolean VIDEO_4K_RESOLUTION_EXPORT = false;
    
    // default to false
    public static boolean VIDEO_REGULAR_QUALITY = false;
    public static boolean VIDEO_GOOD_QUALITY = true;
    
    public static boolean isRendering = false;
	
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
	float triangleScaleHeight = 1.0f; // 0.25f, 0.5f, 0.66f, 0.75f, 1f, 1.25f, 1.5f, 2f, 2.5f, 3f 
	float triangleScaleWidth = 1.0f;

	float width = 2.5f * triangleScaleWidth;
	float height = 2.5f * triangleScaleHeight;
	
	/**
	 * TODO:
	 *     - parametrize and configure for different aspect ratios:
	 *         - 16:9 [80x50]
	 *         - 4:3
	 *         - 1:1
	 *         - 9:16
	 *        
	 * 1ND PALETTE SIZE IS 80x53 ALERT!
	 * 
	 * 2ND PALETTE SIZE IS 1535 colors = 6140 total colors
	 *     - 16:9 FORMAT : 100 x 57
	 *     -  1:1 FORMAT :  78 x 77 = 6006
	 *     - 1.50 RATIO  :  96 x 63 = 6048
	 *     
	 *      1x palettes: total colors =  1535 =  38 x  39 =  1482 triangles = triangle size = 12.0f
	 *      2x palettes: total colors =  3070 =  54 x  55 =  2970 triangles = triangle size =  9.0f 
	 *      3x palettes: total colors =  4605 =  66 x  67 =  4422 triangles = triangle size =  7.0f
	 *      4x palettes: total colors =  6140 =  76 x  77 =  5852 triangles = triangle size =  6.0f
	 *      5x palettes: total colors =  7675 =  86 x  87 =  7482 triangles = triangle size =  5.0f
	 *      6x palettes: total colors =  9210 =  94 x  95 =  8930 triangles = triangle size =  4.5f
	 *      7x
	 *      8x palettes: total colors = 12280 = 110 x 111 = 12210 triangles = triangle size =  4.0f
	 *     16x palettes: total colors = 24560 = 156 x 157 = 24492 triangles = triangle size =  3.0f
	 *     
	 * TRILUX 12 COLORS:
	 *     - 26x16 squares = 416 squares = 1664 triangles
	 *     - 52x33         = 1716 triangle
	 */
	public static int widthTriangles  = 80; // 71
	public static int heightTriangles = 53; // 60
	public static int TOTAL_TRIANGLES = widthTriangles * heightTriangles;

	/**
     * TOTAL_PALLETES
     * 
     *    - This is the number of times each triangle will be subdivided.
     *    - One of the most efficient ways to use each Palette Rectangle is TOTAL_PALLETES = 4
     *      - This will generate 4 triangles for each color.
     *      - Current code uses equilateral triangles resorting in using squared palletes, when the physical ones 
     *          are rectangular, we should take real world measures into consideration if wanting >efficiency over >symetrism
     */
    private int TOTAL_PALLETES              = 4;
	
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
	private boolean HALVE_PARAMETERS_ON_LOW_HEALTH = false;
	
	// will halve parameters if health reaches zero (default is 15)
	private float LOW_HEALTH_HALVE_PARAMETERS_TRESHOLD = 80f; // 7 = good, 4=test
	
	// evolution jumps default to false
	private boolean EVOLUTION_JUMPS_ENABLED = false;
	
	// evolve each 1000 steps default
	private int EVOLVE_HEALTH_CHECKS_ADD_MAX_JUMP_DISTANCE = 2;
	
	// add 1 to max jump distance default
	private int EVOLVE_JUMPS_ADD = 10;
	
	// default to false
	private float lastCheckHealth = -1f;
	private boolean JUMPS_DEPEND_ON_FRESH_HEALTH = false;
	
	// default to 1 (100%)
	private float FRESH_HEALTH_JUMP_PERCENT = 1f;
	
	/**
	 * RENDER GUI / EVOLUTION SPEED
	 */
	private int GUI_FPS = 20; // twitch fps are set to 30
	private int FPS = 40; // 640 or around is the fastest setting
	private int EVOLVER_UPDATE_MS = 1000 / FPS;
//	private int EVOLVER_UPDATE_MS = 0;
	private int GUI_UPDATE_MS = 1000 / GUI_FPS;
	
	/**
	 * Keep track of past n iterations result for better indicators
	 */
	private static final int HEALTH_ITERATIONS = 1000;
	private final float [] GOOD_ITERATIONS  = new float [HEALTH_ITERATIONS];

	private static final int UI_UPDATE_INTERVAL = 40;
	private static final int LOG_INTERVAL_MS = 5000;
	private long lastLogTimeMs = 0;
	
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
	
	private int RANDOM_JUMP_MAX_DISTANCES [] = {
	        TOTAL_TRIANGLES, TOTAL_TRIANGLES, TOTAL_TRIANGLES, TOTAL_TRIANGLES, //  4
	        TOTAL_TRIANGLES, TOTAL_TRIANGLES, TOTAL_TRIANGLES, TOTAL_TRIANGLES, //  8
	        TOTAL_TRIANGLES, TOTAL_TRIANGLES, TOTAL_TRIANGLES, TOTAL_TRIANGLES, // 12
	        TOTAL_TRIANGLES, TOTAL_TRIANGLES, TOTAL_TRIANGLES, TOTAL_TRIANGLES, // 16 
	        TOTAL_TRIANGLES, TOTAL_TRIANGLES, TOTAL_TRIANGLES, TOTAL_TRIANGLES, // 20
	        TOTAL_TRIANGLES, TOTAL_TRIANGLES, TOTAL_TRIANGLES, TOTAL_TRIANGLES, // 24
                                                1, 1, 1, 1, // 28
                                                1, 1, 1, 1, // 32 
	                                            1, 1, 1, 1, // 36
	                                            1, 1, 1, 1, // 40
	                                            1, 1, 1, 1, // 
	                                            1, 1, 1, 1, // 
	                                            1, 1, 1, 1, //
                                                1, 1, 1, 1, //  
                                                1, 1, 1, 1, // 
                                                1, 1, 1, 1, //  
                                                1, 1, 1, 1, // 
                                                1, 1, 1, 1, // 
                                                1, 1, 1, 1, // 
                                                1, 1, 1, 1, // 64
                                                2048, 2048, 2048, 2048, //  4
                                                2048, 2048, 2048, 2048, //  8
                                                2048, 2048, 2048, 2048, // 12
                                                2048, 2048, 2048, 2048, // 16 
                                                1, 1, 1, 1, // 20
                                                1, 1, 1, 1, // 24
                                                1, 1, 1, 1, // 28
                                                1, 1, 1, 1, // 32
                                                //
                                                2048, 2048, 2048, 2048, //
                                                2048, 2048, 2048, 2048, //  
                                                2048, 2048, 2048, 2048, // 
                                                2048, 2048, 2048, 2048, //  
                                                1, 1, 1, 1, // 
                                                1, 1, 1, 1, // 
                                                1, 1, 1, 1, // 
                                                1, 1, 1, 1, // 128
	                                            };
	
	private int CROSSOVERS_MAX [] = {1, 2, 4, 8, 16, 32, 64, 128, 256}; 
	
	public static final int IMAGE_TYPE = BufferedImage.TYPE_INT_ARGB;
	
	/**
	 * TODO: document & optimize
	 * 
	 * EVOLVE_ITERATIONS: 
	 */
	public static int EVOLVE_ITERATIONS    = 2;
	private static int MAX_ITERATIONS      = 10000000;

	private static String SEPARATOR = ";";
	private static String EXPORT_FOLDER = "D:\\Media\\ArtEvolver\\";
	
	private String imageSourceName = null;
	private String imageCategory = null;
	private int exportedImages = 0;

	private List <ImageEvolver> evolvers = new ArrayList<>();

	private final List<TournamentContestant> contestants = new ArrayList<>();
	private TournamentContestant selectedContestant;
	private TournamentManagerWindow tournamentManagerWindow;
	private JComboBox<String> cmbContestant;
	private JComboBox<String> cmbDrawMode;
	private boolean tournamentMode = false;
	private int tournamentDrawMode = 0; // 0=Selected, 1=Best, 2=All

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

	// Enhanced UI components (v3.1)
	private JLabel lblElapsed;
	private JLabel lblIterPerSec;
	private JLabel lblMethod;
	private JLabel lblTriangles;
	private JLabel lblScoreGain;
	private javax.swing.JProgressBar progressBar;
	private JComboBox<String> cmbInitMethod;
	private JComboBox<String> cmbEvolveMethod;
	private JSpinner spnThreads;
	private JSpinner spnPopulation;
	private JSpinner spnGridMutations;
	private JSpinner spnTargetedSwaps;
	private JSpinner spnRandomMutations;
	private JSpinner spnCloseMutations;
	private JSpinner spnCrossoverMax;
	private JSpinner spnPalettes;
	private JSpinner spnGuiFps;
	private JSpinner spnEvolveIterations;
	private javax.swing.JCheckBox chkBlockCrossover;
	private javax.swing.JCheckBox chkValidatePermutation;
	private javax.swing.JCheckBox chkBenchmarkLogging;
	private javax.swing.JCheckBox chkExportVideo;

	private JSpinner spnGridWidth;
	private JSpinner spnGridHeight;
	private JSpinner spnEvolutionFps;
	private JSpinner spnMaxIterations;
	private JSpinner spnMutationDecay;
	private JSpinner spnRandomMutationPct;
	private JSpinner spnCloseMutationPct;
	private JSpinner spnGridMutationPct;

	private FitnessChartWindow fitnessChartWindow;
	private long evolveStartTimeMs;
	private double initialScore = 0.0;
	
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
	 * @throws URISyntaxException 
	 */
    public ArtEvolver() throws IOException, URISyntaxException {
        
        super("ArtEvolver v3.1");

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
    	    
    	    THREADS = 8;
            POPULATION = 3;
            
            triangleScaleHeight = 6f;
            triangleScaleWidth = 6f;
            
            // 4k
//            RANDOM_JUMP_MAX_DISTANCES [0] = 8520 / 2;
//            RANDOM_JUMP_MAX_DISTANCES [1] = 8520 / 2;
//            RANDOM_JUMP_MAX_DISTANCES [2] = 8520 / 2;
//            RANDOM_JUMP_MAX_DISTANCES [3] = 8520 / 2;
//            RANDOM_JUMP_MAX_DISTANCES [0] = 17040 / 2;
//            RANDOM_JUMP_MAX_DISTANCES [1] = 17040 / 2;
//            RANDOM_JUMP_MAX_DISTANCES [2] = 17040 / 2;
//            RANDOM_JUMP_MAX_DISTANCES [3] = 17040 / 2;
//            RANDOM_JUMP_MAX_DISTANCES [4] = 17040 / 2;
//            RANDOM_JUMP_MAX_DISTANCES [5] = 17040 / 2;
//            RANDOM_JUMP_MAX_DISTANCES [6] = 17040 / 2;
//            RANDOM_JUMP_MAX_DISTANCES [7] = 17040 / 2;
//            RANDOM_JUMP_MAX_DISTANCES [8] = 17040 / 2;
//            RANDOM_JUMP_MAX_DISTANCES [9] = 17040 / 2;
//            RANDOM_JUMP_MAX_DISTANCES [10] = 17040 / 2;
//            RANDOM_JUMP_MAX_DISTANCES [11] = 17040 / 2;
//            RANDOM_JUMP_MAX_DISTANCES [12] = 17040 / 2;
//            RANDOM_JUMP_MAX_DISTANCES [13] = 17040 / 2;
//            RANDOM_JUMP_MAX_DISTANCES [14] = 17040 / 2;
//            RANDOM_JUMP_MAX_DISTANCES [15] = 17040 / 2;
            
            if (ULTRA_HIGH_RESOLUTION_EXPORT) {
                
                triangleScaleHeight = 4f;
                triangleScaleWidth = 4f;
                
                // 17k
//                RANDOM_JUMP_MAX_DISTANCES [0] = 17040 / 2;
//                RANDOM_JUMP_MAX_DISTANCES [1] = 17040 / 2;
//                RANDOM_JUMP_MAX_DISTANCES [2] = 17040 / 2;
//                RANDOM_JUMP_MAX_DISTANCES [3] = 17040 / 2;
//                RANDOM_JUMP_MAX_DISTANCES [4] = 17040 / 2;
//                RANDOM_JUMP_MAX_DISTANCES [5] = 17040 / 2;
//                RANDOM_JUMP_MAX_DISTANCES [6] = 17040 / 2;
//                RANDOM_JUMP_MAX_DISTANCES [7] = 17040 / 2;
//                RANDOM_JUMP_MAX_DISTANCES [8] = 17040 / 2;
//                RANDOM_JUMP_MAX_DISTANCES [9] = 17040 / 2;
//                RANDOM_JUMP_MAX_DISTANCES [10] = 17040 / 2;
//                RANDOM_JUMP_MAX_DISTANCES [11] = 17040 / 2;
//                RANDOM_JUMP_MAX_DISTANCES [12] = 17040 / 2;
//                RANDOM_JUMP_MAX_DISTANCES [13] = 17040 / 2;
//                RANDOM_JUMP_MAX_DISTANCES [14] = 17040 / 2;
//                RANDOM_JUMP_MAX_DISTANCES [15] = 17040 / 2;
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
            
            // 3840 x 2160 target resolution
            if (VIDEO_4K_RESOLUTION_EXPORT) {
                
                // 4 k triangles (default) BAD_QUALITY
                triangleScaleWidth = 1f;
                triangleScaleHeight = 1f;
                RENDERING_SCALE = 16;
                
                if (VIDEO_REGULAR_QUALITY) {
                    
                    triangleScaleWidth = 2f;
                    triangleScaleHeight = 2f;
                    RENDERING_SCALE = 8;
                }
                
                if (VIDEO_GOOD_QUALITY) {
                    
                    triangleScaleWidth = 4f;
                    triangleScaleHeight = 4f;
                    RENDERING_SCALE = 4;
                }
            }
            
            // 1920 x 1248
            if (VIDEO_FULL_HD_RESOLUTION_EXPORT) {

                // 4 k triangles (default)
                RENDERING_SCALE = 1;
            }
            
            width = 3.0f * triangleScaleWidth;
            height = 3.0f * triangleScaleHeight;
            
            break;
            
    	case QUALITY_MODE:

    	    THREADS = 24;
    	    POPULATION = 2;
            triangleScaleHeight = 3f;
            triangleScaleWidth = 3f;
            
            if (MASTER_RESOLUTION_EXPORT) {
                triangleScaleHeight = 2f;
                triangleScaleWidth = 2f;
            }
            
            width = 2f * triangleScaleWidth;
            height = 2f * triangleScaleHeight;
              
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
    	    
    	    POPULATION = 2;
            THREADS = 17; // default to 17
            
//            MAX_ITERATIONS = 2500;
//            triangleScaleHeight = 0.5f;
//            triangleScaleWidth = 0.5f;
            triangleScaleHeight = 1f;
            triangleScaleWidth = 1f;
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

        if (!GraphicsEnvironment.isHeadless()) {
            initComponents();
        }
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
				
				// --- Single mode: iterate legacy evolvers ---
				for (AbstractEvolver currentEvolver : evolvers) {
				    ((ImageEvolver)currentEvolver).setSecuential(sequential);
					totalIterations += ((ImageEvolver)currentEvolver).getTotalIterations();
					goodIterations += ((ImageEvolver)currentEvolver).getGoodIterations();
					maxJumpDistanceSum += ((ImageEvolver)currentEvolver).getRandomJumpDistance();

					if (((ImageEvolver)currentEvolver).isDirty()) {
						if (((ImageEvolver)currentEvolver).getBestScore() > bestScore) {
							bestScore = ((ImageEvolver)currentEvolver).getBestScore();
							bestImage = ((ImageEvolver)currentEvolver).getBestImage();
							try {
							    bestPop = ((ImageEvolver)currentEvolver).getBestPop();
							} catch (IndexOutOfBoundsException eb) {
							    // ignore
							}
							isDirty = true;
    					}
						((ImageEvolver)currentEvolver).setDirty(false);
					}
				}

				// --- Tournament mode: iterate contestants ---
				if (tournamentMode && !contestants.isEmpty()) {
				    long tTotalIter = 0;
				    long tGoodIter = 0;
				    TournamentContestant bestContestant = null;
				    for (TournamentContestant c : contestants) {
				        if (c.isFinished()) continue;
				        c.updateBest();
				        if (c.getBestScore() > 0) {
				            c.getFitnessTracker().addSnapshot(c.getBestScore(), c.getTotalIterations());
				        }
				        c.checkStageTransition();
				        tTotalIter += c.getTotalIterations();
				        tGoodIter += c.getGoodIterations();
				        if (bestContestant == null || c.getBestScore() > bestContestant.getBestScore()) {
				            bestContestant = c;
				        }
				    }
				    totalIterations += tTotalIter;
				    goodIterations += tGoodIter;

				    if (tournamentDrawMode == 2) {
				        // Draw All: just mark dirty to trigger full grid repaint
				        isDirty = true;
				        if (bestContestant != null) bestScore = bestContestant.getBestScore();
				    } else if (tournamentDrawMode == 1) {
				        // Draw Best: always show the highest-scoring contestant
				        if (bestContestant != null && bestContestant.getBestImage() != null) {
				            bestScore = bestContestant.getBestScore();
				            bestImage = bestContestant.getBestImage();
				            isDirty = true;
				        }
				    } else {
				        // Draw Selected: show selected or fallback to best
				        TournamentContestant target = (selectedContestant != null) ? selectedContestant : bestContestant;
				        if (target != null && target.getBestImage() != null) {
				            bestScore = Math.max(bestScore, target.getBestScore());
				            bestImage = target.getBestImage();
				            isDirty = true;
				        }
				    }
				}
				
				// draw bestImage to panel
				if (showSource) {
				    showSource();
				} else {
				    int guiFrameSkip = Math.max(1, FPS / GUI_FPS);
				    if (tournamentMode && tournamentDrawMode == 2) {
				        guiFrameSkip = Math.max(guiFrameSkip, 3);
				    }
				    if (currentFrame % guiFrameSkip == 0 && isDirty) {
	                    isDirty = false;
	                    imagePanel.repaint();
	                }
				}
				
				// sync best images for single mode evolvers
				int population = 0;
            	for (AbstractEvolver currentEvolver : evolvers) {
            		if (((ImageEvolver)currentEvolver).getBestScore() < bestScore) {
            			((ImageEvolver)currentEvolver).setBestPop(bestPop);
            		}
            		population += ((ImageEvolver)currentEvolver).getPopulation().size();
            	}
            	// count tournament population too
            	for (TournamentContestant c : contestants) {
            	    for (ImageEvolver ev : c.getEvolvers()) {
            	        population += ev.getPopulation().size();
            	    }
            	}
            	
            	/**
            	 * Keep track of the last n iterations and good iteration count
            	 */
//            	float healthScore = ((float) goodIterations / (float) totalIterations) * 100f;
            	
            	// keep track of only the NEW GOOD ITERATIONS
                GOOD_ITERATIONS[(int) (currentFrame % HEALTH_ITERATIONS)] = (float) (goodIterations - prevGoodIterations) * 100;

            	/**
            	 * Update UI labels frequently (~1 second intervals)
            	 */
            	if (currentFrame % UI_UPDATE_INTERVAL == 0) {

            	    float health = streamAvg(GOOD_ITERATIONS, Math.min((int) currentFrame, HEALTH_ITERATIONS));

            	    lblScore.setText(df4.format(bestScore * 100f) + PERCENT_SIGN);
            	    int progressVal = (int) (bestScore * 10000);
            	    progressBar.setValue(Math.min(progressVal, 10000));
            	    progressBar.setString(df4.format(bestScore * 100f) + PERCENT_SIGN);

            	    if (goodIterations > 0 && totalIterations > 0) {
                        lblAverageScore.setText("Health: " + df.format(health) + PERCENT_SIGN);
                    }

                    double gain = bestScore - initialScore;
                    if (gain > 0) {
                        lblScoreGain.setText("Gain: +" + df4.format(gain * 100f) + PERCENT_SIGN);
                        lblScoreGain.setForeground(new java.awt.Color(34, 139, 34));
                    } else {
                        lblScoreGain.setText("Gain: --");
                    }

                    lblPopulation.setText("Population: " + population + " (" + evolvers.size() + " threads)");
                    lblIterations.setText("Iterations: " + goodIterations + " / " + totalIterations);
                    lblTriangles.setText("Triangles: " + TOTAL_TRIANGLES + " (" + widthTriangles + "x" + heightTriangles + ")");

                    if (evolveStartTimeMs > 0) {
                        long elapsedSec = (System.currentTimeMillis() - evolveStartTimeMs) / 1000;
                        long hrs = elapsedSec / 3600;
                        long mins = (elapsedSec % 3600) / 60;
                        long secs = elapsedSec % 60;
                        if (hrs > 0) {
                            lblElapsed.setText("Elapsed: " + hrs + "h " + mins + "m " + secs + "s");
                        } else if (mins > 0) {
                            lblElapsed.setText("Elapsed: " + mins + "m " + secs + "s");
                        } else {
                            lblElapsed.setText("Elapsed: " + secs + "s");
                        }

                        if (elapsedSec > 0) {
                            long ips = totalIterations / elapsedSec;
                            lblIterPerSec.setText("Speed: " + String.format("%,d", ips) + " iter/sec");
                        }
                    }

                    String[] initNames = {"Random", "Smart Greedy", "LAP Optimal"};
                    String[] evolveNames = {"Legacy", "Delta"};
                    int initIdx = Math.min(cmbInitMethod.getSelectedIndex(), initNames.length - 1);
                    int evolveIdx = Math.min(cmbEvolveMethod.getSelectedIndex(), evolveNames.length - 1);
                    lblMethod.setText("Mode: " + initNames[initIdx] + " + " + evolveNames[evolveIdx]);

                    if (fitnessChartWindow != null && fitnessChartWindow.isVisible()) {
                        if (tournamentMode && !contestants.isEmpty()) {
                            for (TournamentContestant c : contestants) {
                                if (c.isFinished()) continue;
                                if (c.getBestScore() > 0) {
                                    fitnessChartWindow.addDataPoint(c.getId(), c.getName(),
                                        c.getTotalIterations(), c.getBestScore(), c.getChartColor());
                                }
                            }
                        } else if (bestScore > 0) {
                            fitnessChartWindow.addDataPoint(totalIterations, bestScore);
                        }
                    }

                    if (tournamentMode && tournamentManagerWindow != null
                            && tournamentManagerWindow.isVisible()) {
                        tournamentManagerWindow.refreshTable();
                        tournamentManagerWindow.refreshEvolutionaryState();
                        refreshContestantCombo();
                    }
            	}

            	/**
            	 * Health checks, parameter adjustments, logging at HEALTH_ITERATIONS intervals
            	 */
            	if (currentFrame % HEALTH_ITERATIONS == 0) {

                    float health = streamAvg(GOOD_ITERATIONS, HEALTH_ITERATIONS);

                    /**
                     * DYNAMIC HEALTH CHECK
                     */
                    if (HALVE_PARAMETERS_ON_LOW_HEALTH) {
                        
                        if (health <= LOW_HEALTH_HALVE_PARAMETERS_TRESHOLD) {

                            for (AbstractEvolver currentEvolver : evolvers) {
                                
//                                ((ImageEvolver) currentEvolver).halveGeneralParameters();
                                ((ImageEvolver) currentEvolver).raiseMaxJumpDistance(EVOLVE_JUMPS_ADD);
                            }
                        }
                    }
                    
                    if (EVOLUTION_JUMPS_ENABLED) {

                        if (currentFrame % (EVOLVE_HEALTH_CHECKS_ADD_MAX_JUMP_DISTANCE * HEALTH_ITERATIONS) == 0) {

                            for (AbstractEvolver currentEvolver : evolvers) {

                                ((ImageEvolver) currentEvolver).raiseMaxJumpDistance(-EVOLVE_JUMPS_ADD);
                                
                                if (((ImageEvolver) currentEvolver).getRandomJumpDistance() < 0) {
                                    
                                    ((ImageEvolver) currentEvolver).setRandomJumpDistance(1);
                                }
                            }
                        }
                    }
                    
                    if (JUMPS_DEPEND_ON_FRESH_HEALTH) {
                        
                        float healthDifference = lastCheckHealth - health;
                        
                        for (AbstractEvolver currentEvolver : evolvers) {
                         
                            ((ImageEvolver) currentEvolver).raiseMaxJumpDistance((int) healthDifference);
                            
                            if (((ImageEvolver) currentEvolver).getRandomJumpDistance() < 0) {
                                
                                ((ImageEvolver) currentEvolver).setRandomJumpDistance(1);
                            }
                        }
                        
                        lastCheckHealth = health;
                    }

                    // TOURNAMENT PRINT
                    if (TOURNAMENT_MODE_PRINT) {
                        for (AbstractEvolver currentEvolver : evolvers) {
                            System.out.print(((ImageEvolver) currentEvolver).getBestScore() + ",");
                        }
                        System.out.println();
                    }

                    if (benchmarkLogger != null) {
                        benchmarkLogger.record(totalIterations, goodIterations, health,
                                bestScore, THREADS, POPULATION, TOTAL_TRIANGLES,
                                MODES[CURRENT_MODE] != null ? MODES[CURRENT_MODE] : "CUSTOM");
                    }
            	}

            	/**
            	 * Console progress log every LOG_INTERVAL_MS (independent of health check)
            	 */
            	{
            	    long nowMs = System.currentTimeMillis();
            	    if (nowMs - lastLogTimeMs >= LOG_INTERVAL_MS) {
            	        lastLogTimeMs = nowMs;
            	        float health = streamAvg(GOOD_ITERATIONS, Math.min((int) currentFrame, HEALTH_ITERATIONS));
            	        System.out.println("[ArtEvolver] Score: " + df4.format(bestScore * 100f) + "%"
            	                + " | Health: " + df.format(health) + "%"
            	                + " | Iter: " + goodIterations + "/" + totalIterations
            	                + " | Pop: " + population
            	                + " | Threads: " + THREADS);
            	    }
            	}
            	
            	/**
            	 * EXPORT n FRAMES per second
            	 */
            	int exportInterval = EXPORT_VIDEO_FRAMES_FPS > 0 ? Math.max(1, FPS / EXPORT_VIDEO_FRAMES_FPS) : FPS;
            	if (EXPORT_VIDEO && currentFrame % exportInterval == 0) {
            	    
            	    if (isRendering) {
            	        
            	    } else {
            	        isRendering = true;
            	        
                        renderBestImage();
                        exportedImages++;
            	    }
            	}
            	
            	currentFrame++;
            	prevGoodIterations = goodIterations;
            	
            	/**
            	 * Close and Export for Quick and Quick Extended Modes
            	 */
//            	if (CURRENT_MODE != QUALITY_MODE 
//            	        && CURRENT_MODE != QUALITY_MODE_FULL_THREADS) {
//            	    
//            	    if (totalIterations >= MAX_ITERATIONS) {
//                        
//            	        if (!offlineExport) {
//            	        
//                            //
//            	            renderBestImage();
//
//                            /**
//                             * Only call System.exit if required
//                             * 
//                             * TODO: app keeps running on standalone mode (with window invisible and disposed). FIX
//                             */
//                            setVisible(false);
//                            dispose();
//                            
//                            offlineExport = true;
//            	        }
//                    }
//            	}
            }
        });
        
        Container container = getContentPane();

		imagePanel = new JPanel() {
            private static final long serialVersionUID = -1275189729010345619L;
            @Override
	        protected void paintComponent(Graphics g) {
	            super.paintComponent(g);
	            if (tournamentMode && tournamentDrawMode == 2 && !contestants.isEmpty()) {
	                paintAllContestants((Graphics2D) g, getWidth(), getHeight());
	            } else if (bestImage != null) {
	                g.drawImage(bestImage, 16, 16, null);
	            }
	        }
	    };

	    JPanel sidebar = buildSidebarPanel();

	    JScrollPane scrollPane = new JScrollPane(sidebar,
	        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
	        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	    scrollPane.setMinimumSize(new Dimension(310, 300));
	    scrollPane.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0,
	        new java.awt.Color(180, 180, 180)));
	    scrollPane.getVerticalScrollBar().setUnitIncrement(16);

      	container.add(scrollPane, BorderLayout.LINE_END);
      	container.add(imagePanel, BorderLayout.CENTER);

      	int imageW = (int) (width * widthTriangles) + 32;
      	int imageH = (int) (height * heightTriangles - height) + 32;
      	int sidebarW = 320;

      	Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
      	java.awt.Insets insets = java.awt.Toolkit.getDefaultToolkit().getScreenInsets(
      	    getGraphicsConfiguration());
      	int usableW = screenSize.width - insets.left - insets.right;
      	int usableH = screenSize.height - insets.top - insets.bottom;

      	int winW = Math.min(imageW + sidebarW, usableW);
      	int winH = Math.min(Math.max(imageH, 700), usableH);

      	setSize(winW, winH);
      	scrollPane.setPreferredSize(new Dimension(sidebarW, winH));

		File defaultDir = new File("C:\\Media\\Art Evolver Stream");
		if (!defaultDir.exists()) {
			defaultDir = new File(System.getProperty("user.dir"));
		}
		chooser = new JFileChooser(defaultDir);
		chooser.setAcceptAllFileFilterUsed(false);
		chooser.setAccessory(new ImagePreviewPanel(chooser));
		
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
				} catch (URISyntaxException e) {
                    // TODO Auto-generated catch block
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

    /**
     * Default Load Image method, this is called during normal execution of the App
     * 
     * @throws IOException
     */
    public void loadImage() throws IOException {

    	this.chooser.resetChoosableFileFilters();
    	
		this.chooser.setFileFilter(new FileNameExtensionFilter("Image Files", new String[] { "jpg", "jpeg", "png", "gif", "bmp" }));

		if (this.chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {

			try {
				File selected = chooser.getSelectedFile();
				originalImage = ImageIO.read(selected);
				setPath(selected.getAbsolutePath());
				imageSourceName = selected.getName();

			} catch (Exception localException) {
				JOptionPane.showMessageDialog(null, "Unable to Load Image: " + localException.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
        if (getResizedOriginal() == null){
            
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

            System.out.println("[ArtEvolver] Image loaded: " + originalImage.getWidth() + "x" + originalImage.getHeight()
                    + " -> resized to " + newWidth + "x" + newHeight);
            System.out.println("[ArtEvolver] Initializing " + evolvers.size() + " evolvers with "
                    + (widthTriangles * heightTriangles) + " triangles each...");

            long initStart = System.currentTimeMillis();
            for (AbstractEvolver currentEvolver : evolvers) {
                ((ImageEvolver)currentEvolver).setResizedOriginal(resizedOriginal);
                ((ImageEvolver)currentEvolver).initializeIsosceles();
                ((ImageEvolver)currentEvolver).initDeltaEngine();
            }
            long initTime = System.currentTimeMillis() - initStart;

            double initScore = evolvers.isEmpty() ? 0 : ((ImageEvolver) evolvers.get(0)).getBestScore();
            System.out.println("[ArtEvolver] Initialization complete in " + initTime + "ms"
                    + " | Initial score: " + df4.format(initScore * 100f) + "%");

            this.setResizedOriginal(resizedOriginal);
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

    	applyUISettings();

    	start = System.currentTimeMillis();
    	lastLogTimeMs = start;
    	evolveStartTimeMs = start;

    	String[] initNames = {"Random", "Smart Greedy", "LAP Optimal"};
    	String[] evolveNames = {"Legacy", "Delta Fitness"};
    	int initIdx = Math.min(ImageEvolver.INITIALIZATION_METHOD, initNames.length - 1);
    	int evolveIdx = Math.min(cmbEvolveMethod.getSelectedIndex(), evolveNames.length - 1);
    	System.out.println("[ArtEvolver] Starting evolution...");
    	System.out.println("[ArtEvolver] Init: " + initNames[initIdx]
    	        + " | Evolve: " + evolveNames[evolveIdx]
    	        + " | Threads: " + THREADS
    	        + " | Population: " + POPULATION
    	        + " | Triangles: " + TOTAL_TRIANGLES
    	        + " | Grid=" + CrossOver.GRID_MUTATION_CHANCES
    	        + " | Random=" + CrossOver.RANDOM_MUTATION_CHANCES
    	        + " | Close=" + CrossOver.RANDOM_CLOSE_MUTATION_CHANCES
    	        + " | Targeted=" + CrossOver.TARGETED_SWAP_ATTEMPTS
    	        + " | Block=" + CrossOver.CROSSOVER_BLOCK_ENABLED);

    	this.isRunning = true;
    	
    	if (BENCHMARK_LOGGING && benchmarkLogger == null) {
    	    try {
    	        String runLabel = MODES[CURRENT_MODE] != null ? MODES[CURRENT_MODE] : "mode_" + CURRENT_MODE;
    	        benchmarkLogger = new BenchmarkLogger(BENCHMARK_OUTPUT_DIR, runLabel);
    	    } catch (IOException ex) {
    	        System.err.println("Failed to initialize benchmark logger: " + ex.getMessage());
    	    }
    	}
    	
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
    	
    	if (benchmarkLogger != null) {
    	    System.out.println(benchmarkLogger.generateSummary());
    	    benchmarkLogger.close();
    	    benchmarkLogger = null;
    	}
    }
    
    /**
     * 
     */
    public void showSource() {
        // Draw Original Image for Comparison TODO: move to different button
        // draw bestImage to panel
        imagePanel.getGraphics().drawImage(getResizedOriginal(),
                                           32, // TODO: make both offsets dynamic to center in JPanel
                                           32,
                                           null);
        
        imagePanel.getGraphics().dispose();
    }
    
    /**
     * 
     */
    // TODO MOVE UP WITH OTHER RENDERING PARAMETERS
    int RENDERING_SCALE = 1; // default 1
    
    public void renderBestImage() {
        
        String [] splitted = imageSourceName.split("\\.");
        
//        String [] splittedPath = path.split("\\");
//        long elapsed = System.currentTimeMillis() - start;
        
//        System.out.println(imageSourceName + SEPARATOR
////                + THREADS + SEPARATOR
////                + (THREADS * POPULATION) + SEPARATOR
//                + (float) elapsed / 1000f + SEPARATOR
//                + totalIterations + SEPARATOR
//                + goodIterations + SEPARATOR
//                + ((float) goodIterations / (float) totalIterations) + SEPARATOR
//                + bestScore + SEPARATOR
//                + imageCategory);

        if (EXPORT_ENABLED) {
            Renderer.renderToPNG(bestPop,
                    splitted[0],
                    EXPORT_FOLDER,
                    exportedImages,
                    (int) (width * widthTriangles),
                    (int) (height * (heightTriangles - 1)), // do not render last row
                    IMAGE_TYPE,
                    RENDERING_SCALE);
        }
        
        ArtEvolver.isRendering = false;
    }

    /**
     * Exports a high-resolution paint-by-numbers PNG with color names
     * rendered on each triangle, plus a standard colored version.
     */
    private void exportPaintByNumbers() {
        if (bestPop == null || bestPop.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this,
                "No evolution data to export. Load an image and run evolution first.",
                "Export", javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }

        int missing = PaintByColorsExporter.validateAssignment(bestPop);
        if (missing > 0) {
            System.out.println("[Export] " + missing + " triangles without palette color — names will show as blank.");
        }

        String baseName = imageSourceName != null ? imageSourceName.split("\\.")[0] : "artevolver";
        float exportScale = Math.max(RENDERING_SCALE, 4);

        Renderer.renderPaintByNumbersPNG(bestPop, baseName, EXPORT_FOLDER, exportedImages,
                (int) (width * widthTriangles),
                (int) (height * (heightTriangles - 1)),
                IMAGE_TYPE, exportScale);

        javax.swing.JOptionPane.showMessageDialog(this,
            "Paint-by-numbers image exported to:\n" + EXPORT_FOLDER + baseName + "_pbn_" + exportedImages + ".png",
            "Export Complete", javax.swing.JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Exports a clean outline-only guide with color names inside each triangle.
     */
    private void exportOutlineGuide() {
        if (bestPop == null || bestPop.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this,
                "No evolution data to export. Load an image and run evolution first.",
                "Export", javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }

        String baseName = imageSourceName != null ? imageSourceName.split("\\.")[0] : "artevolver";
        float exportScale = Math.max(RENDERING_SCALE, 4);

        Renderer.renderOutlineGuidePNG(bestPop, baseName, EXPORT_FOLDER, exportedImages,
                (int) (width * widthTriangles),
                (int) (height * (heightTriangles - 1)),
                IMAGE_TYPE, exportScale);

        javax.swing.JOptionPane.showMessageDialog(this,
            "Outline guide exported to:\n" + EXPORT_FOLDER + baseName + "_guide_" + exportedImages + ".png",
            "Export Complete", javax.swing.JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Exports the color mapping CSV and materials list.
     */
    private void exportColorMapCSV() {
        if (bestPop == null || bestPop.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this,
                "No evolution data to export. Load an image and run evolution first.",
                "Export", javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }

        String baseName = imageSourceName != null ? imageSourceName.split("\\.")[0] : "artevolver";

        PaintByColorsExporter.validateAssignment(bestPop);
        boolean csv = PaintByColorsExporter.exportToCSV(bestPop, EXPORT_FOLDER, baseName);
        boolean mat = PaintByColorsExporter.exportMaterialsList(bestPop, EXPORT_FOLDER, baseName);

        String msg = "Export results:\n";
        msg += csv ? "  Color map CSV: OK\n" : "  Color map CSV: FAILED\n";
        msg += mat ? "  Materials list: OK" : "  Materials list: FAILED";

        javax.swing.JOptionPane.showMessageDialog(this, msg,
            "CSV Export", javax.swing.JOptionPane.INFORMATION_MESSAGE);
    }

	@Override
	public void stateChanged(ChangeEvent e) {

	}

	// ============================================================
	//  UI Builder helpers
	// ============================================================

	private static final java.awt.Color ACCENT       = new java.awt.Color(40, 120, 200);
	private static final java.awt.Color BG_SIDEBAR   = new java.awt.Color(245, 245, 248);
	private static final java.awt.Color SECTION_BG   = new java.awt.Color(235, 235, 240);
	private static final java.awt.Color SEPARATOR_CLR = new java.awt.Color(200, 200, 210);

	private static final Font FNT_SECTION = new Font(Font.SANS_SERIF, Font.BOLD, 11);
	private static final Font FNT_LABEL   = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
	private static final Font FNT_VALUE   = new Font(Font.MONOSPACED, Font.BOLD, 16);
	private static final Font FNT_SCORE   = new Font(Font.MONOSPACED, Font.BOLD, 22);
	private static final Font FNT_BTN     = new Font(Font.SANS_SERIF, Font.BOLD, 12);
	private static final Dimension BTN_SIZE  = new Dimension(280, 30);
	private static final Dimension CTRL_SIZE = new Dimension(280, 26);

	private JPanel buildSidebarPanel() {
	    JPanel sb = new JPanel();
	    sb.setLayout(new BoxLayout(sb, BoxLayout.Y_AXIS));
	    sb.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	    sb.setBackground(BG_SIDEBAR);

	    // ── STATUS ──────────────────────────────────
	    addSection(sb, "\u25B6  STATUS");

	    lblScore = makeValueLabel("Score: --", FNT_SCORE, ACCENT);
	    sb.add(lblScore);
	    sb.add(Box.createVerticalStrut(4));

	    progressBar = new javax.swing.JProgressBar(0, 10000);
	    progressBar.setStringPainted(true);
	    progressBar.setString("0.00%");
	    progressBar.setFont(FNT_LABEL);
	    progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
	    progressBar.setMaximumSize(CTRL_SIZE);
	    progressBar.setPreferredSize(new Dimension(280, 18));
	    sb.add(progressBar);
	    sb.add(Box.createVerticalStrut(6));

	    lblScoreGain = makeValueLabel("Gain: --", FNT_LABEL, null);
	    sb.add(lblScoreGain);

	    lblAverageScore = makeValueLabel("Health: --", FNT_LABEL, null);
	    sb.add(lblAverageScore);

	    lblIterations = makeValueLabel("Iterations: 0 / 0", FNT_LABEL, null);
	    sb.add(lblIterations);

	    lblIterPerSec = makeValueLabel("Speed: -- iter/sec", FNT_LABEL, null);
	    sb.add(lblIterPerSec);

	    lblElapsed = makeValueLabel("Elapsed: 0s", FNT_LABEL, null);
	    sb.add(lblElapsed);

	    lblPopulation = makeValueLabel("Population: 0", FNT_LABEL, null);
	    sb.add(lblPopulation);

	    lblTriangles = makeValueLabel("Triangles: " + TOTAL_TRIANGLES, FNT_LABEL, null);
	    sb.add(lblTriangles);

	    lblMethod = makeValueLabel("Method: --", FNT_LABEL, null);
	    sb.add(lblMethod);

	    labelSequential = new JLabel("");

	    sb.add(Box.createVerticalStrut(4));

	    JButton btnChart = makeStyledButton("\u2197 Show Fitness Chart");
	    btnChart.setAlignmentX(Component.LEFT_ALIGNMENT);
	    btnChart.setMaximumSize(BTN_SIZE);
	    btnChart.setToolTipText("Open a separate window with a real-time fitness progression chart.");
	    btnChart.addActionListener(e -> showFitnessChart());
	    sb.add(btnChart);

	    addSeparator(sb);

	    // ── ACTIONS ─────────────────────────────────
	    addSection(sb, "\u25B6  ACTIONS");

	    JPanel btnRow1 = new JPanel(new java.awt.GridLayout(1, 2, 4, 0));
	    btnRow1.setOpaque(false);
	    btnRow1.setAlignmentX(Component.LEFT_ALIGNMENT);
	    btnRow1.setMaximumSize(new Dimension(280, 32));

	    JButton loadButton = makeStyledButton("Load Image");
	    loadButton.addActionListener(this);
	    loadButton.setActionCommand("Load");
	    btnRow1.add(loadButton);

	    JButton sourceBtn = makeStyledButton("Toggle Source");
	    sourceBtn.addActionListener(this);
	    sourceBtn.setActionCommand("Source");
	    btnRow1.add(sourceBtn);
	    sb.add(btnRow1);
	    sb.add(Box.createVerticalStrut(4));

	    JPanel btnRow2 = new JPanel(new java.awt.GridLayout(1, 3, 4, 0));
	    btnRow2.setOpaque(false);
	    btnRow2.setAlignmentX(Component.LEFT_ALIGNMENT);
	    btnRow2.setMaximumSize(new Dimension(280, 32));

	    JButton startButton = makeStyledButton("\u25B6 Start");
	    startButton.setBackground(new java.awt.Color(46, 139, 87));
	    startButton.setForeground(java.awt.Color.WHITE);
	    startButton.addActionListener(this);
	    startButton.setActionCommand("Start");
	    btnRow2.add(startButton);

	    JButton stopButton = makeStyledButton("\u25A0 Stop");
	    stopButton.setBackground(new java.awt.Color(178, 34, 34));
	    stopButton.setForeground(java.awt.Color.WHITE);
	    stopButton.addActionListener(this);
	    stopButton.setActionCommand("Stop");
	    btnRow2.add(stopButton);

	    JButton exportBtn = makeStyledButton("Export");
	    exportBtn.addActionListener(this);
	    exportBtn.setActionCommand("Export");
	    btnRow2.add(exportBtn);
	    sb.add(btnRow2);

	    addSeparator(sb);

	    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
	    //  TOURNAMENT MODE
	    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
	    addSection(sb, "\uD83C\uDFC6  TOURNAMENT MODE");

	    JButton btnTournament = makeStyledButton("Open Tournament Manager");
	    btnTournament.setAlignmentX(Component.LEFT_ALIGNMENT);
	    btnTournament.setMaximumSize(BTN_SIZE);
	    btnTournament.setToolTipText("<html>Manage multiple independent evolution runs<br>" +
	        "with different parameters competing simultaneously.</html>");
	    btnTournament.addActionListener(e -> showTournamentManager());
	    sb.add(btnTournament);
	    sb.add(Box.createVerticalStrut(4));

	    addFieldLabel(sb, "Active Contestant:");
	    cmbContestant = new JComboBox<>(new String[]{"(single mode)"});
	    cmbContestant.setToolTipText("Select which contestant's image to display.");
	    styleCombo(cmbContestant);
	    cmbContestant.addActionListener(e -> onContestantSelected());
	    sb.add(cmbContestant);
	    sb.add(Box.createVerticalStrut(4));

	    addFieldLabel(sb, "Display Mode:");
	    cmbDrawMode = new JComboBox<>(new String[]{"Draw Selected", "Draw Best", "Draw All"});
	    cmbDrawMode.setToolTipText("<html>How to render contestants on the main panel:<br>" +
	        "<b>Draw Selected</b> — shows the contestant picked above<br>" +
	        "<b>Draw Best</b> — always shows the highest-scoring contestant<br>" +
	        "<b>Draw All</b> — grid of all contestants (lower frame rate)</html>");
	    styleCombo(cmbDrawMode);
	    cmbDrawMode.addActionListener(e -> {
	        tournamentDrawMode = cmbDrawMode.getSelectedIndex();
	        isDirty = true;
	    });
	    sb.add(cmbDrawMode);

	    addSeparator(sb);

	    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
	    //  QUALITY — Image size, grid, resolution
	    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
	    addSection(sb, "\uD83D\uDDBC  QUALITY");

	    addFieldLabel(sb, "Grid Width (triangle columns):");
	    spnGridWidth = makeSpinner(widthTriangles, 10, 400, 2,
	        "<html>Triangle grid width. Default 80 for 4-palette (~40 visual columns).<br>" +
	        "<b>Requires image reload to take effect.</b></html>");
	    sb.add(spnGridWidth);
	    sb.add(Box.createVerticalStrut(4));

	    addFieldLabel(sb, "Grid Height (triangle rows):");
	    spnGridHeight = makeSpinner(heightTriangles, 10, 300, 1,
	        "<html>Triangle grid height. Default 53 for 4-palette (~26 visual rows).<br>" +
	        "<b>Requires image reload to take effect.</b></html>");
	    sb.add(spnGridHeight);
	    sb.add(Box.createVerticalStrut(4));

	    addFieldLabel(sb, "Palette Repetitions (1 = 1535 colors):");
	    spnPalettes = makeSpinner(TOTAL_PALLETES, 1, 64, 1,
	        "<html>Number of palette copies. More = finer detail but slower.<br>" +
	        "1\u00D71535=1535 | 4\u00D71535=6140 | 8\u00D71535=12280<br>" +
	        "<b>Requires image reload to take effect.</b></html>");
	    sb.add(spnPalettes);
	    sb.add(Box.createVerticalStrut(4));

	    addFieldLabel(sb, "Color Assignment Method:");
	    cmbInitMethod = new JComboBox<>(new String[]{
	        "Random Shuffle",
	        "Smart Greedy (fast heuristic)",
	        "LAP Optimal (Jonker-Volgenant)"
	    });
	    cmbInitMethod.setSelectedIndex(1);
	    cmbInitMethod.setToolTipText(
	        "<html>Random: shuffled palette<br>" +
	        "Smart Greedy: O(N\u00B2) heuristic, good starting point<br>" +
	        "LAP Optimal: O(N\u00B3) exact solver, provably best color assignment</html>");
	    styleCombo(cmbInitMethod);
	    sb.add(cmbInitMethod);

	    addSeparator(sb);

	    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
	    //  GENETIC ALGORITHM — Population, mutation, crossover
	    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
	    addSection(sb, "\uD83E\uDDEC  GENETIC ALGORITHM");

	    addFieldLabel(sb, "Evolution Method:");
	    cmbEvolveMethod = new JComboBox<>(new String[]{
	        "Legacy (render + compare)",
	        "Delta Fitness (50x faster)"
	    });
	    cmbEvolveMethod.setSelectedIndex(1);
	    cmbEvolveMethod.setToolTipText(
	        "<html>Legacy: renders full image each iteration (slow but simple)<br>" +
	        "Delta: computes only affected pixels per swap (50x throughput)</html>");
	    styleCombo(cmbEvolveMethod);
	    sb.add(cmbEvolveMethod);
	    sb.add(Box.createVerticalStrut(4));

	    addFieldLabel(sb, "Population per Thread:");
	    spnPopulation = makeSpinner(POPULATION, 1, 256, 1,
	        "<html>Individuals per thread. Higher = more diversity but slower.<br>" +
	        "Total population = Threads \u00D7 Population</html>");
	    sb.add(spnPopulation);
	    sb.add(Box.createVerticalStrut(4));

	    addFieldLabel(sb, "Crossover Max:");
	    spnCrossoverMax = makeSpinner(CROSSOVER_MAX, 1, 64, 1,
	        "Maximum crossover children per generation.");
	    sb.add(spnCrossoverMax);
	    sb.add(Box.createVerticalStrut(4));

	    addFieldLabel(sb, "Evolve Iterations per Batch:");
	    spnEvolveIterations = makeSpinner(EVOLVE_ITERATIONS, 1, 100, 1,
	        "<html>Iterations per evolution batch.<br>Higher = less overhead, less responsive UI.</html>");
	    sb.add(spnEvolveIterations);
	    sb.add(Box.createVerticalStrut(4));

	    addFieldLabel(sb, "Max Iterations (0 = unlimited):");
	    spnMaxIterations = makeSpinner(MAX_ITERATIONS / 1000, 0, 100000, 1000,
	        "<html>Maximum iterations in thousands (0 = no limit).<br>" +
	        "Current default: " + (MAX_ITERATIONS / 1000) + "K</html>");
	    sb.add(spnMaxIterations);
	    sb.add(Box.createVerticalStrut(4));

	    addFieldLabel(sb, "  \u2500\u2500 Mutation Operators \u2500\u2500");
	    sb.add(Box.createVerticalStrut(2));

	    addFieldLabel(sb, "Grid Mutations / Child:");
	    spnGridMutations = makeSpinner((int) CrossOver.GRID_MUTATION_CHANCES, 0, 512, 4,
	        "Localized color swaps within the evolver's grid section.");
	    sb.add(spnGridMutations);
	    sb.add(Box.createVerticalStrut(4));

	    addFieldLabel(sb, "Grid Mutation Decay (x1000):");
	    spnMutationDecay = makeSpinner((int) (CrossOver.GRID_MUTATION_DECAY * 1000), 0, 1000, 10,
	        "<html>Decay rate per tournament round (x1000).<br>" +
	        "100 = 0.1 decay. Lower = mutations persist longer.</html>");
	    sb.add(spnMutationDecay);
	    sb.add(Box.createVerticalStrut(4));

	    addFieldLabel(sb, "Random Mutations:");
	    spnRandomMutations = makeSpinner(CrossOver.RANDOM_MUTATION_CHANCES, 0, 10000, 100,
	        "Fully random color swaps across the entire image.");
	    sb.add(spnRandomMutations);
	    sb.add(Box.createVerticalStrut(4));

	    addFieldLabel(sb, "Random Mutation Prob (x10000):");
	    spnRandomMutationPct = makeSpinner((int) (CrossOver.RANDOM_MUTATION_PERCENT * 10000), 0, 10000, 1,
	        "<html>Probability multiplier for random mutations (x10000).<br>" +
	        "10 = 0.001. Higher = more mutations applied per attempt.</html>");
	    sb.add(spnRandomMutationPct);
	    sb.add(Box.createVerticalStrut(4));

	    addFieldLabel(sb, "Close Mutations:");
	    spnCloseMutations = makeSpinner(CrossOver.RANDOM_CLOSE_MUTATION_CHANCES, 0, 200, 5,
	        "Random swaps between nearby triangles.");
	    sb.add(spnCloseMutations);
	    sb.add(Box.createVerticalStrut(4));

	    addFieldLabel(sb, "Close Mutation Prob (x10000):");
	    spnCloseMutationPct = makeSpinner((int) (CrossOver.RANDOM_CLOSE_MUTATION_PERCENT * 10000), 0, 10000, 1,
	        "<html>Probability multiplier for close mutations (x10000).<br>" +
	        "1 = 0.0001. Higher = more close mutations applied.</html>");
	    sb.add(spnCloseMutationPct);
	    sb.add(Box.createVerticalStrut(4));

	    addFieldLabel(sb, "Targeted Swap Attempts:");
	    spnTargetedSwaps = makeSpinner(CrossOver.TARGETED_SWAP_ATTEMPTS, 0, 128, 4,
	        "Intelligent swaps: finds worst-matching triangle and tries to improve.");
	    sb.add(spnTargetedSwaps);
	    sb.add(Box.createVerticalStrut(4));

	    chkBlockCrossover = new javax.swing.JCheckBox("Block Crossover (spatial)",
	        CrossOver.CROSSOVER_BLOCK_ENABLED);
	    chkBlockCrossover.setFont(FNT_LABEL);
	    chkBlockCrossover.setOpaque(false);
	    chkBlockCrossover.setAlignmentX(Component.LEFT_ALIGNMENT);
	    chkBlockCrossover.setToolTipText("Enables spatial block crossover between parents.");
	    sb.add(chkBlockCrossover);

	    addSeparator(sb);

	    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
	    //  SPEED & PERFORMANCE — Threads, FPS, resources
	    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
	    addSection(sb, "\u26A1  SPEED & PERFORMANCE");

	    addFieldLabel(sb, "Threads (CPU cores):");
	    spnThreads = makeSpinner(THREADS, 1, 128, 1,
	        "<html>Parallel evolution threads. Each runs an independent evolver.<br>" +
	        "Recommended: match your CPU core count.<br>" +
	        "Available: " + Runtime.getRuntime().availableProcessors() + " cores</html>");
	    sb.add(spnThreads);
	    sb.add(Box.createVerticalStrut(4));

	    addFieldLabel(sb, "Evolution Loop FPS:");
	    spnEvolutionFps = makeSpinner(FPS, 1, 1000, 10,
	        "<html>How fast the evolution timer fires (frames/sec).<br>" +
	        "Higher = more throughput but more CPU overhead.<br>" +
	        "40 = balanced | 200+ = maximum speed</html>");
	    sb.add(spnEvolutionFps);
	    sb.add(Box.createVerticalStrut(4));

	    addFieldLabel(sb, "GUI Update FPS:");
	    spnGuiFps = makeSpinner(GUI_FPS, 1, 60, 5,
	        "<html>Image repaint rate. Higher = smoother but more CPU.<br>" +
	        "10 = low overhead | 30 = smooth | 60 = max</html>");
	    sb.add(spnGuiFps);
	    sb.add(Box.createVerticalStrut(4));

	    chkValidatePermutation = new javax.swing.JCheckBox("Validate Permutation",
	        ImageEvolver.VALIDATE_PERMUTATION);
	    chkValidatePermutation.setFont(FNT_LABEL);
	    chkValidatePermutation.setOpaque(false);
	    chkValidatePermutation.setAlignmentX(Component.LEFT_ALIGNMENT);
	    chkValidatePermutation.setToolTipText(
	        "<html>Check color permutation integrity after each operation.<br>" +
	        "Disabling gives a small speed boost but skips safety checks.</html>");
	    sb.add(chkValidatePermutation);
	    sb.add(Box.createVerticalStrut(2));

	    chkBenchmarkLogging = new javax.swing.JCheckBox("Benchmark CSV Logging",
	        BENCHMARK_LOGGING);
	    chkBenchmarkLogging.setFont(FNT_LABEL);
	    chkBenchmarkLogging.setOpaque(false);
	    chkBenchmarkLogging.setAlignmentX(Component.LEFT_ALIGNMENT);
	    chkBenchmarkLogging.setToolTipText("Write structured CSV benchmark files to benchmarks/ directory.");
	    sb.add(chkBenchmarkLogging);
	    sb.add(Box.createVerticalStrut(2));

	    chkExportVideo = new javax.swing.JCheckBox("Export Video Frames",
	        EXPORT_VIDEO);
	    chkExportVideo.setFont(FNT_LABEL);
	    chkExportVideo.setOpaque(false);
	    chkExportVideo.setAlignmentX(Component.LEFT_ALIGNMENT);
	    chkExportVideo.setToolTipText("Automatically export video frames during evolution.");
	    sb.add(chkExportVideo);

	    addSeparator(sb);

	    // ── EXPORT ────────────────────────────────────
	    addSection(sb, "\uD83C\uDFA8  PAINT-BY-COLORS EXPORT");

	    JButton btnExportPbn = makeStyledButton("Export Paint-by-Numbers");
	    btnExportPbn.setAlignmentX(Component.LEFT_ALIGNMENT);
	    btnExportPbn.setMaximumSize(BTN_SIZE);
	    btnExportPbn.setToolTipText("Export high-res image with color names rendered on each triangle.");
	    btnExportPbn.addActionListener(e -> exportPaintByNumbers());
	    sb.add(btnExportPbn);
	    sb.add(Box.createVerticalStrut(4));

	    JButton btnExportGuide = makeStyledButton("Export Outline Guide");
	    btnExportGuide.setAlignmentX(Component.LEFT_ALIGNMENT);
	    btnExportGuide.setMaximumSize(BTN_SIZE);
	    btnExportGuide.setToolTipText("Export clean outline guide with color labels (for printing).");
	    btnExportGuide.addActionListener(e -> exportOutlineGuide());
	    sb.add(btnExportGuide);
	    sb.add(Box.createVerticalStrut(4));

	    JButton btnExportCsv = makeStyledButton("Export Color Map CSV");
	    btnExportCsv.setAlignmentX(Component.LEFT_ALIGNMENT);
	    btnExportCsv.setMaximumSize(BTN_SIZE);
	    btnExportCsv.setToolTipText("Export CSV with triangle-to-color mapping and materials list.");
	    btnExportCsv.addActionListener(e -> exportColorMapCSV());
	    sb.add(btnExportCsv);

	    sb.add(Box.createVerticalStrut(8));

	    // ── Apply Live button ───────────────────────
	    JButton applyLive = makeStyledButton("Apply Settings Live");
	    applyLive.setBackground(ACCENT);
	    applyLive.setForeground(java.awt.Color.WHITE);
	    applyLive.setAlignmentX(Component.LEFT_ALIGNMENT);
	    applyLive.setMaximumSize(BTN_SIZE);
	    applyLive.setToolTipText("<html>Apply GA and Speed changes without restarting evolution.<br>" +
	        "Quality changes (grid size, palettes) require image reload.</html>");
	    applyLive.addActionListener(e -> applyLiveSettings());
	    sb.add(applyLive);

	    sb.add(Box.createVerticalGlue());

	    return sb;
	}

	private static JButton makeStyledButton(String text) {
	    JButton btn = new JButton(text);
	    btn.setFont(FNT_BTN);
	    btn.setFocusPainted(false);
	    return btn;
	}

	private static JLabel makeValueLabel(String text, Font font, java.awt.Color color) {
	    JLabel lbl = new JLabel(text);
	    lbl.setFont(font);
	    if (color != null) lbl.setForeground(color);
	    lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
	    return lbl;
	}

	private static void addSection(JPanel panel, String text) {
	    panel.add(Box.createVerticalStrut(2));
	    JLabel lbl = new JLabel(text);
	    lbl.setFont(FNT_SECTION);
	    lbl.setForeground(new java.awt.Color(80, 80, 100));
	    lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
	    panel.add(lbl);
	    panel.add(Box.createVerticalStrut(4));
	}

	private static void addSeparator(JPanel panel) {
	    panel.add(Box.createVerticalStrut(6));
	    JSeparator sep = new JSeparator(JSeparator.HORIZONTAL);
	    sep.setForeground(SEPARATOR_CLR);
	    sep.setAlignmentX(Component.LEFT_ALIGNMENT);
	    panel.add(sep);
	    panel.add(Box.createVerticalStrut(6));
	}

	private static void addFieldLabel(JPanel panel, String text) {
	    JLabel lbl = new JLabel(text);
	    lbl.setFont(FNT_LABEL);
	    lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
	    panel.add(lbl);
	}

	private static JSpinner makeSpinner(int value, int min, int max, int step, String tooltip) {
	    JSpinner spn = new JSpinner(new SpinnerNumberModel(value, min, max, step));
	    spn.setMaximumSize(CTRL_SIZE);
	    spn.setAlignmentX(Component.LEFT_ALIGNMENT);
	    spn.setFont(FNT_LABEL);
	    if (tooltip != null) spn.setToolTipText(tooltip);
	    return spn;
	}

	private static void styleCombo(JComboBox<?> cmb) {
	    cmb.setMaximumSize(CTRL_SIZE);
	    cmb.setAlignmentX(Component.LEFT_ALIGNMENT);
	    cmb.setFont(FNT_LABEL);
	}

	/**
	 * Reads current UI control values and applies them to the evolver parameters.
	 * Called before starting evolution.
	 */
	private void applyUISettings() {
	    applyQualitySettings();

	    THREADS = (int) spnThreads.getValue();
	    POPULATION = (int) spnPopulation.getValue();
	    CROSSOVER_MAX = (int) spnCrossoverMax.getValue();
	    EVOLVE_ITERATIONS = (int) spnEvolveIterations.getValue();
	    ImageEvolver.INITIALIZATION_METHOD = cmbInitMethod.getSelectedIndex();
	    ImageEvolver.SMART_INITIALIZATION = (cmbInitMethod.getSelectedIndex() == 1);
	    ImageEvolver.VALIDATE_PERMUTATION = chkValidatePermutation.isSelected();

	    int maxIterK = (int) spnMaxIterations.getValue();
	    MAX_ITERATIONS = maxIterK > 0 ? maxIterK * 1000 : Integer.MAX_VALUE;

	    applyMutationSettings();
	    applySpeedSettings();
	    applyDisplaySettings();

	    boolean useDelta = (cmbEvolveMethod.getSelectedIndex() == 1);
	    for (ImageEvolver ev : evolvers) {
	        ev.setUseDeltaEvolution(useDelta);
	    }

	    evolveStartTimeMs = System.currentTimeMillis();
	    initialScore = bestScore > 0 ? bestScore : 0.0;

	    if (fitnessChartWindow != null) {
	        fitnessChartWindow.clearData();
	    }
	}

	/**
	 * Applies mutation and operator parameters from UI controls.
	 * Can be called live during evolution.
	 */
	private void applyMutationSettings() {
	    CrossOver.GRID_MUTATION_CHANCES = (int) spnGridMutations.getValue();
	    CrossOver.GRID_MUTATION_DECAY = (int) spnMutationDecay.getValue() / 1000f;
	    CrossOver.RANDOM_MUTATION_CHANCES = (int) spnRandomMutations.getValue();
	    CrossOver.RANDOM_MUTATION_PERCENT = (int) spnRandomMutationPct.getValue() / 10000f;
	    CrossOver.RANDOM_CLOSE_MUTATION_CHANCES = (int) spnCloseMutations.getValue();
	    CrossOver.RANDOM_CLOSE_MUTATION_PERCENT = (int) spnCloseMutationPct.getValue() / 10000f;
	    CrossOver.TARGETED_SWAP_ATTEMPTS = (int) spnTargetedSwaps.getValue();
	    CrossOver.CROSSOVER_BLOCK_ENABLED = chkBlockCrossover.isSelected();
	}

	/**
	 * Applies quality settings (grid dimensions, palette count).
	 * These require an image reload to take effect on the evolvers.
	 */
	private void applyQualitySettings() {
	    int newW = (int) spnGridWidth.getValue();
	    int newH = (int) spnGridHeight.getValue();
	    int newPal = (int) spnPalettes.getValue();

	    boolean gridChanged = (newW != widthTriangles || newH != heightTriangles || newPal != TOTAL_PALLETES);

	    widthTriangles = newW;
	    heightTriangles = newH;
	    TOTAL_TRIANGLES = widthTriangles * heightTriangles;
	    TOTAL_PALLETES = newPal;

	    if (gridChanged) {
	        RANDOM_JUMP_MAX_DISTANCES = new int[128];
	        java.util.Arrays.fill(RANDOM_JUMP_MAX_DISTANCES, TOTAL_TRIANGLES);
	        resizedOriginal = null;
	    }
	}

	/**
	 * Applies speed/performance settings from UI controls.
	 * Can be called live during evolution.
	 */
	private void applySpeedSettings() {
	    FPS = (int) spnEvolutionFps.getValue();
	    EVOLVER_UPDATE_MS = FPS > 0 ? 1000 / FPS : 0;
	    if (processTimer != null) {
	        processTimer.setDelay(EVOLVER_UPDATE_MS);
	    }
	}

	/**
	 * Applies display and logging settings from UI controls.
	 */
	private void applyDisplaySettings() {
	    GUI_FPS = (int) spnGuiFps.getValue();
	    GUI_UPDATE_MS = GUI_FPS > 0 ? 1000 / GUI_FPS : 50;
	    BENCHMARK_LOGGING = chkBenchmarkLogging.isSelected();
	    EXPORT_VIDEO = chkExportVideo.isSelected();
	}

	/**
	 * Applies settings that can safely change during evolution
	 * without requiring a restart.
	 */
	private void applyLiveSettings() {
	    applyMutationSettings();
	    applySpeedSettings();
	    applyDisplaySettings();
	    EVOLVE_ITERATIONS = (int) spnEvolveIterations.getValue();
	    POPULATION = (int) spnPopulation.getValue();
	    CROSSOVER_MAX = (int) spnCrossoverMax.getValue();
	    ImageEvolver.VALIDATE_PERMUTATION = chkValidatePermutation.isSelected();

	    int maxIterK = (int) spnMaxIterations.getValue();
	    MAX_ITERATIONS = maxIterK > 0 ? maxIterK * 1000 : Integer.MAX_VALUE;

	    boolean useDelta = (cmbEvolveMethod.getSelectedIndex() == 1);
	    for (ImageEvolver ev : evolvers) {
	        ev.setUseDeltaEvolution(useDelta);
	    }

	    System.out.println("[ArtEvolver] Live settings applied:"
	        + " Grid=" + CrossOver.GRID_MUTATION_CHANCES
	        + " Decay=" + CrossOver.GRID_MUTATION_DECAY
	        + " Random=" + CrossOver.RANDOM_MUTATION_CHANCES + "(p=" + CrossOver.RANDOM_MUTATION_PERCENT + ")"
	        + " Close=" + CrossOver.RANDOM_CLOSE_MUTATION_CHANCES + "(p=" + CrossOver.RANDOM_CLOSE_MUTATION_PERCENT + ")"
	        + " Targeted=" + CrossOver.TARGETED_SWAP_ATTEMPTS
	        + " Block=" + CrossOver.CROSSOVER_BLOCK_ENABLED
	        + " Threads=" + THREADS
	        + " Pop=" + POPULATION
	        + " EvolveFPS=" + FPS
	        + " GUI_FPS=" + GUI_FPS
	        + " EvolveIter=" + EVOLVE_ITERATIONS
	        + " Delta=" + useDelta);
	}

	private void showFitnessChart() {
	    if (fitnessChartWindow == null) {
	        fitnessChartWindow = new FitnessChartWindow();
	    }
	    fitnessChartWindow.setVisible(true);
	    fitnessChartWindow.toFront();
	}

	private void showTournamentManager() {
	    if (tournamentManagerWindow == null) {
	        tournamentManagerWindow = new TournamentManagerWindow(this, contestants);
	    }
	    tournamentManagerWindow.setVisible(true);
	    tournamentManagerWindow.toFront();
	}

	private void onContestantSelected() {
	    if (cmbContestant == null || refreshingCombo) return;
	    int idx = cmbContestant.getSelectedIndex();
	    if (idx >= 0 && idx < contestants.size()) {
	        selectedContestant = contestants.get(idx);
	        if (selectedContestant.getBestImage() != null) {
	            bestImage = selectedContestant.getBestImage();
	            isDirty = true;
	        }
	    } else {
	        selectedContestant = null;
	    }
	}

	private boolean refreshingCombo = false;

	void refreshContestantCombo() {
	    if (cmbContestant == null) return;
	    refreshingCombo = true;
	    try {
	        int prevIdx = cmbContestant.getSelectedIndex();
	        cmbContestant.removeAllItems();
	        if (contestants.isEmpty()) {
	            cmbContestant.addItem("(single mode)");
	        } else {
	            DecimalFormat df = new DecimalFormat("0.00");
	            for (TournamentContestant c : contestants) {
	                if (c.isPromoted()) {
	                    cmbContestant.addItem("\uD83C\uDFC5 " + c.getName() + " (" + df.format(c.getFinalScore() * 100) + "%) PROMOTED");
	                } else if (c.isEliminated()) {
	                    cmbContestant.addItem("\u2620 " + c.getName() + " (" + df.format(c.getFinalScore() * 100) + "%) ELIMINATED");
	                } else {
	                    cmbContestant.addItem(c.getName() + " (" + df.format(
	                        Math.max(0, c.getBestScore()) * 100) + "%)");
	                }
	            }
	        }
	        if (prevIdx >= 0 && prevIdx < cmbContestant.getItemCount()) {
	            cmbContestant.setSelectedIndex(prevIdx);
	        }
	    } finally {
	        refreshingCombo = false;
	    }
	}

	/**
	 * Renders all tournament contestants in a grid layout on the main image panel.
	 * Each cell shows the contestant's image scaled to fit, with name and score overlay.
	 */
	private void paintAllContestants(Graphics2D g, int panelW, int panelH) {
	    int n = contestants.size();
	    if (n == 0) return;

	    g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
	        java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);

	    // Sort: alive by best score descending, then eliminated at bottom
	    List<TournamentContestant> sorted = new java.util.ArrayList<>(contestants);
	    sorted.sort((a, b) -> {
	        // Active first, then promoted, then eliminated
	        int statusA = a.isEliminated() ? 2 : a.isPromoted() ? 1 : 0;
	        int statusB = b.isEliminated() ? 2 : b.isPromoted() ? 1 : 0;
	        if (statusA != statusB) return statusA - statusB;
	        double sa = a.isFinished() ? a.getFinalScore() : a.getBestScore();
	        double sb = b.isFinished() ? b.getFinalScore() : b.getBestScore();
	        return Double.compare(sb, sa);
	    });

	    int cols = (int) Math.ceil(Math.sqrt(n));
	    int rows = (int) Math.ceil((double) n / cols);

	    int pad = 4;
	    int cellW = (panelW - pad) / cols;
	    int cellH = (panelH - pad) / rows;

	    DecimalFormat df = new DecimalFormat("0.00");
	    Font nameFont = new Font(Font.SANS_SERIF, Font.BOLD, Math.max(9, Math.min(12, cellW / 14)));
	    Font scoreFont = new Font(Font.MONOSPACED, Font.BOLD, Math.max(9, Math.min(11, cellW / 16)));
	    Font elimFont = new Font(Font.SANS_SERIF, Font.BOLD, Math.max(10, Math.min(14, cellW / 10)));

	    double bestScoreInTournament = contestants.stream()
	        .filter(c -> !c.isFinished())
	        .mapToDouble(TournamentContestant::getBestScore).max().orElse(0);

	    for (int i = 0; i < n; i++) {
	        TournamentContestant c = sorted.get(i);
	        int col = i % cols;
	        int row = i / cols;
	        int cx = pad + col * cellW;
	        int cy = pad + row * cellH;
	        boolean dead = c.isEliminated();
	        boolean prom = c.isPromoted();
	        boolean finished = dead || prom;

	        BufferedImage img = c.getBestImage();
	        if (img != null) {
	            int imgW = img.getWidth();
	            int imgH = img.getHeight();
	            int availW = cellW - pad * 2;
	            int availH = cellH - pad * 2 - 18;
	            double scale = Math.min((double) availW / imgW, (double) availH / imgH);
	            int drawW = (int) (imgW * scale);
	            int drawH = (int) (imgH * scale);
	            int dx = cx + (cellW - drawW) / 2;
	            int dy = cy + pad;
	            if (finished) {
	                java.awt.Composite origComp = g.getComposite();
	                g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, prom ? 0.6f : 0.35f));
	                g.drawImage(img, dx, dy, drawW, drawH, null);
	                g.setComposite(origComp);
	            } else {
	                g.drawImage(img, dx, dy, drawW, drawH, null);
	            }
	        } else {
	            g.setColor(new java.awt.Color(60, 60, 70));
	            g.fillRect(cx + pad, cy + pad, cellW - pad * 2, cellH - pad * 2 - 18);
	            g.setColor(java.awt.Color.GRAY);
	            g.setFont(nameFont);
	            g.drawString("initializing...", cx + pad + 4, cy + cellH / 2);
	        }

	        if (prom) {
	            g.setStroke(new java.awt.BasicStroke(2f));
	            g.setColor(new java.awt.Color(255, 193, 7, 180));
	            g.drawRect(cx + 1, cy + 1, cellW - 3, cellH - 3);
	            g.setFont(elimFont);
	            g.setColor(new java.awt.Color(255, 193, 7));
	            String promTxt = "\uD83C\uDFC5 PROMOTED";
	            int pw = g.getFontMetrics().stringWidth(promTxt);
	            g.drawString(promTxt, cx + (cellW - pw) / 2, cy + cellH / 2 + 5);
	        } else if (dead) {
	            g.setStroke(new java.awt.BasicStroke(2f));
	            g.setColor(new java.awt.Color(178, 34, 34, 180));
	            g.drawRect(cx + 1, cy + 1, cellW - 3, cellH - 3);
	            g.setFont(elimFont);
	            g.setColor(new java.awt.Color(178, 34, 34));
	            String elimTxt = "\u2620 ELIMINATED";
	            int ew = g.getFontMetrics().stringWidth(elimTxt);
	            g.drawString(elimTxt, cx + (cellW - ew) / 2, cy + cellH / 2 + 5);
	        } else {
	            boolean isBest = (c.getBestScore() > 0 && c.getBestScore() >= bestScoreInTournament);
	            boolean isSelected = (c == selectedContestant);
	            if (isBest || isSelected) {
	                g.setStroke(new java.awt.BasicStroke(isBest ? 3f : 2f));
	                g.setColor(isBest ? new java.awt.Color(255, 215, 0) : c.getChartColor());
	                g.drawRect(cx + 1, cy + 1, cellW - 3, cellH - 3);
	            }
	        }

	        int labelY = cy + cellH - 6;
	        g.setColor(new java.awt.Color(0, 0, 0, 160));
	        g.fillRect(cx, labelY - 13, cellW, 16);
	        g.setFont(nameFont);
	        g.setColor(prom ? new java.awt.Color(255, 193, 7) : dead ? java.awt.Color.GRAY : c.getChartColor());
	        g.drawString(c.getName(), cx + 3, labelY);

	        double displayScore = finished ? c.getFinalScore() : c.getBestScore();
	        String scoreStr = displayScore > 0 ? df.format(displayScore * 100) + "%" : "--";
	        g.setFont(scoreFont);
	        g.setColor(prom ? new java.awt.Color(255, 193, 7) : dead ? java.awt.Color.DARK_GRAY : java.awt.Color.WHITE);
	        int scoreW = g.getFontMetrics().stringWidth(scoreStr);
	        g.drawString(scoreStr, cx + cellW - scoreW - 4, labelY);
	    }
	}

	/**
	 * Populates an EvolutionConfig from the current UI control values.
	 * Used by TournamentManagerWindow when creating new contestants.
	 */
	public void populateConfigFromUI(EvolutionConfig cfg) {
	    cfg.threads = (int) spnThreads.getValue();
	    cfg.population = (int) spnPopulation.getValue();
	    cfg.crossoverMax = (int) spnCrossoverMax.getValue();
	    cfg.evolveIterations = (int) spnEvolveIterations.getValue();
	    cfg.initializationMethod = cmbInitMethod.getSelectedIndex();
	    cfg.useDeltaEvolution = (cmbEvolveMethod.getSelectedIndex() == 1);
	    cfg.validatePermutation = chkValidatePermutation.isSelected();
	    cfg.gridMutationChances = (int) spnGridMutations.getValue();
	    cfg.gridMutationDecay = (int) spnMutationDecay.getValue() / 1000f;
	    cfg.randomMutationChances = (int) spnRandomMutations.getValue();
	    cfg.randomMutationPercent = (int) spnRandomMutationPct.getValue() / 10000f;
	    cfg.closeMutationChances = (int) spnCloseMutations.getValue();
	    cfg.closeMutationPercent = (int) spnCloseMutationPct.getValue() / 10000f;
	    cfg.targetedSwapAttempts = (int) spnTargetedSwaps.getValue();
	    cfg.blockCrossoverEnabled = chkBlockCrossover.isSelected();
	}

	/**
	 * Starts all tournament contestants. Each contestant creates its own
	 * set of ImageEvolver threads with its own EvolutionConfig.
	 */
	public void startTournament() {
	    if (resizedOriginal == null) {
	        JOptionPane.showMessageDialog(this, "Load an image first.", "No Image", JOptionPane.WARNING_MESSAGE);
	        return;
	    }
	    if (contestants.isEmpty()) {
	        JOptionPane.showMessageDialog(this, "Add at least one contestant in the Tournament Manager.",
	                "No Contestants", JOptionPane.WARNING_MESSAGE);
	        return;
	    }

	    tournamentMode = true;

	    if (fitnessChartWindow == null) {
	        fitnessChartWindow = new FitnessChartWindow();
	    }
	    fitnessChartWindow.clearData();
	    fitnessChartWindow.setVisible(true);

	    for (TournamentContestant c : contestants) {
	        if (c.isFinished()) continue;
	        if (!c.isRunning()) {
	            try {
	                if (c.getEvolvers().isEmpty()) {
	                    c.createEvolvers(pallete, width, height, widthTriangles, heightTriangles,
	                            triangleScaleHeight, RANDOM_JUMP_MAX_DISTANCES);
	                    c.initializeWithImage(resizedOriginal);
	                }
	                c.start();
	            } catch (Exception ex) {
	                System.err.println("[Tournament] Failed to start contestant " + c.getName() + ": " + ex.getMessage());
	            }
	        }
	    }

	    if (!isRunning) {
	        evolveStartTimeMs = System.currentTimeMillis();
	        processTimer.start();
	        isRunning = true;
	    }

	    refreshContestantCombo();
	    if (tournamentManagerWindow != null) tournamentManagerWindow.refreshTable();

	    System.out.println("[Tournament] Started " + contestants.size() + " contestants");
	}

	/** Stops all tournament contestants (threads remain alive but idle). */
	public void stopTournament() {
	    if (tournamentManagerWindow != null) {
	        tournamentManagerWindow.resetEvolveButton();
	    }
	    for (TournamentContestant c : contestants) {
	        if (!c.isFinished()) c.stop();
	    }
	    if (tournamentManagerWindow != null) tournamentManagerWindow.refreshTable();
	    refreshContestantCombo();
	    System.out.println("[Tournament] All contestants stopped");
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		
		if (event.getActionCommand().equals("Load")) {
			
		    try {

		        loadImage();
				
			} catch (IOException e) {
			    
			    // ignore close button
				e.printStackTrace();
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
	
    public BufferedImage getResizedOriginal() {
        return resizedOriginal;
    }

    public void setResizedOriginal(BufferedImage resizedOriginal) {
        this.resizedOriginal = resizedOriginal;
    }

    public Palette getPallete() { return pallete; }
    public float getTriangleWidth() { return width; }
    public float getTriangleHeight() { return height; }
    public int getWidthTriangles() { return widthTriangles; }
    public int getHeightTriangles() { return heightTriangles; }
    public float getTriangleScaleHeight() { return triangleScaleHeight; }
    public int[] getJumpDistances() { return RANDOM_JUMP_MAX_DISTANCES; }
    public FitnessChartWindow getFitnessChartWindow() { return fitnessChartWindow; }
    public TournamentManagerWindow getTournamentManagerWindow() { return tournamentManagerWindow; }
    public boolean isRunning() { return isRunning; }
    public void setTournamentMode(boolean mode) { this.tournamentMode = mode; }
    public void startProcessTimer() {
        if (!isRunning) {
            evolveStartTimeMs = System.currentTimeMillis();
            processTimer.start();
            isRunning = true;
        }
        if (fitnessChartWindow == null) {
            fitnessChartWindow = new FitnessChartWindow();
        }
        fitnessChartWindow.setVisible(true);
    }

    // Returns the average of a stream of numbers
    static float streamAvg(float [] array, int n) {
        
        float avg = 0;
        
        for (int i = 0; i < n; i++) {
            
            avg = getAvg(avg, array[i], i);
        }
        
        return avg;
    }

    /**
     * Image preview accessory panel for JFileChooser.
     * Shows a scaled thumbnail and file dimensions when a file is selected.
     */
    static class ImagePreviewPanel extends JPanel implements PropertyChangeListener {

        private static final long serialVersionUID = 1L;
        private static final int PREVIEW_WIDTH = 220;
        private static final int PREVIEW_HEIGHT = 220;

        private final JLabel imageLabel;
        private final JLabel infoLabel;

        ImagePreviewPanel(JFileChooser chooser) {
            setPreferredSize(new Dimension(PREVIEW_WIDTH + 20, PREVIEW_HEIGHT + 60));
            setLayout(new BorderLayout(0, 4));
            setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 4));

            JLabel title = new JLabel("Preview", SwingConstants.CENTER);
            title.setFont(new Font("SansSerif", Font.BOLD, 11));
            add(title, BorderLayout.NORTH);

            imageLabel = new JLabel("", SwingConstants.CENTER);
            imageLabel.setPreferredSize(new Dimension(PREVIEW_WIDTH, PREVIEW_HEIGHT));
            imageLabel.setBorder(BorderFactory.createLineBorder(new java.awt.Color(200, 200, 200)));
            imageLabel.setVerticalAlignment(SwingConstants.CENTER);
            add(imageLabel, BorderLayout.CENTER);

            infoLabel = new JLabel("", SwingConstants.CENTER);
            infoLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
            add(infoLabel, BorderLayout.SOUTH);

            chooser.addPropertyChangeListener(this);
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            String prop = evt.getPropertyName();
            if (!JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(prop) &&
                !JFileChooser.DIRECTORY_CHANGED_PROPERTY.equals(prop)) {
                return;
            }

            File file = null;
            if (evt.getNewValue() instanceof File) {
                file = (File) evt.getNewValue();
            }

            if (file == null || !file.isFile()) {
                imageLabel.setIcon(null);
                imageLabel.setText("No image selected");
                infoLabel.setText("");
                return;
            }

            try {
                BufferedImage img = ImageIO.read(file);
                if (img == null) {
                    imageLabel.setIcon(null);
                    imageLabel.setText("Not an image");
                    infoLabel.setText("");
                    return;
                }

                int w = img.getWidth();
                int h = img.getHeight();
                double scale = Math.min(
                    (double) PREVIEW_WIDTH / w,
                    (double) PREVIEW_HEIGHT / h);
                if (scale > 1.0) scale = 1.0;

                int tw = (int) (w * scale);
                int th = (int) (h * scale);

                Image scaled = img.getScaledInstance(tw, th, Image.SCALE_SMOOTH);
                imageLabel.setIcon(new ImageIcon(scaled));
                imageLabel.setText(null);

                long sizeKb = file.length() / 1024;
                infoLabel.setText(w + " x " + h + "  (" + sizeKb + " KB)");

            } catch (IOException ex) {
                imageLabel.setIcon(null);
                imageLabel.setText("Load error");
                infoLabel.setText("");
            }
        }
    }
}