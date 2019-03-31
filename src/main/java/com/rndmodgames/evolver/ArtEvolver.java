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
import java.util.Locale;

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

public class ArtEvolver extends JFrame implements ActionListener, ChangeListener{

	private static final long serialVersionUID = 6291204469421642923L;
	
	private JFrame mainFrame;
	private JPanel imagePanel;
	private Pallete pallete;

	float triangleScaleHeight = 1f;
	float triangleScaleWidth = 1f;
	
	float width = 3.1f * triangleScaleWidth;
	float height = 3.1f * triangleScaleHeight;
	
	int widthTriangles  = 80; // 71
	int heightTriangles = 53; // 60
	
	// ImageEvolver
	static final int POPULATION 				= 2; // GeneticEvolver: 2-4092 // GreedyEvolver: 1-1 
	static final int RANDOM_JUMP_MAX_DISTANCE	= 1;
	static final int CROSSOVER_MAX 				= 1;
	static final int TOTAL_PALLETES             = 4;
	
	static final int RANDOM_JUMP_MAX_DISTANCES [] = {1, 2, 4, 8, 16, 32, 64, 128, 256}; 
	static final int CROSSOVERS_MAX [] = {1, 2, 4, 8, 16, 32, 64, 128, 256}; 
	
	public static final int IMAGE_TYPE = BufferedImage.TYPE_INT_ARGB;
	
	static final int EVOLVE_ITERATIONS          = 8;
	static final int MAX_ITERATIONS             = 10000000;
	
	ImageEvolver evolver;
//	GreedyEvolver evolver;
	
	// Timer
	private Timer processTimer;
	
	// Components
	private JFileChooser chooser;
	private JLabel lblScore;
	private JLabel lblIterations;
	
	@SuppressWarnings("unused")
	private String path;
	
	private BufferedImage originalImage;
	private BufferedImage resizedOriginal;
	
	long start;
	long totalIterations = 0L;
	long goodIterations = 0L;
	double currentScore = 0.0d;

    public ArtEvolver() throws IOException{
    	super("ArtEvolver v0.01");
    	pallete = new Pallete("Sherwin-Williams", TOTAL_PALLETES);
    	
    	evolver = new ImageEvolver(POPULATION, RANDOM_JUMP_MAX_DISTANCE, CROSSOVER_MAX, triangleScaleHeight, pallete, width, height, widthTriangles, heightTriangles);
//    	evolver = new GreedyEvolver(POPULATION, RANDOM_JUMP_MAX_DISTANCE, CROSSOVER_MAX, triangleScaleHeight, pallete, width, height, widthTriangles, heightTriangles);

        initComponents();
    }
    
	private void initComponents() {
		
		Locale.setDefault(Locale.GERMAN);
		
        mainFrame = this;
        mainFrame.setResizable(true);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // init timer
        processTimer = new Timer(0, new ActionListener() {

			@Override
            public void actionPerformed(ActionEvent e) {
				
				evolver.evolve(start, EVOLVE_ITERATIONS);
            	
            	if (evolver.isDirty()){
	            	// draw bestImage to panel
	            	imagePanel.getGraphics().drawImage(evolver.getBestImage(),
	            									   32, // TODO: make both offsets dynamic to center in JPanel
	            									   32,
	            									   null);
	            	
	            	imagePanel.getGraphics().dispose();
	            	evolver.setDirty(false);
            	}
            	
            	// update stats
            	lblScore.setText("S: " + evolver.getBestScore());
            	lblIterations.setText("I: " + evolver.getGoodIterations() + "/" + evolver.getTotalIterations());
            	
            	if (evolver.getTotalIterations() >= MAX_ITERATIONS){
//            		stop();
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
        
        Container labelContainer = new JPanel();
		lblScore = new JLabel("S: 0.0");
		lblScore.setMinimumSize(new Dimension(160, 24));
		lblScore.setPreferredSize(new Dimension(160, 24));
		lblScore.setMaximumSize(new Dimension(160, 24));
		
		labelContainer.add(lblScore);
		
		lblIterations = new JLabel("I: 0/0");
		lblIterations.setMinimumSize(new Dimension(160, 24));
		lblIterations.setPreferredSize(new Dimension(160, 24));
		lblIterations.setMaximumSize(new Dimension(160, 24));
		
		labelContainer.add(lblIterations);
		
		menuContainer.add(labelContainer);
      	
      	container.add(menuContainer, BorderLayout.LINE_END);
      	container.add(imagePanel, BorderLayout.CENTER);
	    
		setSize(1920/2, 1080/2);  

		chooser = new JFileChooser(new File(System.getProperty("user.dir")));
		chooser.setAcceptAllFileFilterUsed(false);
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

    public void loadImage(){
    	this.chooser.resetChoosableFileFilters();
		this.chooser.setFileFilter(new FileNameExtensionFilter("Image Files", new String[] { "jpg", "jpeg", "png", "gif", "bmp" }));

		if (this.chooser.showOpenDialog(this) == 0) {
			try {
				originalImage = ImageIO.read(new File(chooser.getCurrentDirectory().toString() + "\\"	+ chooser.getSelectedFile().getName()));
				path = (chooser.getCurrentDirectory().toString() + "\\" + chooser.getSelectedFile().getName());
			} catch (Exception localException) {
				JOptionPane.showMessageDialog(null, "Unable to Load Image", "Fail", 2);
			}
		}

		/**
		 * Resizing code seems to be OK
		 */
		int newWidth = (int) ((width * widthTriangles * triangleScaleWidth) * (TOTAL_PALLETES / triangleScaleWidth / 2));
		int newHeight = (int) ((height * heightTriangles * triangleScaleHeight) * (TOTAL_PALLETES / triangleScaleHeight / 2));
    	
    	System.out.println("originalImage.width: " + originalImage.getWidth() + " - originalImage.height: " + originalImage.getHeight());
		
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
    		
    		System.out.println("resizedOriginal.width: " + resizedOriginal.getWidth() + " - resizedOriginal.height: " + resizedOriginal.getHeight());
    		
  			evolver.setResizedOriginal(resizedOriginal);
   			evolver.setCurrentImage(resizedOriginal);
    	}

//    	 evolver.initialize();
    	evolver.initializeEquilateral();
    }

    public void start(){
    	start = System.currentTimeMillis();
    	processTimer.start();
    }
    
    public void stop(){
    	processTimer.stop();
    	
    	// Draw Original Image for Comparison TODO: move to different button
    	// draw bestImage to panel
    	imagePanel.getGraphics().drawImage(evolver.getResizedOriginal(),
    									   32, // TODO: make both offsets dynamic to center in JPanel
    									   32,
    									   null);
    	
    	imagePanel.getGraphics().dispose();
    }
    
	@Override
	public void stateChanged(ChangeEvent e) {

	}

	@Override
	public void actionPerformed(ActionEvent event) {
		if (event.getActionCommand().equals("Load")) {
			loadImage();
		}
		
		if (event.getActionCommand().equals("Start")) {
			start();
		}
		
		if (event.getActionCommand().equals("Stop")) {
			stop();
		}
	}
}