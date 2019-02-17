package com.rndmodgames.evolver;

import java.awt.Color;
import java.awt.Polygon;

public class Triangle extends Polygon{

	private static final long serialVersionUID = -181164485692814464L;
	Color color;
	
	int [] xPoly;
	int [] yPoly;
	int lenght;
	
	public Triangle(int [] xPoly, int [] yPoly, int lenght, Color color){
		super(xPoly, yPoly, lenght);
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
}