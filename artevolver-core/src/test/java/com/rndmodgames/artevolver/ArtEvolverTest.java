package com.rndmodgames.artevolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.rndmodgames.evolver.Palette;
import com.rndmodgames.evolver.PalleteColor;

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
        Palette palette = new Palette("Sherwin-Williams", 1);

        //
        assertNotNull(palette);
    }
    
    @Test
    void getPaletteNumberOfColorsTest() throws IOException, URISyntaxException {
        
        //
        Palette palette = new Palette("Sherwin-Williams", 1);

        //
        assertTrue(palette.getNumberOfColors() > 0);
    }
    
    @Test
    void randomizePaletteTest() throws IOException, URISyntaxException {
        
        //
        Palette palette = new Palette("Sherwin-Williams", 1);
        
        PalleteColor first = palette.getColor(0);
        
        // shuffle
        palette.randomize();
                
        // might fail sometimes!
        assertNotEquals(palette.getColor(0), first);
    }
    
    @Test
    void removeColorTest() throws IOException, URISyntaxException {
        
        //
        Palette palette = new Palette("Sherwin-Williams", 1);
        
        int existingColors = palette.getNumberOfColors();
        
        // remove one color
        palette.removeColor(0);
        
        //
        assertEquals(existingColors - 1, palette.getNumberOfColors());
    }
    
    @Test
    void getColorFromPaletteTest() throws IOException, URISyntaxException {
     
        //
        Palette palette = new Palette("Sherwin-Williams", 1);
        
        //
        assertNotNull(palette.getColor(0).getColor());
    }
    
    @Test
    void orderPalettesByColorTest() throws IOException, URISyntaxException {
        
        //
        Palette palette = new Palette("Sherwin-Williams", 1);

        //
        palette.orderByBLUE();
        palette.orderByGREEN();
        palette.orderByRED();
        palette.orderByLuminescence();
        
        //
        assertNotNull(palette.getColor(0));
    }
}
