package old;

import java.awt.Color;
import java.util.Random;

public class ColorPolygon {
	private int xLimit;
	private int yLimit;
	private IntArray xCoordinates;
	private IntArray yCoordinates;
	private Random rnd;
	private Color color;

	public ColorPolygon(int paramInt1, int paramInt2, Random paramRandom, boolean paramBoolean) {
		this.xLimit = paramInt1;
		this.yLimit = paramInt2;

		this.rnd = paramRandom;
		this.xCoordinates = new IntArray();
		this.yCoordinates = new IntArray();

		this.color = new Color(this.rnd.nextInt(256), this.rnd.nextInt(256), this.rnd.nextInt(256), paramBoolean ? 0
				: this.rnd.nextInt(60));

		this.xCoordinates.add(this.rnd.nextInt(this.xLimit + 1));
		this.yCoordinates.add(this.rnd.nextInt(this.yLimit + 1));
		for (int i = 1; i < 4; i++) {
			this.xCoordinates.add(Math.max(Math.min(this.xCoordinates.array[0] + 3 - this.rnd.nextInt(7), this.xLimit),
					0));
			this.yCoordinates.add(Math.max(Math.min(this.yCoordinates.array[0] + 3 - this.rnd.nextInt(7), this.yLimit),
					0));
		}
	}

	public ColorPolygon(int[] paramArrayOfInt1, int[] paramArrayOfInt2, int paramInt1, int paramInt2, int paramInt3,
			int paramInt4, int paramInt5, int paramInt6, Random paramRandom) {
		this.xCoordinates = new IntArray();
		this.xCoordinates.setArray(paramArrayOfInt1);
		this.yCoordinates = new IntArray();
		this.yCoordinates.setArray(paramArrayOfInt2);
		this.xLimit = paramInt5;
		this.yLimit = paramInt6;
		this.color = new Color(paramInt1, paramInt2, paramInt3, paramInt4);
		this.rnd = paramRandom;
	}

	public ColorPolygon(ColorPolygon paramColorPolygon, int paramInt1, int paramInt2, Random paramRandom) {
		this.xCoordinates = new IntArray(paramColorPolygon.getXC());
		this.yCoordinates = new IntArray(paramColorPolygon.getYC());
		this.xLimit = paramInt1;
		this.yLimit = paramInt2;
		this.color = new Color(paramColorPolygon.getColor().getRed(), paramColorPolygon.getColor().getGreen(),
				paramColorPolygon.getColor().getBlue(), paramColorPolygon.getColor().getAlpha());
		this.rnd = paramRandom;
	}

	public boolean mutate() {
		boolean bool = false;
		int i;
		
		if ((this.rnd.nextInt(1500) == 1) && (this.xCoordinates.length() < ImageMutator.maxVertices)) {
			i = this.rnd.nextInt(this.xCoordinates.length());

			this.xCoordinates.add(
					(this.xCoordinates.array[i] + this.xCoordinates.array[((i + 1) % this.xCoordinates.length())]) / 2,
					i);
			this.yCoordinates.add(
					(this.yCoordinates.array[i] + this.yCoordinates.array[((i + 1) % this.yCoordinates.length())]) / 2,
					i);

			bool = true;
		}
		if ((this.rnd.nextInt(1500) == 1) && (this.xCoordinates.length() > 3)) {
			i = this.rnd.nextInt(this.xCoordinates.length());

			this.xCoordinates.remove(i);
			this.yCoordinates.remove(i);

			bool = true;
		}
		if (this.rnd.nextInt(1500) == 1) {
			this.color = new Color(Math.max(
					Math.min(
							this.color.getRed() + ImageMutator.deltaColor
									- this.rnd.nextInt(ImageMutator.deltaColor * 2 + 1), 255), 0),
					this.color.getGreen(), this.color.getBlue(), this.color.getAlpha());
			bool = true;
		}
		if (this.rnd.nextInt(1500) == 1) {
			this.color = new Color(this.color.getRed(), Math.max(
					Math.min(
							this.color.getGreen() + ImageMutator.deltaColor
									- this.rnd.nextInt(ImageMutator.deltaColor * 2 + 1), 255), 0),
					this.color.getBlue(), this.color.getAlpha());
			bool = true;
		}
		if (this.rnd.nextInt(1500) == 1) {
			this.color = new Color(this.color.getRed(), this.color.getGreen(), Math.max(
					Math.min(
							this.color.getBlue() + ImageMutator.deltaColor
									- this.rnd.nextInt(ImageMutator.deltaColor * 2 + 1), 255), 0),
					this.color.getAlpha());
			bool = true;
		}
		if (this.rnd.nextInt(1500) == 1) {
			this.color = new Color(this.color.getRed(), this.color.getGreen(), this.color.getBlue(), Math.max(
					Math.min(
							this.color.getAlpha() + ImageMutator.deltaColor
									- this.rnd.nextInt(ImageMutator.deltaColor * 2 + 1), 255), 0));
			bool = true;
		}
		
		for (i = 0; i < this.xCoordinates.length(); i++) {
			if (this.rnd.nextInt(500) == 1) {
				this.xCoordinates.array[i] = Math.max(
						Math.min(
								this.xCoordinates.array[i] + ImageMutator.deltaPoint
										- this.rnd.nextInt(ImageMutator.deltaPoint * 2 + 1), this.xLimit), 0);
				this.yCoordinates.array[i] = Math.max(
						Math.min(
								this.yCoordinates.array[i] + ImageMutator.deltaPoint
										- this.rnd.nextInt(ImageMutator.deltaPoint * 2 + 1), this.yLimit), 0);
				bool = true;
			}
		}
		return bool;
	}

	public int[] getXC() {
		return this.xCoordinates.array;
	}

	public int[] getYC() {
		return this.yCoordinates.array;
	}

	public Color getColor() {
		return this.color;
	}

	public int length() {
		return this.xCoordinates.length();
	}

	public String toString() {
		StringBuffer localStringBuffer = new StringBuffer();
		localStringBuffer.append(Integer.toString(this.xCoordinates.length()) + " ");
		for (int i = 0; i < this.xCoordinates.length(); i++) {
			localStringBuffer.append(this.xCoordinates.array[i] + " " + this.yCoordinates.array[i] + " ");
		}
		localStringBuffer.append(Integer.toString(this.color.getRed()) + " " + Integer.toString(this.color.getGreen())
				+ " " + Integer.toString(this.color.getBlue()) + " " + Integer.toString(this.color.getAlpha()) + " ");
		return localStringBuffer.toString();
	}
}
