package com.rndmodgames.evolver;

import java.awt.Color;

public class PalleteColor {
	
	Pallete pallete;
	Long id;
	String name;
	Color color;
	
	public PalleteColor(Pallete pallete, Long id, String name, int r, int g, int b){
		this.pallete = pallete;
		this.id = id;
		this.name = name;
		this.color = new Color(r, g, b);
	}

	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
	}
}