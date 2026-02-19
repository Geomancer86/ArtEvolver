package com.rndmodgames.artevolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        
        int POPULATION = 2;
        int RANDOM_JUMP_MAX_DISTANCE = 2;
        int CROSSOVER_MAX = 2;
        float triangleScaleHeight = 1f;
        
        float width = 3f * triangleScaleHeight;
        float height = 3f * triangleScaleHeight;
        
        // 38x39 = 1482 triangles <= 1535 palette colors: all triangles get colors
        int widthTriangles = 38;
        int heightTriangles = 39;
        
        ImageEvolver evolver = new ImageEvolver(POPULATION, 
                                                RANDOM_JUMP_MAX_DISTANCE,
                                                CROSSOVER_MAX,
                                                triangleScaleHeight,
                                                pallete,
                                                width,
                                                height,
                                                widthTriangles,
                                                heightTriangles);
        
        evolver.setId(1L);
        
        File imageFile = new File("./src/test/resources/000_zeldathumb-1920-789452.jpg");
        BufferedImage originalImage = ImageIO.read(imageFile);

        evolver.setResizedOriginal(originalImage);
        evolver.initialize();
        
        long timeStart = 0;
        int iterations = 1500;
        
        evolver.evolve(timeStart, iterations);
        double score = evolver.getBestScore();
        System.out.println("first best score : " + score);
        
        evolver.evolve(timeStart, iterations);
        double secondScore = evolver.getBestScore();
        System.out.println("second best score: " + secondScore);
        
        assertTrue(secondScore >= score,
            "Score should improve or stay same after more evolution iterations");
    }
}
