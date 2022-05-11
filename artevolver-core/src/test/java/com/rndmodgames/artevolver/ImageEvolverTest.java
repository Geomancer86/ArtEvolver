package com.rndmodgames.artevolver;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;

import com.rndmodgames.evolver.ImageEvolver;
import com.rndmodgames.evolver.Palette;

public class ImageEvolverTest {
    
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
}