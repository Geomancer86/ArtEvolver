package com.rndmodgames.artevolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import com.rndmodgames.evolver.CrossOver;
import com.rndmodgames.evolver.ImageEvolver;
import com.rndmodgames.evolver.Palette;

class CrossOverTest {

    @Test
    void basicCrossOverTest() {
        
        CrossOver crossOver = new CrossOver(1, 1, null);
        
        assertEquals(1, crossOver.getRandomJumpDistance());
    }
    
    @Test 
    void halveCrossOverParameterTest(){
        
        CrossOver crossOver = new CrossOver(1000, 1, null);
        
        crossOver.halveParameters();
        
        assertEquals(500, crossOver.getRandomJumpDistance());
    }
    
    @Test
    void incrementCrossOverParameterTest() {
        
        CrossOver crossOver = new CrossOver(1000, 1, null);
        
        crossOver.incrementParameters(10);
        
        assertEquals(1010, crossOver.getRandomJumpDistance());
    }
    
    @Test
    void getAverageSuccessfulJumpSize() throws IOException, URISyntaxException {
        
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
        
        // evolve
        evolver.evolve(timeStart, iterations);
        
        // get score
        double score = evolver.getBestScore();
        
        System.out.println("first best score : " + score);
        
        // evolve again
        evolver.evolve(timeStart, iterations);
        
        double secondScore = evolver.getBestScore();
        
        System.out.println("second best score: " + secondScore);
        
        assertNotEquals(score, secondScore);
    }
}
