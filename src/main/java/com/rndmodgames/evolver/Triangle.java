package com.rndmodgames.evolver;

import java.awt.Color;
import java.awt.Polygon;
import java.util.Arrays;

public class Triangle extends Polygon{

	private static final long serialVersionUID = -181164485692814464L;
	
	private Long colorId;
	private Color color;
	
	private int [] xPoly;
	private int [] yPoly;
	private int lenght;
	
	public Triangle(int [] xPoly, int [] yPoly, int lenght, Color color){
		super(xPoly, yPoly, lenght);
		this.color = color;
		this.xPoly = xPoly;
		this.yPoly = yPoly;
		this.lenght = lenght;
	}

	public Triangle(int[] xPoly, int[] yPoly, int lenght, Long colorId, Color color) {
		super(xPoly, yPoly, lenght);
		
		this.colorId = colorId;
		this.color = color;
		this.xPoly = xPoly;
		this.yPoly = yPoly;
		this.lenght = lenght;
	}

	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public int[] getxPoly() {
		return xPoly;
	}

	public void setxPoly(int[] xPoly) {
		this.xPoly = xPoly;
	}

	public int[] getyPoly() {
		return yPoly;
	}

	public void setyPoly(int[] yPoly) {
		this.yPoly = yPoly;
	}

	public int getLenght() {
		return lenght;
	}

	public void setLenght(int lenght) {
		this.lenght = lenght;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((colorId == null) ? 0 : colorId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Triangle other = (Triangle) obj;
		if (colorId == null) {
			if (other.colorId != null)
				return false;
		} else if (!colorId.equals(other.colorId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return Arrays.toString(xPoly) + "," + Arrays.toString(yPoly) + ",[" + color.getRed() + ", " + color.getGreen() + ", " + color.getBlue() + "]";
	}
}