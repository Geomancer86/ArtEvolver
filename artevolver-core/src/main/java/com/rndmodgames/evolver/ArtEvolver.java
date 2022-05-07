package com.rndmodgames.evolver;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
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
	
	public static boolean HIGH_RESOLUTION_EXPORT = true;
	public static boolean ULTRA_HIGH_RESOLUTION_EXPORT = false;
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
	private int RANDOM_JUMP_MAX_DISTANCE	= 2; // 1-x MAX: 4239/2
	private int CROSSOVER_MAX 				= 2;
	
	private boolean FITNESS_BASED_PARENT_SELECTION = false;
	
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
	private int RANDOM_JUMP_MAX_DISTANCES [] = {1, 1, 1, 1,
	                                            2, 2, 2, 2,
	                                            4, 4, 4, 4,
	                                            8, 8, 8, 8,
	                                            16, 16, 16, 16,
	                                            32, 32, 32, 32, 32, 32, 32, 32, 64, 64,  4239/2,  4239/2, 
	                                            32, 32, 32, 32, 32, 32, 32, 32, 64, 64,  4239/2,  4239/2,
	                                            32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 64, 64, 4239/2, 4239/2,};
	
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
	double bestScore = Double.MIN_VALUE;
	double currentScore = Double.MIN_VALUE;
	double averagePopulationScore = 0d;
	boolean isDirty = false;
	boolean isRunning = false;
	boolean showSource = false;
	
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
            TOTAL_PALLETES = 16;
            widthTriangles = 116; // 116
            heightTriangles = 73; // 73
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
    	    THREADS = 16;
            POPULATION = 16;
            triangleScaleHeight = 6f;
            triangleScaleWidth = 6f;
            width = 3.0f * triangleScaleWidth;
            height = 3.0f * triangleScaleHeight;
            
            break;
            
    	case QUALITY_MODE:

    	    THREADS = 4;
    	    POPULATION = 4;
            triangleScaleHeight = 3f;
            triangleScaleWidth = 3f;
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
        processTimer = new Timer(EVOLVER_UPDATE_MS, new ActionListener() { 

            @Override
            public void actionPerformed(ActionEvent e) {

				// reset as needed
				totalIterations = 0;
				goodIterations = 0;

				// ignore
				if (imagePanel == null || imagePanel.getGraphics() == null) {
				    
				    return;
				}
				
				for (AbstractEvolver currentEvolver : evolvers) {
					
					totalIterations += ((ImageEvolver)currentEvolver).getTotalIterations();
					goodIterations += ((ImageEvolver)currentEvolver).getGoodIterations();
					

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
            	
            	lblScore.setText("S     : " + bestScore);
//            	lblAverageScore.setText("S(AVG): " + evolver.getAverageScore());
            	lblPopulation.setText("Pop: " + population);
            	lblIterations.setText("I: " + goodIterations + "/" + totalIterations);

            	/**
            	 * This stats only make sense for benchmarking and should keep consistent, for example, printed once each 1 second
            	 */
            	if (currentFrame++ % FPS == 0) {
//           		System.out.println(steps++ + "," + totalIterations + "," + goodIterations + ","  + ImageEvolver.DEFAULT_DECIMAL_FORMAT.format(bestScore));
//            		System.out.println(ImageEvolver.DEFAULT_DECIMAL_FORMAT.format(bestScore));
            	}
            	
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

//        menuContainer.setPreferredSize(new Dimension(160, 480));

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
        
        Container labelContainer = new JPanel();
        
        menuContainer.add(export);
        
        //
		lblScore = new JLabel("S: 0.0");
		lblScore.setMinimumSize(new Dimension(160, 24));
		lblScore.setPreferredSize(new Dimension(160, 24));
		lblScore.setMaximumSize(new Dimension(160, 24));
		
		labelContainer.add(lblScore);
		
//		lblAverageScore = new JLabel("S(AVG): 0.0");
//		lblAverageScore.setMinimumSize(new Dimension(160, 24));
//		lblAverageScore.setPreferredSize(new Dimension(160, 24));
//		lblAverageScore.setMaximumSize(new Dimension(160, 24));
//		
//		labelContainer.add(lblAverageScore);

		//
		lblPopulation = new JLabel("Pop: 0");
		lblPopulation.setMinimumSize(new Dimension(160, 24));
		lblPopulation.setPreferredSize(new Dimension(160, 24));
		lblPopulation.setMaximumSize(new Dimension(160, 24));
		
		labelContainer.add(lblPopulation);

		//
		lblIterations = new JLabel("I: 0/0");
		lblIterations.setMinimumSize(new Dimension(160, 24));
		lblIterations.setPreferredSize(new Dimension(160, 24));
		lblIterations.setMaximumSize(new Dimension(160, 24));

		labelContainer.add(lblIterations);

		//
//		lblIterationsPerSecond = new JLabel("I/second: 0.0");
//		lblIterationsPerSecond.setMinimumSize(new Dimension(160, 24));
//		lblIterationsPerSecond.setPreferredSize(new Dimension(160, 24));
//		lblIterationsPerSecond.setMaximumSize(new Dimension(160, 24));
        
		labelContainer.add(lblPopulation);
        labelContainer.add(lblScore);
		labelContainer.add(lblIterations);
//		labelContainer.add(lblIterationsPerSecond);
		
		menuContainer.add(labelContainer);
		
		// padding
		menuContainer.setPreferredSize(new Dimension(200, 900));
		menuContainer.setSize(new Dimension(200, 900));
		menuContainer.setMaximumSize(new Dimension(200, 900));
//		menuContainer.setBackground(Color.BLUE);
		
//		menuContainer.setBorder(BorderFactory.createCompoundBorder(
//	               BorderFactory.createLineBorder(Color.CYAN, 5),
//	               BorderFactory.createLineBorder(Color.BLACK, 20)));
		
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
		
		if (event.getActionCommand().equals("Secuential")) {
//			evolver.switchSecuential();
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
}