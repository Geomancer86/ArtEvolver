package old;
import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;


public class ImageEvolverGUI extends JFrame implements ActionListener, ChangeListener {
	private JLabel originalImageLabel;
	private JLabel bestImageLabel;
	private JLabel improvedVsTested;
	private JLabel similarity;
	private JLabel polygonCount;
	private JLabel vertexPerPolygonCount;
	private JLabel movesPerSecond;
	private BufferedImage originalImage;
	private JSpinner maxPolygonSpinner;
	private JSpinner maxVertexSpinner;
	private JSpinner colorDeltaSpinner;
	private JSpinner pointDeltaSpinner;
	private JButton[] buttons;
	private JSlider maxPolygonSlider;
	private JSlider maxVertexSlider;
	private JSlider colorDeltaSlider;
	private JSlider pointDeltaSlider;
	private int lastMutationCount = 0;
	private int timeSinceMutationCheck = 0;
	private int lastNumberSaved = 0;
	private ImageMutator imageMutator;
	private String path;
	private JFileChooser chooser;
	private Timer updateTimer;
	private int maxPolygons = 2000;
	private int maxVertices = 200;
	private int deltaColor = 30;
	private int deltaPoint = 40;
	public final DecimalFormat df = new DecimalFormat("0.####");
	public final Random rnd = new Random();
	private boolean saveIncremental = false;
	private static final String VERSION = "1.0.0";

	public static void main(String[] paramArrayOfString) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new ImageEvolverGUI("Image Evolver ");
			}
		});
	}

	public ImageEvolverGUI(String paramString) {
		super(paramString + " " + "1.0.0");
		setSize(1000, 600);
		setDefaultCloseOperation(3);
		setLayout(null);

		this.updateTimer = new Timer(100, this);
		this.updateTimer.setActionCommand("t");

		this.chooser = new JFileChooser(new File(System.getProperty("user.dir")));
		this.chooser.setAcceptAllFileFilterUsed(false);

		JLabel[] arrayOfJLabel = new JLabel[10];
		arrayOfJLabel[0] = new JLabel("Original");
		arrayOfJLabel[0].setBounds(136, 5, 60, 15);
		add(arrayOfJLabel[0]);

		arrayOfJLabel[1] = new JLabel("The Ultimate Mutation");
		arrayOfJLabel[1].setBounds(610, 5, 200, 15);
		add(arrayOfJLabel[1]);

		arrayOfJLabel[2] = new JLabel("Max number of polygons");
		arrayOfJLabel[2].setBounds(106, 420, 180, 15);
		add(arrayOfJLabel[2]);

		arrayOfJLabel[3] = new JLabel("Max vertices per polygon");
		arrayOfJLabel[3].setBounds(106, 480, 180, 15);
		add(arrayOfJLabel[3]);

		arrayOfJLabel[4] = new JLabel("Beneficial/Total Mutations");
		arrayOfJLabel[4].setHorizontalAlignment(0);
		arrayOfJLabel[4].setBounds(275, 390, 250, 15);
		add(arrayOfJLabel[4]);

		arrayOfJLabel[5] = new JLabel("Similarity");
		arrayOfJLabel[5].setHorizontalAlignment(0);
		arrayOfJLabel[5].setBounds(310, 430, 180, 15);
		add(arrayOfJLabel[5]);

		arrayOfJLabel[6] = new JLabel("Max Delta for Color");
		arrayOfJLabel[6].setHorizontalAlignment(0);
		arrayOfJLabel[6].setBounds(700, 420, 150, 15);
		add(arrayOfJLabel[6]);

		arrayOfJLabel[7] = new JLabel("Max Delta for Coordinates");
		arrayOfJLabel[7].setHorizontalAlignment(0);
		arrayOfJLabel[7].setBounds(710, 480, 150, 15);
		add(arrayOfJLabel[7]);

		arrayOfJLabel[8] = new JLabel("Number of Polygons");
		arrayOfJLabel[8].setHorizontalAlignment(0);
		arrayOfJLabel[8].setBounds(470, 430, 250, 15);
		add(arrayOfJLabel[8]);

		arrayOfJLabel[9] = new JLabel("Moves per Second");
		arrayOfJLabel[9].setHorizontalAlignment(0);
		arrayOfJLabel[9].setBounds(470, 390, 250, 15);
		add(arrayOfJLabel[9]);

		this.improvedVsTested = new JLabel();
		this.improvedVsTested.setHorizontalAlignment(0);
		this.improvedVsTested.setBounds(250, 405, 300, 15);
		add(this.improvedVsTested);

		this.similarity = new JLabel();
		this.similarity.setHorizontalAlignment(0);
		this.similarity.setBounds(250, 445, 300, 15);
		add(this.similarity);

		this.polygonCount = new JLabel();
		this.polygonCount.setHorizontalAlignment(0);
		this.polygonCount.setBounds(440, 445, 300, 15);
		add(this.polygonCount);

		this.movesPerSecond = new JLabel();
		this.movesPerSecond.setHorizontalAlignment(0);
		this.movesPerSecond.setBounds(440, 405, 300, 15);
		add(this.movesPerSecond);

		this.buttons = new JButton[6];
		this.buttons[0] = new JButton("Load");
		this.buttons[0].setBounds(106, 350, 120, 30);
		add(this.buttons[0]);

		this.buttons[1] = new JButton("Save");
		this.buttons[1].setBounds(430, 350, 120, 30);
		this.buttons[1].setEnabled(false);
		add(this.buttons[1]);

		this.buttons[2] = new JButton("Start");
		this.buttons[2].setBounds(430, 500, 120, 30);
		this.buttons[2].setEnabled(false);
		add(this.buttons[2]);

		this.buttons[3] = new JButton("Import DNA");
		this.buttons[3].setBounds(715, 350, 100, 30);
		this.buttons[3].setEnabled(false);
		add(this.buttons[3]);

		this.buttons[4] = new JButton("Export DNA");
		this.buttons[4].setBounds(830, 350, 100, 30);
		this.buttons[4].setEnabled(false);
		add(this.buttons[4]);

		this.buttons[5] = new JButton("?");
		this.buttons[5].setBounds(960, 0, 20, 20);
		this.buttons[5].setMargin(new Insets(0, 0, 0, 0));
		add(this.buttons[5]);
		for (int i = 0; i < 6; i++) {
			this.buttons[i].addActionListener(this);
		}
		JCheckBox localJCheckBox = new JCheckBox("Save Incrementally");
		localJCheckBox.setBounds(430, 540, 168, 15);
		localJCheckBox.addActionListener(this);
		add(localJCheckBox);

		this.maxPolygonSpinner = new JSpinner(new SpinnerNumberModel(2000, 1, 2000, 1));
		this.maxPolygonSpinner.setBounds(106, 440, 130, 20);
		this.maxPolygonSpinner.addChangeListener(this);
		this.maxPolygonSpinner.setName("p1");
		((JSpinner.DefaultEditor) this.maxPolygonSpinner.getEditor()).getTextField().setEditable(false);
		add(this.maxPolygonSpinner);

		this.maxPolygonSlider = new JSlider(1, 2000, 2000);
		this.maxPolygonSlider.setBounds(106, 460, 130, 20);
		this.maxPolygonSlider.addChangeListener(this);
		this.maxPolygonSlider.setName("p2");
		add(this.maxPolygonSlider);

		this.maxVertexSpinner = new JSpinner(new SpinnerNumberModel(200, 3, 200, 1));
		this.maxVertexSpinner.setBounds(106, 500, 130, 20);
		this.maxVertexSpinner.addChangeListener(this);
		this.maxVertexSpinner.setName("v1");
		((JSpinner.DefaultEditor) this.maxVertexSpinner.getEditor()).getTextField().setEditable(false);
		add(this.maxVertexSpinner);

		this.maxVertexSlider = new JSlider(3, 200, 200);
		this.maxVertexSlider.setBounds(106, 520, 130, 20);
		this.maxVertexSlider.addChangeListener(this);
		this.maxVertexSlider.setName("v2");
		add(this.maxVertexSlider);

		this.colorDeltaSpinner = new JSpinner(new SpinnerNumberModel(30, 1, 128, 1));
		this.colorDeltaSpinner.setBounds(715, 440, 130, 20);
		this.colorDeltaSpinner.addChangeListener(this);
		this.colorDeltaSpinner.setName("pd1");
		((JSpinner.DefaultEditor) this.colorDeltaSpinner.getEditor()).getTextField().setEditable(false);
		add(this.colorDeltaSpinner);

		this.colorDeltaSlider = new JSlider(1, 128, 30);
		this.colorDeltaSlider.setBounds(715, 460, 130, 20);
		this.colorDeltaSlider.addChangeListener(this);
		this.colorDeltaSlider.setName("pd2");
		add(this.colorDeltaSlider);

		this.pointDeltaSpinner = new JSpinner(new SpinnerNumberModel(40, 1, 200, 1));
		this.pointDeltaSpinner.setBounds(715, 500, 130, 20);
		this.pointDeltaSpinner.addChangeListener(this);
		this.pointDeltaSpinner.setName("cd1");
		((JSpinner.DefaultEditor) this.pointDeltaSpinner.getEditor()).getTextField().setEditable(false);
		add(this.pointDeltaSpinner);

		this.pointDeltaSlider = new JSlider(1, 200, 40);
		this.pointDeltaSlider.setBounds(715, 520, 130, 20);
		this.pointDeltaSlider.addChangeListener(this);
		this.pointDeltaSlider.setName("cd2");
		add(this.pointDeltaSlider);

		this.originalImageLabel = new JLabel();
		this.originalImageLabel.setHorizontalAlignment(0);
		this.originalImageLabel.setBounds(10, 25, 300, 320);
		add(this.originalImageLabel);

		this.bestImageLabel = new JLabel();
		this.bestImageLabel.setHorizontalAlignment(0);
		this.bestImageLabel.setBounds(343, 25, 629, 320);
		add(this.bestImageLabel);

		this.imageMutator = new ImageMutator(this.rnd, this.maxPolygons, this.maxVertices, this.deltaColor,
				this.deltaPoint);

		setVisible(true);
	}

	public void actionPerformed(ActionEvent paramActionEvent) {
		if (paramActionEvent.getActionCommand().equals("Load")) {
			loadProgram();
		} else if (paramActionEvent.getActionCommand().equals("Start")) {
			startProgram();
		} else if (paramActionEvent.getActionCommand().equals("Stop")) {
			stopProgram();
		} else if (paramActionEvent.getActionCommand().equals("Save")) {
			saveImage();
		} else if (paramActionEvent.getActionCommand().equals("Import DNA")) {
			importDNA();
		} else if (paramActionEvent.getActionCommand().equals("Export DNA")) {
			exportDNA();
		} else if (paramActionEvent.getActionCommand().equals("Save Incrementally")) {
			this.saveIncremental = (!this.saveIncremental);
		} else if (paramActionEvent.getActionCommand().equals("t")) {
			updateGUI();
		} else {
			System.out.println(paramActionEvent.getActionCommand());
			JOptionPane
					.showMessageDialog(
							null,
							"Load an image with load.\nStart magic by pushing Start.\nProgram by Aaron Fan.\nInspired by AlteredQualia's program at www.alteredqualia.com/visualization/evolve.\nVersion 1.0.0\nLicensed under the GNU GPL available at <http://www.gnu.org/licenses/>",
							"Help & About", 1);
		}
	}

	public void stateChanged(ChangeEvent paramChangeEvent) {
		if (((Component) paramChangeEvent.getSource()).getName().equals("p1")) {
			this.maxPolygons = Integer.parseInt(this.maxPolygonSpinner.getValue().toString());
		} else if (((Component) paramChangeEvent.getSource()).getName().equals("p2")) {
			this.maxPolygons = this.maxPolygonSlider.getValue();
		} else if (((Component) paramChangeEvent.getSource()).getName().equals("v1")) {
			this.maxVertices = Integer.parseInt(this.maxVertexSpinner.getValue().toString());
		} else if (((Component) paramChangeEvent.getSource()).getName().equals("v2")) {
			this.maxVertices = this.maxVertexSlider.getValue();
		} else if (((Component) paramChangeEvent.getSource()).getName().equals("pd1")) {
			this.deltaColor = Integer.parseInt(this.colorDeltaSpinner.getValue().toString());
		} else if (((Component) paramChangeEvent.getSource()).getName().equals("pd2")) {
			this.deltaColor = this.colorDeltaSlider.getValue();
		} else if (((Component) paramChangeEvent.getSource()).getName().equals("cd1")) {
			this.deltaPoint = Integer.parseInt(this.pointDeltaSpinner.getValue().toString());
		} else if (((Component) paramChangeEvent.getSource()).getName().equals("cd2")) {
			this.deltaPoint = this.pointDeltaSlider.getValue();
		}
		this.maxPolygonSpinner.setValue(Integer.valueOf(this.maxPolygons));
		this.maxPolygonSlider.setValue(this.maxPolygons);
		this.maxVertexSpinner.setValue(Integer.valueOf(this.maxVertices));
		this.maxVertexSlider.setValue(this.maxVertices);
		this.colorDeltaSpinner.setValue(Integer.valueOf(this.deltaColor));
		this.colorDeltaSlider.setValue(this.deltaColor);
		this.pointDeltaSpinner.setValue(Integer.valueOf(this.deltaPoint));
		this.pointDeltaSlider.setValue(this.deltaPoint);

		this.imageMutator.setMaxNumberOfPolygons(this.maxPolygons);
		this.imageMutator.setMaxNumberOfVertices(this.maxVertices);
		this.imageMutator.setDeltaColor(this.deltaColor);
		this.imageMutator.setDeltaPoint(this.deltaPoint);
	}

	private void loadProgram() {
		this.chooser.resetChoosableFileFilters();
		this.chooser.setFileFilter(new FileNameExtensionFilter("Image Files", new String[] { "jpg", "jpeg", "png",
				"gif", "bmp" }));
		if (this.chooser.showOpenDialog(this) == 0) {
			try {
				this.originalImage = ImageIO.read(new File(this.chooser.getCurrentDirectory().toString() + "\\"
						+ this.chooser.getSelectedFile().getName()));
				this.path = (this.chooser.getCurrentDirectory().toString() + "\\" + this.chooser.getSelectedFile()
						.getName());

				this.buttons[2].setEnabled(true);
				if ((this.originalImage.getWidth() > 300)
						&& (this.originalImage.getWidth() / 300.0D > this.originalImage.getHeight() / 320.0D)) {
					this.originalImageLabel.setIcon(new ImageIcon(this.originalImage.getScaledInstance(300,
							(int) (300.0D / this.originalImage.getWidth() * this.originalImage.getHeight()), 1)));
				} else if ((this.originalImage.getHeight() > 320)
						&& (this.originalImage.getHeight() / 320.0D > this.originalImage.getWidth() / 300.0D)) {
					this.originalImageLabel.setIcon(new ImageIcon(this.originalImage.getScaledInstance(
							(int) (320.0D / this.originalImage.getHeight() * this.originalImage.getWidth()), 300, 1)));
				} else {
					this.originalImageLabel.setIcon(new ImageIcon(this.originalImage));
				}
				this.imageMutator.initialized = false;

				this.buttons[1].setEnabled(false);
				this.buttons[4].setEnabled(false);
				this.buttons[3].setEnabled(true);
			} catch (Exception localException) {
				JOptionPane.showMessageDialog(null, "Unable to Load Image", "Fail", 2);
			}
		}
	}

	private void startProgram() {
		if (!this.imageMutator.initialized) {
			this.imageMutator.init(this.originalImage);
			this.imageMutator.initialized = true;
		}
		new Thread(this.imageMutator).start();

		this.buttons[2].setText("Stop");

		this.buttons[1].setEnabled(true);
		this.buttons[4].setEnabled(true);

		this.buttons[0].setEnabled(false);
		this.buttons[3].setEnabled(false);
		this.updateTimer.start();
	}

	private void stopProgram() {
		this.imageMutator.stop();
		this.buttons[2].setText("Start");

		this.buttons[0].setEnabled(true);
		this.buttons[3].setEnabled(true);
		this.updateTimer.stop();
	}

	private void saveImage() {
		int i = 0;
		if (this.imageMutator.isStarted()) {
			this.imageMutator.stop();
			this.updateTimer.stop();
			i = 1;
		}
		this.chooser.resetChoosableFileFilters();
		this.chooser.setFileFilter(new FileNameExtensionFilter("SVG", new String[] { "svg" }));
		this.chooser.setFileFilter(new FileNameExtensionFilter("PNG", new String[] { "png" }));
		if (this.chooser.showSaveDialog(this) == 0) {
			if (this.chooser.getFileFilter().getDescription().equals("PNG")) {
				try {
					String str1 = this.chooser.getSelectedFile().getName().toString();
					ImageIO.write(
							this.imageMutator.getBest(true),
							"png",
							new File(this.chooser.getCurrentDirectory().toString()
									+ "\\"
									+ str1
									+ (str1.substring(str1.length() - 4, str1.length()).equalsIgnoreCase(".png") ? ""
											: ".png")));
				} catch (Exception localException1) {
					JOptionPane.showMessageDialog(null, "Unable to Save Image", "Fail", 2);
				}
			}
			if (this.chooser.getFileFilter().getDescription().equals("SVG")) {
				try {
					String str2 = this.chooser.getSelectedFile().getName().toString();
					PrintWriter localPrintWriter = new PrintWriter(new BufferedWriter(
							new FileWriter(this.chooser.getCurrentDirectory().toString()
									+ "\\"
									+ str2
									+ (str2.substring(str2.length() - 4, str2.length()).equalsIgnoreCase(".svg") ? ""
											: ".svg"))));
					localPrintWriter.println(this.imageMutator.getSVG());
					localPrintWriter.close();
				} catch (Exception localException2) {
					JOptionPane.showMessageDialog(null, "Unable to Save Image", "Fail", 2);
				}
			}
		}
		if (i != 0) {
			new Thread(this.imageMutator).start();
			this.updateTimer.start();
		}
	}

	private void importDNA() {
		this.chooser.resetChoosableFileFilters();
		this.chooser.setFileFilter(new FileNameExtensionFilter("DNA file", new String[] { "dna" }));
		if (this.chooser.showOpenDialog(this) == 0) {
			try {
				BufferedReader localBufferedReader = new BufferedReader(new FileReader(this.chooser
						.getCurrentDirectory().toString() + "\\" + this.chooser.getSelectedFile().getName().toString()));
				this.imageMutator.init(localBufferedReader.readLine(), this.originalImage);

				this.imageMutator.initialized = true;
				this.lastNumberSaved = this.imageMutator.getTested();
				updateGUI();
			} catch (Exception localException) {
				JOptionPane.showMessageDialog(null, "Unable to Load DNA", "Fail", 2);
				localException.printStackTrace();
				this.imageMutator.initialized = false;
			}
		}
	}

	private void exportDNA() {
		int i = 0;
		if (this.imageMutator.isStarted()) {
			this.imageMutator.stop();
			this.updateTimer.stop();
			i = 1;
		}
		this.chooser.resetChoosableFileFilters();
		this.chooser.setFileFilter(new FileNameExtensionFilter("DNA file", new String[] { "dna" }));
		if (this.chooser.showSaveDialog(this) == 0) {
			try {
				String str = this.chooser.getSelectedFile().getName().toString();
				PrintWriter localPrintWriter = new PrintWriter(new BufferedWriter(new FileWriter(this.chooser
						.getCurrentDirectory().toString()
						+ "\\"
						+ str
						+ (str.substring(str.length() - 4, str.length()).equalsIgnoreCase(".dna") ? "" : ".dna"))));
				localPrintWriter.println(this.imageMutator.getDNA());
				localPrintWriter.close();
			} catch (Exception localException) {
				JOptionPane.showMessageDialog(null, "Unable to Save DNA", "Fail", 2);
			}
		}
		if (i != 0) {
			new Thread(this.imageMutator).start();
			this.updateTimer.start();
		}
	}

	private void updateGUI() {
		updateBestImage();
		updateStatistics();
	}

	private void updateBestImage() {
		BufferedImage localBufferedImage = this.imageMutator.getBest(false);
		if ((localBufferedImage.getWidth() > 629)
				&& (localBufferedImage.getWidth() / 629.0D > localBufferedImage.getHeight() / 320.0D)) {
			this.bestImageLabel.setIcon(new ImageIcon(localBufferedImage.getScaledInstance(629,
					(int) (629.0D / localBufferedImage.getWidth() * localBufferedImage.getHeight()), 1)));
		} else if ((localBufferedImage.getHeight() > 320)
				&& (localBufferedImage.getHeight() / 320.0D > localBufferedImage.getWidth() / 629.0D)) {
			this.bestImageLabel.setIcon(new ImageIcon(localBufferedImage.getScaledInstance(
					(int) (320.0D / localBufferedImage.getHeight() * localBufferedImage.getWidth()), 320, 1)));
		} else {
			this.bestImageLabel.setIcon(new ImageIcon(localBufferedImage));
		}
	}

	private void updateStatistics() {
		double d1 = this.imageMutator.getDiff();
		int i = this.imageMutator.getImproved();
		int j = this.imageMutator.getTested();
		this.improvedVsTested.setText(i + "/" + j);
		double d2 = 1.0D;
		d2 *= this.originalImage.getWidth();
		d2 *= this.originalImage.getHeight();
		d2 *= 255.0D;
		d2 *= 3.0D;

		this.similarity.setText(this.df.format((1.0D - d1 / d2) * 100.0D) + "%");
		if (this.timeSinceMutationCheck == 9) {
			this.movesPerSecond.setText(this.df.format(j - this.lastMutationCount));
			this.lastMutationCount = j;
			this.timeSinceMutationCheck = 0;
		} else {
			this.timeSinceMutationCheck += 1;
		}
		if (i - this.lastNumberSaved > 100) {
			saveIncremental(i);
			this.lastNumberSaved = i;
		}
		this.polygonCount.setText(Integer.toString(this.imageMutator.getNumPolygons()));
	}

	private void saveIncremental(int paramInt) {
		try {
			PrintWriter localPrintWriter = new PrintWriter(new BufferedWriter(new FileWriter(
					(this.path.indexOf(".") != -1 ? this.path.substring(0, this.path.lastIndexOf(".")) : this.path)
							+ ".dna")));
			localPrintWriter.println(this.imageMutator.getDNA());
			localPrintWriter.close();
		} catch (Exception localException1) {
		}
		if (this.saveIncremental) {
			try {
				ImageIO.write(this.imageMutator.getBest(true), "png", new File(
						(this.path.indexOf(".") != -1 ? this.path.substring(0, this.path.lastIndexOf(".")) : this.path)
								+ " (" + Integer.toString(paramInt / 100) + ").png"));
			} catch (Exception localException2) {
			}
		}
	}
}
