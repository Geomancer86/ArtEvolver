package com.rndmodgames.artevolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class ArtEvolverTests {

    @Test
    void basicArtEvolverTest() {
        
        //
        assertEquals(1, 1);
    }
    
    @Test
    void artEvolverInstanceTest() {
        
        ArtEvolver evolver = new ArtEvolver();
        
        assertNotNull(evolver);
    }
}
