package old;

import java.awt.image.BufferedImage;

class FitnessWorker implements Runnable {
	private BufferedImage originalImage;
	private int start;
	private int end;

	FitnessWorker(ImageMutator paramImageMutator) {
	}

	public void init(BufferedImage paramBufferedImage, int paramInt1, int paramInt2) {
		this.originalImage = paramBufferedImage;
		this.start = paramInt1;
		this.end = paramInt2;
	}

	public void run() {
//		double d = 0.0D;
//		for (int k = 0; k < this.originalImage.getHeight(); k++) {
//			for (int m = this.start; m < this.end; m++) {
////				int i = ImageMutator.get(this.start).getRGB(m, k);
//				int j = this.originalImage.getRGB(m, k);
//
//				d += Math.abs((i >> 16 & 0xFF) - (j >> 16 & 0xFF)) + Math.abs((i >> 8 & 0xFF) - (j >> 8 & 0xFF))
//						+ Math.abs((i & 0xFF) - (j & 0xFF));
//			}
//		}
////		ImageMutator.access$100(this.start, d);
//		try {
//			ImageMutator.access$200(this.start).await();
//		} catch (Exception localException) {
//			localException.printStackTrace();
//		}
	}
}