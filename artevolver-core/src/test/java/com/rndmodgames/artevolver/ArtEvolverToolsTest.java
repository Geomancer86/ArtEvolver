package com.rndmodgames.artevolver;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;

import com.rndmodgames.evolver.ArtEvolverTools;
import com.rndmodgames.evolver.ImageEvolver;

class ArtEvolverToolsTest {

    @Test
    void getImageEvolverTest() throws IOException, URISyntaxException {
        
        // 
        ImageEvolver evolver = ArtEvolverTools.getImageEvolver(1, 2, 2, 2, null);
        
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
    
    @Test
    void getImageEvolverQuickTest() throws IOException, URISyntaxException {
        
        // 
        ImageEvolver evolver = ArtEvolverTools.getImageEvolver(1, 2, 2, 2, null);
        
        long timeStart = 0;
        int iterations = 20;
        
        // iteration runs
        int runs = 10;
        
        // baseline score
        double score = evolver.getBestScore();
        
        // evolve runs
        for (int a = 0; a < runs; a++) {
            
            // evolve
            evolver.evolve(timeStart, iterations);
        }

        // measure after evolution runs
        double secondScore = evolver.getBestScore();
        
        System.out.println("starting score: " + score);
        System.out.println("final score   : " + secondScore);
        
        assertNotEquals(score, secondScore);
    }
}