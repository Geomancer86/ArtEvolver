package com.rndmodgames.evolver;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class Palette {

	String name;
	int totalPalletes;
	long currentId = 0;
	
	List <PalleteColor> colors = new ArrayList<PalleteColor>();

	public Palette(String name, int repetitions) throws IOException, URISyntaxException {

		this.name = name;
		this.totalPalletes = repetitions;
		
		URL url = getClass().getResource("../../../sherwin.txt").toURI().toURL();  // SHERWIN
//		URL url = getClass().getResource("../../../palette2.txt").toURI().toURL(); // ORIGINAL GBC GREEN 4 SHADES 25% EACH
//		URL url = getClass().getResource("../../../palette3.txt").toURI().toURL(); // BLACK AND WHITE 50% EACH
//		URL url = getClass().getResource("../../../palette4.txt").toURI().toURL(); // SHERWIN 1500+ colors
//		URL url = getClass().getResource("../../../trilux.txt").toURI().toURL(); // 12 Pen Colors

		File file = new File(url.getPath().replace("%20"," "));
		
		for (int a = 0; a < repetitions; a++){
			try (Stream<String>stream = Files.lines(file.toPath(), StandardCharsets.ISO_8859_1)) {
				stream.forEach((line)->{
				    
//				    System.out.println("line: " + line);
				    
					PalleteColor color = null;
					String [] splitted = line.split(" ");
	
					// disabled current id as count, using the palette file id
//					currentId++;
					
					if (splitted.length == 5){
						
						color = new PalleteColor(this,
												 Long.valueOf(splitted[0]),
//												 currentId,
												 splitted[1],
												 Integer.valueOf(splitted[2]).intValue(),
												 Integer.valueOf(splitted[3]).intValue(),
												 Integer.valueOf(splitted[4]).intValue());
					}else{
						if (splitted.length == 6){
							color = new PalleteColor(this,
									  Long.valueOf(splitted[0]),
//									 currentId,
									 splitted[1] + "\n" + splitted[2],
									 Integer.valueOf(splitted[3]).intValue(),
									 Integer.valueOf(splitted[4]).intValue(),
									 Integer.valueOf(splitted[5]).intValue());
						}else{
							if (splitted.length == 7){
								color = new PalleteColor(this,
										  Long.valueOf(splitted[0]),
//										 currentId,
										 splitted[1] + " " + splitted[2] + "\n" + splitted[3],
										 Integer.valueOf(splitted[4]).intValue(),
										 Integer.valueOf(splitted[5]).intValue(),
										 Integer.valueOf(splitted[6]).intValue());
							}else{
								System.out.println("error parsing pallete color!");
								System.out.println(line);
							}
						}
					}
					
					colors.add(color);
				});
			}
		}
		
		// Todo dynamic pallete loading or combobox
//		System.out.println("Pallete " + name + " Loaded: " + colors.size() + " colors in " + repetitions + " repetitions.");
	}
	

	/**
	 * @return the number of colors in this Palette
	 */
	public int getNumberOfColors() {
	    
	    return colors.size();
	}

	public void randomize(){
		Collections.shuffle(colors);
	}
	
	public void orderByRED(){
		Collections.sort(colors, new Comparator<PalleteColor>() {
	        @Override
	        public int compare(PalleteColor c1, PalleteColor c2) {
	            return Float.compare(c1.getColor().getRed(), c2.getColor().getRed());
	        }
	    });
	}
	
	public void orderByGREEN(){
		Collections.sort(colors, new Comparator<PalleteColor>() {
	        @Override
	        public int compare(PalleteColor c1, PalleteColor c2) {
	            return Float.compare(c1.getColor().getGreen(), c2.getColor().getGreen());
	        }
	    });
	}
	
	public void orderByBLUE(){
		Collections.sort(colors, new Comparator<PalleteColor>() {
	        @Override
	        public int compare(PalleteColor c1, PalleteColor c2) {
	            return Float.compare(c1.getColor().getBlue(), c2.getColor().getBlue());
	        }
	    });
	}
	
	public void orderByLuminescence(){
		Collections.sort(colors, new Comparator<PalleteColor>() {
	        @Override
	        public int compare(PalleteColor c1, PalleteColor c2) {
	            return Float.compare(((float) c1.getColor().getRed() * 0.299f + (float) c1.getColor().getGreen() * 0.587f
	                    + (float) c1.getColor().getBlue() * 0.114f) / 256f, ((float) c2.getColor().getRed() * 0.299f + (float) c2.getColor().getGreen()
	                    * 0.587f + (float) c2.getColor().getBlue() * 0.114f) / 256f);
	        }
	    });
	}
	
	public void removeColor(int index) {
		colors.remove(index);
	}
	
	public PalleteColor getColor(int index){
		if (index >= colors.size()){
			return null;
		}
		
		return colors.get(index);
	}
}