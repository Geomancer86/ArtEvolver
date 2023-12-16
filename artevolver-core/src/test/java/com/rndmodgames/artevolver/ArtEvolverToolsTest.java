package com.rndmodgames.artevolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;

import com.rndmodgames.evolver.ArtEvolverTools;
import com.rndmodgames.evolver.ImageEvolver;

class ArtEvolverToolsTest {

    @Test
    void getImageEvolverTest() throws IOException, URISyntaxException {
        
        // 
        ImageEvolver evolver = ArtEvolverTools.getImageEvolver(1, 2, 2, 2, null, false);
        
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
    void shufflePopulationTest() {
        
        // 
        ImageEvolver evolver = ArtEvolverTools.getImageEvolver(1, 8, 8, 2, null, false);
        ImageEvolver evolver2 = ArtEvolverTools.getImageEvolver(1, 8, 8, 2, null, false);
        ImageEvolver evolverShuffled = ArtEvolverTools.getImageEvolver(1, 8, 8, 2, null, true);
        ImageEvolver evolverShuffled2 = ArtEvolverTools.getImageEvolver(1, 8, 8, 2, null, true);
        
        long timeStart = 0;
        int iterations = 100; 
        
        // evolver for first score
        evolver.evolve(timeStart, iterations);
        evolver2.evolve(timeStart, iterations);
        evolverShuffled.evolve(timeStart, iterations);
        evolverShuffled2.evolve(timeStart, iterations);
        
        
        // compare scores
        assertNotEquals(evolver.getBestScore(), evolver2.getBestScore());
    }
    
    @Test
    void imageEvolverOptimizedRunTest() {
        
        // iteration runs
        int runs = 100;
        
        long timeStart = 0;
        int evolveIterations = 10;
        
        // First baseline run:
        ImageEvolver evolver = ArtEvolverTools.getImageEvolver(1, 2, 1, 1, null, false);
        ImageEvolver evolver2 = ArtEvolverTools.getImageEvolver(1, 2, 1000, 1, null, false);
        
        evolver.evolve(0, evolveIterations);
        evolver2.evolve(0, evolveIterations);
        
        double scoreA = evolver.getBestScore();
        double scoreB = evolver2.getBestScore();
        
        // evolve runs
        for (int a = 0; a < runs; a++) {
            
            System.out.println("evolve run #" + a);
            
            // evolve
            evolver.evolve(timeStart, evolveIterations);
            evolver2.evolve(timeStart, evolveIterations);
            
            System.out.println("score a: " + scoreA);
            System.out.println("score b: " + scoreB);
        }
        
        System.out.println("--------------------");
        System.out.println("FINAL SCORES:");
        
        System.out.println("score a: " + scoreA);
        System.out.println("score b: " + scoreB);
        
        assertNotEquals(scoreA, scoreB);
    }
    
    @Test
    void imageEvolverMediumTest() {
        
        // 
        ImageEvolver evolver = ArtEvolverTools.getImageEvolver(1, 8, 8, 2, null, false);
        
        long timeStart = 0;
        int iterations = 200;
        
        // iteration runs
        int runs = 10;
        
        // baseline score
        evolver.evolve(timeStart, iterations);
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
    
    @Test
    void getImageEvolverQuickTest() throws IOException, URISyntaxException {
        
        // 
        ImageEvolver evolver = ArtEvolverTools.getImageEvolver(1, 2, 2, 2, null, false);
        
        long timeStart = 0;
        int iterations = 20;
        
        // iteration runs
        int runs = 10;
        
        // baseline score
        evolver.evolve(timeStart, iterations);
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