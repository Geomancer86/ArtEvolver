package com.rndmodgames.artevolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import org.junit.jupiter.api.Test;

import com.rndmodgames.evolver.ImageEvolver;
import com.rndmodgames.evolver.Palette;

class ImageEvolverTest {
    
    @Test
    void imageEvolverTest() throws IOException, URISyntaxException {
     
        Palette pallete = new Palette("Sherwin-Williams", 1);

        // Create Evolver instances as configured by the THREADS parameter
        int POPULATION = 2;
        int RANDOM_JUMP_MAX_DISTANCE = 2;
        int CROSSOVER_MAX = 2;
        float triangleScaleHeight = 1f;
        
        // FAST SPEED
        float width = 1f;
        float height = 1f;
        
        // REGULAR MODE
        int widthTriangles = 80;
        int heightTriangles = 53;
        
        ImageEvolver evolver = new ImageEvolver(POPULATION, 
                                          RANDOM_JUMP_MAX_DISTANCE,
                                          CROSSOVER_MAX,
                                          triangleScaleHeight,
                                          pallete,
                                          width,
                                          height,
                                          widthTriangles,
                                          heightTriangles);
        
        
        assertNotNull(evolver);
    }
    
    @Test
    void evolveTest() throws IOException, URISyntaxException {

        Palette pallete = new Palette("Sherwin-Williams", 1);
        
        // Create Evolver instances as configured by the THREADS parameter
        int POPULATION = 2;
        int RANDOM_JUMP_MAX_DISTANCE = 2;
        int CROSSOVER_MAX = 2;
        float triangleScaleHeight = 1f;
        
        // FAST SPEED
        float width = 1f;
        float height = 1f;
        
        // REGULAR MODE
        int widthTriangles = 80;
        int heightTriangles = 53;
        
        ImageEvolver evolver = new ImageEvolver(POPULATION, 
                                                RANDOM_JUMP_MAX_DISTANCE,
                                                CROSSOVER_MAX,
                                                triangleScaleHeight,
                                                pallete,
                                                width,
                                                height,
                                                widthTriangles,
                                                heightTriangles);
        
        // ID needs to be set
        evolver.setId(1L);
        
        // Set source file
        File imageFile = new File("./000_zeldathumb-1920-789452.jpg");
              
        BufferedImage originalImage = ImageIO.read(imageFile);

        evolver.setResizedOriginal(originalImage);
        
        // initialize population
        evolver.initialize();
        
        long timeStart = 0;
        int iterations = 100;
        
        evolver.evolve(timeStart, iterations);
        
        assertEquals(iterations, evolver.getTotalIterations());
    }
}