package com.rndmodgames.artevolver;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.rndmodgames.evolver.Palette;

class ArtEvolverTest {

    static com.rndmodgames.evolver.ArtEvolver evolver;
    
    @BeforeAll
    static void beforeAll() throws IOException, URISyntaxException {
        
         evolver = new com.rndmodgames.evolver.ArtEvolver();
    }
    
    @Test
    void artEvolverInstanceSecondTest() {
        
        //
        assertNotNull(evolver);
    }
    
    @Test
    void paletteTest() throws IOException, URISyntaxException {
        
        //
        Palette pallete = new Palette("Sherwin-Williams", 1);

        //
        assertNotNull(pallete);
    }
}
