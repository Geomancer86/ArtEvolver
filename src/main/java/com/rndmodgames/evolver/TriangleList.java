package com.rndmodgames.evolver;

import java.util.ArrayList;
import java.util.List;
import java.util.RandomAccess;

public class TriangleList<E> extends ArrayList<E> implements List<E>, RandomAccess, Cloneable, java.io.Serializable{
	
	private static final long serialVersionUID = -4586973798735728057L;
	private Double score;

	public TriangleList(){
		super();
	}
	
	public TriangleList(TriangleList<? extends E> c) {
		 super(c);
	}

	public Double getScore() {
		return score;
	}

	public void setScore(Double score) {
		this.score = score;
	}
}