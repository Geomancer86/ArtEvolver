package com.rndmodgames.evolver;

import java.awt.Color;

public class PalleteColor {

	private Palette pallete;

	private Long id;
	private String name;
	private Color color;

	public PalleteColor(Palette pallete, Long id, String name, int r, int g, int b){

		this.pallete = pallete;
		this.id = id;
		this.name = name;
		this.color = new Color(r, g, b);
	}

	public Color getColor() {
		return color;
	}

	public Palette getPallete() {
        return pallete;
    }

    public void setPallete(Palette pallete) {
        this.pallete = pallete;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setColor(Color color) {
		this.color = color;
	}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}