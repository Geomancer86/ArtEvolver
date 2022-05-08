package com.rndmodgames.artevolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;

class ArtEvolverTest {

    @Test
    void basicArtEvolverTest() {
        
        //
        assertEquals(1, 1);
    }
    
    @Test
    void artEvolverInstanceSecondTest() {
        
        com.rndmodgames.evolver.ArtEvolver evolver = null;
        
        try {
            
            evolver = new com.rndmodgames.evolver.ArtEvolver();
        } catch (IOException e) {
            
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        assertNotNull(evolver);
    }
}
