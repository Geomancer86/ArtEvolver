package com.rndmodgames.evolver;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.imageio.ImageIO;

/**
 * Art Evolver Tools v1
 * 
 * Used to initialize and run Offline Evolvers
 * 
 * @author WORKSTATION
 *
 */
public class ArtEvolverTools {

    public static ImageEvolver getDefaultEvolver(String imageName) {
        
        return getDefaultImageEvolver(1, 2, 2, 2, imageName, false);
    }
    
    public static ImageEvolver getDefaultImageEvolver(int palettes,
                                               int population,
                                               int randomJumpMaxDistance,
                                               int crossoverMax,
                                               String imageName,
                                               boolean shufflePopulation) {

        return getDefaultImageEvolver(palettes, population, randomJumpMaxDistance,
            crossoverMax, imageName, shufflePopulation, 80, 53, 3f);
    }

    public static ImageEvolver getDefaultImageEvolver(int palettes,
                                               int population,
                                               int randomJumpMaxDistance,
                                               int crossoverMax,
                                               String imageName,
                                               boolean shufflePopulation,
                                               int widthTriangles,
                                               int heightTriangles,
                                               float triangleScale) {
        
        Palette pallete = null;
        
        try {
            
            pallete = new Palette("Sherwin-Williams", palettes);
            
        } catch (IOException | URISyntaxException e) {
            
            // ignore
            e.printStackTrace();
        }

        float width = 3.0f * triangleScale;
        float height = 3.0f * triangleScale;
        
        ImageEvolver evolver = new ImageEvolver(population, 
                                                randomJumpMaxDistance,
                                                crossoverMax,
                                                triangleScale,
                                                pallete,
                                                width,
                                                height,
                                                widthTriangles,
                                                heightTriangles);
        
        // ID needs to be set
        evolver.setId(1L);
        
        // Set source file or default
        File imageFile = new File("./src/test/resources/" + (imageName != null ? imageName : "000_zeldathumb-1920-789452.jpg"));
              
        BufferedImage originalImage = null;
        
        try {
            
            originalImage = ImageIO.read(imageFile);
            
        } catch (IOException e) {
            
            // ignore
            e.printStackTrace();
        }

        evolver.setResizedOriginal(originalImage);
        
        // initialize population
        ImageEvolver.SHUFFLE_PALETTE = shufflePopulation;
        evolver.initialize();
        
        return evolver;
    }
}