package com.rndmodgames.evolver;

import java.awt.BorderLayout;
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
	private JPanel panel;
	private Pallete pallete;

	float width = 5.1f;
	float height = 3.8f;
	float scale = 2f;
	
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
//        mainFrame.setResizable(false);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // init timer
        processTimer = new Timer(1, new ActionListener() {

			@Override
            public void actionPerformed(ActionEvent e) {

            	evolver.evolve(start, EVOLVE_ITERATIONS);
            	
            	if (evolver.isDirty()){
	            	// draw bestImage to panel
	            	panel.getGraphics().drawImage(evolver.getBestImage(), 0, 0, null);
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

        
//        JFrame frame = new JFrame("BorderLayoutDemo - Beginnersbook.com");
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Container container = getContentPane();

        /* Creating and adding 5 buttons to the each area of Border
         * Layout. Button names are intentionally kept same as 
         * area names for better understanding, they can have any names.
         */
//        container.add(new JButton("PAGE_START"), BorderLayout.PAGE_START);
//        container.add(new JButton("PAGE_END"), BorderLayout.PAGE_END);
//        container.add(new JButton("LINE_START"), BorderLayout.LINE_START);
        container.add(new JButton("LINE_END"), BorderLayout.LINE_END);
        container.add(new JButton("CENTER"), BorderLayout.CENTER);
        
//        int offsetX = 248;
//        int offsetY = 167;
//        
//        int offset

//        JButton loadButton = new JButton("Load");
//        loadButton.addActionListener(this);
//        add(loadButton, BorderLayout.PAGE_START);
//        
//        JButton startButton = new JButton("Start");
//        startButton.addActionListener(this);
//        add(startButton, BorderLayout.PAGE_END);
//        
//        JButton stopButton = new JButton("Stop");
//        stopButton.addActionListener(this);
//        add(stopButton, BorderLayout.LINE_START);
//        
//		lblScore = new JLabel("S: 0.0");
//		add(lblScore, BorderLayout.LINE_END);
//		
//		lblIterations = new JLabel("I: 0/0");
//		add(lblIterations, BorderLayout.CENTER);
//		
//		panel = new JPanel() {
//			private static final long serialVersionUID = -4992430268801679523L;
//
//			@Override
//            protected void paintComponent(Graphics g) {
//                super.paintComponent(g);
//            }
//
//            @Override
//            public Dimension getPreferredSize() {
//                return new Dimension((int)(heightTriangles * height * scale), (int)(widthTriangles * width * scale));
//            }
//        };
        
//        add(panel, BorderLayout.CENTER);
        
//		setLayout(new FlowLayout());
//		setLayout(new BorderLayout());
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