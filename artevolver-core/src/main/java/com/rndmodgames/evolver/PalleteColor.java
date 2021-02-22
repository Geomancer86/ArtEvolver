package com.rndmodgames.evolver;

import java.awt.Color;

public class PalleteColor {
	
<<<<<<< HEAD:artevolver-core/src/main/java/com/rndmodgames/evolver/PalleteColor.java
	Palette pallete;
=======
	Pallete pallete;
>>>>>>> threaded_2021:src/main/java/com/rndmodgames/evolver/PalleteColor.java
	Long id;
	String name;
	Color color;
	
<<<<<<< HEAD:artevolver-core/src/main/java/com/rndmodgames/evolver/PalleteColor.java
	public PalleteColor(Palette pallete, Long id, String name, int r, int g, int b){
=======
	public PalleteColor(Pallete pallete, Long id, String name, int r, int g, int b){
>>>>>>> threaded_2021:src/main/java/com/rndmodgames/evolver/PalleteColor.java
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