package com.rndmodgames.artevolver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.rndmodgames.evolver.CrossOver;

class CrossOverTest {

    @Test
    void basicCrossOverTest() {
        
        CrossOver crossOver = new CrossOver(1, 1);
        
        assertEquals(1, crossOver.getRandomJumpDistance());
    }
    
    @Test 
    void halveCrossOverParameterTest(){
        
        CrossOver crossOver = new CrossOver(1000, 1);
        
        crossOver.halveParameters();
        
        assertEquals(500, crossOver.getRandomJumpDistance());
    }
    
    @Test
    void incrementCrossOverParameterTest() {
        
        CrossOver crossOver = new CrossOver(1000, 1);
        
        crossOver.incrementParameters(10);
        
        assertEquals(1010, crossOver.getRandomJumpDistance());
    }
    
    @Test
    void getAverageSuccessfulJumpSize() {
        
        CrossOver crossOver = new CrossOver(1000, 1);
        
        
    }
}
