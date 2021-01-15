package old;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JPanel;

public class ImageMutator extends JPanel implements Runnable {
	public static final int P_RED = 1500;
	public static final int P_GREEN = 1500;
	public static final int P_BLUE = 1500;
	public static final int P_ALPHA = 1500;
	public static final int P_POINT_ADD = 1500;
	public static final int P_POINT_MOVE = 500;
	public static final int P_POINT_DEL = 1500;
	public static final int P_POLY_ADD = 700;
	public static final int P_POLY_MOVE = 1500;
	public static final int P_POLY_DEL = 700;
	public static volatile int deltaColor;
	public static volatile int deltaPoint;
	public static volatile int maxVertices;
	private volatile int maxPolygons;
	private BufferedImage originalImage;
	private BufferedImage bestImage;
	private BufferedImage currentImage;
	private ArrayList<ColorPolygon> polygons;
	private int goodMutationCount;
	private int testedMutationCount;
	private int numOfProcessors = Runtime.getRuntime().availableProcessors();
	private ImageMutator.FitnessWorker[] workers;
	private CyclicBarrier doneSignal;
	private ExecutorService es;
	private double bestScore;
	private double currentScore = 0.0D;
	private Random rnd;
	private boolean hasStarted = false;
	private Map<RenderingHints.Key, Object> hints = new HashMap();
	public boolean initialized = false;

	public ImageMutator(Random paramRandom, int paramInt1, int paramInt2, int paramInt3, int paramInt4) {
		setSize(900, 350);
		this.maxPolygons = paramInt1;
		maxVertices = paramInt2;
		deltaColor = paramInt3;
		deltaPoint = paramInt4;
		this.rnd = paramRandom;
		this.hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		setLayout(null);
		this.es = Executors.newCachedThreadPool();
		this.doneSignal = new CyclicBarrier(this.numOfProcessors + 1);
		this.workers = new ImageMutator.FitnessWorker[this.numOfProcessors];
		for (int i = 0; i < this.numOfProcessors; i++) {
			this.workers[i] = new ImageMutator.FitnessWorker();
		}
	}

	public void init(String paramString, BufferedImage paramBufferedImage) {
		StringTokenizer localStringTokenizer = new StringTokenizer(paramString);
		this.goodMutationCount = Integer.parseInt(localStringTokenizer.nextToken());
		this.testedMutationCount = Integer.parseInt(localStringTokenizer.nextToken());
		this.originalImage = paramBufferedImage;
		this.bestScore = Double.MAX_VALUE;

		this.bestImage = new BufferedImage(this.originalImage.getWidth(), this.originalImage.getHeight(), 1);
		Graphics2D localGraphics2D = this.bestImage.createGraphics();
		localGraphics2D.setColor(Color.WHITE);
		localGraphics2D.fillRect(0, 0, this.originalImage.getWidth(), this.originalImage.getHeight());

		this.currentImage = new BufferedImage(this.originalImage.getWidth(), this.originalImage.getHeight(), 1);
		localGraphics2D = this.currentImage.createGraphics();
		localGraphics2D.setColor(Color.WHITE);
		localGraphics2D.fillRect(0, 0, this.originalImage.getWidth(), this.originalImage.getHeight());

		this.polygons = new ArrayList();
		int i = Integer.parseInt(localStringTokenizer.nextToken());
		for (int j = 0; j < i; j++) {
			int k = Integer.parseInt(localStringTokenizer.nextToken());
			int[] arrayOfInt1 = new int[k];
			int[] arrayOfInt2 = new int[k];
			for (int m = 0; m < k; m++) {
				arrayOfInt1[m] = Integer.parseInt(localStringTokenizer.nextToken());
				arrayOfInt2[m] = Integer.parseInt(localStringTokenizer.nextToken());
			}
			this.polygons.add(new ColorPolygon(arrayOfInt1, arrayOfInt2, Integer.parseInt(localStringTokenizer
					.nextToken()), Integer.parseInt(localStringTokenizer.nextToken()), Integer
					.parseInt(localStringTokenizer.nextToken()), Integer.parseInt(localStringTokenizer.nextToken()),
					this.originalImage.getWidth(), this.originalImage.getHeight(), this.rnd));
		}
		localGraphics2D = this.bestImage.createGraphics();
		for (int j = 0; j < this.polygons.size(); j++) {
			localGraphics2D.setColor(((ColorPolygon) this.polygons.get(j)).getColor());
			localGraphics2D.fillPolygon(((ColorPolygon) this.polygons.get(j)).getXC(),
					((ColorPolygon) this.polygons.get(j)).getYC(), ((ColorPolygon) this.polygons.get(j)).length());
		}
		localGraphics2D = this.currentImage.createGraphics();
		for (int j = 0; j < this.polygons.size(); j++) {
			localGraphics2D.setColor(((ColorPolygon) this.polygons.get(j)).getColor());
			localGraphics2D.fillPolygon(((ColorPolygon) this.polygons.get(j)).getXC(),
					((ColorPolygon) this.polygons.get(j)).getYC(), ((ColorPolygon) this.polygons.get(j)).length());
		}
		countInitial();
	}

	public void init(BufferedImage paramBufferedImage) {
		this.originalImage = paramBufferedImage;
		this.goodMutationCount = 0;
		this.testedMutationCount = 0;
		this.bestScore = Double.MAX_VALUE;

		this.bestImage = new BufferedImage(this.originalImage.getWidth(), this.originalImage.getHeight(), 1);
		Graphics2D localGraphics2D = this.bestImage.createGraphics();
		localGraphics2D.setColor(Color.WHITE);
		localGraphics2D.fillRect(0, 0, this.originalImage.getWidth(), this.originalImage.getHeight());

		this.currentImage = new BufferedImage(this.originalImage.getWidth(), this.originalImage.getHeight(), 1);
		localGraphics2D = this.currentImage.createGraphics();
		localGraphics2D.setColor(Color.WHITE);
		localGraphics2D.fillRect(0, 0, this.originalImage.getWidth(), this.originalImage.getHeight());

		this.polygons = new ArrayList();
		for (int i = 0; i < 8; i++) {
			this.polygons.add(new ColorPolygon(this.originalImage.getWidth(), this.originalImage.getHeight(), this.rnd,
					true));
		}
		countInitial();
	}

	private void countInitial() {
		this.currentScore = 0.0D;
		for (int i = 0; i < this.numOfProcessors; i++) {
			this.workers[i].init(this.originalImage, i * this.originalImage.getWidth() / this.numOfProcessors, (i + 1)
					* (this.originalImage.getWidth() / this.numOfProcessors));
			this.es.execute(this.workers[i]);
		}
		try {
			this.doneSignal.await();
		} catch (Exception localException) {
			localException.printStackTrace();
		}
		this.doneSignal.reset();
		this.bestScore = this.currentScore;
	}

	public void run() {
		this.hasStarted = true;
		while (this.hasStarted) {
			int i = 0;

			ArrayList localArrayList = new ArrayList();
			for (int j = 0; j < this.polygons.size(); j++) {
				localArrayList.add(new ColorPolygon((ColorPolygon) this.polygons.get(j), this.originalImage.getWidth(),
						this.originalImage.getHeight(), this.rnd));
			}
			if ((this.rnd.nextInt(700) == 1) && (localArrayList.size() < this.maxPolygons)) {
				localArrayList
						.add(this.rnd.nextInt(localArrayList.size()), new ColorPolygon(this.originalImage.getWidth(),
								this.originalImage.getHeight(), this.rnd, false));
				i = 1;
			}
			if ((this.rnd.nextInt(1500) == 1) && (localArrayList.size() > 1)) {
				ColorPolygon localColorPolygon = (ColorPolygon) localArrayList.remove(this.rnd.nextInt(localArrayList
						.size()));
				localArrayList.add(this.rnd.nextInt(localArrayList.size()), localColorPolygon);
				i = 1;
			}
			if ((this.rnd.nextInt(700) == 1) && (localArrayList.size() > 2)) {
				localArrayList.remove(this.rnd.nextInt(localArrayList.size()));
				i = 1;
			}
			for (int k = 0; k < localArrayList.size(); k++) {
				i = (((ColorPolygon) localArrayList.get(k)).mutate()) || (i != 0) ? 1 : 0;
			}
			if (i != 0) {
				Graphics2D localGraphics2D = this.currentImage.createGraphics();
				localGraphics2D.setColor(Color.WHITE);
				localGraphics2D.fillRect(0, 0, this.originalImage.getWidth(), this.originalImage.getHeight());
				for (int k = 0; k < localArrayList.size(); k++) {
					localGraphics2D.setColor(((ColorPolygon) localArrayList.get(k)).getColor());
					localGraphics2D.fillPolygon(((ColorPolygon) localArrayList.get(k)).getXC(),
							((ColorPolygon) localArrayList.get(k)).getYC(),
							((ColorPolygon) localArrayList.get(k)).length());
				}
				this.currentScore = 0.0D;
				for (int k = 0; k < this.numOfProcessors; k++) {
					this.workers[k].init(this.originalImage, k * this.originalImage.getWidth() / this.numOfProcessors,
							(k + 1) * (this.originalImage.getWidth() / this.numOfProcessors));
					this.es.execute(this.workers[k]);
				}
				try {
					this.doneSignal.await();
				} catch (Exception localException) {
					localException.printStackTrace();
				}
				this.doneSignal.reset();
				if (this.currentScore <= this.bestScore) {
					this.polygons = localArrayList;
					this.bestImage = this.currentImage;
					this.currentImage = new BufferedImage(this.currentImage.getWidth(), this.currentImage.getHeight(),
							1);
					this.goodMutationCount += 1;
					this.bestScore = this.currentScore;
				}
				this.testedMutationCount += 1;
			}
		}
	}

	class FitnessWorker implements Runnable {
		private BufferedImage originalImage;
		private int start;
		private int end;

		FitnessWorker() {
		}

		public void init(BufferedImage paramBufferedImage, int paramInt1, int paramInt2) {
			this.originalImage = paramBufferedImage;
			this.start = paramInt1;
			this.end = paramInt2;
		}

		public void run() {
			double d = 0.0D;
			for (int k = 0; k < this.originalImage.getHeight(); k++) {
				for (int m = this.start; m < this.end; m++) {
					int i = ImageMutator.this.currentImage.getRGB(m, k);
					int j = this.originalImage.getRGB(m, k);

					d += Math.abs((i >> 16 & 0xFF) - (j >> 16 & 0xFF)) + Math.abs((i >> 8 & 0xFF) - (j >> 8 & 0xFF))
							+ Math.abs((i & 0xFF) - (j & 0xFF));
				}
			}
			ImageMutator.this.addCurrentScore(d);
			try {
				ImageMutator.this.doneSignal.await();
			} catch (Exception localException) {
				localException.printStackTrace();
			}
		}
	}

	private synchronized void addCurrentScore(double paramDouble) {
		this.currentScore += paramDouble;
	}

	public synchronized BufferedImage getBest(boolean paramBoolean) {
		if (paramBoolean) {
			BufferedImage localBufferedImage = new BufferedImage(this.originalImage.getWidth(),
					this.originalImage.getHeight(), 1);
			Graphics2D localGraphics2D = localBufferedImage.createGraphics();
			localGraphics2D.setRenderingHints(this.hints);
			localGraphics2D.setColor(Color.WHITE);
			localGraphics2D.fillRect(0, 0, this.originalImage.getWidth(), this.originalImage.getHeight());
			for (int i = 0; i < this.polygons.size(); i++) {
				localGraphics2D.setColor(((ColorPolygon) this.polygons.get(i)).getColor());
				localGraphics2D.fillPolygon(((ColorPolygon) this.polygons.get(i)).getXC(),
						((ColorPolygon) this.polygons.get(i)).getYC(), ((ColorPolygon) this.polygons.get(i)).length());
			}
			return localBufferedImage;
		}
		return this.bestImage;
	}

	public synchronized double getDiff() {
		return this.bestScore;
	}

	public synchronized int getImproved() {
		return this.goodMutationCount;
	}

	public synchronized int getTested() {
		return this.testedMutationCount;
	}

	public synchronized int getNumPolygons() {
		return this.polygons.size();
	}

	public synchronized void setMaxNumberOfPolygons(int paramInt) {
		this.maxPolygons = paramInt;
	}

	public synchronized void setMaxNumberOfVertices(int paramInt) {
		maxVertices = paramInt;
	}

	public synchronized void setDeltaColor(int paramInt) {
		deltaColor = paramInt;
	}

	public synchronized void setDeltaPoint(int paramInt) {
		deltaPoint = paramInt;
	}

	public synchronized String getDNA() {
		StringBuffer localStringBuffer = new StringBuffer();
		localStringBuffer.append(Integer.toString(this.goodMutationCount) + " "
				+ Integer.toString(this.testedMutationCount) + " " + Integer.toString(this.polygons.size()) + " ");
		for (int i = 0; i < this.polygons.size(); i++) {
			localStringBuffer.append(((ColorPolygon) this.polygons.get(i)).toString());
		}
		return localStringBuffer.toString();
	}

	public synchronized String getSVG() {
		StringBuffer localStringBuffer = new StringBuffer();

		localStringBuffer.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
		localStringBuffer
				.append("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">\n");
		localStringBuffer.append("<svg xmlns=\"http://www.w3.org/2000/svg\"\n");
		localStringBuffer
				.append("xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:ev=\"http://www.w3.org/2001/xml-events\"\n");
		localStringBuffer.append("version=\"1.1\" baseProfile=\"full\"\n");
		localStringBuffer.append("width=\"" + this.originalImage.getWidth() + "px\" height=\""
				+ this.originalImage.getHeight() + "px\">\n");
		for (int i = 0; i < this.polygons.size(); i++) {
			localStringBuffer.append("<polygon points=\"");
			for (int j = 0; j < ((ColorPolygon) this.polygons.get(i)).getXC().length; j++) {
				localStringBuffer.append(((ColorPolygon) this.polygons.get(i)).getXC()[j] + ",");
				localStringBuffer.append(((ColorPolygon) this.polygons.get(i)).getYC()[j] + " ");
			}
			localStringBuffer.append("\" fill=\"rgb(");
			localStringBuffer.append(((ColorPolygon) this.polygons.get(i)).getColor().getRed() + ",");
			localStringBuffer.append(((ColorPolygon) this.polygons.get(i)).getColor().getGreen() + ",");
			localStringBuffer.append(((ColorPolygon) this.polygons.get(i)).getColor().getBlue() + ")\" opacity=\"");
			localStringBuffer.append(255 - ((ColorPolygon) this.polygons.get(i)).getColor().getAlpha() + "\" />\n");
		}
		localStringBuffer.append("</svg>\n");
		return localStringBuffer.toString();
	}

	public void stop() {
		this.hasStarted = false;
	}

	public boolean isStarted() {
		return this.hasStarted;
	}
}
