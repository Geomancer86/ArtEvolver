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

    public static ImageEvolver getImageEvolver(int palettes,
                                               int population,
                                               int randomJumpMaxDistance,
                                               int crossoverMax,
                                               String imageName,
                                               boolean shufflePopulation) {
        
        Palette pallete = null;
        
        try {
            
            pallete = new Palette("Sherwin-Williams", palettes);
            
        } catch (IOException | URISyntaxException e) {
            
            // ignore
            e.printStackTrace();
        }

        // FAST SPEED
        float triangleScaleHeight = 1f;
        float width = 1f;
        float height = 1f;
        
        // REGULAR MODE
        int widthTriangles = 80;
        int heightTriangles = 53;
        
        ImageEvolver evolver = new ImageEvolver(population, 
                                                randomJumpMaxDistance,
                                                crossoverMax,
                                                triangleScaleHeight,
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