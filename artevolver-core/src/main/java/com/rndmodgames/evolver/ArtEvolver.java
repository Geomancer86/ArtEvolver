package com.rndmodgames.evolver;

import java.awt.BorderLayout;
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
import javax.swing.BoxLayout;
import javax.swing.JButton;
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
	private Pallete pallete;
	
	/**
	 * MODES
	 */
	private static final int QUICK_MODE          = 0;
	private static final int QUICK_EXTENDED_MODE = 1;
	private static final int QUICK_EXTENDED_24_THREADS   = 2;
	private static final int FASTEST_MODE           = 10;
	private static final int QUALITY_MODE        = 90;
	
	private static int CURRENT_MODE              = FASTEST_MODE;
	
	/**
	 * TODO: Save Parameters for DROPDOWN SIZE SELECT
	 * 
	 * scale = 3
	 * width = 3 * scale
	 * triangles = 80x53
	 */
	float triangleScaleHeight = 0.5f; // 0.25f, 0.5f, 0.66f, 0.75f, 1f, 1.25f, 1.5f, 2f, 2.5f, 3f 
	float triangleScaleWidth = 0.5f;
	
	float width = 3.0f * triangleScaleWidth;
	float height = 3.0f * triangleScaleHeight;
	
	int widthTriangles  = 80; // 71
	int heightTriangles = 53; // 60

	private int THREADS                 	= 8; // 1-x (32-48 peak)
	private int POPULATION 					= 4; // GeneticEvolver: 2-4096 (multiply by thread count to get the final population number)
	private int RANDOM_JUMP_MAX_DISTANCE	= 4239/2; // 1-x MAX: 4239/2
	private int CROSSOVER_MAX 				= 2;
	
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
	
	private int GUI_FPS = 60;
	private int FPS = 120;
	private int EVOLVER_UPDATE_MS = 1000 / FPS;
	private int GUI_UPDATE_MS = 1000 / GUI_FPS;
	
	private int RANDOM_JUMP_MAX_DISTANCES [] = {4239/2, 4239/2, 4239/2, 4239/2, 4239/2, 4239/2, 4239/2, 4239/2, 4239/2, 4239/2, 4239/2, 4239/2, 4239/2, 4239/2, 4239/2, 4239/2,
												4239/2, 4239/2, 4239/2, 4239/2, 4239/2, 4239/2, 4239/2, 4239/2, 4239/2, 4239/2, 4239/2, 4239/2, 4239/2, 4239/2, 4239/2, 4239/2,
												4239/2, 4239/2, 4239/2, 4239/2, 4239/2, 4239/2, 4239/2, 4239/2, 4239/2, 4239/2, 4239/2, 4239/2, 4239/2, 4239/2, 4239/2, 4239/2,
												4239/2, 4239/2, 4239/2, 4239/2, 4239/2, 4239/2, 4239/2, 4239/2, 4239/2, 4239/2, 4239/2, 4239/2, 4239/2, 4239/2, 4239/2, 4239/2};
	
	private int CROSSOVERS_MAX [] = {1, 2, 4, 8, 16, 32, 64, 128, 256}; 
	
	public static final int IMAGE_TYPE = BufferedImage.TYPE_INT_ARGB;
	
	public static int EVOLVE_ITERATIONS    = 1000;
	private int MAX_ITERATIONS             = 10000000;
	
	private static String SEPARATOR = ",";
	private static String EXPORT_FOLDER = "D:\\Media\\ArtEvolver\\";
	private static String IMAGE_SOURCE_NAME = null;
	private static int EXPORTED_IMAGES = 0;
	
	private List <ImageEvolver> evolvers = new ArrayList<>();

	// Timer
	private Timer processTimer;
	
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
	
	private TriangleList<Triangle> bestPop = new TriangleList<Triangle>();

	/**
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
    public ArtEvolver() throws IOException{
        
    	super("ArtEvolver 2021 v2.03");
    	
    	switch (CURRENT_MODE) {
    	
    	case QUALITY_MODE:
    	    THREADS = 8;
            triangleScaleHeight = 3f;
            triangleScaleWidth = 3f;
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
            
    	case QUICK_MODE:
    	default:
    	    THREADS = 16;
    	    MAX_ITERATIONS = 25000;
    	    triangleScaleHeight = 0.5f;
    	    triangleScaleWidth = 0.5f;
    	    width = 3.0f * triangleScaleWidth;
    	    height = 3.0f * triangleScaleHeight;
    	    break;
    	}
    	
    	pallete = new Pallete("Sherwin-Williams", TOTAL_PALLETES);

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
            	if (CURRENT_MODE != QUALITY_MODE) {
            	    
            	    if (totalIterations >= MAX_ITERATIONS) {
                        
                        //
                        renderBestImage();
                        System.exit(0);
                    }
            	}
            }
        });
        
        Container container = getContentPane();
        Container menuContainer = new JPanel();
        menuContainer.setLayout(new BoxLayout(menuContainer, BoxLayout.Y_AXIS));
        menuContainer.setPreferredSize(new Dimension(160, 480));

		imagePanel = new JPanel() {
			private static final long serialVersionUID = -4992430268801679523L;
	
			@Override
	        protected void paintComponent(Graphics g) {
			    
	            super.paintComponent(g);
	        }
	    };
	    
//	    imagePanel.setBackground(Color.BLUE);

	    JButton loadButton = new JButton("Load");
	    loadButton.setAlignmentX(Component.CENTER_ALIGNMENT);
	    loadButton.addActionListener(this);
	    
	    menuContainer.add(loadButton);
	    
	    JButton startButton = new JButton("Start");
	    startButton.setAlignmentX(Component.CENTER_ALIGNMENT);
      	startButton.addActionListener(this);
      	
      	menuContainer.add(startButton);
      	
        JButton stopButton = new JButton("Stop");
        stopButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        stopButton.addActionListener(this);
        menuContainer.add(stopButton);
        
        JButton source = new JButton("Source");
        source.setAlignmentX(Component.CENTER_ALIGNMENT);
        source.addActionListener(this);
        menuContainer.add(source);
        
        JButton export = new JButton("Export");
        export.setAlignmentX(Component.CENTER_ALIGNMENT);
        export.addActionListener(this);
        
        menuContainer.add(export);
        
        Container labelContainer = new JPanel();
        
        //
		lblScore = new JLabel("S: 0.0");
		lblScore.setMinimumSize(new Dimension(160, 24));
		lblScore.setPreferredSize(new Dimension(160, 24));
		lblScore.setMaximumSize(new Dimension(160, 24));

		//
		lblPopulation = new JLabel("Pop: 0");
		lblPopulation.setMinimumSize(new Dimension(160, 24));
		lblPopulation.setPreferredSize(new Dimension(160, 24));
		lblPopulation.setMaximumSize(new Dimension(160, 24));

		//
		lblIterations = new JLabel("I: 0/0");
		lblIterations.setMinimumSize(new Dimension(160, 24));
		lblIterations.setPreferredSize(new Dimension(160, 24));
		lblIterations.setMaximumSize(new Dimension(160, 24));
		
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
      	
      	container.add(menuContainer, BorderLayout.LINE_END);
      	container.add(imagePanel, BorderLayout.CENTER);
	    
      	// NOTE: set minimum size
      	if (triangleScaleWidth < 1f) {
      	    
      		setSize(420, 300);
      		
      	} else {
      	    
      		setSize((int) (320 * triangleScaleWidth), (int) (200 * triangleScaleHeight));
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

    public void loadImage() throws IOException{
        
    	this.chooser.resetChoosableFileFilters();
    	
		this.chooser.setFileFilter(new FileNameExtensionFilter("Image Files", new String[] { "jpg", "jpeg", "png", "gif", "bmp" }));

		if (this.chooser.showOpenDialog(this) == 0) {
		    
			try {
			    
				originalImage = ImageIO.read(new File(chooser.getCurrentDirectory().toString() + "\\"	+ chooser.getSelectedFile().getName()));
				path = (chooser.getCurrentDirectory().toString() + "\\" + chooser.getSelectedFile().getName());
				
				// 
				IMAGE_SOURCE_NAME = chooser.getSelectedFile().getName();
				
			} catch (Exception localException) {
			    
				JOptionPane.showMessageDialog(null, "Unable to Load Image", "Fail", 2);
			}
		}

		// Ignore on Select File Window Close (without picking a file)
		if (originalImage == null) {
		    
		    return;
		}
		
		/**
		 * Resizing code seems to be OK
		 */
		int newWidth = (int) (width * widthTriangles);
		int newHeight = (int) (((height * heightTriangles))  - height); // substract last serrated row
    	
//    	System.out.println("originalImage.width: " + originalImage.getWidth() + " - originalImage.height: " + originalImage.getHeight());
		
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
    		
//    		System.out.println("resizedOriginal.width: " + resizedOriginal.getWidth() + " - resizedOriginal.height: " + resizedOriginal.getHeight());
    		
//			evolver.setResizedOriginal(resizedOriginal);
//			evolver.setCurrentImage(resizedOriginal);

    		for (AbstractEvolver currentEvolver : evolvers) {
    			((ImageEvolver)currentEvolver).setResizedOriginal(resizedOriginal);
//    			((ImageEvolver)currentEvolver).setCurrentImage(resizedOriginal);
    			((ImageEvolver)currentEvolver).initializeIsosceles();
    		}
    		
    		this.resizedOriginal = resizedOriginal;
    	}

//    	 evolver.initialize();
    	
//    	evolver.initializeIsosceles();
    	
//    	evolver.initializeFromFile("campito_78.txt");
//    	evolver.initializeFromFile("campito_78.txt");
//    	evolver.initializeFromFile("campito_80.txt");
//    	evolver.initializeFromFile("campito_805.txt");
//    	evolver.initializeFromFile("campito_81.txt");
//    	evolver.initializeFromFile("campito_811.txt");
//    	evolver.initializeFromFile("campito_8114.txt");
//    	evolver.initializeFromFile("campito_8115.txt");
//    	evolver.initializeFromFile("campito_8116.txt");
//    	evolver.initializeFromFile("campito_81163.txt");
//    	evolver.initializeFromFile("campito_8117.txt");
//    	evolver.initializeFromFile("campito_8118.txt");
//    	evolver.initializeFromFile("campito_8119.txt");
//    	evolver.initializeFromFile("campito_812.txt");
//    	evolver.initializeFromFile("campito_812.txt");

//    	evolver.initializeFromFile("campito_big2.txt");
//    	evolver.initializeFromFile("campito_big2.txt");
    	
//    	evolver.initializeFromFile("campito_beta_test_1.txt");
//    	evolver.initializeFromFile("campito_beta_test_1.txt");
    	
//    	evolver.initializeFromFile("campito_beta_test_2.txt", 6f);
//    	evolver.initializeFromFile("campito_beta_test_2.txt", 6f);
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
        
        System.out.println(IMAGE_SOURCE_NAME + SEPARATOR
                + THREADS + SEPARATOR
                + (THREADS * POPULATION) + SEPARATOR
                + totalIterations + SEPARATOR
                + goodIterations + SEPARATOR
                + ((float) goodIterations / (float) totalIterations) + SEPARATOR
                + bestScore);
        
        String [] splitted = IMAGE_SOURCE_NAME.split("\\.");
        
        Renderer.renderToPNG(bestPop,
                splitted[0],
                EXPORT_FOLDER,
                EXPORTED_IMAGES,
                (int) (width * widthTriangles),
                (int) (height * (heightTriangles - 1)), // do not render last row
                IMAGE_TYPE,
                1);
        
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
		    if (IMAGE_SOURCE_NAME != null) {
		        
		        renderBestImage();
		        
		        // 
		        EXPORTED_IMAGES++;
		    }
		}
	}
}