package com.rndmodgames.evolver;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

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

	float width = 5.1f;
	float height = 3.8f;
	float scale = 1f;
	
	int widthTriangles  = 24; // 48
	int heightTriangles = 44; // 44
	
	// ImageEvolver
	static final int POPULATION 				= 2; // 2-4092
	static final int RANDOM_JUMP_MAX_DISTANCE	= 1;
	static final int CROSSOVER_MAX 				= 1;
	static final int TOTAL_PALLETES             = 1;
	
	static final int EVOLVE_ITERATIONS          = 16;
	static final int MAX_ITERATIONS             = 10000000;
	
	ImageEvolver evolver;
	
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
    	evolver = new ImageEvolver(POPULATION, RANDOM_JUMP_MAX_DISTANCE, CROSSOVER_MAX, scale, pallete, width, height, widthTriangles, heightTriangles);

        initComponents();
    }
    
	private void initComponents() {
        mainFrame = this;
        mainFrame.setResizable(false);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // init timer
        processTimer = new Timer(1, new ActionListener() {

			@Override
            public void actionPerformed(ActionEvent e) {

            	evolver.evolve(start, EVOLVE_ITERATIONS);
            	
            	if (evolver.isDirty()){
	            	// draw bestImage to panel
	            	imagePanel.getGraphics().drawImage(evolver.getBestImage(), 0, 0, null);
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
	
	        @Override
	        public Dimension getPreferredSize() {
	            return new Dimension((int)(heightTriangles * height * scale), (int)(widthTriangles * width * scale));
	        }
	    };
	    
	    imagePanel.setBackground(Color.BLUE);
	    

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
	    
		setSize(640,480);  
		setVisible(true);

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
		
		// panel size
    	int imgWidth = 385 - 140;
    	int imgHeight = 167;
		
		// initialize currentImage and resizedOriginal
    	if (resizedOriginal == null){
    		resizedOriginal = new BufferedImage(imgWidth, imgHeight, 1);
    		resizedOriginal.getGraphics().drawImage(originalImage, 0, 0, resizedOriginal.getWidth(), resizedOriginal.getHeight(), null);
    		
  			evolver.setResizedOriginal(resizedOriginal);
   			evolver.setCurrentImage(resizedOriginal);
    	}
    	
    	 evolver.initialize();
    }

    public void start(){
    	start = System.currentTimeMillis();
    	processTimer.start();
    }
    
    public void stop(){
    	processTimer.stop();
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