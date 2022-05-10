package com.rndmodgames.artevolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ArtEvolverTest {

    @BeforeAll
    static void beforeAll() {
        
//        System.setProperty("java.awt.headless", "true");
    }
    
    
    @Test
    public void basicArtEvolverTest() {
        
        //
        assertEquals(1, 1);
    }
    
    @Test
    public void artEvolverInstanceSecondTest() {
        
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
